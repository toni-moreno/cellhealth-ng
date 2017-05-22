package cellhealth.core.threads.Metrics;

import cellhealth.core.connection.MBeansManager;
import cellhealth.core.connection.WASConnection;
import cellhealth.core.connection.WASConnectionRMI;
import cellhealth.core.connection.WASConnectionSOAP;
import com.ibm.websphere.management.Session;
import cellhealth.core.statistics.Capturer;
import cellhealth.core.statistics.chStats.Stats;
import cellhealth.sender.Sender;
import cellhealth.sender.graphite.sender.GraphiteSender;
import cellhealth.utils.Utils;
import cellhealth.utils.constants.Constants;
import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import cellhealth.utils.properties.xml.CellHealthMetrics;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;

import javax.management.ObjectName;
import java.util.Date;
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
            L4j.getL4j().debug("TreadManager - run : Begining Thread Manager Loop");
            try {
                long start_time=System.currentTimeMillis();
                this.launchThreads();
                long elapsed = (System.currentTimeMillis() - start_time);
                long waittime = Settings.propertie().getThreadInterval()-elapsed;
                L4j.getL4j().debug("TreadManager - run : Wait time : " + String.valueOf(waittime)+" ms");
                if (waittime > 0 ) {
                    Thread.sleep(Settings.propertie().getThreadInterval()-elapsed);
                } else {
                    Thread.sleep(Settings.propertie().getThreadInterval());
                    L4j.getL4j().warning("TreadManager - run : The WaitTime has been computed less than 0 :"+String.valueOf(waittime) );
                }
            } catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error(" TreadManager - run :Connector not available",e);
                connectToWebSphere();
            } catch (ConnectorException e) {
                L4j.getL4j().error("TreadManager - run : GENERIC Connector Exception trying to recoonect",e);
                connectToWebSphere();
            } catch (InterruptedException e) {
                start = false;
                L4j.getL4j().error("TreadManager - run : sleep error: ", e);
            }
        }
    }

    //Perhaps could be better use the ScheduledThreadPool
    //http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/
    private void launchThreads() throws ConnectorNotAvailableException,ConnectorException {
        Set<ObjectName> runtimes = this.mbeansManager.getAllServerRuntimes();
        Date timeCountStart = new Date();
        ExecutorService thpool = Executors.newFixedThreadPool(runtimes.size());
        checkConnections();
        Stats chStats = new Stats();
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
            Long elapsed =  new Date().getTime() - timeCountStart.getTime();
            L4j.getL4j().debug("ThreadManager - lauchThreads : WaitToThreads Elapsed: " + elapsed+ " ms");
            if(thpool.isTerminated()){
                L4j.getL4j().debug("ThreadManager - lauchThreads : POOL Finished OK!!");
                L4j.getL4j().debug("ThreadManager - lauchThreads : Self stats activated: " + Settings.propertie().isSelfStats());
                if(Settings.propertie().isSelfStats()) {
                    chStats.getSelfStats(String.valueOf(elapsed));
                    L4j.getL4j().debug("ThreadManager - lauchThreads : SelfStats size: " + chStats.getStats().size());
                    if (chStats.getStats() != null) {
                        for (String metric : chStats.getStats()) {
                            sender.send(chStats.getHost(), metric);
                        }
                    }
                }
                waitToThreads = false;
            }
            if(elapsed >= Settings.propertie().getThreadInterval()){
                L4j.getL4j().error("ThreadManager - lauchThreads - The threads are taking too much : "+elapsed+" ms");
            }
        }
    }
   public static WASConnection getWasConnection() {
        if ( Settings.propertie().getConnType().equals("RMI") ){
            L4j.getL4j().info("Begginning RMI connection .....");
            return new WASConnectionRMI();
        }
        L4j.getL4j().info("Beggining SOAP Connection...");
        return new WASConnectionSOAP();
    }

    public void connectToWebSphere(){
        this.wasConnection = getWasConnection();
        this.startMBeansManager();
    }
    public Sender getSender(){
        GraphiteSender sender = new GraphiteSender();
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
        while(!sender.isConnected()){
            try {
                L4j.getL4j().warning("The sender is not connected , waiting to connect");
                Thread.sleep(Settings.propertie().getSenderInterval() / 2);
            } catch (InterruptedException e) {
                L4j.getL4j().error("ThreadManager Interrupt error: ", e);
            }
        }
    }
}