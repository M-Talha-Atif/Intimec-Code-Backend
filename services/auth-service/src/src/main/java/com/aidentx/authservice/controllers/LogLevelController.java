package com.aidentx.authservice.controllers;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.dto.LogLevelRequest;
import com.aidentx.authservice.service.LogLevelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dynamically changing log levels at runtime.
 * All user-facing messages are managed via {@link AppConstants}.
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogLevelController {

    private final LogLevelService logLevelService;

    public LogLevelController(LogLevelService logLevelService) {
        this.logLevelService = logLevelService;
    }

    /**
     * Endpoint to get the current log levels of all loggers.
     *
     * @return HTTP 200 with the current log levels.
     */
    @GetMapping("/level")
    public ResponseEntity<?> getCurrentLogLevels() {
        return ResponseEntity.ok(logLevelService.getCurrentLogLevels());
    }

    /**
     * Endpoint to update the log level of a specific logger.
     *
     * @param request the log level change request.
     * @return HTTP 200 with a success message.
     */
    @PostMapping("/level")
    public ResponseEntity<String> updateLogLevel(@RequestBody LogLevelRequest request) {
        logLevelService.updateLogLevel(request.getLoggerName(), request.getLevel());
        String message = String.format(AppConstants.LOG_LEVEL_UPDATE_SUCCESS, request.getLoggerName());
        return ResponseEntity.ok(message);
    }
}