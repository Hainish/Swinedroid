package com.legind.swinedroid;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import org.xml.sax.SAXException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.ViewSwitcher;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.AlertDbAdapter;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.ssl.CertificateInspect.CertificateInspect;
import com.legind.swinedroid.xml.AlertListXMLElement;
import com.legind.swinedroid.xml.AlertListXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;

public class AlertList extends ListActivity{
	private Long mRowId;
	private String mAlertSeverity;
	private String mSearchTerm;
	private String mBeginningDatetime;
	private String mEndingDatetime;
	private int mPortInt;
	private String mHostText;
	private String mUsernameText;
	private String mPasswordText;
	private AlertListXMLHandler mAlertListXMLHandler;
	private ProgressDialog pd;
	private ServerDbAdapter mDbHelper;
	private AlertDbAdapter mAlertDbHelper;
	private final String LOG_TAG = "com.legind.swinedroid.AlertList";
	private boolean mGotAlerts;
	private ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	private static final SimpleDateFormat yearMonthDayFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat hourMinuteSecondFormat = new SimpleDateFormat("HH:mm:ss");
	private SimpleAdapter alertListAdapter;
	private ViewSwitcher switcher;
	private long mNumAlertsDisplayed;
	private long mNumAlertsTotal;
	private final int ALERTS_INITIAL = 0;
	private final int ALERTS_ADDITIONAL = 1;
	private final int ACTIVITY_HASH_DIALOG_INITIAL = 0;
	private final int ACTIVITY_HASH_DIALOG_ADDITIONAL = 1;
	private final int CERT_REJECTED = 0;
	private final int CERT_ACCEPTED = 1;
	AlertsDisplayRunnable additionalAlertsRunnable;
	AlertsDisplayRunnable initialAlertsRunnable;
	
	private class AlertsDisplayRunnable implements Runnable {
		private Context mCtx;
		private int mFromCode;
		private final int DOCUMENT_VALID = 0;
		private final int IO_ERROR = 1;
		private final int XML_ERROR = 2;
		private final int SERVER_ERROR = 3;
		private final int CERT_ERROR = 4;
		private ErrorMessageHandler mEMH;

		/**
		 * Constructor for AlertsDisplayRunnable.  Runnable becomes context and caller-aware 
		 * 
		 * @param ctx the AlertList context from which it is called
		 * @param fromCode how this runnable is called, either loading the inital alerts or additional alerts
		 */
		public AlertsDisplayRunnable(Context ctx, int fromCode){
			mCtx = ctx;
			mFromCode = fromCode;
			// Display all errors on the ServerView ListActivity
			Context errorMessageContext = ServerView.LA;
			switch(mFromCode){
				case ALERTS_ADDITIONAL:
					// Display all errors on the AlertList ListActivity
					errorMessageContext = mCtx;
				break;
			}
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
				mAlertListXMLHandler.openWebTransportConnection(mHostText, mPortInt);
				CertificateInspect serverCertificateInspect = new CertificateInspect(mAlertListXMLHandler.getWebTransportConnection().getServerCertificate());
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
					String extraArgs = "alert_severity=" + mAlertSeverity + "&search_term=" + mSearchTerm + (mBeginningDatetime != null ? "&beginning_datetime=" + mBeginningDatetime : "") + (mEndingDatetime != null ? "&ending_datetime=" + mEndingDatetime : "") + "&starting_at=" + String.valueOf(mNumAlertsDisplayed);
					mAlertListXMLHandler.createElement(mCtx, mUsernameText, mPasswordText, "alerts", extraArgs);
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
				switch(mFromCode){
					case ALERTS_INITIAL:
						pd.dismiss();
					break;
					case ALERTS_ADDITIONAL:
						switcher.showPrevious();
					break;
				}
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
						mGotAlerts = true;
						Object[] messageObject = (Object[]) message.obj;
			        	Intent i = new Intent(AlertList.this, ServerHashDialog.class);
			        	i.putExtra("SHA1", (String) messageObject[0]);
			        	i.putExtra("MD5", (String) messageObject[1]);
			        	i.putExtra("CERT_INVALID", (Boolean) messageObject[2]);
						switch(mFromCode){
							case ALERTS_INITIAL:
								startActivityForResult(i, ACTIVITY_HASH_DIALOG_INITIAL);
							break;
							case ALERTS_ADDITIONAL:
								startActivityForResult(i, ACTIVITY_HASH_DIALOG_ADDITIONAL);
							break;
						}
					break;
					case DOCUMENT_VALID:
						switch(mFromCode){
							case ALERTS_INITIAL:
								// clear the alerts database
								mAlertDbHelper.deleteAll();
								mGotAlerts = true;
								mNumAlertsTotal = mAlertListXMLHandler.numAlerts;
								mAlertDbHelper.createAlertsFromAlertList(mAlertListXMLHandler.alertList);
								fillData();
							break;
							case ALERTS_ADDITIONAL:
								mAlertDbHelper.createAlertsFromAlertList(mAlertListXMLHandler.alertList);
								fillDataFromAlertList(mAlertListXMLHandler.alertList);
							break;
						}
					break;
				}
				if(message.what != DOCUMENT_VALID && message.what != CERT_ERROR){
					switch(mFromCode){
						case ALERTS_INITIAL:
							finish();
						break;
					}
				}

			}
		};
		
	}
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	// Open up the XML and db handlers
		mAlertListXMLHandler = new AlertListXMLHandler();
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();
		mAlertDbHelper = new AlertDbAdapter(this);
		mAlertDbHelper.open();
		// initial value of mGotAlerts is false
		mGotAlerts = false;
		mNumAlertsDisplayed = 0;
		mNumAlertsTotal = 0;
		
		// create the ViewSwitcher in the current context, create the views and add them to the switcher 
		switcher = new ViewSwitcher(this);
		Button moreButton = (Button)View.inflate(this, R.layout.alert_list_more_button, null);
		View progressBar = View.inflate(this, R.layout.alert_list_progress_bar, null);
		switcher.addView(moreButton);
		switcher.addView(progressBar);
		
		// create the runnables for creating/expanding the alertsList
		additionalAlertsRunnable = new AlertsDisplayRunnable(this, ALERTS_ADDITIONAL);
		initialAlertsRunnable = new AlertsDisplayRunnable(this, ALERTS_INITIAL);
		
		// set up the click listeners...
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				switcher.showNext();
				Thread additionalAlertsThread = new Thread(additionalAlertsRunnable);
				additionalAlertsThread.start();
			}
		});

		if(savedInstanceState != null){
			// if we have a savedInstanceState, load the strings directly
			mRowId = savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID);
			mAlertSeverity = savedInstanceState.getString("mAlertSeverity");
			mSearchTerm = savedInstanceState.getString("mSearchTerm");
			mBeginningDatetime = savedInstanceState.getString("mBeginningDatetime");
			mEndingDatetime = savedInstanceState.getString("mEndingDatetime");
			mGotAlerts = savedInstanceState.getBoolean("mGotAlerts");
			mNumAlertsDisplayed = savedInstanceState.getLong("mNumAlertsDisplayed");
			mNumAlertsTotal = savedInstanceState.getLong("mNumAlertsTotal");
		} else {
			Bundle extras = getIntent().getExtras();
			if(extras != null){
				// if we have an intent, construct the strings from chosen fields.  add 1 to months
				mRowId = extras.getLong(ServerDbAdapter.KEY_ROWID);
				mAlertSeverity = extras.getString("mSpinnerText");
				mSearchTerm = extras.getString("mSearchTermText");
				mBeginningDatetime = extras.getInt("mStartYear") != 0 ? String.format("%04d", extras.getInt("mStartYear")) + "-" + String.format("%02d", extras.getInt("mStartMonth") + 1) + "-" + String.format("%02d", extras.getInt("mStartDay")) + "%20" + String.format("%02d", extras.getInt("mStartHour")) + ":" + String.format("%02d", extras.getInt("mStartMinute")) : null;
				mEndingDatetime = extras.getInt("mEndYear") != 0 ? String.format("%04d", extras.getInt("mEndYear")) + "-" + String.format("%02d", extras.getInt("mEndMonth") + 1) + "-" + String.format("%02d", extras.getInt("mEndDay")) + "%20" + String.format("%02d", extras.getInt("mEndHour")) + ":" + String.format("%02d", extras.getInt("mEndMinute")) : null;
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

		if(!mGotAlerts){
			// Display the progress dialog first
			pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);
			Thread thread = new Thread(initialAlertsRunnable);
			thread.start();
		} else {
	    	fillData();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save not only server row, but all the search strings
		if(!mGotAlerts)
			pd.dismiss();
		outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
		outState.putString("mAlertSeverity", mAlertSeverity);
		outState.putString("mSearchTerm", mSearchTerm);
		outState.putString("mBeginningDatetime", mBeginningDatetime);
		outState.putString("mEndingDatetime", mEndingDatetime);
		outState.putBoolean("mGotAlerts", mGotAlerts);
		outState.putLong("mNumAlertsDisplayed", mNumAlertsDisplayed);
		outState.putLong("mNumAlertsTotal", mNumAlertsTotal);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode){
			/*
			 * The ServerHashDialog activity has finished.  if the cert is rejected, finish this activity.
			 * If accepted, try connecting to the server all over again.
			 */
        	case ACTIVITY_HASH_DIALOG_INITIAL:
        		switch(resultCode){
        			case CERT_REJECTED:
        				finish();
        			break;
        			case CERT_ACCEPTED:
        				mGotAlerts = false;
        				pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);
        				Bundle extras = intent.getExtras();
        				mDbHelper.updateSeverHashes(mRowId, extras.getString("MD5"), extras.getString("SHA1"));
        				Thread thread = new Thread(initialAlertsRunnable);
        				thread.start();
        			break;
        		}
        	break;
        	case ACTIVITY_HASH_DIALOG_ADDITIONAL:
        		switch(resultCode){
	    			case CERT_REJECTED:
	    			break;
	    			case CERT_ACCEPTED:
	    				Bundle extras = intent.getExtras();
	    				mDbHelper.updateSeverHashes(mRowId, extras.getString("MD5"), extras.getString("SHA1"));
	    				switcher.showNext();
	    				Thread thread = new Thread(additionalAlertsRunnable);
	    				thread.start();
	    			break;
        		}
        	break;
        }
    }

	private void fillData() {
		// get alerts from the alerts database, display them
		Cursor alertsCursor = mAlertDbHelper.fetchAll();
		startManagingCursor(alertsCursor);
		if(alertsCursor.moveToFirst()){
			do {
				HashMap<String,String> item = new HashMap<String,String>();
				switch(alertsCursor.getInt(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_SIG_PRIORITY))){
					case 1:
						item.put("icon", Integer.toString(R.drawable.low));
					break;
					case 2:
						item.put("icon", Integer.toString(R.drawable.warn));
					break;
					case 3:
						item.put("icon", Integer.toString(R.drawable.high));
					break;
				}
				item.put("sig_name",alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_SIG_NAME)));
				long ipSrc = alertsCursor.getLong(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_IP_SRC));
				item.put("ip_src","Source IP: " + Integer.toString((int) ((ipSrc % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((ipSrc % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((ipSrc % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (ipSrc % 256)));
				long ipDst = alertsCursor.getLong(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_IP_DST));
				item.put("ip_dst","Destination IP: " + Integer.toString((int) ((ipDst % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((ipDst % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((ipDst % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (ipDst % 256)));
				Timestamp timestamp = Timestamp.valueOf(alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_TIMESTAMP)));
				item.put("timestamp_date",yearMonthDayFormat.format((Date) timestamp));
				item.put("timestamp_time",hourMinuteSecondFormat.format((Date) timestamp));
				list.add(item);
			} while(alertsCursor.moveToNext());	
		}
    	setContentView(R.layout.alert_list);
    	// keep track of the number of displayed alerts
    	mNumAlertsDisplayed = list.size();
		//add the ViewSwitcher to the footer if there are more alerts than those displayed
    	if(mNumAlertsTotal > mNumAlertsDisplayed)
    		getListView().addFooterView(switcher);
		alertListAdapter = new SimpleAdapter(this, list, R.layout.alert_row, new String[] {"icon", "sig_name", "ip_src", "ip_dst", "timestamp_date", "timestamp_time"}, new int[] {R.id.alert_row_icon, R.id.alert_row_sig_name_text, R.id.alert_row_ip_src_text, R.id.alert_row_ip_dst_text, R.id.alert_row_date_text, R.id.alert_row_time_text});
		setListAdapter(alertListAdapter);
    }

	private void fillDataFromAlertList(LinkedList<AlertListXMLElement> alertList) {
    	// iterate through the list of alerts, preparing a set of properties, send them to the SimpleAdapter
		ListIterator<AlertListXMLElement> itr = alertList.listIterator();
		while(itr.hasNext()){
			AlertListXMLElement thisAlertListXMLElement = (AlertListXMLElement) itr.next();
			HashMap<String,String> item = new HashMap<String,String>();
			switch(thisAlertListXMLElement.sigPriority){
				case 1:
					item.put("icon", Integer.toString(R.drawable.low));
				break;
				case 2:
					item.put("icon", Integer.toString(R.drawable.warn));
				break;
				case 3:
					item.put("icon", Integer.toString(R.drawable.high));
				break;
			}
			item.put("sig_name",thisAlertListXMLElement.sigName);
			item.put("ip_src","Source IP: " + Integer.toString((int) ((thisAlertListXMLElement.ipSrc % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ipSrc % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ipSrc % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (thisAlertListXMLElement.ipSrc % 256)));
			item.put("ip_dst","Destination IP: " + Integer.toString((int) ((thisAlertListXMLElement.ipDst % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ipDst % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ipDst % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (thisAlertListXMLElement.ipDst % 256)));
			item.put("timestamp_date",yearMonthDayFormat.format((Date) thisAlertListXMLElement.timestamp));
			item.put("timestamp_time",hourMinuteSecondFormat.format((Date) thisAlertListXMLElement.timestamp));
			list.add(item);
		}
		alertListAdapter.notifyDataSetChanged();
		// keep track of the number of displayed alerts
    	mNumAlertsDisplayed = list.size();
		//add the ViewSwitcher to the footer if there are more alerts than those displayed
    	if(mNumAlertsTotal <= mNumAlertsDisplayed)
    		getListView().removeFooterView(switcher);
    	//alertListAdapter.notifyDataSetChanged();
    }
}
