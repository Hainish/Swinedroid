package com.legind.swinedroid.NetworkRunnable;

import java.io.IOException;

import org.xml.sax.SAXException;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.legind.sqlite.ServerDbAdapter;
import com.legind.swinedroid.ServerHashDialog;
import com.legind.swinedroid.RequestService.Request;
import com.legind.swinedroid.xml.XMLHandlerException;
import com.legind.web.WebTransport.WebTransportException;


public class NetworkRunnable implements Runnable{
	private ServerDbAdapter mDbHelper;
	private Request mBoundRequest;
	private final NetworkRunnableRequires parent;
	private final int DOCUMENT_VALID = 0;
	private final int IO_ERROR = 1;
	private final int XML_ERROR = 2;
	private final int SERVER_ERROR = 3;
	private final int CERT_ERROR = 4;
	private final int CERT_REJECTED = 0;
	private final int CERT_ACCEPTED = 1;
	private final String LOG_TAG = "com.legind.swinedroid.SuperClass.NetworkRunnable";
	
	public NetworkRunnable(NetworkRunnableRequires parent){
		this.parent = parent;
		mDbHelper = new ServerDbAdapter(parent.getContext());
		mDbHelper.open();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
		    mBoundRequest = ((Request.RequestBinder)service).getService();
		    parent.onBoundRequestSet();
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mBoundRequest = null;
		}
	};
    
	public void startRequestService() {
        Intent newIntent = new Intent(parent.getContext(), Request.class);
        newIntent.putExtra(Request.ROW_ID_TAG, parent.getRowId());
        parent.startService(newIntent);
        parent.bindService(newIntent, mConnection, 0);
	}
	
	public void close(){
		mDbHelper.close();
		if(mBoundRequest != null)
			parent.unbindService(mConnection);
	}
	
	public Request getBoundRequest(){
		return mBoundRequest;
	}
    
	public void run() {
		sendRequestHandleResponse();
	}
	
	public void sendRequestHandleResponse(){
		try{
			mBoundRequest.openWebTransportConnection();
			if(!mBoundRequest.inspectCertificate()){
				handler.sendEmptyMessage(CERT_ERROR);
			} else {
				parent.onCertificateInspectVerified();
				handler.sendEmptyMessage(DOCUMENT_VALID);
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
			handler.sendEmptyMessage(IO_ERROR);
		} catch (SAXException e) {
			Log.e(LOG_TAG, e.toString());
			handler.sendEmptyMessage(XML_ERROR);
		} catch (XMLHandlerException e){
			Log.e(LOG_TAG, e.toString());
			Message msg = Message.obtain();
			msg.setTarget(handler);
			msg.what = SERVER_ERROR;
			msg.obj = e.getMessage();
			msg.sendToTarget();
		} catch (WebTransportException e){
			mBoundRequest.closeWebTransportConnection();
			sendRequestHandleResponse();
		}
	}

	// Catch and display any errors sent to the handler, otherwise populate all statistics fields
	private volatile Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			parent.onHandleMessageBegin();
			OnCancelListener cancelListener = parent.getCancelListener();
			switch(message.what){
				case IO_ERROR:
					parent.getEMH().DisplayErrorMessage("Could not connect to server.  Please ensure that your settings are correct and try again later.",cancelListener);
				break;
				case XML_ERROR:
					parent.getEMH().DisplayErrorMessage("Server responded with an invalid XML document.  Please try again later.",cancelListener);
				break;
				case SERVER_ERROR:
					parent.getEMH().DisplayErrorMessage((String) message.obj,cancelListener);
				break;
				case CERT_ERROR:
					/*
					 * If there is a certificate mismatch, display the ServerHashDialog activity
					 */
					// must set this to true, in case onPause happens for child activity
					parent.onCertErrorBegin();
					//mBoundRequest.displayCertificateDialog(ServerView.this);
		        	Intent i = new Intent(parent.getContext(), ServerHashDialog.class);
		        	i.putExtra("SHA1", mBoundRequest.getLastServerCertSHA1());
		        	i.putExtra("MD5", mBoundRequest.getLastServerCertMD5());
		        	i.putExtra("CERT_INVALID", (mBoundRequest.getLastServerCertSHA1() == null && mBoundRequest.getLastServerCertMD5() == null ? false : true));
		        	parent.callHashDialog(i);
				break;
				case DOCUMENT_VALID:
					parent.onDocumentValidReturned();
				break;
			}

		}
	};
	
	public void certificateActivityResult(int requestCode, int resultCode, Intent intent, int hashDialogActivity){
        if(requestCode == hashDialogActivity){
			/*
			 * The ServerHashDialog activity has finished.  if the cert is rejected, finish this activity.
			 * If accepted, try connecting to the server all over again.
			 */
			switch(resultCode){
				case CERT_REJECTED:
					parent.finish();
				break;
				case CERT_ACCEPTED:
					Bundle extras = intent.getExtras();
					mDbHelper.updateSeverHashes(parent.getRowId(), extras.getString("MD5"), extras.getString("SHA1"));
					mBoundRequest.fetchServerHashes();
					Thread thread = new Thread(this);
					thread.start();
				break;
			}
        }
	}
}