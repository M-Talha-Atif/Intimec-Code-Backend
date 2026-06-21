package com.aidentx.logging.appender;

import com.aidentx.logging.model.UnifiedLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(name = "UnifiedHttpAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class UnifiedHttpAppender extends AbstractAppender {

    private final String endpoint;
    private final String username;
    private final String password;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected UnifiedHttpAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions,
            String endpoint, String username, String password) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);

        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
        this.executor = Executors.newFixedThreadPool(2);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PluginFactory
    public static UnifiedHttpAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("endpoint") String endpoint,
            @PluginAttribute("username") String username,
            @PluginAttribute("password") String password,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) boolean ignoreExceptions) {

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new UnifiedHttpAppender(name, filter, layout, ignoreExceptions, endpoint, username, password);
    }

    @Override
    public void append(LogEvent event) {

        // System.out.println(
        //     "Appender received: " + event.getLoggerName() + " - " + event.getMessage().getFormattedMessage()); // Debugging line
            
            Object[] params = event.getMessage().getParameters();
            
            UnifiedLogEntry logEntry;

            if (params != null && params.length > 0 && params[0] instanceof UnifiedLogEntry) {
                logEntry = (UnifiedLogEntry) params[0];
            } else {
                logEntry = new UnifiedLogEntry();
                logEntry.setMessage(event.getMessage().getFormattedMessage());
                logEntry.setTimestamp(event.getTimeMillis());
                logEntry.setId(UUID.randomUUID().toString());
                logEntry.setLevel(event.getLevel().name());
                logEntry.setMicroservice(event.getLoggerName());
            }
            
        // System.out.println("Sending to endpoint: " + endpoint); // Debugging line
        executor.submit(() -> {
            try {
                byte[] payload = objectMapper.writeValueAsBytes(logEntry);

                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
                conn.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }

                if (conn.getResponseCode() >= 400) {
                    // LOGGER.warn("UnifiedHttpAppender HTTP error: {}", conn.getResponseCode());
                }

                // System.out.println("UnifiedHttpAppender sent: " + logEntry.getId() + " to " + endpoint); // Debugging line

            } catch (Exception e) {
                if (!ignoreExceptions())
                    throw new AppenderLoggingException(e);
                // LOGGER.error("UnifiedHttpAppender error: {}", e.getMessage(), e);
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
        executor.shutdown();
    }
}
