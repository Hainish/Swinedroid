package com.legind.ssl.CertificateInspect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class CertificateInspect{
	private final String LOG_TAG = "CertificateInspect";
	private final byte[] encodingTable = {
		(byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
		(byte)'8', (byte)'9', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f'
	};
	private X509Certificate certificate;
	
	public CertificateInspect(X509Certificate certificateLocal){
		certificate = certificateLocal;
	}
	
	private int encode(byte[] data, int off, int length, OutputStream out) throws IOException{
		for (int i = off; i < (off + length); i++){
			int    v = data[i] & 0xff;
			out.write(encodingTable[(v >>> 4)]);
			out.write(encodingTable[v & 0xf]);
		}
		return length * 2;
	}
	
	/*
	 * Generate a string with the hash of the certificate.
	 * 
	 * @param hashingAlgorithm the hashing function
	 * @return a string of the hash, or null if an exception has occurred
	 */
	public String generateFingerprint(String hashingAlgorithm){
		StringBuffer buf = new StringBuffer();
		try{
			MessageDigest md = null;
			md = MessageDigest.getInstance(hashingAlgorithm);
			byte[] digest = md.digest(certificate.getEncoded());
			ByteArrayOutputStream out = new ByteArrayOutputStream(digest.length*2);
			encode(digest, 0, digest.length, out);
			String all = null;
			all = new String(out.toByteArray(), "US-ASCII").toUpperCase();
			Matcher matcher = Pattern.compile("..").matcher(all);
			while(matcher.find()) {
				if(buf.length() > 0) {
					buf.append(":");
				}
				buf.append(matcher.group());
			}
			return buf.toString();
		} catch (NoSuchAlgorithmException e){
			Log.e(LOG_TAG, e.toString());
		} catch (IOException e){
			Log.e(LOG_TAG, e.toString());
		} catch (CertificateEncodingException e){
			Log.e(LOG_TAG, e.toString());
		}
		return null;
	}
	
}