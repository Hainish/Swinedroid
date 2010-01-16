package com.legind.swinedroid.xml;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;

public class AlertListXMLHandler extends XMLHandler{
	private boolean in_alert = false;
	private boolean in_sid = false;
	private boolean in_cid = false;
	private boolean in_ip_src = false;
	private boolean in_ip_dst = false;
	private boolean in_sig_priority = false;
	private boolean in_sig_name = false;
	private boolean in_timestamp = false;
	public LinkedList <AlertListXMLElement> alert_list;
	private SimpleDateFormat date_format;
	private static final String TAG = "AlertListXMLHandler";
	
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts){
		super.startElement(uri, name, qName, atts);
		if(name.trim().equals("alert")){
			in_alert = true;
			alert_list.add(new AlertListXMLElement());
		}
		if(name.trim().equals("sid")){
			in_sid = true;
		}
		if(name.trim().equals("cid")){
			in_cid = true;
		}
		if(name.trim().equals("ip_src")){
			in_ip_src = true;
		}
		if(name.trim().equals("ip_dst")){
			in_ip_dst = true;
		}
		if(name.trim().equals("sig_priority")){
			in_sig_priority = true;
		}
		if(name.trim().equals("sig_name")){
			in_sig_name = true;
		}
		if(name.trim().equals("timestamp")){
			in_timestamp = true;
		}
	}

	@Override
	public void endElement(String uri, String name, String qName){
		super.endElement(uri, name, qName);
		if(name.trim().equals("alert")){
			in_alert = false;
		}
		if(name.trim().equals("sid")){
			in_sid = false;
		}
		if(name.trim().equals("cid")){
			in_cid = false;
		}
		if(name.trim().equals("ip_src")){
			in_ip_src = false;
		}
		if(name.trim().equals("ip_dst")){
			in_ip_dst = false;
		}
		if(name.trim().equals("sig_priority")){
			in_sig_priority = false;
		}
		if(name.trim().equals("sig_name")){
			in_sig_name = false;
		}
		if(name.trim().equals("timestamp")){
			in_timestamp = false;
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length){
		super.characters(ch, start, length);
		String chars = (new String(ch).substring(start, start + length));
		if(in_alert && in_sid)
			alert_list.getLast().sid = Long.parseLong(chars);
		if(in_alert && in_cid)
			alert_list.getLast().cid = Long.parseLong(chars);
		if(in_alert && in_ip_src)
			alert_list.getLast().ip_src = Long.parseLong(chars);
		if(in_alert && in_ip_dst)
			alert_list.getLast().ip_dst = Long.parseLong(chars);
		if(in_alert && in_sig_priority)
			alert_list.getLast().sig_priority = Byte.parseByte(chars);
		if(in_alert && in_sig_name){
			alert_list.getLast().sig_name = chars;
		}
		if(in_alert && in_timestamp){;
			try {
				Date parsed_date = date_format.parse(chars);
				alert_list.getLast().timestamp = new Timestamp(parsed_date.getTime());
			} catch (ParseException e) {
				Log.w(TAG, e.toString());
			}
		}
	}
	
	@Override
	public void createElement(Context ctx, String host, int port, String username, String password, String call, String extra_parameters) throws IOException, SAXException, XMLHandlerException{
		date_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		alert_list = new LinkedList<AlertListXMLElement>();
		super.createElement(ctx, host, port, username, password, call, extra_parameters);
	}
}