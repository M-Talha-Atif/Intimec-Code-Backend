package com.aidentx.core.log_management.controller;

import com.aidentx.core.log_management.config.constants.operations.MicroserviceLogManagementConstants;
import com.aidentx.core.log_management.model.dto.ComponentDebugInfo;
import com.aidentx.core.log_management.model.dto.DebugConfigurationResponse;
import com.aidentx.core.log_management.service.MicroserviceLogManagementService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing log levels across the core service and all
 * registered microservices.
 *
 * <p>Base path: {@code /api/v1/logs}
 *
 * <p>Exposed endpoints:
 * <pre>
 * GET  /api/v1/logs/level               -- retrieve debug configuration for all components
 * POST /api/v1/logs/level               -- update log level for one or more components
 * GET  /api/v1/logs/level/{componentName} -- retrieve debug info for a single component
 * </pre>
 *
 * @author aidentx
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogLevelController {

    private final IUnifiedLogger logger;
    private final MicroserviceLogManagementService microserviceLogManagementService;

    /**
     * Constructs the controller and wires its dependencies.
     *
     * @param microserviceLogManagementService service handling cross-service log level operations
     * @param loggerFactory                    factory used to obtain the {@link IUnifiedLogger} instance
     */
    public LogLevelController(
            MicroserviceLogManagementService microserviceLogManagementService,
            UnifiedLoggerFactory loggerFactory) {
        this.microserviceLogManagementService = microserviceLogManagementService;
        this.logger = loggerFactory.getLogger(
                MicroserviceLogManagementConstants.CONTROLLER_LOGGER_NAME);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/logs/level
    // -------------------------------------------------------------------------

    /**
     * Retrieves the current debug configuration for every registered microservice component.
     *
     * <p>Delegates to {@link MicroserviceLogManagementService#getDebugConfiguration()}.
     * Each component entry in the response includes its name, description, current
     * log level, service URL, and active status.
     *
     * <p>Example response:
     * <pre>
     * HTTP 200
     * {
     *   "totalComponents": 11,
     *   "components": [
     *     { "name": "Core Service",   "level": "INFO",  "active": true },
     *     { "name": "Authentication", "level": "DEBUG", "active": true },
     *     ...
     *   ]
     * }
     * </pre>
     *
     * @return {@code 200 OK} with a populated {@link DebugConfigurationResponse};
     *         re-throws any unexpected service exception so the global error handler
     *         can produce the appropriate error response
     */
    @GetMapping("/level")
    public ResponseEntity<DebugConfigurationResponse> getDebugConfiguration() {
        logger.info()
                .message(MicroserviceLogManagementConstants.LOG_CTRL_GETTING_DEBUG_CONFIG)
                .log();

        try {
            DebugConfigurationResponse response =
                    microserviceLogManagementService.getDebugConfiguration();

            logger.info()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_CTRL_DEBUG_CONFIG_RETRIEVED,
                            response.getTotalComponents()))
                    .log();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(e)
                    .message(MicroserviceLogManagementConstants.LOG_CTRL_ERROR_GETTING_DEBUG_CONFIG)
                    .log();
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/logs/level
    // -------------------------------------------------------------------------

    /**
     * Updates the log level for one or more registered microservice components.
     *
     * <p>The request body is polymorphic and is resolved by
     * {@link MicroserviceLogManagementService#updateComponentLogLevels(Object)}:
     * <ul>
     *   <li>Single component -- {@code { "componentName": "Authentication", "level": "DEBUG" }}</li>
     *   <li>Bulk list        -- {@code [ { "componentName": "...", "level": "..." }, ... ]}</li>
     *   <li>Bulk wrapper     -- {@code { "components": [ ... ] }}</li>
     *   <li>Global shortcut  -- {@code { "componentName": "all", "level": "WARN" }}</li>
     * </ul>
     *
     * <p>Returns {@code 400 Bad Request} when the body is absent or when the
     * service reports that one or more updates failed.
     *
     * @param request the polymorphic update payload; may be a single object, a list,
     *                or a bulk wrapper; {@code null} when no body is provided
     * @return {@code 200 OK} with a success message on full success;
     *         {@code 400 Bad Request} with an error message on partial or full failure
     */
    @PostMapping("/level")
    public ResponseEntity<String> updateLogLevels(
            @RequestBody(required = false) Object request) {

        if (request == null) {
            logger.warn()
                    .message(MicroserviceLogManagementConstants.LOG_CTRL_NO_VALID_PARAMS)
                    .log();
            return ResponseEntity.badRequest()
                    .body(MicroserviceLogManagementConstants.RESPONSE_NO_VALID_PARAMS);
        }

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CTRL_UPDATING_LOG_LEVELS,
                        request))
                .log();

        boolean success = microserviceLogManagementService.updateComponentLogLevels(request);

        if (success) {
            logger.info()
                    .message(MicroserviceLogManagementConstants.LOG_CTRL_LOG_LEVELS_UPDATED)
                    .log();
            return ResponseEntity.ok(
                    MicroserviceLogManagementConstants.RESPONSE_LOG_LEVELS_UPDATED);
        }

        logger.error()
                .message(MicroserviceLogManagementConstants.LOG_CTRL_FAILED_TO_UPDATE)
                .log();
        return ResponseEntity.badRequest()
                .body(MicroserviceLogManagementConstants.RESPONSE_FAILED_TO_UPDATE);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/logs/level/{componentName}
    // -------------------------------------------------------------------------

    /**
     * Retrieves the current debug information for a single named component.
     *
     * <p>The {@code componentName} path variable must match the exact display name
     * registered in the service (case-sensitive), e.g. {@code "Authentication"},
     * {@code "Identity Service"}, {@code "LLM Service"}.
     *
     * <p>Example:
     * <pre>
     * GET /api/v1/logs/level/Authentication
     *
     * HTTP 200
     * { "name": "Authentication", "level": "INFO", "active": true, "serviceUrl": "http://auth-service:8090" }
     *
     * GET /api/v1/logs/level/UnknownService
     * HTTP 404
     * </pre>
     *
     * @param componentName the exact display name of the component to query
     * @return {@code 200 OK} with {@link ComponentDebugInfo} when found;
     *         {@code 404 Not Found} when no component matches the given name
     */
    @GetMapping("/level/{componentName}")
    public ResponseEntity<ComponentDebugInfo> getComponentDebugInfo(
            @PathVariable String componentName) {

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CTRL_GETTING_COMPONENT_DEBUG_INFO,
                        componentName))
                .log();

        ComponentDebugInfo debugInfo =
                microserviceLogManagementService.getComponentDebugInfo(componentName);

        if (debugInfo != null) {
            logger.info()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_CTRL_COMPONENT_DEBUG_INFO_FOUND,
                            componentName, debugInfo.getLevel()))
                    .log();
            return ResponseEntity.ok(debugInfo);
        }

        logger.warn()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CTRL_COMPONENT_NOT_FOUND,
                        componentName))
                .log();
        return ResponseEntity.notFound().build();
    }
}