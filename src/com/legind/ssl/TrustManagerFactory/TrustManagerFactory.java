package com.legind.ssl.TrustManagerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public final class TrustManagerFactory {
	private static final String LOG_TAG = "com.legind.ssl.TrustManagerFactory";

	private static X509TrustManager defaultTrustManager;
	private static X509TrustManager unsecureTrustManager;
	private static CustomX509TrustManager customTrustManager;
	private static X509TrustManager localTrustManager;
   
	private static X509Certificate[] lastCertChain = null;

	private static File keyStoreFile;
	private static KeyStore keyStore;


	public static class CustomX509TrustManager implements X509TrustManager {
		private X509Certificate childCert;
		
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			childCert = chain[0];
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
		
		public X509Certificate getChildCert(){
			return childCert;
		}
	}

	private static class SimpleX509TrustManager implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	private static class SecureX509TrustManager implements X509TrustManager {
		private String mHost;
		private static SecureX509TrustManager me;

		private SecureX509TrustManager() {
		}

		public static X509TrustManager getInstance(String host) {
			if (me == null) {
				me = new SecureX509TrustManager();
			}
			me.mHost = host;
			return me;
		}

		public void setHost(String host){
			mHost = host;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			defaultTrustManager.checkClientTrusted(chain, authType);
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			TrustManagerFactory.setLastCertChain(chain);
			try {
				defaultTrustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException e) {
				localTrustManager.checkServerTrusted(new X509Certificate[] {chain[0]}, authType);
			}
		}

		public X509Certificate[] getAcceptedIssuers() {
			return defaultTrustManager.getAcceptedIssuers();
		}
		
	}

	static {
		try {
			javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
			
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			try {
				keyStore.load(null, "".toCharArray());
			} catch (IOException e) {
				Log.e(LOG_TAG, "KeyStore IOException while initializing TrustManagerFactory ", e);
				keyStore = null;
			} catch (CertificateException e) {
				Log.e(LOG_TAG, "KeyStore CertificateException while initializing TrustManagerFactory ", e);
				keyStore = null;
			}
			tmf.init(keyStore);
			TrustManager[] tms = tmf.getTrustManagers();
			if (tms != null) {
				for (TrustManager tm : tms) {
					if (tm instanceof X509TrustManager) {
						localTrustManager = (X509TrustManager)tm;
						break;
					}
				}
			}
			tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
			tmf.init((KeyStore)null);
			tms = tmf.getTrustManagers();
			if (tms != null) {
				for (TrustManager tm : tms) {
					if (tm instanceof X509TrustManager) {
						defaultTrustManager = (X509TrustManager) tm;
						break;
					}
				}
			}

		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
		} catch (KeyStoreException e) {
			Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
		}
		unsecureTrustManager = new SimpleX509TrustManager();
		customTrustManager = new CustomX509TrustManager();
	}

	private TrustManagerFactory() {
	}

	public static X509TrustManager get(String host, boolean secure) {
		return secure ? SecureX509TrustManager.getInstance(host) : unsecureTrustManager;
	}
	
	public static CustomX509TrustManager getCustomTrustManager(String host){
		return customTrustManager;
	}

	public static KeyStore getKeyStore() {
		return keyStore;
	}

	public static void setLastCertChain(X509Certificate[] chain) {
		lastCertChain = chain;
	}
	
	public static X509Certificate[] getLastCertChain() {
		return lastCertChain;
	}

	public static void addCertificateChain(String alias, X509Certificate[] chain) throws CertificateException {
		try {
			javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509");
			for (int i = 0; i < chain.length; i++){
				keyStore.setCertificateEntry
				(chain[i].getSubjectDN().toString(), chain[i]);
			}

			tmf.init(keyStore);
			TrustManager[] tms = tmf.getTrustManagers();
			if (tms != null) {
				for (TrustManager tm : tms) {
					if (tm instanceof X509TrustManager) {
						localTrustManager = (X509TrustManager) tm;
						break;
					}
				}
			}
			java.io.FileOutputStream keyStoreStream;
			try {
				keyStoreStream = new java.io.FileOutputStream(keyStoreFile);
				keyStore.store(keyStoreStream, "".toCharArray());
				keyStoreStream.close();
			} catch (FileNotFoundException e) {
				throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
			} catch (CertificateException e) {
				throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
			} catch (IOException e) {
				throw new CertificateException("Unable to write KeyStore: " + e.getMessage());
			}

		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG_TAG, "Unable to get X509 Trust Manager ", e);
		} catch (KeyStoreException e) {
			Log.e(LOG_TAG, "Key Store exception while initializing TrustManagerFactory ", e);
		}
	}
}
