package com.legind.swinedroid;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;

import org.xml.sax.SAXException;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.SimpleAdapter;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.swinedroid.xml.AlertListXMLElement;
import com.legind.swinedroid.xml.AlertListXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;

public class AlertList extends ListActivity implements Runnable{
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
	private final String LOG_TAG = "com.legind.swinedroid.AlertList";
	private ErrorMessageHandler mEMH;
	private final int DOCUMENT_VALID = 0;
	private final int IO_ERROR = 1;
	private final int XML_ERROR = 2;
	private final int SERVER_ERROR = 3;
	private ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	private static final SimpleDateFormat yearMonthDayFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat hourMinuteSecondFormat = new SimpleDateFormat("HH:mm:ss");
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		mAlertListXMLHandler = new AlertListXMLHandler();
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();

		if(savedInstanceState != null){
			mRowId = savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID);
			mAlertSeverity = savedInstanceState.getString("mAlertSeverity");
			mSearchTerm = savedInstanceState.getString("mSearchTerm");
			mBeginningDatetime = savedInstanceState.getString("mBeginningDatetime");
			mEndingDatetime = savedInstanceState.getString("mEndingDatetime");
		} else {
			Bundle extras = getIntent().getExtras();
			if(extras != null){
				mRowId = extras.getLong(ServerDbAdapter.KEY_ROWID);
				mAlertSeverity = extras.getString("mSpinnerText");
				mSearchTerm = extras.getString("mSearchTermText");
				mBeginningDatetime = extras.getInt("mStartYear") != 0 ? String.format("%04d", extras.getInt("mStartYear")) + "-" + String.format("%02d", extras.getInt("mStartMonth") + 1) + "-" + String.format("%02d", extras.getInt("mStartDay")) + "%20" + String.format("%02d", extras.getInt("mStartHour")) + ":" + String.format("%02d", extras.getInt("mStartMinute")) : null;
				mEndingDatetime = extras.getInt("mEndYear") != 0 ? String.format("%04d", extras.getInt("mEndYear")) + "-" + String.format("%02d", extras.getInt("mEndMonth") + 1) + "-" + String.format("%02d", extras.getInt("mEndDay")) + "%20" + String.format("%02d", extras.getInt("mEndHour")) + ":" + String.format("%02d", extras.getInt("mEndMinute")) : null;
			}
		}

		if (mRowId != null) {
			Cursor server = mDbHelper.fetchServer(mRowId);
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

		// Display the progress dialog first
		pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);

		// Display all errors on the Swinedroid ListActivity
		mEMH = new ErrorMessageHandler(ServerView.LA,
				findViewById(R.id.server_edit_error_layout_root));
		
		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
		outState.putString("mAlertSeverity", mAlertSeverity);
		outState.putString("mSearchTerm", mSearchTerm);
		outState.putString("mBeginningDatetime", mBeginningDatetime);
		outState.putString("mEndingDatetime", mEndingDatetime);
	}
    
	public void run() {
		try {
			String extraArgs = "alert_severity=" + mAlertSeverity + "&search_term=" + mSearchTerm + (mBeginningDatetime != null ? "&beginning_datetime=" + mBeginningDatetime : "") + (mEndingDatetime != null ? "&ending_datetime=" + mEndingDatetime : "");
			Log.w("extraargs",extraArgs);
			mAlertListXMLHandler.createElement(this, mHostText, mPortInt, mUsernameText, mPasswordText, "alerts", extraArgs);
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
		handler.sendEmptyMessage(DOCUMENT_VALID);
	}

	// Catch and display any errors sent to the handler, otherwise populate all statistics fields
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
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
				case DOCUMENT_VALID:
					fillData();
				break;
			}
			if(message.what != DOCUMENT_VALID){
				mDbHelper.close();
				finish();
			}

		}
	};

	private void fillData() {
    	setContentView(R.layout.alert_list);
		ListIterator<AlertListXMLElement> itr = mAlertListXMLHandler.alert_list.listIterator();
		while(itr.hasNext()){
			AlertListXMLElement thisAlertListXMLElement = (AlertListXMLElement) itr.next();
			HashMap<String,String> item = new HashMap<String,String>();
			switch(thisAlertListXMLElement.sig_priority){
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
			item.put("sig_name",thisAlertListXMLElement.sig_name);
			item.put("ip_src","Source IP: " + Integer.toString((int) ((thisAlertListXMLElement.ip_src % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ip_src % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ip_src % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (thisAlertListXMLElement.ip_src % 256)));
			item.put("ip_dst","Destination IP: " + Integer.toString((int) ((thisAlertListXMLElement.ip_dst % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ip_dst % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((thisAlertListXMLElement.ip_dst % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (thisAlertListXMLElement.ip_dst % 256)));
			item.put("timestamp_date",yearMonthDayFormat.format((Date) thisAlertListXMLElement.timestamp));
			item.put("timestamp_time",hourMinuteSecondFormat.format((Date) thisAlertListXMLElement.timestamp));
			list.add(item);	
		}
		setListAdapter(new SimpleAdapter(this, list, R.layout.alert_row, new String[] {"icon", "sig_name", "ip_src", "ip_dst", "timestamp_date", "timestamp_time"}, new int[] {R.id.alert_row_icon, R.id.alert_row_sig_name_text, R.id.alert_row_ip_src_text, R.id.alert_row_ip_dst_text, R.id.alert_row_date_text, R.id.alert_row_time_text}));
    }

}
