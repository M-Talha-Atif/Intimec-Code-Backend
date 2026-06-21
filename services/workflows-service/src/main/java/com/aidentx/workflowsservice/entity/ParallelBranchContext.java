package com.aidentx.workflowsservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 6b: Stores execution context from individual parallel branches
 * When all branches reach the join node, contexts are merged and workflow continues
 * 
 * This allows parallel branches to:
 * - Store their individual outputs
 * - Contribute to the merged context at join
 * - Maintain branch-specific data throughout execution
 */
@Entity
@Table(
    name = "parallel_branch_contexts",
    indexes = {
        @Index(name = "idx_parallel_branch_execution", columnList = "execution_id, branch_index"),
        @Index(name = "idx_parallel_branch_join", columnList = "join_node_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParallelBranchContext {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;
    
    /**
     * The workflow execution this branch belongs to
     */
    @Column(name = "execution_id", nullable = false)
    private UUID executionId;
    
    /**
     * The parallel-split node that created this branch
     */
    @Column(name = "split_node_id", nullable = false, length = 100)
    private String splitNodeId;
    
    /**
     * The parallel-join node where this branch will merge
     */
    @Column(name = "join_node_id", nullable = false, length = 100)
    private String joinNodeId;
    
    /**
     * Zero-based index of this branch (0 to totalBranches-1)
     */
    @Column(name = "branch_index", nullable = false)
    private Integer branchIndex;
    
    /**
     * The execution context for this specific branch
     * Contains branch-specific variables and outputs
     */
    @Type(JsonBinaryType.class)
    @Column(name = "branch_context", columnDefinition = "jsonb")
    private Map<String, Object> branchContext;
    
    /**
     * Whether this branch has reached the join node
     */
    @Column(name = "is_complete", nullable = false)
    @Builder.Default
    private Boolean isComplete = false;

    /**
     * Phase 7: Whether this branch executed successfully (true) or failed (false)
     * null = not yet complete
     */
    @Column(name = "is_successful")
    private Boolean isSuccessful;

    /**
     * Phase 7: Error message if branch failed
     * null if successful or not yet complete
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * When this branch was created
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    /**
     * When this branch reached the join node
     */
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
