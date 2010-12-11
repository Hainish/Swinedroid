package com.legind.swinedroid.NetworkRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import org.xml.sax.SAXException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.os.IBinder;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.swinedroid.RequestService.Request;
import com.legind.swinedroid.xml.XMLHandlerException;
import com.legind.web.WebTransport.WebTransportException;


public class NetworkRunnableManager implements NetworkRunnableRequires{
	ArrayList<UniqueNetworkRunnable> uniqueNetworkRunnableList;
	private final NetworkRunnableManagerRequires parent;
	private ServerDbAdapter mDbHelper;
	private Request mBoundRequest;
	private NetworkRunnableManager NRM = null;

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
	
	private class UniqueNetworkRunnable extends NetworkRunnable{
		private int networkRunnableId;
		public UniqueNetworkRunnable() {
			super(NRM, mDbHelper, mBoundRequest);
		}
				
	}

	public NetworkRunnableManager(NetworkRunnableManagerRequires parent){
		NRM = this;
		this.parent = parent;
		mDbHelper = new ServerDbAdapter(parent.getContext());
		mDbHelper.open();
		uniqueNetworkRunnableList = new ArrayList<UniqueNetworkRunnable>();
	}
	
	public void addNetworkRunnable(int networkRunnableId){
		UniqueNetworkRunnable thisNetworkRunnable = new UniqueNetworkRunnable();
		thisNetworkRunnable.networkRunnableId = networkRunnableId;
		uniqueNetworkRunnableList.add(new UniqueNetworkRunnable());
	}
	
	public UniqueNetworkRunnable getNetworkRunnable(int networkRunnableId){
		UniqueNetworkRunnable thisNRW;
		ListIterator<UniqueNetworkRunnable> li = uniqueNetworkRunnableList.listIterator();
		while(li.hasNext()){
			thisNRW = li.next();
			if(thisNRW.networkRunnableId == networkRunnableId)
				return thisNRW;
		}
		return null;
	}
	
	public void close(){
	mDbHelper.close();
	if(mBoundRequest != null)
		parent.unbindService(mConnection);
	}

	public boolean bindService(Intent service, ServiceConnection conn, int flags) {
		return parent.bindService(service, conn, flags);
	}

	public void callHashDialog(Intent i) {
		parent.callHashDialog(networkRunnableId, i);
	}

	public void finish() {
		parent.finish();
	}

	public OnCancelListener getCancelListener() {
		return parent.getCancelListener();
	}

	public Context getContext() {
		return parent.getContext();
	}

	public ErrorMessageHandler getEMH() {
		return parent.getEMH();
	}

	public Long getRowId() {
		return parent.getRowId();
	}

	public void onBoundRequestSet() {
		parent.onBoundRequestSet();
	}

	public void onCertErrorBegin() {
		parent.onCertErrorBegin(networkRunnableId);
	}

	public void onCertificateInspectVerified() throws IOException, SAXException, XMLHandlerException, WebTransportException {
		parent.onCertificateInspectVerified(networkRunnableId);
	}

	public void onDocumentValidReturned() {
		parent.onDocumentValidReturned(networkRunnableId);
	}

	public void onHandleMessageBegin() {
		parent.onHandleMessageBegin(networkRunnableId);
	}

	public ComponentName startService(Intent service) {
		return parent.startService(service);
	}

	public void unbindService(ServiceConnection conn) {
		parent.unbindService(conn);
	}
	
}