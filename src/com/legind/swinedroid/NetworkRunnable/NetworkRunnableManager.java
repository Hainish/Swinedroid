package com.legind.swinedroid.NetworkRunnable;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.legind.sqlite.ServerDbAdapter;
import com.legind.swinedroid.RequestService.Request;


public class NetworkRunnableManager{
	private final NetworkRunnableBindRequires parent;
	private ServerDbAdapter mDbHelper;
	private Request mBoundRequest;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
		    mBoundRequest = ((Request.RequestBinder)service).getService();
		    parent.onBoundRequestSet();
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mBoundRequest = null;
		}
	};
    
	public void startRequestService() {
        Intent newIntent = new Intent(parent.getContext(), Request.class);
        newIntent.putExtra(Request.ROW_ID_TAG, parent.getRowId());
        parent.startService(newIntent);
        parent.bindService(newIntent, mConnection, 0);
	}

	public NetworkRunnableManager(NetworkRunnableBindRequires parent){
		this.parent = parent;
		mDbHelper = new ServerDbAdapter(parent.getContext());
		mDbHelper.open();
	}
	
	public void close(){
	mDbHelper.close();
	if(mBoundRequest != null)
		parent.unbindService(mConnection);
	}
	
	public Request getBoundRequest(){
		return mBoundRequest;
	}
	
	public ServerDbAdapter getDbHelper(){
		return mDbHelper;
	}
	
}