package com.aidentx.logging.service;

import com.aidentx.logging.service.impl.UnifiedLoggerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A factory for creating context-aware, fluent logger instances.
 * This factory is a standard Spring bean and should be injected into the
 * constructor
 * of any class that needs a logger. It provides a clean,
 * dependency-injection-friendly
 * way to obtain logger instances.
 */
@Component
public class UnifiedLoggerFactory {

    private final ObjectMapper objectMapper;
    private final String microservice;

    /**
     * The ObjectMapper and microservice dependencies are injected via the constructor by the Spring
     * container.
     *
     * @param objectMapper The shared ObjectMapper bean from the Spring context.
     * @param microservice  The name of the application, injected from the 'spring.application.name' property.
     */
    public UnifiedLoggerFactory(ObjectMapper objectMapper,
                                @Value("${spring.application.name:unknown-service}") String microservice) {
        this.objectMapper = objectMapper;
        this.microservice = microservice;
    }

    /**
     * Gets a logger for the specified class. The class name will be used as the log source.
     *
     * @param clazz The class to create the logger for.
     * @return An IUnifiedLogger instance.
     */
    public IUnifiedLogger getLogger(Class<?> clazz) {
        return new UnifiedLoggerImpl(LogManager.getLogger(clazz), this.objectMapper, this.microservice);
    }

    /**
     * Gets a logger for the specified class with a default category.
     *
     * @param clazz           The class to create the logger for.
     * @param defaultCategory The default category to apply to all log entries from
     *                        this instance.
     * @return An IUnifiedLogger instance.
     */
    public IUnifiedLogger getLogger(Class<?> clazz, String defaultCategory) {
        return new UnifiedLoggerImpl(LogManager.getLogger(clazz), this.objectMapper, this.microservice, defaultCategory);
    }

    /**
     * Gets a logger for the specified custom name. The name will be used as the log source.
     *
     * @param name The custom name for the logger.
     * @return An IUnifiedLogger instance.
     */
    public IUnifiedLogger getLogger(String name) {
        return new UnifiedLoggerImpl(LogManager.getLogger(name), this.objectMapper, this.microservice);
    }

    /**
     * Gets a logger for the specified custom name with a default category.
     *
     * @param name            The custom name for the logger.
     * @param defaultCategory The default category to apply to all log entries from
     *                        this instance.
     * @return An IUnifiedLogger instance.
     */
    public IUnifiedLogger getLogger(String name, String defaultCategory) {
        return new UnifiedLoggerImpl(LogManager.getLogger(name), this.objectMapper, this.microservice, defaultCategory);
    }
}