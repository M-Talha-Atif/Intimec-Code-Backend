package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.repository.IdentityOrgAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes aggregate counts for an {@link OrgUnit}, rolled up recursively
 * across the entire subtree using the materialized {@code path} prefix.
 *
 * <p><strong>Currently implemented (Phase 2):</strong>
 * <ul>
 *   <li>{@code userCount} — all identity types assigned in the subtree</li>
 *   <li>{@code contractorCount} — {@code NON_EMPLOYEE} identities (external
 *       contractors/vendors identified by a contract ID and sponsor)</li>
 *   <li>{@code privilegedCount} — {@code NON_HUMAN} identities (service accounts,
 *       bots, API accounts). Classified as privileged by IGA standards: they
 *       operate outside the normal HR lifecycle, use machine credentials, and
 *       present elevated risk when left unmanaged.</li>
 * </ul>
 *
 * <p><strong>Phase 4 stubs (return zero until access-role integration is built):</strong>
 * {@code roleCount}, {@code adminCount}.
 *
 * <p>Use {@link #computeAggregates(OrgUnit)} to obtain all counts in a single call.
 * Individual methods are kept for callers that need only one count.
 */
@Service
public class OrgAggregateService {

    private final IdentityOrgAssignmentRepository assignmentRepository;

    public OrgAggregateService(IdentityOrgAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    // ------------------------------------------------------------------
    // Value object
    // ------------------------------------------------------------------

    /**
     * Immutable snapshot of all aggregate counts for one org unit.
     * Produced by {@link #computeAggregates(OrgUnit)}.
     *
     * @param userCount       total distinct identities (all types) in the subtree
     * @param contractorCount {@code NON_EMPLOYEE} identities in the subtree
     * @param privilegedCount {@code NON_HUMAN} (service account) identities in the subtree
     * @param roleCount       governed roles scoped to the subtree — Phase 4, always 0
     * @param adminCount      admin-scoped identities in the subtree — Phase 4, always 0
     */
    public record OrgAggregates(
            long userCount,
            long contractorCount,
            long privilegedCount,
            long roleCount,
            long adminCount
    ) {
        /** Zero-value aggregate used when the unit has no materialized path. */
        public static OrgAggregates empty() {
            return new OrgAggregates(0L, 0L, 0L, 0L, 0L);
        }
    }

    // ------------------------------------------------------------------
    // Primary API — obtain all counts in one call
    // ------------------------------------------------------------------

    /**
     * Computes all aggregate counts for the given unit in three DB round-trips
     * (one per identity-type filter). Returns {@link OrgAggregates#empty()} if
     * the unit is null or has no path.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public OrgAggregates computeAggregates(OrgUnit unit) {
        if (unit == null || unit.getPath() == null) return OrgAggregates.empty();
        String pattern = unit.getPath() + "%";
        String tenantId = unit.getTenantId();
        return new OrgAggregates(
                assignmentRepository.countDistinctIdentitiesInSubtree(tenantId, pattern),
                assignmentRepository.countNonEmployeesInSubtree(tenantId, pattern),
                assignmentRepository.countNonHumanIdentitiesInSubtree(tenantId, pattern),
                roleCountInSubtree(unit),
                adminCountInSubtree(unit)
        );
    }

    // ------------------------------------------------------------------
    // Individual count methods (kept for single-count use cases)
    // ------------------------------------------------------------------

    /** All identity types in the subtree. */
    public long userCountInSubtree(OrgUnit unit) {
        if (unit == null || unit.getPath() == null) return 0L;
        return assignmentRepository.countDistinctIdentitiesInSubtree(
                unit.getTenantId(), unit.getPath() + "%");
    }

    /**
     * {@code NON_EMPLOYEE} identities in the subtree — contractors and vendors
     * who have a contract ID and an internal sponsor relationship.
     */
    public long contractorCountInSubtree(OrgUnit unit) {
        if (unit == null || unit.getPath() == null) return 0L;
        return assignmentRepository.countNonEmployeesInSubtree(
                unit.getTenantId(), unit.getPath() + "%");
    }

    /**
     * {@code NON_HUMAN} identities in the subtree — service accounts, bots,
     * and API accounts. These are treated as privileged by IGA standards.
     */
    public long privilegedUserCountInSubtree(OrgUnit unit) {
        if (unit == null || unit.getPath() == null) return 0L;
        return assignmentRepository.countNonHumanIdentitiesInSubtree(
                unit.getTenantId(), unit.getPath() + "%");
    }

    /** Role count scoped to the subtree. Wired in Phase 4. */
    public long roleCountInSubtree(OrgUnit unit) {
        return 0L;
    }

    /** Admin count scoped to the subtree. Wired in Phase 4. */
    public long adminCountInSubtree(OrgUnit unit) {
        return 0L;
    }
}
