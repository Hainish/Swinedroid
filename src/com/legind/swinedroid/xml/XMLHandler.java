package com.legind.swinedroid.xml;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.security.KeyManagementException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.legind.web.WebTransport.WebTransport;
import com.legind.web.WebTransport.WebTransportConnection;
import com.legind.web.WebTransport.WebTransportException;



public class XMLHandler extends DefaultHandler{
	private boolean inError = false;
	private String errorString = null;
	private WebTransportConnection webtransportconnection;
	private String mHost;
	private int mPort;
	
	public void startElement(String uri, String name, String qName, Attributes atts){
		if(name.trim().equals("error"))
			inError = true;
	}

	public void endElement(String uri, String name, String qName){
		if(name.trim().equals("error"))
			inError = false;
	}

	public void characters(char ch[], int start, int length){
		String chars = (new String(ch).substring(start, start + length));
		if(inError)
			errorString = chars;
	}
	
	public void openWebTransportConnection(String host, int port) throws IOException{
		try{
			webtransportconnection = new WebTransport("https://" + host + ":" + Integer.toString(port) + "/").getConnection();
			webtransportconnection.open();
		} catch(MalformedURLException e){
			Log.e("Swinedroid",e.toString());
		} catch (WebTransportException e){
			Log.e("Swinedroid",e.toString());
		} catch (KeyManagementException e){
			Log.e("Swinedroid",e.toString());
		}
	}
	
	public void createElement(String username, String password, String call) throws IOException, SAXException, XMLHandlerException{
		createElement(username, password, call, "");
	}
	
	public void createElement(String username, String password, String call, String extra_parameters) throws IOException, SAXException, XMLHandlerException{
		try{
			String[] webrequest = {
				"GET /?username=" + username + "&password=" + password + "&call=" + call + (extra_parameters != "" ? "&" + extra_parameters : "") + " HTTP/1.1",
				"Host: " + mHost + ":" + Integer.toString(mPort), "User-Agent: Swinedroid","Keep-Alive: 300","Connection: keep-alive"};
			webtransportconnection.sendRequest(webrequest);
			webtransportconnection.handleHeaders();
			webtransportconnection.handleDocument();
			String xmlString = webtransportconnection.getLastDocument();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.parse(new InputSource(new StringReader(xmlString)));
			if(errorString != null){
				 throw new XMLHandlerException(errorString);
			}
		} catch (ParserConfigurationException e){
			Log.e("Swinedroid",e.toString());
		}
	}
	
	public WebTransportConnection getWebTransportConnection(){
		return webtransportconnection;
	}
}