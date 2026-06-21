package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DynamicRule entity operations.
 * 
 * <p>This repository provides data access methods for managing dynamic rules
 * that control role assignment behavior, including activation status,
 * notification settings, and approval requirements.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface DynamicRuleRepository extends JpaRepository<DynamicRule, UUID> {
    
    /**
     * Find all dynamic rules associated with a specific role.
     * 
     * @param roleId the UUID of the role
     * @return list of dynamic rules for the role
     */
    List<DynamicRule> findByRoleId(UUID roleId);
    
    /**
     * Check if a dynamic rule exists for the given role.
     * 
     * @param roleId the UUID of the role
     * @return true if a dynamic rule exists for this role
     */
    boolean existsByRoleId(UUID roleId);
    
    /**
     * Find all active dynamic rules for a specific role.
     * 
     * @param roleId the UUID of the role
     * @param ruleIsActive the active status (true for active rules)
     * @return list of active dynamic rules for the role
     */
    List<DynamicRule> findByRoleIdAndRuleIsActive(UUID roleId, Boolean ruleIsActive);
    
    /**
     * Find all active dynamic rules across all roles.
     * 
     * @param ruleIsActive the active status (true for active rules)
     * @return list of all active dynamic rules
     */
    List<DynamicRule> findByRuleIsActive(Boolean ruleIsActive);
    
    /**
     * Find dynamic rules that require approval for a specific role.
     * 
     * @param roleId the UUID of the role
     * @param requireApproval true to find rules requiring approval
     * @return list of dynamic rules requiring approval
     */
    List<DynamicRule> findByRoleIdAndRequireApprovalBeforeAssignment(UUID roleId, Boolean requireApproval);
    
    /**
     * Find dynamic rules that send notifications for a specific role.
     * 
     * @param roleId the UUID of the role
     * @param sendNotification true to find rules that send notifications
     * @return list of dynamic rules that send notifications
     */
    List<DynamicRule> findByRoleIdAndSendNotificationOnAssignment(UUID roleId, Boolean sendNotification);
    
    /**
     * Check if there are any active dynamic rules for a specific role.
     * 
     * @param roleId the UUID of the role
     * @return true if active rules exist for the role
     */
    @Query("SELECT CASE WHEN COUNT(dr) > 0 THEN true ELSE false END FROM DynamicRule dr WHERE dr.roleId = :roleId AND dr.ruleIsActive = true")
    boolean existsActiveRulesByRoleId(@Param("roleId") UUID roleId);
    
    /**
     * Count active dynamic rules for a specific role.
     * 
     * @param roleId the UUID of the role
     * @return count of active rules for the role
     */
    @Query("SELECT COUNT(dr) FROM DynamicRule dr WHERE dr.roleId = :roleId AND dr.ruleIsActive = true")
    long countActiveRulesByRoleId(@Param("roleId") UUID roleId);
    
    /**
     * Find dynamic rules by schedule type.
     * 
     * @param scheduleType the schedule type to filter by
     * @return list of dynamic rules with the specified schedule type
     */
    List<DynamicRule> findByScheduleType(ScheduleType scheduleType);
    
    /**
     * Find active dynamic rules by schedule type.
     * 
     * @param scheduleType the schedule type to filter by
     * @param ruleIsActive the active status (true for active rules)
     * @return list of active dynamic rules with the specified schedule type
     */
    List<DynamicRule> findByScheduleTypeAndRuleIsActive(ScheduleType scheduleType, Boolean ruleIsActive);
    
    /**
     * Find dynamic rules for a role by schedule type.
     * 
     * @param roleId the UUID of the role
     * @param scheduleType the schedule type to filter by
     * @return list of dynamic rules for the role with the specified schedule type
     */
    List<DynamicRule> findByRoleIdAndScheduleType(UUID roleId, ScheduleType scheduleType);
    
    /**
     * Find all dynamic rules with schedule type not equal to the specified one.
     * 
     * @param scheduleType the schedule type to exclude
     * @return list of dynamic rules excluding the specified schedule type
     */
    List<DynamicRule> findByScheduleTypeNot(ScheduleType scheduleType);
    
    /**
     * Find all scheduled rules (non-immediate) that are active.
     * 
     * @return list of active scheduled dynamic rules (DAILY, WEEKLY, MONTHLY)
     */
    @Query("SELECT dr FROM DynamicRule dr WHERE dr.scheduleType != 'IMMEDIATE' AND dr.ruleIsActive = true")
    List<DynamicRule> findAllActiveScheduledRules();
    
    /**
     * Find all IMMEDIATE rules that are active for continuous reconciliation.
     * 
     * <p>IMMEDIATE rules are executed every minute to provide near real-time
     * role assignment based on user attribute changes.</p>
     * 
     * @return list of active IMMEDIATE dynamic rules
     */
    @Query("SELECT dr FROM DynamicRule dr WHERE dr.scheduleType = 'IMMEDIATE' AND dr.ruleIsActive = true")
    List<DynamicRule> findAllActiveImmediateRules();
}