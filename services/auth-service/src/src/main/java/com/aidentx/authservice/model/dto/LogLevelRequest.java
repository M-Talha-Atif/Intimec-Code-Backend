package com.aidentx.authservice.model.dto;

/**
 * DTO representing a request to change the log level of a specific logger at runtime.
 * <p>
 * Example usage:
 * {
 * "loggerName": "com.aidentx.authservice",
 * "level": "DEBUG"
 * }
 */
public class LogLevelRequest {

    /**
     * The fully qualified name of the logger to update.
     * For example: "com.aidentx.authservice.service.AuthServiceImpl"
     */
    private String loggerName;

    /**
     * The desired log level to set for the given logger.
     * Supported values: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
     */
    private String level;

    /**
     * Returns the name of the logger.
     *
     * @return logger name
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Sets the name of the logger to be updated.
     *
     * @param loggerName fully qualified logger name
     */
    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    /**
     * Returns the target log level.
     *
     * @return log level as a String
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the target log level for the logger.
     *
     * @param level log level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }
}
