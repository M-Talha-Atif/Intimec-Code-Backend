package com.aidentx.authservice.service;

import java.util.Map;

/**
 * Service for managing application log levels at runtime.
 * <p>
 * Provides functionality to dynamically update log levels at runtime.
 * Log viewing is now handled through OpenSearch/Elasticsearch.
 * </p>
 */
public interface LogLevelService {

    /**
     * Updates the logging level for a given logger name at runtime.
     *
     * @param loggerName the name of the logger to update (e.g., "com.aidentx.authservice")
     * @param level      the desired log level (e.g., "DEBUG", "INFO", "WARN")
     */
    void updateLogLevel(String loggerName, String level);

    /**
     * Retrieves the current log levels for all configured loggers.
     *
     * @return a map of logger names to their current log levels
     */
    Map<String, String> getCurrentLogLevels();
}