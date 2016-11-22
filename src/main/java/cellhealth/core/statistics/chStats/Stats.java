package cellhealth.core.statistics.chStats;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;

public class Stats {

    private List<String> stats;
    private String pathChStats;
    private int metrics;
    private String host;

    public Stats(){
        this.stats = new LinkedList<String>();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getStats() {
        return stats;
    }

    public int getMetrics() {
        return metrics;
    }

    public void count(int count){
        this.metrics = this.metrics + count;
    }

    public void add(String name, String metric) {
        this.stats.add(this.pathChStats + name + " " + metric + " " + System.currentTimeMillis() / 1000L);
    }

    public String getPathChStats() {
        return pathChStats;
    }

    public void setPathChStats(String pathChStats) {
        this.pathChStats = pathChStats;
    }

    public void getSelfStats(String globalTime){
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        this.add(".metrics.global.retrieve_time", String.valueOf(globalTime));
        this.add(".metrics.global.number_metrics", String.valueOf(this.getMetrics()));
        this.add(".os.freememory", String.valueOf(runtime.freeMemory()));
        this.add(".os.maxmemory", String.valueOf(runtime.maxMemory()));
        this.add(".os.totalmemory", String.valueOf(""));
        this.add(".os.availableprocessors" , String.valueOf(runtime.availableProcessors()));
        for(GarbageCollectorMXBean garbageCollectorMXBean: ManagementFactory.getGarbageCollectorMXBeans()){
            String name = garbageCollectorMXBean.getName().replace(" ", "_");
            this.add(".jvm.gc." + name + ".gc_collection_count", String.valueOf(garbageCollectorMXBean.getCollectionCount()));
            this.add(".jvm.gc." + name + ".gc_collection_time", String.valueOf(garbageCollectorMXBean.getCollectionTime()));
        }
        this.add(".jvm.memory.heapUsage_init", String.valueOf(memoryMXBean.getHeapMemoryUsage().getInit()));
        this.add(".jvm.memory.heapUsage_used", String.valueOf(memoryMXBean.getHeapMemoryUsage().getUsed()));
        this.add(".jvm.memory.heapUsage_committed", String.valueOf(memoryMXBean.getHeapMemoryUsage().getCommitted()));
        this.add(".jvm.memory.heapUsage_max", String.valueOf(memoryMXBean.getHeapMemoryUsage().getMax()));
        this.add(".jvm.compiler.total_compilation_time", String.valueOf(compilationMXBean.getTotalCompilationTime()));
        this.add(".jvm.classloading.class_count", String.valueOf(classLoadingMXBean.getLoadedClassCount()));

        for(MemoryPoolMXBean memoryPoolMXBean:ManagementFactory.getMemoryPoolMXBeans()){
            String name = memoryPoolMXBean.getName().replace(" ", "_");
            this.add(".jvm.memoryPool." + name + ".init", String.valueOf(memoryPoolMXBean.getUsage().getInit()));
            this.add(".jvm.memoryPool." + name + ".used", String.valueOf(memoryPoolMXBean.getUsage().getUsed()));
            this.add(".jvm.memoryPool." + name + ".committed", String.valueOf(memoryPoolMXBean.getUsage().getCommitted()));
            this.add(".jvm.memoryPool." + name + ".max", String.valueOf(memoryPoolMXBean.getUsage().getMax()));
        }
        this.add(".jvm.threads.daemon", String.valueOf(threadMXBean.getDaemonThreadCount()));
        this.add(".jvm.threads.thread_count", String.valueOf(threadMXBean.getThreadCount()));
    }
}
