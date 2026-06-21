package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementAuditHistoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;

import java.util.UUID;

/**
 * Service interface for entitlement audit log operations.
 *
 * <p>Provides async write methods for persisting audit records
 * from governance updates, rule CRUD, and rule evaluations,
 * and read methods for querying audit history.
 */
public interface EntitlementAuditLogService {

    // ===================================================================
    //  Write Methods
    // ===================================================================

    /**
     * Logs a governance metadata update for an entitlement.
     *
     * @param tenantId        the tenant identifier
     * @param entitlementId   the entitlement UUID
     * @param entitlementName the entitlement display name
     * @param resourceId      the resource UUID (nullable)
     * @param resourceName    the resource display name (nullable)
     * @param actorId         the actor's ID
     * @param actorUsername   the actor's username
     * @param description     human-readable summary
     * @param stateBefore     JSON snapshot before the update
     * @param stateAfter      JSON snapshot after the update
     */
    void logGovernanceUpdate(String tenantId, UUID entitlementId, String entitlementName,
                             UUID resourceId, String resourceName,
                             String actorId, String actorUsername,
                             String description,
                             JsonNode stateBefore, JsonNode stateAfter);

    /**
     * Logs a rule creation event against the rule's target entitlement.
     *
     * @param tenantId        the tenant identifier
     * @param entitlementId   the target entitlement UUID
     * @param entitlementName the target entitlement display name
     * @param resourceId      the resource UUID
     * @param actorId         the actor's ID
     * @param actorUsername   the actor's username
     * @param description     human-readable summary
     * @param ruleId          the created rule's UUID
     * @param ruleName        the created rule's display name
     * @param stateAfter      JSON snapshot of the rule after creation
     */
    void logRuleCreated(String tenantId, UUID entitlementId, String entitlementName,
                        UUID resourceId,
                        String actorId, String actorUsername,
                        String description,
                        UUID ruleId, String ruleName,
                        JsonNode stateAfter);

    /**
     * Logs a rule update event against the rule's target entitlement.
     *
     * @param tenantId        the tenant identifier
     * @param entitlementId   the target entitlement UUID
     * @param entitlementName the target entitlement display name
     * @param resourceId      the resource UUID
     * @param actorId         the actor's ID
     * @param actorUsername   the actor's username
     * @param description     human-readable summary
     * @param ruleId          the updated rule's UUID
     * @param ruleName        the updated rule's display name
     * @param stateBefore     JSON snapshot of changed fields before the update
     * @param stateAfter      JSON snapshot of changed fields after the update
     */
    void logRuleUpdated(String tenantId, UUID entitlementId, String entitlementName,
                        UUID resourceId,
                        String actorId, String actorUsername,
                        String description,
                        UUID ruleId, String ruleName,
                        JsonNode stateBefore, JsonNode stateAfter);

    /**
     * Logs a rule deletion event against the rule's target entitlement.
     *
     * @param tenantId        the tenant identifier
     * @param entitlementId   the target entitlement UUID
     * @param entitlementName the target entitlement display name
     * @param resourceId      the resource UUID
     * @param actorId         the actor's ID
     * @param actorUsername   the actor's username
     * @param description     human-readable summary
     * @param ruleId          the deleted rule's UUID
     * @param ruleName        the deleted rule's display name
     * @param stateBefore     JSON snapshot of the rule before deletion
     */
    void logRuleDeleted(String tenantId, UUID entitlementId, String entitlementName,
                        UUID resourceId,
                        String actorId, String actorUsername,
                        String description,
                        UUID ruleId, String ruleName,
                        JsonNode stateBefore);

    /**
     * Logs a rule evaluation result against the rule's target entitlement.
     *
     * @param tenantId           the tenant identifier
     * @param entitlementId      the target entitlement UUID
     * @param entitlementName    the target entitlement display name
     * @param resourceId         the resource UUID
     * @param actorId            the actor's ID (user or SYSTEM)
     * @param actorUsername      the actor's username
     * @param description        human-readable summary
     * @param triggerSource      what triggered the evaluation
     * @param ruleId             the evaluated rule's UUID
     * @param ruleName           the evaluated rule's display name
     * @param executionId        the execution history record UUID (nullable)
     * @param identitiesMatched  number of identities matched
     * @param assignmentsCreated number of assignments created
     * @param assignmentsRevoked number of assignments revoked
     * @param skippedNoShadow    number skipped due to missing shadow
     * @param durationMs         evaluation duration in milliseconds
     */
    void logRuleEvaluated(String tenantId, UUID entitlementId, String entitlementName,
                          UUID resourceId,
                          String actorId, String actorUsername,
                          String description, String triggerSource,
                          UUID ruleId, String ruleName, UUID executionId,
                          int identitiesMatched, int assignmentsCreated,
                          int assignmentsRevoked, int skippedNoShadow, long durationMs);

    // ===================================================================
    //  Read Methods
    // ===================================================================

    /**
     * Finds all audit records for a specific entitlement, most recent first.
     *
     * @param tenantId       the tenant identifier
     * @param entitlementId  the entitlement UUID
     * @param pageable       pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByEntitlementId(String tenantId, UUID entitlementId, Pageable pageable);

    /**
     * Finds all audit records for a specific action type within a tenant.
     *
     * @param tenantId  the tenant identifier
     * @param action    the action type (e.g. GOVERNANCE_UPDATED, RULE_EVALUATED)
     * @param pageable  pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByAction(String tenantId, String action, Pageable pageable);

    /**
     * Finds all audit records performed by a specific actor within a tenant.
     *
     * @param tenantId       the tenant identifier
     * @param actorUsername  the actor's username
     * @param pageable       pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByActor(String tenantId, String actorUsername, Pageable pageable);

    /**
     * Finds all audit records for a specific rule within a tenant.
     *
     * @param tenantId  the tenant identifier
     * @param ruleId    the rule UUID
     * @param pageable  pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByRuleId(String tenantId, UUID ruleId, Pageable pageable);

    /**
     * Finds all audit records within a time range for a tenant.
     *
     * @param tenantId  the tenant identifier
     * @param from      start time (epoch millis, inclusive)
     * @param to        end time (epoch millis, inclusive)
     * @param pageable  pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByDateRange(String tenantId, Long from, Long to, Pageable pageable);

    /**
     * Finds all audit records for a specific entitlement and action type.
     *
     * @param tenantId       the tenant identifier
     * @param entitlementId  the entitlement UUID
     * @param action         the action type
     * @param pageable       pagination and sorting
     * @return a page of audit log entries
     */
    Page<EntitlementAuditLog> findByEntitlementIdAndAction(String tenantId, UUID entitlementId, String action, Pageable pageable);

        /**
     * Finds all audit records for a specific entitlement and maps to UI DTOs.
     *
     * @param tenantId       the tenant identifier
     * @param entitlementId  the entitlement UUID
     * @param action         optional action filter (nullable)
     * @param pageable       pagination and sorting
     * @return a page of UI-friendly audit history DTOs
     */
    Page<EntitlementAuditHistoryDto> findAuditHistory(String tenantId, UUID entitlementId, String action, Pageable pageable);
}