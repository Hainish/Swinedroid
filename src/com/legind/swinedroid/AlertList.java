package com.legind.swinedroid;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.ViewSwitcher;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.AlertDbAdapter;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.swinedroid.NetworkRunnable.NetworkRunnable;
import com.legind.swinedroid.NetworkRunnable.NetworkRunnableBindRequires;
import com.legind.swinedroid.NetworkRunnable.NetworkRunnableManager;
import com.legind.swinedroid.NetworkRunnable.NetworkRunnableUniqueRequires;
import com.legind.swinedroid.RequestService.Request;
import com.legind.swinedroid.xml.AlertListXMLElement;
import com.legind.swinedroid.xml.AlertListXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;
import com.legind.web.WebTransport.WebTransportException;

public class AlertList extends ListActivity implements NetworkRunnableBindRequires{
	private Long mRowId;
	private String mAlertSeverity;
	private String mSearchTerm;
	private String mBeginningDatetime;
	private String mEndingDatetime;
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
    private final int ACTIVITY_VIEW=2;
	private final int CERT_REJECTED = 0;
	private final int CERT_ACCEPTED = 1;
	private AlertsDisplay additionalAlertsRunnable;
	private AlertsDisplay initialAlertsRunnable;
	private ArrayList<AlertListTracker> AlertListTracker = new ArrayList<AlertListTracker>();
	private ErrorMessageHandler mEMH;
    public static Activity LA = null;
    private NetworkRunnableManager mNetRunMan;
	
	public class AlertListTracker extends Object {
		public long sid;
		public long cid;
		public String sig_name;
		public InetAddress ip_src;
		public InetAddress ip_dst;
		public String timestamp_date;
		public String timestamp_time;
		public byte sig_priority;
	}
	
	private class AlertsDisplay implements NetworkRunnableUniqueRequires{
		private int mFromCode;
	    public NetworkRunnable mNetRun;
		

		/**
		 * Constructor for AlertsDisplayRunnable.  Runnable becomes context and caller-aware 
		 * 
		 * @param ctx the AlertList context from which it is called
		 * @param fromCode how this runnable is called, either loading the inital alerts or additional alerts
		 */
		public AlertsDisplay(int fromCode, NetworkRunnableBindRequires nrbr, ServerDbAdapter dbHelper, Request boundRequest){
			mFromCode = fromCode;
			mNetRun = new NetworkRunnable(this, nrbr, dbHelper, boundRequest);
		}

		public void callHashDialog(Intent i) {
			switch(mFromCode){
				case ALERTS_INITIAL:
					startActivityForResult(i, ACTIVITY_HASH_DIALOG_INITIAL);
				break;
				case ALERTS_ADDITIONAL:
					startActivityForResult(i, ACTIVITY_HASH_DIALOG_ADDITIONAL);
				break;
			}
		}

		public OnCancelListener getCancelListener() {
			return new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					switch(mFromCode){
						case ALERTS_INITIAL:
							finish();
						break;
					}
					return;
				}
			};
		}

		public ErrorMessageHandler getEMH() {
			return mEMH;
		}

		public void onCertErrorBegin() {
			mGotAlerts = true;
		}

		public void onCertificateInspectVerified() throws IOException, SAXException, XMLHandlerException, WebTransportException {
			String extraArgs = "alert_severity=" + mAlertSeverity + "&search_term=" + mSearchTerm + (mBeginningDatetime != null ? "&beginning_datetime=" + mBeginningDatetime : "") + (mEndingDatetime != null ? "&ending_datetime=" + mEndingDatetime : "") + "&starting_at=" + String.valueOf(mNumAlertsDisplayed);
			mAlertListXMLHandler.createElement(mNetRun.getBoundRequest(), "alerts", extraArgs);
		}

		public void onDocumentValidReturned() {
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
		}

		public void onHandleMessageBegin() {
			switch(mFromCode){
				case ALERTS_INITIAL:
					pd.dismiss();
				break;
				case ALERTS_ADDITIONAL:
					switcher.showPrevious();
				break;
			}
		}
		
	}
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	LA = this;
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
		
		mEMH = new ErrorMessageHandler(AlertList.LA,
				findViewById(R.id.server_edit_error_layout_root));
		
		// create the ViewSwitcher in the current context, create the views and add them to the switcher 
		switcher = new ViewSwitcher(this);
		Button moreButton = (Button)View.inflate(this, R.layout.alert_list_more_button, null);
		View progressBar = View.inflate(this, R.layout.alert_list_progress_bar, null);
		switcher.addView(moreButton);
		switcher.addView(progressBar);
		
		// create the runnables for creating/expanding the alertsList
		mNetRunMan = new NetworkRunnableManager(this); 
		
		// set up the click listeners...
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				switcher.showNext();
				Thread additionalAlertsThread = new Thread(additionalAlertsRunnable.mNetRun);
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

		mNetRunMan.startRequestService();
	}
    
	@Override
	protected void onDestroy() {
		mAlertDbHelper.close();
		initialAlertsRunnable.mNetRun.close();
		additionalAlertsRunnable.mNetRun.close();
		super.onDestroy();
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
        				Thread thread = new Thread(initialAlertsRunnable.mNetRun);
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
        				initialAlertsRunnable.mNetRun.getBoundRequest().fetchServerHashes();
	    				switcher.showNext();
	    				Thread thread = new Thread(additionalAlertsRunnable.mNetRun);
	    				thread.start();
	    			break;
        		}
        	break;
        }
    }

	private void fillData() {
		try{
			// get alerts from the alerts database, display them
			Cursor alertsCursor = mAlertDbHelper.fetchAll();
			startManagingCursor(alertsCursor);
			if(alertsCursor.moveToFirst()){
				do {
					HashMap<String,String> item = new HashMap<String,String>();
					switch(alertsCursor.getShort(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_SIG_PRIORITY))){
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
					
					InetAddress ipSrc = InetAddress.getByAddress(alertsCursor.getBlob(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_IP_SRC)));
					item.put("ip_src","Source IP: " + ipSrc.getHostAddress());
					InetAddress ipDst = InetAddress.getByAddress(alertsCursor.getBlob(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_IP_DST)));
					item.put("ip_dst","Destination IP: " + ipDst.getHostAddress());
					Timestamp timestamp = Timestamp.valueOf(alertsCursor.getString(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_TIMESTAMP)));
					item.put("timestamp_date",yearMonthDayFormat.format((Date) timestamp));
					item.put("timestamp_time",hourMinuteSecondFormat.format((Date) timestamp));
					list.add(item);
					// Remember to add a tracker for the UIDs of this alert as well
					AlertListTracker thisTracker = new AlertListTracker();
					thisTracker.cid = alertsCursor.getLong(alertsCursor.getColumnIndex(AlertDbAdapter.KEY_CID));
					thisTracker.sid = alertsCursor.getLong(alertsCursor.getColumnIndex(AlertDbAdapter.KEY_SID));
					thisTracker.sig_name = alertsCursor.getString(alertsCursor.getColumnIndex(AlertDbAdapter.KEY_SIG_NAME));
					thisTracker.ip_src = InetAddress.getByAddress(alertsCursor.getBlob(alertsCursor.getColumnIndex(AlertDbAdapter.KEY_IP_SRC)));
					thisTracker.ip_dst = InetAddress.getByAddress(alertsCursor.getBlob(alertsCursor.getColumnIndex(AlertDbAdapter.KEY_IP_DST)));
					thisTracker.timestamp_date = yearMonthDayFormat.format((Date) timestamp);
					thisTracker.timestamp_time = hourMinuteSecondFormat.format((Date) timestamp);
					thisTracker.sig_priority = (byte) alertsCursor.getShort(alertsCursor.getColumnIndexOrThrow(AlertDbAdapter.KEY_SIG_PRIORITY));
					AlertListTracker.add(thisTracker);
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
	    	registerForContextMenu(getListView());
		} catch (UnknownHostException e) {
			Log.w(LOG_TAG,e.toString());
		}
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
			item.put("ip_src","Source IP: " + thisAlertListXMLElement.ipSrc.getHostAddress());
			item.put("ip_dst","Destination IP: " + thisAlertListXMLElement.ipDst.getHostAddress());
			item.put("timestamp_date",yearMonthDayFormat.format((Date) thisAlertListXMLElement.timestamp));
			item.put("timestamp_time",hourMinuteSecondFormat.format((Date) thisAlertListXMLElement.timestamp));
			list.add(item);
			// Remember to add a tracker for the UIDs of this alert as well
			AlertListTracker thisTracker = new AlertListTracker();
			thisTracker.cid = thisAlertListXMLElement.cid;
			thisTracker.sid = thisAlertListXMLElement.sid;
			thisTracker.sig_name = thisAlertListXMLElement.sigName;
			thisTracker.ip_src = thisAlertListXMLElement.ipSrc;
			thisTracker.ip_dst = thisAlertListXMLElement.ipDst;
			thisTracker.timestamp_date = yearMonthDayFormat.format((Date) thisAlertListXMLElement.timestamp);
			thisTracker.timestamp_time = hourMinuteSecondFormat.format((Date) thisAlertListXMLElement.timestamp);
			thisTracker.sig_priority = thisAlertListXMLElement.sigPriority;
			AlertListTracker.add(thisTracker);
		}
		alertListAdapter.notifyDataSetChanged();
		// keep track of the number of displayed alerts
    	mNumAlertsDisplayed = list.size();
		//add the ViewSwitcher to the footer if there are more alerts than those displayed
    	if(mNumAlertsTotal <= mNumAlertsDisplayed)
    		getListView().removeFooterView(switcher);
    	//alertListAdapter.notifyDataSetChanged();
    }
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        AlertListTracker tracker = AlertListTracker.get((int)id);
        Intent i = new Intent(this, AlertView.class);

    	i.putExtra(AlertDbAdapter.KEY_ROWID, mRowId);
        i.putExtra(AlertDbAdapter.KEY_CID, tracker.cid);
        i.putExtra(AlertDbAdapter.KEY_SID, tracker.sid);
        i.putExtra(AlertDbAdapter.KEY_SIG_NAME, tracker.sig_name);
        i.putExtra(AlertDbAdapter.KEY_IP_SRC, tracker.ip_src.getAddress());
        i.putExtra(AlertDbAdapter.KEY_IP_DST, tracker.ip_dst.getAddress());
        i.putExtra("date", tracker.timestamp_date);
        i.putExtra("time", tracker.timestamp_time);
        i.putExtra(AlertDbAdapter.KEY_SIG_PRIORITY, tracker.sig_priority);
        startActivityForResult(i, ACTIVITY_VIEW);
    }

	public Context getContext() {
		return this;
	}

	public Long getRowId() {
		return mRowId;
	}

	public void onBoundRequestSet() {
		additionalAlertsRunnable = new AlertsDisplay(ALERTS_ADDITIONAL, this, mNetRunMan.getDbHelper(), mNetRunMan.getBoundRequest());
		initialAlertsRunnable = new AlertsDisplay(ALERTS_INITIAL, this, mNetRunMan.getDbHelper(), mNetRunMan.getBoundRequest());
		if(!mGotAlerts){
			// Display the progress dialog first
			pd = ProgressDialog.show(AlertList.this, "", "Connecting. Please wait...", true);
			
			Thread thread = new Thread(initialAlertsRunnable.mNetRun);
			thread.start();
		} else {
	    	fillData();
		}
	}
}
