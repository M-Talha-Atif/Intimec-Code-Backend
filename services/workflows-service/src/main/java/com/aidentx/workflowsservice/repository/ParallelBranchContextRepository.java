package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.ParallelBranchContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Phase 6b: Repository for parallel branch context storage
 * Manages individual branch execution contexts during parallel workflow execution
 */
@Repository
public interface ParallelBranchContextRepository extends JpaRepository<ParallelBranchContext, UUID> {
    
    /**
     * Find all branch contexts for a specific execution and join node
     * Used to retrieve all branch outputs for context merging
     */
    @Query("SELECT pbc FROM ParallelBranchContext pbc " +
           "WHERE pbc.executionId = :executionId " +
           "AND pbc.joinNodeId = :joinNodeId " +
           "ORDER BY pbc.branchIndex ASC")
    List<ParallelBranchContext> findByExecutionIdAndJoinNodeId(
        @Param("executionId") UUID executionId,
        @Param("joinNodeId") String joinNodeId
    );
    
    /**
     * Count completed branches for a specific execution and join node
     * Used to verify all branches have reached the join
     */
    @Query("SELECT COUNT(pbc) FROM ParallelBranchContext pbc " +
           "WHERE pbc.executionId = :executionId " +
           "AND pbc.joinNodeId = :joinNodeId " +
           "AND pbc.isComplete = true")
    long countCompletedBranches(
        @Param("executionId") UUID executionId,
        @Param("joinNodeId") String joinNodeId
    );
    
    /**
     * Find all branch contexts for a specific execution and split node
     * Used for debugging and tracking branch creation
     */
    List<ParallelBranchContext> findByExecutionIdAndSplitNodeId(UUID executionId, String splitNodeId);
    
    /**
     * Delete all branch contexts for a specific execution
     * Called when workflow execution completes or is cancelled
     */
    void deleteByExecutionId(UUID executionId);
}
