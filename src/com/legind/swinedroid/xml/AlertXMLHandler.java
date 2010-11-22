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
			inProtocol = true;
		}
		if(name.trim().equals("alert")){
			inAlert = true;
		}
		if(name.trim().equals("hostname")){
			inHostname = true;
		}
		if(name.trim().equals("interface")){
			inInterface = true;
		}
		if(name.trim().equals("payload")){
			inPayload = true;
		}
		if(name.trim().equals("sport")){
			inSport = true;
		}
		if(name.trim().equals("dport")){
			inDport = true;
		}
		if(name.trim().equals("type")){
			inType = true;
		}
		if(name.trim().equals("code")){
			inCode = true;
		}
	}

	@Override
	public void endElement(String uri, String name, String qName){
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
	
	@Override
	public void characters(char ch[], int start, int length){
		super.characters(ch, start, length);
		String chars = (new String(ch).substring(start, start + length));
		if(inAlert && inProtocol){
			if(chars.equals("icmp"))
				alert.protocol = AlertXMLHandler.PROTO_ICMP;
			if(chars.equals("tcp"))
				alert.protocol = AlertXMLHandler.PROTO_TCP;
			if(chars.equals("udp"))
				alert.protocol = AlertXMLHandler.PROTO_UDP;
		}
		if(inAlert && inHostname)
			alert.hostname = chars;
		if(inAlert && inInterface)
			alert.interface_name = chars;
		if(inAlert && inPayload)
			alert.payload = chars;
		if(inAlert && inSport)
			alert.sport = Integer.parseInt(chars);
		if(inAlert && inDport)
			alert.dport = Integer.parseInt(chars);
		if(inAlert && inType)
			alert.type = Byte.parseByte(chars);
		if(inAlert && inCode)
			alert.code = Byte.parseByte(chars);
	}
	
	@Override
	public void createElement(Request request, String call, String extra_parameters) throws IOException, SAXException, XMLHandlerException, WebTransportException{
		alert = new AlertXMLElement();
		super.createElement(request, call, extra_parameters);
	}
}