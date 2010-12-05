package com.legind.swinedroid.NetworkRunnable;

import java.io.IOException;

import org.xml.sax.SAXException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.swinedroid.xml.XMLHandlerException;
import com.legind.web.WebTransport.WebTransportException;

public interface NetworkRunnableRequires {
	ComponentName startService(Intent service);
	void unbindService(ServiceConnection conn);
	boolean bindService (Intent service, ServiceConnection conn, int flags);
	Long getRowId();
	Context getContext();
	void finish();
	void onBoundRequestSet();
	void onDocumentValidReturned();
	void onCertificateInspectVerified() throws IOException, SAXException, XMLHandlerException, WebTransportException;
	void onHandleMessageBegin();
	void onCertErrorBegin();
	void callHashDialog(Intent i);
	ErrorMessageHandler getEMH();
	OnCancelListener getCancelListener();
}