package cellhealth.utils.constants;

/**
 * Created by Alberto Pascual on 8/06/15.
 */
public class Constants {

    public static final String SSL_SOCKETFACTORY_PROVIDER = "ssl.SocketFactory.provider";
    public static final String JSSE2_SSLSOCKETFACTORYIMPL = "com.ibm.jsse2.SSLSocketFactoryImpl";
    public static final String PATH_CELLHEALTH_PROPERTIES = System.getProperty("ch_config_path");
    public static final String PATH_L4J_PROPERTIES = System.getProperty("ch_l4j_configuration_path");
    public static final String CELLHEALTH_PATH = System.getProperty("ch_dir_path");
    public final static String NAME = "name";
    public final static String NODE = "node";
    public static final String QUERY_SERVER_RUNTIME = "WebSphere:*,type=Server,j2eeType=J2EEServer";
}
