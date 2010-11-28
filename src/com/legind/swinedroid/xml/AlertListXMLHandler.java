package com.legind.swinedroid.xml;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.util.Log;

import com.legind.swinedroid.RequestService.Request;
import com.legind.web.WebTransport.WebTransportException;

public class AlertListXMLHandler extends XMLHandler{
	private boolean inNumAlerts = false;
	private boolean inAlert = false;
	private boolean inSid = false;
	private boolean inCid = false;
	private boolean inIpSrc = false;
	private boolean inIpDst = false;
	private boolean inSigPriority = false;
	private boolean inSigName = false;
	private boolean inTimestamp = false;
	public long numAlerts;
	public LinkedList <AlertListXMLElement> alertList;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String LOG_TAG = "com.legind.swinedroid.xml.AlertListXMLHandler";
	
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts){
		super.startElement(uri, name, qName, atts);
		if(name.trim().equals("num_alerts")){
			super.clearStringBuilder();
			inNumAlerts = true;
		}
		if(name.trim().equals("alert")){
			inAlert = true;
			alertList.add(new AlertListXMLElement());
		}
		if(name.trim().equals("sid")){
			super.clearStringBuilder();
			inSid = true;
		}
		if(name.trim().equals("cid")){
			super.clearStringBuilder();
			inCid = true;
		}
		if(name.trim().equals("ip_src")){
			super.clearStringBuilder();
			inIpSrc = true;
		}
		if(name.trim().equals("ip_dst")){
			super.clearStringBuilder();
			inIpDst = true;
		}
		if(name.trim().equals("sig_priority")){
			super.clearStringBuilder();
			inSigPriority = true;
		}
		if(name.trim().equals("sig_name")){
			super.clearStringBuilder();
			inSigName = true;
		}
		if(name.trim().equals("timestamp")){
			super.clearStringBuilder();
			inTimestamp = true;
		}
	}

	@Override
	public void endElement(String uri, String name, String qName){
		handleString();
		super.endElement(uri, name, qName);
		if(name.trim().equals("num_alerts")){
			inNumAlerts = false;
		}
		if(name.trim().equals("alert")){
			inAlert = false;
		}
		if(name.trim().equals("sid")){
			inSid = false;
		}
		if(name.trim().equals("cid")){
			inCid = false;
		}
		if(name.trim().equals("ip_src")){
			inIpSrc = false;
		}
		if(name.trim().equals("ip_dst")){
			inIpDst = false;
		}
		if(name.trim().equals("sig_priority")){
			inSigPriority = false;
		}
		if(name.trim().equals("sig_name")){
			inSigName = false;
		}
		if(name.trim().equals("timestamp")){
			inTimestamp = false;
		}
	}
	
	public void handleString(){
		try{
			if(inNumAlerts)
				numAlerts = Long.parseLong(super.getStringBuilder().toString());
			if(inAlert && inSid)
				alertList.getLast().sid = Long.parseLong(super.getStringBuilder().toString());
			if(inAlert && inCid)
				alertList.getLast().cid = Long.parseLong(super.getStringBuilder().toString());
			if(inAlert && inIpSrc)
				alertList.getLast().ipSrc = InetAddress.getByName(super.getStringBuilder().toString());
			if(inAlert && inIpDst)
				alertList.getLast().ipDst = InetAddress.getByName(super.getStringBuilder().toString());
			if(inAlert && inSigPriority)
				alertList.getLast().sigPriority = Byte.parseByte(super.getStringBuilder().toString());
			if(inAlert && inSigName){
				alertList.getLast().sigName = super.getStringBuilder().toString();
			}
			if(inAlert && inTimestamp){
				Date parsed_date = dateFormat.parse(super.getStringBuilder().toString());
				alertList.getLast().timestamp = new Timestamp(parsed_date.getTime());
			}
		} catch (UnknownHostException e) {
			Log.w(LOG_TAG, e.toString());
		} catch (ParseException e) {
			Log.w(LOG_TAG, e.toString());
		}
	}
	
	@Override
	public void createElement(Request request, String call, String extra_parameters) throws IOException, SAXException, XMLHandlerException, WebTransportException{
		alertList = new LinkedList<AlertListXMLElement>();
		super.createElement(request, call, extra_parameters);
	}
}