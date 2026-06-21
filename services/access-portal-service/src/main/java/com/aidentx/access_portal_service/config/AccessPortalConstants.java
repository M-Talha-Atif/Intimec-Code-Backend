package com.aidentx.access_portal_service.config;

/**
 * Application constants for Access Portal Service.
 * Contains all constant values to avoid hardcoding throughout the application.
 */
public final class AccessPortalConstants {

    // ==================== DEFAULT VALUES ====================
    public static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    // Tenant ID extraction
    public static final String LOG_TENANT_ID_EXTRACTION_FAILED = "Failed to extract tenant ID from JWT";

    // ==================== LOGGER CATEGORIES ====================
    public static final String  LOG_CATEGORY_TILE_AUDIT = "access-portal-audit";
    public static final String TILE_DUPLICATE_VALIDATOR_LOGGER_NAME = "TileDuplicateValidator";
    public static final String CART_SERVICE_LOGGER_NAME = "cart-service";
    public static final String LOG_CATEGORY_CART_SERVICE = "cart-service";
    public static final String ACCESS_REQUEST_SERVICE_LOGGER_NAME =
            "access-request-service";

    // Performance monitoring
    public static final String LOG_OBJECT_TYPE_PERFORMANCE_WARNING = "PERFORMANCE_WARNING";
    public static final String LOG_OBJECT_TYPE_SLOW_QUERY = "SLOW_QUERY";


    public static final String LOG_CATEGORY_ACCESS_REQUEST_SERVICE =
            "access-request-service";

    public static final String LOG_CATEGORY_TILE_DELETION_SERVICE = "TILE_DELETION_SERVICE";



    // ==================== JWT CLAIMS ====================
    public static final String CLAIM_TENANT_ID = "tenantId";

    // ==================== API PATHS ====================
    public static final String API_BASE_TILES = "/tiles";
    public static final String API_TILE_BY_ID = "/{tileId}";
    public static final String API_BASE_CART = "/cart";
    public static final String API_CART_ITEM_BY_ID = "/{cartItemId}";
    public static final String API_BASE_ACCESS_REQUEST = "/access-requests";
    public static final String API_TILE_ACCESS_SCOPE =
            "/{tileId}/access-scope";


    // ==================== SWAGGER TAGS ====================
    public static final String TAG_TILE_API = "Tile API";
    public static final String TAG_TILE_API_DESC = "APIs for managing Access Portal Tiles";
    public static final String TAG_CART_API = "Cart API";
    public static final String TAG_CART_API_DESC = "APIs for managing shopping cart";
    public static final String TAG_ACCESS_REQUEST_API = "Access Request API";
    public static final String TAG_ACCESS_REQUEST_API_DESC =
            "APIs for submitting and managing access requests";
    public static final String OP_GET_TILE_ACCESS_SCOPE =
            "Get tile access scope items";

    public static final String OP_GET_TILE_ACCESS_SCOPE_DESC =
            "Fetch access scope items (applications, roles, groups, entitlements) configured for a tile";


    // ==================== SWAGGER OPERATIONS - TILES ====================
    public static final String OP_CREATE_TILE = "Create Tile";
    public static final String OP_CREATE_TILE_DESC = "Create a new access portal tile";
    public static final String OP_UPDATE_TILE = "Update Tile";
    public static final String OP_UPDATE_TILE_DESC = "Update an existing access portal tile";
    public static final String OP_DELETE_TILE = "Delete Tile";
    public static final String OP_DELETE_TILE_DESC = "Delete a tile by its UUID";
    public static final String OP_GET_TILE = "Get Tile";
    public static final String OP_GET_TILE_DESC = "Get tile details by UUID";
    public static final String OP_LIST_TILES = "List Tiles";
    public static final String OP_LIST_TILES_DESC = "Get all tiles for a tenant";

    // ==================== SWAGGER OPERATIONS - Requests ====================

    public static final String OP_SUBMIT_ACCESS_REQUEST =
            "Submit access request";

    public static final String OP_SUBMIT_ACCESS_REQUEST_DESC =
            "Submit all active cart items as a single access request";

    // ==================== SWAGGER OPERATIONS - CART ====================
    public static final String OP_ADD_TO_CART = "Add to Cart";
    public static final String OP_ADD_TO_CART_DESC = "Add an item to shopping cart";
    public static final String OP_GET_CART = "Get Cart";
    public static final String OP_GET_CART_DESC = "Get cart items with pagination";
    public static final String OP_REMOVE_FROM_CART = "Remove from Cart";
    public static final String OP_REMOVE_FROM_CART_DESC = "Remove an item from cart";

    // ==================== SUCCESS MESSAGES - TILES ====================
    public static final String SUCCESS_TILE_CREATED = "Tile created successfully";
    public static final String SUCCESS_TILE_UPDATED = "Tile updated successfully";
    public static final String SUCCESS_TILE_DELETED = "Tile deleted successfully";
    public static final String SUCCESS_ACCESS_REQUEST_SUBMITTED =
            "Access request submitted successfully";

    // --------------------
    // Tile Access Scope – Log Object Types
    // --------------------

    public static final String LOG_OBJECT_TYPE_TILE_ACCESS_SCOPE_FETCH_START =
            "TILE_ACCESS_SCOPE_FETCH_START";

    public static final String LOG_OBJECT_TYPE_TILE_ACCESS_SCOPE_FETCH_SUCCESS =
            "TILE_ACCESS_SCOPE_FETCH_SUCCESS";


    // ==================== SUCCESS MESSAGES - CART ====================
    public static final String SUCCESS_ITEM_ADDED_TO_CART = "Item added to cart successfully";
    public static final String SUCCESS_ITEM_REMOVED_FROM_CART = "Item removed from cart successfully";

    // ==================== CART STATUS ====================
    public static final String CART_STATUS_ACTIVE = "ACTIVE";
    public static final String CART_STATUS_REMOVED = "REMOVED";

    // ==================== ITEM TYPES ====================
    public static final String ITEM_TYPE_APPLICATION = "APPLICATION";
    public static final String ITEM_TYPE_INTERNAL_ROLE = "INTERNAL_ROLE";
    public static final String ITEM_TYPE_ENTITLEMENT = "ENTITLEMENT";
    public static final String ITEM_TYPE_BUSINESS_ROLE = "BUSINESS_ROLE";

    // ==================== REQUEST TYPES ====================
    public static final String REQUEST_TYPE_SELF = "SELF";
    public static final String REQUEST_TYPE_FOR_OTHERS = "FOR_OTHERS";

    // ===== ACCESS REQUEST =====
    public static final String REQUEST_STATUS_SUBMITTED = "SUBMITTED";
    public static final String REQUEST_STATUS_DRAFT = "DRAFT";

    // ===== REQUEST TARGET =====
    public static final String REQUEST_TARGET_SELF = "SELF";
    public static final String REQUEST_TARGET_OTHERS = "OTHERS";

    // ===== VALIDATION MESSAGES =====
    public static final String ERROR_CART_EMPTY = "Cart is empty";
    public static final String ERROR_JUSTIFICATION_REQUIRED = "Justification is required";
    public static final String ERROR_INVALID_URGENCY = "Invalid urgency value";
    public static final String ERROR_ACCESS_DENIED = "User not allowed to submit request";

    // ===== SORT FIELDS =====
    public static final String SORT_CREATED_AT = "createdAt";

    // ==================== PAGINATION DEFAULTS ====================
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    // ==================== HEADER CONSTANTS ====================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    // Logger Names
    public static final String TILE_VISIBILITY_VALIDATOR_LOGGER_NAME = "TileVisibilityValidator";
    public static final String ACCESS_PORTAL_SERVICE_LOGGER_NAME = "AccessPortalService";
    public static final String TILE_REQUEST_VALIDATOR_LOGGER_NAME = "TileRequestValidator";
    public static final String TILE_EXTERNAL_REF_VALIDATOR_LOGGER_NAME = "TileExternalReferenceValidator";
    public static final String TILE_BUSINESS_RULES_VALIDATOR_LOGGER_NAME = "TileBusinessRulesValidator";
    public static final String TILE_SERVICE_LOGGER_NAME = "TileService";


    // ============================================
    // LOG CATEGORIES
    // ============================================
    public static final String LOG_CATEGORY_TILE_ACCESS = "tile-access";
    public static final String LOG_CATEGORY_TILE_VALIDATION = "tile-validation";
    public static final String LOG_CATEGORY_TILE_MANAGEMENT = "tile-management";
    public static final String LOG_CATEGORY_ACCESS_CONTROL = "access-control";
    public static final String LOG_CATEGORY_AUTHENTICATION = "authentication";
    public static final String LOG_CATEGORY_API = "api";
    public static final String LOG_CATEGORY_EXTERNAL_VALIDATION = "external-validation";
    public static final String LOG_CATEGORY_EXTERNAL_SERVICE = "external-service";
    public static final String LOG_CATEGORY_BUSINESS_RULES = "business-rules";
    public static final String LOG_CATEGORY_FILTER_VALIDATION = "filter-validation";

    public static final String LOG_CATEGORY_TILE_SERVICE = "tile-service";
    public static final String LOG_CATEGORY_TILE_OPERATIONS = "tile-operations";




    // Object Types
    public static final String LOG_OBJECT_TYPE_TILE_ACCESS_VALIDATION = "tile-access-validation";
    public static final String LOG_OBJECT_TYPE_TILE_ACCESS_VALIDATION_SUCCESS = "tile-access-validation-success";
    public static final String LOG_OBJECT_TYPE_TILE_VALIDATION_ERROR = "tile-validation-error";
    public static final String LOG_OBJECT_TYPE_TILE_VALIDATION = "tile-validation";
    public static final String LOG_OBJECT_TYPE_TENANT_VALIDATION = "tenant-validation";
    public static final String LOG_OBJECT_TYPE_TENANT_VALIDATION_ERROR = "tenant-validation-error";
    public static final String LOG_OBJECT_TYPE_TENANT_ACCESS_DENIED = "tenant-access-denied";
    public static final String LOG_OBJECT_TYPE_TENANT_VALIDATION_SUCCESS = "tenant-validation-success";
    public static final String LOG_OBJECT_TYPE_TILE_STATUS_VALIDATION = "tile-status-validation";
    public static final String LOG_OBJECT_TYPE_TILE_INACTIVE = "tile-inactive";
    public static final String LOG_OBJECT_TYPE_TILE_STATUS_VALIDATION_SUCCESS = "tile-status-validation-success";
    public static final String LOG_OBJECT_TYPE_ROLE_VALIDATION = "role-validation";
    public static final String LOG_OBJECT_TYPE_ROLE_VALIDATION_ERROR = "role-validation-error";
    public static final String LOG_OBJECT_TYPE_TILE_NO_ROLES = "tile-no-roles";
    public static final String LOG_OBJECT_TYPE_TILE_ROLES_INFO = "tile-roles-info";
    public static final String LOG_OBJECT_TYPE_ROLE_ACCESS_DENIED = "role-access-denied";
    public static final String LOG_OBJECT_TYPE_ROLE_VALIDATION_SUCCESS = "role-validation-success";
    public static final String LOG_OBJECT_TYPE_TILE_VISIBILITY_CHECK = "tile-visibility-check";
    public static final String LOG_OBJECT_TYPE_REQUEST_FOR_OTHERS_VALIDATION = "request-for-others-validation";
    public static final String LOG_OBJECT_TYPE_REQUEST_FOR_OTHERS_DENIED = "request-for-others-denied";
    public static final String LOG_OBJECT_TYPE_REQUEST_FOR_OTHERS_VALIDATION_SUCCESS = "request-for-others-validation-success";
    public static final String LOG_OBJECT_TYPE_ROLE_NOT_FOUND = "role-not-found";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_ITEM_VALIDATION = "external-item-validation";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_ITEM_VALIDATION_SUCCESS = "external-item-validation-success";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_ITEM_VALIDATION_ERROR = "external-item-validation-error";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_ITEM_NOT_FOUND = "external-item-not-found";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL = "external-service-call";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_SERVICE_ERROR = "external-service-error";
    public static final String LOG_OBJECT_TYPE_USER_ROLES_FETCH = "user-roles-fetch";
    public static final String LOG_OBJECT_TYPE_USER_ROLES_FETCH_SUCCESS = "user-roles-fetch-success";
    public static final String LOG_OBJECT_TYPE_USER_ROLES_FETCH_ERROR = "user-roles-fetch-error";
    public static final String LOG_OBJECT_TYPE_BULK_VALIDATION = "bulk-validation";
    public static final String LOG_OBJECT_TYPE_BULK_VALIDATION_SUCCESS = "bulk-validation-success";
    public static final String LOG_OBJECT_TYPE_BULK_VALIDATION_ERROR = "bulk-validation-error";

    // Tile Service
    public static final String LOG_OBJECT_TYPE_TILE_CREATION_START = "tile-creation-start";
    public static final String LOG_OBJECT_TYPE_TILE_CREATION_SUCCESS = "tile-creation-success";
    public static final String LOG_OBJECT_TYPE_TILE_CREATION_ERROR = "tile-creation-error";
    public static final String LOG_OBJECT_TYPE_TILE_UPDATE_START = "tile-update-start";
    public static final String LOG_OBJECT_TYPE_TILE_UPDATE_SUCCESS = "tile-update-success";
    public static final String LOG_OBJECT_TYPE_TILE_DELETION_START = "tile-deletion-start";
    public static final String LOG_OBJECT_TYPE_TILE_DELETION_SUCCESS = "tile-deletion-success";
    public static final String LOG_OBJECT_TYPE_TILE_FETCH_START = "tile-fetch-start";
    public static final String LOG_OBJECT_TYPE_TILE_FETCH_SUCCESS = "tile-fetch-success";
    public static final String LOG_OBJECT_TYPE_TILE_BASIC_FOUND = "tile-basic-found";
    public static final String LOG_OBJECT_TYPE_USER_ROLES_FETCHED = "user-roles-fetched";
    public static final String LOG_OBJECT_TYPE_TILE_ROLES_FETCHED = "tile-roles-fetched";
    public static final String LOG_OBJECT_TYPE_TILE_RELATIONS_FETCH = "tile-relations-fetch";
    public static final String LOG_OBJECT_TYPE_TILE_RELATIONS_FETCHED = "tile-relations-fetched";
    public static final String LOG_OBJECT_TYPE_TILE_DISAPPEARED = "tile-disappeared";
    public static final String LOG_OBJECT_TYPE_TILES_FETCH_START = "tiles-fetch-start";
    public static final String LOG_OBJECT_TYPE_USER_ACTIVE_ROLES = "user-active-roles";
    public static final String LOG_OBJECT_TYPE_TILES_FETCHED = "tiles-fetched";
    public static final String LOG_OBJECT_TYPE_TILES_VISIBLE = "tiles-visible";
    public static final String LOG_OBJECT_TYPE_TILE_NOT_FOUND = "tile-not-found";
    public static final String LOG_OBJECT_TYPE_CART_MERGE_SUCCESS = "CART_MERGE_SUCCESS";

    // ===========================================
    public static final String LOG_OBJECT_TYPE_VISUAL_FILTER_RULES_VALIDATION = "visual-filter-rules-validation";
    public static final String LOG_OBJECT_TYPE_VALIDATION_SKIPPED = "validation-skipped";
    public static final String LOG_OBJECT_TYPE_INDIVIDUAL_RULE_VALIDATION = "individual-rule-validation";
    public static final String LOG_OBJECT_TYPE_INDIVIDUAL_RULE_VALIDATION_SUCCESS = "individual-rule-validation-success";
    public static final String LOG_OBJECT_TYPE_FIRST_RULE_VALIDATION_ERROR = "first-rule-validation-error";
    public static final String LOG_OBJECT_TYPE_VISUAL_FILTER_RULES_VALIDATION_SUCCESS = "visual-filter-rules-validation-success";
    public static final String LOG_OBJECT_TYPE_FIELD_NAME_VALIDATION = "field-name-validation";
    public static final String LOG_OBJECT_TYPE_FIELD_NAME_VALIDATION_ERROR = "field-name-validation-error";
    public static final String LOG_OBJECT_TYPE_INVALID_FIELD_NAME = "invalid-field-name";
    public static final String LOG_OBJECT_TYPE_FIELD_NAME_VALIDATION_SUCCESS = "field-name-validation-success";
    public static final String LOG_OBJECT_TYPE_LOGICAL_OPERATOR_VALIDATION = "logical-operator-validation";
    public static final String LOG_OBJECT_TYPE_LOGICAL_OPERATOR_VALIDATION_ERROR = "logical-operator-validation-error";
    public static final String LOG_OBJECT_TYPE_INVALID_LOGICAL_OPERATOR = "invalid-logical-operator";
    public static final String LOG_OBJECT_TYPE_LOGICAL_OPERATOR_VALIDATION_SUCCESS = "logical-operator-validation-success";
    public static final String LOG_OBJECT_TYPE_COMPARISON_OPERATOR_VALIDATION = "comparison-operator-validation";
    public static final String LOG_OBJECT_TYPE_COMPARISON_OPERATOR_VALIDATION_ERROR = "comparison-operator-validation-error";
    public static final String LOG_OBJECT_TYPE_INVALID_COMPARISON_OPERATOR = "invalid-comparison-operator";
    public static final String LOG_OBJECT_TYPE_COMPARISON_OPERATOR_VALIDATION_SUCCESS = "comparison-operator-validation-success";
    public static final String LOG_OBJECT_TYPE_RULE_VALUE_VALIDATION = "rule-value-validation";
    public static final String LOG_OBJECT_TYPE_RULE_VALUE_VALIDATION_ERROR = "rule-value-validation-error";
    public static final String LOG_OBJECT_TYPE_INVALID_LIST_VALUE = "invalid-list-value";
    public static final String LOG_OBJECT_TYPE_VALUE_LENGTH_EXCEEDED = "value-length-exceeded";
    public static final String LOG_OBJECT_TYPE_RULE_VALUE_VALIDATION_SUCCESS = "rule-value-validation-success";


    // Object types for user profile logging
    public static final String LOG_OBJECT_TYPE_USER_PROFILE_FETCH_START = "USER_PROFILE_FETCH_START";
    public static final String LOG_OBJECT_TYPE_USER_PROFILE_FETCH_SUCCESS = "USER_PROFILE_FETCH_SUCCESS";
    public static final String LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR = "USER_PROFILE_FETCH_ERROR";

    // ============================================
    // FILTER VALIDATION CONSTANTS
   // ============================================
    public static final String[] ALLOWED_FILTER_FIELDS = {
            "department", "riskLevel", "location",
            "displayName", "firstName", "lastName",
            "email", "vendorName", "contractEndDate",
            "userName", "jobTitle", "employmentType"
    };

    public static final String[] LOGICAL_OPERATORS = {"AND", "OR", "NOT"};
    public static final String[] COMPARISON_OPERATORS = {"EQ", "NE", "GT", "LT", "GTE", "LTE", "IN", "NOT_IN", "CONTAINS"};
    public static final int MAX_FILTER_VALUE_LENGTH = 1000;


    // ============================================
   // ERROR MESSAGES (For business rules validation)
   // ============================================
    public static final String ERROR_FIELD_NAME_REQUIRED = "Field name is required for filter rules";
    public static final String ERROR_LOGICAL_OPERATOR_REQUIRED = "Logical operator is required for filter rules";
    public static final String ERROR_OPERATOR_REQUIRED = "Operator is required for filter rules";
    public static final String ERROR_VALUE_REQUIRED = "Value is required for filter rules";
    public static final String ERROR_INVALID_FIELD_NAME = "Invalid field name. Allowed fields: ";
    public static final String ERROR_INVALID_LOGICAL_OPERATOR = "Invalid logical operator. Must be one of: ";
    public static final String ERROR_INVALID_OPERATOR = "Invalid operator. Must be one of: ";
    public static final String ERROR_FIRST_RULE_NO_LOGICAL_OP = "Logical operator is not allowed for the first filter rule";
    public static final String ERROR_INVALID_LIST_FORMAT = "For IN and NOT_IN operators, value must be a comma-separated list or JSON array";
    public static final String ERROR_VALUE_TOO_LONG = "Value cannot exceed ";


    // ============================================
    // ERROR MESSAGES (For external validation)
    // ============================================
    public static final String ERROR_INVALID_ROLE = "Invalid role ID: ";
    public static final String ERROR_INVALID_EXTERNAL_ITEM = "Invalid item ID: ";
    public static final String ERROR_EXTERNAL_SERVICE_UNAVAILABLE = "External service unavailable";
    public static final String ERROR_TENANT_ID_REQUIRED = "Tenant ID is required";
    public static final String ERROR_ITEM_ID_REQUIRED = "Item ID is required";
    public static final String ERROR_ITEM_TYPE_REQUIRED = "Item type is required";
    public static final String ERROR_USER_ID_REQUIRED = "User ID is required";

    // ============================================
    // EXTERNAL SERVICE CONFIGURATION
    // ============================================
    public static final String EXTERNAL_SERVICE_TIMEOUT = "5000"; // 5 seconds
    public static final String EXTERNAL_SERVICE_MAX_RETRIES = "3";



    // ============================================
    // LOG OBJECT TYPES (For TileRequestValidator)
    // ============================================
    public static final String LOG_OBJECT_TYPE_TILE_REQUEST_VALIDATION = "tile-request-validation";
    public static final String LOG_OBJECT_TYPE_TILE_REQUEST_VALIDATION_ERROR = "tile-request-validation-error";
    public static final String LOG_OBJECT_TYPE_TILE_REQUEST_VALIDATION_SUCCESS = "tile-request-validation-success";
    public static final String LOG_OBJECT_TYPE_ACCESS_SCOPE_ITEM_VALIDATION = "access-scope-item-validation";
    public static final String LOG_OBJECT_TYPE_VALIDATION_FAILED = "validation-failed";
    public static final String LOG_OBJECT_TYPE_VALIDATION_RESULT = "validation-result";
    public static final String LOG_OBJECT_TYPE_TITLE_VALIDATION = "title-validation";
    public static final String LOG_OBJECT_TYPE_TITLE_VALIDATION_ERROR = "title-validation-error";
    public static final String LOG_OBJECT_TYPE_TITLE_LENGTH_ERROR = "title-length-error";
    public static final String LOG_OBJECT_TYPE_TITLE_VALIDATION_SUCCESS = "title-validation-success";
    public static final String LOG_OBJECT_TYPE_TILE_CONFIGURATION_VALIDATION = "tile-configuration-validation";
    public static final String LOG_OBJECT_TYPE_ACCESS_SCOPE_CONSISTENCY_CHECK = "access-scope-consistency-check";
    public static final String LOG_OBJECT_TYPE_INDIVIDUAL_ITEM_VALIDATION = "individual-item-validation";
    public static final String LOG_OBJECT_TYPE_CONFIGURATION_INCONSISTENCY = "configuration-inconsistency";
    public static final String LOG_OBJECT_TYPE_TILE_CONFIGURATION_VALIDATION_SUCCESS = "tile-configuration-validation-success";
    public static final String LOG_OBJECT_TYPE_RISK_LEVEL_VALIDATION = "risk-level-validation";
    public static final String LOG_OBJECT_TYPE_RISK_LEVEL_SKIPPED = "risk-level-skipped";
    public static final String LOG_OBJECT_TYPE_RISK_LEVEL_VALIDATION_ERROR = "risk-level-validation-error";
    public static final String LOG_OBJECT_TYPE_RISK_LEVEL_VALIDATION_SUCCESS = "risk-level-validation-success";


    /**
     * Log object types for duplicate validation
     */
    public static final String LOG_OBJECT_TYPE_TILE_DUPLICATE_CHECK = "TILE_DUPLICATE_CHECK";
    public static final String LOG_OBJECT_TYPE_TILE_DUPLICATE_CHECK_PASSED = "TILE_DUPLICATE_CHECK_PASSED";
    public static final String LOG_OBJECT_TYPE_TILE_DUPLICATE_FOUND = "TILE_DUPLICATE_FOUND";
    public static final String LOG_OBJECT_TYPE_TILE_DUPLICATE_VALIDATION_START = "TILE_DUPLICATE_VALIDATION_START";
    public static final String LOG_OBJECT_TYPE_TILE_DUPLICATE_VALIDATION_SUCCESS = "TILE_DUPLICATE_VALIDATION_SUCCESS";
    public static final String LOG_OBJECT_TYPE_TILE_DUPLICATE_VALIDATION_ERROR = "TILE_DUPLICATE_VALIDATION_ERROR";

    /**
     * Log object types for comparison operations
     */
    public static final String LOG_OBJECT_TYPE_TILE_BASIC_ATTRIBUTES_COMPARISON = "TILE_BASIC_ATTRIBUTES_COMPARISON";
    public static final String LOG_OBJECT_TYPE_TILE_ALLOWED_ROLES_COMPARISON = "TILE_ALLOWED_ROLES_COMPARISON";
    public static final String LOG_OBJECT_TYPE_TILE_ACCESS_SCOPE_COMPARISON = "TILE_ACCESS_SCOPE_COMPARISON";
    public static final String LOG_OBJECT_TYPE_TILE_VISUAL_FILTERS_COMPARISON = "TILE_VISUAL_FILTERS_COMPARISON";
    public static final String LOG_OBJECT_TYPE_TILE_COMPARISON_DIFFERENCE_FOUND = "TILE_COMPARISON_DIFFERENCE_FOUND";

    /**
     * Log object types for tile operations
     */
    public static final String LOG_OBJECT_TYPE_TILE_UPDATE_ERROR = "TILE_UPDATE_ERROR";

    /**
     * Error messages for duplicate validation
     */
    public static final String ERROR_MSG_DUPLICATE_TILE_FOUND = "A tile with identical configuration already exists (ID: %s). At least one attribute must be different.";
    public static final String ERROR_MSG_DUPLICATE_TILE_TITLE = "A tile with the same title and configuration already exists";

    /**
     * Log categories
     */
    public static final String LOG_CATEGORY_TILE_DUPLICATE_VALIDATION = "TILE_DUPLICATE_VALIDATION";

    public static final String LOG_OBJECT_TYPE_VALIDATION_ERROR = "VALIDATION_ERROR";

    // Object types for logging
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_DETAILS_FETCH_START = "ACCESS_REQUEST_DETAILS_FETCH_START";
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_DETAILS_FETCH_SUCCESS = "ACCESS_REQUEST_DETAILS_FETCH_SUCCESS";
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_LIFECYCLE_FETCH_START = "ACCESS_REQUEST_LIFECYCLE_FETCH_START";
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_LIFECYCLE_FETCH_SUCCESS = "ACCESS_REQUEST_LIFECYCLE_FETCH_SUCCESS";
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_NOT_FOUND = "ACCESS_REQUEST_NOT_FOUND";
    public static final String LOG_OBJECT_TYPE_UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";

    // Withdraw request constants
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_WITHDRAW_START = "ACCESS_REQUEST_WITHDRAW_START";
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_WITHDRAW_SUCCESS = "ACCESS_REQUEST_WITHDRAW_SUCCESS";
    public static final String LOG_OBJECT_TYPE_INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";


    // Exception handling
    public static final String EXCEPTION_LOGGER_NAME = "access-portal-exception-handler";
    public static final String LOG_CATEGORY_EXCEPTION = "exception";
    public static final String LOG_OBJECT_TYPE_DUPLICATE_ERROR = "duplicate-resource";
    public static final String LOG_OBJECT_TYPE_ACCESS_DENIED = "access-denied";
    public static final String LOG_OBJECT_TYPE_ILLEGAL_ARGUMENT = "illegal-argument";
    public static final String LOG_OBJECT_TYPE_ILLEGAL_STATE = "illegal-state";
    public static final String LOG_OBJECT_TYPE_SECURITY_EXCEPTION = "security-exception";
    public static final String LOG_OBJECT_TYPE_UNEXPECTED_EXCEPTION = "unexpected-exception";




    // ==================== USER ROLE REST CLIENT CONSTANTS ====================

    /**
     * Logger name for user role and profile REST client
     */
    public static final String USER_ROLE_REST_CLIENT_LOGGER_NAME = "UserRoleRestClient";
    public static final String USER_PROFILE_REST_CLIENT_LOGGER_NAME = "UserProfileRestClient";

    /**
     * Log object types for external service operations
     */
    public static final String LOG_OBJECT_TYPE_EXTERNAL_SERVICE_SUCCESS = "EXTERNAL_SERVICE_SUCCESS";
    public static final String LOG_OBJECT_TYPE_EXTERNAL_SERVICE_WARNING = "EXTERNAL_SERVICE_WARNING";
    public static final String LOG_OBJECT_TYPE_HTTP_REQUEST = "HTTP_REQUEST";

    /**
     * Log object types for user role operations
     */
    public static final String LOG_OBJECT_TYPE_USER_ROLES_RETRIEVED = "USER_ROLES_RETRIEVED";
    public static final String LOG_OBJECT_TYPE_ROLE_CHECK = "ROLE_CHECK";
    public static final String LOG_OBJECT_TYPE_ROLE_CHECK_RESULT = "ROLE_CHECK_RESULT";
    public static final String LOG_OBJECT_TYPE_ACTIVE_ROLES_FETCH = "ACTIVE_ROLES_FETCH";
    public static final String LOG_OBJECT_TYPE_ACTIVE_ROLES_RETRIEVED = "ACTIVE_ROLES_RETRIEVED";
    public static final String LOG_OBJECT_TYPE_ROLE_PARSING_ERROR = "ROLE_PARSING_ERROR";

    /**
     * Log object types for JWT operations
     */
    public static final String LOG_OBJECT_TYPE_JWT_FORWARDING = "JWT_FORWARDING";
    public static final String LOG_OBJECT_TYPE_JWT_FORWARDING_ERROR = "JWT_FORWARDING_ERROR";

    /**
     * Log object types for health check operations
     */
    public static final String LOG_OBJECT_TYPE_HEALTH_CHECK = "HEALTH_CHECK";
    public static final String LOG_OBJECT_TYPE_HEALTH_CHECK_RESULT = "HEALTH_CHECK_RESULT";
    public static final String LOG_OBJECT_TYPE_HEALTH_CHECK_FAILED = "HEALTH_CHECK_FAILED";

    /* =======================
       Cart Object Types
       ======================= */

    public static final String LOG_OBJECT_TYPE_CART_ADD_START =
            "cart-add-start";

    public static final String LOG_OBJECT_TYPE_CART_ADD_SUCCESS =
            "cart-add-success";

    public static final String LOG_OBJECT_TYPE_CART_DUPLICATE =
            "cart-duplicate-item";

    public static final String LOG_OBJECT_TYPE_CART_FETCH_START =
            "cart-fetch-start";

    public static final String LOG_OBJECT_TYPE_CART_FETCH_SUCCESS =
            "cart-fetch-success";

    public static final String LOG_OBJECT_TYPE_CART_REMOVE_START =
            "cart-remove-start";

    public static final String LOG_OBJECT_TYPE_CART_REMOVE_SUCCESS =
            "cart-remove-success";

    public static final String LOG_OBJECT_TYPE_CART_COUNT =
            "cart-item-count";

     /* =======================
       Access Request Object Types
       ======================= */

    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_SUBMIT_START =
            "access-request-submit-start";

    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_SUBMIT_SUCCESS =
            "access-request-submit-success";

    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_HISTORY_FETCH_START =
            "access-request-history-fetch-start";

    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_HISTORY_FETCH_SUCCESS =
            "access-request-history-fetch-success";


    /* =======================
       Cart-related (shared)
       ======================= */

    public static final String LOG_OBJECT_TYPE_CART_ITEMS_FETCHED =
            "cart-items-fetched";

    public static final String LOG_OBJECT_TYPE_CART_EMPTY =
            "cart-empty";


    /* =======================
       Role / Item Fetch Object Types
       ======================= */

    public static final String LOG_OBJECT_TYPE_ROLE_FETCH =
            "role-fetch-start";

    public static final String LOG_OBJECT_TYPE_ROLE_FETCH_SUCCESS =
            "role-fetch-success";

    public static final String LOG_OBJECT_TYPE_ITEM_FETCH_ERROR =
            "item-fetch-error";

    public static final String LOG_OBJECT_TYPE_UNSUPPORTED_ITEM_TYPE =
            "unsupported-item-type";

    // ─── Entitlement Fetch (gRPC) ───────────────────────────────────
    public static final String LOG_OBJECT_TYPE_ENTITLEMENT_FETCH =
            "entitlement-fetch";

    public static final String LOG_OBJECT_TYPE_ENTITLEMENT_FETCH_SUCCESS =
            "entitlement-fetch-success";

    // ─── Business Role Fetch (REST) ─────────────────────────────────
    public static final String LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH =
            "business-role-fetch-start";

    public static final String LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_SUCCESS =
            "business-role-fetch-success";

    public static final String LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_ERROR =
            "business-role-fetch-error";

    // ─── Access Request Item Type Gates ─────────────────────────────
    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_WORKFLOW_GATE =
            "access-request-workflow-gate";

    public static final String LOG_OBJECT_TYPE_ACCESS_REQUEST_BUSINESS_ROLE_WORKFLOW =
            "access-request-business-role-workflow";

    // ─── Identity Service REST Integration ──────────────────────────
    /** Path template for fetching a single access role detail by ID. */
    public static final String IDENTITY_ACCESS_ROLE_BY_ID_PATH = "/api/v1/access-roles/{roleId}";

    /** Tenant header name expected by identity-service controllers. */
    public static final String IDENTITY_HEADER_TENANT_ID = "X-Tenant-Id";

    // ─── Error messages for business role validation ────────────────
    public static final String ERROR_INVALID_BUSINESS_ROLE =
            "Invalid business role ID: ";


    // ─── Analytics API ───────────────────────────────────────────────
    public static final String API_BASE_ANALYTICS       = "/analytics";
    public static final String TAG_ANALYTICS_API        = "Access Portal Analytics";
    public static final String TAG_ANALYTICS_API_DESC   = "Endpoints for access request analytics and reporting";
    public static final String OP_GET_ANALYTICS         = "Get access portal analytics";
    public static final String OP_GET_ANALYTICS_DESC    = "Returns aggregated analytics for access requests filtered by tenant and optional time window";
    public static final String SUCCESS_ANALYTICS_FETCHED = "Analytics fetched successfully";

    // ─── Analytics defaults ──────────────────────────────────────────
    public static final int    ANALYTICS_DEFAULT_DAYS       = 30;
    public static final int    ANALYTICS_TOP_ITEMS_LIMIT    = 5;
    public static final double ANALYTICS_DEFAULT_APPROVAL_RATE     = 0.0;
    public static final int    ANALYTICS_DEFAULT_AVG_PROCESSING    = 0;
    public static final double ANALYTICS_DEFAULT_AVG_RISK_SCORE    = 1.0;


    public static final String LOG_OBJECT_TYPE_ROLE_CHECK_ERROR = "ROLE_CHECK_ERROR";



    private AccessPortalConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}