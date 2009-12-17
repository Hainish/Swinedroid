package com.legind.Dialogs;

import android.app.Activity;
import android.content.Context;
import android.view.View;

class MessageHandler extends Activity{
	protected Context mCtx;
	protected View mV;
	
	public MessageHandler(Context ctx, View v){
		mCtx = ctx;
		mV = v;
	}
	
	public MessageHandler(View v){
		mV = v;
	}
	
	public void setContext(Context ctx){
		mCtx = ctx;
	}
}