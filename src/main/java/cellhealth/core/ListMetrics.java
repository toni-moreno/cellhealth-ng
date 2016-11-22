package cellhealth.core;

import cellhealth.core.connection.MBeansManager;
import cellhealth.core.connection.WASConnection;
import cellhealth.core.statistics.MBeanStats;
import cellhealth.utils.constants.Constants;
import cellhealth.utils.logs.L4j;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.websphere.pmi.stat.WSStats;

import javax.management.ObjectName;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ListMetrics {

    private MBeansManager mbeansManager;
    private Scanner scanner;
    private final String prefix;
    private final String sufic;
    private File file;

    public ListMetrics(WASConnection wasConnection) {
        this.mbeansManager = new MBeansManager(wasConnection);
        this.scanner = new Scanner(System.in);
        this.prefix = "logs/";
        this.sufic = "metrics.info";

    }

    public void list() throws ConnectorNotAvailableException {

        List<String> options = new LinkedList<String>();
        options.add("yes");
        options.add("y");
        options.add("n");
        options.add("no");
        String option;
        boolean exit = false;
        do {
            System.out.print("You specify the server you want to display the list of metrics (y/n(default))");
            option = scanner.nextLine();
            if (options.contains(option.toLowerCase()) || option == null || option.length() == 0) {
                exit = true;
            } else {
                System.out.println("unknown option");
            }
        } while(!exit);
        Map<Integer,Map<String, String>> serverOptions = null;
        ObjectName serverPerfMBean = null;
        if("yes".equals(option.toLowerCase()) || "y".equals(option.toLowerCase())) {
            serverOptions = new TreeMap<Integer, Map<String, String>>();
            Set<ObjectName> runtimes = mbeansManager.getAllServerRuntimes();
            int count = 0;
            for (ObjectName serverRuntime : runtimes) {
                if(!"DeploymentManager".equals(serverRuntime.getKeyProperty("processType"))) {
                    count++;
                    Map<String, String> infoServer = new HashMap<String, String>();
                    infoServer.put("serverName", serverRuntime.getKeyProperty(Constants.NAME));
                    infoServer.put("node", serverRuntime.getKeyProperty(Constants.NODE));
                    serverOptions.put(count, infoServer);
                    String node = ((infoServer.get("node") == null) || (infoServer.get("node").length() == 0)) ? "<NOT SET IN CONFIG>" : infoServer.get("node");
                    System.out.println(count + ")" + " Server: " + infoServer.get("serverName") + " Node: " + node + " Type: " + serverRuntime.getKeyProperty("processType"));
                }
            }   
            boolean optionServerExit = false;
            do {
                System.out.print("Select server [1-" + count +"]");
                option = scanner.nextLine();
                try {
                    if (Integer.valueOf(option) > 0 && Integer.valueOf(option) <= count) {
                        Map<String, String> infoServer = serverOptions.get(Integer.valueOf(option));
                        serverPerfMBean = mbeansManager.getMBean("WebSphere:type=Perf,node=" + infoServer.get("node") + ",process=" + infoServer.get("serverName") + ",*");
                        optionServerExit = true;
                    } else {
                        System.out.println("unknown option");
                    }
                } catch (ConnectorNotAvailableException e) {
                    L4j.getL4j().error("Connector not available",e);
                } catch(NumberFormatException e){
                    System.out.println("unknown option");
                }
            } while(!optionServerExit);
        } else {
            try {
            serverPerfMBean = mbeansManager.getMBean("WebSphere:type=Perf,*");
            }  catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error("Connector not available",e);
            }
            String node = ((serverPerfMBean.getKeyProperty(Constants.NODE) == null) || (serverPerfMBean.getKeyProperty(Constants.NODE).length() == 0)) ? "<NOT SET IN CONFIG>" : serverPerfMBean.getKeyProperty(Constants.NODE);
            System.out.println("Server: " + serverPerfMBean.getKeyProperty("process") + " Node: " + node);
        }
        System.out.println("Query perf bean: " + serverPerfMBean);

        Set<MBeanStats> typeBeans = new TreeSet<MBeanStats>();
        L4j.getL4j().info(new StringBuilder("Getting the list of names and possible metric of the node: ").append(serverPerfMBean.getKeyProperty("node")).append(", instance: ").append(serverPerfMBean.getKeyProperty("process")).toString());
        String pathMetricsResult = prefix + serverPerfMBean.getKeyProperty(Constants.NODE) + "-" + serverPerfMBean.getKeyProperty("process") + "-" + sufic;
        this.file = new File(pathMetricsResult);
        if(this.file.exists()){
            this.file.delete();
        }
        try {
            this.file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            for(ObjectName objectName: mbeansManager.getMBeans("WebSphere:*")){
                String[] signature = new String[] {"javax.management.ObjectName","java.lang.Boolean"};
                Object[] params = new Object[] {objectName, new Boolean(true)};
                WSStats wsStats = (WSStats) mbeansManager.getClient().invoke(serverPerfMBean, "getStatsObject", params, signature);
                if(wsStats != null) {
                    MBeanStats mbeanStats = new MBeanStats();
                    mbeanStats.setObjectName(objectName);
                    mbeanStats.setName(wsStats.getName());
                    mbeanStats.setSubStats((wsStats.getSubStats().length > 0));
                    mbeanStats.setWsStats(wsStats);
                    typeBeans.add(mbeanStats);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        print("\n\n");
        print("List of MBeans (Statistics)\n");
        print("###########################");
        for(MBeanStats t: typeBeans) {
            if(t.getObjectName().getKeyProperty("name").equals(serverPerfMBean.getKeyProperty("process"))) {
                print("\n\nSpecific Bean: " + t.getObjectName().getKeyProperty("name") + "\n");
                mostrarStats(t);
            }
        }
        System.out.println("The result is saved in " + this.file.getAbsolutePath());
    }

    public void mostrarStats(MBeanStats t){
        print("\n\tMetric name: (1 nivel): " + t.getWsStats().getName() + "\n");
        if(t.isSubStats()){
            mostrarSubstats(t.getWsStats().getSubStats(), 1);
        } else {
            for(WSStatistic statistic:t.getWsStats().getStatistics()){
                mostrarValoresStatistis(statistic);
            }
        }
    }

    public void mostrarValoresStatistis(WSStatistic statistic){
        print("\t\tId: " + statistic.getId() + " Name: " + statistic.getName() + "\n");
    }

    public void mostrarSubstats(WSStats[] stats, int cant){
        cant++;
        for(WSStats statss: stats){
            print("\n\tMetric name (" + cant + " nivel): " + statss.getName() + "\n");
            for(WSStatistic statistic:statss.getStatistics()){
                mostrarValoresStatistis(statistic);
            }
            if(statss.getSubStats().length > 0) {
                mostrarSubstats(statss.getSubStats(), cant);
            }
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
