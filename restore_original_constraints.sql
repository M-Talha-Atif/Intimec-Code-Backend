-- Restore original constraints (without workflow types)
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_operation_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_operation_type_check 
CHECK (operation_type IN ('PASSWORD_RESET', 'ACCESS_REQUEST_APPROVED', 'PASSWORD_REMINDER_RESET', 'ASSIGN_INTERNAL_ROLE', 'REVOKE_INTERNAL_ROLE'));

ALTER TABLE template_managers DROP CONSTRAINT IF EXISTS template_managers_trigger_type_check;
ALTER TABLE template_managers ADD CONSTRAINT template_managers_trigger_type_check 
CHECK (trigger_type IN ('PASSWORD_RESET', 'ACCESS_REQUEST_APPROVED', 'PASSWORD_REMINDER_RESET', 'ASSIGN_INTERNAL_ROLE', 'REVOKE_INTERNAL_ROLE'));

ALTER TABLE template_manager_versions DROP CONSTRAINT IF EXISTS template_manager_versions_trigger_type_check;
ALTER TABLE template_manager_versions ADD CONSTRAINT template_manager_versions_trigger_type_check 
CHECK (trigger_type IN ('PASSWORD_RESET', 'ACCESS_REQUEST_APPROVED', 'PASSWORD_REMINDER_RESET', 'ASSIGN_INTERNAL_ROLE', 'REVOKE_INTERNAL_ROLE'));
