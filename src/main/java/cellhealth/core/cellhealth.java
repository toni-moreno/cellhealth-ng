package cellhealth.core;

import cellhealth.core.connection.WASConnection;
import cellhealth.core.connection.WASConnectionSOAP;
import cellhealth.core.connection.WASConnectionRMI;

import cellhealth.core.test.TestMetrics;
import cellhealth.core.threads.Metrics.ThreadManager;
import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import cellhealth.utils.properties.xml.ReadMetricXml;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;
import com.ibm.websphere.management.exception.ConnectorException;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class cellhealth {
    
    public static WASConnection getWasConnection() {
        if ( Settings.properties().getConnType().equals("RMI") ){
            L4j.getL4j().info("Beginning RMI connection .....");
            return new WASConnectionRMI();
        }
        L4j.getL4j().info("Beginning SOAP Connection...");
        return new WASConnectionSOAP();
    }

    public static void main(String[] args) throws Exception {
        L4j.getL4j().setConfig(Settings.properties().getPathLog(), Settings.properties().getLogLevel());
        List<String> mainOptions = new LinkedList<String>();
        mainOptions.add("-l");
        mainOptions.add("-b");
        mainOptions.add("-o");
        mainOptions.add("-a");
        mainOptions.add("-t");
        mainOptions.add("-h");
        List<String> configOptions = new LinkedList<String>();
        configOptions.add("--type");
        configOptions.add("--host");
        configOptions.add("--port");
        int foundMainOptions = 0;
        boolean isError = false;
        boolean isOptionArgument = false;
        String optionArgument = "";
        String option = "";
        Map<String, String> options = new HashMap<String, String>();

        for(String argument: args) {
            if(!isOptionArgument) {
                if(mainOptions.contains(argument)){
                    option = argument;
                    foundMainOptions++;
                } else if(configOptions.contains(argument) || argument.length() >= 6 || configOptions.contains(argument.substring(0,6))){
                    if(argument.length() > 6){
                        options.put(argument.substring(0,6),  argument.substring(6));
                    } else {
                        isOptionArgument = true;
                        optionArgument = argument;
                    }
                } else {
                    isError = true;
                }
            } else {

                if(argument == null || mainOptions.contains(argument) || configOptions.contains(argument) || (argument.length() >= 6  && configOptions.contains(argument.substring(0,6)))){
                    isError = true;
                } else {
                    options.put(optionArgument, argument);
                }
                isOptionArgument = false;
            }

        }

        if(isError || foundMainOptions > 1) {
            L4j.getL4j().info("Option not supported");
            L4j.getL4j().info("Try '-help' to see options");
        } else {
            if(options.get("--type") != null){
                Settings.properties().setConnType(options.get("--type"));
            }
            if(options.get("--host") != null){
                Settings.properties().setHostWebsphere(options.get("--host"));
            }
            if(options.get("--port") != null) {
                Settings.properties().setPortWebsphere(options.get("--port"));
            }
            if ("-l".equals(option)) {
                showListOfMetrics();
            } else if ("-b".equals(option)) {
                launchListBean(1);
            } else if ("-o".equals(option)) {
                launchListBean(2);
            } else if ("-a".equals(option)) {
                launchListBean(3);
            } else if ("-t".equals(option)) {
                launchTest();
            } else if ("-h".equals(option)) {
                launchHelp();
            } else {
                L4j.getL4j().info("################################");
                L4j.getL4j().info("# WebSphere metrics collection #");
                L4j.getL4j().info("#        CellHealth-ng         #");
                L4j.getL4j().info("################################");
                L4j.getL4j().info("Try '-help' to see options");
                startCellHealth();
            }
        }
    }

    public static void startCellHealth() {
        L4j.getL4j().info("Starting CellHealth - Normal mode");
        ReadMetricXml readMetricXml = new ReadMetricXml();
        ThreadManager manager = new ThreadManager(readMetricXml.getCellHealthMetrics());
        Thread threadManager = new Thread(manager,"manager");
        threadManager.start();
    }
    public static void showListOfMetrics() {
        L4j.getL4j().info("Starting CellHealth - Metrics list");
        ListMetrics listMetrics = new ListMetrics(getWasConnection());
        try {
            listMetrics.list();
        } catch (ConnectorNotAvailableException e) {
             L4j.getL4j().error("Connector not available",e);
        } catch (ConnectorException e) {
             L4j.getL4j().error("GENERIC Connector exception",e);
        }
    }

    public static void launchListBean(int option){
        Scanner scanner = new Scanner(System.in);
        InfoBeans infoBeans = new InfoBeans(getWasConnection());

        System.out.print("List results based on the query (*:* default): ");
        String query = scanner.nextLine();
        if(query != null && query.length() > 0) {
            infoBeans.setQuery(query);
        }
        try {
            if(option == 1) {
                L4j.getL4j().info("Starting CellHealth - Bean list");
                infoBeans.listBean();
            } else if(option == 2){
                L4j.getL4j().info("Starting CellHealth - Bean list operations");
                infoBeans.listOperationsBean();
            } else if(option == 3) {
                L4j.getL4j().info("Starting CellHealth - Bean list attributes");
                infoBeans.listAttributesBean();
            }
        } catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error("Connector not available",e);
        } catch (ConnectorException e) {
                L4j.getL4j().error("GENERIC Connector exception",e);
        } finally {
        	scanner.close();
        }
    }

    public static void launchTest() {
        L4j.getL4j().info("Starting CellHealth - Metrics Tree");
        TestMetrics listMetrics = new TestMetrics(getWasConnection());
        try {
            listMetrics.test();
        } catch (ConnectorNotAvailableException e) {
            L4j.getL4j().error("Connector not available",e);
        } catch (ConnectorException e) {
            L4j.getL4j().error("Generic Connector Exception",e);
        }
        
    }

    public static void launchHelp() {
        System.out.println("################################");
        System.out.println("# WebSphere metrics collection #");
        System.out.println("#        CellHealth-ng         #");
        System.out.println("################################");
        System.out.println("CellHealth has different options");
        System.out.println("OPTIONS:");
        System.out.println("\t-l Show list of server beans");
        System.out.println("\t-b Show all beans");
        System.out.println("\t-o Show options of beans");
        System.out.println("\t-a Show attributes of beans");
        System.out.println("\t-h this help");
        System.out.println("CONFIGURATION OPTIONS (optional):");
        System.out.println("\t--host host of websphere");
        System.out.println("\t--port port of websphere");
        System.out.println("\t-v verbose");
    }
}