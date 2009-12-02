package com.legind.swinedroid;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.legind.Dialogs.ErrorMessageHandler.ErrorMessageHandler;
import com.legind.swinedroid.xml.OverviewXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;

public class ServerView extends Activity implements Runnable {
	private ServerDbAdapter mDbHelper;
	private TextView mAllTimeHighText;
	private TextView mAllTimeMediumText;
	private TextView mAllTimeLowText;
	private TextView mAllTimeTotalText;
	private TextView mLast72HighText;
	private TextView mLast72MediumText;
	private TextView mLast72LowText;
	private TextView mLast72TotalText;
	private TextView mLast24HighText;
	private TextView mLast24MediumText;
	private TextView mLast24LowText;
	private TextView mLast24TotalText;
	private Long mRowId;
	private OverviewXMLHandler mOverviewXMLHandler;
	private int mPortInt;
	private String mHostText;
	private String mUsernameText;
	private String mPasswordText;
	private ErrorMessageHandler mEMH;
	private ProgressDialog pd;
	private final String LOG_TAG = "com.legind.swinedroid.ServerView";
	private final int DOCUMENT_VALID = 0;
	private final int IO_ERROR = 1;
	private final int XML_ERROR = 2;
	private final int SERVER_ERROR = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mOverviewXMLHandler = new OverviewXMLHandler();
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.server_view);

		pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);

		mEMH = new ErrorMessageHandler(Swinedroid.LA,
				findViewById(R.id.server_edit_error_layout_root));

		mAllTimeHighText = (TextView) findViewById(R.id.all_time_high);
		mAllTimeMediumText = (TextView) findViewById(R.id.all_time_med);
		mAllTimeLowText = (TextView) findViewById(R.id.all_time_low);
		mAllTimeTotalText = (TextView) findViewById(R.id.all_time_total);
		mLast72HighText = (TextView) findViewById(R.id.last_72_high);
		mLast72MediumText = (TextView) findViewById(R.id.last_72_med);
		mLast72LowText = (TextView) findViewById(R.id.last_72_low);
		mLast72TotalText = (TextView) findViewById(R.id.last_72_total);
		mLast24HighText = (TextView) findViewById(R.id.last_24_high);
		mLast24MediumText = (TextView) findViewById(R.id.last_24_med);
		mLast24LowText = (TextView) findViewById(R.id.last_24_low);
		mLast24TotalText = (TextView) findViewById(R.id.last_24_total);

		mRowId = savedInstanceState != null ? savedInstanceState
				.getLong(ServerDbAdapter.KEY_ROWID) : null;
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ServerDbAdapter.KEY_ROWID)
					: null;
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
		
		/*
		mSometextText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mEMH.setContext(ServerView.this);
				mEMH.DisplayErrorMessage("Could not connect to server.  Please ensure that your settings are correct and try again later.");
			}
		});*/

		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public void run() {
		try {
			mOverviewXMLHandler.createElement(this, mHostText, mPortInt, mUsernameText, mPasswordText, "overview");
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
					DecimalFormat df = new DecimalFormat();
					DecimalFormatSymbols dfs = new DecimalFormatSymbols();
					dfs.setGroupingSeparator(',');
					df.setDecimalFormatSymbols(dfs);
					mAllTimeHighText.setText(df.format(mOverviewXMLHandler.current_element.all_time_high));
					mAllTimeMediumText.setText(df.format(mOverviewXMLHandler.current_element.all_time_medium));
					mAllTimeLowText.setText(df.format(mOverviewXMLHandler.current_element.all_time_low));
					mAllTimeTotalText.setText(df.format(mOverviewXMLHandler.current_element.all_time_high + mOverviewXMLHandler.current_element.all_time_medium + mOverviewXMLHandler.current_element.all_time_low));
					mLast72HighText.setText(df.format(mOverviewXMLHandler.current_element.last_72_high));
					mLast72MediumText.setText(df.format(mOverviewXMLHandler.current_element.last_72_medium));
					mLast72LowText.setText(df.format(mOverviewXMLHandler.current_element.last_72_low));
					mLast72TotalText.setText(df.format(mOverviewXMLHandler.current_element.last_72_high + mOverviewXMLHandler.current_element.last_72_medium + mOverviewXMLHandler.current_element.last_72_low));
					mLast24HighText.setText(df.format(mOverviewXMLHandler.current_element.last_24_high));
					mLast24MediumText.setText(df.format(mOverviewXMLHandler.current_element.last_24_medium));
					mLast24LowText.setText(df.format(mOverviewXMLHandler.current_element.last_24_low));
					mLast24TotalText.setText(df.format(mOverviewXMLHandler.current_element.last_72_high + mOverviewXMLHandler.current_element.last_24_medium + mOverviewXMLHandler.current_element.last_24_low));
				break;
			}
			if(message.what != DOCUMENT_VALID){
				mDbHelper.close();
				finish();
			}

		}
	};
}
