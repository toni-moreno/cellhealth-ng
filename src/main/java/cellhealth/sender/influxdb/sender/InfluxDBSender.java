package cellhealth.sender.influxdb.sender;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point.Builder;

import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import cellhealth.core.connection.DBConnection;
import cellhealth.sender.Sender;
import cellhealth.utils.Utils;

public class InfluxDBSender implements Sender {

    private Properties influxdbProperties;
    private InfluxDB influxDB;
    private String type = "influxdb";
    private String sSelfDbName;
    private String sDestDbName;
    private Map<String, String> mapExtraTags;
    private Map<String, String> nodeMapToHost;
    private static String DEFAULT_CONF = "Properties not found will be initialized by default";
    private static String INFLUXDB_INIT = "InfluxDB sender Background starting up";
    private static String SET_PORT = "The format of the port is incorrect, setting the default 8086";
    private static String SET_RECONNECT_TIMEOUT = "The format of the timeout is incorrect, setting the default 60";
    private static String DISABLE = "Statistics Retriever Background Service has been disabled. Reason: ";

    public InfluxDBSender(InfluxDB influxDB) {
        this.influxDB = influxDB;
    }
    
    public InfluxDBSender() {
        boolean haveProperties = true;
        this.influxdbProperties = new Properties();
        try {
            this.influxdbProperties.load(new FileInputStream(Settings.getInstance().getPathSenderConf()));
            this.mapExtraTags = getMapFromString(this.influxdbProperties.getProperty("extraTags"));
            this.nodeMapToHost = getMapFromString(this.influxdbProperties.getProperty("forceNodeMapToHost"));
        } catch (IOException e) {
            haveProperties = false;
        }
        if(!haveProperties || this.influxdbProperties.isEmpty()) {
            this.setDefaultProperties();
        }
    }
    
    private LinkedHashMap<String, String> getMapFromString(String sInput) {
        L4j.getL4j().info("Entering getMapFromString with: ["+sInput+"]");
    	LinkedHashMap<String, String> mapOutput = new LinkedHashMap<String, String>();
        if (sInput != null && sInput.length()>0) {
            for(String keyValue : sInput.split(" *, *")) {
               String[] pairs = keyValue.split(" *= *", 2);
               String key = pairs[0];
               String val = pairs.length == 1 ? "" : pairs[1];
               mapOutput.put(key,val);
               L4j.getL4j().info("getMapFromString. Mapping: key ["+key+"] value ["+val+"]");
            }
        }
    	return mapOutput;
    }
    
    private void setDefaultProperties(){
        L4j.getL4j().info(DEFAULT_CONF);
        this.influxdbProperties.setProperty("host", "localhost");
        this.influxdbProperties.setProperty("port", "8086");
        this.influxdbProperties.setProperty("selfDbName", "_cellhealth");
        this.influxdbProperties.setProperty("destDbName", "was_metrics");
        this.influxdbProperties.setProperty("username", "ifxuser");
        this.influxdbProperties.setProperty("password", "ifxpass");
        this.influxdbProperties.setProperty("ssl", "false");
        this.influxdbProperties.setProperty("sslCertFilePath", "/etc/ssl/influxdb-selfsigned.crt");
        this.influxdbProperties.setProperty("reconnectTimeout", "60");
        this.influxdbProperties.setProperty("hostSuffix", "was");
    }

    public void init() {
        L4j.getL4j().info(INFLUXDB_INIT);
        try {
        	this.influxDB = this.getInfluxDB();
        } catch (Exception e) {
            L4j.getL4j().critical(new StringBuilder(DISABLE).append(e.toString()).toString());
        }
    }

    public boolean isConnected() {
        boolean isConnected = false;
        try {
            isConnected = Utils.isConnected(this.influxDB);
        } catch (Exception e) {
            L4j.getL4j().error("isConnected", e);
        }
        return isConnected;
    }
    
    public String getType() {
    	return this.type;
    }

    public String getSelfDbName() {
    	return this.sSelfDbName;
    }

    public String getDestDbName() {
    	return this.sDestDbName;
    }

    public void send(cellhealth.core.statistics.Stats stats) {
        L4j.getL4j().debug("sendAllMetricRange. getInitialPoint for Measurement: "+stats.getMeasurement());
        Builder bPoint = Utils.getBuilderPoint(stats.getMeasurement(), stats.getTime().longValue(), stats.getFields(), stats.getTags());
        try {
        	this.send(bPoint, false, stats.getHost());
		} catch (InterruptedException e) {
            L4j.getL4j().error("send(stats). Error sending point to InfluxDB server: "+e.getMessage());
		}
    }
    
    public void send(cellhealth.core.statistics.chStats.Stats chStats) {
        try {
			this.send(chStats.getBuilderPoints(), true, chStats.getHost());
		} catch (InterruptedException e) {
            L4j.getL4j().error("send(chStats). Error sending point to InfluxDB server: "+e.getMessage());
		}
    }
    
    private void send(List<Builder> lsBuilderPoints, boolean bSelfDatabase, String sHost) throws InterruptedException {
    	L4j.getL4j().debug("InfluxDBSender. Begin send.");
		long lInitTime = System.currentTimeMillis();
		long lSpentTime = 0;
		String sDatabase = this.sSelfDbName;
		if (!bSelfDatabase) sDatabase = this.sDestDbName;
		
    	try {
			if (lsBuilderPoints.size() > 0) {
		    	for (Builder bPoint: lsBuilderPoints) {
		    		bPoint = addTagsFromConfig(bPoint, sHost);
    				influxDB.write(sDatabase, "", bPoint.build());
		    	}
			}
			lSpentTime = System.currentTimeMillis() - lInitTime;
			L4j.getL4j().debug("InfluxDBSender.send. Time spent writing to influx (ms):" + lSpentTime);
		} catch (Exception e) {
			L4j.getL4j().error("InfluxDBSender.send. Error writing to InfluxDB. ErrorMsg: " + e.getMessage());
		}
    }

    private void send(Builder bPoint, boolean bSelfDatabase, String sHost) throws InterruptedException {
    	L4j.getL4j().debug("InfluxDBSender. Begin send.");
		long lInitTime = System.currentTimeMillis();
		long lSpentTime = 0;
		String sDatabase = this.sSelfDbName;
		if (!bSelfDatabase) sDatabase = this.sDestDbName;
		
		try {
    		bPoint = addTagsFromConfig(bPoint, sHost);
			influxDB.write(sDatabase, "", bPoint.build());
			lSpentTime = System.currentTimeMillis() - lInitTime;
			L4j.getL4j().debug("InfluxDBSender.send. Time spent writing to influx (ms):" + lSpentTime);
		} catch (Exception e) {
			L4j.getL4j().error("InfluxDBSender.send. Error writing to InfluxDB. ErrorMsg: " + e.getMessage());
		}
    }
    
    private Builder addTagsFromConfig(Builder bPoint, String sHost) {
    	if (this.influxdbProperties.getProperty("hostSuffix") != null && this.influxdbProperties.getProperty("hostSuffix").length() > 0) {
    		bPoint.tag("hostSuffix", this.influxdbProperties.getProperty("hostSuffix"));
    	}
    	if (this.mapExtraTags != null && this.mapExtraTags.size() > 0) {
    		bPoint.tag(this.mapExtraTags);
    	}
    	if (this.nodeMapToHost != null && this.nodeMapToHost.size() > 0) {
    		String sMapped = this.nodeMapToHost.get(sHost);
    		if (sMapped != null && sMapped.length() > 0) {
        		bPoint.tag("host", sMapped);
    		}
    	}
    	return bPoint;
    }

    private InfluxDB getInfluxDB() throws InterruptedException {
        L4j.getL4j().debug("InfluxDBSender. Begin getInfluxDB.");
		long lInitTime = System.currentTimeMillis();
    	//Try connection to DestDB until it's done.
		String sHost = this.influxdbProperties.getProperty("host");
		Long lPort = new Long(8086);
        try {
        	lPort = new Long(this.influxdbProperties.getProperty("port"));
        } catch(NumberFormatException e){
            L4j.getL4j().warning(SET_PORT);
            lPort = new Long(8086);
        }
		long lReconnectTimeout = 60;
        try {
        	lReconnectTimeout = Long.parseLong(this.influxdbProperties.getProperty("reconnectTimeout"));
        } catch(NumberFormatException e){
            L4j.getL4j().warning(SET_RECONNECT_TIMEOUT);
            lReconnectTimeout = 60;
        }
        this.sSelfDbName = this.influxdbProperties.getProperty("selfDbName");
        this.sDestDbName = this.influxdbProperties.getProperty("destDbName");
		String sUsername = this.influxdbProperties.getProperty("username");
		String sPassword = this.influxdbProperties.getProperty("password");
		Boolean bSsl = new Boolean(this.influxdbProperties.getProperty("ssl"));
		String sSslCertFilePath = this.influxdbProperties.getProperty("sslCertFilePath");
		Long lTimeToConnect = null;
    	InfluxDB influxDB = DBConnection.getInfluxDBConnection(sHost, lPort, sUsername, sPassword, bSsl, sSslCertFilePath, lReconnectTimeout, lTimeToConnect);
		long lSpentTime = System.currentTimeMillis() - lInitTime;
		L4j.getL4j().debug("InfluxDBSender. End getInfluxDB. Time spent (ms):" + lSpentTime);
    	return influxDB;
    }
    
}
