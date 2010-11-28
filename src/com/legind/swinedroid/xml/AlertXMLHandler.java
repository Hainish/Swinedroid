package com.legind.swinedroid.xml;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.legind.swinedroid.RequestService.Request;
import com.legind.web.WebTransport.WebTransportException;

public class AlertXMLHandler extends XMLHandler{
	private boolean inProtocol = false;
	private boolean inAlert = false;
	private boolean inHostname = false;
	private boolean inInterface = false;
	private boolean inPayload = false;
	private boolean inSport = false;
	private boolean inDport = false;
	private boolean inType = false;
	private boolean inCode = false;
	public AlertXMLElement alert;
	public static final int PROTO_ICMP = 1;
	public static final int PROTO_TCP = 2;
	public static final int PROTO_UDP = 3;
	
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts){
		super.startElement(uri, name, qName, atts);
		if(name.trim().equals("protocol")){
			super.clearStringBuilder();
			inProtocol = true;
		}
		if(name.trim().equals("alert")){
			inAlert = true;
		}
		if(name.trim().equals("hostname")){
			super.clearStringBuilder();
			inHostname = true;
		}
		if(name.trim().equals("interface")){
			super.clearStringBuilder();
			inInterface = true;
		}
		if(name.trim().equals("payload")){
			super.clearStringBuilder();
			inPayload = true;
		}
		if(name.trim().equals("sport")){
			super.clearStringBuilder();
			inSport = true;
		}
		if(name.trim().equals("dport")){
			super.clearStringBuilder();
			inDport = true;
		}
		if(name.trim().equals("type")){
			super.clearStringBuilder();
			inType = true;
		}
		if(name.trim().equals("code")){
			super.clearStringBuilder();
			inCode = true;
		}
	}

	public void endElement(String uri, String name, String qName){
		handleString();
		super.endElement(uri, name, qName);
		if(name.trim().equals("protocol")){
			inProtocol = false;
		}
		if(name.trim().equals("alert")){
			inAlert = false;
		}
		if(name.trim().equals("hostname")){
			inHostname = false;
		}
		if(name.trim().equals("interface")){
			inInterface = false;
		}
		if(name.trim().equals("payload")){
			inPayload = false;
		}
		if(name.trim().equals("sport")){
			inSport = false;
		}
		if(name.trim().equals("dport")){
			inDport = false;
		}
		if(name.trim().equals("type")){
			inType = false;
		}
		if(name.trim().equals("code")){
			inCode = false;
		}
	}

	public void handleString(){
		if(inAlert && inProtocol){
			if(super.getStringBuilder().toString().equals("icmp"))
				alert.protocol = AlertXMLHandler.PROTO_ICMP;
			if(super.getStringBuilder().toString().equals("tcp"))
				alert.protocol = AlertXMLHandler.PROTO_TCP;
			if(super.getStringBuilder().toString().equals("udp"))
				alert.protocol = AlertXMLHandler.PROTO_UDP;
		}
		if(inAlert && inHostname)
			alert.hostname = super.getStringBuilder().toString();
		if(inAlert && inInterface)
			alert.interface_name = super.getStringBuilder().toString();
		if(inAlert && inPayload)
			alert.payload = super.getStringBuilder().toString();
		if(inAlert && inSport)
			alert.sport = Integer.parseInt(super.getStringBuilder().toString());
		if(inAlert && inDport)
			alert.dport = Integer.parseInt(super.getStringBuilder().toString());
		if(inAlert && inType)
			alert.type = Byte.parseByte(super.getStringBuilder().toString());
		if(inAlert && inCode)
			alert.code = Byte.parseByte(super.getStringBuilder().toString());
	}
	
	@Override
	public void createElement(Request request, String call, String extra_parameters) throws IOException, SAXException, XMLHandlerException, WebTransportException{
		alert = new AlertXMLElement();
		super.createElement(request, call, extra_parameters);
	}
}