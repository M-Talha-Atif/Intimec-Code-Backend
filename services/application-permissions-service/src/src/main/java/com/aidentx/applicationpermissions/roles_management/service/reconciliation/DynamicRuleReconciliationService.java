package com.aidentx.applicationpermissions.roles_management.service.reconciliation;

import java.util.UUID;

/**
 * Service interface for dynamic rule reconciliation operations.
 * 
 * <p>Provides methods for reconciling user role assignments based on dynamic rule conditions.
 * This service is used by both scheduled (cron-based) reconciliation and event-driven (CDC-based)
 * reconciliation.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface DynamicRuleReconciliationService {
    
    /**
     * Reconciles a specific user's role assignments with all active IMMEDIATE dynamic rules.
     * This method is called by the CDC listener when a user's data changes.
     * 
     * <p>Only evaluates rules with IMMEDIATE schedule type. Other schedules (DAILY/WEEKLY/MONTHLY)
     * are handled by the scheduled reconciliation cron job.</p>
     * 
     * @param userId the ID of the user to reconcile
     * @param tenantId the tenant ID (String)
     * @throws IllegalArgumentException if the user doesn't exist or doesn't belong to the tenant
     */
    void reconcileUserWithImmediateRules(UUID userId, String tenantId);
    
    /**
     * Reconciles all users in a tenant with all active dynamic rules (all schedules).
     * This method is called by scheduled reconciliation for DAILY/WEEKLY/MONTHLY rules.
     * 
     * @param tenantId the tenant ID
     */
    void reconcileAllUsersForTenant(UUID tenantId);
}
