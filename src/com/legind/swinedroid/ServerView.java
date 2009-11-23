package com.legind.swinedroid;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

public class ServerView extends Activity {
	private ServerDbAdapter mDbHelper;
	private TextView mSometextText;
	private Long mRowId;
	private XMLHandler mXMLHandler;
	private int mPortInt;
	private String mHostText;
	private String mUsernameText;
	private String mPasswordText;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	mXMLHandler = new XMLHandler();
        mDbHelper = new ServerDbAdapter(this);
        mDbHelper.open();
        setContentView(R.layout.server_view);
        
        mSometextText = (TextView) findViewById(R.id.sometext);
        
        mRowId = savedInstanceState != null ? savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID) : null;
        if(mRowId == null){
        	Bundle extras = getIntent().getExtras();
        	mRowId = extras != null ? extras.getLong(ServerDbAdapter.KEY_ROWID) : null;
        }

    	if(mRowId != null){
    		Cursor server = mDbHelper.fetchServer(mRowId);
    		startManagingCursor(server);
    		mHostText = server.getString(server.getColumnIndexOrThrow(ServerDbAdapter.KEY_HOST));
    		mPortInt = server.getInt(server.getColumnIndexOrThrow(ServerDbAdapter.KEY_PORT));
    		mUsernameText = server.getString(server.getColumnIndexOrThrow(ServerDbAdapter.KEY_USERNAME));
    		mPasswordText = server.getString(server.getColumnIndexOrThrow(ServerDbAdapter.KEY_PASSWORD));
    	}
		mXMLHandler.createElement(this, mHostText, mPortInt, mUsernameText, mPasswordText);
		mSometextText.setText(mXMLHandler.currentElement.something);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState){
    	super.onSaveInstanceState(outState);
    	outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    }
}
