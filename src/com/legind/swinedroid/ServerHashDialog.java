package com.legind.swinedroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class ServerHashDialog extends Activity {
	private TextView mMessageText;
	private TextView mMd5Text;
	private TextView mSha1Text;
	private Button mAcceptButton;
	private Button mRejectButton;
	private final int CERT_REJECTED = 0;
	private final int CERT_ACCEPTED = 1;
	private final String MSG_NEW_STRING = "Authenticity of host cannot be established.  Please review the following fingerprints to authenticate host.";
	private final String MSG_INVALID_STRING = "This hosts certificate information differs from the previously accepted certificate.  This may indicate an attack on your session is taking place.  Please review the following fingerprints carefully before accepting the server authenticity.";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		// get rid of title, also set the layout to fill parent.  this doesn't function properly in the layout XML
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.server_hash_dialog);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
    	mMessageText = (TextView) findViewById(R.id.server_hash_dialog_message);
    	mMd5Text = (TextView) findViewById(R.id.md5_hash);
    	mSha1Text = (TextView) findViewById(R.id.sha1_hash);
    	mAcceptButton = (Button) findViewById(R.id.server_hash_dialog_accept);
    	mRejectButton = (Button) findViewById(R.id.server_hash_dialog_reject);
    	
    	/*
    	 * If the accept button is clicked, send the sha1 and md5 hashes back
    	 * to the parent activity.
    	 */
		mAcceptButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent();
				i.putExtra("MD5", mMd5Text.getText().toString());
				i.putExtra("SHA1", mSha1Text.getText().toString());
				setResult(CERT_ACCEPTED, i);
				finish();
			}
		});
		
		mRejectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResult(CERT_REJECTED);
				finish();
			}
		});
		
		if(savedInstanceState != null){
			// if we have a savedInstanceState, load the strings directly
			mMessageText.setText(savedInstanceState.getString("mMessageText"));
			mMd5Text.setText(savedInstanceState.getString("mMd5Text"));
			mSha1Text.setText(savedInstanceState.getString("mSha1Text"));
		} else {
			Bundle extras = getIntent().getExtras();
			if(extras != null){
				// if we have an intent, construct the strings from intent
				if(extras.getBoolean("CERT_INVALID") == true){
					mMessageText.setText(MSG_INVALID_STRING);
				} else {
					mMessageText.setText(MSG_NEW_STRING);
				}
				mMd5Text.setText(extras.getString("MD5"));
				mSha1Text.setText(extras.getString("SHA1"));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("mMessageText", mMessageText.getText().toString());
		outState.putString("mMd5Text", mMd5Text.getText().toString());
		outState.putString("mSha1Text", mSha1Text.getText().toString());
	}
}