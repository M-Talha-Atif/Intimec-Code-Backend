-- Phase 6b/7: Parallel execution tracking for barrier synchronization
-- This table tracks the state of parallel gateway execution to ensure all branches complete before continuing

CREATE TABLE IF NOT EXISTS parallel_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID NOT NULL,
    split_node_id VARCHAR(100) NOT NULL,
    join_node_id VARCHAR(100) NOT NULL,
    total_branches INTEGER NOT NULL CHECK (total_branches > 0),
    completed_branches INTEGER NOT NULL DEFAULT 0 CHECK (completed_branches >= 0 AND completed_branches <= total_branches),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_parallel_exec_execution 
        FOREIGN KEY (execution_id) 
        REFERENCES workflow_executions(id) 
        ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_parallel_exec_execution_join 
    ON parallel_executions(execution_id, join_node_id);
    
CREATE INDEX IF NOT EXISTS idx_parallel_exec_execution_split 
    ON parallel_executions(execution_id, split_node_id);

-- Comments for documentation
COMMENT ON TABLE parallel_executions IS 
    'Phase 6b: Tracks parallel gateway execution state for barrier synchronization. Ensures all parallel branches complete before workflow continues.';
COMMENT ON COLUMN parallel_executions.total_branches IS 
    'Total number of parallel branches created at split node';
COMMENT ON COLUMN parallel_executions.completed_branches IS 
    'Number of branches that have reached the join node (atomic counter protected by pessimistic locking)';
COMMENT ON COLUMN parallel_executions.split_node_id IS 
    'The parallel-split node ID that created the branches';
COMMENT ON COLUMN parallel_executions.join_node_id IS 
    'The parallel-join node ID where branches merge';
