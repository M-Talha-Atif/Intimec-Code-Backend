package com.aidentx.iam.common.logging;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller exposing log level read and write operations for this
 * microservice.
 *
 * <p>Consumed by the core service's {@code MicroserviceLogManagementService}
 * to provide centralised log level visibility and control across all services.
 *
 * <p>Endpoints:
 * <pre>
 * GET  /api/v1/logs/level              -- returns current log levels as { loggerName -> level }
 * POST /api/v1/logs/level              -- updates the level for a named logger
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogLevelController {

    private final LoggingSystem loggingSystem;

    /**
     * Constructs the controller with the Spring-managed {@link LoggingSystem}.
     *
     * @param loggingSystem Spring Boot's logging system abstraction; auto-wired by Spring
     */
    public LogLevelController(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    /**
     * Returns the current log levels for all configured loggers in this service.
     *
     * <p>The response is a flat map of logger name to level string, e.g.:
     * <pre>
     * {
     *   "ROOT": "INFO",
     *   "com.aidentx": "DEBUG"
     * }
     * </pre>
     *
     * <p>This is the format expected by {@code MicroserviceLogManagementServiceImpl}
     * which reads the {@code ROOT} key as the effective service-level log level.
     *
     * @return {@code 200 OK} with the logger-to-level map
     */
    @GetMapping("/level")
    public ResponseEntity<Map<String, String>> getLogLevels() {
        Map<String, String> levels = new HashMap<>();

        loggingSystem.getLoggerConfigurations().forEach(config -> {
            if (config.getEffectiveLevel() != null) {
                levels.put(config.getName(), config.getEffectiveLevel().name());
            }
        });

        // Guarantee ROOT is always present so the caller's fallback logic works
        levels.putIfAbsent("ROOT", LogLevel.INFO.name());

        return ResponseEntity.ok(levels);
    }

    /**
     * Updates the log level for the named logger in this service.
     *
     * <p>Expected request body:
     * <pre>
     * { "loggerName": "ROOT", "level": "DEBUG" }
     * </pre>
     *
     * <p>Passing {@code "ROOT"} as the logger name changes the effective level
     * for the entire service.
     *
     * @param request map containing {@code loggerName} and {@code level} keys
     * @return {@code 200 OK} on success; {@code 400 Bad Request} when either
     *         key is missing or the level string is invalid
     */
    @PostMapping("/level")
    public ResponseEntity<String> updateLogLevel(@RequestBody Map<String, String> request) {
        String loggerName = request.get("loggerName");
        String level = request.get("level");

        if (loggerName == null || loggerName.isBlank()
                || level == null || level.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Both 'loggerName' and 'level' are required");
        }

        try {
            LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
            loggingSystem.setLogLevel(loggerName, logLevel);
            return ResponseEntity.ok(
                    String.format("Log level for [%s] set to [%s]", loggerName, level));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(String.format("Invalid log level [%s]. Valid values: TRACE, DEBUG, INFO, WARN, ERROR, OFF", level));
        }
    }
}
