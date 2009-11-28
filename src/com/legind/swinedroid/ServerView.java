package com.legind.swinedroid;

import java.io.IOException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.legind.Dialogs.ErrorMessageHandler.ErrorMessageHandler;

public class ServerView extends Activity {
	private ServerDbAdapter mDbHelper;
	private TextView mSometextText;
	private Long mRowId;
	private XMLHandler mXMLHandler;
	private int mPortInt;
	private String mHostText;
	private String mUsernameText;
	private String mPasswordText;
	private ErrorMessageHandler mEMH;
	private final String LOG_TAG = "com.legind.swinedroid.ServerView";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mXMLHandler = new XMLHandler();
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();
		setContentView(R.layout.server_view);

		mEMH = new ErrorMessageHandler(Swinedroid.LA,
				findViewById(R.id.server_edit_error_layout_root));

		mSometextText = (TextView) findViewById(R.id.sometext);

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
		try {
			mXMLHandler.createElement(this, mHostText, mPortInt, mUsernameText,
					mPasswordText);
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
			mEMH.DisplayErrorMessage("Could not connect to server.  Please ensure that your settings are correct and try again later.");
			mDbHelper.close();
			finish();
		} catch (SAXException e) {
			Log.e(LOG_TAG, e.toString());
			mEMH.DisplayErrorMessage("Server responded with an invalid XML document.  Please try again later.");
			mDbHelper.close();
			finish();
		}
		mSometextText.setText(mXMLHandler.currentElement.something);
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
}
