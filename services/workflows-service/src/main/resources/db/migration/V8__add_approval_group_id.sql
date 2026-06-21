-- Phase 7: Multi-approval group tracking
-- Adds approval_group_id to track approvals that must ALL be approved

ALTER TABLE workflow_approvals 
    ADD COLUMN IF NOT EXISTS approval_group_id UUID;

-- Index for group queries
CREATE INDEX IF NOT EXISTS idx_approval_group 
    ON workflow_approvals(approval_group_id) 
    WHERE approval_group_id IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN workflow_approvals.approval_group_id IS 
    'Phase 7: Groups multiple approvals that must all be approved (multi-approval pattern). Workflow resumes only when ALL approvals in group are approved.';
