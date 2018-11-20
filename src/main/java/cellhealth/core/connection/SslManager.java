package cellhealth.core.connection;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.squareup.okhttp.OkHttpClient;

/*
 * 
 * Enable HTTPS in the InfluxDB configuration file 
 * HTTPS is disabled by default. 
 * Enable HTTPS in InfluxDB’s the [http] section of the configuration file (/etc/influxdb/influxdb.conf) by setting:
 * https-enabled to true
 * http-certificate to /etc/ssl/<signed-certificate-file>.crt (or to /etc/ssl/<bundled-certificate-file>.pem)
 * http-private-key to /etc/ssl/<private-key-file>.key (or to /etc/ssl/<bundled-certificate-file>.pem)
 * 
 */

public class SslManager {
	
	public static OkHttpClient getSSLClient(String sSslCertFilePath) throws
		CertificateException,
	    IOException, 
	    KeyManagementException,
	    KeyStoreException,
	    NoSuchAlgorithmException
	{
		OkHttpClient client;
		SSLContext sslContext;
		SSLSocketFactory sslSocketFactory;
		TrustManager[] trustManagers;
		TrustManagerFactory trustManagerFactory;
		X509TrustManager trustManager;

		trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore ks = readKeyStore(sSslCertFilePath);
		trustManagerFactory.init(ks);
		trustManagers = trustManagerFactory.getTrustManagers();

		if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
		}

		trustManager = (X509TrustManager) trustManagers[0];

		sslContext = SSLContext.getInstance("TLS");

		sslContext.init(null, new TrustManager[]{trustManager}, null);

		sslSocketFactory = sslContext.getSocketFactory();

		client = new OkHttpClient();
		client.setSslSocketFactory(sslSocketFactory);
		return client;
	}

	/**
	* Get keys store.
	*
	* @return Keys store.
	* @throws IOException 
	* @throws CertificateException 
	* @throws NoSuchAlgorithmException 
	* @throws KeyStoreException 
	*/
	private static KeyStore readKeyStore(String sSslCertFilePath) throws 
		CertificateException, 
		IOException, 
		KeyStoreException, 
		NoSuchAlgorithmException
	{
	    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
	    
	    FileInputStream fis = null;
	    try {
	        fis = new FileInputStream(sSslCertFilePath);

	        CertificateFactory cf = CertificateFactory.getInstance("X.509");
	        X509Certificate caCert = (X509Certificate) cf.generateCertificate(fis);

		    ks.load(null); // KeyStore instance is not needed from a file.
		    ks.setCertificateEntry("caCert", caCert);
	    } finally {
	        if (fis != null) {
	            fis.close();
	        }
	    }
	    return ks;
	}
}
