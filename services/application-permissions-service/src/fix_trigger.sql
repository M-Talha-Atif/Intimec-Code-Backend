-- Show all triggers on users table first
SELECT tgname, pg_get_triggerdef(oid) FROM pg_trigger WHERE tgrelid = 'users'::regclass;

-- Drop ALL triggers on users table
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tgname FROM pg_trigger WHERE tgrelid = 'users'::regclass) 
    LOOP
        EXECUTE 'DROP TRIGGER IF EXISTS ' || r.tgname || ' ON users CASCADE';
    END LOOP;
END $$;

-- Drop all versions of the function
DROP FUNCTION IF EXISTS notify_user_changes() CASCADE;
DROP FUNCTION IF EXISTS notify_user_changes(text) CASCADE;

-- Drop the notification table if it exists
DROP TABLE IF EXISTS user_change_notifications CASCADE;

-- Create the correct notification function that uses pg_notify directly
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
    
    -- Send notification directly using pg_notify (NO TABLE!)
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
CREATE TRIGGER user_changes_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW
    EXECUTE FUNCTION notify_user_changes();

-- Verify the trigger was created
SELECT tgname, pg_get_triggerdef(oid) FROM pg_trigger WHERE tgrelid = 'users'::regclass;
