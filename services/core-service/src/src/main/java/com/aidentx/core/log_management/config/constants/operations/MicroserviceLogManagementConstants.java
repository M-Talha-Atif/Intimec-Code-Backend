package com.aidentx.core.log_management.config.constants.operations;

/**
 * Constants for {@link com.aidentx.core.log_management.service.impl.MicroserviceLogManagementServiceImpl}.
 *
 * <p>All string literals used in the service -- log messages, field names,
 * level values, component names, endpoint path, and service type keys -- are
 * defined here. No magic strings appear in the service implementation.
 *
 * @author aidentx
 * @since 1.0
 */
public final class MicroserviceLogManagementConstants {

    private MicroserviceLogManagementConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // Logger identity
    // -------------------------------------------------------------------------

    /**
     * Logger name passed to {@link com.aidentx.logging.service.UnifiedLoggerFactory}.
     */
    public static final String LOGGER_NAME = "MicroserviceLogManagement";

    /**
     * Fallback Logback logger name used when the component logger is absent.
     */
    public static final String LOGGER_ROOT = "ROOT";

    // -------------------------------------------------------------------------
    // REST endpoint
    // -------------------------------------------------------------------------

    /**
     * Path appended to the service base URL for log level GET and POST calls for standard services.
     */
    public static final String LOGS_ENDPOINT = "/api/v1/logs/level";

    /**
     * Path appended to the API gateway base URL for log level GET and POST calls.
     */
    public static final String LOGS_ENDPOINT_API_GATEWAY = "/api/v1/gateway/logs/level";

    // -------------------------------------------------------------------------
    // Service type routing keys
    // -------------------------------------------------------------------------

    /**
     * Service type key for the core microservice (local in-process dispatch).
     */
    public static final String SERVICE_TYPE_CORE = "core";

    /**
     * Service type key for the auth microservice.
     */
    public static final String SERVICE_TYPE_AUTH = "auth";

    /**
     * Service type key for the workflows microservice.
     */
    public static final String SERVICE_TYPE_WORKFLOW = "workflow";

    /**
     * Service type key for the application-permissions microservice.
     */
    public static final String SERVICE_TYPE_APPLICATION_PERMISSIONS = "application-permissions";

    /**
     * Service type key for the connector-runtime microservice.
     */
    public static final String SERVICE_TYPE_CONNECTOR_RUNTIME = "connector-runtime";

    /**
     * Service type key for the configuration microservice.
     */
    public static final String SERVICE_TYPE_CONFIGURATION = "configuration";

    /**
     * Service type key for the access-portal microservice.
     */
    public static final String SERVICE_TYPE_ACCESS_PORTAL = "access-portal";

    /**
     * Service type key for the identity microservice.
     */
    public static final String SERVICE_TYPE_IDENTITY = "identity";

    /**
     * Service type key for the LLM microservice.
     */
    public static final String SERVICE_TYPE_LLM = "llm";

    /**
     * Service type key for the task-management microservice.
     */
    public static final String SERVICE_TYPE_TASK_MANAGEMENT = "task-management";

    /**
     * Service type key for the API gateway.
     */
    public static final String SERVICE_TYPE_API_GATEWAY = "api-gateway";

    // -------------------------------------------------------------------------
    // Log level values
    // -------------------------------------------------------------------------

    /**
     * Level returned when a component's log level cannot be determined.
     */
    public static final String LEVEL_UNKNOWN = "UNKNOWN";

    /**
     * Level value indicating the logger is fully silenced.
     */
    public static final String LEVEL_OFF = "OFF";

    /**
     * Default remote fallback level when the logger name is absent from the response map.
     */
    public static final String LEVEL_INFO = "INFO";

    // -------------------------------------------------------------------------
    // Component names
    // -------------------------------------------------------------------------

    /**
     * Display name for the authentication component.
     */
    public static final String COMPONENT_AUTH_NAME = "Authentication";

    /**
     * Display name for the core service component.
     */
    public static final String COMPONENT_CORE_NAME = "Core Service";

    /**
     * Display name for the workflows service component.
     */
    public static final String COMPONENT_WORKFLOW_NAME = "Workflow Service";

    /**
     * Display name for the application-permissions service component.
     */
    public static final String COMPONENT_APPLICATION_PERMISSIONS_NAME = "Application Permissions Service";

    /**
     * Display name for the connector-runtime service component.
     */
    public static final String COMPONENT_CONNECTOR_RUNTIME_NAME = "Connector Runtime Service";

    /**
     * Display name for the configuration service component.
     */
    public static final String COMPONENT_CONFIGURATION_NAME = "Configuration Service";

    /**
     * Display name for the access-portal service component.
     */
    public static final String COMPONENT_ACCESS_PORTAL_NAME = "Access Portal Service";

    /**
     * Display name for the identity service component.
     */
    public static final String COMPONENT_IDENTITY_NAME = "Identity Service";

    /**
     * Display name for the LLM service component.
     */
    public static final String COMPONENT_LLM_NAME = "LLM Service";

    /**
     * Display name for the task-management service component.
     */
    public static final String COMPONENT_TASK_MANAGEMENT_NAME = "Task Management Service";

    /**
     * Display name for the API gateway component.
     */
    public static final String COMPONENT_API_GATEWAY_NAME = "API Gateway";

    // -------------------------------------------------------------------------
    // Component descriptions
    // -------------------------------------------------------------------------

    /**
     * Description for the authentication component.
     */
    public static final String COMPONENT_AUTH_DESCRIPTION = "Authentication and SSO operations";

    /**
     * Description for the core service component.
     */
    public static final String COMPONENT_CORE_DESCRIPTION = "Core business logic and data operations";

    /**
     * Description for the workflows service component.
     */
    public static final String COMPONENT_WORKFLOW_DESCRIPTION = "Workflow orchestration and execution";

    /**
     * Description for the application-permissions service component.
     */
    public static final String COMPONENT_APPLICATION_PERMISSIONS_DESCRIPTION = "Application-level permission management";

    /**
     * Description for the connector-runtime service component.
     */
    public static final String COMPONENT_CONNECTOR_RUNTIME_DESCRIPTION = "Connector lifecycle and runtime execution";

    /**
     * Description for the configuration service component.
     */
    public static final String COMPONENT_CONFIGURATION_DESCRIPTION = "Centralised configuration management";

    /**
     * Description for the access-portal service component.
     */
    public static final String COMPONENT_ACCESS_PORTAL_DESCRIPTION = "Access portal and role assignment operations";

    /**
     * Description for the identity service component.
     */
    public static final String COMPONENT_IDENTITY_DESCRIPTION = "User identity and profile management";

    /**
     * Description for the LLM service component.
     */
    public static final String COMPONENT_LLM_DESCRIPTION = "Large language model inference and prompt routing";

    /**
     * Description for the task-management service component.
     */
    public static final String COMPONENT_TASK_MANAGEMENT_DESCRIPTION = "Task creation, assignment, and tracking";

    /**
     * Description for the API gateway component.
     */
    public static final String COMPONENT_API_GATEWAY_DESCRIPTION = "API gateway routing and cross-cutting concerns";

    // -------------------------------------------------------------------------
    // Special component name
    // -------------------------------------------------------------------------

    /**
     * Magic component name that triggers a global log level update across all components.
     * Compared case-insensitively in the service.
     */
    public static final String COMPONENT_NAME_ALL = "all";

    // -------------------------------------------------------------------------
    // Request map keys
    // -------------------------------------------------------------------------

    /**
     * JSON/Map key checked to identify a single-component update payload.
     */
    public static final String MAP_KEY_COMPONENT_NAME = "componentName";

    // -------------------------------------------------------------------------
    // Log messages -- startup
    // -------------------------------------------------------------------------

    public static final String LOG_SERVICE_INITIALIZED =
            "MicroserviceLogManagementService initialized";

    public static final String LOG_AUTH_SERVICE_URL =
            "Auth service URL: %s";

    public static final String LOG_CORE_SERVICE_URL =
            "Core service URL: %s";

    public static final String LOG_WORKFLOW_SERVICE_URL =
            "Workflow service URL: %s";

    public static final String LOG_APPLICATION_PERMISSIONS_SERVICE_URL =
            "Application permissions service URL: %s";

    public static final String LOG_CONNECTOR_RUNTIME_SERVICE_URL =
            "Connector runtime service URL: %s";

    public static final String LOG_CONFIGURATION_SERVICE_URL =
            "Configuration service URL: %s";

    public static final String LOG_ACCESS_PORTAL_SERVICE_URL =
            "Access portal service URL: %s";

    public static final String LOG_IDENTITY_SERVICE_URL =
            "Identity service URL: %s";

    public static final String LOG_LLM_SERVICE_URL =
            "LLM service URL: %s";

    public static final String LOG_TASK_MANAGEMENT_SERVICE_URL =
            "Task management service URL: %s";

    public static final String LOG_API_GATEWAY_URL =
            "API gateway URL: %s";

    public static final String LOG_CONFIGURED_COMPONENTS =
            "Configured components: %d";

    public static final String LOG_COMPONENT_TYPE_LOGGER =
            "Component [%s] serviceType=[%s] loggerName=[%s]";

    // -------------------------------------------------------------------------
    // Log messages -- read operations
    // -------------------------------------------------------------------------

    public static final String LOG_GETTING_DEBUG_CONFIG =
            "Getting debug configuration for all components";

    public static final String LOG_PROCESSING_COMPONENT =
            "Processing component [%s] serviceType=[%s]";

    public static final String LOG_COMPONENT_LEVEL_ACTIVE =
            "Component [%s] level=[%s] active=[%s]";

    public static final String LOG_DEBUG_CONFIG_COMPLETE =
            "Debug configuration complete: %d/%d components active";

    public static final String LOG_DEBUG_INFO_FOR_COMPONENT =
            "Resolving debug info for component [%s] serviceType=[%s] loggerName=[%s]";

    public static final String LOG_GETTING_LOCAL_LOG_LEVEL =
            "Getting local log level for core component [%s]";

    public static final String LOG_LOCAL_COMPONENT_LEVEL =
            "Local component [%s] resolved level=[%s]";

    public static final String LOG_SERVICE_URL_NOT_CONFIGURED =
            "No service URL configured for component [%s]";

    public static final String LOG_CALLING_SERVICE_FOR_COMPONENT =
            "Calling [%s] to get log level for component [%s]";

    public static final String LOG_SERVICE_RESPONSE_STATUS =
            "Service response: status=[%s] hasBody=[%s]";

    public static final String LOG_SERVICE_RETURNED_LEVELS =
            "Service returned %d log level entries";

    public static final String LOG_COMPONENT_LEVEL_FROM_LOGGER =
            "Component [%s] resolved level=[%s] from logger=[%s]";

    public static final String LOG_SERVICE_CALL_FAILED =
            "Service call failed or returned no body for component [%s]";

    public static final String LOG_COMPONENT_FINAL_RESULT =
            "Component [%s] final result: level=[%s] active=[%s]";

    public static final String LOG_ERROR_GETTING_DEBUG_INFO =
            "Error getting debug info for component [%s]: %s";

    // -------------------------------------------------------------------------
    // Log messages -- write operations
    // -------------------------------------------------------------------------

    public static final String LOG_UPDATING_LOG_LEVEL =
            "Updating log level for component [%s] to [%s]";

    public static final String LOG_COMPONENT_CONFIG_NOT_FOUND =
            "Component config not found for [%s]";

    public static final String LOG_UPDATING_LOCAL_COMPONENT =
            "Updating local core component [%s] logger=[%s] to level=[%s]";

    public static final String LOG_LOCAL_UPDATE_SUCCESS =
            "Successfully updated local component [%s]";

    public static final String LOG_CALLING_SERVICE_TO_UPDATE =
            "Calling [%s] to update component [%s] logger=[%s] to level=[%s]";

    public static final String LOG_SERVICE_UPDATE_RESPONSE =
            "Service update response: status=[%s] body=[%s]";

    public static final String LOG_REMOTE_UPDATE_SUCCESS =
            "Successfully updated remote component [%s]";

    public static final String LOG_REMOTE_UPDATE_FAILED =
            "Failed to update remote component [%s]: status=[%s]";

    public static final String LOG_ERROR_UPDATING_COMPONENT =
            "Error updating component [%s]: %s";

    public static final String LOG_ERROR_NORMALISING_REQUEST =
            "Error normalising update request payload";


    // -------------------------------------------------------------------------
    // Controller -- identity
    // -------------------------------------------------------------------------

    /**
     * Logger name for {@link com.aidentx.core.log_management.controller.LogLevelController}.
     */
    public static final String CONTROLLER_LOGGER_NAME = "LogLevelController";

    // -------------------------------------------------------------------------
    // Controller -- HTTP response bodies
    // -------------------------------------------------------------------------

    /**
     * Response body returned when a log level update completes successfully.
     */
    public static final String RESPONSE_LOG_LEVELS_UPDATED = "Log levels updated successfully";

    /**
     * Response body returned when a log level update fails.
     */
    public static final String RESPONSE_FAILED_TO_UPDATE = "Failed to update log levels";

    /**
     * Response body returned when the POST body is absent.
     */
    public static final String RESPONSE_NO_VALID_PARAMS = "No valid parameters provided";

    // -------------------------------------------------------------------------
    // Controller -- log messages
    // -------------------------------------------------------------------------

    public static final String LOG_CTRL_GETTING_DEBUG_CONFIG =
            "GET /level -- retrieving debug configuration for all components";

    public static final String LOG_CTRL_DEBUG_CONFIG_RETRIEVED =
            "Debug configuration retrieved successfully: totalComponents=[%d]";

    public static final String LOG_CTRL_ERROR_GETTING_DEBUG_CONFIG =
            "Error retrieving debug configuration";

    public static final String LOG_CTRL_UPDATING_LOG_LEVELS =
            "POST /level -- updating log levels: request=[%s]";

    public static final String LOG_CTRL_LOG_LEVELS_UPDATED =
            "Component log levels updated successfully";

    public static final String LOG_CTRL_FAILED_TO_UPDATE =
            "Failed to update component log levels";

    public static final String LOG_CTRL_NO_VALID_PARAMS =
            "POST /level -- request body is absent; no update performed";

    public static final String LOG_CTRL_GETTING_COMPONENT_DEBUG_INFO =
            "GET /level/{componentName} -- retrieving debug info for component=[%s]";

    public static final String LOG_CTRL_COMPONENT_DEBUG_INFO_FOUND =
            "Component debug info found: component=[%s] level=[%s]";

    public static final String LOG_CTRL_COMPONENT_NOT_FOUND =
            "Component not found: component=[%s]";
}