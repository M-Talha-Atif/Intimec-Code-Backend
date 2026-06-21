package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.client.NotificationKafkaClient;
import com.aidentx.workflowsservice.client.UserGrpcClient;
import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import com.aidentx.workflowsservice.repository.ApprovalDelegationRepository;
import com.aidentx.workflowsservice.util.JwtUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.ApprovalCacheConfig.*;
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Service for managing approval delegations
 *
 * Provides functionality for:
 * - Creating time-based delegation rules
 * - Finding active delegations for automatic approval routing
 * - Managing delegation lifecycle (create, update, deactivate)
 * - Cleanup of expired delegations
 *
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@Service
public class ApprovalDelegationService {

    private final IUnifiedLogger logger;
    private final ApprovalDelegationRepository delegationRepository;
    private final UserGrpcClient userClient;
    private final NotificationKafkaClient notificationClient;
    private final JwtUtils jwtUtils;

    public ApprovalDelegationService(
            ApprovalDelegationRepository delegationRepository,
            UserGrpcClient userClient,
            NotificationKafkaClient notificationClient,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(ApprovalDelegationService.class, LOG_CATEGORY_WORKFLOW_APPROVAL);
        this.delegationRepository = delegationRepository;
        this.userClient = userClient;
        this.notificationClient = notificationClient;
        this.jwtUtils = jwtUtils;
    }

    // ==================== DELEGATION LOOKUP ====================

    /**
     * Find the effective delegate for a user at the current time
     * This is the primary method for automatic approval routing
     *
     * Phase 5: Delegation lookups are cached for 2 minutes to reduce
     * database queries during high approval volume. Cache is keyed by
     * fromUserId + tenantId combination.
     *
     * @param fromUserId The original approver's user ID
     * @param tenantId The tenant ID
     * @return Optional containing the delegate's user ID if an active delegation exists
     */
    @Cacheable(value = CACHE_ACTIVE_DELEGATIONS,
               key = "#fromUserId.toString() + '_' + #tenantId",
               unless = "#result == null || !#result.isPresent()")
    public Optional<UUID> findEffectiveDelegate(UUID fromUserId, String tenantId) {
        return findEffectiveDelegateInternal(fromUserId, tenantId, null);
    }

    /**
     * Find the effective delegate for a user, optionally for a specific workflow
     *
     * @param fromUserId The original approver's user ID
     * @param tenantId The tenant ID
     * @param workflowId Optional workflow ID for workflow-specific delegations
     * @return Optional containing the delegate's user ID if an active delegation exists
     */
    @Cacheable(value = CACHE_ACTIVE_DELEGATIONS,
               key = "#fromUserId.toString() + '_' + #tenantId + '_' + (#workflowId != null ? #workflowId.toString() : 'all')",
               unless = "#result == null || !#result.isPresent()")
    public Optional<UUID> findEffectiveDelegate(UUID fromUserId, String tenantId, UUID workflowId) {
        return findEffectiveDelegateInternal(fromUserId, tenantId, workflowId);
    }

    /**
     * Internal implementation of findEffectiveDelegate without caching
     */
    private Optional<UUID> findEffectiveDelegateInternal(UUID fromUserId, String tenantId, UUID workflowId) {
        OffsetDateTime now = OffsetDateTime.now();

        Optional<ApprovalDelegation> delegation;
        if (workflowId != null) {
            delegation = delegationRepository.findActiveDelegationForWorkflow(
                fromUserId, now, tenantId, workflowId.toString()
            );
        } else {
            delegation = delegationRepository.findFirstActiveDelegation(fromUserId, now, tenantId);
        }

        if (delegation.isPresent()) {
            ApprovalDelegation active = delegation.get();
            logger.info()
                .message(LOG_DELEGATION_FOUND_FOR_USER)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_FROM_USER_ID, fromUserId.toString())
                .detail(DETAIL_KEY_TO_USER_ID, active.getToUserId().toString())
                .detail(DETAIL_KEY_DELEGATION_ID, active.getId().toString())
                .detail(DETAIL_KEY_REASON, active.getReason())
                .log();
            return Optional.of(active.getToUserId());
        }

        return Optional.empty();
    }

    /**
     * Get the full delegation details if active
     */
    public Optional<ApprovalDelegation> findActiveDelegation(UUID fromUserId, String tenantId) {
        OffsetDateTime now = OffsetDateTime.now();
        return delegationRepository.findFirstActiveDelegation(fromUserId, now, tenantId);
    }

    /**
     * Check if a user has any active delegation
     */
    public boolean hasActiveDelegation(UUID fromUserId, String tenantId) {
        OffsetDateTime now = OffsetDateTime.now();
        return delegationRepository.hasActiveDelegation(fromUserId, now, tenantId);
    }

    // ==================== DELEGATION MANAGEMENT ====================

    /**
     * Create a new delegation rule
     *
     * Phase 5: Evicts the delegation cache for the fromUserId to ensure
     * subsequent lookups fetch the fresh delegation data.
     *
     * @param fromUserId User who is delegating
     * @param toUserId User who will receive delegated approvals
     * @param startDate When delegation starts
     * @param endDate When delegation ends
     * @param reason Reason for delegation
     * @param scope Delegation scope (ALL, WORKFLOW, SPECIFIC_WORKFLOWS)
     * @param workflowIds Optional comma-separated workflow IDs (for SPECIFIC_WORKFLOWS)
     * @param notifyDelegate Whether to notify delegate on each routed approval
     * @param notifyDelegator Whether to notify original user when approvals are routed
     * @param createdBy User creating this delegation
     * @param tenantId Tenant ID
     * @return Created delegation entity
     */
    @Transactional
    @CacheEvict(value = CACHE_ACTIVE_DELEGATIONS, key = "#fromUserId.toString() + '_' + #tenantId")
    public ApprovalDelegation createDelegation(
            UUID fromUserId,
            UUID toUserId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String reason,
            ApprovalDelegation.DelegationScope scope,
            String workflowIds,
            Boolean notifyDelegate,
            Boolean notifyDelegator,
            UUID createdBy,
            String tenantId) {

        logger.info()
            .message(LOG_CREATING_DELEGATION)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .detail(DETAIL_KEY_FROM_USER_ID, fromUserId.toString())
            .detail(DETAIL_KEY_TO_USER_ID, toUserId.toString())
            .detail(DETAIL_KEY_START_DATE, startDate.toString())
            .detail(DETAIL_KEY_END_DATE, endDate.toString())
            .log();

        // Validate: Cannot delegate to self
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException(ERROR_DELEGATION_CANNOT_DELEGATE_TO_SELF);
        }

        // Validate: End date must be after start date
        if (endDate.isBefore(startDate) || endDate.equals(startDate)) {
            throw new IllegalArgumentException(ERROR_DELEGATION_INVALID_DATE_RANGE);
        }

        // Validate: Check for overlapping delegations (excluding current if updating)
        if (delegationRepository.hasOverlappingDelegation(fromUserId, startDate, endDate, tenantId, UUID.randomUUID())) {
            throw new IllegalArgumentException(ERROR_DELEGATION_OVERLAPPING_EXISTS);
        }

        ApprovalDelegation delegation = ApprovalDelegation.builder()
                .tenantId(tenantId)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .startDate(startDate)
                .endDate(endDate)
                .reason(reason)
                .scope(scope != null ? scope : ApprovalDelegation.DelegationScope.ALL)
                .workflowIds(workflowIds)
                .notifyDelegate(notifyDelegate != null ? notifyDelegate : true)
                .notifyDelegator(notifyDelegator != null ? notifyDelegator : false)
                .createdBy(createdBy)
                .active(true)
                .build();

        ApprovalDelegation saved = delegationRepository.save(delegation);

        // Notify the delegate about the upcoming delegation
        try {
            sendDelegationCreatedNotification(saved);
        } catch (Exception e) {
            logger.warn()
                .message(LOG_FAILED_SEND_DELEGATION_CREATED_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_ERROR, e.getMessage())
                .log();
        }

        logger.info()
            .message(LOG_DELEGATION_CREATED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_DELEGATION)
            .objectId(saved.getId().toString())
            .log();

        return saved;
    }

    /**
     * Update an existing delegation
     *
     * Phase 5: Evicts all delegation cache entries since we need to evict
     * by the fromUserId which requires fetching the delegation first.
     */
    @Transactional
    @CacheEvict(value = CACHE_ACTIVE_DELEGATIONS, allEntries = true)
    public ApprovalDelegation updateDelegation(
            UUID delegationId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String reason,
            ApprovalDelegation.DelegationScope scope,
            String workflowIds,
            Boolean notifyDelegate,
            Boolean notifyDelegator,
            UUID updatedBy) {

        ApprovalDelegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_DELEGATION_NOT_FOUND + delegationId));

        // Validate: End date must be after start date
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(ERROR_DELEGATION_INVALID_DATE_RANGE);
        }

        // Validate: Check for overlapping delegations
        OffsetDateTime checkStart = startDate != null ? startDate : delegation.getStartDate();
        OffsetDateTime checkEnd = endDate != null ? endDate : delegation.getEndDate();
        if (delegationRepository.hasOverlappingDelegation(
                delegation.getFromUserId(), checkStart, checkEnd, delegation.getTenantId(), delegationId)) {
            throw new IllegalArgumentException(ERROR_DELEGATION_OVERLAPPING_EXISTS);
        }

        if (startDate != null) delegation.setStartDate(startDate);
        if (endDate != null) delegation.setEndDate(endDate);
        if (reason != null) delegation.setReason(reason);
        if (scope != null) delegation.setScope(scope);
        if (workflowIds != null) delegation.setWorkflowIds(workflowIds);
        if (notifyDelegate != null) delegation.setNotifyDelegate(notifyDelegate);
        if (notifyDelegator != null) delegation.setNotifyDelegator(notifyDelegator);
        delegation.setUpdatedBy(updatedBy);

        logger.info()
            .message(LOG_DELEGATION_UPDATED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_DELEGATION)
            .objectId(delegationId.toString())
            .log();

        return delegationRepository.save(delegation);
    }

    /**
     * Deactivate a delegation (soft delete)
     *
     * Phase 5: Evicts all delegation cache entries to ensure deactivated
     * delegations are not served from cache.
     */
    @Transactional
    @CacheEvict(value = CACHE_ACTIVE_DELEGATIONS, allEntries = true)
    public ApprovalDelegation deactivateDelegation(UUID delegationId, UUID deactivatedBy) {
        ApprovalDelegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_DELEGATION_NOT_FOUND + delegationId));

        delegation.setActive(false);
        delegation.setUpdatedBy(deactivatedBy);

        logger.info()
            .message(LOG_DELEGATION_DEACTIVATED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .objectType(OBJECT_TYPE_DELEGATION)
            .objectId(delegationId.toString())
            .log();

        return delegationRepository.save(delegation);
    }

    /**
     * Get a delegation by ID
     */
    public Optional<ApprovalDelegation> getDelegation(UUID delegationId) {
        return delegationRepository.findById(delegationId);
    }

    /**
     * Get all delegations for a user (as delegator)
     */
    public List<ApprovalDelegation> getDelegationsForUser(UUID userId) {
        return delegationRepository.findByFromUserId(userId);
    }

    /**
     * Get all delegations to a user (as delegate)
     */
    public List<ApprovalDelegation> getDelegationsToUser(UUID userId) {
        return delegationRepository.findByToUserId(userId);
    }

    /**
     * Get all active delegations for a tenant (admin view)
     */
    public List<ApprovalDelegation> getAllActiveDelegations(String tenantId) {
        return delegationRepository.findAllActiveDelegations(OffsetDateTime.now(), tenantId);
    }

    // ==================== NOTIFICATION HELPERS ====================

    /**
     * Send notification when a delegation is created
     */
    private void sendDelegationCreatedNotification(ApprovalDelegation delegation) {
        try {
            // Fetch delegate user details
            String delegateEmail = DEFAULT_APPROVER_EMAIL;
            String delegateName = DEFAULT_APPROVER_NAME;
            String delegatorName = DEFAULT_APPROVER_NAME;

            try {
                var toUserResponse = userClient.getUserById(
                    delegation.getToUserId().toString(),
                    delegation.getTenantId()
                );
                if (toUserResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) toUserResponse;
                    delegateEmail = (String) userMap.getOrDefault("email", delegateEmail);
                    delegateName = (String) userMap.getOrDefault("fullName", delegateName);
                }

                var fromUserResponse = userClient.getUserById(
                    delegation.getFromUserId().toString(),
                    delegation.getTenantId()
                );
                if (fromUserResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) fromUserResponse;
                    delegatorName = (String) userMap.getOrDefault("fullName", delegatorName);
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_FETCH_DELEGATEE_DETAILS)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail(DETAIL_KEY_ERROR, e.getMessage())
                    .log();
            }

            // Send notification to delegate
            notificationClient.sendDelegationCreatedNotification(
                delegation.getTenantId(),
                delegation.getToUserId().toString(),
                delegateEmail,
                delegateName,
                delegatorName,
                delegation.getStartDate().toString(),
                delegation.getEndDate().toString(),
                delegation.getReason()
            );

            logger.info()
                .message(LOG_DELEGATION_NOTIFICATION_SENT)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_TO_USER_ID, delegation.getToUserId().toString())
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_DELEGATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_ERROR, e.getMessage())
                .log();
        }
    }

    /**
     * Send notification when an approval is auto-delegated
     */
    public void sendApprovalDelegatedNotification(
            ApprovalDelegation delegation,
            UUID approvalId,
            String workflowName,
            String executionId) {

        if (!Boolean.TRUE.equals(delegation.getNotifyDelegate())) {
            return;
        }

        try {
            String delegateEmail = DEFAULT_APPROVER_EMAIL;
            String delegateName = DEFAULT_APPROVER_NAME;
            String delegatorName = DEFAULT_APPROVER_NAME;

            try {
                var toUserResponse = userClient.getUserById(
                    delegation.getToUserId().toString(),
                    delegation.getTenantId()
                );
                if (toUserResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) toUserResponse;
                    delegateEmail = (String) userMap.getOrDefault("email", delegateEmail);
                    delegateName = (String) userMap.getOrDefault("fullName", delegateName);
                }

                var fromUserResponse = userClient.getUserById(
                    delegation.getFromUserId().toString(),
                    delegation.getTenantId()
                );
                if (fromUserResponse instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) fromUserResponse;
                    delegatorName = (String) userMap.getOrDefault("fullName", delegatorName);
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_FAILED_FETCH_DELEGATEE_DETAILS)
                    .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                    .detail(DETAIL_KEY_ERROR, e.getMessage())
                    .log();
            }

            notificationClient.sendApprovalAutoDelegatedNotification(
                delegation.getTenantId(),
                delegation.getToUserId().toString(),
                delegateEmail,
                delegateName,
                delegatorName,
                workflowName,
                executionId,
                approvalId.toString()
            );

            logger.info()
                .message(LOG_AUTO_DELEGATION_NOTIFICATION_SENT)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_APPROVAL_ID, approvalId.toString())
                .detail(DETAIL_KEY_TO_USER_ID, delegation.getToUserId().toString())
                .log();

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_SEND_AUTO_DELEGATION_NOTIFICATION)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .detail(DETAIL_KEY_ERROR, e.getMessage())
                .log();
        }
    }

    // ==================== SCHEDULED CLEANUP ====================

    /**
     * Scheduled job to deactivate expired delegations
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredDelegations() {
        logger.info()
            .message(LOG_DELEGATION_CLEANUP_STARTED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .log();

        List<ApprovalDelegation> expired = delegationRepository.findExpiredActiveDelegations(OffsetDateTime.now());

        int count = 0;
        for (ApprovalDelegation delegation : expired) {
            delegation.setActive(false);
            delegationRepository.save(delegation);
            count++;
        }

        logger.info()
            .message(LOG_DELEGATION_CLEANUP_COMPLETED)
            .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
            .detail(DETAIL_KEY_COUNT, String.valueOf(count))
            .log();
    }

    // ==================== HELPER METHODS ====================

    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return jwtUtils.extractTenantId(request);
            }
        } catch (Exception e) {
            logger.warn()
                .message(LOG_REQUEST_CONTEXT_NOT_AVAILABLE)
                .category(LOG_CATEGORY_WORKFLOW_APPROVAL)
                .log();
        }
        return null;
    }
}