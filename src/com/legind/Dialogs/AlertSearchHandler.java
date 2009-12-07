package com.legind.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.legind.swinedroid.R;

public class AlertSearchHandler extends MessageHandler{

	public AlertSearchHandler(Context ctx, View v) {
		super(ctx, v);
	}

	public AlertSearchHandler(View v){
		super(v);
	}

	public void DisplaySearchDialog() {
		final Builder builder;
		Dialog alertDialog;

		OnClickListener okListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				return;
			}
		};
		
		LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.alert_search,
				((ViewGroup) mV));

	    Spinner s = (Spinner) layout.findViewById(R.id.alert_level_spinner);
	    ArrayAdapter adapter = ArrayAdapter.createFromResource(mCtx, R.array.alert_levels, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    s.setAdapter(adapter);

		builder = new AlertDialog.Builder(mCtx);
		builder.setView(layout);
		builder.setPositiveButton("Ok", okListener);
		alertDialog = builder.create();
		alertDialog.show();

	}
}