package com.aidentx.authservice.service.Policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.exception.PolicyAlreadyExistsException;
import com.aidentx.authservice.exception.PolicyNotFoundException;
import com.aidentx.authservice.model.dto.AccountLockPolicyRequest;
import com.aidentx.authservice.model.dto.AccountLockPolicyResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.model.entity.AccountLockPolicy;
import com.aidentx.authservice.repository.AccountLockPolicyRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.utils.RequestUtils;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the AccountLockPolicyService interface.
 * <p>
 * This service handles all account lock policy management operations with proper logging,
 * error handling, and tenant-specific isolation. Each tenant can have only one account lock policy.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Service
public class AccountLockPolicyServiceImpl implements AccountLockPolicyService {

    private final IUnifiedLogger logger;
    private final AccountLockPolicyRepository accountLockPolicyRepository;
    private final AuditLogEventService auditLogEventService;
    private final JwtUtil jwtUtil;

    public AccountLockPolicyServiceImpl(UnifiedLoggerFactory unifiedLoggerFactory,
                                       AccountLockPolicyRepository accountLockPolicyRepository,
                                       AuditLogEventService auditLogEventService,
                                       JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("AccountLockPolicyService", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.accountLockPolicyRepository = accountLockPolicyRepository;
        this.auditLogEventService = auditLogEventService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public StatusMessageResponse createAccountLockPolicy(String tenantId, AccountLockPolicyRequest request) {
        logger.trace().message(String.format(AppConstants.TRACE_CREATE_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        // Get request context for audit logging
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String clientIp = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        String actorName = getActorNameFromRequest(httpRequest);
        String actorId = getActorIdFromRequest(httpRequest);

        try {
            // Check if policy already exists for this tenant
            if (accountLockPolicyRepository.existsByTenantId(tenantId)) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_ALREADY_EXISTS, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_CREATE_ACCOUNT_LOCK_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();

                // Send failure audit event
                sendAccountLockPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_CREATE, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_ALREADY_EXISTS, clientIp, userAgent, actorName, actorId, null, null);

                throw new PolicyAlreadyExistsException(AppConstants.ACCOUNT_LOCK_POLICY_ALREADY_EXISTS_MSG);
            }

            // Create new account lock policy
            AccountLockPolicy accountLockPolicy = new AccountLockPolicy(tenantId);
            accountLockPolicy.setMaxFailedLoginAttempts(request.getMaxFailedLoginAttempts());
            // Convert minutes to seconds for database storage
            accountLockPolicy.setLockoutDurationSeconds(request.getLockoutDurationMinutes() * 60);
            accountLockPolicy.setIncreaseDelayAfterFailureSeconds(request.getIncreaseDelayAfterFailureSeconds());

            AccountLockPolicy savedPolicy = accountLockPolicyRepository.save(accountLockPolicy);

            // Prepare state after for audit
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateAfter = createAccountLockPolicyStateNode(objectMapper, savedPolicy);

            // Send success audit event
            sendAccountLockPolicyAuditEvent(savedPolicy.getId().toString(), savedPolicy.getId().toString(), 
                    tenantId, AppConstants.AUDIT_EVENT_CREATE, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_CREATED_SUCCESSFULLY,
                    clientIp, userAgent, actorName, actorId, null, stateAfter);

            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(savedPolicy.getId().toString())
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_CREATED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_CREATE_ACCOUNT_LOCK_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, savedPolicy.getId().toString()))
                    .log();

            return new StatusMessageResponse(201, AppConstants.ACCOUNT_LOCK_POLICY_CREATED_MSG);

        } catch (PolicyAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_CREATE_ERROR, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_CREATE_ACCOUNT_LOCK_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();

            // Send failure audit event
            sendAccountLockPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_CREATE, AppConstants.FAILURE_EVENT_TYPE,
                    AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_CREATION_FAILED, clientIp, userAgent, actorName, actorId, null, null);

            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    @Override
    @Transactional
    public StatusMessageResponse updateAccountLockPolicy(String tenantId, AccountLockPolicyRequest request) {
        logger.trace().message(String.format(AppConstants.TRACE_UPDATE_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        // Get current HTTP request for audit context
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String clientIp = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        String actorName = getActorNameFromRequest(httpRequest);
        String actorId = getActorIdFromRequest(httpRequest);

        try {
            Optional<AccountLockPolicy> existingPolicyOpt = accountLockPolicyRepository.findByTenantId(tenantId);
            if (existingPolicyOpt.isEmpty()) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_UPDATE_ACCOUNT_LOCK_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();
                
                // Send failure audit event
                sendAccountLockPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_UPDATE, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_NOT_FOUND_FOR_UPDATE, clientIp, userAgent, actorName, actorId, null, null);
                
                throw new PolicyNotFoundException(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND_MSG);
            }

            AccountLockPolicy existingPolicy = existingPolicyOpt.get();
            
            // Prepare state before for audit
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateBefore = createAccountLockPolicyStateNode(objectMapper, existingPolicy);
            
            existingPolicy.setMaxFailedLoginAttempts(request.getMaxFailedLoginAttempts());
            // Convert minutes to seconds for database storage
            existingPolicy.setLockoutDurationSeconds(request.getLockoutDurationMinutes() * 60);
            existingPolicy.setIncreaseDelayAfterFailureSeconds(request.getIncreaseDelayAfterFailureSeconds());

            AccountLockPolicy savedPolicy = accountLockPolicyRepository.save(existingPolicy);

            // Prepare state after for audit
            ObjectNode stateAfter = createAccountLockPolicyStateNode(objectMapper, savedPolicy);

            // Send success audit event
            sendAccountLockPolicyAuditEvent(savedPolicy.getId().toString(), savedPolicy.getId().toString(), 
                    tenantId, AppConstants.AUDIT_EVENT_UPDATE, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_UPDATED_SUCCESSFULLY,
                    clientIp, userAgent, actorName, actorId, stateBefore, stateAfter);

            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(savedPolicy.getId().toString())
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_UPDATED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_UPDATE_ACCOUNT_LOCK_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, savedPolicy.getId().toString()))
                    .log();

            return new StatusMessageResponse(200, AppConstants.ACCOUNT_LOCK_POLICY_UPDATED_MSG);

        } catch (PolicyNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_UPDATE_ERROR, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_UPDATE_ACCOUNT_LOCK_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();
            
            // Send failure audit event
            sendAccountLockPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_UPDATE, AppConstants.FAILURE_EVENT_TYPE,
                    AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_UPDATE_FAILED, clientIp, userAgent, actorName, actorId, null, null);
            
            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AccountLockPolicyResponse getAccountLockPolicy(String tenantId) {
        logger.trace().message(String.format(AppConstants.TRACE_GET_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            Optional<AccountLockPolicy> policyOpt = accountLockPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_GET_ACCOUNT_LOCK_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();
                throw new PolicyNotFoundException(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND_MSG);
            }

            AccountLockPolicy policy = policyOpt.get();
            
            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(policy.getId().toString())
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_RETRIEVED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_GET_ACCOUNT_LOCK_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, policy.getId().toString()))
                    .log();

            // Convert entity to response DTO
            AccountLockPolicyResponse response = new AccountLockPolicyResponse();
            response.setId(policy.getId());
            response.setMaxFailedLoginAttempts(policy.getMaxFailedLoginAttempts());
            // Convert seconds back to minutes for API response
            response.setLockoutDurationMinutes(policy.getLockoutDurationSeconds() / 60);
            response.setIncreaseDelayAfterFailureSeconds(policy.getIncreaseDelayAfterFailureSeconds());
            response.setTenantId(policy.getTenantId());
            response.setCreatedAt(policy.getCreatedAt());
            response.setUpdatedAt(policy.getUpdatedAt());

            return response;

        } catch (PolicyNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.ERROR_RETRIEVE_ACCOUNT_LOCK_POLICY_FOR_TENANT, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_GET_ACCOUNT_LOCK_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();
            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    @Override
    @Transactional
    public StatusMessageResponse deleteAccountLockPolicy(String tenantId) {
        logger.trace().message(String.format(AppConstants.TRACE_DELETE_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        // Get current HTTP request for audit context
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String clientIp = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        String actorName = getActorNameFromRequest(httpRequest);
        String actorId = getActorIdFromRequest(httpRequest);

        try {
            Optional<AccountLockPolicy> policyOpt = accountLockPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_DELETE_ACCOUNT_LOCK_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();
                
                // Send failure audit event
                sendAccountLockPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_DELETE, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_NOT_FOUND_FOR_DELETION, clientIp, userAgent, actorName, actorId, null, null);
                
                throw new PolicyNotFoundException(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND_MSG);
            }

            AccountLockPolicy policy = policyOpt.get();
            
            // Prepare state before for audit
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateBefore = createAccountLockPolicyStateNode(objectMapper, policy);
            
            accountLockPolicyRepository.deleteByTenantId(tenantId);

            // Send success audit event
            sendAccountLockPolicyAuditEvent(policy.getId().toString(), policy.getId().toString(), 
                    tenantId, AppConstants.AUDIT_EVENT_DELETE, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_DELETED_SUCCESSFULLY,
                    clientIp, userAgent, actorName, actorId, stateBefore, null);

            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(policy.getId().toString())
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_DELETED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_DELETE_ACCOUNT_LOCK_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, policy.getId().toString()))
                    .log();

            return new StatusMessageResponse(200, AppConstants.ACCOUNT_LOCK_POLICY_DELETED_MSG);

        } catch (PolicyNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_DELETE_ERROR, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_DELETE_ACCOUNT_LOCK_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();
            
            // Send failure audit event
            sendAccountLockPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_DELETE, AppConstants.FAILURE_EVENT_TYPE,
                    AppConstants.AUDIT_ACCOUNT_LOCK_POLICY_DELETION_FAILED, clientIp, userAgent, actorName, actorId, null, null);
            
            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    /**
     * Helper method to get current HTTP request
     */
    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    /**
     * Helper method to extract actor name from JWT token
     */
    private String getActorNameFromRequest(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                return jwtUtil.extractUsername(token);
            }
        } catch (Exception e) {
            logger.debug().message(AppConstants.DEBUG_TOKEN_EXTRACTION_FAILED).log();
        }
        return AppConstants.SYSTEM_USER;
    }

    /**
     * Helper method to extract actor ID from JWT token
     */
    private String getActorIdFromRequest(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                return jwtUtil.extractUserId(token).toString();
            }
        } catch (Exception e) {
            logger.debug().message(AppConstants.DEBUG_ACTOR_ID_EXTRACTION_FAILED).log();
        }
        return null;
    }

    /**
     * Helper method to extract JWT token from request
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(AppConstants.BEARER_PREFIX)) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Helper method to create account lock policy state node for audit logging
     */
    private ObjectNode createAccountLockPolicyStateNode(ObjectMapper objectMapper, AccountLockPolicy policy) {
        ObjectNode stateNode = objectMapper.createObjectNode();
        stateNode.put("id", policy.getId().toString());
        stateNode.put("tenantId", policy.getTenantId());
        stateNode.put("maxFailedLoginAttempts", policy.getMaxFailedLoginAttempts());
        stateNode.put("lockoutDurationSeconds", policy.getLockoutDurationSeconds());
        stateNode.put("increaseDelayAfterFailureSeconds", policy.getIncreaseDelayAfterFailureSeconds());
        stateNode.put("createdAt", policy.getCreatedAt());
        stateNode.put("updatedAt", policy.getUpdatedAt());
        return stateNode;
    }

    /**
     * Helper method to send account lock policy audit events
     */
    private void sendAccountLockPolicyAuditEvent(String objectId, String policyId, String tenantId, 
                                                String event, String status, String message, 
                                                String clientIp, String userAgent, String actorName, String actorId,
                                                ObjectNode stateBefore, ObjectNode stateAfter) {
        try {
            logger.debug().message(AppConstants.DEBUG_ACCOUNT_LOCK_AUDIT_PREPARE)
                    .detail("event", event)
                    .detail("status", status)
                    .detail("tenantId", tenantId)
                    .detail("actorName", actorName)
                    .log();
                    
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode customMetadata = objectMapper.createObjectNode();
            customMetadata.put("username", actorName);
            customMetadata.put("type", "account_lock_policies");
            customMetadata.put("identifier", actorName);

            auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(
                    actorId != null ? java.util.UUID.fromString(actorId) : null,
                    "user", actorName, tenantId,
                    objectId != null ? java.util.UUID.fromString(objectId) : null,
                    "account_lock_policies", 
                    "Lockout Policy",
                    event, status, AppConstants.EVENT_RISK_LOW,
                    clientIp, userAgent, message, stateBefore, stateAfter, null, customMetadata));
                    
            logger.info().message(AppConstants.INFO_ACCOUNT_LOCK_AUDIT_SUCCESS)
                    .detail("event", event)
                    .detail("status", status)
                    .detail("tenantId", tenantId)
                    .log();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.ERROR_ACCOUNT_LOCK_AUDIT_FAILED)
                    .detail("event", event)
                    .detail("status", status)
                    .detail("tenantId", tenantId)
                    .log();
        }
    }
}
