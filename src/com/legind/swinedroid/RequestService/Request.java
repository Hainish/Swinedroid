package com.legind.swinedroid.RequestService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.ListIterator;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.legind.sqlite.ServerDbAdapter;
import com.legind.ssl.CertificateInspect.CertificateInspect;
import com.legind.web.WebTransport.WebTransport;
import com.legind.web.WebTransport.WebTransportException;

public class Request extends Service{
	private static final String LOG_TAG = "com.legind.swinedroid.RequestService";
	private ServerDbAdapter mDbHelper;
	public static final String ROW_ID_TAG = "row_id";
	private ArrayList <RequestElement> requestList;
	private RequestElement mCurrentRequestElement;
	private String mLastServerCertMD5;
	private String mLastServerCertSHA1;
	
	
	public class RequestBinder extends Binder{
		public Request getService(){
			return Request.this;
		}
	}
	
    @Override
    public void onCreate() {
		mDbHelper = new ServerDbAdapter(this);
		mDbHelper.open();
	}

    @Override
	public void onStart(Intent intent, int startId) {
		Long rowId = intent.getExtras().getLong(ROW_ID_TAG);
		
		Boolean foundRequestElement = false;
    	ListIterator<RequestElement> requestListIterator = requestList.listIterator();
    	while(requestListIterator.hasNext()){
    		mCurrentRequestElement = requestListIterator.next();
    		if(mCurrentRequestElement.mRowId == rowId){
    			foundRequestElement = true;
    			break;
    		}
    	}
    	if(!foundRequestElement){
    		requestListIterator.add(new RequestElement());
    		mCurrentRequestElement = requestListIterator.previous();
    		mCurrentRequestElement.mRowId = rowId;
    	}
		
		if (mCurrentRequestElement.mRowId != null) {
			/* TODO: Make it unnecessary to retrieve this information each onStart */
			Cursor server = mDbHelper.fetch(mCurrentRequestElement.mRowId);
			mCurrentRequestElement.mHostText = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_HOST));
			mCurrentRequestElement.mPortInt = server.getInt(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_PORT));
			mCurrentRequestElement.mUsernameText = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_USERNAME));
			mCurrentRequestElement.mPasswordText = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_PASSWORD));
			mCurrentRequestElement.mMD5 = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_MD5));
			mCurrentRequestElement.mSHA1 = server.getString(server
					.getColumnIndexOrThrow(ServerDbAdapter.KEY_SHA1));
		}
	
		
    }
    
	/* Creates a webtransportconnection if one does not already exist for the given RequestElement */
	public void openWebTransportConnection() throws IOException{
		if(!mCurrentRequestElement.webtransportconnection.isConnected()){
			try{
				mCurrentRequestElement.webtransportconnection = new WebTransport("https://" + mCurrentRequestElement.mHostText + ":" + Integer.toString(mCurrentRequestElement.mPortInt) + "/").getConnection();
				mCurrentRequestElement.webtransportconnection.open();
			} catch(MalformedURLException e){
				Log.e(LOG_TAG,e.toString());
			} catch (WebTransportException e){
				Log.e(LOG_TAG,e.toString());
			} catch (KeyManagementException e){
				Log.e(LOG_TAG,e.toString());
			}
		}
	}
	
	/* 
	 * Returns whether the md5 and sha1 hashes match those we have stored, and thus whether the certificate is valid 
	 * @return whether the certificate is valid
	 */
    public Boolean inspectCertificate(){
		CertificateInspect serverCertificateInspect = new CertificateInspect(mCurrentRequestElement.webtransportconnection.getServerCertificate());
		mLastServerCertMD5 = serverCertificateInspect.generateFingerprint("MD5");
		mLastServerCertSHA1 = serverCertificateInspect.generateFingerprint("SHA1"); 
		if(!mLastServerCertSHA1.equals(mCurrentRequestElement.mSHA1) || !mLastServerCertMD5.equals(mCurrentRequestElement.mMD5))
        	return false;
		return true;
    }
    
    /*
     * 
     */
    /*
    public void displayCertificateDialog(Context ctx){
    	ctx.
    	Intent i = new Intent(ctx, ServerHashDialog.class);
    	i.putExtra("SHA1", mLastServerCertSHA1);
    	i.putExtra("MD5", mLastServerCertMD5);
    	i.putExtra("CERT_INVALID", (mLastServerCertSHA1 == null && mLastServerCertMD5 == null ? false : true));
    	startActivity(i);
    	
    }*/
	
	/* Make a request, return the document response in string form
	 * @param call the call request to pass to the webtransportconnection
	 * @param extra_parameters a ampersand-seperated list of extra parameters for the call
	 */
	public String makeRequest(String call, String extra_parameters) throws IOException{
		String[] webrequest = {
			"GET /?username=" + mCurrentRequestElement.mUsernameText + "&password=" + mCurrentRequestElement.mPasswordText + "&call=" + call + (extra_parameters != "" ? "&" + extra_parameters : "") + " HTTP/1.1",
			"Host: " + mCurrentRequestElement.mHostText + ":" + Integer.toString(mCurrentRequestElement.mPortInt), "User-Agent: Swinedroid","Keep-Alive: 300","Connection: keep-alive"};
		mCurrentRequestElement.webtransportconnection.sendRequest(webrequest);
		mCurrentRequestElement.webtransportconnection.handleHeaders();
		mCurrentRequestElement.webtransportconnection.handleDocument();
		return mCurrentRequestElement.webtransportconnection.getLastDocument();
	}
    
    @Override
    public void onDestroy() {
		mDbHelper.close();
    }

	@Override
	public IBinder onBind(Intent intent) {
        return mBinder;
	}
	
	public final IBinder mBinder = new RequestBinder();
	
	public String getCurrentHost(){
		return mCurrentRequestElement.mHostText;
	}

	/* TODO: Remove getter md5 and sha1 methods, move ServerHashDialog handling to Request service */
	public String getCurrentMD5(){
		return mCurrentRequestElement.mMD5;
	}
	public String getCurrentSHA1(){
		return mCurrentRequestElement.mSHA1;
	}
}