package com.legind.swinedroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.ServerDbAdapter;

public class ServerEdit extends Activity {
	private ServerDbAdapter mDbHelper;
	private ErrorMessageHandler mEMH;

	private EditText mHostText;
	private EditText mPortText;
	private EditText mUsernameText;
	private EditText mPasswordText;
	private Long mRowId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();
		mEMH = new ErrorMessageHandler(this, findViewById(R.id.server_edit_error_layout_root));
		setContentView(R.layout.server_edit);

		mHostText = (EditText) findViewById(R.id.host);
		mPortText = (EditText) findViewById(R.id.port);
		mUsernameText = (EditText) findViewById(R.id.username);
		mPasswordText = (EditText) findViewById(R.id.password);

		Button confirmButton = (Button) findViewById(R.id.confirm);

		if(savedInstanceState != null){
			if(!savedInstanceState.getBoolean(ServerDbAdapter.KEY_ROWID + "_null")){
				mRowId = savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID);
				mHostText.setText(savedInstanceState.getString(ServerDbAdapter.KEY_HOST));
				mPortText.setText(savedInstanceState.getString(ServerDbAdapter.KEY_PORT));
				mUsernameText.setText(savedInstanceState.getString(ServerDbAdapter.KEY_USERNAME));
				mPasswordText.setText(savedInstanceState.getString(ServerDbAdapter.KEY_PASSWORD));
			} else {
				mRowId = null;
			}
		} else {
			mRowId = null;
		}
		
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			if(extras != null){
				mRowId = extras.getLong(ServerDbAdapter.KEY_ROWID);
				populateFields();
			} else {
				mRowId = null;
			}
		}

		final Pattern hostPattern = Pattern.compile("^((?:[\\w-]+\\.)*\\w[\\w-]{0,66})\\.([a-z]{2,6}(?:\\.[a-z]{2})?)$");
		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				String errorString = "";
				
				Matcher hostMatcher = hostPattern.matcher(mHostText.getText().toString());
				if(!hostMatcher.matches())
					errorString += "Invalid host specification.  Please make sure you entered a valid host.";
				int port = 0;
				if(mPortText.getText().toString().length() != 0)
					port = Integer.parseInt(mPortText.getText().toString());
				if(port < 1 || port > 65535)
					errorString += (errorString == "" ? "" : "\n\n") + "Invalid port specification.  Valid range is 1-65535.";
				if(errorString != ""){
					mEMH.DisplayErrorMessage(errorString);
				} else {
					saveState();
					setResult(RESULT_OK);
					finish();
				}
			}

		});
	}

	private void populateFields() {
		if (mRowId != null) {
			Cursor server = mDbHelper.fetch(mRowId);
			startManagingCursor(server);
			mHostText.setText(server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_HOST)));
			mPortText.setText(server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_PORT)));
			mUsernameText.setText(server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_USERNAME)));
			mPasswordText.setText(server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_PASSWORD)));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mRowId != null){
			outState.putBoolean(ServerDbAdapter.KEY_ROWID + "_null", false);
			outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
			outState.putString(ServerDbAdapter.KEY_HOST, mHostText.getText().toString());
			outState.putString(ServerDbAdapter.KEY_PORT, mPortText.getText().toString());
			outState.putString(ServerDbAdapter.KEY_USERNAME, mUsernameText.getText().toString());
			outState.putString(ServerDbAdapter.KEY_PASSWORD, mPasswordText.getText().toString());
		} else {
			outState.putBoolean(ServerDbAdapter.KEY_ROWID + "_null", true);
		}
	}

	private void saveState() {
		String host = mHostText.getText().toString();
		int port = Integer.parseInt(mPortText.getText().toString());
		String username = mUsernameText.getText().toString();
		String password = mPasswordText.getText().toString();

		if (mRowId == null) {
			long id = mDbHelper.createServer(host, port, username, password);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateServer(mRowId, host, port, username, password);
		}
	}

}
