package cellhealth.core.connection;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.Connection;

import cellhealth.utils.logs.L4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;

import com.squareup.okhttp.OkHttpClient;
import retrofit.client.OkClient;

public class DBConnection {

	public static Connection getPgConnection(String sHost, long lPort, String sDbName, String sUsername, String sPassword) 
    		throws SQLException {
        String sJdbcDriver = "org.postgresql.Driver";
        String sConnectionString = "jdbc:postgresql://" + sHost + ":" + lPort + "/" + sDbName;
        Connection connection = getJdbcConnection(sJdbcDriver, sConnectionString, sUsername, sPassword);
        return connection;
    }

    public static Connection getOracleConnection(String sHost, long lPort, String sDbName, String sUsername, String sPassword) 
    		throws SQLException {
        String sJdbcDriver = "oracle.jdbc.driver.OracleDriver";
        String sConnectionString = "jdbc:oracle:thin:@" + sHost + ":" + lPort + ":" + sDbName;
        Connection connection = getJdbcConnection(sJdbcDriver, sConnectionString, sUsername, sPassword);
        return connection;
    }
    
    private static Connection getJdbcConnection(String sJdbcDriver, String sConnectionString, String sUsername, String sPassword) 
    		throws SQLException {
    	Connection connection = null;
        try {
            Class.forName(sJdbcDriver);
        	connection = DriverManager.getConnection(sConnectionString, sUsername, sPassword);
        } catch (ClassNotFoundException e) {
            L4j.getL4j().error("DBConnection.getJdbcConnection. Error getting JDBC Driver: " + sJdbcDriver);
        }
        return connection;
    }

	private static InfluxDB getInfluxDBConnection(String sDbHost, Long lDbPort, String sDbUser, String sDbPassword, 
			Boolean bSsl, String sSslCertFilePath) 
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException 
	{
		InfluxDB influxDB = null;
        String sProtocol = "http";
        String sConnectionString = sProtocol + "://" + sDbHost + ":" + lDbPort;
        if (bSsl.booleanValue()) {
            OkHttpClient client = new OkHttpClient();
			client = SslManager.getSSLClient(sSslCertFilePath);
	        OkClient rfClient = new OkClient(client);
        	sProtocol = "https";
	        sConnectionString = sProtocol + "://" + sDbHost + ":" + lDbPort;
	        L4j.getL4j().debug("getInfluxDBConnection. Connecting to: " + sConnectionString);
	    	influxDB = InfluxDBFactory.connect(sConnectionString, sDbUser, sDbPassword, rfClient);
        } else {
        	influxDB = InfluxDBFactory.connect(sConnectionString, sDbUser, sDbPassword);
        }
    	return influxDB;
    }
    
	public static InfluxDB getInfluxDBConnection(String sDbHost, Long lDbPort, String sDbUser, String sDbPassword, 
			Boolean bSsl, String sSslCertFilePath, long lReconnectTimeout, Long lTimeToConnect) 
					throws InterruptedException 
	{
		InfluxDB influxDB = null;
		long lInitTime = System.currentTimeMillis();
		long lSpentTime = 0;
        long lReconnectTimeoutMs = lReconnectTimeout * 1000;
        boolean bIsConnected = false;
        while (!bIsConnected) {
        	try {
            	influxDB = getInfluxDBConnection(sDbHost, lDbPort, sDbUser, sDbPassword, bSsl, sSslCertFilePath);
        		L4j.getL4j().debug("getInfluxDBConnection. Calling ping");
		    	Pong pong = influxDB.ping();
		    	L4j.getL4j().debug("getInfluxDBConnection. " + pong.toString());
            	bIsConnected = (pong != null && pong.getVersion().length() > 0);
                L4j.getL4j().info("getInfluxDBConnection. Connected to InfluxDB (Host: " + sDbHost + " Port: " + lDbPort + ")");
        	} catch (Exception e) {
        		e.printStackTrace();
        		L4j.getL4j().warning("getInfluxDBConnection. Exception: " + e.getMessage());
				lSpentTime = System.currentTimeMillis() - lInitTime;
	            if (!bIsConnected) {
		            if ((lTimeToConnect == null) || (lSpentTime + lReconnectTimeoutMs) < lTimeToConnect.longValue()) {
		            	L4j.getL4j().warning("getInfluxDBConnection. Error pinging to host:port: " + sDbHost + ":" + lDbPort   
	                			+ ". Trying again after " + lReconnectTimeout + " seconds.");
						Thread.sleep(lReconnectTimeoutMs);
		            } else {
		            	L4j.getL4j().warning("getInfluxDBConnection. The process spent " + lSpentTime + " ms trying connection to host:port: " + sDbHost + ":" + lDbPort);
		            	break;
		            }
	            }
        	}
        }
        if (!bIsConnected) influxDB = null;
    	return influxDB;
    }
    
}
