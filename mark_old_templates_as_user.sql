-- Mark old manually-created templates as user templates (not system templates)
-- These templates were created before the SystemTemplateInitializer was implemented
-- and should not be shown as system templates

UPDATE workflow_templates
SET is_system_template = false
WHERE name IN (
    'Access Request',
    'Employee Onboarding',
    'Employee Termination',
    'Role Change'
)
AND created_at < '2026-01-15 00:00:00+00';

-- Verify the update
SELECT id, name, is_system_template, category, created_at 
FROM workflow_templates 
WHERE name IN (
    'Access Request',
    'Employee Onboarding',
    'Employee Termination',
    'Role Change'
)
ORDER BY name;
