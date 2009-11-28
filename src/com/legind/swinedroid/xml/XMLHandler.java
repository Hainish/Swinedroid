package com.legind.swinedroid.xml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;

import com.legind.web.WebTransport.WebTransport;
import com.legind.web.WebTransport.WebTransportConnection;
import com.legind.web.WebTransport.WebTransportException;



public class XMLHandler extends DefaultHandler{
	public void createElement(Context ctx, String host, int port, String username, String password, String call) throws IOException, SAXException{
		try{
			//URL url = new URL("http://" + host + ":" + Integer.toString(port) + "/?username=" + username + "&password=" + password + "&call=drivel");
			WebTransportConnection webtransportconnection = new WebTransport("https://" + host + ":" + Integer.toString(port) + "/").getConnection();
			webtransportconnection.open();
			String[] webrequest = {
				"GET /?username=" + username + "&password=" + password + "&call=" + call + " HTTP/1.0",
				"User-Agent: Swinedroid"};
			webtransportconnection.sendRequest(webrequest);
			webtransportconnection.handleHeaders();
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.parse(new InputSource(webtransportconnection.getInputStream()));
		} catch(MalformedURLException e){
			Log.e("Swinedroid",e.toString());
		} catch (ParserConfigurationException e){
			Log.e("Swinedroid",e.toString());
		} catch (KeyManagementException e) {
			Log.e("Swinedroid",e.toString());
		} catch (WebTransportException e){
			Log.e("Swinedroid",e.toString());
		}
	}
}