package com.legind.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.legind.swinedroid.R;

public class ErrorMessageHandler extends MessageHandler{

	public ErrorMessageHandler(Context ctx, View v) {
		super(ctx, v);
	}

	public ErrorMessageHandler(View v){
		super(v);
	}
	
	public void DisplayErrorMessage(String message, OnCancelListener cancelListener){
		final Builder builder;
		Dialog alertDialog;

		OnClickListener okListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				return;
			}
		};
		LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.server_edit_error,
				((ViewGroup) mV));

		TextView text = (TextView) layout.findViewById(R.id.server_edit_error_text);
		text.setText(message);

		ImageView image = (ImageView) layout.findViewById(R.id.server_edit_error_icon);
		image.setImageResource(R.drawable.icon);

		builder = new AlertDialog.Builder(mCtx);
		builder.setView(layout);
		builder.setPositiveButton("Ok", okListener);
		builder.setOnCancelListener(cancelListener);
		alertDialog = builder.create();
		alertDialog.show();
	}

	public void DisplayErrorMessage(String message) {
		OnCancelListener cancelListener = new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				return;
			}
		};
		DisplayErrorMessage(message, cancelListener);
	}
}