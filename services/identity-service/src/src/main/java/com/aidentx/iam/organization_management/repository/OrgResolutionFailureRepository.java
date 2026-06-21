package com.aidentx.iam.organization_management.repository;

import com.aidentx.iam.organization_management.model.entity.OrgResolutionFailure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgResolutionFailureRepository extends JpaRepository<OrgResolutionFailure, UUID> {

    Page<OrgResolutionFailure> findByTenantIdAndResolvedOrderByOccurredAtDesc(
            String tenantId, boolean resolved, Pageable pageable);

    Page<OrgResolutionFailure> findByTenantIdAndTaskRunIdOrderByOccurredAtDesc(
            String tenantId, String taskRunId, Pageable pageable);

    long countByTenantIdAndTaskRunId(String tenantId, String taskRunId);

    Optional<OrgResolutionFailure> findFirstByTenantIdAndIdentityIdAndAttemptedValueAndResolvedFalseOrderByOccurredAtDesc(
            String tenantId, UUID identityId, String attemptedValue);

    List<OrgResolutionFailure> findByTenantIdAndIdentityIdAndResolvedFalse(
            String tenantId, UUID identityId);

    @Modifying
    @Query("""
        UPDATE OrgResolutionFailure f
           SET f.resolved = true, f.resolvedAt = :now
         WHERE f.tenantId = :tenantId
           AND f.identityId = :identityId
           AND f.resolved = false
        """)
    int markResolvedForIdentity(@Param("tenantId") String tenantId,
                                @Param("identityId") UUID identityId,
                                @Param("now") Instant now);
}
