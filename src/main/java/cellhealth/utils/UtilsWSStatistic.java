package cellhealth.utils;

import cellhealth.core.statistics.Stats;
import cellhealth.utils.properties.xml.PmiStatsType;
import com.ibm.websphere.pmi.stat.WSAverageStatistic;
import com.ibm.websphere.pmi.stat.WSBoundaryStatistic;
import com.ibm.websphere.pmi.stat.WSBoundedRangeStatistic;
import com.ibm.websphere.pmi.stat.WSCountStatistic;
import com.ibm.websphere.pmi.stat.WSDoubleStatistic;
import com.ibm.websphere.pmi.stat.WSRangeStatistic;
import com.ibm.websphere.pmi.stat.WSStatistic;
import com.ibm.websphere.pmi.stat.WSTimeStatistic;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alberto Pascual on 18/07/15.
 */
public class UtilsWSStatistic {

    public static List<Stats> parseStatistics(String prefix, WSStatistic wsStatistic, PmiStatsType pmiStatsType, String node){

//        if(prefix.contains("threadPool")){
//                System.out.println("3: " +prefix);
//        }

        List<Stats> result = new LinkedList<Stats>();
        String type;
        String separator = getMetricSeparator(pmiStatsType);
        if (wsStatistic != null && (wsStatistic.getName() != null )) { //|| pmiStatsType.getMetricName() != null
            String metricName = "";//(pmiStatsType.getMetricName() == null || pmiStatsType.getMetricName().length() == 0)?wsStatistic.getName():pmiStatsType.getMetricName();
            type = Utils.getWSStatisticType(wsStatistic);
            if ("CountStatistic".equals(type)) {
                Map<String, Boolean> mapCounterStatistic = pmiStatsType.getCountStatistic();
                WSCountStatistic countStatistic = (WSCountStatistic) wsStatistic;
                if(mapCounterStatistic.get("count")){
                    String separatorType = (pmiStatsType.isUnit())?"_" + countStatistic.getUnit() + " ":" ";
                    if("N/A".equals(countStatistic.getUnit())) {
                        separatorType = " ";
                    }
                    separatorType = separatorType.toLowerCase();
                    Stats stats = new Stats();
                    stats.setHost(node);
                    stats.setMetric(prefix + "." + metricName + separator +"long" + separatorType + countStatistic.getCount() + " " + System.currentTimeMillis() / 1000L);
                    result.add(stats);
                }
            } else if ("DoubleStatistic".equals(type)) {
                WSDoubleStatistic doubleStatistic = (WSDoubleStatistic) wsStatistic;
                Map<String, Boolean> mapDoubleStatistic = pmiStatsType.getCountStatistic();
                if(mapDoubleStatistic.get("count")) {
                    String separatorType = (pmiStatsType.isUnit())?"_" + doubleStatistic.getUnit() + " ":" ";
                    if("N/A".equals(doubleStatistic.getUnit())) {
                        separatorType = " ";
                    }
                    separatorType = separatorType.toLowerCase();
                    Stats stats = new Stats();
                    stats.setHost(node);
                    stats.setMetric(prefix + "." + metricName + separator +"double" + separatorType + doubleStatistic.getDouble() + " " + System.currentTimeMillis() / 1000L);
                    result.add(stats);
                }
            } else if ("AverageStatistic".equals(type)) {
//                if(stats.getMetric().contains("threadPool")){
//                    System.out.println(stats.getMetric());
//                }
//                if(prefix.contains("threadPool")){
//                    System.out.println("AVE " +prefix);
//                }
                result.addAll(getAverageStatistic(prefix, wsStatistic, pmiStatsType, node));
            } else if ("TimeStatistic".equals(type)) {
//                if(prefix.contains("threadPool")){
//                    System.out.println("TIM " +prefix);
//                }
                result.addAll(getTimeStatistic(prefix, wsStatistic, pmiStatsType, node));
            } else if ("BoundaryStatistic".equals(type)) {
//                if(prefix.contains("threadPool")){
//                    System.out.println("BOU " +prefix);
//                }
                result.addAll(getBoundaryStatistic(prefix, wsStatistic, pmiStatsType, node));
            } else if ("RangeStatistic".equals(type)) {
//                if(prefix.contains("threadPool")){
//                    System.out.println("RAM " +prefix);
//                }
                result.addAll(getRangeStatistic(prefix, wsStatistic, pmiStatsType, node));
            } else if ("BoundedRangeStatistic".equals(type)) {
//                if(prefix.contains("threadPool")){
//                    System.out.println("BOUR " +prefix);
//                }
                result.addAll(getBoundedRangeStatistic(prefix, wsStatistic, pmiStatsType, node));
            }
        } else if(wsStatistic != null){
            //l4j
        }
        return result;
    }

    private static List<Stats> getAverageStatistic(String prefix, WSStatistic wsStatistic, PmiStatsType pmiStatsType, String node) {
        String metricName = "";//(pmiStatsType.getMetricName() == null || pmiStatsType.getMetricName().length() == 0)?wsStatistic.getName():pmiStatsType.getMetricName();
        String metricSeparator = getMetricSeparator(pmiStatsType);
        Map<String, Boolean> mapAverageStatistic = pmiStatsType.getAverageStatistic();
        List<Stats> result = new LinkedList<Stats>();
        WSAverageStatistic averageStatistic = (WSAverageStatistic) wsStatistic;
        String unity = (pmiStatsType.isUnit())?"_" + averageStatistic.getUnit() + " ":" ";
        if("N/A".equals(averageStatistic.getUnit())) {
            unity = " ";
        }
        unity = unity.toLowerCase();
        if(mapAverageStatistic.get("count")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "count" + unity + averageStatistic.getCount() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapAverageStatistic.get("total")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "total" + unity + averageStatistic.getTotal() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapAverageStatistic.get("min")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "min" + unity + averageStatistic.getMin() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapAverageStatistic.get("max")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "max" + unity + averageStatistic.getMax() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        for(Stats stats:result){
            if(stats.getMetric().contains("threadPool")){
                System.out.println("Prefix: " + prefix);
                System.out.println(stats.getMetric());
                System.out.println(metricName);
            }
        }
        return result;
    }

    private static List<Stats> getTimeStatistic(String prefix, WSStatistic wsStatistic, PmiStatsType pmiStatsType, String node) {
        String metricName = "";//(pmiStatsType.getMetricName() == null || pmiStatsType.getMetricName().length() == 0)?wsStatistic.getName():pmiStatsType.getMetricName();
        Map<String, Boolean> mapTimeStatistic = pmiStatsType.getTimeStatistic();
        String metricSeparator = getMetricSeparator(pmiStatsType);
        List<Stats> result = new LinkedList<Stats>();
        WSTimeStatistic timeStatistic = (WSTimeStatistic) wsStatistic;
        String unity = (pmiStatsType.isUnit())?"_" + timeStatistic.getUnit() + " ":" ";
        if("N/A".equals(timeStatistic.getUnit())) {
            unity = " ";
        }
        unity = unity.toLowerCase();
        if(mapTimeStatistic.get("count")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "count" + unity + timeStatistic.getCount() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapTimeStatistic.get("total")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "total" + unity + timeStatistic.getTotal() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapTimeStatistic.get("min")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "min" + unity + timeStatistic.getMin() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapTimeStatistic.get("max")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "max" + unity + timeStatistic.getMax() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        for(Stats stats:result){
            if(stats.getMetric().contains("threadPool")){
                System.out.println("Prefix: " + prefix);
                System.out.println(stats.getMetric());
                System.out.println(metricName);
            }
        }
        return result;
    }

    private static List<Stats> getBoundaryStatistic(String prefix, WSStatistic wsStatistic, PmiStatsType pmiStatsType, String node){
        String metricName = "";//(pmiStatsType.getMetricName() == null || pmiStatsType.getMetricName().length() == 0)?wsStatistic.getName():pmiStatsType.getMetricName();
        Map<String, Boolean> mapBoundaryStatistic = pmiStatsType.getBoundaryStatistic();
        String metricSeparator = getMetricSeparator(pmiStatsType);
        List<Stats> result = new LinkedList<Stats>();
        WSBoundaryStatistic boundaryStatistic = (WSBoundaryStatistic) wsStatistic;
        String unity = (pmiStatsType.isUnit())?"_" + boundaryStatistic.getUnit() + " ":" ";
        if("N/A".equals(boundaryStatistic.getUnit())) {
            unity = " ";
        }
        unity = unity.toLowerCase();
        if(mapBoundaryStatistic.get("upperBound")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "upperbound" + unity + boundaryStatistic.getUpperBound() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapBoundaryStatistic.get("lowerBound")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "lowerbound" + unity + boundaryStatistic.getLowerBound() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        for(Stats stats:result){
            if(stats.getMetric().contains("threadPool")){
                System.out.println("Prefix: " + prefix);
                System.out.println(stats.getMetric());
                System.out.println(metricName);
            }
        }
        return result;
    }

    private static List<Stats> getRangeStatistic(String prefix, WSStatistic wsStatistic, PmiStatsType pmiStatsType, String node){
        String metricName = "";//(pmiStatsType.getMetricName() == null || pmiStatsType.getMetricName().length() == 0)?wsStatistic.getName():pmiStatsType.getMetricName();
        Map<String, Boolean> mapRangeStatistic = pmiStatsType.getRangeStatistic();
        String metricSeparator = getMetricSeparator(pmiStatsType);
        List<Stats> result = new LinkedList<Stats>();
        WSRangeStatistic rangeStatistic = (WSRangeStatistic) wsStatistic;
        String unity = (pmiStatsType.isUnit())?"_" + rangeStatistic.getUnit() + " ":" ";
        if("N/A".equals(rangeStatistic.getUnit())) {
            unity = " ";
        }
        unity = unity.toLowerCase();
        if(mapRangeStatistic.get("highWaterMark")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "highwatermark" + unity + rangeStatistic.getHighWaterMark() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapRangeStatistic.get("lowWaterMark")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "lowwatermark" + unity + rangeStatistic.getLowWaterMark() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapRangeStatistic.get("current")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "current" + unity + rangeStatistic.getCurrent() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapRangeStatistic.get("integral")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "integral" + unity + rangeStatistic.getIntegral() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        for(Stats stats:result){
            if(stats.getMetric().contains("threadPool")){
                System.out.println("Prefix: " + prefix);
                System.out.println(stats.getMetric());
                System.out.println(metricName);
            }
        }
        return result;
    }

    private static List<Stats> getBoundedRangeStatistic(String prefix, WSStatistic wsStatistic, PmiStatsType pmiStatsType, String node){
        String metricName = "";//(pmiStatsType.getMetricName() == null || pmiStatsType.getMetricName().length() == 0)?wsStatistic.getName():pmiStatsType.getMetricName();
        Map<String, Boolean> mapBoundedRangeStatistic = pmiStatsType.getBoundedRangeStatistic();
        String metricSeparator = getMetricSeparator(pmiStatsType);
        List<Stats> result = new LinkedList<Stats>();
        WSBoundedRangeStatistic boundedRangeStatistic = (WSBoundedRangeStatistic) wsStatistic;
        String unity = (pmiStatsType.isUnit())?"_" + boundedRangeStatistic.getUnit() + " ":" ";
        if("N/A".equals(boundedRangeStatistic.getUnit())) {
            unity = " ";
        }
        unity = unity.toLowerCase();
        if(mapBoundedRangeStatistic.get("upperBound")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "upperbound" + unity + boundedRangeStatistic.getUpperBound() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapBoundedRangeStatistic.get("lowerBound")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "lowerbound" + unity + boundedRangeStatistic.getLowerBound() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapBoundedRangeStatistic.get("highWaterMark")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "highwatermark" + unity + boundedRangeStatistic.getHighWaterMark() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapBoundedRangeStatistic.get("lowWaterMark")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "lowwatermark" + unity + boundedRangeStatistic.getLowWaterMark() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapBoundedRangeStatistic.get("current")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "current" + unity + boundedRangeStatistic.getCurrent() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        if(mapBoundedRangeStatistic.get("integral")) {
            Stats stats = new Stats();
            stats.setHost(node);
            stats.setMetric(prefix + "." + metricName + metricSeparator + "integral" + unity + boundedRangeStatistic.getIntegral() + " " + System.currentTimeMillis() / 1000L);
            result.add(stats);
        }
        for(Stats stats:result){
            if(stats.getMetric().contains("threadPool")){
                System.out.println("Prefix: " + prefix);
                System.out.println(stats.getMetric());
                System.out.println(metricName);
            }
        }
        return result;
    }

    public static String getMetricSeparator(PmiStatsType pmiStatsType){
        if(pmiStatsType.isSeparateMetric()){
            return ".";
        } else {
            return "_";
        }
    }
}
