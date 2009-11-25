package com.legind.web.WebTransport;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.util.ArrayList;

import com.legind.ssl.SSLHandler.SSLHandler;

public class WebTransportConnection{
	private WebTransport parent;
	private SSLHandler sslhandler;
	private ArrayList<String> lastHeaders;
	private ArrayList<String> lastDocument;
	
	public WebTransportConnection(WebTransport webtransport){
		lastHeaders = new ArrayList<String>();
		lastDocument = new ArrayList<String>();
		parent = webtransport;
	}
	
	public void open() throws KeyManagementException, IOException{
		parent.setLocked(true);
		if(parent.getSsl()){
			sslhandler = new SSLHandler(parent.getHost(), parent.getPort());
			sslhandler.open();
		} else {
			/* TODO: Handle non-SSL connections as well */
		}
	}
	
	public void close(){
		parent.setLocked(false);
		if(parent.getSsl()){
			sslhandler.close();
		}
	}
    
	/**
	 * Send a request over the http(s) socket
	 * @param requestLines an array of strings, each a line of the request 
	 */
	public void sendRequest(String[] requestLines) throws IOException{
		try {
			if(parent.getSsl()){
				for(String requestLine : requestLines){
					sslhandler.writeLine(requestLine);
				}
				sslhandler.writeLine("");
			} else {
				/* TODO: Handle non-SSL connections as well */
			}
		} catch (IOException e){
			throw new IOException(e.toString());
		}
	}
	
	public void handleResponse() throws IOException{
		handleHeaders();
		handleDocument();
	}
	
	/** Sort the response into headers, document */
	public void handleHeaders() throws IOException{
		String line;
		if(parent.getSsl()){
			do{
				line = sslhandler.readLine();
				if(line.trim() != "")
					lastHeaders.add(line.trim());
			} while(line.trim() != "");
		} else {
			/* TODO: Handle non-SSL connections as well */
		}
	}
	
	public void handleDocument() throws IOException{
		String line;
		while((line = sslhandler.readLine()) != null){
			lastDocument.add(line);
		}
	}
	
	public ArrayList<String> getLastHeaders(){
		return lastHeaders;
	}
	
	public ArrayList<String> getLastDocument(){
		return lastDocument;
	}
	
	public BufferedInputStream getInputStream(){
		if(parent.getSsl()){
			return sslhandler.getInputStream();
		} else {
			/* TODO: Handle non-SSL connections as well */
		}
		return null;
	}
	
	public OutputStream getOutputStream(){
		if(parent.getSsl()){
			return sslhandler.getOutputStream();
		} else {
			/* TODO: Handle non-SSL connections as well */
		}
		return null;
	}
	
}