-- Reset permission system to force bootstrap to recreate with correct relationships
DELETE FROM role_permission;
DELETE FROM user_role WHERE role_id IN (SELECT id FROM role WHERE role_type != 'CUSTOM');
DELETE FROM action;
DELETE FROM permission;
DELETE FROM page;
-- Don't delete modules or roles, just the pages/actions/permissions
