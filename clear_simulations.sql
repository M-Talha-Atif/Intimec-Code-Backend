-- Clear all simulation and execution data to allow fresh simulations
-- This preserves workflows and templates, only deletes execution history

-- Delete workflow approvals (if any exist from simulations)
DELETE FROM workflow_approvals WHERE execution_id IN (
    SELECT id FROM workflow_executions WHERE simulation_mode = true
);

-- Delete workflow node executions (simulation execution steps)
DELETE FROM workflow_node_executions WHERE execution_id IN (
    SELECT id FROM workflow_executions WHERE simulation_mode = true
);

-- Delete workflow executions (simulation runs)
DELETE FROM workflow_executions WHERE simulation_mode = true;

-- Delete workflow simulations (simulation metadata)
DELETE FROM workflow_simulations;

-- Show remaining data
SELECT 'Workflows remaining:' as info, COUNT(*) as count FROM workflows
UNION ALL
SELECT 'Executions remaining:', COUNT(*) FROM workflow_executions
UNION ALL
SELECT 'Node executions remaining:', COUNT(*) FROM workflow_node_executions
UNION ALL
SELECT 'Simulations remaining:', COUNT(*) FROM workflow_simulations;
