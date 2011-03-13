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
	private final NetworkRunnableUniqueRequires nrur;
	private final NetworkRunnableBindRequires nrbr;
	private final int DOCUMENT_VALID = 0;
	private final int IO_ERROR = 1;
	private final int XML_ERROR = 2;
	private final int SERVER_ERROR = 3;
	private final int CERT_ERROR = 4;
	private final int CERT_REJECTED = 0;
	private final int CERT_ACCEPTED = 1;
	private final int HAS_MANAGER_YES = 0;
	private final int HAS_MANAGER_NO = 1;
	private final String LOG_TAG = "com.legind.swinedroid.SuperClass.NetworkRunnable";
	private int mHasManager;

	public NetworkRunnable(NetworkRunnableRequires nrr){
		this.nrur = (NetworkRunnableUniqueRequires) nrr;
		this.nrbr = (NetworkRunnableBindRequires) nrr;
		mDbHelper = new ServerDbAdapter(nrbr.getContext());
		mDbHelper.open();
		mHasManager = HAS_MANAGER_NO;
	}
	
	public NetworkRunnable(NetworkRunnableUniqueRequires nrur, NetworkRunnableBindRequires nrbr, ServerDbAdapter dbHelper, Request boundRequest){
		this.nrur = nrur;
		this.nrbr = nrbr;
		mDbHelper = dbHelper;
		mBoundRequest = boundRequest;
		mHasManager = HAS_MANAGER_YES;
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
		    mBoundRequest = ((Request.RequestBinder)service).getService();
		    nrbr.onBoundRequestSet();
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mBoundRequest = null;
		}
	};
    
	public void startRequestService() {
        Intent newIntent = new Intent(nrbr.getContext(), Request.class);
        newIntent.putExtra(Request.ROW_ID_TAG, nrbr.getRowId());
        nrbr.startService(newIntent);
        nrbr.bindService(newIntent, mConnection, 0);
	}
	
	public void close(){
		if(mHasManager == HAS_MANAGER_NO){
			mDbHelper.close();
			if(mBoundRequest != null)
				nrbr.unbindService(mConnection);
		}
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
				nrur.onCertificateInspectVerified();
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
			nrur.onHandleMessageBegin();
			OnCancelListener cancelListener = nrur.getCancelListener();
			switch(message.what){
				case IO_ERROR:
					nrur.getEMH().DisplayErrorMessage("Could not connect to server.  Please ensure that your settings are correct and try again later.",cancelListener);
				break;
				case XML_ERROR:
					nrur.getEMH().DisplayErrorMessage("Server responded with an invalid XML document.  Please try again later.",cancelListener);
				break;
				case SERVER_ERROR:
					nrur.getEMH().DisplayErrorMessage((String) message.obj,cancelListener);
				break;
				case CERT_ERROR:
					/*
					 * If there is a certificate mismatch, display the ServerHashDialog activity
					 */
					// must set this to true, in case onPause happens for child activity
					nrur.onCertErrorBegin();
					//mBoundRequest.displayCertificateDialog(ServerView.this);
		        	Intent i = new Intent(nrbr.getContext(), ServerHashDialog.class);
		        	i.putExtra("SHA1", mBoundRequest.getLastServerCertSHA1());
		        	i.putExtra("MD5", mBoundRequest.getLastServerCertMD5());
		        	i.putExtra("CERT_INVALID", (mBoundRequest.getLastServerCertSHA1() == null && mBoundRequest.getLastServerCertMD5() == null ? false : true));
		        	nrur.callHashDialog(i);
				break;
				case DOCUMENT_VALID:
					nrur.onDocumentValidReturned();
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
					nrbr.finish();
				break;
				case CERT_ACCEPTED:
					Bundle extras = intent.getExtras();
					mDbHelper.updateServerHashes(nrbr.getRowId(), extras.getString("MD5"), extras.getString("SHA1"));
					mBoundRequest.fetchServerHashes();
					Thread thread = new Thread(this);
					thread.start();
				break;
			}
        }
	}
}