package cellhealth.core.test;

import cellhealth.core.connection.MBeansManager;
import cellhealth.core.connection.WASConnection;
import cellhealth.utils.Utils;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Alberto Pascual on 7/07/15.
 */
public class TestMetrics {

    private MBeansManager mbeansManager;
    protected static final int DEFAULT_CONTENT_LINE_LEN = 100;
    private final DateFormat secondDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public static final char SEPARATOR_CHAR = ',';

    /**
     * The comma separator used in the CSV files to separate different
     * property values
     */
    public static final String SEPARATOR = "" + SEPARATOR_CHAR;
    protected static final long BYTES_IN_MEGABYTE = 1024 * 1024;

    public void test() throws ConnectorNotAvailableException{

        ObjectName dmgr = this.mbeansManager.getMBean("WebSphere:processType=DeploymentManager,*");
        String cell = dmgr.getKeyProperty("cell");
        if(cell != null) {
            String query = "WebSphere:processType=ManagedProcess,cell=" + cell + ",*";
            String chStatsNode = "";
            String chStatsHost = "";
            String chStatsCell = "";
            String path = "";
            String host = "";
            for (ObjectName objectName : this.mbeansManager.getMBeans(query)) {
                chStatsNode = objectName.getKeyProperty("node");
                chStatsHost = Utils.getHostByNode(chStatsNode);
                chStatsCell = objectName.getKeyProperty("cell");
                if (path != null && path.length() > 0) {
                    if ((chStatsNode != null && chStatsNode.length() > 0) && (chStatsHost != null && chStatsHost.length() > 0) && (chStatsCell != null && chStatsCell.length() > 0)) {
                        path = chStatsCell + ".ch_stats";
                        host = chStatsHost;
                    }
                }
            }

        }

    }

    public void testt() {
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        MemoryMXBean mbs = ManagementFactory.getMemoryMXBean();
        CompilationMXBean cmp = ManagementFactory.getCompilationMXBean();
        ClassLoadingMXBean classl = ManagementFactory.getClassLoadingMXBean();
        List<MemoryManagerMXBean> mmbs = ManagementFactory.getMemoryManagerMXBeans();
        List<MemoryPoolMXBean> mpbs = ManagementFactory.getMemoryPoolMXBeans();
        OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
        MBeanServer mbser = ManagementFactory.getPlatformMBeanServer();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean tmg = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();


        System.out.println("########## List GarbageCollectorMXBean ##########");
        for(GarbageCollectorMXBean gc:gcs){
            System.out.println("GC NAME " + gc.getName());
            System.out.println("GC COLLECTION COUNT: " + gc.getCollectionCount());
            System.out.println("GC COLLECTION TIME: " + gc.getCollectionTime());
            for(String mp: gc.getMemoryPoolNames()){
                System.out.println("GC MEMORI POOL NAME: " + mp);
            }
        }
        System.out.println("############################################\n");

        System.out.println("########## MemoryMXBean ##########");
        System.out.println("HEAP MEMORY USAGE: " + mbs.getHeapMemoryUsage());
        System.out.println("NON HEAP MEMORY USAGE: " + mbs.getNonHeapMemoryUsage());
        System.out.println("OBJECT PENDING FINALIZATION COUNT: " + mbs.getObjectPendingFinalizationCount());
        System.out.println("############################################\n");

        System.out.println("########## CompilationMXBean ##########");
        System.out.println("NAME: " + cmp.getName());
        System.out.println("TOTAL COMPILATION TIME: " + cmp.getTotalCompilationTime());
        System.out.println("############################################\n");

        System.out.println("########## ClassLoadingMXBean ##########");
        System.out.println("LOADED CLASS COUNT: " + classl.getLoadedClassCount());
        System.out.println("TOTAL LOADED CLASS COUNT: " + classl.getTotalLoadedClassCount());
        System.out.println("UNLOADED CLASS COUNT: " + classl.getUnloadedClassCount());
        System.out.println("############################################\n");

        System.out.println("########## List MemoryManagerMXBean ##########");
        for(MemoryManagerMXBean mmb: mmbs){
            System.out.println("NAME: " + mmb.getName().toString());
            for(String g: mmb.getMemoryPoolNames()){
                System.out.println("MEMORY POOL NAMES: " + g);
            }
            System.out.println("MEMORY POOL NAMES: " + mmb.getMemoryPoolNames());
        }
        System.out.println("############################################\n");

        System.out.println("########## List MemoryPoolMXBean ##########");
        for(MemoryPoolMXBean mpb:mpbs){
            System.out.println("NAME: " + mpb.getName());
            System.out.println("COLLECTION USAGE: " + mpb.getCollectionUsage());
            //System.out.println("COLLECTION USAGE THRESHOLD: " + mpb.getCollectionUsageThreshold());
            //System.out.println("COLLECTION USAGE THRESHOLD COUNT: " + mpb.getCollectionUsageThresholdCount());
            for(String names: mpb.getMemoryManagerNames()) {
                System.out.println("MEMORY MANAGER NAMES: " + names);
            }
            System.out.println("PEAK USAGE: " + mpb.getPeakUsage());
            System.out.println("TYPE: " + mpb.getType());
            System.out.println("USAGE: " + mpb.getUsage());
        }
        System.out.println("############################################\n");

        System.out.println("########## OperatingSystemMXBean ##########");
        System.out.println("NAME: " + osb.getName());
        System.out.println("ARCH: " + osb.getArch());
        System.out.println("VERSION: " + osb.getVersion());
        System.out.println("AVAILABLEPROCESSORS: " + osb.getAvailableProcessors());
        //System.out.println("SYSTEM LOAD AVERAGE: " + osb.getSystemLoadAverage());
        System.out.println("############################################\n");

        System.out.println("########## MBeanServer ##########");
        System.out.println("DEFAULT DOMAIN: " + mbser.getDefaultDomain());
        System.out.println("TO STRING: " + mbser.toString());
        System.out.println("CASI LOS MISMOS METODOS QUE ADMINCLIENT");
        System.out.println("############################################\n");

        System.out.println("########## runtimeMXBean ##########");
        System.out.println("NAME: " + runtimeMXBean.getName());
        System.out.println("BOOTCLASSPATH: " + runtimeMXBean.getBootClassPath());
        System.out.println("CLASS PATH: " + runtimeMXBean.getClassPath());
        System.out.println("LIBRARY PATH: " + runtimeMXBean.getLibraryPath());
        System.out.println("MANAGER SPEC VERSION: " + runtimeMXBean.getManagementSpecVersion());
        System.out.println("SPEC NAME: " + runtimeMXBean.getSpecName());
        System.out.println("SPEC VENDOR: " + runtimeMXBean.getSpecVendor());
        System.out.println("VM NAME: " + runtimeMXBean.getVmName());
        System.out.println("VM VENDOR: " + runtimeMXBean.getVmVendor());
        System.out.println("START TIME: " + runtimeMXBean.getStartTime());
        System.out.println("UP TIME: " + runtimeMXBean.getUptime());
        System.out.println("TAMBIEN HAY SYSTEM PROPIEDADES Y INPUTS ARGUMENTS");
        System.out.println("############################################\n");

        System.out.println("########## ThreadMXBean ##########");
//        for(Long l: tmg.findDeadlockedThreads()){
//            System.out.println("DEAD LOCKED THREADS: " + l);
//        }
//        for(Long l: tmg.findMonitorDeadlockedThreads()){
//            System.out.println("MONITOR DEAD LOCKED THREADS: " + l);
//        }
        for(Long l: tmg.getAllThreadIds()){
            System.out.println("ALL THREADS IDS: " + l);
        }
        System.out.println("CURRENT THREAD CPU TIME: " + tmg.getCurrentThreadCpuTime());
        System.out.println("CURRENT THREAD USER TIME: " + tmg.getCurrentThreadUserTime());
        System.out.println("DAEMON THREAD COUNT: " + tmg.getDaemonThreadCount());
        System.out.println("PEAK THREAD COUNT: " + tmg.getPeakThreadCount());
        System.out.println("THREAD COUNT: " + tmg.getThreadCount());
        System.out.println("############################################\n");

        System.out.println("########## Runtime ##########");
        System.out.println("AVAILABLE PROCESSORS: " + runtime.availableProcessors());
        System.out.println("FREEMEMORY: " + runtime.freeMemory());
        System.out.println("MAXMEMORY" + runtime.maxMemory());
        System.out.println("TOTAL MEMORY: " + runtime.totalMemory());
        System.out.println("############################################\n");

    }

    public TestMetrics(WASConnection wasConnection) {
        this.mbeansManager = new MBeansManager(wasConnection);
    }
}
