-- Migration: Add REVOKE_INTERNAL_ROLE operation type support
-- Description: Updates existing tables to accept the new REVOKE_INTERNAL_ROLE enum value
-- Author: aidentx Development Team
-- Date: 2025-11-21

-- Update notifications table
-- Drop the existing check constraint
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_operation_type_check;

-- Add the new check constraint with REVOKE_INTERNAL_ROLE included
ALTER TABLE notifications ADD CONSTRAINT notifications_operation_type_check 
CHECK (operation_type IN ('PASSWORD_RESET', 'ACCESS_REQUEST_APPROVED', 'PASSWORD_REMINDER_RESET', 'ASSIGN_INTERNAL_ROLE', 'REVOKE_INTERNAL_ROLE'));

-- Update template_managers table
-- Drop the existing check constraint
ALTER TABLE template_managers DROP CONSTRAINT IF EXISTS template_managers_trigger_type_check;

-- Add the new check constraint with REVOKE_INTERNAL_ROLE included
ALTER TABLE template_managers ADD CONSTRAINT template_managers_trigger_type_check 
CHECK (trigger_type IN ('PASSWORD_RESET', 'ACCESS_REQUEST_APPROVED', 'PASSWORD_REMINDER_RESET', 'ASSIGN_INTERNAL_ROLE', 'REVOKE_INTERNAL_ROLE'));

-- Update template_manager_versions table
-- Drop the existing check constraint
ALTER TABLE template_manager_versions DROP CONSTRAINT IF EXISTS template_manager_versions_trigger_type_check;

-- Add the new check constraint with REVOKE_INTERNAL_ROLE included
ALTER TABLE template_manager_versions ADD CONSTRAINT template_manager_versions_trigger_type_check 
CHECK (trigger_type IN ('PASSWORD_RESET', 'ACCESS_REQUEST_APPROVED', 'PASSWORD_REMINDER_RESET', 'ASSIGN_INTERNAL_ROLE', 'REVOKE_INTERNAL_ROLE'));

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Migration V1.0.24 completed: REVOKE_INTERNAL_ROLE operation type support added to all tables';
END $$;
