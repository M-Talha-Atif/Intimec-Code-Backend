package com.aidentx.iam.organization_management.repository;

import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord;
import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord.Outcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrgAssignmentRunRecordRepository extends JpaRepository<OrgAssignmentRunRecord, UUID> {

    Page<OrgAssignmentRunRecord> findByTenantIdAndTaskRunIdOrderByOccurredAtDesc(
            String tenantId, String taskRunId, Pageable pageable);

    Page<OrgAssignmentRunRecord> findByTenantIdAndTaskRunIdAndOutcomeOrderByOccurredAtDesc(
            String tenantId, String taskRunId, Outcome outcome, Pageable pageable);

    long countByTenantIdAndTaskRunId(String tenantId, String taskRunId);

    /**
     * Aggregate outcome counts for a single task run. Returned rows are
     * {@code [Outcome, Long]} pairs suitable for the summary chips above the
     * detail table.
     */
    @Query("""
        SELECT r.outcome, COUNT(r)
          FROM OrgAssignmentRunRecord r
         WHERE r.tenantId = :tenantId
           AND r.taskRunId = :taskRunId
         GROUP BY r.outcome
        """)
    List<Object[]> countOutcomesByRun(@Param("tenantId") String tenantId,
                                      @Param("taskRunId") String taskRunId);
}
