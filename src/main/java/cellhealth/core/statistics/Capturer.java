package cellhealth.core.statistics;

import cellhealth.core.connection.MBeansManager;
import cellhealth.core.statistics.parser.ParserWSStatistics;
import cellhealth.utils.Utils;
import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.xml.CellHealthMetrics;
import cellhealth.utils.properties.xml.Metric;
import cellhealth.utils.properties.xml.MetricGroup;
import cellhealth.utils.properties.xml.PmiStatsType;
import com.ibm.websphere.management.exception.ConnectorNotAvailableException;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.websphere.pmi.stat.WSStats;

import javax.management.JMRuntimeException;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Capturer {

    private MBeansManager mbeansManager;
    private String serverName;
    private String node;
    private List<MetricGroup> metricGroups;
    private PmiStatsType pmiStatsType;
    private ObjectName serverPerfMBean;
    private String host;
    private String prefix;

    public Capturer(MBeansManager mbeansManager, String node, String serverName, CellHealthMetrics cellHealthMetrics) {
        this.mbeansManager = mbeansManager;
        this.serverName = serverName;
        this.node = node;
        this.metricGroups = cellHealthMetrics.getMetricGroups();
        this.pmiStatsType = cellHealthMetrics.getPmiStatsType();
        //Obtiene el host de un servidor
        this.host = Utils.getHostByNode(node);
    }

    /**
     * Metodo que obtiene las estadisticas de servidor pasado por parametros, menos DMGR
     * @return listado de estadisticas
     */
    public List<Stats> getMetrics() {
        List<Stats> stats = null;
        if (!"dmgr".equals(this.serverName)) {
            stats = getStats(getWSStats());
        }
        return stats;
    }

    private WSStats getWSStats() {
        WSStats wsStats = null;
        try {
            this.serverPerfMBean = mbeansManager.getMBean("WebSphere:type=Perf,node=" + this.node + ",process=" + this.serverName + ",*");
            ObjectName objectName = mbeansManager.getMBean("WebSphere:name=" + this.serverName + ",node=" + this.node + ",process=" + this.serverName + ",type=Server,*");
            if (objectName != null) {
                String[] signature = new String[]{"javax.management.ObjectName", "java.lang.Boolean"};
                Object[] params = new Object[]{objectName, true};
                try {
                    wsStats = (WSStats) mbeansManager.getClient().invoke(serverPerfMBean, "getStatsObject", params, signature);
                } catch (JMRuntimeException e) {
                    L4j.getL4j().error("JMRuntime Exception ( Credentials maybe?) EXITING ",e);
                    System.exit(-1);
                } catch (Exception e) {
                    L4j.getL4j().error("Capturer Generic Exception ", e);
                }
            }
        } catch (ConnectorNotAvailableException e) {
                L4j.getL4j().error("Connector not available",e);
        } catch (ConnectorException e) {
                L4j.getL4j().error("GENERIC Connector exception",e);
        }
        
        return wsStats;
    }

    private List<Stats> getStats(WSStats wsStats) {
        List<Stats> stats = new LinkedList<Stats>();
        if (wsStats != null) {
            for (MetricGroup metricGroup : this.metricGroups) {
                WSStats especificStats = wsStats.getStats(metricGroup.getStatsType());
                if(especificStats != null){
                    stats.addAll(getStatsType(metricGroup, especificStats, true));
                } else {
                    L4j.getL4j().debug("Node: " + this.node + " Server: " + this.serverName + " Not found statstype " + metricGroup.getStatsType());
                }
            }
        } else {
            L4j.getL4j().warning("Node: " + this.node + " Server: " + this.serverName + " Not found stats");
        }
        return stats;
    }

    public List<Stats> getStatsType(MetricGroup metricGroup, WSStats wsStats, boolean isInstance) {
        List<Stats> result = new LinkedList<Stats>();
        List<Stats> globalStats;
        List<Stats> instances;
        this.prefix = this.serverPerfMBean.getKeyProperty("cell") + "." + this.serverPerfMBean.getKeyProperty("process") + "." + metricGroup.getPrefix();
        globalStats = getGlobalStats(wsStats, metricGroup, this.prefix);
        if(globalStats.size() > 0) {
            result.addAll(globalStats);
        }
        if(wsStats.getSubStats().length > 0){
            instances = getInstances(Arrays.asList(wsStats.getSubStats()), metricGroup, this.prefix, isInstance);
            if(instances.size() > 0){
                result.addAll(instances);
            }
        }
        return result;
    }

    public synchronized List<Stats> getInstances(List<WSStats> wsStats, MetricGroup metricGroup, String path, boolean isInstance) {
        List<Stats> result = new LinkedList<Stats>();
        for(WSStats substats: wsStats){
            String auxPath = path + "." + Utils.getParseBeanName(substats.getName());
            if (!Utils.listContainsReg(metricGroup.getInstanceExclude(), substats.getName())) {
                if (isInstance) {
                    if (metricGroup.getInstanceInclude() != null && metricGroup.getInstanceInclude().size() > 0 && Utils.listContainsReg(metricGroup.getInstanceInclude(), substats.getName())) {
                        if (metricGroup.getAllowGlobal() && substats.getSubStats().length > 0) {
                            for (Metric metric : metricGroup.getMetrics()) {
                                WSStatistic wsStatistic = substats.getStatistic(metric.getId());
                                if (wsStatistic != null) {
                                    String metricName = (metric.getName() == null || metric.getName().length() == 0) ? wsStatistic.getName() : metric.getName();
                                    ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), auxPath, metricName);
                                    result.addAll(parserWSStatistics.parseStatistics());
                                }
                            }
                        } else if (substats.getSubStats().length == 0) {
                            for (Metric metric : metricGroup.getMetrics()) {
                                WSStatistic wsStatistic = substats.getStatistic(metric.getId());
                                if (wsStatistic != null) {
                                    String metricName = (metric.getName() == null || metric.getName().length() == 0) ? wsStatistic.getName() : metric.getName();
                                    ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), auxPath, metricName);
                                    result.addAll(parserWSStatistics.parseStatistics());
                                }
                            }
                        }
                        if (substats.getSubStats().length > 0) {
                            result.addAll(getInstances(Arrays.asList(substats.getSubStats()), metricGroup, auxPath, false));
                        }
                    } else if (metricGroup.getInstanceInclude() == null || metricGroup.getInstanceInclude().size() == 0) {
                        if (metricGroup.getAllowGlobal() && substats.getSubStats().length > 0) {
                            for (Metric metric : metricGroup.getMetrics()) {
                                WSStatistic wsStatistic = substats.getStatistic(metric.getId());
                                if (wsStatistic != null) {
                                    String metricName = (metric.getName() == null || metric.getName().length() == 0) ? wsStatistic.getName() : metric.getName();
                                    ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), auxPath, metricName);
                                    result.addAll(parserWSStatistics.parseStatistics());
                                }
                            }
                        } else if (substats.getSubStats().length == 0) {
                            for (Metric metric : metricGroup.getMetrics()) {
                                WSStatistic wsStatistic = substats.getStatistic(metric.getId());
                                if (wsStatistic != null) {
                                    String metricName = (metric.getName() == null || metric.getName().length() == 0) ? wsStatistic.getName() : metric.getName();
                                    ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), auxPath, metricName);
                                    result.addAll(parserWSStatistics.parseStatistics());
                                }
                            }
                        }
                        if (substats.getSubStats().length > 0) {
                            result.addAll(getInstances(Arrays.asList(substats.getSubStats()), metricGroup, auxPath, false));
                        }
                    }
                } else {
                    for (Metric metric : metricGroup.getMetrics()) {
                        WSStatistic wsStatistic = substats.getStatistic(metric.getId());
                        if (wsStatistic != null) {
                            String metricName = (metric.getName() == null || metric.getName().length() == 0) ? wsStatistic.getName() : metric.getName();
                            ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), auxPath, metricName);
                            result.addAll(parserWSStatistics.parseStatistics());
                        }
                    }
                    if (substats.getSubStats().length > 0) {
                        result.addAll(getInstances(Arrays.asList(substats.getSubStats()), metricGroup, auxPath, false));
                    }
                }
            }
        }
        return result;
    }

    public List<Stats> getGlobalStats(WSStats wsStats, MetricGroup metricGroup, String path) {
        List<Stats> result = new LinkedList<Stats>();
        if(metricGroup.getAllowGlobal()){
            for (Metric metric : metricGroup.getMetrics()) {
                WSStatistic wsStatistic = wsStats.getStatistic(metric.getId());
                String metricName = (metric.getName() == null || metric.getName().length() == 0)?wsStatistic.getName():metric.getName();
                ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), path + ".global", metricName);
                result.addAll(parserWSStatistics.parseStatistics());
            }
        } else if(wsStats.getSubStats().length == 0) {
            for (Metric metric : metricGroup.getMetrics()) {
                WSStatistic wsStatistic = wsStats.getStatistic(metric.getId());
                String metricName = (metric.getName() == null || metric.getName().length() == 0)?wsStatistic.getName():metric.getName();
                ParserWSStatistics parserWSStatistics = new ParserWSStatistics(wsStatistic, this.pmiStatsType, Utils.getHostByNode(this.node), path, metricName);
                result.addAll(parserWSStatistics.parseStatistics());
            }
        }
        return result;
    }

    public String getHost() {
        return this.host;
    }

    public String getPrefix(){
        return this.prefix;
    }

    public MBeansManager getMbeansManager() {
        return mbeansManager;
    }
}