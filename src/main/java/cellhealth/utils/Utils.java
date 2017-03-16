package cellhealth.utils;

import cellhealth.core.connection.MBeansManager;
import cellhealth.utils.constants.Constants;
import cellhealth.utils.logs.L4j;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.pmi.stat.WSStatistic;

import javax.management.ObjectName;
import java.util.List;
import java.util.Set;

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
        node = node.toLowerCase();
        String[] nodeSplit = node.split("node");
        return nodeSplit[0];
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
}
