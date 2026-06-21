package com.aidentx.authservice.service.Policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.exception.PolicyAlreadyExistsException;
import com.aidentx.authservice.exception.PolicyNotFoundException;
import com.aidentx.authservice.model.dto.PasswordPolicyRequest;
import com.aidentx.authservice.model.dto.PasswordPolicyResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.model.entity.PasswordPolicy;
import com.aidentx.authservice.repository.PasswordPolicyRepository;
import com.aidentx.authservice.repository.UserRepository;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the PasswordPolicyService interface.
 * <p>
 * This service handles all password policy management operations with proper logging,
 * error handling, and tenant-specific isolation. Each tenant can have only one password policy.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Service
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private final IUnifiedLogger logger;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final UserRepository userRepository;
    private final AuditLogEventService auditLogEventService;
    private final JwtUtil jwtUtil;

    public PasswordPolicyServiceImpl(UnifiedLoggerFactory unifiedLoggerFactory,
                                   PasswordPolicyRepository passwordPolicyRepository,
                                   UserRepository userRepository,
                                   AuditLogEventService auditLogEventService,
                                   JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordPolicyService", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.userRepository = userRepository;
        this.auditLogEventService = auditLogEventService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public StatusMessageResponse createPasswordPolicy(String tenantId, PasswordPolicyRequest request) {
        logger.trace().message(String.format(AppConstants.TRACE_CREATE_PASSWORD_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        // Get request context for audit logging
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String clientIp = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        String actorName = getActorNameFromRequest(httpRequest);
        String actorId = getActorIdFromRequest(httpRequest);

        try {
            // Check if policy already exists for this tenant
            if (passwordPolicyRepository.existsByTenantId(tenantId)) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.PASSWORD_POLICY_ALREADY_EXISTS, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_CREATE_PASSWORD_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();

                // Send failure audit event
                sendPasswordPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_CREATE, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.AUDIT_PASSWORD_POLICY_ALREADY_EXISTS, clientIp, userAgent, actorName, actorId, null, null);

                throw new PolicyAlreadyExistsException(AppConstants.PASSWORD_POLICY_ALREADY_EXISTS_MSG);
            }

            // Create new password policy
            PasswordPolicy passwordPolicy = new PasswordPolicy(tenantId);
            passwordPolicy.setMinPasswordLength(request.getMinPasswordLength());
            passwordPolicy.setPasswordHistoryCount(request.getPasswordHistoryCount());
            passwordPolicy.setRequireUppercase(request.getRequireUppercase());
            passwordPolicy.setRequireLowercase(request.getRequireLowercase());
            passwordPolicy.setRequireDigits(request.getRequireDigits());
            passwordPolicy.setRequireSpecialChars(request.getRequireSpecialChars());
            passwordPolicy.setDisallowUsernameOrEmail(request.getDisallowUsernameOrEmail());
            passwordPolicy.setBannedSubstrings(request.getBannedSubstrings());
            passwordPolicy.setMaxPasswordValidityDays(request.getMaxPasswordValidityDays());
            passwordPolicy.setPasswordExpirationNotificationDays(request.getPasswordExpirationNotificationDays());
            
            // Calculate simple policy expiration: current time + maxPasswordValidityDays
            if (request.getMaxPasswordValidityDays() != null && request.getMaxPasswordValidityDays() > 0) {
                long currentTimeMillis = System.currentTimeMillis();
                long daysInMillis = request.getMaxPasswordValidityDays() * 24L * 60L * 60L * 1000L;
                passwordPolicy.setPolicyExpiresAt(currentTimeMillis + daysInMillis);
            } else {
                passwordPolicy.setPolicyExpiresAt(null); // No expiration
            }

            PasswordPolicy savedPolicy = passwordPolicyRepository.save(passwordPolicy);

            // Prepare state after for audit
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateAfter = createPasswordPolicyStateNode(objectMapper, savedPolicy);

            // Send success audit event
            sendPasswordPolicyAuditEvent(savedPolicy.getId().toString(), savedPolicy.getId().toString(), 
                    tenantId, AppConstants.AUDIT_EVENT_CREATE, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.AUDIT_PASSWORD_POLICY_CREATED_SUCCESSFULLY,
                    clientIp, userAgent, actorName, actorId, null, stateAfter);

            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(savedPolicy.getId().toString())
                    .message(String.format(AppConstants.PASSWORD_POLICY_CREATED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_CREATE_PASSWORD_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, savedPolicy.getId().toString()))
                    .log();

            return new StatusMessageResponse(201, AppConstants.PASSWORD_POLICY_CREATED_MSG);

        } catch (PolicyAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.PASSWORD_POLICY_CREATE_ERROR, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_CREATE_PASSWORD_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();

            // Send failure audit event
            sendPasswordPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_CREATE, AppConstants.FAILURE_EVENT_TYPE,
                    AppConstants.AUDIT_PASSWORD_POLICY_CREATION_FAILED, clientIp, userAgent, actorName, actorId, null, null);

            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    @Override
    @Transactional
    public StatusMessageResponse updatePasswordPolicy(String tenantId, PasswordPolicyRequest request) {
        logger.trace().message(String.format(AppConstants.TRACE_UPDATE_PASSWORD_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        // Get request context for audit logging
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String clientIp = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        String actorName = getActorNameFromRequest(httpRequest);
        String actorId = getActorIdFromRequest(httpRequest);

        try {
            Optional<PasswordPolicy> existingPolicyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            if (existingPolicyOpt.isEmpty()) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.PASSWORD_POLICY_NOT_FOUND, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_UPDATE_PASSWORD_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();

                // Send failure audit event
                sendPasswordPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_UPDATE, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.AUDIT_PASSWORD_POLICY_NOT_FOUND_FOR_UPDATE, clientIp, userAgent, actorName, actorId, null, null);

                throw new PolicyNotFoundException(AppConstants.PASSWORD_POLICY_NOT_FOUND_MSG);
            }

            PasswordPolicy existingPolicy = existingPolicyOpt.get();
            
            // Prepare state before for audit
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateBefore = createPasswordPolicyStateNode(objectMapper, existingPolicy);

            // Check if maxPasswordValidityDays is being changed to determine if we need to recalculate policyExpiresAt
            Integer currentMaxPasswordValidityDays = existingPolicy.getMaxPasswordValidityDays();
            Integer newMaxPasswordValidityDays = request.getMaxPasswordValidityDays();
            
            boolean maxPasswordValidityDaysChanged = !Objects.equals(currentMaxPasswordValidityDays, newMaxPasswordValidityDays);

            existingPolicy.setMinPasswordLength(request.getMinPasswordLength());
            existingPolicy.setPasswordHistoryCount(request.getPasswordHistoryCount());
            existingPolicy.setRequireUppercase(request.getRequireUppercase());
            existingPolicy.setRequireLowercase(request.getRequireLowercase());
            existingPolicy.setRequireDigits(request.getRequireDigits());
            existingPolicy.setRequireSpecialChars(request.getRequireSpecialChars());
            existingPolicy.setDisallowUsernameOrEmail(request.getDisallowUsernameOrEmail());
            existingPolicy.setBannedSubstrings(request.getBannedSubstrings());
            existingPolicy.setMaxPasswordValidityDays(request.getMaxPasswordValidityDays());
            existingPolicy.setPasswordExpirationNotificationDays(request.getPasswordExpirationNotificationDays());

            // Only recalculate policyExpiresAt if maxPasswordValidityDays has actually changed
            if (maxPasswordValidityDaysChanged) {
                if (newMaxPasswordValidityDays != null && newMaxPasswordValidityDays > 0) {
                    long currentTimeMillis = System.currentTimeMillis();
                    long daysInMillis = newMaxPasswordValidityDays * 24L * 60L * 60L * 1000L;
                    existingPolicy.setPolicyExpiresAt(currentTimeMillis + daysInMillis);
                } else {
                    existingPolicy.setPolicyExpiresAt(null); // No expiration
                }
            }

            PasswordPolicy savedPolicy = passwordPolicyRepository.save(existingPolicy);

            // Prepare state after for audit
            ObjectNode stateAfter = createPasswordPolicyStateNode(objectMapper, savedPolicy);

            // Send success audit event
            sendPasswordPolicyAuditEvent(savedPolicy.getId().toString(), savedPolicy.getId().toString(), 
                    tenantId, AppConstants.AUDIT_EVENT_UPDATE, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.AUDIT_PASSWORD_POLICY_UPDATED_SUCCESSFULLY,
                    clientIp, userAgent, actorName, actorId, stateBefore, stateAfter);

            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(savedPolicy.getId().toString())
                    .message(String.format(AppConstants.PASSWORD_POLICY_UPDATED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_UPDATE_PASSWORD_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, savedPolicy.getId().toString()))
                    .log();

            return new StatusMessageResponse(200, AppConstants.PASSWORD_POLICY_UPDATED_MSG);

        } catch (PolicyNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.PASSWORD_POLICY_UPDATE_ERROR, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_UPDATE_PASSWORD_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();

            // Send failure audit event
            sendPasswordPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_UPDATE, AppConstants.FAILURE_EVENT_TYPE,
                    AppConstants.AUDIT_PASSWORD_POLICY_UPDATE_FAILED, clientIp, userAgent, actorName, actorId, null, null);

            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordPolicyResponse getPasswordPolicy(String tenantId) {
        logger.trace().message(String.format(AppConstants.TRACE_GET_PASSWORD_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.PASSWORD_POLICY_NOT_FOUND, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_GET_PASSWORD_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();
                throw new PolicyNotFoundException(AppConstants.PASSWORD_POLICY_NOT_FOUND_MSG);
            }

            PasswordPolicy policy = policyOpt.get();
            
            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(policy.getId().toString())
                    .message(String.format(AppConstants.PASSWORD_POLICY_RETRIEVED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_GET_PASSWORD_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, policy.getId().toString()))
                    .log();

            // Convert entity to response DTO
            PasswordPolicyResponse response = new PasswordPolicyResponse();
            response.setId(policy.getId());
            response.setMinPasswordLength(policy.getMinPasswordLength());
            response.setPasswordHistoryCount(policy.getPasswordHistoryCount());
            response.setRequireUppercase(policy.getRequireUppercase());
            response.setRequireLowercase(policy.getRequireLowercase());
            response.setRequireDigits(policy.getRequireDigits());
            response.setRequireSpecialChars(policy.getRequireSpecialChars());
            response.setDisallowUsernameOrEmail(policy.getDisallowUsernameOrEmail());
            response.setBannedSubstrings(policy.getBannedSubstrings());
            response.setMaxPasswordValidityDays(policy.getMaxPasswordValidityDays());
            response.setPasswordExpirationNotificationDays(policy.getPasswordExpirationNotificationDays());
            response.setTenantId(policy.getTenantId());
            response.setCreatedAt(policy.getCreatedAt());
            response.setUpdatedAt(policy.getUpdatedAt());

            return response;

        } catch (PolicyNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.ERROR_RETRIEVE_PASSWORD_POLICY_FOR_TENANT, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_GET_PASSWORD_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();
            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    @Override
    @Transactional
    public StatusMessageResponse deletePasswordPolicy(String tenantId) {
        logger.trace().message(String.format(AppConstants.TRACE_DELETE_PASSWORD_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        // Get request context for audit logging
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String clientIp = RequestUtils.getClientIP(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        String actorName = getActorNameFromRequest(httpRequest);
        String actorId = getActorIdFromRequest(httpRequest);

        try {
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                logger.warn().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                        .message(String.format(AppConstants.PASSWORD_POLICY_NOT_FOUND, tenantId))
                        .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_DELETE_PASSWORD_POLICY, AppConstants.LOG_DETAIL_TENANT_ID, tenantId))
                        .log();

                // Send failure audit event
                sendPasswordPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_DELETE, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.AUDIT_PASSWORD_POLICY_NOT_FOUND_FOR_DELETION, clientIp, userAgent, actorName, actorId, null, null);

                throw new PolicyNotFoundException(AppConstants.PASSWORD_POLICY_NOT_FOUND_MSG);
            }

            PasswordPolicy policy = policyOpt.get();
            
            // Prepare state before for audit
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateBefore = createPasswordPolicyStateNode(objectMapper, policy);

            passwordPolicyRepository.deleteByTenantId(tenantId);

            // Send success audit event
            sendPasswordPolicyAuditEvent(policy.getId().toString(), policy.getId().toString(), 
                    tenantId, AppConstants.AUDIT_EVENT_DELETE, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.AUDIT_PASSWORD_POLICY_DELETED_SUCCESSFULLY,
                    clientIp, userAgent, actorName, actorId, stateBefore, null);

            logger.info().objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(policy.getId().toString())
                    .message(String.format(AppConstants.PASSWORD_POLICY_DELETED_SUCCESS, tenantId))
                    .details(Map.of(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_DELETE_PASSWORD_POLICY, 
                            AppConstants.LOG_DETAIL_TENANT_ID, tenantId, AppConstants.LOG_DETAIL_POLICY_ID, policy.getId().toString()))
                    .log();

            return new StatusMessageResponse(200, AppConstants.PASSWORD_POLICY_DELETED_MSG);

        } catch (PolicyNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType(AppConstants.LOG_OBJECT_TYPE_PASSWORD_POLICY).objectId(tenantId)
                    .message(String.format(AppConstants.PASSWORD_POLICY_DELETE_ERROR, tenantId))
                    .detail(AppConstants.LOG_DETAIL_OPERATION, AppConstants.OP_DELETE_PASSWORD_POLICY)
                    .detail(AppConstants.LOG_DETAIL_TENANT_ID, tenantId).log();

            // Send failure audit event
            sendPasswordPolicyAuditEvent(null, null, tenantId, AppConstants.AUDIT_EVENT_DELETE, AppConstants.FAILURE_EVENT_TYPE,
                    AppConstants.AUDIT_PASSWORD_POLICY_DELETION_FAILED, clientIp, userAgent, actorName, actorId, null, null);

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
     * Helper method to create password policy state node for audit logging
     */
    private ObjectNode createPasswordPolicyStateNode(ObjectMapper objectMapper, PasswordPolicy policy) {
        ObjectNode stateNode = objectMapper.createObjectNode();
        stateNode.put("id", policy.getId().toString());
        stateNode.put("tenantId", policy.getTenantId());
        stateNode.put("minPasswordLength", policy.getMinPasswordLength());
        stateNode.put("passwordHistoryCount", policy.getPasswordHistoryCount());
        stateNode.put("requireUppercase", policy.getRequireUppercase());
        stateNode.put("requireLowercase", policy.getRequireLowercase());
        stateNode.put("requireDigits", policy.getRequireDigits());
        stateNode.put("requireSpecialChars", policy.getRequireSpecialChars());
        stateNode.put("disallowUsernameOrEmail", policy.getDisallowUsernameOrEmail());
        stateNode.put("bannedSubstrings", objectMapper.valueToTree(policy.getBannedSubstrings()));
        stateNode.put("maxPasswordValidityDays", policy.getMaxPasswordValidityDays());
        stateNode.put("passwordExpirationNotificationDays", policy.getPasswordExpirationNotificationDays());
        stateNode.put("createdAt", policy.getCreatedAt());
        stateNode.put("updatedAt", policy.getUpdatedAt());
        return stateNode;
    }

    /**
     * Helper method to send password policy audit events
     */
    private void sendPasswordPolicyAuditEvent(String objectId, String policyId, String tenantId, 
                                            String event, String status, String message, 
                                            String clientIp, String userAgent, String actorName, String actorId,
                                            ObjectNode stateBefore, ObjectNode stateAfter) {
        try {
            logger.debug().message(AppConstants.DEBUG_PASSWORD_POLICY_AUDIT_PREPARE)
                    .detail("event", event)
                    .detail("status", status)
                    .detail("tenantId", tenantId)
                    .detail("actorName", actorName)
                    .log();
                    
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode customMetadata = objectMapper.createObjectNode();
            customMetadata.put("username", actorName);
            customMetadata.put("type", "password_policies");
            customMetadata.put("identifier", actorName);

            auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(
                    actorId != null ? java.util.UUID.fromString(actorId) : null,
                    "user", actorName, tenantId,
                    objectId != null ? java.util.UUID.fromString(objectId) : null,
                    "password_policies", 
                    "Password Policy",
                    event, status, AppConstants.EVENT_RISK_LOW,
                    clientIp, userAgent, message, stateBefore, stateAfter, null, customMetadata));
                    
            logger.info().message(AppConstants.INFO_PASSWORD_POLICY_AUDIT_SUCCESS)
                    .detail("event", event)
                    .detail("status", status)
                    .detail("tenantId", tenantId)
                    .log();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.ERROR_PASSWORD_POLICY_AUDIT_FAILED)
                    .detail("event", event)
                    .detail("status", status)
                    .detail("tenantId", tenantId)
                    .log();
        }
    }
}

