package com.aidentx.api_gateaway.service;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service implementation for managing application log levels at runtime.
 */
@Service
public class LogLevelImpl implements LogLevelService {

    /**
     * Updates the logging level for the given logger name at runtime.
     * <p>
     * This change is applied immediately without requiring an application restart.
     * </p>
     *
     * @param loggerName the name of the logger to update (e.g.,
     *                   "com.aidentx.authservice")
     * @param level      the desired log level (e.g., "DEBUG", "INFO", "WARN",
     *                   "ERROR")
     */
    @Override
    public void updateLogLevel(String loggerName, String level) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);

        if (!loggerConfig.getName().equals(loggerName)) {
            loggerConfig = new LoggerConfig(loggerName, Level.toLevel(level.toUpperCase(Locale.ROOT)), true);
            config.addLogger(loggerName, loggerConfig);
        } else {
            loggerConfig.setLevel(Level.toLevel(level.toUpperCase(Locale.ROOT)));
        }
        context.updateLoggers();
    }

    /**
     * Retrieves the current log levels for all configured loggers.
     *
     * @return a map of logger names to their current log levels
     */
    @Override
    public Map<String, String> getCurrentLogLevels() {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        Map<String, String> logLevels = new HashMap<>();

        // Get all logger configurations
        Map<String, LoggerConfig> loggers = config.getLoggers();
        for (Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            String loggerName = entry.getKey();
            LoggerConfig loggerConfig = entry.getValue();

            // Skip empty logger name (root logger)
            if (!loggerName.isEmpty()) {
                Level level = loggerConfig.getLevel();
                logLevels.put(loggerName, level != null ? level.toString() : "INHERIT");
            }
        }

        // Also include root logger
        LoggerConfig rootLogger = config.getRootLogger();
        if (rootLogger != null) {
            Level rootLevel = rootLogger.getLevel();
            logLevels.put("ROOT", rootLevel != null ? rootLevel.toString() : "UNKNOWN");
        }

        return logLevels;
    }
}