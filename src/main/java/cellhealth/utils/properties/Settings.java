package cellhealth.utils.properties;

import cellhealth.utils.constants.Constants;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Alberto Pascual on 8/06/15.
 */

public class Settings {

    private static Settings instance;

    private String hostWebsphere;
    private String portWebsphere;
    private long threadInterval;
    private long senderInterval;
    private long soapInterval;
    private String l4jPattern;
    private String l4jPatternConfig;
    private String appName;
    private String l4jName;
    private String l4jGetLogger;
    private String pathConf;
    private String pathSenderConf;
    private String pathLog;
    private String logLevel;
    private boolean isSelfStats;

    private Settings() {
    }

    public static synchronized Settings propertie() {
        if (instance == null) {
            instance = new Settings();
            instance.globalProperties();
            instance.readBaseProperties();
            instance.readL4jProperties();
        }
        return instance;
    }

    private static void globalProperties() {
        instance.setPathConf(Constants.CELLHEALTH_PATH + "conf/");
    }

    private static void readBaseProperties() {
        FileInputStream fileProperties;
        try {
            fileProperties = new FileInputStream(Constants.PATH_CELLHEALTH_PROPERTIES);
            Properties confProperties = new Properties();
            confProperties.load(fileProperties);
            instance.setHostWebsphere(confProperties.getProperty("was-adminhost"));
            instance.setPortWebsphere(confProperties.getProperty("was-soapport"));
            instance.setThreadInterval(Long.valueOf(confProperties.getProperty("ch_query_interval_secs")) * 1000l);
            instance.setSenderInterval(Long.valueOf(confProperties.getProperty("ch_reconnect_timeout")) * 1000l);
            instance.setSoapInterval(Long.valueOf(confProperties.getProperty("ch_soap_interval_secs")) * 1000l);
            instance.setPathSenderConf(instance.getPathConf() + confProperties.getProperty("sender_properties"));
            String logPath = (confProperties.getProperty("ch_output_log_path") == null) ? "cellhealth-ng.log" : confProperties.getProperty("ch_output_log_path");
            logPath = Constants.CELLHEALTH_PATH + "logs/" + logPath;
            instance.setPathLog(logPath);
            String logLevel = (confProperties.getProperty("ch_output_log_level") == null) ? "INFO" : confProperties.getProperty("ch_output_log_level");
            instance.setLogLevel(logLevel);
            instance.setSelfStats(Boolean.valueOf(confProperties.getProperty("ch_enable_self_jvm_stats")));
            fileProperties.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void readL4jProperties() {
        FileInputStream fileProperties;
        try {
            fileProperties = new FileInputStream(Constants.PATH_L4J_PROPERTIES);
            Properties confProperties = new Properties();
            confProperties.load(fileProperties);
            instance.setAppName(confProperties.getProperty("appName"));
            instance.setL4jPattern(confProperties.getProperty("l4jPattern"));
            instance.setL4jPatternConfig(confProperties.getProperty("l4jPatternConfig"));
            instance.setL4jName(confProperties.getProperty("l4jName"));
            instance.setL4jGetLogger(confProperties.getProperty("l4jgetLogger"));
            fileProperties.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getHostWebsphere() {
        return hostWebsphere;
    }

    public void setHostWebsphere(String hostWebsphere) {
        this.hostWebsphere = hostWebsphere;
    }

    public static Settings getInstance() {
        return instance;
    }

    public static void setInstance(Settings instance) {
        Settings.instance = instance;
    }

    public String getL4jPattern() {
        return l4jPattern;
    }

    public void setL4jPattern(String l4jPattern) {
        this.l4jPattern = l4jPattern;
    }

    public String getL4jPatternConfig() {
        return l4jPatternConfig;
    }

    public void setL4jPatternConfig(String l4jPatternConfig) {
        this.l4jPatternConfig = l4jPatternConfig;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getL4jName() {
        return l4jName;
    }

    public void setL4jName(String l4jName) {
        this.l4jName = l4jName;
    }

    public String getL4jGetLogger() {
        return l4jGetLogger;
    }

    public void setL4jGetLogger(String l4jGetLogger) {
        this.l4jGetLogger = l4jGetLogger;
    }

    public String getPathConf() {
        return pathConf;
    }

    public void setPathConf(String pathConf) {
        this.pathConf = pathConf;
    }

    public String getPortWebsphere() {
        return portWebsphere;
    }

    public void setPortWebsphere(String portWebsphere) {
        this.portWebsphere = portWebsphere;
    }

    public String getPathSenderConf() {
        return pathSenderConf;
    }

    public void setPathSenderConf(String pathSenderConf) {
        this.pathSenderConf = pathSenderConf;
    }

    public String getPathLog() {
        return pathLog;
    }

    public void setPathLog(String pathLog) {
        this.pathLog = pathLog;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public long getThreadInterval() {
        return threadInterval;
    }

    public void setThreadInterval(long threadInterval) {
        this.threadInterval = threadInterval;
    }

    public long getSenderInterval() {
        return senderInterval;
    }

    public void setSenderInterval(long senderInterval) {
        this.senderInterval = senderInterval;
    }

    public long getSoapInterval() {
        return soapInterval;
    }

    public void setSoapInterval(long soapInterval) {
        this.soapInterval = soapInterval;
    }

    public boolean isSelfStats() {
        return isSelfStats;
    }

    public void setSelfStats(boolean isSelfStats) {
        this.isSelfStats = isSelfStats;
    }
}