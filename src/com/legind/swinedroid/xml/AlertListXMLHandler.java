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
			inNumAlerts = true;
		}
		if(name.trim().equals("alert")){
			inAlert = true;
			alertList.add(new AlertListXMLElement());
		}
		if(name.trim().equals("sid")){
			inSid = true;
		}
		if(name.trim().equals("cid")){
			inCid = true;
		}
		if(name.trim().equals("ip_src")){
			inIpSrc = true;
		}
		if(name.trim().equals("ip_dst")){
			inIpDst = true;
		}
		if(name.trim().equals("sig_priority")){
			inSigPriority = true;
		}
		if(name.trim().equals("sig_name")){
			inSigName = true;
		}
		if(name.trim().equals("timestamp")){
			inTimestamp = true;
		}
	}

	@Override
	public void endElement(String uri, String name, String qName){
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
	
	@Override
	public void characters(char ch[], int start, int length){
		try{
			super.characters(ch, start, length);
			String chars = (new String(ch).substring(start, start + length));
			if(inNumAlerts)
				numAlerts = Long.parseLong(chars);
			if(inAlert && inSid)
				alertList.getLast().sid = Long.parseLong(chars);
			if(inAlert && inCid)
				alertList.getLast().cid = Long.parseLong(chars);
			if(inAlert && inIpSrc)
				alertList.getLast().ipSrc = InetAddress.getByName(chars);
			if(inAlert && inIpDst)
				alertList.getLast().ipDst = InetAddress.getByName(chars);
			if(inAlert && inSigPriority)
				alertList.getLast().sigPriority = Byte.parseByte(chars);
			if(inAlert && inSigName){
				alertList.getLast().sigName = chars;
			}
			if(inAlert && inTimestamp){
				Date parsed_date = dateFormat.parse(chars);
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