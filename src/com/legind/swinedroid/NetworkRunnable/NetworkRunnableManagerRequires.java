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

public interface NetworkRunnableManagerRequires {
	ComponentName startService(Intent service);
	void unbindService(ServiceConnection conn);
	boolean bindService (Intent service, ServiceConnection conn, int flags);
	Long getRowId();
	Context getContext();
	void finish();
	void onBoundRequestSet();
	void onDocumentValidReturned(int networkRunnableId);
	void onCertificateInspectVerified(int networkRunnableId) throws IOException, SAXException, XMLHandlerException, WebTransportException;
	void onHandleMessageBegin(int networkRunnableId);
	void onCertErrorBegin(int networkRunnableId);
	void callHashDialog(int networkRunnableId, Intent i);
	ErrorMessageHandler getEMH();
	OnCancelListener getCancelListener();
}