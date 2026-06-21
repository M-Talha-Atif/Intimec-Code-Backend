-- Migration: Create trigger for user change notifications
-- Purpose: Notify application when users table changes to trigger dynamic rule reconciliation
-- This enables event-driven reconciliation instead of polling every minute

-- Create notification function that will be called by trigger
CREATE OR REPLACE FUNCTION notify_user_changes()
RETURNS trigger AS $$
DECLARE
    notification JSON;
BEGIN
    -- Build notification payload with operation type and affected user
    notification = json_build_object(
        'operation', TG_OP,
        'table', TG_TABLE_NAME,
        'userId', CASE 
            WHEN TG_OP = 'DELETE' THEN OLD.id
            ELSE NEW.id
        END,
        'tenantId', CASE 
            WHEN TG_OP = 'DELETE' THEN OLD.tenant_id
            ELSE NEW.tenant_id
        END,
        'timestamp', extract(epoch from now()) * 1000
    );
    
    -- Send notification to 'user_changes' channel
    PERFORM pg_notify('user_changes', notification::text);
    
    -- Return appropriate row based on operation
    IF (TG_OP = 'DELETE') THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on users table for INSERT, UPDATE, DELETE
DROP TRIGGER IF EXISTS user_changes_trigger ON users;
CREATE TRIGGER user_changes_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW
    EXECUTE FUNCTION notify_user_changes();

-- Add comment explaining the trigger
COMMENT ON TRIGGER user_changes_trigger ON users IS 
'Trigger that notifies application of user changes for event-driven dynamic rule reconciliation';

COMMENT ON FUNCTION notify_user_changes() IS 
'Function that sends PostgreSQL notification when users table changes (INSERT/UPDATE/DELETE)';
