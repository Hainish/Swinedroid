package com.legind.ssl.SSLHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import android.util.Log;

import com.legind.ssl.TrustManagerFactory.TrustManagerFactory;
import com.legind.ssl.TrustManagerFactory.TrustManagerFactory.CustomX509TrustManager;

public class SSLHandler {
	private String mHost;
	private int mPort;
	private BufferedInputStream mIn;
	private OutputStream mOut;
	private SSLSocket mSocket;
	private CustomX509TrustManager[] trustManagerArray;
	private X509Certificate mServerCertificate;
	
	public SSLHandler(String host, int port){
        mHost = host;
        mPort = port;
	}
	
	public void open() throws IOException{
		try{
	        SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
	        SSLContext sslContext = SSLContext.getInstance("TLS");
	        trustManagerArray = new CustomX509TrustManager[]{TrustManagerFactory.getCustomTrustManager(mHost)};
	        sslContext.init(null, trustManagerArray, new SecureRandom());
	        mSocket = (SSLSocket) sslContext.getSocketFactory().createSocket();
	        mSocket.connect(socketAddress, 10000);
	        mSocket.startHandshake();
	        mServerCertificate = trustManagerArray[0].getChildCert();
	        // RFC 1047
            mSocket.setSoTimeout(300000);
            mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
            mOut = mSocket.getOutputStream();
		} catch (NoSuchAlgorithmException e) {
			Log.e("Swinedroid",e.toString());
		} catch (KeyManagementException e) {
			Log.e("Swinedroid",e.toString());
		}
	}

    public void close() {
        try {
            mIn.close();
        } catch (Exception e) {

        }
        try {
            mOut.close();
        } catch (Exception e) {

        }
        try {
            mSocket.close();
        } catch (Exception e) {

        }
        mIn = null;
        mOut = null;
        mSocket = null;
    }

    public String readLine() throws IOException {
        StringBuffer sb = new StringBuffer();
        int d;
        while ((d = mIn.read()) != -1) {
            if (((char)d) == '\r') {
                continue;
            } else if (((char)d) == '\n') {
                break;
            } else {
                sb.append((char)d);
            }
        }
        String ret = sb.toString();
        
        return ret;
    }

    public void writeLine(String s) throws IOException {
        mOut.write(s.getBytes());
        mOut.write('\r');
        mOut.write('\n');
        mOut.flush();
    }

    private void checkLine(String line) throws Exception
    {
	if (line.length() < 1)
	{
	   throw new Exception("SMTP response is 0 length");
	}
        char c = line.charAt(0);
        if ((c == '4') || (c == '5')) {
            throw new Exception(line);
        }
    }

    public List<String> executeSimpleCommand(String command) throws IOException, Exception {
        List<String> results = new ArrayList<String>();
        if (command != null) {
            writeLine(command);
        }
        
        boolean cont = false;
        do
        {
            String line = readLine();
            checkLine(line);
            if (line.length() > 4)
            {
                results.add(line.substring(4));
                if (line.charAt(3) == '-')
                {
                    cont = true;
                }
                else
                {
                    cont = false;
                }
            }
        } while (cont);
        return results;

    }
    
    public X509Certificate getServerCertificate(){
    	return mServerCertificate;
    }
    
    public BufferedInputStream getInputStream(){
    	return mIn;
    }
    
    public OutputStream getOutputStream(){
    	return mOut;
    }
}
