package cellhealth.core.statistics.chStats;

import cellhealth.utils.logs.L4j;
import cellhealth.utils.Utils;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.influxdb.dto.Point.Builder;

/**
 * Class used to store information for a list of self metrics
 * For Graphite server a list of Strings is used to store this information
 * For InfluxDB server a list of Points is used to store this information
 */
public class Stats {

    //Graphite vars
    private List<String> stats;
    private String pathChStats;
    private int metrics;
    private String host;
    private String cell;
    //InfluxDB vars
    private String measurement;
    private Map<String, String> tags = new HashMap<String, String>();
    private Map<String, Object> fields = new HashMap<String, Object>();
	List<Builder> lsBuilderPoints = new LinkedList<Builder>();

    public Stats(){
        this.stats = new LinkedList<String>();
    }

    public List<String> getStats() {
        return stats;
    }
    
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public int getMetrics() {
        return metrics;
    }

    public void count(int count){
        this.metrics = this.metrics + count;
    }

    public String getPathChStats() {
        return pathChStats;
    }

    public void setPathChStats(String pathChStats) {
        this.pathChStats = pathChStats;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public void addField(String key, Object value) {
        this.fields.put(key, value);
    }

    public List<Builder> getBuilderPoints() {
    	return lsBuilderPoints;
    }

    public void add(String name, String metric) {
		L4j.getL4j().debug("chStats.add. Setting Metric to: " + this.pathChStats + name + " " + metric + " " + System.currentTimeMillis() / 1000L);
        this.stats.add(this.pathChStats + name + " " + metric + " " + System.currentTimeMillis() / 1000L);
    }

    public void add(Builder bPoint) {
    	this.lsBuilderPoints.add(bPoint);
    }

    public Builder getInitialBPoint() {
    	Builder bPoint = Utils.getInitialBPoint(this.measurement);
        bPoint.tag("host", this.host);
        bPoint.tag("cell", this.cell);
    	return bPoint;
    }
    
    public void addFieldToBPoint(Builder bPoint, String sFieldName, long lFieldValue) {
    	L4j.getL4j().debug("Stats. addFieldToBPoint. Adding FieldName=FieldValue: " + sFieldName + "=" + lFieldValue);
    	bPoint.addField(sFieldName, lFieldValue);
    	return;
    }
	
    public void getSelfStats(Long globalTime){
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Create a new Point without tags
		Builder bPoint = this.getInitialBPoint();
        long lFreeMemory = runtime.freeMemory();
        long lMaxMemory = runtime.maxMemory();
        long lTotalMemory = runtime.totalMemory();
        long lAvailableProcessors = (new Long(runtime.availableProcessors())).longValue();
        this.add(".os.freememory", String.valueOf(lFreeMemory));
        this.add(".os.maxmemory", String.valueOf(lMaxMemory));
        this.add(".os.totalmemory", String.valueOf(lTotalMemory));
        this.add(".os.availableprocessors" , String.valueOf(lAvailableProcessors));
        this.addFieldToBPoint(bPoint, "os.freememory", lFreeMemory);
        this.addFieldToBPoint(bPoint, "os.maxmemory", lMaxMemory);
        this.addFieldToBPoint(bPoint, "os.totalmemory", lTotalMemory);
        this.addFieldToBPoint(bPoint, "os.availableprocessors", lAvailableProcessors);
        long lHeapUsageInit = memoryMXBean.getHeapMemoryUsage().getInit();
        long lHeapUsageUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long lHeapUsageCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        long lHeapUsageMax = memoryMXBean.getHeapMemoryUsage().getMax();
        long lTotalCompilationTime = compilationMXBean.getTotalCompilationTime();
        long lLoadedClassCount = (new Long(classLoadingMXBean.getLoadedClassCount())).longValue();
        this.add(".jvm.memory.heapUsage_init", String.valueOf(lHeapUsageInit));
        this.add(".jvm.memory.heapUsage_used", String.valueOf(lHeapUsageUsed));
        this.add(".jvm.memory.heapUsage_committed", String.valueOf(lHeapUsageCommitted));
        this.add(".jvm.memory.heapUsage_max", String.valueOf(lHeapUsageMax));
        this.add(".jvm.compiler.total_compilation_time", String.valueOf(lTotalCompilationTime));
        this.add(".jvm.classloading.class_count", String.valueOf(lLoadedClassCount));
        this.addFieldToBPoint(bPoint, "jvm.memory.heapUsage_init", lHeapUsageInit);
        this.addFieldToBPoint(bPoint, "jvm.memory.heapUsage_used", lHeapUsageUsed);
        this.addFieldToBPoint(bPoint, "jvm.memory.heapUsage_committed", lHeapUsageCommitted);
        this.addFieldToBPoint(bPoint, "jvm.memory.heapUsage_max", lHeapUsageMax);
        this.addFieldToBPoint(bPoint, "jvm.compiler.total_compilation_time", lTotalCompilationTime);
        this.addFieldToBPoint(bPoint, "jvm.classloading.class_count", lLoadedClassCount);
        long lDaemonThreadCount = threadMXBean.getDaemonThreadCount();
        long lThreadCount = threadMXBean.getThreadCount();
        this.add(".jvm.threads.daemon_thread_count", String.valueOf(lDaemonThreadCount));
        this.add(".jvm.threads.thread_count", String.valueOf(lThreadCount));
        this.addFieldToBPoint(bPoint, "jvm.threads.daemon_thread_count", lDaemonThreadCount);
        this.addFieldToBPoint(bPoint, "jvm.threads.thread_count", lThreadCount);
    	this.add(bPoint);

    	long lNumMetrics = (new Long(this.getMetrics()).longValue());
        this.add(".metrics.global.retrieve_time", String.valueOf(globalTime));
        this.add(".metrics.global.number_metrics", String.valueOf(lNumMetrics));
        // Create a new Point for tag "server"
		bPoint = this.getInitialBPoint();
        bPoint.tag("server", "global");
        this.addFieldToBPoint(bPoint, "metrics.retrieve_time", globalTime.longValue());
        this.addFieldToBPoint(bPoint, "metrics.number_metrics", lNumMetrics);
    	this.add(bPoint);

        // Create a new Point for each tag "bean"
        for(GarbageCollectorMXBean garbageCollectorMXBean: ManagementFactory.getGarbageCollectorMXBeans()){
            String name = garbageCollectorMXBean.getName().replace(" ", "_");
            long lCollectionCount = garbageCollectorMXBean.getCollectionCount();
            long lCollectionTime = garbageCollectorMXBean.getCollectionTime();
            this.add(".jvm.gc." + name + ".gc_collection_count", String.valueOf(lCollectionCount));
            this.add(".jvm.gc." + name + ".gc_collection_time", String.valueOf(lCollectionTime));
    		bPoint = this.getInitialBPoint();
            bPoint.tag("bean", name);
            this.addFieldToBPoint(bPoint, "jvm.gc.gc_collection_count", lCollectionCount);
            this.addFieldToBPoint(bPoint, "jvm.gc.gc_collection_time", lCollectionTime);
        	this.add(bPoint);
        }

        // Create a new Point for each tag "bean"
    	for(MemoryPoolMXBean memoryPoolMXBean:ManagementFactory.getMemoryPoolMXBeans()){
            String name = memoryPoolMXBean.getName().replace(" ", "_");
    		bPoint = this.getInitialBPoint();
            bPoint.tag("bean", name);
            long lMemoryPoolInit = memoryPoolMXBean.getUsage().getInit();
            long lMemoryPoolUsed = memoryPoolMXBean.getUsage().getUsed();
            long lMemoryPoolCommitted = memoryPoolMXBean.getUsage().getCommitted();
            long lMemoryPoolMax = memoryPoolMXBean.getUsage().getMax();
            this.add(".jvm.memoryPool." + name + ".init", String.valueOf(lMemoryPoolInit));
            this.add(".jvm.memoryPool." + name + ".used", String.valueOf(lMemoryPoolUsed));
            this.add(".jvm.memoryPool." + name + ".committed", String.valueOf(lMemoryPoolCommitted));
            this.add(".jvm.memoryPool." + name + ".max", String.valueOf(lMemoryPoolMax));
            this.addFieldToBPoint(bPoint, "jvm.memoryPool.init", lMemoryPoolInit);
            this.addFieldToBPoint(bPoint, "jvm.memoryPool.used", lMemoryPoolUsed);
            this.addFieldToBPoint(bPoint, "jvm.memoryPool.committed", lMemoryPoolCommitted);
            this.addFieldToBPoint(bPoint, "jvm.memoryPool.max", lMemoryPoolMax);
        	this.add(bPoint);
        }
    }
}
