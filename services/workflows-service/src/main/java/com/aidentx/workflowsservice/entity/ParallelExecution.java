package com.aidentx.workflowsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity to track parallel workflow branch execution state.
 * 
 * When a workflow reaches a parallel-split gateway, multiple branches execute concurrently.
 * This entity tracks how many branches were created and how many have completed,
 * enabling the parallel-join gateway to wait until all branches finish before continuing.
 * 
 * Design Pattern: Barrier Synchronization
 * - parallel-split creates this record with totalBranches count
 * - Each branch increments completedBranches when it reaches parallel-join
 * - Last branch to arrive triggers workflow continuation
 * 
 * Example:
 * splitNodeId="split-1" creates 3 branches -> totalBranches=3, completedBranches=0
 * Branch 1 completes -> completedBranches=1 (wait)
 * Branch 2 completes -> completedBranches=2 (wait)
 * Branch 3 completes -> completedBranches=3 (continue workflow)
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Entity
@Table(name = "parallel_executions", indexes = {
    @Index(name = "idx_parallel_exec_execution_join", columnList = "execution_id, join_node_id"),
    @Index(name = "idx_parallel_exec_execution_split", columnList = "execution_id, split_node_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParallelExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The workflow execution this parallel execution belongs to
     */
    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    /**
     * The node ID of the parallel-split gateway that created the branches
     */
    @Column(name = "split_node_id", nullable = false, length = 100)
    private String splitNodeId;

    /**
     * The node ID of the parallel-join gateway where branches converge
     */
    @Column(name = "join_node_id", nullable = false, length = 100)
    private String joinNodeId;

    /**
     * Total number of parallel branches created by the split
     */
    @Column(name = "total_branches", nullable = false)
    private Integer totalBranches;

    /**
     * Number of branches that have completed and reached the join
     */
    @Column(name = "completed_branches", nullable = false)
    private Integer completedBranches;

    /**
     * Timestamp when parallel execution was created
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when all branches completed (null if still waiting)
     */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (completedBranches == null) {
            completedBranches = 0;
        }
    }

    /**
     * Check if all branches have completed
     */
    public boolean isComplete() {
        return completedBranches != null && totalBranches != null && 
               completedBranches.equals(totalBranches);
    }

    /**
     * Increment the completed branches count (thread-safe via database)
     */
    public void incrementCompletedBranches() {
        this.completedBranches = (this.completedBranches == null ? 0 : this.completedBranches) + 1;
        
        if (isComplete() && completedAt == null) {
            this.completedAt = OffsetDateTime.now();
        }
    }
}
