package com.legind.swinedroid;

import java.io.IOException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.AlertDbAdapter;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.ssl.CertificateInspect.CertificateInspect;
import com.legind.swinedroid.xml.AlertXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;

public class AlertView extends Activity{
	private Long mRowId;
	private ProgressDialog pd;
	private AlertXMLHandler mAlertXMLHandler;
	private ServerDbAdapter mDbHelper;
	private long mCid;
	private long mSid;
	private String mSigName;
	private long mIpSrc;
	private long mIpDst;
	private String mDate;
	private String mTime;
	private byte mSigPriority;
	private String mHostText;
	private int mPortInt;
	private String mUsernameText;
	private String mPasswordText;
	private final String LOG_TAG = "com.legind.swinedroid.AlertView";
	private boolean mGotAlert;
	AlertDisplayRunnable alertRunnable;
	private final int ACTIVITY_HASH_DIALOG = 0;
	
	private class AlertDisplayRunnable implements Runnable {
		private Context mCtx;
		private final int DOCUMENT_VALID = 0;
		private final int IO_ERROR = 1;
		private final int XML_ERROR = 2;
		private final int SERVER_ERROR = 3;
		private final int CERT_ERROR = 4;
		private ErrorMessageHandler mEMH;
		
		

		/**
		 * Constructor for AlertDisplayRunnable.  Runnable becomes context-aware
		 */
		public AlertDisplayRunnable(){
			// Display all errors on the AlertList ListActivity
			Context errorMessageContext = AlertList.LA;
			mEMH = new ErrorMessageHandler(errorMessageContext,
					findViewById(R.id.server_edit_error_layout_root));
		}
		
		/**
		 * Send an XML request to the XML handler.  If an error occurs, send a message
		 * to the handler with the appropriate error code
		 */
		public void run() {
			try {
				Cursor server = mDbHelper.fetch(mRowId);
				startManagingCursor(server);
				String mMD5 = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_MD5));
				String mSHA1 = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_SHA1));
				mAlertXMLHandler.openWebTransportConnection(mHostText, mPortInt);
				CertificateInspect serverCertificateInspect = new CertificateInspect(mAlertXMLHandler.getWebTransportConnection().getServerCertificate());
				String mServerCertMD5 = serverCertificateInspect.generateFingerprint("MD5");
				String mServerCertSHA1 = serverCertificateInspect.generateFingerprint("SHA1"); 
				if(!mServerCertSHA1.equals(mSHA1) || !mServerCertMD5.equals(mMD5)){
					Message msg = Message.obtain();
					msg.setTarget(handler);
					msg.what = CERT_ERROR;
					msg.obj = new Object[]{mServerCertSHA1, mServerCertMD5, (mSHA1 == null && mMD5 == null ? false : true)};
					msg.sendToTarget();
				} else {
					// construct the GET arguments string, send it to the XML handler
					String extraArgs = "cid=" + mCid + "&sid=" + mSid;
					mAlertXMLHandler.createElement(mCtx, mUsernameText, mPasswordText, "alert", extraArgs);
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
			}
		}

		/**
		 *  Catch and display any errors sent to the handler, otherwise populate all alerts
		 */
		private Handler handler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				/* We must dismiss the ProgressDialogue if it is the initial alerts population,
				 * and show the previous view for the switcher for additional alerts.
				 */
				pd.dismiss();
				switch(message.what){
					case IO_ERROR:
						mEMH.DisplayErrorMessage("Could not connect to server.  Please ensure that your settings are correct and try again later.");
					break;
					case XML_ERROR:
						mEMH.DisplayErrorMessage("Server responded with an invalid XML document.  Please try again later.");
					break;
					case SERVER_ERROR:
						mEMH.DisplayErrorMessage((String) message.obj);
					break;
					case CERT_ERROR:
						/*
						 * If there is a certificate mismatch, display the ServerHashDialog activity
						 */
						// must set this to true, in case onPause happens for child activity
						mGotAlert = true;
						Object[] messageObject = (Object[]) message.obj;
			        	Intent i = new Intent(AlertView.this, ServerHashDialog.class);
			        	i.putExtra("SHA1", (String) messageObject[0]);
			        	i.putExtra("MD5", (String) messageObject[1]);
			        	i.putExtra("CERT_INVALID", (Boolean) messageObject[2]);
						startActivityForResult(i, ACTIVITY_HASH_DIALOG);
					break;
					case DOCUMENT_VALID:
						mGotAlert = true;
						//fillData();
					break;
				}
				if(message.what != DOCUMENT_VALID && message.what != CERT_ERROR){
					finish();
				}

			}
		};
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	// Open up the XML and db handlers
		mAlertXMLHandler = new AlertXMLHandler();
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.alert_view);
		// initial value of mGotAlert is false
		mGotAlert = false;
		
		// create the runnables
		alertRunnable = new AlertDisplayRunnable();

		if(savedInstanceState != null){
			// if we have a savedInstanceState, load the strings directly
			mRowId = savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID);
	        mCid = savedInstanceState.getLong(AlertDbAdapter.KEY_CID);
	        mSid = savedInstanceState.getLong(AlertDbAdapter.KEY_SID);
	        mSigName = savedInstanceState.getString(AlertDbAdapter.KEY_SIG_NAME);
	        mIpSrc = savedInstanceState.getLong(AlertDbAdapter.KEY_IP_SRC);
	        mIpDst = savedInstanceState.getLong(AlertDbAdapter.KEY_IP_DST);
	        mDate = savedInstanceState.getString("date");
	        mTime = savedInstanceState.getString("time");
			mSigPriority = savedInstanceState.getByte(AlertDbAdapter.KEY_SIG_PRIORITY);
			mGotAlert = savedInstanceState.getBoolean("mGotAlert");
		} else {
			Bundle extras = getIntent().getExtras();
			if(extras != null){
				// if we have an intent, construct the strings from chosen fields.  add 1 to months
				mRowId = extras.getLong(ServerDbAdapter.KEY_ROWID);
				mCid = extras.getLong(AlertDbAdapter.KEY_CID);
				mSid = extras.getLong(AlertDbAdapter.KEY_SID);
				mSigName = extras.getString(AlertDbAdapter.KEY_SIG_NAME);
				mIpSrc = extras.getLong(AlertDbAdapter.KEY_IP_SRC);
				mIpDst = extras.getLong(AlertDbAdapter.KEY_IP_DST);
				mDate = extras.getString("date");
				mTime = extras.getString("time");
				mSigPriority = extras.getByte(AlertDbAdapter.KEY_SIG_PRIORITY);
			}
		}

		if (mRowId != null) {
			Cursor server = mDbHelper.fetch(mRowId);
			startManagingCursor(server);
			mHostText = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_HOST));
			mPortInt = server.getInt(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_PORT));
			mUsernameText = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_USERNAME));
			mPasswordText = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_PASSWORD));
		}

		if(!mGotAlert){
			// Display the progress dialog first
			pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);
			Thread thread = new Thread(alertRunnable);
			thread.start();
		} else {
	    	//fillData();
		}
	}
    
	@Override
	protected void onDestroy() {
		mDbHelper.close();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save not only server row, but all the search strings
		//if(!mGotAlert)
		//	pd.dismiss();
		outState.putLong("mGotAlert", mRowId);
		outState.putLong(AlertDbAdapter.KEY_CID, mCid);
		outState.putLong(AlertDbAdapter.KEY_SID, mSid);
		outState.putString(AlertDbAdapter.KEY_SIG_NAME, mSigName);
		outState.putLong(AlertDbAdapter.KEY_IP_SRC, mIpSrc);
		outState.putLong(AlertDbAdapter.KEY_IP_DST, mIpDst);
		outState.putString("date", mDate);
		outState.putString("time", mTime);
		outState.putByte(AlertDbAdapter.KEY_SIG_PRIORITY, mSigPriority);
	}
}