package com.aidentx.logging.service.impl;

import com.aidentx.logging.model.UnifiedLogEntry;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * The internal implementation of the IUnifiedLogger interface.
 * This class enriches a UnifiedLogEntry object with context and passes the
 * raw object to the underlying Log4j2 logger, which is responsible for
 * serializing it to JSON.
 */
public class UnifiedLoggerImpl implements IUnifiedLogger {

    private final Logger log4jLogger;
    private final String defaultCategory;
    private final String microservice;

    public UnifiedLoggerImpl(Logger log4jLogger, ObjectMapper objectMapper, String microservice,String defaultCategory) {
        this.log4jLogger = log4jLogger;
        this.defaultCategory = defaultCategory;
        this.microservice = microservice != null ? microservice : "unknown-service";
    }

    public UnifiedLoggerImpl(Logger log4jLogger, ObjectMapper objectMapper, String microservice) {
        this(log4jLogger, objectMapper,microservice ,null);
    }

    // --- Fluent API Method Implementations (Unchanged) ---
    @Override
    public LogEventBuilder trace() {
        return new LogEventBuilder(this, "TRACE");
    }

    @Override
    public LogEventBuilder debug() {
        return new LogEventBuilder(this, "DEBUG");
    }

    @Override
    public LogEventBuilder info() {
        return new LogEventBuilder(this, "INFO");
    }

    @Override
    public LogEventBuilder warn() {
        return new LogEventBuilder(this, "WARN");
    }

    @Override
    public LogEventBuilder error() {
        return new LogEventBuilder(this, "ERROR");
    }

    @Override
    public LogEventBuilder error(Throwable throwable) {
        return new LogEventBuilder(this, "ERROR", throwable);
    }

    /**
     * The core logging method where all context is applied and the log is
     * dispatched.
     * 
     * @param logEntry The log entry built by the fluent builder chain.
     */
    @Override
    public void log(UnifiedLogEntry logEntry) {
        // 1. Add Automatically Managed Context
        if (logEntry.getId() == null) {
            logEntry.setId(UUID.randomUUID().toString());
        }
        logEntry.setSource(log4jLogger.getName());
        logEntry.setMicroservice(this.microservice);

        if (logEntry.getCorrelationId() == null) {
            logEntry.setCorrelationId(MDC.get("correlationId"));
        }

        if (logEntry.getCategory() == null && this.defaultCategory != null) {
            logEntry.setCategory(this.defaultCategory);
        }

    // Log4j2's JsonLayout will handle the serialization.
        // String level = logEntry.getLevel() != null ? logEntry.getLevel().toUpperCase() : "INFO";
        final Level level = Level.toLevel(logEntry.getLevel(), Level.INFO);
        // switch (level) {
        //     case "TRACE" -> log4jLogger.trace(jsonLogEntry);
        //     case "DEBUG" -> log4jLogger.debug(jsonLogEntry);
        //     case "INFO" -> log4jLogger.info(jsonLogEntry);
        //     case "WARN" -> log4jLogger.warn(jsonLogEntry);
        //     case "ERROR", "FATAL" -> log4jLogger.error(jsonLogEntry);
        //     default -> log4jLogger.info(jsonLogEntry);
        // }
        log4jLogger.log(level, logEntry);
    }
}