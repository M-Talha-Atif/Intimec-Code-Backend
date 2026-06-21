package com.aidentx.workflowsservice.config;

/**
 * Application constants for Workflows Service.
 * Contains all constant values to avoid hardcoding throughout the application.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public final class WorkflowConstants {

    // ==================== LOGGER CATEGORIES ====================
    public static final String LOG_CATEGORY_WORKFLOW_AUDIT = "workflow-audit";
    public static final String LOG_CATEGORY_WORKFLOW_SERVICE = "workflow-service";
    public static final String LOG_CATEGORY_WORKFLOW_EXECUTION = "workflow-execution";
    public static final String LOG_CATEGORY_WORKFLOW_SIMULATION = "workflow-simulation";
    public static final String LOG_CATEGORY_WORKFLOW_APPROVAL = "workflow-approval";
    public static final String LOG_CATEGORY_WORKFLOW_CONTROLLER = "workflow-controller";
    public static final String LOG_CATEGORY_WORKFLOW_CONFIG = "workflow-config";
    
    // ==================== ERROR MESSAGES ====================
    public static final String ERROR_WORKFLOW_NOT_FOUND = "Workflow not found";
    public static final String ERROR_WORKFLOW_NOT_FOUND_WITH_ID = "Workflow not found with id: ";
    public static final String ERROR_WORKFLOW_ALREADY_EXISTS = "Workflow with name '%s' already exists";
    public static final String ERROR_WORKFLOW_CREATION_FAILED = "Failed to create workflow";
    public static final String ERROR_WORKFLOW_UPDATE_FAILED = "Failed to update workflow";
    public static final String ERROR_WORKFLOW_DELETION_FAILED = "Failed to delete workflow";
    public static final String ERROR_WORKFLOW_EXECUTION_FAILED = "Workflow execution failed";
    public static final String ERROR_WORKFLOW_NO_NODES = "Workflow has no nodes to execute";
    public static final String ERROR_WORKFLOW_NO_EDGES = "Workflow has no edges defined";
    public static final String ERROR_WORKFLOW_NOT_PERMITTED = "Workflow execution is not permitted";
    public static final String ERROR_WORKFLOW_NOT_ACTIVE = "Only ACTIVE workflows can be executed. Current status: ";
    public static final String ERROR_EXECUTION_NOT_FOUND = "Execution not found with id: ";
    public static final String ERROR_EXECUTION_CANNOT_CANCEL = "Cannot cancel execution with status: ";
    public static final String ERROR_EXECUTION_CANNOT_RETRY = "Only failed executions can be retried. Current status: ";
    public static final String ERROR_EXECUTION_NOT_PAUSED = "Cannot resume execution that is not PAUSED (current status: ";
    public static final String ERROR_EXECUTION_CURRENT_NODE_NULL = "Cannot resume execution: currentNodeId is null";
    public static final String ERROR_PAUSED_NODE_NOT_FOUND = "Paused node not found: ";
    public static final String ERROR_NO_START_NODE = "No start node found in workflow";
    public static final String ERROR_NODE_EXECUTION_FAILED = "Node execution failed: ";
    public static final String ERROR_NODE_DATA_MISSING = "Node data is missing for approval node";
    public static final String ERROR_APPROVER_ID_REQUIRED = "Approver ID is required for single approval";
    public static final String ERROR_INVALID_CATEGORY = "Invalid category: ";
    public static final String ERROR_INVALID_TRIGGER_TYPE = "Invalid trigger type: ";
    public static final String ERROR_INVALID_UUID = "Invalid UUID: ";
    public static final String ERROR_INVALID_EXECUTION_STATUS = "Invalid execution status: ";
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    public static final String ERROR_UNEXPECTED_ERROR = "An unexpected error occurred: ";
    public static final String ERROR_EXECUTION_CANCELLED_BY_USER = "Execution cancelled by user: ";
    
    // ==================== SUCCESS MESSAGES ====================
    public static final String SUCCESS_WORKFLOW_CREATED = "Workflow created successfully";
    public static final String SUCCESS_WORKFLOW_UPDATED = "Workflow updated successfully";
    public static final String SUCCESS_WORKFLOW_DELETED = "Workflow deleted successfully";
    public static final String SUCCESS_WORKFLOW_DUPLICATED = "Workflow duplicated successfully";
    public static final String SUCCESS_WORKFLOW_EXECUTION_STARTED = "Workflow execution started";
    public static final String SUCCESS_WORKFLOW_EXECUTIONS_RETRIEVED = "Workflow executions retrieved successfully";
    public static final String SUCCESS_EXECUTION_RETRIEVED = "Execution retrieved successfully";
    public static final String SUCCESS_EXECUTION_TRACE_RETRIEVED = "Execution trace retrieved successfully";
    public static final String SUCCESS_EXECUTION_CANCELLED = "Execution cancelled successfully";
    public static final String SUCCESS_EXECUTION_RETRY_STARTED = "Execution retry started";
    
    // ==================== VALIDATION MESSAGES ====================
    public static final String VALIDATION_WORKFLOW_NAME_REQUIRED = "Workflow name is required";
    public static final String VALIDATION_WORKFLOW_NAME_SIZE = "Workflow name must be between 3 and 255 characters";
    public static final String VALIDATION_DESCRIPTION_MAX_SIZE = "Description must not exceed 5000 characters";
    public static final String VALIDATION_CATEGORY_REQUIRED = "Category is required";
    public static final String VALIDATION_NODES_REQUIRED = "Nodes array is required";
    public static final String VALIDATION_NODES_MIN_SIZE = "At least one node is required";
    public static final String VALIDATION_EDGES_REQUIRED = "Edges array is required";
    public static final String VALIDATION_NODE_ID_REQUIRED = "Node ID is required";
    public static final String VALIDATION_NODE_TYPE_REQUIRED = "Node type is required";
    public static final String VALIDATION_EDGE_ID_REQUIRED = "Edge ID is required";
    public static final String VALIDATION_EDGE_SOURCE_REQUIRED = "Source node is required";
    public static final String VALIDATION_EDGE_TARGET_REQUIRED = "Target node is required";
    public static final String VALIDATION_SOURCE_NODE_REQUIRED = "Source node is required";
    public static final String VALIDATION_TARGET_NODE_REQUIRED = "Target node is required";
    
    // ==================== VALIDATION CONSTRAINTS ====================
    public static final int WORKFLOW_NAME_MIN_SIZE = 3;
    public static final int WORKFLOW_NAME_MAX_SIZE = 255;
    public static final int DESCRIPTION_MAX_SIZE = 5000;
    public static final int DEFAULT_APPROVAL_TIMEOUT_MINUTES = 1440; // 24 hours
    
    // ==================== LOG MESSAGES ====================
    public static final String LOG_CREATING_WORKFLOW = "Creating new workflow: name={}, createdBy={}, triggerType={}, category={}";
    public static final String LOG_WORKFLOW_CREATED = "Created workflow: name={}, workflowId={}, version={}, status={}, triggerType={}";
    public static final String LOG_UPDATING_WORKFLOW = "Updating workflow: workflowId={}, modifiedBy={}";
    public static final String LOG_WORKFLOW_UPDATED = "Updated workflow: workflowId={}, name={}, version={}, structuralChange={}, changes={}";
    public static final String LOG_DELETING_WORKFLOW = "Deleting workflow: workflowId={}";
    public static final String LOG_WORKFLOW_DELETED = "Deleted workflow: workflowId={}, name={}, triggerType={}";
    public static final String LOG_DUPLICATING_WORKFLOW = "Duplicating workflow: {} with new name: {}";
    public static final String LOG_WORKFLOW_DUPLICATED = "Duplicated workflow: {} -> {}";
    public static final String LOG_CREATED_WORKFLOW = "Created workflow: {} with ID: {}";
    public static final String LOG_EXECUTING_WORKFLOW = "Executing workflow: {} for tenant: {} by user: {}";
    public static final String LOG_GETTING_EXECUTION = "Getting execution: {}";
    public static final String LOG_GETTING_EXECUTION_TRACE = "Getting execution trace: {}";
    public static final String LOG_CANCELLING_EXECUTION = "Cancelling execution: {} by user: {}";
    public static final String LOG_EXECUTION_CANCELLED = "Execution {} cancelled by user: {}";
    public static final String LOG_RETRYING_EXECUTION = "Retrying execution: {} by user: {}";
    public static final String LOG_RETRY_STARTED = "Retrying execution {} as new execution";
    public static final String LOG_GETTING_EXECUTIONS_FOR_WORKFLOW = "Getting executions for workflow: {}";
    public static final String LOG_STARTING_EXECUTION = "Starting execution of workflow: {} by user: {}";
    public static final String LOG_DISPATCHING_EXECUTION = "Dispatching workflow execution to background: executionId={}";
    public static final String LOG_INVALID_TRIGGERED_BY_UUID = "Invalid triggeredBy UUID: {}";
    public static final String LOG_STARTING_WORKFLOW_EXECUTION = "Starting workflow execution: {} (workflow: {})";
    public static final String LOG_WORKFLOW_EXECUTION_PAUSED = "Workflow execution paused at node: {}";
    public static final String LOG_WORKFLOW_EXECUTION_COMPLETED = "Workflow execution completed: {}";
    public static final String LOG_WORKFLOW_COMPLETION_NOTIFICATION_SENT = "Sent workflow completion notification to requester: {}";
    public static final String LOG_WORKFLOW_COMPLETION_NOTIFICATION_FAILED = "Failed to send workflow completion notification: {}";
    public static final String LOG_WORKFLOW_EXECUTION_FAILED = "Workflow execution failed: {}";
    public static final String LOG_RESUMING_WORKFLOW_EXECUTION = "Resuming workflow execution: {} from node: {}";
    public static final String LOG_RESUMING_FROM_NODE = "Resuming from node {} to {} next nodes";
    public static final String LOG_WORKFLOW_EXECUTION_RESUMED = "Workflow execution resumed and completed: {}";
    public static final String LOG_WORKFLOW_RESUME_FAILED = "Workflow resume failed: {}";
    public static final String LOG_EXECUTING_NODE = "Executing node: {} (type: {})";
    public static final String LOG_PAUSING_EXECUTION_AT_NODE = "Pausing execution at node: {} (reason: {})";
    public static final String LOG_EXECUTION_PAUSED_SUCCESSFULLY = "Execution paused successfully: executionId={}, nodeId={}";
    public static final String LOG_REACHED_END_NODE = "Reached end node: {}";
    public static final String LOG_NO_NEXT_NODES = "No next nodes found for node: {}";
    public static final String LOG_SKIPPING_NODE_CONDITION = "Skipping node {} due to condition evaluation";
    public static final String LOG_NODE_EXECUTION_FAILED = "Node execution failed: {} - {}";
    public static final String LOG_EXECUTING_NODE_WITH_TYPE = "Executing node: {} with type: {}";
    public static final String LOG_NODE_NO_TYPE = "Node has no type: {}";
    public static final String LOG_UNKNOWN_NODE_TYPE = "Unknown node type: {}. Executing as no-op.";
    public static final String LOG_STARTING_WORKFLOW = "Starting workflow execution";
    public static final String LOG_ENDING_WORKFLOW = "Ending workflow execution";
    public static final String LOG_EXECUTING_SINGLE_APPROVAL_NODE = "Executing single approval node";
    public static final String LOG_SIMULATION_MODE_AUTO_APPROVE = "Simulation mode: auto-approving";
    public static final String LOG_CREATED_APPROVAL_REQUEST = "Created approval request: {} for approver: {}";
    public static final String LOG_EXECUTING_MULTI_APPROVAL_NODE = "Executing multi-approval node";
    public static final String LOG_TRACE_ENTERING_MULTI_APPROVAL = "Entering executeApprovalMulti method - detailed trace";
    public static final String LOG_TRACE_RESOLVED_CONTEXT_APPROVERS = "Resolved approvers from context - dynamic resolution";
    public static final String LOG_TRACE_GENERATED_APPROVAL_GROUP_ID = "Generated approval group ID for multi-approval tracking";
    public static final String LOG_TRACE_EVALUATING_CONDITION = "Evaluating condition expression - detailed trace";
    public static final String LOG_TRACE_HTTP_REQUEST_PREPARED = "HTTP request prepared with headers and body";
    public static final String LOG_TRACE_HTTP_RESPONSE_RECEIVED = "HTTP response received - detailed trace";
    public static final String LOG_TRACE_CONTEXT_UPDATED = "Execution context updated with node output";
    public static final String LOG_TRACE_PARALLEL_BRANCH_CREATED = "Parallel branch created for execution";
    public static final String LOG_TRACE_APPROVER_RESOLUTION_START = "Starting dynamic approver resolution";
    public static final String LOG_TRACE_TEMPLATE_VARIABLE_REPLACED = "Template variable replaced with context value";
    public static final String LOG_TRACE_NODE_TRANSITION = "Transitioning from node to next node in execution path";
    public static final String LOG_MULTI_APPROVAL_NOT_IMPLEMENTED = "Multi-approval not fully implemented yet - treating as single approval";
    public static final String LOG_EXECUTING_DYNAMIC_APPROVAL_NODE = "Executing dynamic approval node";
    public static final String LOG_DYNAMIC_APPROVAL_NOT_IMPLEMENTED = "Dynamic approval not fully implemented yet - treating as single approval";
    public static final String LOG_EXECUTING_IF_CONDITION_NODE = "Executing if-condition node";
    public static final String LOG_EXECUTING_SWITCH_CONDITION_NODE = "Executing switch-condition node";
    public static final String LOG_EXECUTING_PROVISION_ACTION = "Executing provision action";
    public static final String LOG_SIMULATION_MODE_SKIPPING_PROVISIONING = "Simulation mode: skipping actual provisioning";
    public static final String LOG_NOTIFICATION_SENT_KAFKA = "Notification sent via Kafka to user={}, type={}";
    public static final String LOG_NOTIFICATION_RECIPIENTS_MISSING = "Missing notification recipients";
    public static final String LOG_RESOURCE_NOT_FOUND = "Resource not found: {}";
    public static final String LOG_VALIDATION_ERROR = "Validation error: {}";
    public static final String LOG_VALIDATION_ERRORS = "Validation errors: {}";
    public static final String LOG_UNEXPECTED_ERROR = "Unexpected error";
    public static final String LOG_FAILED_TO_EXTRACT_JWT = "Failed to extract JWT claims: {}";
    public static final String LOG_FAILED_TO_EXTRACT_TENANT_ID = "Failed to extract tenantId from JWT token: {}";
    public static final String LOG_FAILED_TO_EXTRACT_USER_ID = "Failed to extract userId from JWT token: {}";
    public static final String LOG_FAILED_TO_EXTRACT_USERNAME = "Failed to extract username from JWT token: {}";
    public static final String LOG_EDGE_HAS_CONDITION = "Edge has condition (not yet evaluated): {}";
    
    // Identity/Employee operations
    public static final String LOG_EMPLOYEE_CREATED_SUCCESS = "Employee created successfully";
    public static final String LOG_EMPLOYEE_CREATION_FAILED = "Failed to create employee";
    public static final String LOG_IDENTITY_RETRIEVED_SUCCESS = "Identity retrieved successfully";
    public static final String LOG_IDENTITY_RETRIEVAL_FAILED = "Failed to retrieve identity";
    public static final String LOG_IDENTITY_DELETED_SUCCESS = "Identity deleted successfully";
    public static final String LOG_IDENTITY_DELETION_FAILED = "Failed to delete identity";
    public static final String LOG_IDENTITY_EXISTS_CHECK = "Checking if identity exists";
    public static final String LOG_IDENTITY_NOT_FOUND = "Identity not found";
    public static final String LOG_IDENTITY_CHECK_FAILED = "Identity existence check failed";
    
    // Approval operations
    public static final String LOG_APPROVAL_REQUEST_NOTIFICATION_SENT = "Approval request notification sent";
    public static final String LOG_APPROVAL_DECISION_NOTIFICATION_SENT = "Approval decision notification sent";
    public static final String LOG_REJECTION_NOTIFICATION_SENT = "Rejection notification sent";
    public static final String LOG_GETTING_WORKFLOW_EXECUTIONS = "Getting workflow executions";
    
    // Tenant ID extraction
    public static final String LOG_TENANT_ID_EXTRACTION_FAILED = "Failed to extract tenant ID from JWT";
    
    // Node Executor - Provision/Deprovision actions
    public static final String LOG_UNKNOWN_PROVISION_ACTION_TYPE = "Unknown provision action type: {}";
    public static final String LOG_ERROR_EXECUTING_PROVISION_ACTION = "Error executing provision action";
    public static final String LOG_FAILED_TO_PARSE_HIRE_DATE = "Failed to parse hireDate: {}";
    public static final String LOG_CREATING_EMPLOYEE_VIA_REST = "Creating employee via REST: username={}, email={}, department={}";
    public static final String LOG_EMPLOYEE_CREATED_WITH_ID = "Employee created successfully with ID: {}";
    public static final String LOG_FAILED_TO_CREATE_EMPLOYEE_REST = "Failed to create employee via REST";
    public static final String LOG_CANNOT_DELETE_EMPLOYEE_NULL_ID = "Cannot delete employee: identityId is null";
    public static final String LOG_CANNOT_DELETE_EMPLOYEE_NULL_TENANT = "Cannot delete employee: tenantId is null or empty";
    public static final String LOG_EMPLOYEE_DELETED_SUCCESSFULLY = "Employee deleted successfully: identityId={}, tenantId={}";
    public static final String LOG_FAILED_TO_DELETE_EMPLOYEE_GRPC = "Failed to delete employee via gRPC";
    public static final String LOG_EXECUTING_DEPROVISION_ACTION = "Executing deprovision action";
    public static final String LOG_CANNOT_DEPROVISION_NULL_ID = "Cannot deprovision: identityId is null";
    public static final String LOG_CANNOT_DEPROVISION_NULL_TENANT = "Cannot deprovision: tenantId is null or empty";
    public static final String LOG_IDENTITY_DEPROVISIONED_SUCCESSFULLY = "Identity deprovisioned successfully: identityId={}, tenantId={}";
    public static final String LOG_FAILED_TO_DEPROVISION_IDENTITY = "Failed to deprovision identity via gRPC: {}";
    public static final String LOG_EXECUTING_NOTIFY_ACTION = "Executing notify action";
    public static final String LOG_FAILED_TO_SEND_NOTIFICATION = "Failed to send notification";
    public static final String LOG_EXECUTING_SCRIPT_ACTION = "Executing script action";
    public static final String LOG_EXECUTING_SCRIPT_MOCK = "Executing script (mock): {}";
    public static final String LOG_EXECUTING_DELAY_WAIT_NODE = "Executing delay/wait node";
    public static final String LOG_WAIT_DELAY_MOCK = "Wait delay (mock): {} seconds";
    public static final String LOG_EXECUTING_SCHEDULE_NODE = "Executing schedule node";
    public static final String LOG_SCHEDULED_TIME_MOCK = "Scheduled time (mock): {}";
    
    // ==================== AUDIT ACTION NAMES ====================
    public static final String AUDIT_ACTION_WORKFLOW_CREATED = "workflow-created";
    public static final String AUDIT_ACTION_WORKFLOW_UPDATED = "workflow-updated";
    public static final String AUDIT_ACTION_WORKFLOW_DELETED = "workflow-deleted";
    public static final String AUDIT_ACTION_WORKFLOW_EXECUTED = "workflow-executed";
    public static final String AUDIT_ACTION_WORKFLOW_SIMULATED = "workflow-simulated";
    public static final String AUDIT_ACTION_EXECUTION_CANCELLED = "Workflow-Execution-Cancelled";
    public static final String AUDIT_ACTION_APPROVAL_CREATED = "approval-created";
    public static final String AUDIT_ACTION_APPROVAL_APPROVED = "approval-approved";
    public static final String AUDIT_ACTION_APPROVAL_REJECTED = "approval-rejected";
    
    // ==================== NOTIFICATION TYPES ====================
    public static final String NOTIFICATION_TYPE_WORKFLOW_UPDATE = "workflow_update";
    public static final String NOTIFICATION_TYPE_WORKFLOW_COMPLETED = "workflow_completed";
    public static final String NOTIFICATION_TYPE_WORKFLOW_FAILED = "workflow_failed";
    public static final String NOTIFICATION_TYPE_APPROVAL_PENDING = "approval_pending";
    public static final String NOTIFICATION_TYPE_APPROVAL_APPROVED = "approval_approved";
    public static final String NOTIFICATION_TYPE_APPROVAL_REJECTED = "approval_rejected";
    
    // ==================== KAFKA TOPICS ====================
    public static final String KAFKA_TOPIC_AUDIT_LOG = "audit-log";
    public static final String KAFKA_TOPIC_WORKFLOW_NOTIFICATIONS = "workflow-notifications";
    public static final String KAFKA_TOPIC_IDENTITY_LIFECYCLE = "identity-lifecycle-events";
    public static final String KAFKA_TOPIC_ROLE_MANAGEMENT = "role-management-events";
    
    // ==================== CONTEXT KEYS ====================
    public static final String CONTEXT_KEY_WORKFLOW_NAME = "workflowName";
    public static final String CONTEXT_KEY_WORKFLOW_ID = "workflowId";
    public static final String CONTEXT_KEY_EXECUTION_ID = "executionId";
    public static final String CONTEXT_KEY_TRIGGERED_BY = "triggeredBy";
    public static final String CONTEXT_KEY_NOTIFICATION_TYPE = "notificationType";
    public static final String CONTEXT_KEY_SIMULATION_MODE = "simulationMode";
    public static final String CONTEXT_KEY_TENANT_ID = "tenantId";
    public static final String CONTEXT_KEY_USER_ID = "userId";
    public static final String CONTEXT_KEY_NODE_ID = "nodeId";
    public static final String CONTEXT_KEY_NODE_NAME = "nodeName";
    public static final String CONTEXT_KEY_NODE_TYPE = "nodeType";
    public static final String CONTEXT_KEY_EXECUTED = "executed";
    public static final String CONTEXT_KEY_TIMESTAMP = "timestamp";
    public static final String CONTEXT_KEY_STATUS = "status";
    public static final String CONTEXT_KEY_APPROVED = "approved";
    public static final String CONTEXT_KEY_APPROVER = "approver";
    public static final String CONTEXT_KEY_PAUSE_REQUIRED = "pauseRequired";
    public static final String CONTEXT_KEY_REASON = "reason";
    public static final String CONTEXT_KEY_APPROVAL_ID = "approvalId";
    public static final String CONTEXT_KEY_APPROVER_ID = "approverId";
    public static final String CONTEXT_KEY_SIMULATED = "simulated";
    public static final String CONTEXT_KEY_CONDITION_MET = "conditionMet";
    public static final String CONTEXT_KEY_NOTE = "note";
    public static final String CONTEXT_KEY_SELECTED_BRANCH = "selectedBranch";
    public static final String CONTEXT_KEY_ERROR = "error";
    public static final String CONTEXT_KEY_STACK_TRACE = "stackTrace";
    
    // ==================== NODE TYPES ====================
    public static final String NODE_TYPE_TRIGGER_START = "trigger.start";
    public static final String NODE_TYPE_TRIGGER_END = "trigger.end";
    public static final String NODE_TYPE_APPROVAL_SINGLE = "approval.single";
    public static final String NODE_TYPE_APPROVAL_MULTI = "approval.multi";
    public static final String NODE_TYPE_APPROVAL_DYNAMIC = "approval.dynamic";
    public static final String NODE_TYPE_CONDITION_IF = "condition.if";
    public static final String NODE_TYPE_CONDITION_SWITCH = "condition.switch";
    public static final String NODE_TYPE_ACTION_PROVISION = "action.provision";
    public static final String NODE_TYPE_ACTION_DEPROVISION = "action.deprovision";
    public static final String NODE_TYPE_ACTION_NOTIFY = "action.notify";
    public static final String NODE_TYPE_ACTION_SCRIPT = "action.script";
    public static final String NODE_TYPE_ACTION_HTTP = "action.http";
    
    // Template-based actions are loaded from action-templates.json
    // No constants needed - any action.* not in switch goes to template executor
    // Examples: action.role.activate, action.user.create, action.permission.grant
    
    public static final String NODE_TYPE_DELAY_WAIT = "delay.wait";
    public static final String NODE_TYPE_DELAY_SCHEDULE = "delay.schedule";
    
    // ==================== OBJECT TYPES (for audit/logging) ====================
    // HTTP action context keys
    public static final String CONTEXT_KEY_HTTP_METHOD = "httpMethod";
    public static final String CONTEXT_KEY_HTTP_URL = "httpUrl";
    public static final String CONTEXT_KEY_HTTP_STATUS = "httpStatusCode";
    public static final String CONTEXT_KEY_HTTP_RESPONSE = "httpResponse";
    public static final String CONTEXT_KEY_HTTP_ERROR = "httpError";
    public static final String CONTEXT_KEY_HTTP_SUCCESS = "httpSuccess";

    // Workflow-origin propagation (prevents workflow-trigger loops)
    public static final String HEADER_WORKFLOW_EXECUTION_ID = "X-Workflow-Execution-Id";
    public static final String METADATA_KEY_WORKFLOW_EXECUTION_ID = "workflowExecutionId";
    
    public static final String OBJECT_TYPE_WORKFLOW = "workflow";
    public static final String OBJECT_TYPE_WORKFLOW_EXECUTION = "workflow-execution";
    public static final String OBJECT_TYPE_WORKFLOW_APPROVAL = "workflow-approval";
    
    // ==================== API DOCUMENTATION ====================
    public static final String API_TAG_WORKFLOWS = "Workflows";
    public static final String API_TAG_WORKFLOW_EXECUTION = "Workflow Execution";
    public static final String API_DESC_WORKFLOWS = "Workflow CRUD operations - Create, Read, Update, Delete workflows";
    public static final String API_DESC_WORKFLOW_EXECUTION = "Execute and monitor workflow executions";
    public static final String API_DESC_CREATE_WORKFLOW = "Creates a new workflow with nodes, edges, and metadata. Returns the created workflow with generated ID and version 1.0.";
    public static final String API_DESC_EXECUTE_WORKFLOW = "Start a new execution of the specified workflow";
    public static final String API_DESC_GET_EXECUTION = "Retrieve detailed information about a workflow execution";
    public static final String API_DESC_GET_EXECUTION_TRACE = "Get the path trace showing which nodes were executed";
    public static final String API_DESC_CANCEL_EXECUTION = "Cancel a running workflow execution";
    public static final String API_DESC_GET_WORKFLOW_EXECUTIONS = "List all executions of a specific workflow";
    public static final String API_DESC_RETRY_EXECUTION = "Retry a failed workflow execution";
    
    // API Summaries
    public static final String API_SUMMARY_CREATE_WORKFLOW = "Create a new workflow";
    public static final String API_SUMMARY_EXECUTE_WORKFLOW = "Execute a workflow";
    public static final String API_SUMMARY_GET_EXECUTION = "Get execution details";
    public static final String API_SUMMARY_GET_EXECUTION_TRACE = "Get execution trace/logs";
    public static final String API_SUMMARY_CANCEL_EXECUTION = "Cancel a running execution";
    public static final String API_SUMMARY_GET_WORKFLOW_EXECUTIONS = "Get all executions for a workflow";
    public static final String API_SUMMARY_RETRY_EXECUTION = "Retry a failed execution";
    
    // API Parameter Descriptions
    public static final String API_DESC_WORKFLOW_CREATION_REQUEST = "Workflow creation request";
    public static final String API_DESC_USER_ID_PARAM = "User ID from header";
    public static final String API_DESC_WORKFLOW_ID_PARAM = "Workflow ID";
    public static final String API_DESC_EXECUTION_ID_PARAM = "Execution ID";
    public static final String API_DESC_PAGE_NUMBER_PARAM = "Page number (0-based)";
    public static final String API_DESC_PAGE_SIZE_PARAM = "Page size";
    public static final String API_DESC_SORT_FIELD_PARAM = "Sort field";
    public static final String API_DESC_SORT_DIRECTION_PARAM = "Sort direction (ASC/DESC)";
    public static final String API_DESC_FILTER_STATUS_PARAM = "Filter by execution status";
    public static final String API_RESPONSE_200_TRACE_DESCRIPTION = "Execution trace retrieved successfully";
    
    // ==================== API RESPONSE CODES ====================
    public static final String API_RESPONSE_200_DESCRIPTION = "Success";
    public static final String API_RESPONSE_201_DESCRIPTION = "Workflow created successfully";
    public static final String API_RESPONSE_202_DESCRIPTION = "Execution started";
    public static final String API_RESPONSE_400_DESCRIPTION = "Invalid request - validation failed";
    public static final String API_RESPONSE_404_DESCRIPTION = "Workflow not found";
    public static final String API_RESPONSE_409_DESCRIPTION = "Workflow name already exists";
    public static final String API_RESPONSE_EXECUTION_CANCELLED_DESCRIPTION = "Execution cancelled";
    public static final String API_RESPONSE_EXECUTION_CANNOT_CANCEL_DESCRIPTION = "Execution cannot be cancelled";
    public static final String API_RESPONSE_EXECUTION_CANNOT_RETRY_DESCRIPTION = "Execution cannot be retried";
    public static final String API_RESPONSE_TRACE_RETRIEVED_DESCRIPTION = "Trace retrieved";
    public static final String API_RESPONSE_EXECUTIONS_RETRIEVED_DESCRIPTION = "Executions retrieved";
    public static final String API_RESPONSE_RETRY_STARTED_DESCRIPTION = "Retry started";
    public static final String API_RESPONSE_EXECUTION_NOT_FOUND_DESCRIPTION = "Execution not found";
    public static final String API_RESPONSE_EXECUTION_FOUND_DESCRIPTION = "Execution found";
    public static final String API_RESPONSE_INVALID_EXECUTION_REQUEST_DESCRIPTION = "Invalid execution request";
    
    // ==================== HEADER CONSTANTS ====================
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_X_USER_ID = "X-User-ID";
    public static final String HEADER_USER_ID = "X-User-ID"; // Alias for consistency
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_BEARER_PREFIX = "Bearer ";
    
    // ==================== DEFAULT VALUES ====================
    // NOTE: DEFAULT_TENANT_ID removed - all operations must have explicit tenant from event payload or JWT
    // No hardcoded tenant fallbacks are allowed to ensure proper multi-tenant isolation
    public static final String DEFAULT_REQUESTER_EMAIL = "requester@example.com";
    public static final String DEFAULT_REQUESTER_NAME = "Requester";
    public static final String DEFAULT_APPROVER_AUTO = "simulation-auto-approve";
    public static final String DEFAULT_APPROVAL_LABEL = "Approval Required";
    public static final String DEFAULT_STATUS_COMPLETED = "completed";
    public static final String DEFAULT_STATUS_WOULD_PROVISION = "would-provision";
    public static final String DEFAULT_SELECTED_BRANCH = "default";
    public static final String DEFAULT_SORT_BY_UPDATED_AT = "updatedAt";
    public static final String DEFAULT_SORT_BY_STARTED_AT = "startedAt";
    public static final String DEFAULT_SORT_DIR_DESC = "DESC";
    public static final String DEFAULT_SORT_DIR_ASC = "ASC";
    public static final String DEFAULT_TRIGGER_TYPE_MANUAL = "MANUAL";
    public static final String DEFAULT_TRIGGER_TYPE_RETRY = "RETRY";
    public static final String DEFAULT_TRIGGER_TYPE_EVENT = "EVENT";
    public static final String DEFAULT_TRIGGER_TYPE_TIME = "TIME";
    public static final String DEFAULT_WORKFLOW_NAME = "Workflow";
    public static final String DEFAULT_STACK_TRACE_UNKNOWN = "unknown";
    public static final String DEFAULT_DUPLICATE_SUFFIX = " (Copy)";
    public static final String DEFAULT_VERSION = "1.0";
    public static final String DEFAULT_APPROVAL_COMMENT_APPROVED = "Approved";
    public static final String DEFAULT_APPROVAL_COMMENT_REJECTED = "Rejected";
    public static final double DEFAULT_FAILURE_THRESHOLD = 0.3; // 30% - Phase 7
    public static final String DEFAULT_FAILURE_POLICY = "FAIL_FAST"; // Phase 7
    
    // ==================== ADDITIONAL EXECUTION CONTEXT KEYS ====================
    public static final String CONTEXT_KEY_TRIGGER = "trigger";
    public static final String CONTEXT_KEY_TRIGGER_TYPE = "triggerType";
    public static final String CONTEXT_KEY_EVENT_TYPE = "eventType";
    public static final String CONTEXT_KEY_EVENT_ID = "eventId";
    public static final String CONTEXT_KEY_SOURCE_SERVICE = "sourceService";
    public static final String CONTEXT_KEY_CORRELATION_ID = "correlationId";
    public static final String CONTEXT_KEY_SCHEDULED_TIME = "scheduledTime";
    public static final String CONTEXT_KEY_CRON_EXPRESSION = "cronExpression";
    public static final String CONTEXT_KEY_NEXT_FIRE_TIME = "nextFireTime";
    
    // ==================== HEALTH ENDPOINT ====================
    public static final String HEALTH_STATUS = "status";
    public static final String HEALTH_STATUS_UP = "UP";
    public static final String HEALTH_SERVICE = "service";
    public static final String HEALTH_VERSION = "version";
    public static final String SERVICE_VERSION = "1.0.0";
    public static final String SUCCESS_SERVICE_HEALTHY = "Service is healthy";
    public static final String PING_RESPONSE = "pong";
    
    // ==================== JWT CLAIMS ====================
    public static final String CLAIM_TENANT_ID = "tenantId";
    
    // ==================== VERSION/CHANGELOG MESSAGES ====================
    public static final String VERSION_INITIAL = "Initial version";
    public static final String VERSION_DUPLICATED_FROM = "Duplicated from %s";
    public static final String VERSION_NAME_CHANGED = "Name changed; ";
    public static final String VERSION_DESCRIPTION_UPDATED = "Description updated; ";
    public static final String VERSION_STATUS_CHANGED = "Status changed to ";
    public static final String VERSION_CATEGORY_CHANGED = "Category changed; ";
    public static final String VERSION_NODES_UPDATED = "Nodes updated; ";
    public static final String VERSION_EDGES_UPDATED = "Edges updated; ";
    
    // ==================== MISCELLANEOUS ====================
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_TARGET = "target";
    public static final String FIELD_CONDITION = "condition";
    public static final String FIELD_LABEL = "label";
    public static final String FIELD_POSITION = "position";
    public static final String FIELD_X = "x";
    public static final String FIELD_Y = "y";
    public static final String FIELD_SOURCE_HANDLE = "sourceHandle";
    public static final String FIELD_TARGET_HANDLE = "targetHandle";
    public static final String FIELD_ACTION = "action";
    public static final String FIELD_TIMEOUT_MINUTES = "timeoutMinutes";
    public static final String FIELD_CAN_EXECUTE = "canExecute";
    public static final String FIELD_FAILURE_POLICY = "failurePolicy";
    public static final String FIELD_FAILURE_THRESHOLD = "failureThreshold";
    public static final String ACTION_TYPE_CREATE_USER = "create_user";
    public static final String ACTION_TYPE_CREATE_EMPLOYEE = "create_employee";
    public static final String NOTE_PHASE_5_CONDITION_EVALUATION = "Phase 5 will implement condition evaluation";
    public static final String TODO_GET_FROM_IDENTITY_SERVICE = "TODO: Get from identity-service";
    public static final String TODO_FETCH_REQUESTER_DETAILS = "TODO: Fetch real requester details from identity-service";
    
    // ==================== SWAGGER CONFIG ====================
    public static final String SWAGGER_TITLE = "Workflows Service API";
    public static final String SWAGGER_CONTACT_NAME = "AidentX Team";
    public static final String SWAGGER_CONTACT_EMAIL = "support@aidentx.com";
    public static final String SWAGGER_LICENSE_NAME = "Proprietary";
    public static final String SWAGGER_LICENSE_URL = "https://aidentx.com/license";
    public static final String SWAGGER_SERVER_DESCRIPTION_LOCAL = "Local Development Server";
    public static final String SWAGGER_SERVER_URL_LOCAL_PREFIX = "http://localhost:";
    
    // ==================== PARAMETER DESCRIPTIONS ====================
    public static final String PARAM_DESC_WORKFLOW_ID = "Workflow ID";
    public static final String PARAM_DESC_EXECUTION_ID = "Execution ID";
    public static final String PARAM_DESC_USER_ID = "UUID of the user creating the workflow";
    public static final String PARAM_DESC_PAGE_NUMBER = "Page number (0-based)";
    
    // ==================== ADDITIONAL ERROR MESSAGES ====================
    public static final String ERROR_APPROVAL_NOT_FOUND = "Approval not found: ";
    public static final String ERROR_APPROVAL_NOT_AUTHORIZED = "User is not authorized to approve this request";
    public static final String ERROR_APPROVAL_NOT_AUTHORIZED_REJECT = "User is not authorized to reject this request";
    public static final String ERROR_APPROVAL_NOT_AUTHORIZED_DELEGATE = "User is not authorized to delegate this request";
    public static final String ERROR_APPROVAL_ALREADY_DECIDED = "Approval already decided: ";
    public static final String ERROR_APPROVAL_CANNOT_DELEGATE_DECIDED = "Cannot delegate a decided approval";
    public static final String ERROR_TENANT_ID_REQUIRED = "tenantId is required for creating employee";
    public static final String ERROR_TENANT_ID_REQUIRED_DELETE = "tenantId is required for deleting identity";
    public static final String ERROR_CREATE_EMPLOYEE_FAILED = "Failed to create employee [%s]: %s";
    public static final String ERROR_GET_IDENTITY_FAILED = "Failed to get identity [%s]: %s";
    public static final String ERROR_DELETE_IDENTITY_FAILED = "Failed to delete identity [%s]: %s";
    public static final String ERROR_CHECK_IDENTITY_EXISTS_FAILED = "Service unavailable while checking identity [%s]: %s";
    
    // ==================== ADDITIONAL LOG MESSAGES ====================
    public static final String LOG_CREATING_APPROVAL_REQUEST = "Creating approval request for execution: {}, approver: {}";
    public static final String LOG_SENT_APPROVAL_REQUEST_NOTIFICATION = "Sent approval request notification to approver: {}";
    public static final String LOG_FAILED_APPROVAL_REQUEST_NOTIFICATION = "Failed to send approval request notification: {}";
    public static final String LOG_APPROVING_REQUEST = "Approving approval request: {} by user: {}";
    public static final String LOG_TRIGGERING_WORKFLOW_RESUME = "Triggering workflow resume for execution: {}";
    public static final String LOG_SENT_APPROVAL_DECISION_NOTIFICATION = "Sent approval decision notification to workflow requester: {}";
    public static final String LOG_FAILED_APPROVAL_DECISION_NOTIFICATION = "Failed to send approval decision notification: {}";
    public static final String LOG_REJECTING_REQUEST = "Rejecting approval request: {} by user: {}";
    public static final String LOG_APPROVAL_REJECTED_WORKFLOW_FAIL = "Approval rejected - workflow execution will be marked as failed";
    public static final String LOG_SENT_REJECTION_NOTIFICATION = "Sent rejection notification to workflow requester: {}";
    public static final String LOG_FAILED_REJECTION_NOTIFICATION = "Failed to send rejection notification: {}";
    public static final String LOG_DELEGATING_APPROVAL = "Delegating approval {} from {} to {}";
    public static final String LOG_DELEGATION_NOTIFICATION_SENT = "Delegation notification sent to user: {}";
    public static final String LOG_PROCESSING_TIMED_OUT_APPROVALS = "Processing timed-out approvals";
    public static final String LOG_APPROVAL_TIMED_OUT = "Approval {} timed out for execution {}";
    public static final String LOG_PROCESSED_TIMED_OUT_APPROVALS = "Processed {} timed-out approvals";
    public static final String LOG_GRPC_WARN_CREATE_EMPLOYEE_NOT_IMPLEMENTED = "createEmployee via gRPC is NOT IMPLEMENTED in identity-service. Use IdentityRestClient instead.";
    public static final String LOG_GRPC_CREATING_EMPLOYEE = "Creating employee via gRPC: name={}, email={}";
    public static final String LOG_GRPC_EMPLOYEE_CREATED = "Employee created successfully: id={}";
    public static final String LOG_GRPC_CREATE_EMPLOYEE_FAILED = "Failed to create employee via gRPC: status={}, description={}";
    public static final String LOG_GRPC_GETTING_IDENTITY = "Getting identity by ID via gRPC: id={}";
    public static final String LOG_GRPC_IDENTITY_RETRIEVED = "Identity retrieved successfully: id={}";
    public static final String LOG_GRPC_GET_IDENTITY_FAILED = "Failed to get identity via gRPC: status={}, description={}";
    public static final String LOG_GRPC_DELETING_IDENTITY = "Deleting identity via gRPC: id={}, tenantId={}";
    public static final String LOG_GRPC_IDENTITY_DELETED = "Identity deleted successfully: id={}, tenantId={}";
    public static final String LOG_GRPC_DELETE_IDENTITY_FAILED = "Failed to delete identity via gRPC: status={}, description={}";
    public static final String LOG_GRPC_CHECKING_IDENTITY_EXISTS = "Checking if identity exists via gRPC: id={}";
    public static final String LOG_GRPC_IDENTITY_EXISTS_CHECK = "Identity exists check: id={}, exists={}";
    public static final String LOG_GRPC_CHECK_IDENTITY_EXISTS_FAILED = "Failed to check identity existence via gRPC: status={}, description={}";
    public static final String LOG_GRPC_IDENTITY_NOT_FOUND = "Identity not found: id={}";
    
    // ==================== ADDITIONAL DEFAULT VALUES ====================
    public static final String DEFAULT_APPROVER_EMAIL = "approver@example.com";
    public static final String DEFAULT_APPROVER_NAME = "Approver";
    public static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000000";
    public static final String DEFAULT_APPROVAL_COMMENTS_APPROVED = "Approved";
    public static final String DEFAULT_APPROVAL_COMMENTS_REJECTED = "Rejected";
    public static final String DEFAULT_DELEGATION_REASON = "Delegated by user";
    public static final String PARAM_DESC_PAGE_SIZE = "Page size";
    public static final String PARAM_DESC_SORT_FIELD = "Sort field";
    public static final String PARAM_DESC_SORT_DIRECTION = "Sort direction (ASC/DESC)";
    public static final String PARAM_DESC_FILTER_BY_STATUS = "Filter by execution status";
    public static final String PARAM_DESC_WORKFLOW_CREATION_REQUEST = "Workflow creation request with nodes, edges, and metadata";
    
    // ==================== APPROVAL STATS KEYS ====================
    public static final String STATS_KEY_TOTAL = "total";
    public static final String STATS_KEY_PENDING = "pending";
    public static final String STATS_KEY_APPROVED = "approved";
    public static final String STATS_KEY_REJECTED = "rejected";
    public static final String STATS_KEY_TIMEOUT = "timeout";
    public static final String STATS_KEY_DELEGATED = "delegated";
    
    // ==================== TEMPLATE ERRORS ====================
    public static final String ERROR_TEMPLATE_NAME_EXISTS = "Template with name '%s' already exists";
    public static final String ERROR_TEMPLATE_NOT_FOUND = "Template not found: ";
    public static final String ERROR_CANNOT_DELETE_SYSTEM_TEMPLATE = "Cannot delete system templates";
    public static final String ERROR_CANNOT_EDIT_SYSTEM_TEMPLATE = "Cannot edit system templates";
    
    // ==================== TEMPLATE LOG MESSAGES ====================
    public static final String LOG_CREATING_TEMPLATE = "Creating new template";
    public static final String LOG_UPDATING_TEMPLATE = "Updating template";
    public static final String LOG_DELETING_TEMPLATE = "Deleting template";
    public static final String LOG_LISTING_TEMPLATES = "Listing templates";
    public static final String LOG_GETTING_TEMPLATE = "Getting template";
    public static final String LOG_INSTANTIATING_TEMPLATE = "Instantiating template as workflow";
    public static final String LOG_TEMPLATE_INSTANTIATED = "Template instantiated successfully";
    public static final String LOG_WORKFLOW_NAME_EXISTS = "Workflow name already exists, using unique name";
    
    // ==================== CONTROLLER LOG MESSAGES ====================
    public static final String LOG_GETTING_ALL_EXECUTIONS = "Getting all executions";
    public static final String LOG_GETTING_EXECUTION_STATISTICS = "Getting global execution statistics";
    public static final String LOG_GETTING_APPROVALS_FOR_EXECUTION = "Getting approvals for execution";
    public static final String LOG_GETTING_APPROVAL = "Getting approval";
    public static final String LOG_APPROVING_APPROVAL = "Approving approval";
    public static final String LOG_REJECTING_APPROVAL = "Rejecting approval";
    public static final String LOG_GETTING_APPROVAL_STATS = "Getting approval stats";
    public static final String LOG_GETTING_PENDING_APPROVALS = "Getting pending approvals";
    public static final String LOG_PROCESSING_APPROVAL_TIMEOUTS = "Processing timed-out approvals";
    
    // ==================== SCHEDULER LOG MESSAGES ====================
    public static final String LOG_WORKFLOW_UNSCHEDULED = "Successfully unscheduled workflow";
    public static final String LOG_RESCHEDULING_WORKFLOW = "Rescheduling workflow";
    
    // ==================== ADDITIONAL JWT/AUTH LOG MESSAGES ====================
    public static final String LOG_JWT_TENANT_ID_NULL = "Tenant ID is null or empty in JWT claims";
    public static final String LOG_JWT_TOKEN_NULL = "JWT token is null in request";
    public static final String LOG_JWT_EXTRACT_USERID_FAILED = "Failed to extract userId from JWT token";
    public static final String LOG_JWT_EXTRACT_USERNAME_CALLED = "JwtUtils.extractUsername called";
    public static final String LOG_JWT_USERNAME_HEADER_VALUE = "X-Username header value";
    public static final String LOG_JWT_USERNAME_FROM_HEADER = "Returning username from X-Username header";
    public static final String LOG_JWT_TOKEN_EXTRACTED = "JWT token extracted";
    public static final String LOG_JWT_USERNAME_FROM_TOKEN = "Username from JWT token";
    public static final String LOG_JWT_USERNAME_EXTRACTION_FAILED = "Failed to extract username from JWT token";
    public static final String LOG_JWT_USERNAME_NULL = "Returning NULL username - no source available";
    
    // ==================== WORKFLOW EXECUTION SERVICE LOG MESSAGES ====================
    public static final String LOG_TRANSACTION_COMMITTED_DISPATCHING = "Transaction committed, now dispatching execution";
    public static final String LOG_GETTING_NODE_EXECUTIONS = "Getting node executions for execution";
    public static final String LOG_FOUND_NODE_EXECUTIONS = "Found node executions for execution";
    
    // ==================== WORKFLOW APPROVAL SERVICE LOG MESSAGES ====================
    public static final String LOG_CUSTOM_EMAIL_SENT = "Custom email sent to approver";
    public static final String LOG_CANNOT_RESUME_EXECUTION_NOT_FOUND = "Cannot resume workflow - execution not found";
    public static final String LOG_CANNOT_RESUME_EXECUTION_NOT_PAUSED = "Cannot resume workflow - execution is not PAUSED. Current status";
    public static final String LOG_FLUSHED_NODE_EXECUTION_UPDATES = "Flushed node execution updates to database";
    public static final String LOG_SINGLE_APPROVAL_APPROVED = "Single approval approved, resuming workflow";
    public static final String LOG_TRANSACTION_COMMITTED_RESUMING = "Transaction committed, resuming workflow";
    public static final String LOG_APPROVAL_REJECTED_RESUMING = "Approval rejected - resuming workflow with rejection data: executionId={}";
    public static final String LOG_TRANSACTION_COMMITTED_RESUMING_REJECTION = "Transaction committed, now resuming workflow after rejection";
    public static final String LOG_WORKFLOW_RESUME_AFTER_REJECTION = "Workflow will resume after rejection once transaction commits: executionId={}";
    public static final String LOG_FAILED_SEND_REJECTION_NOTIFICATION = "Failed to send rejection notification";
    public static final String LOG_NO_PAUSED_NODE_EXECUTION = "No PAUSED node execution found for approval";
    public static final String LOG_UPDATED_NODE_WITH_APPROVAL_DATA = "Updated node execution with approval data";
    public static final String LOG_FAILED_UPDATE_NODE_APPROVAL_DATA = "Failed to update node execution with approval data";
    public static final String LOG_UPDATED_NODE_WITH_REJECTION_DATA = "Updated node execution with rejection data";
    public static final String LOG_FAILED_UPDATE_NODE_REJECTION_DATA = "Failed to update node execution with rejection data";
    public static final String LOG_FAILED_FETCH_DELEGATEE_DETAILS = "Failed to fetch delegatee details from user-service, using defaults";
    public static final String LOG_FAILED_SEND_DELEGATION_NOTIFICATION = "Failed to send delegation notification";
    
    // ==================== NODE EXECUTOR LOG MESSAGES ====================
    public static final String LOG_EXECUTING_TEMPLATED_ACTION = "Executing templated action";
    public static final String LOG_EXECUTING_IF_ELSE_CONDITION_NODE = "Executing if-else condition node";
    public static final String LOG_NO_CONDITION_DATA_DEFAULT_FALSE = "No condition data configured, defaulting to false";
    public static final String LOG_MISSING_FIELD_OPERATOR_DEFAULT_FALSE = "Missing field or operator, defaulting to false";
    public static final String LOG_CONDITION_EVALUATION = "Condition evaluation: field='{}', operator='{}', expected='{}', actual='{}', result={}";
    public static final String LOG_ERROR_EVALUATING_CONDITION = "Error evaluating condition";
    public static final String LOG_UNKNOWN_OPERATOR_DEFAULT_EQUALS = "Unknown operator: {}, defaulting to equals";
    public static final String LOG_EXECUTING_HTTP_ACTION_NODE = "Executing HTTP action node";
    public static final String LOG_TEMPLATE_VARIABLE_NOT_FOUND = "Template variable {{{}}} not found in context";
    public static final String LOG_HTTP_REQUEST_FAILED_RETRYING = "HTTP request failed (attempt {}/{}), retrying in {}ms";
    public static final String LOG_EXECUTING_TEMPLATE_ACTION = "Executing template-based action";
    public static final String LOG_APPLYING_ACTION_TEMPLATE_RUNTIME = "Applying action template at runtime";
    public static final String LOG_ADDED_MISSING_FIELD_FROM_TEMPLATE = "Added missing field from template";
    public static final String LOG_LOADING_CONFIG_TEMPLATE = "Loading template from config.template";
    public static final String LOG_FAILED_PROCESS_HEADERS_TEMPLATE = "Failed to process headers template";
    public static final String LOG_FAILED_PROCESS_BODY_TEMPLATE = "Failed to process body template";
    public static final String LOG_HTTP_CONFIG_KEYS_BEFORE_MARKER = "httpConfig keys before marker";
    public static final String LOG_HTTP_CONFIG_CONTAINS_TEMPLATE = "httpConfig contains 'template'=";
    public static final String LOG_HTTP_CONFIG_AFTER_MARKER = "httpConfig after marker - contains '__skipTemplateCheck'={}, contains 'template'=";
    public static final String LOG_DELEGATING_TO_EXECUTE_HTTP = "Delegating to executeActionHttp with cleaned node and __skipTemplateCheck marker";
    public static final String LOG_FAILED_UPDATE_BRANCH_CONTEXT = "Failed to update branch context";
    
    // ==================== CONFIG/INITIALIZATION LOG MESSAGES ====================
    public static final String LOG_INITIALIZING_SYSTEM_TEMPLATES = "Initializing system templates...";
    public static final String LOG_SYSTEM_TEMPLATES_EXIST = "System templates already exist ({}). Skipping initialization.";
    public static final String LOG_SYSTEM_TEMPLATES_INITIALIZED = "System templates initialized successfully";
    public static final String LOG_CREATED_SYSTEM_TEMPLATE_ONBOARDING = "Created system template: Employee Onboarding";
    public static final String LOG_CREATED_SYSTEM_TEMPLATE_ACCESS_REQUEST = "Created system template: Access Request";
    public static final String LOG_CREATED_SYSTEM_TEMPLATE_TERMINATION = "Created system template: Employee Termination";
    public static final String LOG_CREATED_SYSTEM_TEMPLATE_ROLE_CHANGE = "Created system template: Role Change";
    public static final String LOG_KAFKA_PRODUCER_CONFIG_INITIALIZED = "Kafka Producer Configuration initialized";
    public static final String LOG_KAFKA_BOOTSTRAP_SERVERS = "Kafka Bootstrap Servers";
    public static final String LOG_KAFKA_NOTIFICATION_PRODUCER = "Notification Producer: workflows-service-producer";
    public static final String LOG_KAFKA_AUDIT_PRODUCER = "Audit Log Producer: workflows-service-audit-producer";
    public static final String LOG_GRPC_INTERCEPTING_CALL = "Intercepting gRPC call";
    public static final String LOG_GRPC_ADDED_USER_CONTEXT = "Added user context to gRPC headers: tenantId={}, userId={}, username={}";
    public static final String LOG_GRPC_NO_USER_CONTEXT = "No user context available for gRPC call";
    public static final String LOG_INITIALIZING_THREAD_POOL = "Initializing workflow executor thread pool: core={}, max={}, queue={}";
    public static final String LOG_THREAD_POOL_INITIALIZED = "Workflow executor thread pool initialized successfully";
    public static final String LOG_ACTION_TEMPLATE_CONFIG_CONSTRUCTOR = "=== ActionTemplateConfig constructor called ===";
    public static final String LOG_CREATING_ACTION_TEMPLATE_SERVICE_BEAN = "=== Creating ActionTemplateService bean explicitly via @Bean configuration ===";
    public static final String LOG_ACTION_TEMPLATE_SERVICE_CREATED = "=== ActionTemplateService bean created successfully ===";
    
    // ==================== ASYNC EXECUTION LOG MESSAGES ====================
    public static final String LOG_DISPATCHING_TO_THREAD_POOL = "Dispatching workflow execution to thread pool: executionId={}";
    public static final String LOG_STARTING_WORKFLOW_IN_BACKGROUND = "Starting workflow execution in background: executionId={}, workflowId={}, workflowName={}";
    public static final String LOG_WORKFLOW_COMPLETED_SUCCESSFULLY = "Workflow execution completed successfully: executionId={}";
    public static final String LOG_WORKFLOW_FAILED_IN_BACKGROUND = "Workflow execution failed in background thread: executionId={}, error={}";
    public static final String LOG_FAILED_SAVE_ERROR_STATE = "Failed to save error state for execution: executionId={}";
    public static final String LOG_RESUMING_PAUSED_WORKFLOW = "Resuming paused workflow execution: executionId={}";
    public static final String LOG_RESUMING_FROM_NODE_ASYNC = "Resuming workflow from node: executionId={}, nodeId={}";
    public static final String LOG_WORKFLOW_RESUMED_COMPLETED = "Workflow execution resumed and completed: executionId={}";
    public static final String LOG_WORKFLOW_RESUME_FAILED_BACKGROUND = "Workflow resume failed in background thread: executionId={}, error={}";
    public static final String LOG_FAILED_UPDATE_STATUS_AFTER_RESUME_ERROR = "Failed to update execution status after resume error";
    public static final String LOG_CANCELLING_WORKFLOW_EXECUTION = "Cancelling workflow execution: executionId={}";
    public static final String LOG_EXECUTION_CANCELLED_SUCCESSFULLY = "Execution cancelled successfully: executionId={}";
    public static final String LOG_THREAD_POOL_STATUS = "Thread pool status: active={}, pool={}, max={}, queue={}";
    
    // ==================== EXCEPTION HANDLER LOG MESSAGES ====================
    public static final String LOG_UNEXPECTED_ERROR_OCCURRED = "Unexpected error occurred";
    
    // ==================== TEMPLATE METADATA KEYS ====================
    public static final String TEMPLATE_META_TEMPLATE_ID = "templateId";
    public static final String TEMPLATE_META_TEMPLATE_NAME = "templateName";
    public static final String TEMPLATE_META_TEMPLATE_VERSION = "templateVersion";
    public static final String TEMPLATE_META_TEMPLATE_CATEGORY = "templateCategory";
    public static final String TEMPLATE_META_INSTANTIATED_AT = "instantiatedAt";
    public static final String TEMPLATE_META_VARIABLES_USED = "variablesUsed";
    
    // ==================== TEMPLATE DEFAULTS ====================
    public static final String TEMPLATE_DESCRIPTION_PREFIX = "Created from template: ";
    public static final String TEMPLATE_VARIABLE_PLACEHOLDER_PREFIX = "{{";
    public static final String TEMPLATE_VARIABLE_PLACEHOLDER_SUFFIX = "}}";
    public static final String TEMPLATE_NAME_SEPARATOR = "_";
    
    // ==================== PHASE 6B: PARALLEL EXECUTION ====================
    
    // Parallel Gateway Node Types
    public static final String NODE_TYPE_GATEWAY_PARALLEL_SPLIT = "gateway.parallel-split";
    public static final String NODE_TYPE_GATEWAY_PARALLEL_JOIN = "gateway.parallel-join";
    
    // Parallel Execution Error Messages
    public static final String ERROR_PARALLEL_EXEC_NOT_FOUND = "Parallel execution not found for join node: ";
    public static final String ERROR_PARALLEL_NO_BRANCHES = "Parallel split must have at least 2 branches";
    public static final String ERROR_PARALLEL_JOIN_NODE_NOT_FOUND = "Parallel join node not found for split: ";
    public static final String ERROR_PARALLEL_SPLIT_ALREADY_EXISTS = "Parallel execution already exists for split node: ";
    public static final String ERROR_PARALLEL_BRANCH_FAILED = "Parallel branch execution failed";
    public static final String ERROR_PARALLEL_BRANCH_THRESHOLD_EXCEEDED = "Too many parallel branches failed";
    public static final String ERROR_UNKNOWN_FAILURE_POLICY = "Unknown failure policy: ";
    
    // Parallel Execution Log Messages
    public static final String LOG_EXECUTING_PARALLEL_SPLIT = "Executing parallel-split node: {}, creating {} branches";
    public static final String LOG_PARALLEL_SPLIT_CREATED = "Parallel split created: splitNode={}, joinNode={}, branches={}";
    public static final String LOG_EXECUTING_PARALLEL_JOIN = "Executing parallel-join node: {}";
    public static final String LOG_PARALLEL_BRANCH_COMPLETED = "Parallel branch completed: {}/{} branches done";
    public static final String LOG_PARALLEL_ALL_BRANCHES_COMPLETE = "All parallel branches completed, continuing workflow";
    public static final String LOG_PARALLEL_WAITING_FOR_BRANCHES = "Waiting for remaining branches: {}/{} completed";
    public static final String LOG_PARALLEL_BRANCH_DISPATCHED = "Parallel branch dispatched: branch={}, nodeId={}";
    public static final String LOG_PARALLEL_BRANCH_SUCCEEDED = "Parallel branch succeeded: branch={}, execution={}";
    public static final String LOG_PARALLEL_BRANCH_FAILED_EXEC = "Parallel branch failed: branch={}, execution={}, error={}";
    public static final String LOG_PARALLEL_JOIN_CHECKING_FAILURES = "Checking for failed branches at join: completed={}, failed={}, policy={}";
    public static final String LOG_PARALLEL_JOIN_FAILURE_THRESHOLD_EXCEEDED = "Failure threshold exceeded: {}% failed (threshold: {}%)";
    public static final String LOG_PARALLEL_JOIN_FAIL_SAFE_CONTINUING = "Continuing with {} successful branches (fail-safe policy)";
    
    // Parallel Execution Context Keys
    public static final String CONTEXT_KEY_SPLIT_NODE = "splitNode";
    public static final String CONTEXT_KEY_JOIN_NODE = "joinNode";
    public static final String CONTEXT_KEY_BRANCHES = "branches";
    public static final String CONTEXT_KEY_ALL_BRANCHES_COMPLETE = "allBranchesComplete";
    public static final String CONTEXT_KEY_COMPLETED_BRANCHES = "completedBranches";
    public static final String CONTEXT_KEY_TOTAL_BRANCHES = "totalBranches";
    
    // ==================== PHASE 7: FOREACH LOOP CONTROL ====================
    
    // ForEach Loop Node Types
    public static final String NODE_TYPE_CONTROL_FOREACH = "control.foreach";
    public static final String NODE_TYPE_CONTROL_FOREACH_END = "control.foreach-end";
    
    // ForEach Iteration Types
    public static final String FOREACH_ITERATION_COLLECTION = "collection";
    public static final String FOREACH_ITERATION_COUNT = "count";
    public static final String FOREACH_ITERATION_WHILE = "while";
    
    // ForEach Error Messages
    public static final String ERROR_FOREACH_NO_COLLECTION = "ForEach node requires a collection path or count value";
    public static final String ERROR_FOREACH_INVALID_COLLECTION = "ForEach collection is not iterable: ";
    public static final String ERROR_FOREACH_MAX_ITERATIONS_EXCEEDED = "ForEach loop exceeded maximum iterations: ";
    public static final String ERROR_FOREACH_END_NOT_FOUND = "ForEach end node not found for loop: ";
    public static final String ERROR_FOREACH_NO_LOOP_STATE = "ForEach end node reached without active loop state";
    
    // ForEach Log Messages
    public static final String LOG_FOREACH_STARTING = "Starting ForEach loop: node={}, type={}, totalItems={}";
    public static final String LOG_FOREACH_ITERATION = "ForEach iteration: node={}, index={}, currentItem={}";
    public static final String LOG_FOREACH_COMPLETED = "ForEach loop completed: node={}, totalIterations={}";
    public static final String LOG_FOREACH_END_CONTINUE = "ForEach end: continuing loop, iteration={}/{}";
    public static final String LOG_FOREACH_END_COMPLETE = "ForEach end: loop complete, processed {} items";
    public static final String LOG_FOREACH_WHILE_CONDITION = "ForEach while condition evaluated: result={}";
    
    // ForEach Context Keys
    public static final String CONTEXT_KEY_LOOP_STATE_PREFIX = "_loopState_";
    public static final String CONTEXT_KEY_LOOP_INDEX = "index";
    public static final String CONTEXT_KEY_LOOP_ITEM = "currentItem";
    public static final String CONTEXT_KEY_LOOP_ITEMS = "items";
    public static final String CONTEXT_KEY_LOOP_TOTAL = "totalItems";
    public static final String CONTEXT_KEY_LOOP_IS_FIRST = "isFirst";
    public static final String CONTEXT_KEY_LOOP_IS_LAST = "isLast";
    public static final String CONTEXT_KEY_LOOP_HAS_MORE = "hasMore";
    public static final String CONTEXT_KEY_LOOP_RESULTS = "results";
    public static final String CONTEXT_KEY_LOOP_CONTINUE = "continueLoop";
    public static final String CONTEXT_KEY_LOOP_START_NODE = "loopStartNode";
    public static final String CONTEXT_KEY_LOOP_END_NODE = "loopEndNode";
    
    // ForEach Default Configuration
    public static final int FOREACH_DEFAULT_MAX_ITERATIONS = 1000;
    
    // ==================== PHASE 7: DYNAMIC SWITCH GATEWAY ====================
    
    // Dynamic Switch Node Types
    public static final String NODE_TYPE_GATEWAY_SWITCH = "gateway.switch";
    public static final String NODE_TYPE_GATEWAY_SWITCH_JOIN = "gateway.switch-join";
    
    // Switch Execution Modes
    public static final String SWITCH_MODE_PARALLEL = "parallel";
    public static final String SWITCH_MODE_SEQUENTIAL = "sequential";
    
    // Switch Error Messages
    public static final String ERROR_SWITCH_NO_ITEMS = "Switch node requires items collection path";
    public static final String ERROR_SWITCH_NO_CASES = "Switch node must have at least one case defined";
    public static final String ERROR_SWITCH_JOIN_NOT_FOUND = "Switch join node not found for switch: ";
    public static final String ERROR_SWITCH_EXEC_NOT_FOUND = "Switch execution not found for join node: ";
    
    // Switch Log Messages
    public static final String LOG_SWITCH_STARTING = "Starting dynamic switch: node={}, totalItems={}, cases={}";
    public static final String LOG_SWITCH_ROUTING_ITEM = "Routing item to case: index={}, condition={}, branch={}";
    public static final String LOG_SWITCH_DEFAULT_ROUTE = "Routing item to default branch: index={}";
    public static final String LOG_SWITCH_JOIN_WAITING = "Switch join waiting: completed={}, total={}";
    public static final String LOG_SWITCH_JOIN_COMPLETE = "Switch join complete: merged {} branch results";
    public static final String LOG_SWITCH_BRANCH_DISPATCHED = "Switch branch dispatched: case={}, itemIndex={}";
    
    // Switch Context Keys
    public static final String CONTEXT_KEY_SWITCH_EXEC = "_switchExec_";
    public static final String CONTEXT_KEY_SWITCH_CASE = "switchCase";
    public static final String CONTEXT_KEY_SWITCH_ITEM = "switchItem";
    public static final String CONTEXT_KEY_SWITCH_ITEM_INDEX = "switchItemIndex";
    public static final String CONTEXT_KEY_SWITCH_JOIN_NODE = "switchJoinNode";
    public static final String CONTEXT_KEY_SWITCH_RESULTS = "switchResults";
    public static final String CONTEXT_KEY_SWITCH_MODE = "switchMode";
    
    // ==================== PHASE 7: DYNAMIC PARALLEL SPLIT ====================
    
    // Dynamic Parallel Split Node Types
    public static final String NODE_TYPE_GATEWAY_DYNAMIC_SPLIT = "gateway.dynamic-split";
    public static final String NODE_TYPE_GATEWAY_DYNAMIC_JOIN = "gateway.dynamic-join";
    
    // Dynamic Parallel Split Error Messages
    public static final String ERROR_DYNAMIC_SPLIT_NO_ITEMS = "Dynamic split node requires items collection path";
    public static final String ERROR_DYNAMIC_SPLIT_JOIN_NOT_FOUND = "Dynamic split join node not found: ";
    public static final String ERROR_DYNAMIC_SPLIT_EXEC_NOT_FOUND = "Dynamic split execution not found for join: ";
    
    // Dynamic Parallel Split Log Messages
    public static final String LOG_DYNAMIC_SPLIT_STARTING = "Starting dynamic parallel split: node={}, totalItems={}";
    public static final String LOG_DYNAMIC_SPLIT_BRANCH_DISPATCHED = "Dynamic split branch dispatched: itemIndex={}, item={}";
    public static final String LOG_DYNAMIC_SPLIT_JOIN_WAITING = "Dynamic split join waiting: completed={}, total={}";
    public static final String LOG_DYNAMIC_SPLIT_JOIN_COMPLETE = "Dynamic split join complete: merged {} results";
    
    // Dynamic Parallel Split Context Keys
    public static final String CONTEXT_KEY_DYNAMIC_SPLIT_EXEC = "_dynamicSplitExec_";
    public static final String CONTEXT_KEY_DYNAMIC_ITEM = "dynamicItem";
    public static final String CONTEXT_KEY_DYNAMIC_ITEM_INDEX = "dynamicItemIndex";
    public static final String CONTEXT_KEY_DYNAMIC_TOTAL_ITEMS = "dynamicTotalItems";
    public static final String CONTEXT_KEY_DYNAMIC_RESULTS = "dynamicResults";
    
    // ==================== PHASE 6B: MULTI-APPROVAL ====================
    
    // Multi-Approval Error Messages
    public static final String ERROR_MULTI_APPROVAL_NO_APPROVERS = "Multi-approval node requires at least one approver";
    public static final String ERROR_MULTI_APPROVAL_INVALID_APPROVER = "Invalid approver ID in multi-approval list: ";
    
    // Multi-Approval Log Messages
    public static final String LOG_CREATING_MULTI_APPROVAL = "Creating multi-approval with {} approvers for execution: {}";
    public static final String LOG_MULTI_APPROVAL_CREATED = "Created {} approval requests for multi-approval node";
    public static final String LOG_CHECKING_MULTI_APPROVAL_STATUS = "Checking multi-approval status: {}/{} approved";
    public static final String LOG_MULTI_APPROVAL_COMPLETE = "All approvers have approved, resuming workflow: {}";
    public static final String LOG_MULTI_APPROVAL_INCOMPLETE = "Multi-approval incomplete: {}/{} approved, waiting";
    public static final String LOG_MULTI_APPROVAL_REJECTED = "Multi-approval rejected by approver: {}, failing workflow: {}";
    
    // Multi-Approval Context Keys
    public static final String CONTEXT_KEY_APPROVAL_IDS = "approvalIds";
    public static final String CONTEXT_KEY_REQUIRED_APPROVALS = "requiredApprovals";
    public static final String CONTEXT_KEY_APPROVER_IDS = "approverIds";
    
    // ==================== PHASE 6B: DYNAMIC APPROVER RESOLUTION ====================
    
    // Dynamic Approver Resolution Strategies
    public static final String APPROVER_STRATEGY_MANAGER = "manager";
    public static final String APPROVER_STRATEGY_ROLE = "role";
    public static final String APPROVER_STRATEGY_DEPARTMENT_HEAD = "department_head";
    public static final String APPROVER_STRATEGY_CUSTOM = "custom";
    
    // Multi-Approval Resolution Strategies (Phase 1 Enhancement)
    public static final String RESOLUTION_STRATEGY_CONTEXT = "context";
    public static final String RESOLUTION_STRATEGY_STATIC = "static";
    public static final String RESOLUTION_STRATEGY_ROLE = "role";
    public static final String RESOLUTION_STRATEGY_HIERARCHY = "hierarchy";
    public static final String RESOLUTION_STRATEGY_COMPOSITE = "composite";
    
    // Resolution Strategy Config Keys
    public static final String CONFIG_KEY_STRATEGY = "strategy";
    public static final String CONFIG_KEY_CONTEXT_KEY = "contextKey";
    public static final String CONFIG_KEY_APPROVER_IDS = "approverIds";
    public static final String CONFIG_KEY_ROLE_NAMES = "roleNames";
    public static final String CONFIG_KEY_LIMIT = "limit";
    public static final String CONFIG_KEY_FILTER_BY_DEPARTMENT = "filterByDepartment";
    public static final String CONFIG_KEY_RELATIVE_TO = "relativeTo";
    public static final String CONFIG_KEY_HIERARCHY_TYPE = "hierarchyType";
    public static final String CONFIG_KEY_LEVELS = "levels";
    public static final String CONFIG_KEY_STRATEGIES = "strategies";
    public static final String CONFIG_KEY_CONDITION = "condition";
    public static final String CONFIG_KEY_DEDUPLICATE_APPROVERS = "deduplicateApprovers";
    public static final String CONFIG_KEY_FALLBACK = "fallback";
    public static final String CONFIG_KEY_OPERATION = "operation";
    public static final String CONFIG_KEY_REQUIRED_APPROVALS = "requiredApprovals";
    
    // Hierarchy Types
    public static final String HIERARCHY_TYPE_MANAGER = "manager";
    public static final String HIERARCHY_TYPE_SKIP_LEVEL = "skip_level";
    public static final String HIERARCHY_TYPE_DEPARTMENT_HEAD = "department_head";
    
    // Approval Policy Types
    public static final String APPROVAL_POLICY_ALL = "ALL";
    public static final String APPROVAL_POLICY_ANY = "ANY";
    public static final String APPROVAL_POLICY_THRESHOLD = "THRESHOLD";
    public static final String APPROVAL_POLICY_FAIL_FAST = "FAIL_FAST";
    
    // Dynamic Approver Error Messages
    public static final String ERROR_APPROVER_RESOLUTION_FAILED = "Failed to resolve approver using strategy: ";
    public static final String ERROR_APPROVER_STRATEGY_UNKNOWN = "Unknown approver resolution strategy: ";
    public static final String ERROR_APPROVER_NOT_FOUND = "No approver found for strategy: ";
    public static final String ERROR_APPROVER_USER_ID_REQUIRED = "userId required in context for manager resolution";
    public static final String ERROR_APPROVER_ROLE_REQUIRED = "requiredApproverRole required in context for role resolution";
    public static final String ERROR_APPROVER_DEPARTMENT_REQUIRED = "department required in context for department head resolution";
    
    // Multi-Approval Resolution Error Messages (Phase 1)
    public static final String ERROR_RESOLUTION_STRATEGY_UNKNOWN = "Unknown resolution strategy: ";
    public static final String ERROR_CONTEXT_KEY_REQUIRED = "contextKey required for context strategy";
    public static final String ERROR_CONTEXT_KEY_NOT_FOUND = "Context key not found: ";
    public static final String ERROR_APPROVERS_NOT_FOUND_IN_CONTEXT = "No approvers found in context key: ";
    public static final String ERROR_APPROVER_IDS_REQUIRED = "approverIds required for static strategy";
    public static final String ERROR_STATIC_APPROVERS_EMPTY = "Static approver list cannot be empty";
    public static final String ERROR_ROLE_NAMES_REQUIRED = "roleNames required for role strategy";
    public static final String ERROR_NO_USERS_WITH_ROLES = "No users found with roles: ";
    public static final String ERROR_HIERARCHY_CONFIG_REQUIRED = "Hierarchy configuration required";
    public static final String ERROR_HIERARCHY_TYPE_UNKNOWN = "Unknown hierarchy type: ";
    public static final String ERROR_HIERARCHY_RESOLUTION_FAILED = "Hierarchy resolution failed: ";
    public static final String ERROR_MANAGER_NOT_FOUND = "No manager found at level: ";
    public static final String ERROR_COMPOSITE_NO_APPROVERS = "Composite strategy resolved no approvers";
    public static final String ERROR_COMPOSITE_STRATEGIES_REQUIRED = "Composite strategy requires sub-strategies list";
    public static final String ERROR_CONDITION_EVALUATION_FAILED = "Failed to evaluate condition: ";
    public static final String ERROR_THRESHOLD_REQUIRED_APPROVALS_MISSING = "requiredApprovals is required for THRESHOLD approval policy";
    
    // Dynamic Approver Log Messages
    public static final String LOG_RESOLVING_APPROVER = "Resolving approver using strategy: {}";
    public static final String LOG_APPROVER_RESOLVED = "Approver resolved: strategy={}, approverId={}";
    public static final String LOG_RESOLVING_MANAGER = "Resolving manager for user: {}";
    public static final String LOG_MANAGER_RESOLVED = "Manager resolved: userId={}, managerId={}";
    public static final String LOG_RESOLVING_BY_ROLE = "Resolving approver by role: {}";
    public static final String LOG_ROLE_APPROVER_RESOLVED = "Role approver resolved: role={}, approverId={}";
    public static final String LOG_RESOLVING_DEPARTMENT_HEAD = "Resolving department head for department: {}";
    public static final String LOG_DEPARTMENT_HEAD_RESOLVED = "Department head resolved: department={}, headId={}";
    
    // Multi-Approval Resolution Log Messages (Phase 1)
    public static final String LOG_RESOLVING_APPROVERS = "Resolving approvers using strategy";
    public static final String LOG_APPROVERS_RESOLVED = "Approvers resolved successfully";
    public static final String LOG_APPROVERS_RESOLVED_FROM_CONTEXT = "Resolved approvers from context";
    public static final String LOG_APPROVERS_RESOLVED_STATIC = "Using static approver list";
    public static final String LOG_APPROVERS_RESOLVED_FROM_ROLES = "Resolved approvers by roles";
    public static final String LOG_APPROVERS_RESOLVED_FROM_HIERARCHY = "Resolved approver from hierarchy";
    public static final String LOG_APPROVERS_RESOLVED_COMPOSITE_UNION = "Resolved approvers using composite union";
    public static final String LOG_APPROVERS_RESOLVED_COMPOSITE_INTERSECTION = "Resolved approvers using composite intersection";
    public static final String LOG_APPROVERS_RESOLVED_LIMITED = "Limited approvers to configured maximum";
    public static final String LOG_USING_FLEXIBLE_RESOLUTION = "Using flexible approver resolution system";
    public static final String LOG_USING_BACKWARD_COMPATIBLE_MODE = "Using backward compatible resolution mode";
    public static final String LOG_RESOLUTION_FROM_CONTEXT = "Resolving approvers from context";
    public static final String LOG_RESOLUTION_FROM_STATIC = "Using static approver list";
    public static final String LOG_RESOLUTION_FROM_ROLE = "Resolving approvers by role";
    public static final String LOG_RESOLUTION_FROM_HIERARCHY = "Resolving approvers from hierarchy";
    public static final String LOG_RESOLUTION_FROM_COMPOSITE = "Resolving approvers using composite strategy";
    public static final String LOG_SKIPPING_STRATEGY_CONDITION_FALSE = "Skipping strategy due to condition";
    public static final String LOG_APPLYING_FALLBACK_STRATEGY = "Primary strategy failed, applying fallback";
    public static final String LOG_APPROVAL_POLICY_CONFIGURED = "Approval policy configured";
    public static final String LOG_FLEXIBLE_RESOLUTION_SUCCESS = "Flexible resolution completed successfully";
    public static final String LOG_USING_LEGACY_RESOLUTION = "Using legacy approver resolution (backward compatibility)";
    
    // Dynamic Approver Context Keys
    public static final String CONTEXT_KEY_RESOLUTION_STRATEGY = "resolutionStrategy";
    public static final String CONTEXT_KEY_RESOLVED_APPROVER_ID = "resolvedApproverId";
    public static final String CONTEXT_KEY_REQUIRED_APPROVER_ROLE = "requiredApproverRole";
    public static final String CONTEXT_KEY_DEPARTMENT = "department";
    
    // Dynamic Approver Field Names
    public static final String FIELD_RESOLUTION_STRATEGY = "resolutionStrategy";
    public static final String FIELD_APPROVERS = "approvers";
    
    // Composite Operation Types
    public static final String COMPOSITE_OPERATION_UNION = "union";
    public static final String COMPOSITE_OPERATION_INTERSECTION = "intersection";
    
    // Additional Error Messages
    public static final String ERROR_NO_APPROVERS_RESOLVED = "No approvers resolved from flexible resolution strategy";
    public static final String ERROR_INVALID_APPROVAL_POLICY = "Invalid approval policy specified";
    
    // ==================== SIMULATION MODE CONSTANTS ====================
    public static final String LOG_SIMULATION_SKIPPING_HTTP = "Simulation mode: skipping HTTP request";
    public static final String LOG_SIMULATION_SKIPPING_NOTIFICATION = "Simulation mode: skipping notification send";
    public static final String LOG_SIMULATION_SKIPPING_SCRIPT = "Simulation mode: skipping script execution";
    public static final String SIMULATION_SUCCESS_MESSAGE = "Simulated success response";
    public static final String SIMULATION_STATUS_WOULD_PROVISION = "would-provision";
    public static final String SIMULATION_STATUS_WOULD_DEPROVISION = "would-deprovision";
    public static final String SIMULATION_STATUS_WOULD_NOTIFY = "would-send-notification";
    public static final String SIMULATION_STATUS_WOULD_HTTP = "would-make-http-request";
    public static final String SIMULATION_STATUS_WOULD_SCRIPT = "would-execute-script";
    
    // ==================== ADDITIONAL LOG MESSAGES - NODE EXECUTOR (NON-DUPLICATE) ====================
    public static final String LOG_EXECUTING_WORKFLOW_NODE = "Executing workflow node";
    public static final String LOG_NODE_HAS_NO_TYPE = "Node has no type defined";
    public static final String LOG_STARTING_WORKFLOW_TRIGGER_START = "Starting workflow - trigger start node";
    public static final String LOG_ENDING_WORKFLOW_TRIGGER_END = "Ending workflow - trigger end node";
    public static final String LOG_SIMULATION_MODE_AUTO_APPROVE_SINGLE = "Simulation mode: auto-approving single approval";
    public static final String LOG_USING_FIRST_OWNER_ID_AS_APPROVER = "Using first ownerId from context as approverId";
    public static final String LOG_RESOLVED_ROLE_OWNERS_MULTI_APPROVAL = "Resolved role owners from context for multi-approval";
    public static final String LOG_USING_OWNER_IDS_MULTI_APPROVAL = "Using ownerIds from context for multi-approval";
    public static final String LOG_CREATING_MULTI_APPROVAL_REQUESTS = "Creating multi-approval requests";
    public static final String LOG_GENERATED_APPROVAL_GROUP_ID = "Generated approval group ID for multi-approval";
    public static final String LOG_INVALID_APPROVER_ID_MULTI_APPROVAL = "Invalid approver ID in multi-approval";
    public static final String LOG_MULTI_APPROVAL_REQUESTS_CREATED = "Multi-approval requests created";
    public static final String LOG_FAILED_RESOLVE_DYNAMIC_APPROVER = "Failed to resolve dynamic approver";
    public static final String LOG_MISSING_FIELD_OR_OPERATOR_DEFAULT_FALSE = "Missing field or operator, defaulting to false";
    public static final String LOG_UPDATED_BRANCH_CONTEXT_COMPLETE = "Updated branch context as complete";
    public static final String LOG_FAILURE_RATE_BELOW_THRESHOLD = "Failure rate below threshold, continuing with successful branches";
    public static final String LOG_MERGED_SUCCESSFUL_BRANCH_CONTEXTS = "Merged successful branch contexts into unified context";
    public static final String LOG_EXECUTE_ACTION_HTTP_PROCESSING_DIRECTLY = "executeActionHttp called for node type action.http - processing HTTP request directly";
    public static final String LOG_AUTO_ADDED_JWT_TO_AUTHORIZATION = "Automatically added JWT token from execution context to Authorization header";
    public static final String LOG_EXECUTING_HTTP_REQUEST = "Executing HTTP request";
    public static final String LOG_HTTP_ACTION_FAILED = "HTTP action failed";
    public static final String LOG_HTTP_ACTION_SUCCEEDED = "HTTP action succeeded";
    public static final String LOG_HTTP_ACTION_EXECUTION_FAILED = "HTTP action execution failed";
    
    // ==================== ADDITIONAL LOG MESSAGES - WORKFLOW EXECUTOR (NON-DUPLICATE) ====================
    public static final String LOG_FOUND_START_NODE = "Found start node";
    public static final String LOG_FAILED_RESUME_WORKFLOW_EXECUTION = "Failed to resume workflow execution";
    public static final String LOG_MARKED_NODE_SKIPPED_COMPLETION = "Marked node as SKIPPED due to workflow completion";
    public static final String LOG_CHECKING_NODE_OUTPUT_DATA = "Checking node execution output data for context merge";
    public static final String LOG_MERGING_NODE_OUTPUT_DATA = "Merging node execution output data into context";
    public static final String LOG_NODE_NO_OUTPUT_DATA = "Node execution has no output data to merge - approval decision may be missing";
    public static final String LOG_MARKED_PAUSED_NODE_COMPLETED = "Marked paused node as COMPLETED after resume";
    public static final String LOG_UPDATED_CONTEXT_AFTER_RESUME = "Updated execution context after resume";
    public static final String LOG_SKIPPING_NODE_DUE_TO_CONDITION = "Skipping node due to condition";
    public static final String LOG_WORKFLOW_RESUMED_AND_COMPLETED = "Workflow execution resumed and completed";
    public static final String LOG_WORKFLOW_NOT_MARKED_COMPLETED = "Workflow execution not marked as completed";
    public static final String LOG_PAUSING_WORKFLOW_AT_NODE = "Pausing workflow execution at node";
    public static final String LOG_SAVED_CONTEXT_BEFORE_PAUSING = "Saved context before pausing execution";
    public static final String LOG_PARALLEL_JOIN_COMPLETED_MERGED_CONTEXT = "Parallel join completed - continuing with merged context";
    public static final String LOG_CONTEXT_REPLACED_PARALLEL_BRANCHES = "Context replaced with merged output from parallel branches";
    public static final String LOG_UPDATED_CONTEXT_AFTER_NODE_COMPLETION = "Updated execution context after node completion";
    public static final String LOG_REACHED_WORKFLOW_END_NODE = "Reached workflow end node";
    public static final String LOG_NO_NEXT_NODES_AFTER_CURRENT = "No next nodes found after current node";
    public static final String LOG_DISPATCHING_PARALLEL_BRANCHES = "Dispatching parallel branches";
    public static final String LOG_PARALLEL_BRANCH_COMPLETED_SUCCESSFULLY = "Parallel branch completed successfully";
    public static final String LOG_CALCULATED_STEP_NUMBERS = "Calculated step numbers for workflow nodes";
    public static final String LOG_CONDITION_NODE_EDGE_NO_SOURCE_HANDLE = "Condition node has edge without sourceHandle";
    public static final String LOG_CONDITION_RESULT_NOT_IN_CONTEXT = "Condition result not found in context";
    public static final String LOG_CONDITIONAL_BRANCH_EVALUATION = "Conditional branch evaluation";
    public static final String LOG_SWITCH_NODE_MISSING_SOURCE_HANDLE = "Switch node missing sourceHandle or selectedBranch";
    public static final String LOG_SWITCH_BRANCH_EVALUATION = "Switch branch evaluation";
    public static final String LOG_EDGE_EXPLICIT_CONDITION_NOT_IMPLEMENTED = "Edge has explicit condition (not yet implemented)";
    public static final String LOG_PARALLEL_BRANCH_MARKED_COMPLETED = "Parallel branch marked as completed";
    public static final String LOG_FAILED_MARK_BRANCH_COMPLETED = "Failed to mark branch as completed";
    public static final String LOG_WORKFLOW_EXECUTION_FAILED_STATUS = "Workflow execution FAILED";
    public static final String LOG_FAILED_UPDATE_STATUS_TO_FAILED = "Failed to update execution status to FAILED";
    
    // ==================== ADDITIONAL LOG MESSAGES - WORKFLOW SERVICE (NON-DUPLICATE) ====================
    public static final String LOG_FAILED_ENRICH_WORKFLOW_STATS = "Failed to enrich workflow with execution stats";
    public static final String LOG_PARTIALLY_UPDATING_WORKFLOW = "Partially updating workflow";
    public static final String LOG_WORKFLOW_PARTIALLY_UPDATED = "Workflow partially updated";
    public static final String LOG_WORKFLOW_TRIGGER_TYPE_CHANGED_UNSCHEDULING = "Workflow trigger type changed from TIME_BASED, unscheduling";
    public static final String LOG_RESCHEDULING_TIME_BASED_WORKFLOW = "Rescheduling time-based workflow";
    public static final String LOG_SCHEDULING_TIME_BASED_WORKFLOW = "Scheduling time-based workflow";
    public static final String LOG_UNSCHEDULING_TIME_BASED_WORKFLOW = "Unscheduling time-based workflow";
    
    // ==================== ADDITIONAL LOG MESSAGES - OTHER SERVICES (NON-DUPLICATE) ====================
    public static final String LOG_DUPLICATE_EXECUTION_PREVENTED = "Duplicate execution prevented by idempotency key";
    public static final String LOG_FAILED_FETCH_APPROVER_DETAILS = "Failed to fetch approver details, using defaults";
    public static final String LOG_FAILED_SEND_APPROVAL_REQUEST_NOTIFICATION = "Failed to send approval request notification";
    public static final String LOG_TRANSACTION_COMMITTED_RESUMING_WORKFLOW = "Transaction committed, resuming workflow";
    public static final String LOG_FAILED_SEND_APPROVAL_DECISION_NOTIFICATION = "Failed to send approval decision notification";
    public static final String LOG_ACTION_TEMPLATE_SERVICE_CONSTRUCTOR = "ActionTemplateService constructor called";
    public static final String LOG_STARTING_ACTION_TEMPLATE_INIT = "Starting ActionTemplateService initialization";
    public static final String LOG_LOOKING_FOR_ACTION_TEMPLATES_JSON = "Looking for action-templates.json in classpath";
    public static final String LOG_ACTION_TEMPLATES_JSON_NOT_FOUND = "action-templates.json NOT FOUND in classpath";
    public static final String LOG_FOUND_ACTION_TEMPLATES_JSON = "Found action-templates.json, loading";
    public static final String LOG_TEMPLATE_CONFIG_NULL_OR_NO_TEMPLATES = "Template config is null or has no templates";
    public static final String LOG_PARSING_TEMPLATES_FROM_CONFIG = "Parsing templates from config";
    public static final String LOG_LOADED_ACTION_TEMPLATE = "Loaded action template";
    public static final String LOG_SUCCESSFULLY_LOADED_ACTION_TEMPLATES = "Successfully loaded action templates";
    public static final String LOG_FAILED_LOAD_ACTION_TEMPLATES = "Failed to load action templates";
    public static final String LOG_UNEXPECTED_ERROR_LOADING_TEMPLATES = "Unexpected error loading templates";
    public static final String LOG_STARTING_CLEANUP_PROCESSED_EVENTS = "Starting cleanup of processed events";
    public static final String LOG_CLEANED_UP_PROCESSED_EVENTS = "Cleaned up processed events";
    public static final String LOG_NO_PROCESSED_EVENTS_TO_CLEANUP = "No processed events to clean up";
    public static final String LOG_FAILED_CLEANUP_PROCESSED_EVENTS = "Failed to clean up old processed events";
    public static final String LOG_CUSTOM_APPROVER_NOT_IMPLEMENTED = "Custom approver resolution not yet implemented - returning default";
    public static final String LOG_CANNOT_SCHEDULE_NON_TIME_BASED = "Cannot schedule non-time-based workflow";
    public static final String LOG_CANNOT_SCHEDULE_NO_CRON = "Cannot schedule workflow without cron expression";
    public static final String LOG_REMOVING_EXISTING_SCHEDULED_JOB = "Removing existing scheduled job";
    public static final String LOG_SUCCESSFULLY_SCHEDULED_WORKFLOW = "Successfully scheduled workflow";
    public static final String LOG_FAILED_SCHEDULE_WORKFLOW = "Failed to schedule workflow";
    public static final String LOG_FAILED_UNSCHEDULE_WORKFLOW = "Failed to unschedule workflow";
    public static final String LOG_NO_SCHEDULED_JOB_FOUND = "No scheduled job found for workflow";
    public static final String LOG_FAILED_CHECK_IF_SCHEDULED = "Failed to check if workflow is scheduled";
    public static final String LOG_INVALID_CRON_EXPRESSION = "Invalid cron expression";
    public static final String LOG_EXECUTING_SCHEDULED_WORKFLOW = "Executing scheduled workflow";
    public static final String LOG_WORKFLOW_NOT_ACTIVE_SKIPPING = "Workflow is not active, skipping execution";
    public static final String LOG_WORKFLOW_NOT_TIME_BASED_SKIPPING = "Workflow is not time-based, skipping execution";
    public static final String LOG_SUCCESSFULLY_EXECUTED_SCHEDULED_WORKFLOW = "Successfully executed scheduled workflow";
    public static final String LOG_FAILED_EXECUTE_SCHEDULED_WORKFLOW = "Failed to execute scheduled workflow";
    
    // ==================== ADDITIONAL LOG MESSAGES - WORKFLOW EVENT CONSUMER ====================
    public static final String LOG_RECEIVED_WORKFLOW_EVENT = "Received workflow event";
    public static final String LOG_EVENT_ALREADY_PROCESSED_SKIPPING = "Event already processed, skipping";
    public static final String LOG_EVENT_CONTAINS_WORKFLOW_ID = "Event contains workflowId";
    public static final String LOG_EVENT_PAYLOAD_IS_NULL = "Event payload is null";
    public static final String LOG_NO_WORKFLOWS_SUBSCRIBED_TO_EVENT = "No workflows subscribed to event";
    public static final String LOG_FOUND_WORKFLOWS_TO_EXECUTE_FOR_EVENT = "Found workflows to execute for event";
    public static final String LOG_FAILED_TO_PROCESS_EVENT = "Failed to process event";
    public static final String LOG_MARKED_EVENT_AS_PROCESSED = "Marked event as processed";
    public static final String LOG_FAILED_TO_MARK_EVENT_AS_PROCESSED = "Failed to mark event as processed";
    public static final String LOG_EVENT_SPECIFIES_WORKFLOW_ID_EXECUTING_ONLY_THAT = "Event specifies workflowId, executing only that workflow";
    public static final String LOG_SPECIFIED_WORKFLOW_ID_NOT_FOUND = "Specified workflowId not found or not subscribed to event";
    public static final String LOG_EVENT_HAS_NO_PAYLOAD_CANNOT_EXTRACT_USER = "Event has no payload, cannot extract user";
    public static final String LOG_EXTRACTED_USER_FROM_EVENT_PAYLOAD = "Extracted user from event payload";
    public static final String LOG_NO_VALID_USER_FOUND_IN_EVENT_PAYLOAD = "No valid user found in event payload";
    
    // ==================== ADDITIONAL LOG MESSAGES - TEMPLATE PROCESSING ====================
    public static final String LOG_FAILED_TO_PROCESS_BODY_TEMPLATE = "Failed to process body template";
    public static final String LOG_HTTPCONFIG_KEYS_BEFORE_MARKER = "httpConfig keys before marker";
    public static final String LOG_HTTPCONFIG_CONTAINS_TEMPLATE = "httpConfig contains template";
    public static final String LOG_HTTPCONFIG_AFTER_MARKER = "httpConfig after marker";
    public static final String LOG_DELEGATING_TO_EXECUTE_ACTION_HTTP = "Delegating to executeActionHttp with cleaned node and __skipTemplateCheck marker";
    public static final String LOG_FAILED_TO_PROCESS_HEADERS_TEMPLATE = "Failed to process headers template";
    
    // ==================== ADDITIONAL LOG MESSAGES - SCHEDULER ====================
    public static final String LOG_FAILED_UNSCHEDULE_WORKFLOW_INNER = "Failed to unschedule workflow";
    public static final String LOG_NO_SCHEDULED_JOB_FOUND_FOR_WORKFLOW = "No scheduled job found for workflow";
    public static final String LOG_FAILED_CHECK_IF_WORKFLOW_SCHEDULED = "Failed to check if workflow is scheduled";
    
    // ==================== ADDITIONAL LOG MESSAGES - EVENT CONSUMER ====================
    public static final String LOG_EVENT_PAYLOAD_KEYS = "Event payload keys";
    
    // ==================== ADDITIONAL LOG MESSAGES - CONTROLLERS ====================
    public static final String LOG_FETCHING_ALL_ACTION_TEMPLATES = "Fetching all action templates";
    public static final String LOG_FETCHING_ACTION_TEMPLATE = "Fetching action template";
    public static final String LOG_FETCHING_ACTION_TEMPLATES_GROUPED = "Fetching action templates grouped by category";
    public static final String LOG_EXECUTING_WORKFLOW_CONTROLLER = "Executing workflow";
    public static final String LOG_FAILED_EXTRACT_JWT_CLAIMS = "Failed to extract JWT claims";
    public static final String LOG_NO_PAUSED_NODE_EXECUTION_FOUND = "No PAUSED node execution found for approval";
    public static final String LOG_SYSTEM_TEMPLATES_EXIST_SKIPPING = "System templates already exist - skipping initialization";
    public static final String LOG_KAFKA_PRODUCER_INITIALIZED = "Kafka Producer Configuration initialized";
    public static final String LOG_KAFKA_BOOTSTRAP_CONFIGURED = "Kafka Bootstrap Servers configured";
    public static final String LOG_NOTIFICATION_PRODUCER_CONFIGURED = "Notification Producer configured";
    public static final String LOG_AUDIT_LOG_PRODUCER_CONFIGURED = "Audit Log Producer configured";
    public static final String LOG_INITIALIZING_WORKFLOW_THREAD_POOL = "Initializing workflow executor thread pool";
    public static final String LOG_WORKFLOW_THREAD_POOL_INITIALIZED = "Workflow executor thread pool initialized successfully";
    public static final String LOG_INTERCEPTING_GRPC_CALL = "Intercepting gRPC call";
    public static final String LOG_ADDED_USER_CONTEXT_GRPC = "Added user context to gRPC headers";
    public static final String LOG_NO_USER_CONTEXT_GRPC = "No user context available for gRPC call";
    public static final String LOG_CREATING_ACTION_TEMPLATE_BEAN = "Creating ActionTemplateService bean explicitly via @Bean configuration";
    public static final String LOG_ACTION_TEMPLATE_BEAN_CREATED = "ActionTemplateService bean created successfully";
    
    // ==================== ADDITIONAL LOG MESSAGES - CLIENTS ====================
    public static final String LOG_CREATE_EMPLOYEE_GRPC_NOT_IMPLEMENTED = "createEmployee via gRPC is NOT IMPLEMENTED in identity-service - Use IdentityRestClient instead";
    public static final String LOG_ATTEMPTING_CREATE_EMPLOYEE_GRPC = "Attempting to create employee via gRPC";
    public static final String LOG_GETTING_IDENTITY_BY_ID_GRPC = "Getting identity by ID via gRPC";
    public static final String LOG_DELETING_IDENTITY_GRPC = "Deleting identity via gRPC";
    public static final String LOG_CHECKING_IDENTITY_EXISTS_GRPC = "Checking if identity exists via gRPC";
    public static final String LOG_IDENTITY_SERVICE_UNAVAILABLE_DELETE = "Identity service unavailable for delete operation";
    public static final String LOG_IDENTITY_SERVICE_UNAVAILABLE_GET = "Identity service unavailable for get operation";
    public static final String LOG_IDENTITY_SERVICE_UNAVAILABLE_EXISTS = "Identity service unavailable for existence check - assuming NOT exists";
    public static final String LOG_USER_GRPC_DISABLED = "UserGrpcClient.getUsersByTenant() is DISABLED - auth protos not in common-protos";
    public static final String LOG_RETURNING_EMPTY_LIST_TENANT = "Returning empty list for tenant";
    public static final String LOG_USER_GET_BY_ID_DISABLED = "UserGrpcClient.getUserById() is DISABLED - auth protos not in common-protos";
    public static final String LOG_USER_REACHABLE_DISABLED = "UserGrpcClient.isAuthServiceReachable() is DISABLED - auth protos not in common-protos";
    public static final String LOG_PERMISSION_CHECK_RESULT = "Permission check result";
    public static final String LOG_GRPC_ERROR_CHECKING_PERMISSION = "gRPC error checking permission";
    public static final String LOG_UNEXPECTED_ERROR_CHECKING_PERMISSION = "Unexpected error checking permission";
    public static final String LOG_PERMISSIONS_HEALTH_CHECK_FAILED = "Permissions service health check failed";
    public static final String LOG_MARKED_NOTIFICATION_VIEWED = "Successfully marked notification as viewed";
    public static final String LOG_FAILED_MARK_NOTIFICATION_VIEWED = "Failed to mark notification as viewed";
    public static final String LOG_MARKED_ALL_APPROVAL_NOTIFICATIONS_VIEWED = "Successfully marked all approval notifications as viewed";
    public static final String LOG_FAILED_MARK_ALL_APPROVAL_NOTIFICATIONS_VIEWED = "Failed to mark all approval notifications as viewed";
    
    // ==================== ADDITIONAL LOG MESSAGES - AUDIT ====================
    public static final String LOG_WORKFLOW_AUDIT_ASPECT_INITIALIZED = "WorkflowAuditAspect initialized successfully";
    public static final String LOG_NO_HTTP_CONTEXT_FOR_TENANT = "No HTTP request context available for tenant extraction";
    public static final String LOG_FAILED_EXTRACT_TENANT_FROM_REQUEST = "Failed to extract tenant ID from request";
    public static final String LOG_COULD_NOT_EXTRACT_USER_ID = "Could not extract user ID from request";
    public static final String LOG_ATTEMPTING_EXTRACT_USERNAME = "Attempting to extract username";
    public static final String LOG_REQUEST_HEADERS_CHECK = "Request headers check";
    public static final String LOG_USERNAME_EXTRACTION_RESULT = "Username extraction result";
    public static final String LOG_HTTP_CONTEXT_LOST = "RequestContextHolder has no attributes - HTTP context lost";
    public static final String LOG_ERROR_EXTRACTING_USERNAME = "Error extracting username from request";
    public static final String LOG_COULD_NOT_EXTRACT_CLIENT_IP = "Could not extract client IP from request";
    public static final String LOG_COULD_NOT_EXTRACT_USER_AGENT = "Could not extract user agent from request";
    public static final String LOG_ASPECT_TRIGGERED_CREATED = "Aspect triggered - auditWorkflowCreated";
    public static final String LOG_RESULT_NULL_CANNOT_AUDIT_CREATION = "Result is null, cannot audit workflow creation";
    public static final String LOG_FOUND_WORKFLOW_ENTITY_FOR_AUDIT = "Found Workflow entity for audit";
    public static final String LOG_PUBLISHED_WORKFLOW_CREATED_AUDIT = "Published workflow-created audit event";
    public static final String LOG_FAILED_PUBLISH_WORKFLOW_CREATED_AUDIT = "Failed to publish workflow-created audit event";
    public static final String LOG_ASPECT_TRIGGERED_UPDATED = "Aspect triggered - auditWorkflowUpdated";
    public static final String LOG_PUBLISHED_WORKFLOW_UPDATED_AUDIT = "Published workflow-updated audit event";
    public static final String LOG_FAILED_PUBLISH_WORKFLOW_UPDATED_AUDIT = "Failed to publish workflow-updated audit event";
    public static final String LOG_PUBLISHED_WORKFLOW_DELETED_AUDIT = "Published workflow-deleted audit event";
    public static final String LOG_FAILED_PUBLISH_WORKFLOW_DELETED_AUDIT = "Failed to publish workflow-deleted audit event";
    public static final String LOG_ASPECT_TRIGGERED_EXECUTED = "Aspect triggered - auditWorkflowExecuted";
    public static final String LOG_USING_ACTOR_FROM_EXECUTION_CONTEXT = "Using actor from execution context";
    public static final String LOG_COULD_NOT_EXTRACT_CREATOR_ID = "Could not extract creator ID from execution context";
    public static final String LOG_EXTRACTED_TENANT_FROM_EXECUTION = "Extracted tenantId from execution context";
    public static final String LOG_EXTRACTED_TENANT_FROM_HTTP_FALLBACK = "Extracted tenantId from HTTP context (fallback)";
    public static final String LOG_PUBLISHING_WORKFLOW_EXECUTION_AUDIT = "Publishing workflow execution audit with tenantId";
    public static final String LOG_PUBLISHED_WORKFLOW_EXECUTED_AUDIT = "Published workflow-executed audit event";
    public static final String LOG_FAILED_PUBLISH_WORKFLOW_EXECUTED_AUDIT = "Failed to publish workflow-executed audit event";
    public static final String LOG_PUBLISHED_APPROVAL_CREATED_AUDIT = "Published approval-created audit event";
    public static final String LOG_FAILED_PUBLISH_APPROVAL_CREATED_AUDIT = "Failed to publish approval-created audit event";
    public static final String LOG_PUBLISHED_APPROVAL_APPROVED_AUDIT = "Published approval-approved audit event";
    public static final String LOG_FAILED_PUBLISH_APPROVAL_APPROVED_AUDIT = "Failed to publish approval-approved audit event";
    public static final String LOG_PUBLISHED_APPROVAL_REJECTED_AUDIT = "Published approval-rejected audit event";
    public static final String LOG_FAILED_PUBLISH_APPROVAL_REJECTED_AUDIT = "Failed to publish approval-rejected audit event";
    public static final String LOG_PUBLISHED_EXECUTION_CANCELLED_AUDIT = "Published execution-cancelled audit event";
    public static final String LOG_FAILED_PUBLISH_EXECUTION_CANCELLED_AUDIT = "Failed to publish execution-cancelled audit event";

    // ==================== AUDIT EVENT PUBLISHER LOG MESSAGES ====================
    public static final String LOG_CONVERTING_TO_STANDARD_AUDIT = "Converting workflow audit event to standard audit format";
    public static final String LOG_PUBLISHING_AUDIT_TO_KAFKA = "Publishing audit event to Kafka";
    public static final String LOG_AUDIT_EVENT_PUBLISHED = "Audit event published successfully";
    public static final String LOG_FAILED_PUBLISH_AUDIT_TO_KAFKA = "Failed to publish audit event to Kafka";
    public static final String LOG_REQUEST_CONTEXT_NOT_AVAILABLE = "Request context not available (async thread, scheduled task, etc.)";

    // ==================== EMAIL REST CLIENT LOG MESSAGES ====================
    public static final String LOG_COULD_NOT_EXTRACT_AUTH_HEADER = "Could not extract auth header from request context";
    public static final String LOG_EMAIL_SENT_SUCCESSFULLY = "Email sent successfully via core-service";
    public static final String LOG_FAILED_SEND_EMAIL = "Failed to send email via core-service";
    public static final String LOG_EXCEPTION_SENDING_EMAIL = "Exception occurred while sending email";
    public static final String LOG_CORE_SERVICE_EMAIL_UNAVAILABLE = "Core-service email API unavailable";
    public static final String LOG_EMAIL_FALLBACK = "Email fallback activated - email queued for retry";
    public static final String LOG_CORE_SERVICE_HEALTH_CHECK_FAILED = "Core-service health check failed";

    // ==================== IDENTITY REST CLIENT LOG MESSAGES ====================
    public static final String LOG_FORWARDING_JWT_TO_IDENTITY = "Forwarding JWT token to identity service";
    public static final String LOG_NO_JWT_TOKEN_FOUND = "No JWT token found in request context";
    public static final String LOG_COULD_NOT_EXTRACT_JWT = "Could not extract JWT from request context";
    public static final String LOG_EMPLOYEE_CREATED_VIA_REST = "Employee created successfully via REST API";
    public static final String LOG_HTTP_ERROR_CREATING_EMPLOYEE = "HTTP error creating employee via REST API";
    public static final String LOG_ERROR_CREATING_EMPLOYEE_VIA_REST = "Error creating employee via REST API";
    public static final String LOG_IDENTITY_SERVICE_HEALTH_CHECK_FAILED = "Identity service health check failed";

    // ==================== NOTIFICATION KAFKA CLIENT LOG MESSAGES ====================
    public static final String LOG_FAILED_SEND_WORKFLOW_COMPLETED_NOTIFICATION = "Failed to send workflow completed notification";
    public static final String LOG_NOTIFICATION_SENT_TO_KAFKA = "Notification sent to Kafka successfully";
    public static final String LOG_FAILED_SEND_NOTIFICATION_TO_KAFKA = "Failed to send notification to Kafka";
    public static final String LOG_FAILED_SERIALIZE_NOTIFICATION = "Failed to serialize notification for Kafka";
    public static final String LOG_KAFKA_HEALTH_CHECK_FAILED = "Kafka health check failed";
    public static final String LOG_KAFKA_NOTIFICATION_UNAVAILABLE = "Kafka notification service unavailable - fallback activated";
    public static final String LOG_NOTIFICATION_NOT_SENT_KAFKA_UNAVAILABLE = "Notification not sent - Kafka unavailable";

    // ==================== APPROVAL DELEGATION CONSTANTS (Phase 3) ====================
    // Object Types
    public static final String OBJECT_TYPE_DELEGATION = "ApprovalDelegation";
    
    // Error Messages
    public static final String ERROR_DELEGATION_NOT_FOUND = "Delegation not found: ";
    public static final String ERROR_DELEGATION_CANNOT_DELEGATE_TO_SELF = "Cannot delegate approvals to yourself";
    public static final String ERROR_DELEGATION_INVALID_DATE_RANGE = "End date must be after start date";
    public static final String ERROR_DELEGATION_OVERLAPPING_EXISTS = "An overlapping delegation already exists for this period";
    public static final String ERROR_DELEGATION_ALREADY_INACTIVE = "Delegation is already inactive";
    public static final String ERROR_DELEGATION_NOT_AUTHORIZED = "You are not authorized to manage this delegation";
    
    // Log Messages - Delegation Lookup
    public static final String LOG_DELEGATION_FOUND_FOR_USER = "Active delegation found for user";
    public static final String LOG_NO_DELEGATION_FOUND_FOR_USER = "No active delegation found for user";
    public static final String LOG_AUTO_DELEGATION_APPLIED = "Approval auto-delegated based on active delegation rule";
    
    // Log Messages - Delegation Management
    public static final String LOG_CREATING_DELEGATION = "Creating new approval delegation";
    public static final String LOG_DELEGATION_CREATED = "Approval delegation created successfully";
    public static final String LOG_DELEGATION_UPDATED = "Approval delegation updated";
    public static final String LOG_DELEGATION_DEACTIVATED = "Approval delegation deactivated";
    public static final String LOG_DELEGATION_DELETED = "Approval delegation deleted";
    public static final String LOG_GETTING_DELEGATION = "Getting delegation details";
    public static final String LOG_GETTING_USER_DELEGATIONS = "Getting user delegations";
    public static final String LOG_GETTING_DELEGATIONS_TO_USER = "Getting delegations assigned to user";
    public static final String LOG_GETTING_ALL_ACTIVE_DELEGATIONS = "Getting all active delegations for tenant";
    
    // Log Messages - Notifications
    public static final String LOG_FAILED_SEND_DELEGATION_CREATED_NOTIFICATION = "Failed to send delegation created notification";
    public static final String LOG_AUTO_DELEGATION_NOTIFICATION_SENT = "Auto-delegation notification sent to delegate";
    public static final String LOG_FAILED_SEND_AUTO_DELEGATION_NOTIFICATION = "Failed to send auto-delegation notification";
    
    // Log Messages - Cleanup
    public static final String LOG_DELEGATION_CLEANUP_STARTED = "Starting expired delegation cleanup";
    public static final String LOG_DELEGATION_CLEANUP_COMPLETED = "Expired delegation cleanup completed";
    
    // Detail Keys
    public static final String DETAIL_KEY_FROM_USER_ID = "fromUserId";
    public static final String DETAIL_KEY_TO_USER_ID = "toUserId";
    public static final String DETAIL_KEY_DELEGATION_ID = "delegationId";
    public static final String DETAIL_KEY_START_DATE = "startDate";
    public static final String DETAIL_KEY_END_DATE = "endDate";
    public static final String DETAIL_KEY_REASON = "reason";
    public static final String DETAIL_KEY_ERROR = "error";
    public static final String DETAIL_KEY_COUNT = "count";
    public static final String DETAIL_KEY_APPROVAL_ID = "approvalId";
    public static final String DETAIL_KEY_ORIGINAL_APPROVER_ID = "originalApproverId";
    public static final String DETAIL_KEY_EFFECTIVE_APPROVER_ID = "effectiveApproverId";
    public static final String DETAIL_KEY_TENANT_ID = "tenantId";

    // ==================== Phase 4: Escalation Constants ====================

    // Escalation Config Keys
    public static final String CONFIG_KEY_ESCALATION = "escalation";
    public static final String CONFIG_KEY_ENABLED = "enabled";
    public static final String CONFIG_KEY_AFTER_MINUTES = "afterMinutes";
    public static final String CONFIG_KEY_MAX_ESCALATION_LEVELS = "maxEscalationLevels";
    public static final String CONFIG_KEY_TIMEOUT_MINUTES = "timeoutMinutes";
    public static final String CONFIG_KEY_NOTIFY_ORIGINAL_APPROVER = "notifyOriginalApprover";

    // Escalation Log Messages
    public static final String LOG_ESCALATION_CHECK_STARTED = "Starting approval escalation check";
    public static final String LOG_ESCALATION_CHECK_COMPLETED = "Approval escalation check completed";
    public static final String LOG_ESCALATION_APPROVALS_FOUND = "Found approvals past timeout for escalation check";
    public static final String LOG_ESCALATION_CREATED = "Escalated approval created successfully";
    public static final String LOG_ESCALATION_FAILED = "Failed to escalate approval";
    public static final String LOG_ESCALATION_NOT_CONFIGURED = "Escalation not configured for this approval";
    public static final String LOG_ESCALATION_NO_NODE_CONFIG = "No node configuration found for approval";
    public static final String LOG_ESCALATION_MAX_LEVEL_REACHED = "Maximum escalation level reached";
    public static final String LOG_ESCALATION_NO_APPROVERS_FOUND = "No escalation approvers found";
    public static final String LOG_ESCALATION_RESOLUTION_FAILED = "Failed to resolve escalation approvers";
    public static final String LOG_APPROVAL_TIMED_OUT_NO_ESCALATION = "Approval timed out without escalation";
    public static final String LOG_ESCALATION_NOTIFICATION_SENT = "Escalation notification sent to new approver";
    public static final String LOG_ESCALATION_ORIGINAL_APPROVER_NOTIFIED = "Original approver notified of escalation";
    public static final String LOG_FAILED_SEND_ESCALATION_NOTIFICATION = "Failed to send escalation notification";

    // ==================== PHASE 5: CACHING CONSTANTS ====================
    
    // Cache log messages
    public static final String LOG_CACHE_HIT = "Cache hit for key";
    public static final String LOG_CACHE_MISS = "Cache miss for key";
    public static final String LOG_CACHE_EVICTED = "Cache evicted for key";
    public static final String LOG_CACHE_CLEARED = "Cache cleared";
    public static final String LOG_CACHE_STATS = "Cache statistics retrieved";

    // Private constructor to prevent instantiation
    private WorkflowConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}
