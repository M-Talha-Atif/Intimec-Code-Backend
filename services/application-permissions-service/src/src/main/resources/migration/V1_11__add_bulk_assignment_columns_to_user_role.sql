-- Migration: Add bulk assignment columns to user_role table
-- Description: Adds columns for tracking bulk role assignments
-- Author: AidentX Development Team
-- Version: 1.11

-- Add assigned_at column for tracking when the role was assigned
ALTER TABLE user_role
ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add batch_id column for tracking bulk assignments (NULL for single assignments)
ALTER TABLE user_role
ADD COLUMN IF NOT EXISTS batch_id UUID;

-- Add validity_days column for temporary access grants (NULL = permanent)
ALTER TABLE user_role
ADD COLUMN IF NOT EXISTS validity_days INTEGER;

-- Create index on batch_id for efficient bulk operation queries
CREATE INDEX IF NOT EXISTS idx_user_role_batch_id ON user_role(batch_id);

-- Comments for documentation
COMMENT ON COLUMN user_role.assigned_at IS 'Timestamp when this role was assigned';
COMMENT ON COLUMN user_role.batch_id IS 'Reference to bulk assignment batch job (NULL for single assignments)';
COMMENT ON COLUMN user_role.validity_days IS 'Number of days this assignment is valid (NULL = permanent)';
