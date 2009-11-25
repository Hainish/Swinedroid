package com.legind.swinedroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ServerEdit extends Activity {
	private ServerDbAdapter mDbHelper;

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
		setContentView(R.layout.server_edit);

		mHostText = (EditText) findViewById(R.id.host);
		mPortText = (EditText) findViewById(R.id.port);
		mUsernameText = (EditText) findViewById(R.id.username);
		mPasswordText = (EditText) findViewById(R.id.password);

		Button confirmButton = (Button) findViewById(R.id.confirm);

		mRowId = savedInstanceState != null ? savedInstanceState
				.getLong(ServerDbAdapter.KEY_ROWID) : null;
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ServerDbAdapter.KEY_ROWID)
					: null;
		}

		populateFields();

		final Pattern hostPattern = Pattern.compile("^((?:[\\w-]+\\.)*\\w[\\w-]{0,66})\\.([a-z]{2,6}(?:\\.[a-z]{2})?)$");
		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				String errorString = "";
				
				Matcher hostMatcher = hostPattern.matcher(mHostText.getText().toString());
				if(!hostMatcher.matches())
					errorString += "Invalid host specification.  Please make sure you entered a valid host.";
				int port = Integer.parseInt(mPortText.getText().toString());
				if(port < 1 || port > 65535)
					errorString += (errorString == "" ? "" : "\n\n") + "Invalid port specification.  Valid range is 1-65535.";
				if(errorString != ""){
					errorMessage(errorString);
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
			Cursor server = mDbHelper.fetchServer(mRowId);
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
		outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
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

	private void errorMessage(String message) {
		final Builder builder;
		Dialog alertDialog;

		OnClickListener okListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				return;

			}
		};
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.server_edit_error,
		                               (ViewGroup) findViewById(R.id.server_edit_error_layout_root));

		TextView text = (TextView) layout.findViewById(R.id.server_edit_error_text);
		text.setText(message);

		ImageView image = (ImageView) layout.findViewById(R.id.server_edit_error_icon);
		image.setImageResource(R.drawable.icon);

		builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setPositiveButton("Ok", okListener);
		alertDialog = builder.create();
		alertDialog.show();

	}
}
