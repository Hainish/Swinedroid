package com.legind.swinedroid;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.SimpleAdapter;

public class AlertList extends ListActivity{
	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.server_list);
    	fillData();
    	registerForContextMenu(getListView());
	}

    private void fillData() {
		HashMap<String,String> item = new HashMap<String,String>();
		item.put("sig_name","blah blah alert threat [emegingthreats]");
		item.put("ip_src","Source IP: 192.168.1.1");
		item.put("ip_dst","Destination IP: 192.168.1.2");
		item.put("timestamp_date","2012-10-10");
		item.put("timestamp_time","10:11:10");
		list.add(item);
		item.put("sig_name","blah blah alert threat [emegingthreats]");
		item.put("ip_src","Source IP: 192.168.1.1");
		item.put("ip_dst","Destination IP: 192.168.1.2");
		item.put("timestamp_date","2012-10-10");
		item.put("timestamp_time","10:11:10");
		list.add(item);
		setListAdapter(new SimpleAdapter(this, list, R.layout.alert_row, new String[] {"sig_name", "ip_src", "ip_dst", "timestamp_date", "timestamp_time"}, new int[] {R.id.alert_row_sig_name_text, R.id.alert_row_ip_src_text, R.id.alert_row_ip_dst_text, R.id.alert_row_date_text, R.id.alert_row_time_text}));
    }
}
