-- Migration: Add assignment_type column to user_role table
-- Description: Adds assignment_type enum field to track whether role assignments are MANUAL or DYNAMIC
-- Author: AidentX Development Team
-- Version: 1.10

-- Add assignment_type column with default value MANUAL for existing records
ALTER TABLE user_role
ADD COLUMN assignment_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- Create index on (role_id, assignment_type) for efficient querying
-- This index optimizes queries that check if a role has manual assignments or dynamic rules
CREATE INDEX idx_user_role_assignment_type ON user_role(role_id, assignment_type);

-- Add check constraint to ensure only valid assignment types are stored
ALTER TABLE user_role
ADD CONSTRAINT chk_user_role_assignment_type CHECK (assignment_type IN ('MANUAL', 'DYNAMIC'));

-- Comments for documentation
COMMENT ON COLUMN user_role.assignment_type IS 'Type of assignment: MANUAL (direct admin assignment) or DYNAMIC (automatic via rules)';
COMMENT ON INDEX idx_user_role_assignment_type IS 'Index for efficient lookup of assignment types by role';
