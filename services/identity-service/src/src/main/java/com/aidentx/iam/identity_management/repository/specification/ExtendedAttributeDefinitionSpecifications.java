package com.aidentx.iam.identity_management.repository.specification;

import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import com.aidentx.iam.identity_management.model.entity.*;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.util.UUID;

/**
 * JPA Specifications for ExtendedAttributeDefinition queries.
 * Provides reusable and composable query specifications.
 */
public class ExtendedAttributeDefinitionSpecifications {

    /**
     * Specification to find definitions that have actual values for a specific identity.
     * This is more efficient than loading all definitions and filtering in memory.
     *
     * @param identityId The identity ID to check for
     * @return Specification for definitions with values
     */
    public static Specification<ExtendedAttributeDefinition> hasValuesForIdentity(UUID identityId) {
        return (root, query, criteriaBuilder) -> {
            // Create subqueries for each attribute value table
            Subquery<UUID> stringValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrStringValue.class, identityId);
            Subquery<UUID> numberValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrNumberValue.class, identityId);
            Subquery<UUID> booleanValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrBooleanValue.class, identityId);
            Subquery<UUID> dateValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrDateValue.class, identityId);

            // Return definitions that exist in any of the value tables
            return criteriaBuilder.or(
                criteriaBuilder.exists(stringValueSubquery),
                criteriaBuilder.exists(numberValueSubquery),
                criteriaBuilder.exists(booleanValueSubquery),
                criteriaBuilder.exists(dateValueSubquery)
            );
        };
    }

    /**
     * Specification to filter by tenant ID.
     *
     * @param tenantId The tenant ID to filter by
     * @return Specification for tenant filtering
     */
    public static Specification<ExtendedAttributeDefinition> hasTenantId(String tenantId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Specification to filter by identity type.
     *
     * @param identityType The identity type to filter by
     * @return Specification for identity type filtering
     */
    public static Specification<ExtendedAttributeDefinition> hasIdentityType(String identityType) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("identityType"), identityType);
    }

    /**
     * Specification to filter by system key.
     *
     * @param systemKey The system key to filter by
     * @return Specification for system key filtering
     */
    public static Specification<ExtendedAttributeDefinition> hasSystemKey(String systemKey) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("systemKey"), systemKey);
    }

    /**
     * Specification to filter by data type.
     *
     * @param dataType The data type to filter by
     * @return Specification for data type filtering
     */
    public static Specification<ExtendedAttributeDefinition> hasDataType(String dataType) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("dataType"), dataType);
    }

    /**
     * Specification to find definitions with their actual values for a specific identity.
     * This uses a more efficient join approach to fetch both definitions and values in one query.
     *
     * @param tenantId The tenant ID
     * @param identityType The identity type
     * @param identityId The identity ID
     * @return Specification for definitions with values
     */
    public static Specification<ExtendedAttributeDefinition> definitionsWithValuesForIdentityOptimized(
            String tenantId, String identityType, UUID identityId) {
        return (root, query, criteriaBuilder) -> {
            // Select only the fields we need
            query.multiselect(
                root.get("id"),
                root.get("systemKey"),
                root.get("dataType"),
                root.get("displayName")
            );

            // Create subqueries for each value type to check existence
            Subquery<UUID> stringValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrStringValue.class, identityId);
            Subquery<UUID> numberValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrNumberValue.class, identityId);
            Subquery<UUID> booleanValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrBooleanValue.class, identityId);
            Subquery<UUID> dateValueSubquery = createValueSubquery(
                query, criteriaBuilder, ExtAttrDateValue.class, identityId);

            // Apply filters
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("tenantId"), tenantId),
                criteriaBuilder.equal(root.get("identityType"), identityType),
                criteriaBuilder.or(
                    criteriaBuilder.exists(stringValueSubquery),
                    criteriaBuilder.exists(numberValueSubquery),
                    criteriaBuilder.exists(booleanValueSubquery),
                    criteriaBuilder.exists(dateValueSubquery)
                )
            );
        };
    }

    /**
     * Helper method to create subqueries for checking existence in value tables.
     *
     * @param query The main query
     * @param criteriaBuilder The criteria builder
     * @param valueClass The value entity class
     * @param identityId The identity ID to check for
     * @return Subquery for checking existence
     */
    private static <T extends AbstractExtendedAttributeValue> Subquery<UUID> createValueSubquery(
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            Class<T> valueClass,
            UUID identityId) {

        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<T> valueRoot = subquery.from(valueClass);

        subquery.select(valueRoot.get("definitionId"))
                .where(criteriaBuilder.equal(valueRoot.get("identityId"), identityId));

        return subquery;
    }
}
