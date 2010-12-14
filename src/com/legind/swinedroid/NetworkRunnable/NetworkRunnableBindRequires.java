package com.legind.swinedroid.NetworkRunnable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public interface NetworkRunnableBindRequires {
	ComponentName startService(Intent service);
	void unbindService(ServiceConnection conn);
	boolean bindService (Intent service, ServiceConnection conn, int flags);
	Long getRowId();
	Context getContext();
	void onBoundRequestSet();
	void finish();
}