package cellhealth.core.threads.Metrics;

import cellhealth.core.statistics.Capturer;
import cellhealth.core.statistics.chStats.Stats;
import cellhealth.sender.Sender;
import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;
import com.ibm.websphere.management.exception.ConnectorException;

import java.util.List;
import java.util.Map;


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
        if(this.capturer.getPrefix() != null && Settings.propertie().isSelfStats()) {
            try {
                Map<String,String> chStatsPath = this.capturer.getMbeansManager().getPathHostChStats();
                String[] aux = this.capturer.getPrefix().split("\\.");
                if (this.chStats.getPathChStats() == null) {
                    this.chStats.setPathChStats(chStatsPath.get("path"));
                }
                if (this.chStats.getHost() == null) {
                    this.chStats.setHost(chStatsPath.get("host"));
                }
                this.chStats.add(".metrics." + aux[1] + ".retrieve_time", String.valueOf(serverIn));
                this.chStats.add(".metrics." + aux[1] + ".number_metrics", String.valueOf(metrics.size()));
                this.chStats.count(metrics.size());
            }catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error("Connector not available",e);
            }catch (ConnectorException e) {
                L4j.getL4j().error("GENERIC Connector not available",e);
            }
        }
    }

    private void sendAllMetricRange(List<cellhealth.core.statistics.Stats> metrics) {
        if(metrics != null) {
            for (cellhealth.core.statistics.Stats stats : metrics) {
                sender.send(stats.getHost(), stats.getMetric());
            }
        }
    }
}
