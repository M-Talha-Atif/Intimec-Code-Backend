-- Phase 6b/7: Individual branch context storage for parallel execution
-- Stores execution context from each parallel branch for merging at join

CREATE TABLE IF NOT EXISTS parallel_branch_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID NOT NULL,
    split_node_id VARCHAR(100) NOT NULL,
    join_node_id VARCHAR(100) NOT NULL,
    branch_index INTEGER NOT NULL CHECK (branch_index >= 0),
    branch_context JSONB,
    is_complete BOOLEAN NOT NULL DEFAULT FALSE,
    is_successful BOOLEAN,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    
    CONSTRAINT fk_parallel_branch_execution 
        FOREIGN KEY (execution_id) 
        REFERENCES workflow_executions(id) 
        ON DELETE CASCADE,
        
    CONSTRAINT uq_branch_per_execution 
        UNIQUE (execution_id, split_node_id, branch_index)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_parallel_branch_execution 
    ON parallel_branch_contexts(execution_id, branch_index);
    
CREATE INDEX IF NOT EXISTS idx_parallel_branch_join 
    ON parallel_branch_contexts(join_node_id);

-- GIN index for JSONB queries
CREATE INDEX IF NOT EXISTS idx_parallel_branch_context_gin 
    ON parallel_branch_contexts USING GIN (branch_context);

-- Comments for documentation
COMMENT ON TABLE parallel_branch_contexts IS 
    'Phase 6b: Stores execution context for individual parallel branches. Contexts are merged at join node to create unified workflow state.';
COMMENT ON COLUMN parallel_branch_contexts.branch_context IS 
    'JSONB context data specific to this branch execution. Contains branch-specific variables and outputs.';
COMMENT ON COLUMN parallel_branch_contexts.branch_index IS 
    'Zero-based index of this branch (0 to totalBranches-1)';
COMMENT ON COLUMN parallel_branch_contexts.is_successful IS 
    'Phase 7: Whether branch executed successfully (true) or failed (false). null = not yet complete.';
COMMENT ON COLUMN parallel_branch_contexts.error_message IS 
    'Phase 7: Error message if branch failed. Used for failure policy evaluation (FAIL_FAST, FAIL_SAFE, THRESHOLD).';
