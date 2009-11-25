package com.legind.web.WebTransport;

import java.net.MalformedURLException;
import java.net.URL;

public class WebTransport{
	private String rHost;
	private int rPort;
	private boolean rSsl;
	/* INFO: Don't allow changing socket specifications if rLocked is true */
	private boolean rLocked;

	/** Initialize the WebTransport object
	 * @param urlString the url string for the WebTransport
	 * @throws WebTransportException - If the protocol is not http or https
	 * @throws MalformedURLException - If the URL is malformed. */
	public WebTransport(String urlString) throws WebTransportException, MalformedURLException{
		URL url = new URL(urlString);
		InitializeWebTransport(url);
	}

	/** Initialize the WebTransport object
	 * @param url the url object for the WebTransport
	 * @throws WebTransportException - If the protocol is not http or https */
	public WebTransport(URL url) throws WebTransportException{
		InitializeWebTransport(url);
	}

	/** Helper method for the WebTransport constructor */
	private void InitializeWebTransport(URL url) throws WebTransportException{
		if(url.getProtocol().equalsIgnoreCase("http")){
			rSsl = false;
		} else if(url.getProtocol().toUpperCase().equalsIgnoreCase("https")) {
			rSsl = true;
		} else {
			throw new WebTransportException("Invalid protocol type: " + url.getProtocol());
		}
		rHost = url.getHost();
		rPort = url.getPort();
	}
	
	/** Create a new WebTransportConnection and return it
	 * @return WebTransportConnection */
	public WebTransportConnection getConnection(){
		WebTransportConnection webtransportconnection = new WebTransportConnection(this);
		return webtransportconnection;
	}
	
	public boolean getLocked(){
		return rLocked;
	}
	
	protected void setLocked(boolean locked){
		rLocked = locked;
	}
	
	public String getHost(){
		return rHost;
	}
	
	public void setHost(String host){
		if(!rLocked)
			rHost = host;
	}
	
	public int getPort(){
		return rPort;
	}
	
	public void setPort(int port){
		if(!rLocked)
			rPort = port;
	}
	
	public boolean getSsl(){
		return rSsl;
	}
	
	public void setSsl(boolean ssl){
		if(!rLocked)
			rSsl = ssl;
	}
}