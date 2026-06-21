package com.aidentx.iam.access_roles_management.controller;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.dto.AccessRoleAuditHistoryDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRulePreviewRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleGlobalScheduleUpdateRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleConfigResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleUpdateResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRulePreviewResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRulePreviewService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for dynamic access-role assignment-rule CRUD, preview, and
 * manual trigger operations.
 *
 * <p>Rules are tenant-scoped through the {@code X-Tenant-Id} header and target a
 * single access role via {@code roleId}. This controller also emits structured
 * endpoint-level logs so operators can identify which endpoint was invoked, for
 * which tenant, and with which primary identifiers.</p>
 *
 * <p>Execution history retrieval is served separately by
 * {@code AccessRoleAssignmentRuleExecutionHistoryController}.</p>
 */
@RestController
@RequestMapping(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
public class AccessRoleAssignmentRuleController {

    private final AccessRoleAssignmentRuleService ruleService;
    private final AccessRoleAssignmentExecutionService executionService;
    private final AccessRoleAssignmentRulePreviewService previewService;
    private final AccessRoleAuditLogService auditLogService;
    private final IUnifiedLogger logger;

    /**
     * Creates the controller with required rule, preview, and execution services.
     *
     * @param ruleService service handling CRUD operations for assignment rules
     * @param executionService service handling manual/scheduled execution of rules
     * @param previewService service handling dry-run previews
     * @param unifiedLoggerFactory logger factory used to build structured logs
     */
    public AccessRoleAssignmentRuleController(AccessRoleAssignmentRuleService ruleService,
                                              AccessRoleAssignmentExecutionService executionService,
                                              AccessRoleAuditLogService auditLogService,
                                              AccessRoleAssignmentRulePreviewService previewService,
                                              UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ruleService = ruleService;
        this.executionService = executionService;
        this.auditLogService = auditLogService;
        this.previewService = previewService;
        this.logger = unifiedLoggerFactory.getLogger(
                getClass(),
                AccessRoleAssignmentRuleConstants.LOG_CATEGORY
        );
    }

    /**
     * Creates a new access-role assignment rule.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param request create request body
     * @return created rule response
     */
    @PostMapping
    public ResponseEntity<AccessRoleAssignmentRuleResponse> createRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @Valid @RequestBody CreateAccessRoleAssignmentRuleRequest request) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_CREATE_RULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT, AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID,
                        request != null && request.getRoleId() != null ? request.getRoleId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE,
                        request != null && request.getScheduleType() != null ? request.getScheduleType() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_GROUP_COUNT,
                        request != null && request.getConditionGroups() != null
                                ? String.valueOf(request.getConditionGroups().size())
                                : "0")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, getAuthenticatedUsername())
                .log();

        AccessRoleAssignmentRuleResponse response =
                ruleService.createRule(tenantId, request, getAuthenticatedUsername());

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_CREATE_RULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT, AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.CREATED.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID,
                        response != null && response.getId() != null ? response.getId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID,
                        response != null && response.getRoleId() != null ? response.getRoleId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_NAME,
                        response != null ? response.getName() : "null")
                .log();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing access-role assignment rule.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param ruleId rule identifier
     * @param request update request body
     * @return updated rule response
     */
    @PutMapping("/{ruleId}")
    public ResponseEntity<AccessRoleAssignmentRuleResponse> updateRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateAccessRoleAssignmentRuleRequest request) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_UPDATE_RULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "PUT")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE,
                        request != null && request.getScheduleType() != null ? request.getScheduleType() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_GROUP_COUNT,
                        request != null && request.getConditionGroups() != null
                                ? String.valueOf(request.getConditionGroups().size())
                                : "0")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, getAuthenticatedUsername())
                .log();

        AccessRoleAssignmentRuleResponse response =
                ruleService.updateRule(tenantId, ruleId, request, getAuthenticatedUsername());

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_UPDATE_RULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "PUT")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID,
                        response != null && response.getId() != null ? response.getId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID,
                        response != null && response.getRoleId() != null ? response.getRoleId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_NAME,
                        response != null ? response.getName() : "null")
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Fetches a rule by ID.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param ruleId rule identifier
     * @return matching rule response
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<AccessRoleAssignmentRuleResponse> getRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID ruleId) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .log();

        AccessRoleAssignmentRuleResponse response = ruleService.getRule(tenantId, ruleId);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID,
                        response != null && response.getId() != null ? response.getId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID,
                        response != null && response.getRoleId() != null ? response.getRoleId().toString() : "null")
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves persistent audit history for a specific assignment rule.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param ruleId rule identifier
     * @param action optional audit action filter
     * @param search optional actor username/id search text
     * @param pageable pagination and sorting information
     * @return paged rule audit history entries
     */
    @GetMapping(AccessRoleAssignmentRuleConstants.PATH_AUDIT_HISTORY)
    public ResponseEntity<Page<AccessRoleAuditHistoryDto>> getRuleAuditHistory(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID ruleId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_AUDIT_HISTORY_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + AccessRoleAssignmentRuleConstants.PATH_AUDIT_HISTORY)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ACTION, action != null ? action : "ALL")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SEARCH, search != null ? search : "ALL")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_PAGE, String.valueOf(pageable.getPageNumber()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SIZE, String.valueOf(pageable.getPageSize()))
                .log();

        Page<AccessRoleAuditHistoryDto> response = auditLogService.findRuleAuditHistory(
                tenantId,
                ruleId,
                action,
                search,
                pageable);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_AUDIT_HISTORY_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + AccessRoleAssignmentRuleConstants.PATH_AUDIT_HISTORY)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ACTION, action != null ? action : "ALL")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SEARCH, search != null ? search : "ALL")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RESULT_COUNT,
                        String.valueOf(response != null ? response.getNumberOfElements() : 0))
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the tenant-level global schedule config used by access-role assignment rules.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @return persisted global schedule config or the default config when none exists
     */
    @GetMapping(AccessRoleAssignmentRuleConstants.PATH_GLOBAL_SCHEDULE)
    public ResponseEntity<AccessRoleAssignmentRuleGlobalScheduleConfigResponse> getGlobalScheduleConfig(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_GLOBAL_SCHEDULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + AccessRoleAssignmentRuleConstants.PATH_GLOBAL_SCHEDULE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .log();

        AccessRoleAssignmentRuleGlobalScheduleConfigResponse response = ruleService.getGlobalScheduleConfig(tenantId);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_GLOBAL_SCHEDULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + AccessRoleAssignmentRuleConstants.PATH_GLOBAL_SCHEDULE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE,
                        response != null ? response.getScheduleType() : "null")
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the tenant-level global schedule and applies it to all access-role rules.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param request global schedule request body
     * @return updated schedule summary
     */
    @PutMapping(AccessRoleAssignmentRuleConstants.PATH_GLOBAL_SCHEDULE)
    public ResponseEntity<AccessRoleAssignmentRuleGlobalScheduleUpdateResponse> updateGlobalSchedule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @Valid @RequestBody AccessRoleAssignmentRuleGlobalScheduleUpdateRequest request) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_UPDATE_GLOBAL_SCHEDULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + AccessRoleAssignmentRuleConstants.PATH_GLOBAL_SCHEDULE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "PUT")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE,
                        request != null ? request.getScheduleType() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_CRON_EXPRESSION,
                        request != null && request.getCronExpression() != null ? request.getCronExpression() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TIME,
                        request != null && request.getScheduleTime() != null ? request.getScheduleTime() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_DATE,
                        request != null && request.getScheduleDate() != null ? request.getScheduleDate() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TIMEZONE,
                        request != null && request.getScheduleTimezone() != null ? request.getScheduleTimezone() : "none")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, getAuthenticatedUsername())
                .log();

        AccessRoleAssignmentRuleGlobalScheduleUpdateResponse response =
                ruleService.updateGlobalScheduleType(tenantId, request);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_UPDATE_GLOBAL_SCHEDULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + AccessRoleAssignmentRuleConstants.PATH_GLOBAL_SCHEDULE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "PUT")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SCHEDULE_TYPE,
                        response != null ? response.getScheduleType() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULES_UPDATED,
                        String.valueOf(response != null ? response.getRulesUpdated() : 0))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_NEXT_EXECUTION_PREVIEW,
                        response != null && response.getNextExecutionPreview() != null
                                ? response.getNextExecutionPreview()
                                : "none")
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Fetches the rule targeting a specific role.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param roleId target role identifier
     * @return matching rule response or 404 when none exists
     */
    @GetMapping("/by-role/{roleId}")
    public ResponseEntity<AccessRoleAssignmentRuleResponse> getRuleByRole(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_BY_ROLE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/by-role/{roleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId != null ? roleId.toString() : "null")
                .log();

        return ruleService.getRuleByRole(tenantId, roleId)
                .map(response -> {
                    logger.info()
                            .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_BY_ROLE_RESPONSE)
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                                    AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/by-role/{roleId}")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID,
                                    response.getRoleId() != null ? response.getRoleId().toString() : "null")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID,
                                    response.getId() != null ? response.getId().toString() : "null")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_FOUND, "true")
                            .log();
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    logger.info()
                            .message(AccessRoleAssignmentRuleConstants.LOG_GET_RULE_BY_ROLE_NOT_FOUND)
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                                    AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/by-role/{roleId}")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.NOT_FOUND.value()))
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId != null ? roleId.toString() : "null")
                            .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_FOUND, "false")
                            .log();
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Lists rules for a tenant.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param page zero-based page number
     * @param size page size
     * @param sort optional sort field; prefix with {@code -} for descending
     * @return paged rule responses
     */
    @GetMapping
    public ResponseEntity<Page<AccessRoleAssignmentRuleResponse>> listRules(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_LIST_RULES_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT, AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_PAGE, String.valueOf(page))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SIZE, String.valueOf(size))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SORT, sort != null ? sort : "null")
                .log();

        Page<AccessRoleAssignmentRuleResponse> response = ruleService.listRules(tenantId, page, size, sort);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_LIST_RULES_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT, AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "GET")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_PAGE, String.valueOf(page))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SIZE, String.valueOf(size))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RESULT_COUNT,
                        String.valueOf(response != null ? response.getNumberOfElements() : 0))
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Runs a read-only preview/dry-run for an access-role assignment rule.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param request preview request body
     * @return simulated assignment/revocation impact without writing records
     */
    @PostMapping("/preview")
    public ResponseEntity<AccessRoleAssignmentRulePreviewResponse> previewRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @Valid @RequestBody AccessRoleAssignmentRulePreviewRequest request) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_PREVIEW_RULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/preview")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID,
                        request != null && request.getRuleId() != null ? request.getRuleId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID,
                        request != null && request.getRoleId() != null ? request.getRoleId().toString() : "null")
                .log();

        AccessRoleAssignmentRulePreviewResponse response = previewService.previewRule(tenantId, request);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_PREVIEW_RULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/preview")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .log();

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an access-role assignment rule.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param ruleId rule identifier
     * @return empty 204 response
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID ruleId) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_DELETE_RULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "DELETE")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, getAuthenticatedUsername())
                .log();

        ruleService.deleteRule(tenantId, ruleId);

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_DELETE_RULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "DELETE")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.NO_CONTENT.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .log();

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Trigger endpoints
    // -------------------------------------------------------------------------

    /**
     * Manually triggers evaluation of a single assignment rule.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param ruleId rule identifier
     * @return execution summary
     */
    @PostMapping("/{ruleId}/execute")
    public ResponseEntity<AccessRoleAssignmentExecution> executeRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID ruleId) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_EXECUTE_RULE_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}/execute")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, "MANUAL")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, getAuthenticatedUsername())
                .log();

        AccessRoleAssignmentExecution execution = executionService.executeRule(
                tenantId,
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.MANUAL,
                getAuthenticatedUsername()
        );

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_EXECUTE_RULE_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/{ruleId}/execute")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId != null ? ruleId.toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXECUTION_ID,
                        execution != null && execution.getId() != null ? execution.getId().toString() : "null")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXECUTION_STATUS,
                        execution != null && execution.getStatus() != null ? execution.getStatus().name() : "null")
                .log();

        return ResponseEntity.ok(execution);
    }

    /**
     * Triggers evaluation of all enabled assignment rules for a tenant.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @return list of execution summaries
     */
    @PostMapping("/execute-all")
    public ResponseEntity<List<AccessRoleAssignmentExecution>> executeAllRules(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId) {

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_EXECUTE_ALL_RULES_REQUEST)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/execute-all")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, "MANUAL")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, getAuthenticatedUsername())
                .log();

        List<AccessRoleAssignmentExecution> executions =
                executionService.executeAllRules(tenantId, getAuthenticatedUsername());

        logger.info()
                .message(AccessRoleAssignmentRuleConstants.LOG_EXECUTE_ALL_RULES_RESPONSE)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENDPOINT,
                        AccessRoleAssignmentRuleConstants.API_BASE_PATH + "/execute-all")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_HTTP_METHOD, "POST")
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_STATUS_CODE, String.valueOf(HttpStatus.OK.value()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXECUTION_COUNT,
                        String.valueOf(executions != null ? executions.size() : 0))
                .log();

        return ResponseEntity.ok(executions);
    }

    /**
     * Resolves the authenticated username from Spring Security context.
     *
     * <p>If no authentication is available or the principal name is blank,
     * this method falls back to {@link AccessRoleAssignmentRuleConstants#SYSTEM_TRIGGER}
     * so that audit and endpoint logs always have an actor value.</p>
     *
     * @return authenticated username or the system fallback value
     */
    private String getAuthenticatedUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {
            // Fallback handled below.
        }
        return AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER;
    }
}