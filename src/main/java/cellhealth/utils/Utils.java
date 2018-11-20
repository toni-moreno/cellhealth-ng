package cellhealth.utils;

import cellhealth.core.connection.MBeansManager;
import cellhealth.utils.constants.Constants;
import cellhealth.utils.logs.L4j;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.pmi.stat.WSStatistic;
import cellhealth.utils.properties.Settings;
import cellhealth.core.connection.DBConnection;

import javax.management.ObjectName;

import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Point.Builder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static String getWSStatisticType(WSStatistic wsstatistic) {
        String chain = wsstatistic.toString().replace(" ", "");

        for(String type: chain.split("\\,")){
            String[] splitType = type.split("=");
            if("type".equals(splitType[0])) {
                return splitType[1];
            }
        }
        return "N/A";
    }
    public static String getHostByNode(String node){
        if(!Settings.properties().getHostNameFromNodeName()) {
            L4j.getL4j().debug("GET HOSTNAME  from Node Name disabled: hostname will be set as the NodeName: "  + node);
            return node;
        }
        
        node = node.toLowerCase();
        String[] nodeSplit = node.split("node");
        String host = nodeSplit[0].replaceAll("[\\W]", "");
        L4j.getL4j().debug("GET HOSTNAME :" + host + " From NODE: " + node);
        return host ;
    }

    public static void showInstances(MBeansManager mbeansManager) {
        try {
        Set<ObjectName> runtimes = mbeansManager.getAllServerRuntimes();
            for(ObjectName serverRuntime: runtimes){
                String serverName = serverRuntime.getKeyProperty(Constants.NAME);
                String node = serverRuntime.getKeyProperty(Constants.NODE);
                String showServerHost = (( node==null ) || ( node.length() == 0 ))?"<NOT SET IN CONFIG>":node;
                L4j.getL4j().info("SERVER :" + serverName + " NODE: " + showServerHost);
            }
        } catch (ConnectorNotAvailableException e) {
            L4j.getL4j().error("showInstances Connector not available ",e);
        } catch (ConnectorException e) {
            L4j.getL4j().error("showInstances GENERIC Connector Exception ",e);
        }
    }

    public static String getParseBeanName(String beanName){
        beanName = beanName.replace(".", "_");
        beanName = beanName.replace(" ", "_");
        beanName = beanName.replace("/", "_");
        beanName = beanName.replace(":", "_");
        beanName = beanName.replace(")", "");
        beanName = beanName.replace("(", "");
        return beanName;
    }

    public static boolean listContainsReg(List<String> listString, String regex){
        for(String string:listString){
            if(string.matches(regex)){
                return true;
            }
        }
        return false;
    }

	/*
	 * isConnected. Returns if the database is connected or not. 
	 */
	public static boolean isConnected(Connection connection) throws SQLException {
		boolean bIsConnected = false;
        bIsConnected = (connection != null && !connection.isClosed());
		return bIsConnected;
	}

	/*
	 * isConnected. Returns if the database is connected or not. 
	 */
	public static boolean isConnected(InfluxDB influxDB) {
		boolean bIsConnected = false;
    	L4j.getL4j().debug("getInfluxDB. Calling ping");
    	Pong pong = influxDB.ping();
    	L4j.getL4j().debug("getInfluxDB. " + pong.toString());
    	bIsConnected = (pong != null && pong.getVersion().length() > 0);
		return bIsConnected;
	}

	/*
	 * getEmptyBatchPoints. Returns a BatchPoints object to be written to the destination InfluxDB. 
	 */
    public static BatchPoints getEmptyBatchPoints(String sDestDatabaseName) {
    	BatchPoints batchPoints = BatchPoints.database(sDestDatabaseName).build();
    	return batchPoints;
    }
    
	/*
	 * addBPointToBatchPoints. Adds a Builder Point as Point to BatchPoints. 
	 */
    public static BatchPoints addBPointToBatchPoints(BatchPoints batchPoints, Builder bPoint) {
		Point point = bPoint.build();
		batchPoints.point(point);
    	return batchPoints;
    }
    
	/*
	 * addPointToBatchPoints. Adds Point to BatchPoints. 
	 */
    public static BatchPoints addPointToBatchPoints(BatchPoints batchPoints, Point point) {
		batchPoints.point(point);
    	return batchPoints;
    }
    
	/*
	 * getInitialBPoint. Returns a Builder Point with current time in milliseconds for the measurement. 
	 */
    public static Builder getInitialBPoint(String sMeasurementId) {
		Builder bPoint = Point.measurement(sMeasurementId);
		bPoint.time(System.currentTimeMillis()/1000L, TimeUnit.SECONDS);
		return bPoint;
    }
    
	/*
	 * getPoint. Returns a Builder Point with time, fields and tags for the measurement. 
	 */
    public static Builder getBuilderPoint(String sMeasurementId, long lTime, Map<String, Object> fields, Map<String, String> tags) {
		Builder bPoint = getInitialBPoint(sMeasurementId);
		bPoint.time(lTime/1000L, TimeUnit.SECONDS);
		bPoint.fields(fields);
		bPoint.tag(tags);
		return bPoint;
    }
    
	/*
	 * addTagsToBPoint. Returns the Builder Point with the tags. 
	 * Parameters:
	 * 	- Builder bPoint: builder for the InfluxDB point
	 * 	- String sTags: tags in format tag1=value1,tag2=value2,..., tagN=valueN
	 */
    public static Builder addTagsToBPoint(Builder bPoint, String sTags) {
    	if (sTags != null) {
        	String[] asTags = sTags.split(",");
        	for (int i=0; i < asTags.length; i++) {
        		String[] asTagNameValue = asTags[i].split("=");
        		bPoint.tag(asTagNameValue[0], asTagNameValue[1]);
        	}
    	}
		return bPoint;
    }
    
	/*
	 * addTagsToBPoint. Returns the Builder Point with the tags. 
	 * Parameters:
	 * 	- Builder bPoint: builder for the InfluxDB point
	 * 	- Map<String, String> tagsToAdd: map with the tags to add
	 */
    public static Builder addTagsToBPoint(Builder bPoint, Map<String, String> tagsToAdd) {
		bPoint.tag(tagsToAdd);
		return bPoint;
    }
    
	/*
	 * addFieldsToBPoint. Returns the Builder Point with the fields. 
	 */
    public static Builder addFieldsToBPoint(Builder bPoint, Map<String, Object> fieldsToAdd) {
		bPoint.fields(fieldsToAdd);
		return bPoint;
    }
    
	/*
	 * getBpsInfoProcess. Returns BatchPoints with info about the process.
	 * Info must be passed with tags and fields. 
	 */
    public static BatchPoints getBpsInfoProcess(String sDestDatabaseName, String sMeasurementId, Map<String, String> tagsToAdd, Map<String, Object> fieldsToAdd) {
		BatchPoints bpsReadProcess = getEmptyBatchPoints(sDestDatabaseName);
		Builder bPoint = getInitialBPoint(sMeasurementId);
		bPoint = addTagsToBPoint(bPoint, tagsToAdd);
		bPoint = addFieldsToBPoint(bPoint, fieldsToAdd);
    	bpsReadProcess = addBPointToBatchPoints(bpsReadProcess, bPoint);
    	return bpsReadProcess;
    }
}
