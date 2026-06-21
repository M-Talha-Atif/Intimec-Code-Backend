package com.aidentx.core.notifications_management.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bootstrap component for updating notification table constraints.
 * 
 * <p>This component runs on application startup and ensures the notifications table
 * has the correct CHECK constraint for operation_type that includes workflow types.</p>
 * 
 * <p>This is needed because Hibernate's ddl-auto=update doesn't update existing
 * CHECK constraints when new enum values are added.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 2026-01-15
 */
@Component
public class NotificationConstraintBootstrap {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationConstraintBootstrap.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public NotificationConstraintBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Updates notification table constraints on application startup.
     * Ensures the operation_type constraint includes workflow notification types.
     */
    @PostConstruct
    public void updateNotificationConstraints() {
        logger.info("========================================");
        logger.info("UPDATING NOTIFICATION TABLE CONSTRAINTS");
        logger.info("========================================");
        
        try {
            // Check if constraint needs updating
            if (constraintNeedsUpdate("notifications", "notifications_operation_type_check", "DELEGATION_CREATED")) {
                logger.info("Constraint is missing delegation/escalation types, updating...");
                
                // Drop existing constraint
                logger.info("Dropping existing operation_type constraint...");
                jdbcTemplate.execute(
                    "ALTER TABLE notifications DROP CONSTRAINT notifications_operation_type_check"
                );
                
                // Add updated constraint with all operation types including workflow types
                logger.info("Adding updated operation_type constraint with workflow types...");
                jdbcTemplate.execute(
                    "ALTER TABLE notifications ADD CONSTRAINT notifications_operation_type_check " +
                    "CHECK (operation_type IN (" +
                    "    'PASSWORD_RESET', " +
                    "    'ACCESS_REQUEST_APPROVED', " +
                    "    'PASSWORD_REMINDER_RESET', " +
                    "    'ASSIGN_INTERNAL_ROLE', " +
                    "    'REVOKE_INTERNAL_ROLE', " +
                    "    'WORKFLOW_APPROVAL_REQUESTED', " +
                    "    'WORKFLOW_COMPLETED', " +
                    "    'WORKFLOW_APPROVAL_APPROVED', " +
                    "    'WORKFLOW_APPROVAL_REJECTED', " +
                    "    'DELEGATION_CREATED', " +
                    "    'APPROVAL_AUTO_DELEGATED', " +
                    "    'APPROVAL_ESCALATED', " +
                    "    'APPROVAL_ESCALATED_FROM_YOU'" +
                    "))"
                );
                
                logger.info("✓ Successfully updated notifications constraint");
            } else {
                logger.info("✓ Notifications constraint already includes workflow types, skipping update");
            }
            
            // Update template_managers constraint if table exists
            updateTemplateManagersConstraint();
            
            // Update template_manager_versions constraint if table exists
            updateTemplateManagerVersionsConstraint();
            
            logger.info("========================================");
            logger.info("NOTIFICATION CONSTRAINTS UPDATE COMPLETED");
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Failed to update notification constraints", e);
            // Don't fail the application startup if constraint update fails
            logger.warn("Continuing with application startup despite constraint update failure");
        }
    }
    
    private void updateTemplateManagersConstraint() {
        try {
            if (constraintNeedsUpdate("template_managers", "template_managers_trigger_type_check", "DELEGATION_CREATED")) {
                logger.info("Updating template_managers trigger_type constraint...");
                jdbcTemplate.execute(
                    "ALTER TABLE template_managers DROP CONSTRAINT template_managers_trigger_type_check"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE template_managers ADD CONSTRAINT template_managers_trigger_type_check " +
                    "CHECK (trigger_type IN (" +
                    "    'PASSWORD_RESET', " +
                    "    'ACCESS_REQUEST_APPROVED', " +
                    "    'PASSWORD_REMINDER_RESET', " +
                    "    'ASSIGN_INTERNAL_ROLE', " +
                    "    'REVOKE_INTERNAL_ROLE', " +
                    "    'WORKFLOW_APPROVAL_REQUESTED', " +
                    "    'WORKFLOW_COMPLETED', " +
                    "    'WORKFLOW_APPROVAL_APPROVED', " +
                    "    'WORKFLOW_APPROVAL_REJECTED', " +
                    "    'DELEGATION_CREATED', " +
                    "    'APPROVAL_AUTO_DELEGATED', " +
                    "    'APPROVAL_ESCALATED', " +
                    "    'APPROVAL_ESCALATED_FROM_YOU'" +
                    "))"
                );
                logger.info("✓ Successfully updated template_managers constraint");
            } else {
                logger.info("✓ Template managers constraint already up to date");
            }
        } catch (Exception e) {
            logger.debug("Skipping template_managers constraint update (table may not exist): {}", e.getMessage());
        }
    }
    
    private void updateTemplateManagerVersionsConstraint() {
        try {
            if (constraintNeedsUpdate("template_manager_versions", "template_manager_versions_trigger_type_check", "DELEGATION_CREATED")) {
                logger.info("Updating template_manager_versions trigger_type constraint...");
                jdbcTemplate.execute(
                    "ALTER TABLE template_manager_versions DROP CONSTRAINT template_manager_versions_trigger_type_check"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE template_manager_versions ADD CONSTRAINT template_manager_versions_trigger_type_check " +
                    "CHECK (trigger_type IN (" +
                    "    'PASSWORD_RESET', " +
                    "    'ACCESS_REQUEST_APPROVED', " +
                    "    'PASSWORD_REMINDER_RESET', " +
                    "    'ASSIGN_INTERNAL_ROLE', " +
                    "    'REVOKE_INTERNAL_ROLE', " +
                    "    'WORKFLOW_APPROVAL_REQUESTED', " +
                    "    'WORKFLOW_COMPLETED', " +
                    "    'WORKFLOW_APPROVAL_APPROVED', " +
                    "    'WORKFLOW_APPROVAL_REJECTED', " +
                    "    'DELEGATION_CREATED', " +
                    "    'APPROVAL_AUTO_DELEGATED', " +
                    "    'APPROVAL_ESCALATED', " +
                    "    'APPROVAL_ESCALATED_FROM_YOU'" +
                    "))"
                );
                logger.info("✓ Successfully updated template_manager_versions constraint");
            } else {
                logger.info("✓ Template manager versions constraint already up to date");
            }
        } catch (Exception e) {
            logger.debug("Skipping template_manager_versions constraint update (table may not exist): {}", e.getMessage());
        }
    }
    
    /**
     * Checks if a constraint needs to be updated by testing if it accepts a workflow operation type.
     * 
     * @param tableName the name of the table
     * @param constraintName the name of the constraint
     * @param testValue a delegation/escalation operation type to test (e.g., 'DELEGATION_CREATED')
     * @return true if constraint needs update (doesn't include workflow types), false otherwise
     */
    private boolean constraintNeedsUpdate(String tableName, String constraintName, String testValue) {
        try {
            // Query the constraint definition from PostgreSQL system catalog
            String query = "SELECT pg_get_constraintdef(oid) as constraint_def " +
                          "FROM pg_constraint " +
                          "WHERE conname = ? AND conrelid = ?::regclass";
            
            String constraintDef = jdbcTemplate.queryForObject(
                query,
                String.class,
                constraintName,
                tableName
            );
            
            // Check if the constraint definition includes the test value (workflow type)
            if (constraintDef != null && constraintDef.contains(testValue)) {
                return false; // Already includes workflow types
            }
            
            return true; // Needs update
            
        } catch (Exception e) {
            logger.warn("Could not check constraint {}, will attempt update: {}", constraintName, e.getMessage());
            return true; // Assume it needs update if we can't check
        }
    }
}
