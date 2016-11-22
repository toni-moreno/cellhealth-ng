package cellhealth.core;

import cellhealth.core.connection.MBeansManager;
import cellhealth.core.connection.WASConnection;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class InfoBeans {

    private MBeansManager mbeansManager;
    private final String prefix;
    private File file;
    private String query;

    public InfoBeans(WASConnection wasConnection) {
        this.mbeansManager = new MBeansManager(wasConnection);
        this.query = "*:*";
        this.prefix = "logs/";
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void listBean() throws ConnectorNotAvailableException {
        this.startFile("listbeans.info");
        for(ObjectName objectName: mbeansManager.getMBeans(this.query)){
            print("############## BEAN ##############\n");
            print("name: " + objectName.getKeyProperty("name") + "\n");
            print("cell: " + objectName.getKeyProperty("cell") + "\n");
            print("node: " + objectName.getKeyProperty("node") + "\n");
            print("process: " + objectName.getKeyProperty("process") + "\n");
            print("ObjectName: " + objectName + "\n");
            print("##################################\n\n");

        }
    }

    public void listOperationsBean() throws ConnectorNotAvailableException {
        this.startFile("listOperationBeans.info");
        for(ObjectName objectName: mbeansManager.getMBeans(this.query)){
            print("############## BEAN ##############\n");
            print("ObjectName: " + objectName + "\n");
            print("Operations");
            MBeanInfo mbeanInfo = getBean(objectName);
            for(MBeanOperationInfo mbeanOperationInfo: mbeanInfo.getOperations()){
                print("\n\t Name: " + mbeanOperationInfo.getName() + "\n");
                print("\t Return type: " + mbeanOperationInfo.getReturnType() + "\n");
                print("\t Description: " + mbeanOperationInfo.getDescription() + "\n");
            }
            print("##################################\n\n");
        }
    }

    public void listAttributesBean() throws ConnectorNotAvailableException {
        this.startFile("listAttributeBeans.info");
        for(ObjectName objectName: mbeansManager.getMBeans(this.query)){
            print("############## BEAN ##############\n");
            print("ObjectName: " + objectName + "\n");
            print("Attributes");
            MBeanInfo mbeanInfo = getBean(objectName);
            for(MBeanAttributeInfo mbeanAttributeInfo: mbeanInfo.getAttributes()){
                print("\n\t Name: " + mbeanAttributeInfo.getName() + "\n");
                print("\t Type: " + mbeanAttributeInfo.getType() + "\n");
                print("\t Description: " + mbeanAttributeInfo.getDescription() + "\n");
            }
            print("##################################\n\n");
        }
    }

    public MBeanInfo getBean(ObjectName objectName){
        MBeanInfo mbeanInfo = null;
        try {
            mbeanInfo = mbeansManager.getClient().getMBeanInfo(objectName);
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (ConnectorException e) {
            e.printStackTrace();
        }
        return mbeanInfo;
    }

    public void startFile(String path){
        String pathMetricsResult = prefix + path;
        this.file = new File(pathMetricsResult);
        if(this.file.exists()){
            this.file.delete();
        }
        try {
            this.file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void print(String line) {
        System.out.print(line);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(this.file, true);
            fileWriter.write(line);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
