-- Delete old manually-created templates that are no longer needed
-- These templates were created before the SystemTemplateInitializer was implemented

DELETE FROM workflow_templates
WHERE name IN (
    'Access Request',
    'Employee Onboarding',
    'Employee Termination',
    'Role Change'
)
AND created_at < '2026-01-15 00:00:00+00';

-- Verify deletion
SELECT COUNT(*) as remaining_templates,
       COUNT(*) FILTER (WHERE is_system_template = true) as system_templates,
       COUNT(*) FILTER (WHERE is_system_template = false) as user_templates
FROM workflow_templates;

-- Show remaining templates
SELECT name, is_system_template, category, created_at 
FROM workflow_templates 
ORDER BY is_system_template DESC, name;
