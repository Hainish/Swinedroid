package com.legind.swinedroid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;

import com.legind.sqlite.AlertDbAdapter;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.swinedroid.xml.AlertXMLHandler;

public class AlertView extends Activity{
	private Long mRowId;
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
	private boolean mGotAlert;

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
				mGotAlert = savedInstanceState.getBoolean("mGotAlert");
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

		//if(!mGotAlert){
		//	// Display the progress dialog first
		//	pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);
		//	Thread thread = new Thread(initialAlertsRunnable);
		//	thread.start();
		//} else {
	    //	fillData();
		//}
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