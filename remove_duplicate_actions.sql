-- Remove duplicate View actions, keeping the one with permissions assigned
-- This script identifies duplicate View actions for each page and deletes extras

DO $$
DECLARE
    page_record RECORD;
    action_record RECORD;
    actions_to_keep UUID[];
    action_to_delete UUID;
    deleted_count INT := 0;
BEGIN
    -- For each page that has duplicate View actions
    FOR page_record IN 
        SELECT p.id as page_id, p.page_name, p.page_path, COUNT(a.id) as action_count
        FROM page p
        JOIN action a ON a.page_id = p.id
        WHERE a.action_name = 'View'
        GROUP BY p.id, p.page_name, p.page_path
        HAVING COUNT(a.id) > 1
    LOOP
        RAISE NOTICE 'Processing page: % (%) - % duplicate View actions', 
            page_record.page_name, page_record.page_path, page_record.action_count;
        
        actions_to_keep := ARRAY[]::UUID[];
        
        -- Get all View actions for this page, ordered by permission count (desc)
        FOR action_record IN
            SELECT a.id, a.action_name, a.endpoint, a.method,
                   COUNT(DISTINCT p.id) as permission_count,
                   COUNT(DISTINCT rp.id) as role_permission_count
            FROM action a
            LEFT JOIN permission p ON p.action_id = a.id
            LEFT JOIN role_permission rp ON rp.permission_id = p.id
            WHERE a.page_id = page_record.page_id 
              AND a.action_name = 'View'
            GROUP BY a.id, a.action_name, a.endpoint, a.method
            ORDER BY COUNT(DISTINCT rp.id) DESC, a.id
        LOOP
            -- Keep the first action (the one with most role_permissions)
            IF array_length(actions_to_keep, 1) IS NULL THEN
                actions_to_keep := array_append(actions_to_keep, action_record.id);
                RAISE NOTICE '  Keeping action % (permissions: %, role_permissions: %)', 
                    action_record.id, action_record.permission_count, action_record.role_permission_count;
            ELSE
                -- Delete this duplicate action
                -- First, need to delete role_permissions and permissions referencing it
                DELETE FROM role_permission 
                WHERE permission_id IN (
                    SELECT id FROM permission WHERE action_id = action_record.id
                );
                
                DELETE FROM permission WHERE action_id = action_record.id;
                DELETE FROM action WHERE id = action_record.id;
                
                deleted_count := deleted_count + 1;
                RAISE NOTICE '  Deleted duplicate action % (had permissions: %, role_permissions: %)', 
                    action_record.id, action_record.permission_count, action_record.role_permission_count;
            END IF;
        END LOOP;
    END LOOP;
    
    RAISE NOTICE 'Cleanup complete. Deleted % duplicate View actions.', deleted_count;
END $$;

-- Verify the cleanup
SELECT p.page_name, p.page_path, COUNT(a.id) as view_action_count
FROM page p
JOIN action a ON a.page_id = p.id
WHERE a.action_name = 'View'
GROUP BY p.id, p.page_name, p.page_path
HAVING COUNT(a.id) > 1
ORDER BY p.page_name;
