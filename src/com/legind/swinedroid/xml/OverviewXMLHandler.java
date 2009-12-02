package com.legind.swinedroid.xml;

import org.xml.sax.Attributes;

public class OverviewXMLHandler extends XMLHandler{
	private boolean in_all_time = false;
	private boolean in_last_72 = false;
	private boolean in_last_24 = false;
	private boolean in_high = false;
	private boolean in_medium = false;
	private boolean in_low = false;
	public OverviewXMLElement current_element = new OverviewXMLElement();
	
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts){
		super.startElement(uri, name, qName, atts);
		if(name.trim().equals("all_time")){
			in_all_time = true;
		}
		if(name.trim().equals("last_72")){
			in_last_72 = true;
		}
		if(name.trim().equals("last_24")){
			in_last_24 = true;
		}
		if(name.trim().equals("high")){
			in_high = true;
		}
		if(name.trim().equals("medium")){
			in_medium = true;
		}
		if(name.trim().equals("low")){
			in_low = true;
		}
	}

	@Override
	public void endElement(String uri, String name, String qName){
		super.endElement(uri, name, qName);
		if(name.trim().equals("all_time")){
			in_all_time = false;
		}
		if(name.trim().equals("last_72")){
			in_last_72 = false;
		}
		if(name.trim().equals("last_24")){
			in_last_24 = false;
		}
		if(name.trim().equals("high")){
			in_high = false;
		}
		if(name.trim().equals("medium")){
			in_medium = false;
		}
		if(name.trim().equals("low")){
			in_low = false;
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length){
		super.characters(ch, start, length);
		String chars = (new String(ch).substring(start, start + length));
		if(in_all_time && in_high)
			current_element.all_time_high = Integer.parseInt(chars);
		if(in_all_time && in_medium)
			current_element.all_time_medium = Integer.parseInt(chars);
		if(in_all_time && in_low)
			current_element.all_time_low = Integer.parseInt(chars);
		if(in_last_72 && in_high)
			current_element.last_72_high = Integer.parseInt(chars);
		if(in_last_72 && in_medium)
			current_element.last_72_medium = Integer.parseInt(chars);
		if(in_last_72 && in_low)
			current_element.last_72_low = Integer.parseInt(chars);
		if(in_last_24 && in_high)
			current_element.last_24_high = Integer.parseInt(chars);
		if(in_last_24 && in_medium)
			current_element.last_24_medium = Integer.parseInt(chars);
		if(in_last_24 && in_low)
			current_element.last_24_low = Integer.parseInt(chars);
	}
}