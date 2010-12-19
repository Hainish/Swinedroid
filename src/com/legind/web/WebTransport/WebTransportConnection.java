package com.legind.web.WebTransport;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import com.legind.ssl.SSLHandler.SSLHandler;

public class WebTransportConnection{
	private WebTransport parent;
	private SSLHandler sslhandler;
	private ArrayList<String> lastHeaders;
	private String lastDocument;
	private X509Certificate serverCertificate;
	
	public WebTransportConnection(WebTransport webtransport){
		lastDocument = new String();
		lastHeaders = new ArrayList<String>();
		parent = webtransport;
	}
	
	public void open() throws KeyManagementException, IOException{
		parent.setLocked(true);
		if(parent.getSsl()){
			sslhandler = new SSLHandler(parent.getHost(), parent.getPort());
			sslhandler.open();
			serverCertificate = sslhandler.getServerCertificate();
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
			}
		} catch (IOException e){
			throw new IOException(e.toString());
		}
	}
	
	public void handleResponse() throws IOException, WebTransportException{
		handleHeaders();
		handleDocument();
	}
	
	/** Sort the response into headers, document */
	public void handleHeaders() throws IOException, WebTransportException{
		String line;
		lastHeaders.clear();
		if(parent.getSsl()){
			do{
				line = sslhandler.readLine();
				if(line.trim().contentEquals("HTTP/1.1 408 Request Timeout"))
					throw new WebTransportException("Request Timeout");
				if(line.trim() != "")
					lastHeaders.add(line.trim());
			} while(line.trim() != "");
		}
	}
	
	public void handleDocument() throws IOException, WebTransportException{
		if(!lastHeaders.isEmpty()){
			for(String header : lastHeaders){
				if(header.contains("Content-Length: ")){
					// Pass the Content-Length so readBuffer knows when to stop reading 
					lastDocument = sslhandler.readBuffer(Integer.parseInt(header.replace("Content-Length: ", "")));
					break;
				}
			}
		} else {
			throw new WebTransportException("Empty Header");
		}
	}
	
	public ArrayList<String> getLastHeaders(){
		return lastHeaders;
	}
	
	public String getLastDocument(){
		return lastDocument;
	}
	
	public BufferedInputStream getInputStream(){
		if(parent.getSsl()){
			return sslhandler.getInputStream();
		}
		return null;
	}
	
	public OutputStream getOutputStream(){
		if(parent.getSsl()){
			return sslhandler.getOutputStream();
		}
		return null;
	}
	
	public X509Certificate getServerCertificate(){
		return serverCertificate;
	}
	
}