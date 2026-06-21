package com.aidentx.iam.shadow_management.repository;

import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.model.entity.Shadow.SynchronizationSituation;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specification builder for creating dynamic queries for Shadow entities.
 * Used for advanced search and filtering capabilities.
 */
public class ShadowSpecification {

    /**
     * Creates a specification for filtering shadows based on various criteria.
     *
     * @param tenantId                 Required tenant ID
     * @param resourceUid              Optional resource UID filter
     * @param objectClass              Optional object class filter
     * @param synchronizationSituation Optional synchronization situation filter
     * @param linked                   Optional linked status filter (true = linked,
     *                                 false = unlinked, null = all)
     * @param kind                     Optional kind filter
     * @param linkedIdentityId         Optional linked identity ID filter
     * @return Specification for querying shadows
     */
    public static Specification<Shadow> filterShadows(
            String tenantId,
            String resourceUid,
            String objectClass,
            SynchronizationSituation synchronizationSituation,
            Boolean linked,
            String kind,
            UUID linkedIdentityId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant ID is always required
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            // Optional filters
            if (resourceUid != null && !resourceUid.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceId"), resourceUid));
            }

            if (objectClass != null && !objectClass.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("objectClass"), objectClass));
            }

            if (synchronizationSituation != null) {
                predicates.add(criteriaBuilder.equal(root.get("synchronizationSituation"), synchronizationSituation));
            }

            if (linked != null) {
                if (linked) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("linkedIdentityId")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("linkedIdentityId")));
                }
            }

            if (kind != null && !kind.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("kind"), kind));
            }

            if (linkedIdentityId != null) {
                predicates.add(criteriaBuilder.equal(root.get("linkedIdentityId"), linkedIdentityId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification for searching shadows by resource UID pattern.
     *
     * @param tenantId           Required tenant ID
     * @param resourceUidPattern Pattern to match against resource UID (supports SQL
     *                           LIKE)
     * @return Specification for querying shadows
     */
    public static Specification<Shadow> searchByResourceUid(String tenantId, String resourceUidPattern) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            predicates.add(criteriaBuilder.like(root.get("resourceUid"), resourceUidPattern));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification for finding shadows that need synchronization.
     * (e.g., shadows that haven't been synchronized recently)
     *
     * @param tenantId          Required tenant ID
     * @param lastSyncThreshold Timestamp threshold (milliseconds since epoch)
     * @return Specification for querying shadows
     */
    public static Specification<Shadow> needsSynchronization(String tenantId, Long lastSyncThreshold) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            // Shadows that have never been synchronized or were synchronized before the
            // threshold
            Predicate neverSynced = criteriaBuilder.isNull(root.get("lastSynchronizedAt"));
            Predicate oldSync = criteriaBuilder.lessThan(root.get("lastSynchronizedAt"), lastSyncThreshold);

            predicates.add(criteriaBuilder.or(neverSynced, oldSync));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification for finding shadows by multiple resource UIDs.
     *
     * @param tenantId     Required tenant ID
     * @param resourceUids List of resource UIDs
     * @return Specification for querying shadows
     */
    public static Specification<Shadow> inResources(String tenantId, List<String> resourceUids) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            if (resourceUids != null && !resourceUids.isEmpty()) {
                predicates.add(root.get("resourceUid").in(resourceUids));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification for finding disputed shadows.
     * Disputed shadows have DISPUTED synchronization situation and are not linked.
     *
     * @param tenantId Required tenant ID
     * @return Specification for querying shadows
     */
    public static Specification<Shadow> disputedShadows(String tenantId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            predicates.add(criteriaBuilder.equal(root.get("synchronizationSituation"),
                    SynchronizationSituation.DISPUTED));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification for finding orphaned shadows.
     * Orphaned shadows are linked to an identity that may no longer exist.
     * This is a marker method - actual orphan detection requires a join with
     * Identity table.
     *
     * @param tenantId Required tenant ID
     * @return Specification for querying shadows
     */
    public static Specification<Shadow> potentialOrphanedShadows(String tenantId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            predicates.add(criteriaBuilder.isNotNull(root.get("linkedIdentityId")));
            // Note: Actual orphan detection would require checking if the identity exists
            // This would be implemented in the service layer with a more complex query
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a specification with AND combination of multiple specifications.
     *
     * @param specs Variable number of specifications to combine
     * @return Combined specification
     */
    @SafeVarargs
    public static Specification<Shadow> and(Specification<Shadow>... specs) {
        Specification<Shadow> result = null;
        for (Specification<Shadow> spec : specs) {
            if (spec != null) {
                result = result == null ? spec : result.and(spec);
            }
        }
        return result;
    }

    /**
     * Creates a specification with OR combination of multiple specifications.
     *
     * @param specs Variable number of specifications to combine
     * @return Combined specification
     */
    @SafeVarargs
    public static Specification<Shadow> or(Specification<Shadow>... specs) {
        Specification<Shadow> result = null;
        for (Specification<Shadow> spec : specs) {
            if (spec != null) {
                result = result == null ? spec : result.or(spec);
            }
        }
        return result;
    }
}
