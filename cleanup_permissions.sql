-- Clean up permission bootstrap data to allow fresh initialization
-- Run this to fix duplicate pages issue

-- Delete in correct order (respecting foreign keys)
DELETE FROM role_permission WHERE permission_id IN (SELECT id FROM permission);
DELETE FROM permission WHERE action_id IN (SELECT id FROM action);
DELETE FROM action WHERE page_id IN (SELECT id FROM page);
DELETE FROM page WHERE module_id IN (SELECT id FROM module_table);
DELETE FROM module_table;

-- Verify cleanup
SELECT 'Modules' as table_name, COUNT(*) as count FROM module_table
UNION ALL
SELECT 'Pages', COUNT(*) FROM page
UNION ALL
SELECT 'Actions', COUNT(*) FROM action
UNION ALL
SELECT 'Permissions', COUNT(*) FROM permission
UNION ALL
SELECT 'Role-Permissions', COUNT(*) FROM role_permission;
