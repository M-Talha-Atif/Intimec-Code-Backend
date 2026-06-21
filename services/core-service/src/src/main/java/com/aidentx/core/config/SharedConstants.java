package com.aidentx.core.config;

/**
 * Shared constants for Core service.
 * Contains all constant values to avoid hardcoding throughout the application.
 *
 * @author aidentx Development Team
 * @since 1.0
 */
public class SharedConstants {

    // ===================================================================
    // GENERAL CONSTANTS
    // ===================================================================

    private SharedConstants() {
        // Private constructor to prevent instantiation
    }

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DGRAPH INITIALIZER
    // ===================================================================
    public static final String LOG_DGRAPH_INITIALIZER_STARTUP = "Dgraph initializer starting up";
    public static final String LOG_DGRAPH_INITIALIZER_SCHEMA_INIT_STARTED = "Initializing Dgraph schema for core service";
    public static final String LOG_DGRAPH_INITIALIZER_SCHEMA_INIT_COMPLETED = "Dgraph schema initialization completed successfully";
    public static final String LOG_DGRAPH_INITIALIZER_SCHEMA_INIT_FAILED = "Failed to initialize Dgraph schema";
    public static final String LOG_DGRAPH_INITIALIZER_FALLBACK_MODE = "Application will continue to run, but Dgraph functionality may be limited";
    public static final String LOG_DGRAPH_INITIALIZER_SERVICE_CONFIGURED = "Dgraph schema service configured";
    public static final String LOG_DGRAPH_INITIALIZER_PROFILE_ACTIVE = "Dgraph initializer running in non-test profile";
    public static final String LOG_DGRAPH_INITIALIZER_STARTUP_COMPLETED = "Dgraph initializer startup completed";

    // ===================================================================
    // EXCEPTION MESSAGES - DGRAPH INITIALIZER
    // ===================================================================
    public static final String EXCEPTION_DGRAPH_INITIALIZER_STARTUP_FAILED = "Dgraph initializer startup failed";
    public static final String EXCEPTION_DGRAPH_INITIALIZER_SERVICE_UNAVAILABLE = "Dgraph schema service is unavailable";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DGRAPH SCHEMA SERVICE
    // ===================================================================
    public static final String LOG_DGRAPH_BASIC_SCHEMA_INIT_STARTED = "Basic Dgraph schema initialization started";
    public static final String LOG_DGRAPH_BASIC_SCHEMA_INIT_SUCCESS = "Basic Dgraph schema initialized successfully";
    public static final String LOG_DGRAPH_BASIC_SCHEMA_INIT_FAILED = "Failed to initialize basic Dgraph schema";
    public static final String LOG_DGRAPH_SCHEMA_SYNC_STARTED = "Schema synchronization to Dgraph started";
    public static final String LOG_DGRAPH_SCHEMA_SYNC_SUCCESS = "Schema synced to Dgraph successfully";
    public static final String LOG_DGRAPH_SCHEMA_SYNC_FAILED = "Failed to sync schema to Dgraph";
    public static final String LOG_DGRAPH_SCHEMA_REMOVAL_NOTED = "Schema removal from Dgraph noted (manual cleanup may be required)";
    public static final String LOG_DGRAPH_SCHEMA_REMOVAL_FAILED = "Failed to remove schema from Dgraph";
    public static final String LOG_DGRAPH_FIELD_SYNC_STARTED = "Field synchronization to Dgraph started";
    public static final String LOG_DGRAPH_FIELD_SYNC_SUCCESS = "Field synced to Dgraph successfully";
    public static final String LOG_DGRAPH_FIELD_SYNC_FAILED = "Failed to sync field to Dgraph";
    public static final String LOG_DGRAPH_SCHEMA_UPDATE_STARTED = "Schema update in Dgraph started";
    public static final String LOG_DGRAPH_SCHEMA_UPDATE_SUCCESS = "Schema updated in Dgraph successfully";
    public static final String LOG_DGRAPH_SCHEMA_UPDATE_FAILED = "Failed to update schema in Dgraph";
    public static final String LOG_DGRAPH_OPERATION_BUILDING = "Building Dgraph operation";
    public static final String LOG_DGRAPH_OPERATION_EXECUTING = "Executing Dgraph operation";
    public static final String LOG_DGRAPH_SCHEMA_DEFINITION_BUILDING = "Building schema definition";
    public static final String LOG_DGRAPH_FIELD_DEFINITION_BUILDING = "Building field definition";

    // ===================================================================
    // EXCEPTION MESSAGES - DGRAPH SCHEMA SERVICE
    // ===================================================================
    public static final String EXCEPTION_DGRAPH_SCHEMA_INIT_FAILED = "Failed to initialize Dgraph schema";
    public static final String EXCEPTION_DGRAPH_SCHEMA_SYNC_FAILED = "Failed to sync schema to Dgraph";
    public static final String EXCEPTION_DGRAPH_FIELD_SYNC_FAILED = "Failed to sync field to Dgraph";
    public static final String EXCEPTION_DGRAPH_SCHEMA_UPDATE_FAILED = "Failed to update schema in Dgraph";

    // ===================================================================
    // LOGGER CATEGORIES
    // ===================================================================
    public static final String LOG_CATEGORY_DGRAPH_SCHEMA = "Dgraph-Schema";

    // ===================================================================
    // DEBUG LOGGING MESSAGES
    // ===================================================================
    public static final String LOG_DEBUG_METHOD_ENTRY = "Entering method";
    public static final String LOG_DEBUG_METHOD_EXIT = "Exiting method";
    public static final String LOG_DEBUG_PARAMETER_VALUES = "Method parameter values";
    public static final String LOG_DEBUG_PROCESSING_STEP = "Processing step";
    public static final String LOG_DEBUG_VALIDATION_STEP = "Validation step";
    public static final String LOG_DEBUG_STATE_CHANGE = "State change detected";
    public static final String LOG_DEBUG_CONDITION_CHECK = "Condition check";
    public static final String LOG_DEBUG_LOOP_ITERATION = "Loop iteration";
    public static final String LOG_DEBUG_OBJECT_STATE = "Object state";
    public static final String LOG_DEBUG_CONFIGURATION_VALUE = "Configuration value";
    public static final String LOG_DEBUG_TEMPORARY_DATA = "Temporary data state";
}
