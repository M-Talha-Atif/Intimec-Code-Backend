package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.ParallelExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ParallelExecution entity.
 * 
 * Provides database operations for parallel workflow branch synchronization.
 * Uses pessimistic locking to prevent race conditions when multiple branches
 * complete simultaneously and try to increment the counter.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Repository
public interface ParallelExecutionRepository extends JpaRepository<ParallelExecution, UUID> {

    /**
     * Find parallel execution record by execution ID and join node ID.
     * Uses pessimistic write lock to prevent race conditions when multiple
     * branches complete at the same time.
     * 
     * @param executionId The workflow execution ID
     * @param joinNodeId The parallel-join node ID
     * @return Optional containing the parallel execution record
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParallelExecution p WHERE p.executionId = :executionId AND p.joinNodeId = :joinNodeId")
    Optional<ParallelExecution> findByExecutionIdAndJoinNodeIdWithLock(
        @Param("executionId") UUID executionId, 
        @Param("joinNodeId") String joinNodeId
    );

    /**
     * Find parallel execution record by execution ID and join node ID (no lock).
     * Use this for read-only operations that don't modify the record.
     * 
     * @param executionId The workflow execution ID
     * @param joinNodeId The parallel-join node ID
     * @return Optional containing the parallel execution record
     */
    Optional<ParallelExecution> findByExecutionIdAndJoinNodeId(UUID executionId, String joinNodeId);

    /**
     * Find parallel execution record by execution ID and split node ID.
     * 
     * @param executionId The workflow execution ID
     * @param splitNodeId The parallel-split node ID
     * @return Optional containing the parallel execution record
     */
    Optional<ParallelExecution> findByExecutionIdAndSplitNodeId(UUID executionId, String splitNodeId);
}
