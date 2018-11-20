package cellhealth.core.threads.Metrics;

import cellhealth.core.statistics.Capturer;

import cellhealth.core.statistics.chStats.Stats;
import cellhealth.sender.Sender;
import cellhealth.utils.properties.Settings;
import java.util.List;
import org.influxdb.dto.Point.Builder;


public class MetricsCollector implements Runnable {

    private final Capturer capturer;
    private final Sender sender;
    private final Stats chStats;


    public MetricsCollector(Capturer capturer, Sender sender, Stats chStats) {
        this.capturer = capturer;
        this.sender = sender;
        this.chStats = chStats;
    }

    public void run() {
        long start_time=System.currentTimeMillis();
        List<cellhealth.core.statistics.Stats> metrics = this.capturer.getMetrics();
        long serverIn = (System.currentTimeMillis() - start_time);
        this.sendAllMetricRange(metrics);
        if(this.capturer.getPrefix() != null && Settings.properties().isSelfStats()) {
           // try {
                String[] aux = this.capturer.getPrefix().split("\\.");
                /*
                Map<String,String> chStatsPath = this.capturer.getMbeansManager().getPathHostChStats();
                
                if (this.chStats.getPathChStats() == null) {
                    this.chStats.setPathChStats(chStatsPath.get("path"));
                }
                if (this.chStats.getHost() == null) {
                    this.chStats.setHost(chStatsPath.get("host"));
                }*/
                this.chStats.add(".metrics." + aux[1] + ".retrieve_time", String.valueOf(serverIn));
                this.chStats.add(".metrics." + aux[1] + ".number_metrics", String.valueOf(metrics.size()));
                this.chStats.count(metrics.size());
        		Builder bPoint = this.chStats.getInitialBPoint();
                bPoint.tag("server", aux[1]);
                this.chStats.addFieldToBPoint(bPoint, "metrics.retrieve_time", serverIn);
                this.chStats.addFieldToBPoint(bPoint, "metrics.number_metrics", (new Long(metrics.size()).longValue()));
            	this.chStats.add(bPoint);
            /*}catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error("Connector not available",e);
            }catch (ConnectorException e) {
                L4j.getL4j().error("GENERIC Connector not available",e);
            }*/
        }
    }

    private void sendAllMetricRange(List<cellhealth.core.statistics.Stats> metrics) {
        if(metrics != null) {
            for (cellhealth.core.statistics.Stats stats : metrics) {
                sender.send(stats);
            }
        }
    }
}
