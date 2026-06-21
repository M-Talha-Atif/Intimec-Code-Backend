package com.aidentx.iam.identity_management.repository.specification;

import com.aidentx.iam.identity_management.model.entity.Identity;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for Identity entity queries.
 * Provides reusable query specifications for complex filtering.
 */
public class IdentitySpecifications {

    /**
     * Specification to find identities by tenant ID and username containing a search term.
     *
     * @param tenantId The tenant ID to filter by.
     * @param username The username search term (partial match).
     * @return Specification for the query.
     */
    public static Specification<Identity> byTenantIdAndUsernameContaining(String tenantId, String username) {
        return (root, query, criteriaBuilder) -> {
            var tenantPredicate = criteriaBuilder.equal(root.get("tenantId"), tenantId);
            var usernamePredicate = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("username")),
                "%" + username.toLowerCase() + "%"
            );
            return criteriaBuilder.and(tenantPredicate, usernamePredicate);
        };
    }

    /**
     * Specification to find all identities by tenant ID.
     *
     * @param tenantId The tenant ID to filter by.
     * @return Specification for the query.
     */
    public static Specification<Identity> byTenantId(String tenantId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Specification to find identities by tenant ID and identity type.
     *
     * @param tenantId The tenant ID to filter by.
     * @param identityType The identity type to filter by.
     * @return Specification for the query.
     */
    public static Specification<Identity> byTenantIdAndIdentityType(String tenantId, String identityType) {
        return (root, query, criteriaBuilder) -> {
            var tenantPredicate = criteriaBuilder.equal(root.get("tenantId"), tenantId);
            var typePredicate = criteriaBuilder.equal(root.get("identityType"), identityType);
            return criteriaBuilder.and(tenantPredicate, typePredicate);
        };
    }

    /**
     * Specification to find identities by tenant ID and status.
     *
     * @param tenantId The tenant ID to filter by.
     * @param status The status to filter by.
     * @return Specification for the query.
     */
    public static Specification<Identity> byTenantIdAndStatus(String tenantId, String status) {
        return (root, query, criteriaBuilder) -> {
            var tenantPredicate = criteriaBuilder.equal(root.get("tenantId"), tenantId);
            var statusPredicate = criteriaBuilder.equal(root.get("status"), status);
            return criteriaBuilder.and(tenantPredicate, statusPredicate);
        };
    }
}
