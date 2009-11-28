package com.legind.swinedroid.xml;

import org.xml.sax.Attributes;

public class OverviewXMLHandler extends XMLHandler{

	private boolean inSomething = false;
	public Element currentElement = new Element();
	
	public void startElement(String uri, String name, String qName, Attributes atts){
		if(name.trim().equals("something"))
			inSomething = true;
	}

	public void endElement(String uri, String name, String qName){
		if(name.trim().equals("something"))
			inSomething = false;
	}
	
	public void characters(char ch[], int start, int length){
		String chars = (new String(ch).substring(start, start + length));
		if(inSomething)
			currentElement.something = chars;
		currentElement.something = new String(ch);
	}
}