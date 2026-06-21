package com.aidentx.core.audit_management.specification;

import com.aidentx.core.audit_management.model.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * JPA Specifications for building dynamic queries for AuditLog entities.
 * Provides static methods to create specifications for various filtering criteria.
 */
public class AuditLogSpecification {

    /**
     * Creates a specification for global search across multiple text fields.
     * Searches in actorName, objectType, event, status, risk, and errorDetails fields.
     * 
     * @param searchTerm the term to search for (case-insensitive, partial match)
     * @return specification for global search
     */
    public static Specification<AuditLog> hasGlobalSearch(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            
            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("actorName")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("objectType")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("event")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("risk")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("errorDetails")), likePattern)
            );
        };
    }

    /**
     * Creates a specification for event matching (supports multiple events).
     * Accepts a single event or comma-separated list of events.
     * 
     * @param events the event(s) to match (single value or comma-separated list)
     * @return specification for event filtering
     */
    public static Specification<AuditLog> hasEvent(String events) {
        return (root, query, criteriaBuilder) -> {
            if (events == null || events.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            
            // Split by comma and trim each value
            String[] eventArray = events.split(",");
            if (eventArray.length == 1) {
                // Single event - use exact match
                return criteriaBuilder.equal(root.get("event"), events.trim());
            } else {
                // Multiple events - use IN clause
                return root.get("event").in((Object[]) java.util.Arrays.stream(eventArray)
                    .map(String::trim)
                    .toArray(String[]::new));
            }
        };
    }

    /**
     * Creates a specification for object type matching (supports multiple object types).
     * Accepts a single object type or comma-separated list of object types.
     * 
     * @param objectTypes the object type(s) to match (single value or comma-separated list)
     * @return specification for object type filtering
     */
    public static Specification<AuditLog> hasObjectType(String objectTypes) {
        return (root, query, criteriaBuilder) -> {
            if (objectTypes == null || objectTypes.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            
            // Split by comma and trim each value
            String[] objectTypeArray = objectTypes.split(",");
            if (objectTypeArray.length == 1) {
                // Single object type - use exact match
                return criteriaBuilder.equal(root.get("objectType"), objectTypes.trim());
            } else {
                // Multiple object types - use IN clause
                return root.get("objectType").in((Object[]) java.util.Arrays.stream(objectTypeArray)
                    .map(String::trim)
                    .toArray(String[]::new));
            }
        };
    }

    /**
     * Creates a specification for risk level matching (supports multiple risk levels).
     * Accepts a single risk level or comma-separated list of risk levels.
     * 
     * @param risks the risk level(s) to match (single value or comma-separated list)
     * @return specification for risk filtering
     */
    public static Specification<AuditLog> hasRisk(String risks) {
        return (root, query, criteriaBuilder) -> {
            if (risks == null || risks.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            
            // Split by comma and trim each value
            String[] riskArray = risks.split(",");
            if (riskArray.length == 1) {
                // Single risk level - use exact match
                return criteriaBuilder.equal(root.get("risk"), risks.trim());
            } else {
                // Multiple risk levels - use IN clause
                return root.get("risk").in((Object[]) java.util.Arrays.stream(riskArray)
                    .map(String::trim)
                    .toArray(String[]::new));
            }
        };
    }

    /**
     * Creates a specification for status matching (supports multiple statuses).
     * Accepts a single status or comma-separated list of statuses.
     * 
     * @param statuses the status(es) to match (single value or comma-separated list)
     * @return specification for status filtering
     */
    public static Specification<AuditLog> hasStatus(String statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            
            // Split by comma and trim each value
            String[] statusArray = statuses.split(",");
            if (statusArray.length == 1) {
                // Single status - use exact match
                return criteriaBuilder.equal(root.get("status"), statuses.trim());
            } else {
                // Multiple statuses - use IN clause
                return root.get("status").in((Object[]) java.util.Arrays.stream(statusArray)
                    .map(String::trim)
                    .toArray(String[]::new));
            }
        };
    }

    /**
     * Creates a specification for timestamp greater than or equal to the specified date.
     * 
     * @param dateFrom the start date (inclusive)
     * @return specification for date from filtering
     */
    public static Specification<AuditLog> hasTimestampFrom(Instant dateFrom) {
        return (root, query, criteriaBuilder) -> {
            if (dateFrom == null) {
                return criteriaBuilder.conjunction(); // Always true
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), dateFrom);
        };
    }

    /**
     * Creates a specification for timestamp less than or equal to the specified date.
     * 
     * @param dateTo the end date (inclusive)
     * @return specification for date to filtering
     */
    public static Specification<AuditLog> hasTimestampTo(Instant dateTo) {
        return (root, query, criteriaBuilder) -> {
            if (dateTo == null) {
                return criteriaBuilder.conjunction(); // Always true
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), dateTo);
        };
    }

    /**
     * Creates a specification for exact tenant ID matching.
     * 
     * @param tenantId the tenant ID to match exactly
     * @return specification for tenant ID filtering
     */
    public static Specification<AuditLog> hasTenantId(String tenantId) {
        return (root, query, criteriaBuilder) -> {
            if (tenantId == null || tenantId.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            return criteriaBuilder.equal(root.get("tenantId"), tenantId);
        };
    }

    /**
     * Creates a specification that excludes records matching any of the given object types.
     * Used to hide internal configuration audit events (e.g., ATTRIBUTE_MAPPING, CORRELATION_RULE)
     * from the main Audit Logs page while keeping them accessible in domain-specific views.
     *
     * @param excludedObjectTypes the object type(s) to exclude (single value or comma-separated list)
     * @return specification that excludes the given object types
     */
    public static Specification<AuditLog> excludeObjectTypes(String excludedObjectTypes) {
        return (root, query, criteriaBuilder) -> {
            if (excludedObjectTypes == null || excludedObjectTypes.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true
            }
            String[] types = java.util.Arrays.stream(excludedObjectTypes.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            if (types.length == 0) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.not(root.get("objectType").in((Object[]) types));
        };
    }
}
