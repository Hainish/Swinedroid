package com.legind.swinedroid.xml;

import org.xml.sax.Attributes;

public class OverviewXMLHandler extends XMLHandler{
	private boolean inSomething = false;
	public Element currentElement = new Element();
	
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts){
		super.startElement(uri, name, qName, atts);
		if(name.trim().equals("something"))
			inSomething = true;
	}

	@Override
	public void endElement(String uri, String name, String qName){
		super.endElement(uri, name, qName);
		if(name.trim().equals("something"))
			inSomething = false;
	}
	
	@Override
	public void characters(char ch[], int start, int length){
		super.characters(ch, start, length);
		String chars = (new String(ch).substring(start, start + length));
		if(inSomething)
			currentElement.something = chars;
	}
}