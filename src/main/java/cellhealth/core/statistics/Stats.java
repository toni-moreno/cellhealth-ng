package cellhealth.core.statistics;

import java.util.HashMap;
import java.util.Map;

import cellhealth.utils.logs.L4j;

/**
 * Created by Alberto Pascual on 12/08/15.
 * Class used to store information for a single metric from metrics.xml
 * For Graphite server the variables host and metric are used to store this information
 * For InfluxDB server the variables measurement, tags, fields and time are used to store this information
 */
public class Stats {

    //Graphite vars
	private String metric;
    private String host;
    //InfluxDB vars
    private String measurement;
    private Map<String, String> tags = new HashMap<String, String>();
    private Map<String, Object> fields = new HashMap<String, Object>();
    private Long time;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
		L4j.getL4j().debug("Stats.setMetric. Setting Metric to: " + metric);
        this.metric = metric;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
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
}
