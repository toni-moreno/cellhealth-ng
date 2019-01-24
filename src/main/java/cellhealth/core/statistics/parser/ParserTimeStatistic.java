package cellhealth.core.statistics.parser;

import cellhealth.core.statistics.Stats;
import cellhealth.utils.properties.xml.PmiStatsType;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.websphere.pmi.stat.WSTimeStatistic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alberto Pascual on 13/08/15.
 */
public class ParserTimeStatistic<E extends WSTimeStatistic> extends AbstractParser<E>{


    private String metricName;
    private E parserStatistic;
    private Map<String, Boolean> mapPmiStatsType;
    private String metricSeparator;
    private String node;
    private String prefix;
    private String unity;
    //InfluxDB vars
    private String measurement;
    private Map<String, String> tags = new HashMap<String, String>();

    public ParserTimeStatistic(PmiStatsType pmiStatsType, WSStatistic wsStatistic, String node, String prefix, String metricName) {
        this.metricName = metricName;
        this.metricSeparator = getMetricSeparator(pmiStatsType);
        this.parserStatistic =  (E) wsStatistic;
        this.unity = this.getUnity(pmiStatsType, this.parserStatistic);
        this.mapPmiStatsType = pmiStatsType.getTimeStatistic();
        this.prefix = prefix;
        this.node = node;
    }

    public ParserTimeStatistic(PmiStatsType pmiStatsType, WSStatistic wsStatistic, String node, String prefix, String metricName, String measurement, Map<String, String> tags) {
        this.metricName = metricName;
        this.metricSeparator = getMetricSeparator(pmiStatsType);
        this.parserStatistic =  (E) wsStatistic;
        this.unity = this.getUnity(pmiStatsType, this.parserStatistic);
        this.mapPmiStatsType = pmiStatsType.getTimeStatistic();
        this.prefix = prefix;
        this.node = node;
        this.measurement = measurement;
        this.tags = tags;
    }

    public List<Stats> getStatistic() {
        List<Stats> result = new LinkedList<Stats>();
        for (Map.Entry<String,Boolean> entry : this.mapPmiStatsType.entrySet()) {
            if(entry.getValue() != null && entry.getValue()){
                String method = entry.getKey();
                Stats stats = new Stats();
                stats.setHost(this.node);
                stats.setMeasurement(this.measurement);
                this.tags.put("host", this.node);
                String metric = "";
                if("count".equals(method)){
                    metric = String.valueOf(this.parserStatistic.getCount());
                } else if("total".equals(method)) {
                    metric = String.valueOf(this.parserStatistic.getTotal());
                } else if("min".equals(method)) {
                    metric = String.valueOf(this.parserStatistic.getMin());
                } else if("max".equals(method)){
                    metric = String.valueOf(this.parserStatistic.getMax());
                }
                if (metric.length() > 0) {
                    stats.addField(this.metricName, new Long(metric));
                }
                this.tags.put("statMethod", method);
                if (this.unity.length() > 0 && this.unity.trim().length() > 0) {
                	this.tags.put("unit", this.unity.trim().substring(1));
                }
                stats.setTags(this.tags);
                long lTime = System.currentTimeMillis();
                stats.setTime(new Long(lTime));
                stats.setMetric(this.prefix + "." + this.metricName + this.metricSeparator + method + this.unity + metric + " " + lTime / 1000L);
                result.add(stats);
            }
        }
        return result;
    }
}
