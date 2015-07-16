package com.yangc.bridge.comm.handler.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.yangc.utils.Message;

public class SslContextFactory {

	private static final Logger logger = Logger.getLogger(SslContextFactory.class);

	private static final String KEYSTORE_PASSWORD = Message.getMessage("bridge.keystore");
	private static final String TRUSTSTORE_PASSWORD = Message.getMessage("bridge.truststore");

	private static final String PROTOCOL = "TLS";
	private static final SSLContext SSL_CONTEXT;

	static {
		SSLContext sslContext = null;

		String path = SslContextFactory.class.getResource("//").getFile();
		File keyStoreFile = new File(path + "server_keystore.jks"), trustStoreFile = new File(path + "server_truststore.jks");
		if (keyStoreFile.exists() && trustStoreFile.exists()) {
			logger.info("SslContextBuilder - keystore=" + keyStoreFile.getAbsolutePath() + ", truststore=" + trustStoreFile.getAbsolutePath());
			FileInputStream keyStoreStream = null, trustStoreStream = null;
			try {
				keyStoreStream = new FileInputStream(keyStoreFile);
				trustStoreStream = new FileInputStream(trustStoreFile);

				KeyStore keyStore = KeyStore.getInstance("JKS");
				keyStore.load(keyStoreStream, KEYSTORE_PASSWORD.toCharArray());
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

				KeyStore trustStore = KeyStore.getInstance("JKS");
				trustStore.load(trustStoreStream, TRUSTSTORE_PASSWORD.toCharArray());
				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(trustStore);

				sslContext = SSLContext.getInstance(PROTOCOL);
				sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (keyStoreStream != null) keyStoreStream.close();
					if (trustStoreStream != null) trustStoreStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			logger.error("keystore or truststore file does not exist");
		}
		SSL_CONTEXT = sslContext;
	}

	private SslContextFactory() {
	}

	public static SSLContext getSslContext() {
		return SSL_CONTEXT;
	}

}
