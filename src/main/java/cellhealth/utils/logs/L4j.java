package cellhealth.utils.logs;

import cellhealth.utils.properties.Settings;
import org.apache.log4j.*;

/**
 * Created by Alberto Pascual on 10/06/15.
 */
public class L4j {

    private static final L4j instance;
    static Logger log;

    static {
        instance = new L4j();
    }

    private L4j(){
        ConsoleAppender console = new ConsoleAppender();
        console.setName(Settings.properties().getAppName());
        console.setLayout(new PatternLayout(Settings.properties().getL4jPattern()));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        log = Logger.getLogger(Settings.properties().getL4jGetLogger());
    }

    public static L4j getL4j() {
        return instance;
    }

    public void setConfig(String filename, String level) {
        FileAppender fileAppender = new FileAppender();
        fileAppender.setName(Settings.properties().getAppName());
        fileAppender.setFile(filename);
        fileAppender.setLayout(new PatternLayout(Settings.properties().getL4jPatternConfig()));
        fileAppender.setThreshold(Level.toLevel(level));
        fileAppender.setAppend(true);
        fileAppender.activateOptions();
        Logger.getRootLogger().getLoggerRepository().resetConfiguration();
        Logger.getRootLogger().addAppender(fileAppender);
        log = Logger.getLogger(Settings.properties().getAppName());
    }

    public void critical(String msg) {
        if (log.isEnabledFor(Level.FATAL)) {
            log.fatal(msg);
        }
    }

    public void error(String msg) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(msg);
        }
    }

    public void error(String msg, Throwable t) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(msg, t);
        }
    }

    public void warning(String msg) {
        if (log.isEnabledFor(Level.WARN)) {
            log.warn((Object)msg);
        }
    }

    public void notice(String msg) {
        if (log.isInfoEnabled()) {
            log.info(msg);
        }
    }

    public void info(String msg) {
        if (log.isInfoEnabled()) {
            log.info(msg);
        }
    }

    public void debug(String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    public void debug(String msg, Throwable t) {
        if (log.isDebugEnabled()) {
            log.debug(msg, t);
        }
    }
}
