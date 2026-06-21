package com.aidentx.logging.service;

import com.aidentx.logging.model.UnifiedLogEntryBuilder;

import java.util.Map;

public class LogEventBuilder {

    private final IUnifiedLogger logger;
    private final UnifiedLogEntryBuilder entryBuilder;

    // Constructor for standard logs
    public LogEventBuilder(IUnifiedLogger logger, String level) {
        this.logger = logger;
        this.entryBuilder = UnifiedLogEntryBuilder.builder().level(level);
    }

    // Constructor for logs with an exception
    public LogEventBuilder(IUnifiedLogger logger, String level, Throwable throwable) {
        this.logger = logger;
        this.entryBuilder = UnifiedLogEntryBuilder.builder()
                .level(level)
                .detail("exceptionClass", throwable.getClass().getName())
                .detail("exceptionMessage", throwable.getMessage())
                .detail("stackTrace", IUnifiedLogger.getStackTraceAsString(throwable)); // Add full stack trace
    }

    public LogEventBuilder message(String message) {
        entryBuilder.message(message);
        return this;
    }

    public LogEventBuilder category(String category) {
        entryBuilder.category(category);
        return this;
    }

    public LogEventBuilder objectType(String objectType) {
        entryBuilder.objectType(objectType);
        return this;
    }

    public LogEventBuilder objectId(String objectId) {
        entryBuilder.objectId(objectId);
        return this;
    }

    public LogEventBuilder failureReason(String reason) {
        entryBuilder.failureReason(reason);
        return this;
    }

    public LogEventBuilder detail(String key, Object value) {
        entryBuilder.detail(key, value);
        return this;
    }

    /**
     * Adds multiple detail key-value pairs at once from a map.
     * This is the recommended way to add multiple contextual details.
     * @param details A map of detail key-value pairs.
     * @return This builder instance.
     */
    public LogEventBuilder details(Map<String, Object> details) {
        if (details != null && !details.isEmpty()) {
            details.forEach(entryBuilder::detail);
        }
        return this;
    }

    /**
     * Sets a specific, pre-generated unique ID for this log entry.
     * This is crucial for correlating API error responses with logs.
     *
     * @param id The unique identifier (e.g., a UUID string).
     * @return This builder instance.
     */
    public LogEventBuilder id(String id) {
        entryBuilder.id(id);
        return this;
    }

    // Finalizer method
    public void log() {
        logger.log(entryBuilder.build());
    }
}