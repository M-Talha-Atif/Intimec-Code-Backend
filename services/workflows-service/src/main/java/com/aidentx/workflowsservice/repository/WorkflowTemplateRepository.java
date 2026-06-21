package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkflowTemplate entity operations
 */
@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    Optional<WorkflowTemplate> findByName(String name);

    List<WorkflowTemplate> findByCategory(String category);

    List<WorkflowTemplate> findByFramework(String framework);

    List<WorkflowTemplate> findByComplexity(WorkflowTemplate.Complexity complexity);

    List<WorkflowTemplate> findByIsSystemTemplate(Boolean isSystemTemplate);

    @Query("SELECT t FROM WorkflowTemplate t WHERE t.usageCount > :minUsage ORDER BY t.usageCount DESC")
    List<WorkflowTemplate> findPopularTemplates(@Param("minUsage") Integer minUsage);

    @Query("SELECT t FROM WorkflowTemplate t ORDER BY t.rating DESC")
    List<WorkflowTemplate> findTopRatedTemplates();
}
