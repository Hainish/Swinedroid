package com.legind.swinedroid.xml;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.legind.swinedroid.RequestService.Request;
import com.legind.web.WebTransport.WebTransportException;



public class XMLHandler extends DefaultHandler{
	private boolean inError = false;
	private String errorString = null;
	
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
	
	public void createElement(Request request, String call) throws IOException, SAXException, XMLHandlerException, WebTransportException{
		createElement(request, call, "");
	}
	
	public void createElement(Request request, String call, String extra_parameters) throws IOException, SAXException, XMLHandlerException, WebTransportException{
		try{
			String xmlString = request.makeRequest(call, extra_parameters);
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
}