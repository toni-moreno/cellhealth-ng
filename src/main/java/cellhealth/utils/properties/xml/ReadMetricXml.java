package cellhealth.utils.properties.xml;

import cellhealth.utils.logs.L4j;
import cellhealth.utils.properties.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by alberto on 1/07/15.
 */
public class ReadMetricXml {

    private Document dom;
    private CellHealthMetrics cellHealthMetrics;

    public ReadMetricXml() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.dom = db.parse(Settings.getInstance().getPathConf() + "metrics.xml");
            this.getMetricGroup();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void getMetricGroup() {
        this.cellHealthMetrics = new CellHealthMetrics();
        List<MetricGroup> metricProperties = new LinkedList<MetricGroup>();
        PmiStatsType pmiStatsType = new PmiStatsType();
        Element root = this.dom.getDocumentElement();
        NodeList nodeList = root.getChildNodes();
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if ("MetricGroup".equals(node.getNodeName())) {
                        Element element = (Element) node;
                        metricProperties.add(getMetricGroupProperties(node, element));
                    }
                    if ("PMIStatsType".equals(node.getNodeName())) {
                        Element element = (Element) node;
                        boolean unit = getBooleanAttribute(element, "unit");
                        String unitSeparator = getStringAttribute(element, "unitseparator");
                        boolean separateMetric = getBooleanAttribute(element, "separateMetric");
                        pmiStatsType = getPmiStatsTypeProperties(node, unit, unitSeparator, separateMetric);
                    }
                }

            }
        }
        this.cellHealthMetrics.setMetricGroups(metricProperties);
        this.cellHealthMetrics.setPmiStatsType(pmiStatsType);
    }

    private MetricGroup getMetricGroupProperties(Node node, Element element) {
        MetricGroup metricGroup = new MetricGroup();
        metricGroup.setStatsType(getStringValue(element, "StatsType"));
        metricGroup.setAllowGlobal(getBooleanValue(element, "AllowGlobal"));
        metricGroup.setPrefix(getStringValue(element, "Prefix"));
        metricGroup.setUniqueInstance(getBooleanValue(element, "UniqueInstance"));
        metricGroup.setInstanceInclude(getInstanceFilter(element, "InstanceInclude"));
        metricGroup.setInstanceExclude(getInstanceFilter(element, "InstanceExclude"));
        List<Metric> metrics = getMetriGroupMetrics(node.getChildNodes());
        metricGroup.setMetrics(metrics);
        if(metricGroup.getStatsType() == null || metricGroup.getPrefix() == null) {
            L4j.getL4j().error("Metrics.xml", new NullPointerException());
        }
        return metricGroup;
    }

    private PmiStatsType getPmiStatsTypeProperties(Node node, boolean unit, String unitSeparator, boolean separateMetric){
        PmiStatsType pmiStatsType = new PmiStatsType();
        pmiStatsType.setUnit(unit);
        pmiStatsType.setUnitSeparator(unitSeparator);
        pmiStatsType.setSeparateMetric(separateMetric);
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node no = nodeList.item(i);
            if(no.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) no;
                if ("CountStatistic".equals(no.getNodeName())) {
                    pmiStatsType.setCountStatistic("count", getBooleanAttribute(element, "count"));
                } else if ("DoubleStatistic".equals(element.getTagName())) {
                    pmiStatsType.setDoubleStatistic("count", getBooleanAttribute(element, "count"));
                } else if ("AverageStatistic".equals(no.getNodeName())) {
                    Map<String, Boolean> averageStatistic = new HashMap<String, Boolean>();
                    averageStatistic.put("count", getBooleanAttribute(element, "count"));
                    averageStatistic.put("total", getBooleanAttribute(element, "total"));
                    averageStatistic.put("min", getBooleanAttribute(element, "min"));
                    averageStatistic.put("max", getBooleanAttribute(element, "max"));
                    pmiStatsType.setAverageStatistic(averageStatistic);
                } else if ("BoundaryStatistic".equals(no.getNodeName())) {
                    Map<String, Boolean> boundaryStatistic = new HashMap<String, Boolean>();
                    boundaryStatistic.put("upperBound", getBooleanAttribute(element, "upperBound"));
                    boundaryStatistic.put("lowerBound", getBooleanAttribute(element, "lowerBound"));
                    pmiStatsType.setBoundaryStatistic(boundaryStatistic);
                } else if ("RangeStatistic".equals(no.getNodeName())) {
                    Map<String, Boolean> rangeStatistic = new HashMap<String, Boolean>();
                    rangeStatistic.put("highWaterMark", getBooleanAttribute(element, "highWaterMark"));
                    rangeStatistic.put("lowWaterMark", getBooleanAttribute(element, "lowWaterMark"));
                    rangeStatistic.put("current", getBooleanAttribute(element, "current"));
                    rangeStatistic.put("integral", getBooleanAttribute(element, "integral"));
                    pmiStatsType.setRangeStatistic(rangeStatistic);
                } else if ("TimeStatistic".equals(no.getNodeName())) {
                    Map<String, Boolean> timeStatistic = new HashMap<String, Boolean>();
                    timeStatistic.put("count", getBooleanAttribute(element, "count"));
                    timeStatistic.put("total", getBooleanAttribute(element, "total"));
                    timeStatistic.put("min", getBooleanAttribute(element, "min"));
                    timeStatistic.put("max", getBooleanAttribute(element, "max"));
                    pmiStatsType.setTimeStatistic(timeStatistic);
                } else if ("BoundedRangeStatistic".equals(no.getNodeName())) {
                    Map<String, Boolean> boundedRangeStatistic = new HashMap<String, Boolean>();
                    boundedRangeStatistic.put("upperBound", getBooleanAttribute(element, "upperBound"));
                    boundedRangeStatistic.put("lowerBound", getBooleanAttribute(element, "lowerBound"));
                    boundedRangeStatistic.put("highWaterMark", getBooleanAttribute(element, "highWaterMark"));
                    boundedRangeStatistic.put("lowWaterMark", getBooleanAttribute(element, "lowWaterMark"));
                    boundedRangeStatistic.put("current", getBooleanAttribute(element, "current"));
                    boundedRangeStatistic.put("integral", getBooleanAttribute(element, "integral"));
                    pmiStatsType.setBoundedRangeStatistic(boundedRangeStatistic);
                }
            }
        }
        return pmiStatsType;
    }

    private List<Metric> getMetriGroupMetrics(NodeList nodeList) {
        List<Metric> metrics = new LinkedList<Metric>();
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if ("Metrics".equals(node.getNodeName())) {
                        metrics = getMetriGroupMetrics(node.getChildNodes());
                    } else if ("Metric".equals(node.getNodeName())) {
                        Metric metric = getMetricGroupMetricProperties((Element) node);
                        metrics.add(metric);
                    }
                }
            }
        }
        return metrics;
    }

    private Metric getMetricGroupMetricProperties(Element element) {
        Metric metric = new Metric();
        String id = element.getAttribute("id");
        if(id == null){
            L4j.getL4j().error("Metrics.xml", new NullPointerException());
        }
        metric.setId(Integer.parseInt(id));
        metric.setName(getStringValue(element, "Name"));
        metric.setScale(getIntValue(element, "Scale"));
        return metric;
    }

    private String getStringValue(Element element, String tagName) {
        String string = null;
        NodeList nl = element.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element aux = (Element) nl.item(0);
            string = aux.getFirstChild().getNodeValue();
        }
        return string;
    }

    private boolean getBooleanValue(Element element, String tagName) {
        String bool = "false";
        if(getStringValue(element, tagName) != null){
            bool = getStringValue(element, tagName);
        }
        return Boolean.valueOf(bool);
    }

    private int getIntValue(Element element, String tagName) {
        if(getStringValue(element, tagName) != null) {
            return Integer.parseInt(getStringValue(element, tagName));
        }
        return 0;
    }

    private List<String> getInstanceFilter(Element element, String tagName) {
        List<String> listInstances = new LinkedList<String>();
        tagName = getStringValue(element, tagName);
        if(tagName != null) {
            String aux = tagName.replace(" ", "");
            for (String split : aux.split(",")) {
                split = split.replace(",", "");
                listInstances.add(split);
            }
        }
        return listInstances;
    }

    private boolean getBooleanAttribute(Element element, String tagName) {
        String bool = "false";
        if(getStringAttribute(element, tagName) != null){
            bool = getStringAttribute(element, tagName);
        }
        return Boolean.valueOf(bool);
    }

    private String getStringAttribute(Element element, String tagName) {
        return element.getAttribute(tagName);
    }

    public CellHealthMetrics getCellHealthMetrics() {
        return cellHealthMetrics;
    }
}