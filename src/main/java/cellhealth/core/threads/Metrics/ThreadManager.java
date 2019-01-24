package cellhealth.core.threads.Metrics;

import cellhealth.core.connection.MBeansManager;
import cellhealth.core.connection.WASConnection;
import cellhealth.core.connection.WASConnectionRMI;
import cellhealth.core.connection.WASConnectionSOAP;
import cellhealth.core.statistics.Capturer;
import cellhealth.core.statistics.chStats.Stats;
import cellhealth.sender.Sender;
import cellhealth.sender.graphite.sender.GraphiteSender;
import cellhealth.sender.influxdb.sender.InfluxDBSender;
import cellhealth.utils.Utils;
import cellhealth.utils.constants.Constants;
import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import cellhealth.utils.properties.xml.CellHealthMetrics;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;

import javax.management.ObjectName;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadManager implements Runnable {

    private WASConnection wasConnection;
    private Sender sender;
    private MBeansManager mbeansManager;
    private CellHealthMetrics cellHealthMetrics;

    public ThreadManager(CellHealthMetrics cellHealthMetrics) {
        this.cellHealthMetrics = cellHealthMetrics;
        this.connectToWebSphere();
        this.sender = this.getSender();
    }

    private void startMBeansManager()  {
        this.mbeansManager = new MBeansManager(this.wasConnection);
    }

    public void run() {
        Utils.showInstances(this.mbeansManager);
        boolean start = true;
        while(start){
            L4j.getL4j().debug("ThreadManager - run : Beginning Loop Iteration");
            try {
                long start_time=System.currentTimeMillis();
                this.launchThreads();
                long elapsed = (System.currentTimeMillis() - start_time);
                long waittime = Settings.properties().getThreadInterval()-elapsed;
                L4j.getL4j().debug("ThreadManager - run : Wait time : " + String.valueOf(waittime)+" ms");
                if (waittime > 0 ) {
                    Thread.sleep(Settings.properties().getThreadInterval()-elapsed);
                } else {
                    Thread.sleep(Settings.properties().getThreadInterval());
                    L4j.getL4j().warning("ThreadManager - run : The WaitTime has been computed less than 0 :"+String.valueOf(waittime) );
                }
            } catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error(" ThreadManager - run :Connector not available",e);
                connectToWebSphere();
            } catch (ConnectorException e) {
                L4j.getL4j().error("ThreadManager - run : GENERIC Connector Exception trying to reconnect",e);
                connectToWebSphere();
            } catch (InterruptedException e) {
                start = false;
                L4j.getL4j().error("ThreadManager - run : sleep error: ", e);
            }
        }
        L4j.getL4j().debug("ThreadManager - run : End Loop Iteration");
    }

    //Perhaps could be better use the ScheduledThreadPool
    //http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/
    private void launchThreads() throws ConnectorNotAvailableException,ConnectorException {
        L4j.getL4j().debug("ThreadManager - launchThreads : BEGIN");
        Set<ObjectName> runtimes = this.mbeansManager.getAllServerRuntimes();
        Date timeCountStart = new Date();
        ExecutorService thpool = Executors.newFixedThreadPool(runtimes.size());
        checkConnections();
        Stats chStats = new Stats();
        try {
                Map<String,String> chStatsPath = this.mbeansManager.getPathHostChStats();
                chStats.setPathChStats(chStatsPath.get("path"));
                chStats.setHost(chStatsPath.get("host"));
                chStats.setCell(chStatsPath.get("cell"));
                chStats.setMeasurement(chStatsPath.get("measurement"));
        } catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error("Connector not available",e);
        } catch (ConnectorException e) {
                L4j.getL4j().error("GENERIC Connector not available",e);
        }
        
        for(ObjectName serverRuntime: runtimes){
            String serverName = serverRuntime.getKeyProperty(Constants.NAME);
            String node = serverRuntime.getKeyProperty(Constants.NODE);
            String cell = serverRuntime.getKeyProperty(Constants.CELL);
            L4j.getL4j().info("ThreadManager - LaunchThreads: for CELL ["+cell+"] NODE ["+node+"] SERVER ["+serverName+"]");
            Capturer capturer = new Capturer(this.mbeansManager, cell,node, serverName, cellHealthMetrics);
            Runnable worker = new MetricsCollector(capturer, this.sender, chStats);
            thpool.execute(worker);
        }
        thpool.shutdown(); // Disable new tasks from being submitted

        boolean waitToThreads = true;
        while(waitToThreads){
            
            try {
                thpool.awaitTermination(2, TimeUnit.SECONDS);
             } catch (InterruptedException e) {
                L4j.getL4j().error("ThreadManager- launchThreads Interrupt error: ", e);
            }
 
            if(thpool.isTerminated()){
                L4j.getL4j().debug("ThreadManager - launchThreads : POOL Finished OK!!");
                L4j.getL4j().debug("ThreadManager - launchThreads : Self stats activated: " + Settings.properties().isSelfStats());
                Long time_took =  new Date().getTime() - timeCountStart.getTime();
                if(Settings.properties().isSelfStats()) {
                    try {
                        chStats.getSelfStats(time_took);
                        L4j.getL4j().debug("ThreadManager - launchThreads : SelfStats size: " + chStats.getStats().size());
                        if (chStats.getStats() != null) {
                            sender.send(chStats);
                        }
                        L4j.getL4j().debug("ThreadManager - launchThreads : SelfStats sent OK");
                    } catch (Exception e) {
                        L4j.getL4j().error("ThreadManager - launchThreads : SelfStats Interrupt error: ", e);
                    }
                }
                L4j.getL4j().debug("ThreadManager - launchThreads : Thread Pool has terminated OK time TOOK: "+time_took+" ms");
                waitToThreads = false;
            }
            Long elapsed =  new Date().getTime() - timeCountStart.getTime();
            L4j.getL4j().debug("ThreadManager - launchThreads : WaitToThreads Elapsed: " + elapsed+ " ms");
            if(elapsed >= Settings.properties().getThreadInterval()){
                L4j.getL4j().error("ThreadManager - launchThreads - The threads are taking too much : "+elapsed+" ms");
            }
        }
        L4j.getL4j().debug("ThreadManager - launchThreads : END");
    }
   public static WASConnection getWasConnection() {
       if ( Settings.properties().getConnType().equals("RMI") ){
           L4j.getL4j().info("Beginning RMI connection .....");
           return new WASConnectionRMI();
       }
       L4j.getL4j().info("Beginning SOAP Connection...");
       return new WASConnectionSOAP();
    }

    public void connectToWebSphere(){
        this.wasConnection = getWasConnection();
        this.startMBeansManager();
    }
    public Sender getSender(){
        Sender sender = null;
        String senderType = Settings.properties().getSenderType();
        L4j.getL4j().info("getSender. senderType: " + senderType);
    	if (senderType != null && senderType.equalsIgnoreCase("influxdb")) {
            L4j.getL4j().info("getSender. Creating InfluxDBSender");
            sender = new InfluxDBSender();
    	} else {
            L4j.getL4j().info("getSender. Creating GraphiteSender");
            sender = new GraphiteSender();
    	}
        L4j.getL4j().info("getSender. sender.getType(): " + sender.getType());
        sender.init();
        return sender;
    }

    public void checkConnections(){
        /*try {
            Session Ses= mbeansManager.getClient().isAlive();
            L4j.getL4j().info("Current Session: "+Ses.toString());
        } catch (ConnectorException e) {
            if(e instanceof ConnectorNotAvailableException){
                connectToWebSphere();
            }
        }*/
        L4j.getL4j().info("checkConnections. sender.getType(): " + this.sender.getType());
        while(!sender.isConnected()){
            try {
                L4j.getL4j().warning("The sender is not connected , waiting to connect");
                Thread.sleep(Settings.properties().getSenderInterval() / 2);
            } catch (InterruptedException e) {
                L4j.getL4j().error("ThreadManager Interrupt error: ", e);
            }
        }
    }
}