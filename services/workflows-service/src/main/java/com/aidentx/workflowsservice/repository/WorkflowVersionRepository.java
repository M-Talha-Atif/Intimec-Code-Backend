package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowVersion entity operations
 */
@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {

    List<WorkflowVersion> findByWorkflowId(UUID workflowId);

    Optional<WorkflowVersion> findByWorkflowIdAndVersion(UUID workflowId, String version);

    @Query("SELECT v FROM WorkflowVersion v WHERE v.workflowId = :workflowId AND v.isActive = true")
    Optional<WorkflowVersion> findActiveVersion(@Param("workflowId") UUID workflowId);

    @Query("SELECT v FROM WorkflowVersion v WHERE v.workflowId = :workflowId ORDER BY v.createdAt DESC")
    List<WorkflowVersion> findVersionHistory(@Param("workflowId") UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}
