package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.service.LogLevelService;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service implementation for managing application log levels at runtime in core service.
 */
@Service
public class LogLevelServiceImpl implements LogLevelService {

    /**
     * Updates the logging level for the given logger name at runtime.
     *
     * @param loggerName the name of the logger to update (e.g., "com.aidentx.core")
     * @param level      the desired log level (e.g., "DEBUG", "INFO", "WARN", "ERROR")
     */
    @Override
    public void updateLogLevel(String loggerName, String level) {
        LoggerContext context = LoggerContext.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        
        // Handle ROOT logger specially
        if ("ROOT".equals(loggerName)) {
            LoggerConfig rootConfig = config.getRootLogger();
            rootConfig.setLevel(Level.toLevel(level.toUpperCase(Locale.ROOT)));
        } else {
            LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);

            if (!loggerConfig.getName().equals(loggerName)) {
                loggerConfig = new LoggerConfig(loggerName, Level.toLevel(level.toUpperCase(Locale.ROOT)), true);
                config.addLogger(loggerName, loggerConfig);
            } else {
                loggerConfig.setLevel(Level.toLevel(level.toUpperCase(Locale.ROOT)));
            }
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
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
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
        
        // Also include root logger - get the actual effective level
        LoggerConfig rootLogger = config.getRootLogger();
        if (rootLogger != null) {
            Level rootLevel = rootLogger.getLevel();
            // If level is null, it might be inheriting - get the effective level
            if (rootLevel == null) {
                // Get the actual logger instance to check its effective level
                org.apache.logging.log4j.Logger actualRootLogger = context.getRootLogger();
                if (actualRootLogger instanceof org.apache.logging.log4j.core.Logger) {
                    org.apache.logging.log4j.core.Logger coreLogger = (org.apache.logging.log4j.core.Logger) actualRootLogger;
                    rootLevel = coreLogger.getLevel();
                }
            }
            logLevels.put("ROOT", rootLevel != null ? rootLevel.toString() : "UNKNOWN");
        }
        
        return logLevels;
    }
}
