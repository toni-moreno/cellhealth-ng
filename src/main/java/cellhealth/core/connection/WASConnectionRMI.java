package cellhealth.core.connection;

import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import com.ibm.websphere.management.Session;
import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;

import java.util.Properties;

/**
 * Clase que implementa la interfaz WASConnection
 * Utiliza el conector de typo soap, para establecer la conexion
 * @author Alberto Pascual
 * @version 1.0
 */
public class WASConnectionRMI implements WASConnection {
    private Session ses;
    private AdminClient client;

    /**
     * En este caso el constructor es el encargado de establecer la conexion, con un retraso entre
     * intentos establecido en las propiedades
     */
    public WASConnectionRMI() {
        do {
            this.connect();
            if(this.client == null) {
                try {
                    Thread.sleep(Settings.properties().getSoapInterval());
                } catch (InterruptedException e) {
                    L4j.getL4j().error("SOAP ERROR",e);
                }
            }
        } while(this.client == null);
    }

    /**
     * Este metodo establece la conexion con websphere
     * Genera las proiedades con seguridad, las demas se pasan por parametros a la aplicacion
     * mediente el lanzador
     */
    public void connect() {
        Properties properties = new Properties();
        //Security.setProperty(Constants.SSL_SOCKETFACTORY_PROVIDER, Constants.JSSE2_SSLSOCKETFACTORYIMPL);
        properties.setProperty(AdminClient.CONNECTOR_HOST, Settings.properties().getHostWebsphere());
        properties.setProperty(AdminClient.CONNECTOR_PORT, Settings.properties().getPortWebsphere());
        properties.setProperty(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_RMI);
        //properties.setProperty(AdminClient.USERNAME, "waspuppet");
        //properties.setProperty(AdminClient.PASSWORD, "12345678");
        try {
            this.client = AdminClientFactory.createAdminClient(properties);
           
        } catch (ConnectorException e) {
            L4j.getL4j().error("The system can not create a RMI connector to connect host " +  Settings.properties().getHostWebsphere() + " on port " + Settings.properties().getPortWebsphere());
            L4j.getL4j().error("RMI ERROR: ",e);
        }
        if(this.client != null) {
            try {
                 ses= client.isAlive();
            } catch (ConnectorException e) {
                 L4j.getL4j().error("The system can not create get Session Info: ",e);
            }
            L4j.getL4j().info("Connection to process \"deploy manager\" through PROTOCOL: " + properties.getProperty(AdminClient.CONNECTOR_TYPE) + " into HOST: " + properties.getProperty(AdminClient.CONNECTOR_HOST) + " Port: " + properties.getProperty(AdminClient.CONNECTOR_PORT));
            L4j.getL4j().info("Current Session| ID: "+ses.getSessionId()+" | UserName: " +ses.getUserName());
        }
    }

    public AdminClient getClient() {
        return this.client;
    }
}

