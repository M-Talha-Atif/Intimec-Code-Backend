package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApprovalDelegation entity operations
 * 
 * Provides methods for finding active delegations and managing delegation rules.
 * 
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@Repository
public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, UUID> {

    /**
     * Find all delegations for a specific user (as delegator)
     */
    List<ApprovalDelegation> findByFromUserId(UUID fromUserId);

    /**
     * Find all delegations to a specific user (as delegate)
     */
    List<ApprovalDelegation> findByToUserId(UUID toUserId);

    /**
     * Find all delegations for a tenant
     */
    List<ApprovalDelegation> findByTenantId(String tenantId);

    /**
     * Find active delegations for a user within the current date range
     * This is the primary method for automatic delegation routing
     */
    @Query("""
        SELECT d FROM ApprovalDelegation d 
        WHERE d.fromUserId = :fromUserId 
        AND d.active = true 
        AND d.startDate <= :now 
        AND d.endDate >= :now
        AND d.tenantId = :tenantId
        ORDER BY d.createdAt DESC
        """)
    List<ApprovalDelegation> findActiveDelegationsForUser(
        @Param("fromUserId") UUID fromUserId,
        @Param("now") OffsetDateTime now,
        @Param("tenantId") String tenantId
    );

    /**
     * Find the first active delegation for a user (most recent takes precedence)
     * Returns the delegate user ID if found
     */
    @Query("""
        SELECT d FROM ApprovalDelegation d 
        WHERE d.fromUserId = :fromUserId 
        AND d.active = true 
        AND d.startDate <= :now 
        AND d.endDate >= :now
        AND d.tenantId = :tenantId
        ORDER BY d.createdAt DESC
        LIMIT 1
        """)
    Optional<ApprovalDelegation> findFirstActiveDelegation(
        @Param("fromUserId") UUID fromUserId,
        @Param("now") OffsetDateTime now,
        @Param("tenantId") String tenantId
    );

    /**
     * Find active delegations for a specific workflow
     */
    @Query("""
        SELECT d FROM ApprovalDelegation d 
        WHERE d.fromUserId = :fromUserId 
        AND d.active = true 
        AND d.startDate <= :now 
        AND d.endDate >= :now
        AND d.tenantId = :tenantId
        AND (d.scope = 'ALL' OR d.scope = 'WORKFLOW' 
             OR (d.scope = 'SPECIFIC_WORKFLOWS' AND d.workflowIds LIKE %:workflowId%))
        ORDER BY d.createdAt DESC
        LIMIT 1
        """)
    Optional<ApprovalDelegation> findActiveDelegationForWorkflow(
        @Param("fromUserId") UUID fromUserId,
        @Param("now") OffsetDateTime now,
        @Param("tenantId") String tenantId,
        @Param("workflowId") String workflowId
    );

    /**
     * Find all active delegations that have not yet expired
     * Useful for admin oversight
     */
    @Query("""
        SELECT d FROM ApprovalDelegation d 
        WHERE d.active = true 
        AND d.endDate >= :now
        AND d.tenantId = :tenantId
        ORDER BY d.startDate ASC
        """)
    List<ApprovalDelegation> findAllActiveDelegations(
        @Param("now") OffsetDateTime now,
        @Param("tenantId") String tenantId
    );

    /**
     * Find expired delegations that are still marked as active (for cleanup)
     */
    @Query("""
        SELECT d FROM ApprovalDelegation d 
        WHERE d.active = true 
        AND d.endDate < :now
        """)
    List<ApprovalDelegation> findExpiredActiveDelegations(@Param("now") OffsetDateTime now);

    /**
     * Check if a user has any active delegation
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM ApprovalDelegation d 
        WHERE d.fromUserId = :fromUserId 
        AND d.active = true 
        AND d.startDate <= :now 
        AND d.endDate >= :now
        AND d.tenantId = :tenantId
        """)
    boolean hasActiveDelegation(
        @Param("fromUserId") UUID fromUserId,
        @Param("now") OffsetDateTime now,
        @Param("tenantId") String tenantId
    );

    /**
     * Check for overlapping delegations (validation)
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM ApprovalDelegation d 
        WHERE d.fromUserId = :fromUserId 
        AND d.active = true 
        AND d.tenantId = :tenantId
        AND d.id != :excludeId
        AND ((d.startDate <= :endDate AND d.endDate >= :startDate))
        """)
    boolean hasOverlappingDelegation(
        @Param("fromUserId") UUID fromUserId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate,
        @Param("tenantId") String tenantId,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Count active delegations for a user
     */
    @Query("""
        SELECT COUNT(d) FROM ApprovalDelegation d 
        WHERE d.fromUserId = :fromUserId 
        AND d.active = true
        AND d.tenantId = :tenantId
        """)
    long countActiveDelegationsByUser(
        @Param("fromUserId") UUID fromUserId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find delegations created by a specific user (admin use)
     */
    List<ApprovalDelegation> findByCreatedBy(UUID createdBy);

    /**
     * Deactivate all delegations for a user (when user is disabled/deleted)
     */
    @Query("""
        UPDATE ApprovalDelegation d 
        SET d.active = false, d.updatedAt = :now 
        WHERE d.fromUserId = :userId 
        AND d.active = true
        """)
    void deactivateAllDelegationsForUser(
        @Param("userId") UUID userId,
        @Param("now") OffsetDateTime now
    );
}
