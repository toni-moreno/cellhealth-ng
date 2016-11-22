package cellhealth.core;

import cellhealth.core.connection.WASConnectionSOAP;

import cellhealth.core.test.TestMetrics;
import cellhealth.core.threads.Metrics.ThreadManager;
import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import cellhealth.utils.properties.xml.ReadMetricXml;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class cellhealth {

    public static void main(String[] args) throws Exception {
        L4j.getL4j().setConfig(Settings.propertie().getPathLog(), Settings.propertie().getLogLevel());
        List<String> mainOptions = new LinkedList<String>();
        mainOptions.add("-l");
        mainOptions.add("-b");
        mainOptions.add("-o");
        mainOptions.add("-a");
        mainOptions.add("-t");
        mainOptions.add("-h");
        List<String> configOptions = new LinkedList<String>();
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
            if(options.get("--host") != null){
                Settings.propertie().setHostWebsphere(options.get("--host"));
            }
            if(options.get("--port") != null) {
                Settings.propertie().setPortWebsphere(options.get("--port"));
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
                startCellehealth();
            }
        }
    }

    public static void startCellehealth() {
        L4j.getL4j().info("Starting CellHealth - Normal mode");
        ReadMetricXml readMetricXml = new ReadMetricXml();
        ThreadManager manager = new ThreadManager(readMetricXml.getCellHealthMetrics());
        Thread threadManager = new Thread(manager,"manager");
        threadManager.start();
    }
    public static void showListOfMetrics() {
        L4j.getL4j().info("Starting CellHealth - Metrics list");
        ListMetrics listMetrics = new ListMetrics(new WASConnectionSOAP());
        try {
            listMetrics.list();
        } catch (ConnectorNotAvailableException e) {
             L4j.getL4j().error("Connector not available",e);
        }
    }

    public static void launchListBean(int option){
        Scanner scanner = new Scanner(System.in);
        InfoBeans infoBeans = new InfoBeans(new WASConnectionSOAP());

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
        }
    }

    public static void launchTest() {
        L4j.getL4j().info("Starting CellHealth - Metrics Tree");
//        TreeBeans treeBeans = new TreeBeans(new WASConnectionSOAP());
//        treeBeans.list();
        TestMetrics listMetrics = new TestMetrics(new WASConnectionSOAP());
        try {
            listMetrics.test();
        } catch (ConnectorNotAvailableException e) {
            L4j.getL4j().error("Connector not available",e);
        }
    }

    public static void launchHelp() {
        L4j.getL4j().info("################################");
        L4j.getL4j().info("# WebSphere metrics collection #");
        L4j.getL4j().info("#        CellHealth-ng         #");
        L4j.getL4j().info("################################");
        L4j.getL4j().info("Cellhealth have a diferents options");
        L4j.getL4j().info("OPTIONS:");
        L4j.getL4j().info("\t-l Show list of server beans");
        L4j.getL4j().info("\t-b Show all beans");
        L4j.getL4j().info("\t-o Show options of beans");
        L4j.getL4j().info("\t-a Show attributes of beans");
        L4j.getL4j().info("\t-h this help");
        L4j.getL4j().info("CONFIGURATION OPTIONS (optional):");
        L4j.getL4j().info("\t--host host of websphere");
        L4j.getL4j().info("\t--port port of websphere");
        L4j.getL4j().info("\t-v verbose");
    }
}