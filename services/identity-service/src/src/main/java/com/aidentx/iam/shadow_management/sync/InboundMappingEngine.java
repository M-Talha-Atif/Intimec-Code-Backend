package com.aidentx.iam.shadow_management.sync;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO.AttributeSourcePriorityDTO;
import com.aidentx.iam.contracts.dto.OnboardingRequestDTO.ResourcePriorityDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityTypePolicyDTO;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import com.aidentx.iam.identity_management.service.IdentityLifecycleStateMachine;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.service.IdentityTypePolicyService;
import com.aidentx.iam.policy.service.ResourceGovernancePolicyService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Inbound Mapping Engine — transforms connector attributes into identity fields.
 * <p>
 * This is the core of the IGA import pipeline's reaction phase.
 * It reads {@link AttributeMappingDTO} rules from the governance policy
 * and applies them to map resource object attributes (from shadows)
 * to identity entity fields.
 * <p>
 * Follows MidPoint's inbound mapping pattern:
 * <pre>
 *   Resource Attribute (source) → Mapping Rule → Identity Field (target)
 * </pre>
 * <p>
 * Supports:
 * <ul>
 *   <li>Direct attribute mapping (source → target)</li>
 *   <li>Field derivation for required fields (username, displayName)</li>
 *   <li>Type conversion (String → Date, String → Enum)</li>
 *   <li>Extended attributes for unmapped fields</li>
 * </ul>
 */
@Component
public class InboundMappingEngine {

    private final IUnifiedLogger logger;
    private final ResourceGovernancePolicyService governancePolicyService;
    private final IdentityTypePolicyService identityTypePolicyService;

    // Cache for inbound mappings - same policy applies to all objects from same resource
    // Key: "tenantId|resourceId|identityType", Value: cached mappings list
    private final ConcurrentHashMap<String, List<AttributeMappingDTO>> mappingsCache = new ConcurrentHashMap<>();

    // Cache for source priorities per identity type
    // Key: "tenantId|identityType", Value: map of targetAttribute → list of ResourcePriorityDTO (sorted by priority asc)
    private final ConcurrentHashMap<String, Map<String, List<ResourcePriorityDTO>>> priorityCache = new ConcurrentHashMap<>();

    /**
     * Known identity fields that map to the JOINED inheritance model.
     * Any target field not in this set goes to ExtendedAttributes.
     */
    private static final Set<String> KNOWN_IDENTITY_FIELDS = Set.of(
            // Identity (base)
            "status",
            // HumanIdentity
            "username", "email", "firstName", "lastName", "displayName", "passwordHash", "riskLevel",
            // Employee
            "employeeId", "jobTitle", "department", "hireDate", "terminationDate", "manager",
            // NonEmployee
            "contractId", "vendorName", "contractEndDate",
            // NonHumanIdentity
            "serviceName", "description", "ownerTeam"
    );

    /**
     * Modes that indicate inbound mapping (import from connector → identity).
     * Includes "default", "lowercase", "uppercase" as these are transformation modes
     * that also imply inbound direction.
     */
    private static final Set<String> INBOUND_MODES = Set.of(
            "inbound", "both", "", "default", "lowercase", "uppercase"
    );

    /** Date formats to try when parsing date strings from connectors */
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy",
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'"
    };

    public InboundMappingEngine(UnifiedLoggerFactory loggerFactory,
                                ResourceGovernancePolicyService governancePolicyService,
                                IdentityTypePolicyService identityTypePolicyService) {
        this.logger = loggerFactory.getLogger(InboundMappingEngine.class);
        this.governancePolicyService = governancePolicyService;
        this.identityTypePolicyService = identityTypePolicyService;
    }

    // ========================================================================
    //  Public API
    // ========================================================================

    /**
     * Creates a new identity from shadow attributes using inbound mappings.
     * Routes to the correct identity type builder based on the configured identityType.
     *
     * @param tenantId     Tenant context
     * @param resourceId   The resource (application) ID
     * @param identityType The target identity type (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN)
     * @param shadowAttrs  Attributes from the shadow (connector data)
     * @return A populated Identity entity ready to be persisted, or empty if mappings insufficient
     */
    public Optional<Identity> createIdentityFromShadow(
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        List<AttributeMappingDTO> mappings = loadInboundMappings(tenantId, resourceId, identityType);
        if (mappings.isEmpty()) {
            logger.warn().message("No inbound attribute mappings configured")
                    .detail("tenantId", tenantId)
                    .detail("resourceId", resourceId)
                    .log();
            return Optional.empty();
        }

        // Build the mapped fields from shadow attributes
        Map<String, Object> mappedFields = applyMappings(mappings, shadowAttrs);

        // Derive required fields that weren't explicitly mapped
        deriveRequiredFields(mappedFields, identityType, shadowAttrs);

        // Validate that we have enough to create an identity
        List<String> missingRequired = validateRequiredFields(mappedFields, identityType);
        if (!missingRequired.isEmpty()) {
            logger.warn().message("Cannot create identity — missing required fields after mapping")
                    .detail("missingFields", String.join(", ", missingRequired))
                    .detail("mappedFields", mappedFields.keySet().toString())
                    .detail("identityType", identityType.name())
                    .log();
            return Optional.empty();
        }

        // Build the identity entity based on type
        Identity identity = switch (identityType) {
            case EMPLOYEE -> buildEmployee(tenantId, identityType, mappedFields);
            case NON_EMPLOYEE -> buildNonEmployee(tenantId, identityType, mappedFields);
            case NON_HUMAN -> buildNonHumanIdentity(tenantId, identityType, mappedFields);
        };

        return Optional.of(identity);
    }

    /**
     * Updates an existing identity's attributes from fresh shadow data using inbound mappings.
     * Respects source priority — if a higher-priority source is configured for a field,
     * this source will NOT overwrite values already set by that higher-priority source.
     *
     * @param identity     The existing identity to update
     * @param tenantId     Tenant context
     * @param resourceId   The resource (application) ID
     * @param identityType The target identity type
     * @param shadowAttrs  Fresh attributes from the shadow (connector data)
     * @return Number of attributes that were actually changed
     */
    public int updateIdentityFromShadow(
            Identity identity,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        List<AttributeMappingDTO> mappings = loadInboundMappings(tenantId, resourceId, identityType);
        if (mappings.isEmpty()) {
            logger.debug().message("No inbound mappings configured — skipping identity update").log();
            return 0;
        }

        Map<String, Object> mappedFields = applyMappings(mappings, shadowAttrs);

        // Filter out fields where this resource doesn't have the highest priority
        Map<String, Object> allowedFields = filterBySourcePriority(
                mappedFields, tenantId, resourceId, identityType);

        return applyFieldUpdates(identity, allowedFields);
    }

    /**
     * Represents a single attribute change (old value → new value).
     * Used by dry-run simulation to show what WOULD change without persisting.
     */
    public record AttributeChange(String fieldName, String oldValue, String newValue) {}

    /**
     * Computes attribute changes that WOULD be applied to an identity during sync,
     * without actually modifying anything. Used for dry-run/preview.
     *
     * @param identity     The existing identity to compare against
     * @param tenantId     Tenant context
     * @param resourceId   The resource (application) ID
     * @param identityType The target identity type
     * @param shadowAttrs  Fresh attributes from the shadow (connector data)
     * @return List of attribute changes that would occur
     */
    public List<AttributeChange> computeAttributeChanges(
            Identity identity,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        List<AttributeChange> changes = new ArrayList<>();

        List<AttributeMappingDTO> mappings = loadInboundMappings(tenantId, resourceId, identityType);
        if (mappings.isEmpty()) {
            return changes;
        }

        Map<String, Object> mappedFields = applyMappings(mappings, shadowAttrs);

        // Apply same priority filtering as the real update path
        Map<String, Object> allowedFields = filterBySourcePriority(
                mappedFields, tenantId, resourceId, identityType);

        for (Map.Entry<String, Object> entry : allowedFields.entrySet()) {
            String field = entry.getKey();
            Object newValue = entry.getValue();
            if (newValue == null || !KNOWN_IDENTITY_FIELDS.contains(field)) continue;

            String newStr = toString(newValue);
            String oldStr = getFieldValue(identity, field);

            if (!Objects.equals(newStr, oldStr)) {
                changes.add(new AttributeChange(field, oldStr != null ? oldStr : "", newStr != null ? newStr : ""));
            }
        }

        return changes;
    }

    /**
     * Computes what attributes WOULD be set on a new identity created from shadow data.
     * Used for dry-run preview of UNMATCHED → add-focus reaction.
     *
     * @param tenantId     Tenant context
     * @param resourceId   Resource ID
     * @param identityType Target identity type
     * @param shadowAttrs  Shadow attributes
     * @return List of attribute changes (oldValue is always empty for creation)
     */
    public List<AttributeChange> computeCreationAttributes(
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        List<AttributeChange> changes = new ArrayList<>();

        List<AttributeMappingDTO> mappings = loadInboundMappings(tenantId, resourceId, identityType);
        if (mappings.isEmpty()) return changes;

        Map<String, Object> mappedFields = applyMappings(mappings, shadowAttrs);
        deriveRequiredFields(mappedFields, identityType, shadowAttrs);

        for (Map.Entry<String, Object> entry : mappedFields.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                changes.add(new AttributeChange(field, "", toString(value)));
            }
        }

        return changes;
    }

    /**
     * Gets the current value of a known field from an Identity entity.
     * Handles polymorphic dispatch across HumanIdentity, Employee, NonEmployee, and NonHumanIdentity.
     */
    private String getFieldValue(Identity identity, String field) {
        // Base Identity field
        if ("status".equals(field)) {
            return identity.getStatus() != null ? identity.getStatus().name() : null;
        }

        // HumanIdentity fields (shared by Employee and NonEmployee)
        if (identity instanceof HumanIdentity human) {
            String humanVal = switch (field) {
                case "email" -> human.getEmail();
                case "firstName" -> human.getFirstName();
                case "lastName" -> human.getLastName();
                case "displayName" -> human.getDisplayName();
                case "username" -> human.getUsername();
                    case "riskLevel" -> human.getRiskLevel();
                default -> null;
            };
            if (humanVal != null) return humanVal;

            // Employee-specific fields
            if (identity instanceof Employee emp) {
                return switch (field) {
                    case "employeeId" -> emp.getEmployeeId();
                    case "jobTitle" -> emp.getJobTitle();
                    case "department" -> emp.getDepartment();
                    case "hireDate" -> emp.getHireDate() != null ? emp.getHireDate().toString() : null;
                    case "terminationDate" -> emp.getTerminationDate() != null ? emp.getTerminationDate().toString() : null;
                    case "manager" -> emp.getManagerRef();
                    default -> null;
                };
            }

            // NonEmployee-specific fields
            if (identity instanceof NonEmployee ne) {
                return switch (field) {
                    case "contractId" -> ne.getContractId();
                    case "vendorName" -> ne.getVendorName();
                    case "contractEndDate" -> ne.getContractEndDate() != null ? ne.getContractEndDate().toString() : null;
                    default -> null;
                };
            }
        }

        // NonHumanIdentity fields
        if (identity instanceof NonHumanIdentity nhi) {
            return switch (field) {
                case "serviceName" -> nhi.getServiceName();
                case "description" -> nhi.getDescription();
                case "ownerTeam" -> nhi.getOwnerTeam();
                case "riskLevel" -> nhi.getRiskLevel();
                default -> null;
            };
        }

        return null;
    }

    /**
     * Extracts extended attributes (fields not in the JOINED entity model)
     * from the shadow attributes based on the mapping configuration.
     *
     * @param tenantId     Tenant context
     * @param resourceId   Resource ID
     * @param identityType Identity type
     * @param shadowAttrs  Shadow attributes
     * @return Map of extended attribute name → value
     */
    public Map<String, Object> extractExtendedAttributes(
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        List<AttributeMappingDTO> mappings = loadInboundMappings(tenantId, resourceId, identityType);
        Map<String, Object> extended = new LinkedHashMap<>();

        for (AttributeMappingDTO mapping : mappings) {
            String target = mapping.targetAttributeName();
            if (target != null && !KNOWN_IDENTITY_FIELDS.contains(target)) {
                Object value = resolveSourceValue(mapping.sourceAttributeName(), shadowAttrs);
                if (value != null) {
                    extended.put(target, value);
                }
            }
        }

        return extended;
    }

    // ========================================================================
    //  Internal: Mapping Resolution
    // ========================================================================

    /**
     * Loads inbound attribute mappings from the governance policy.
     * Filters by mode = "inbound" or "both" (or null/empty for backward compat).
     * PERFORMANCE: Caches mappings to avoid repeated DB lookups during batch sync.
     */
    private List<AttributeMappingDTO> loadInboundMappings(
            String tenantId, String resourceId, IdentityType identityType) {

        String cacheKey = tenantId + "|" + resourceId + "|" + identityType;

        return mappingsCache.computeIfAbsent(cacheKey, key -> {
            try {
                logger.debug().message("Loading and caching inbound mappings")
                        .detail("tenantId", tenantId)
                        .detail("resourceId", resourceId)
                        .detail("identityType", identityType)
                        .log();

                IdentityMappingPolicyDTO policy = governancePolicyService.getPolicy(
                        tenantId, identityType, UUID.fromString(resourceId));

                if (policy == null || policy.attributeMappings() == null) {
                    logger.debug().message("No policy or mappings found, caching empty list")
                            .detail("resourceId", resourceId)
                            .log();
                    return Collections.emptyList();
                }

                List<AttributeMappingDTO> filtered = policy.attributeMappings().stream()
                        .filter(m -> m.sourceAttributeName() != null && m.targetAttributeName() != null)
                        .filter(m -> m.mode() == null || INBOUND_MODES.contains(m.mode().toLowerCase()))
                        .collect(Collectors.toList());

                logger.debug().message("Cached inbound mappings")
                        .detail("resourceId", resourceId)
                        .detail("cachedMappings", filtered.size())
                        .log();

                return filtered;
            } catch (Exception e) {
                logger.warn().message("Failed to load inbound mappings")
                        .detail("resourceId", resourceId)
                        .detail("error", e.getMessage())
                        .log();
                return Collections.emptyList();
            }
        });
    }

    /**
     * Clears the mappings cache. Call between sync runs or when policies change.
     */
    public void clearMappingsCache() {
        mappingsCache.clear();
        priorityCache.clear();
        logger.debug().message("Cleared inbound mappings and priority caches").log();
    }

    /**
     * Filters mapped fields by source priority policy.
     * For each target field, checks whether this resource has the highest priority (lowest number).
     * If a higher-priority resource is configured for the field, the field is excluded
     * so that the lower-priority source does not overwrite the authoritative value.
     * <p>
     * If no priority policy is configured, all fields pass through (backward-compatible).
     */
    private Map<String, Object> filterBySourcePriority(
            Map<String, Object> mappedFields,
            String tenantId,
            String resourceId,
            IdentityType identityType) {

        Map<String, List<ResourcePriorityDTO>> priorities = loadSourcePriorities(tenantId, identityType);
        if (priorities.isEmpty()) {
            // No priority policy configured — all fields allowed (backward-compatible)
            return mappedFields;
        }

        UUID currentResourceId = UUID.fromString(resourceId);
        Map<String, Object> allowed = new LinkedHashMap<>();
        int skipped = 0;

        for (Map.Entry<String, Object> entry : mappedFields.entrySet()) {
            String targetField = entry.getKey();
            List<ResourcePriorityDTO> fieldPriorities = priorities.get(targetField);

            if (fieldPriorities == null || fieldPriorities.isEmpty()) {
                // No priority rule for this field — allow the write
                allowed.put(targetField, entry.getValue());
                continue;
            }

            // Check if the current resource is the highest priority for this field
            ResourcePriorityDTO highestPriority = fieldPriorities.get(0); // sorted ascending
            if (highestPriority.resourceId().equals(currentResourceId)) {
                // This resource IS the authoritative source — allow
                allowed.put(targetField, entry.getValue());
            } else {
                // Check if current resource is in the priority list at all
                boolean currentInList = fieldPriorities.stream()
                        .anyMatch(rp -> rp.resourceId().equals(currentResourceId));
                if (!currentInList) {
                    // Current resource not in the priority list — allow (unconstrained)
                    allowed.put(targetField, entry.getValue());
                } else {
                    // Current resource has lower priority — skip this field
                    skipped++;
                    logger.debug().message("Source priority: skipping field — higher priority source configured")
                            .detail("targetField", targetField)
                            .detail("currentResource", resourceId)
                            .detail("authoritativeResource", highestPriority.resourceId().toString())
                            .log();
                }
            }
        }

        if (skipped > 0) {
            logger.info().message("Source priority filtering applied")
                    .detail("totalFields", mappedFields.size())
                    .detail("allowedFields", allowed.size())
                    .detail("skippedFields", skipped)
                    .log();
        }

        return allowed;
    }

    /**
     * Loads source priorities for an identity type. Cached per sync run.
     * Returns a map of targetAttribute → sorted list of ResourcePriorityDTO (priority ascending).
     */
    private Map<String, List<ResourcePriorityDTO>> loadSourcePriorities(
            String tenantId, IdentityType identityType) {

        String cacheKey = tenantId + "|" + identityType;

        return priorityCache.computeIfAbsent(cacheKey, key -> {
            try {
                IdentityTypePolicyDTO policy = identityTypePolicyService.getIdentityTypePolicy(
                        tenantId, identityType);

                if (policy == null || policy.sourcePriorities() == null || policy.sourcePriorities().isEmpty()) {
                    return Collections.emptyMap();
                }

                Map<String, List<ResourcePriorityDTO>> result = new HashMap<>();
                for (AttributeSourcePriorityDTO asp : policy.sourcePriorities()) {
                    if (asp.targetAttribute() != null && asp.priorities() != null) {
                        List<ResourcePriorityDTO> sorted = asp.priorities().stream()
                                .sorted(Comparator.comparingInt(ResourcePriorityDTO::priority))
                                .toList();
                        result.put(asp.targetAttribute(), sorted);
                    }
                }

                logger.debug().message("Loaded source priorities")
                        .detail("identityType", identityType.name())
                        .detail("fieldsWithPriority", result.size())
                        .log();

                return result;
            } catch (Exception e) {
                logger.warn().message("Failed to load source priorities — allowing all writes")
                        .detail("error", e.getMessage())
                        .log();
                return Collections.emptyMap();
            }
        });
    }

    /**
     * Applies attribute mappings to transform shadow attributes into identity field values.
     *
     * @param mappings    The inbound mappings from governance policy
     * @param shadowAttrs The raw connector attributes on the shadow
     * @return Map of targetFieldName → resolved value
     */
    private Map<String, Object> applyMappings(
            List<AttributeMappingDTO> mappings,
            Map<String, Object> shadowAttrs) {

        Map<String, Object> result = new LinkedHashMap<>();

        for (AttributeMappingDTO mapping : mappings) {
            String source = mapping.sourceAttributeName();
            String target = mapping.targetAttributeName();

            Object rawValue = resolveSourceValue(source, shadowAttrs);
            if (rawValue == null) {
                continue;
            }

            // Apply script transformation if configured (future: Groovy/JS engine)
            Object transformedValue = applyTransformation(mapping, rawValue);

            result.put(target, transformedValue);
        }

        return result;
    }

    /**
     * Resolves the source value from shadow attributes.
     * Handles nested attribute names using dot-notation (e.g., "address.city").
     */
    private Object resolveSourceValue(String sourceAttrName, Map<String, Object> shadowAttrs) {
        if (sourceAttrName == null || shadowAttrs == null) {
            return null;
        }

        // Direct lookup first
        Object value = shadowAttrs.get(sourceAttrName);
        if (value != null) {
            return value;
        }

        // Try case-insensitive lookup (connectors may return varying cases)
        for (Map.Entry<String, Object> entry : shadowAttrs.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(sourceAttrName)) {
                return entry.getValue();
            }
        }

        // Support dot-notation for nested objects
        if (sourceAttrName.contains(".")) {
            String[] parts = sourceAttrName.split("\\.", 2);
            Object nested = shadowAttrs.get(parts[0]);
            if (nested instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nested;
                return resolveSourceValue(parts[1], nestedMap);
            }
        }

        return null;
    }

    /**
     * Applies a script transformation to the resolved value.
     * Currently a placeholder — returns value unchanged.
     * Future: Groovy/JavaScript engine integration.
     */
    private Object applyTransformation(AttributeMappingDTO mapping, Object rawValue) {
        if (mapping.script() == null || mapping.script().data() == null || mapping.script().data().isBlank()) {
            return rawValue;
        }

        // TODO: Integrate Groovy/JavaScript transformation engine
        // ScriptDTO script = mapping.script();
        // return scriptEngine.evaluate(script.lang(), script.data(), rawValue);

        logger.debug().message("Script transformation not yet implemented — using raw value")
                .detail("target", mapping.targetAttributeName())
                .log();
        return rawValue;
    }

    // ========================================================================
    //  Internal: Field Derivation
    // ========================================================================

    /**
     * Derives required fields that weren't covered by explicit mappings.
     * Type-aware: derives different fields based on identity type.
     */
    private void deriveRequiredFields(Map<String, Object> mappedFields,
                                      IdentityType identityType,
                                      Map<String, Object> shadowAttrs) {
        if (identityType == IdentityType.NON_HUMAN) {
            // NHI: derive serviceName from shadow attributes if not mapped
            if (!mappedFields.containsKey("serviceName")) {
                for (String key : List.of("__NAME__", "cn", "name", "serviceName", "sAMAccountName")) {
                    Object val = shadowAttrs.get(key);
                    if (val != null) {
                        mappedFields.put("serviceName", toString(val));
                        break;
                    }
                }
            }
            return;
        }

        // Human identity types (EMPLOYEE, NON_EMPLOYEE): derive username/email/displayName

        // Derive username from email prefix if not mapped
        if (!mappedFields.containsKey("username") && mappedFields.containsKey("email")) {
            String email = toString(mappedFields.get("email"));
            if (email != null && email.contains("@")) {
                mappedFields.put("username", email.substring(0, email.indexOf('@')));
            }
        }

        // Derive username from employeeId/contractId if still missing
        if (!mappedFields.containsKey("username")) {
            if (mappedFields.containsKey("employeeId")) {
                mappedFields.put("username", toString(mappedFields.get("employeeId")));
            } else if (mappedFields.containsKey("contractId")) {
                mappedFields.put("username", toString(mappedFields.get("contractId")));
            }
        }

        // Derive displayName from firstName + lastName
        if (!mappedFields.containsKey("displayName")) {
            String firstName = toString(mappedFields.get("firstName"));
            String lastName = toString(mappedFields.get("lastName"));
            if (firstName != null && lastName != null) {
                mappedFields.put("displayName", firstName + " " + lastName);
            } else if (firstName != null) {
                mappedFields.put("displayName", firstName);
            } else if (lastName != null) {
                mappedFields.put("displayName", lastName);
            }
        }

        // Derive email from shadow attrs if not mapped (try common connector attribute names)
        if (!mappedFields.containsKey("email")) {
            for (String emailKey : List.of("email", "mail", "emailAddress", "Email", "Mail")) {
                Object emailVal = shadowAttrs.get(emailKey);
                if (emailVal != null) {
                    mappedFields.put("email", toString(emailVal));
                    break;
                }
            }
        }
    }

    /**
     * Validates that all required fields for the identity type are present.
     * Returns list of missing required field names.
     */
    private List<String> validateRequiredFields(Map<String, Object> mappedFields, IdentityType identityType) {
        List<String> missing = new ArrayList<>();

        return switch (identityType) {
            case EMPLOYEE -> {
                if (!mappedFields.containsKey("username")) missing.add("username");
                if (!mappedFields.containsKey("email")) missing.add("email");
                if (!mappedFields.containsKey("employeeId")) missing.add("employeeId");
                yield missing;
            }
            case NON_EMPLOYEE -> {
                if (!mappedFields.containsKey("username")) missing.add("username");
                if (!mappedFields.containsKey("email")) missing.add("email");
                if (!mappedFields.containsKey("contractId")) missing.add("contractId");
                yield missing;
            }
            case NON_HUMAN -> {
                if (!mappedFields.containsKey("serviceName")) missing.add("serviceName");
                yield missing;
            }
        };
    }

    // ========================================================================
    //  Internal: Entity Building
    // ========================================================================

    /**
     * Builds a new Employee entity from the mapped field values.
     */
    private Employee buildEmployee(String tenantId, IdentityType identityType,
                                   Map<String, Object> mappedFields) {
        Employee employee = new Employee();

        // Base Identity fields
        employee.setTenantId(tenantId);
        employee.setIdentityType(identityType);
        employee.setStatus(resolveStatus(mappedFields.get("status")));

        // HumanIdentity fields
        employee.setUsername(toString(mappedFields.get("username")));
        employee.setEmail(toString(mappedFields.get("email")));
        employee.setFirstName(toString(mappedFields.get("firstName")));
        employee.setLastName(toString(mappedFields.get("lastName")));
        employee.setDisplayName(toString(mappedFields.get("displayName")));
        employee.setRiskLevel(toString(mappedFields.get("riskLevel")));

        // Employee fields
        employee.setEmployeeId(toString(mappedFields.get("employeeId")));
        employee.setJobTitle(toString(mappedFields.get("jobTitle")));
        employee.setDepartment(toString(mappedFields.get("department")));
        employee.setHireDate(toDate(mappedFields.get("hireDate")));
        employee.setTerminationDate(toDate(mappedFields.get("terminationDate")));

        // Manager: store raw connector reference for post-import resolution
        String managerVal = toString(mappedFields.get("manager"));
        if (managerVal != null) {
            employee.setManagerRef(managerVal);
        }

        return employee;
    }

    /**
     * Builds a new NonEmployee entity from the mapped field values.
     */
    private NonEmployee buildNonEmployee(String tenantId, IdentityType identityType,
                                         Map<String, Object> mappedFields) {
        NonEmployee nonEmployee = new NonEmployee();

        // Base Identity fields
        nonEmployee.setTenantId(tenantId);
        nonEmployee.setIdentityType(identityType);
        nonEmployee.setStatus(resolveStatus(mappedFields.get("status")));

        // HumanIdentity fields
        nonEmployee.setUsername(toString(mappedFields.get("username")));
        nonEmployee.setEmail(toString(mappedFields.get("email")));
        nonEmployee.setFirstName(toString(mappedFields.get("firstName")));
        nonEmployee.setLastName(toString(mappedFields.get("lastName")));
        nonEmployee.setDisplayName(toString(mappedFields.get("displayName")));
        nonEmployee.setRiskLevel(toString(mappedFields.get("riskLevel")));

        // NonEmployee fields
        nonEmployee.setContractId(toString(mappedFields.get("contractId")));
        nonEmployee.setVendorName(toString(mappedFields.get("vendorName")));
        nonEmployee.setContractEndDate(toDate(mappedFields.get("contractEndDate")));

        return nonEmployee;
    }

    /**
     * Builds a new NonHumanIdentity entity from the mapped field values.
     */
    private NonHumanIdentity buildNonHumanIdentity(String tenantId, IdentityType identityType,
                                                    Map<String, Object> mappedFields) {
        NonHumanIdentity nhi = new NonHumanIdentity();

        // Base Identity fields
        nhi.setTenantId(tenantId);
        nhi.setIdentityType(identityType);
        nhi.setStatus(resolveStatus(mappedFields.get("status")));

        // NonHumanIdentity fields
        nhi.setServiceName(toString(mappedFields.get("serviceName")));
        nhi.setDescription(toString(mappedFields.get("description")));
        nhi.setOwnerTeam(toString(mappedFields.get("ownerTeam")));
        nhi.setRiskLevel(toString(mappedFields.get("riskLevel")));

        return nhi;
    }

    /**
     * Applies mapped field values to an existing Identity entity.
     * Polymorphic: dispatches to the correct field setters based on runtime type.
     *
     * @return Number of fields actually changed
     */
    private int applyFieldUpdates(Identity identity, Map<String, Object> mappedFields) {
        int changed = 0;

        for (Map.Entry<String, Object> entry : mappedFields.entrySet()) {
            String field = entry.getKey();
            Object newValue = entry.getValue();

            if (newValue == null || !KNOWN_IDENTITY_FIELDS.contains(field)) {
                continue;
            }

            boolean fieldChanged = applyFieldUpdate(identity, field, newValue);
            if (fieldChanged) {
                changed++;
            }
        }

        // Re-derive displayName if first/last name changed (HumanIdentity types only)
        if (identity instanceof HumanIdentity human) {
            if (mappedFields.containsKey("firstName") || mappedFields.containsKey("lastName")) {
                String dn = (human.getFirstName() != null ? human.getFirstName() : "")
                        + " "
                        + (human.getLastName() != null ? human.getLastName() : "");
                String trimmed = dn.trim();
                if (!trimmed.isEmpty() && !trimmed.equals(human.getDisplayName())) {
                    human.setDisplayName(trimmed);
                }
            }
        }

        return changed;
    }

    /**
     * Applies a single field update to an Identity entity.
     * Polymorphic: uses instanceof to dispatch to correct setter.
     *
     * @return true if the field was actually changed
     */
    private boolean applyFieldUpdate(Identity identity, String field, Object newValue) {
        String strVal = toString(newValue);

        // Base Identity fields
        if ("status".equals(field)) {
            IdentityStatus newStatus = resolveStatus(newValue);
            if (newStatus != identity.getStatus()) {
                if (IdentityLifecycleStateMachine.isValidTransition(identity.getStatus(), newStatus)) {
                    identity.setStatus(newStatus);
                    return true;
                } else {
                    logger.warn().message("Inbound mapping skipped — invalid lifecycle transition")
                            .detail("identityId", identity.getId() != null ? identity.getId().toString() : "new")
                            .detail("currentStatus", identity.getStatus().name())
                            .detail("targetStatus", newStatus.name())
                            .detail("validTargets", IdentityLifecycleStateMachine.getValidTargets(identity.getStatus()).toString())
                            .log();
                    return false;
                }
            }
            return false;
        }

        // HumanIdentity fields (shared by Employee and NonEmployee)
        if (identity instanceof HumanIdentity human) {
            switch (field) {
                case "email" -> {
                    if (!Objects.equals(strVal, human.getEmail())) {
                        human.setEmail(strVal);
                        return true;
                    }
                    return false;
                }
                case "firstName" -> {
                    if (!Objects.equals(strVal, human.getFirstName())) {
                        human.setFirstName(strVal);
                        return true;
                    }
                    return false;
                }
                case "lastName" -> {
                    if (!Objects.equals(strVal, human.getLastName())) {
                        human.setLastName(strVal);
                        return true;
                    }
                    return false;
                }
                case "displayName" -> {
                    if (!Objects.equals(strVal, human.getDisplayName())) {
                        human.setDisplayName(strVal);
                        return true;
                    }
                    return false;
                }
                case "username" -> {
                    if (!Objects.equals(strVal, human.getUsername())) {
                        human.setUsername(strVal);
                        return true;
                    }
                    return false;
                }
                case "riskLevel" -> {
                    if (!Objects.equals(strVal, human.getRiskLevel())) {
                        human.setRiskLevel(strVal);
                        return true;
                    }
                    return false;
                }
            }

            // Employee-specific fields
            if (identity instanceof Employee employee) {
                switch (field) {
                    case "employeeId" -> {
                        if (!Objects.equals(strVal, employee.getEmployeeId())) {
                            employee.setEmployeeId(strVal);
                            return true;
                        }
                    }
                    case "jobTitle" -> {
                        if (!Objects.equals(strVal, employee.getJobTitle())) {
                            employee.setJobTitle(strVal);
                            return true;
                        }
                    }
                    case "department" -> {
                        if (!Objects.equals(strVal, employee.getDepartment())) {
                            employee.setDepartment(strVal);
                            return true;
                        }
                    }
                    case "hireDate" -> {
                        Date dateVal = toDate(newValue);
                        if (dateVal != null && !dateVal.equals(employee.getHireDate())) {
                            employee.setHireDate(dateVal);
                            return true;
                        }
                    }
                    case "terminationDate" -> {
                        Date dateVal = toDate(newValue);
                        if (dateVal != null && !dateVal.equals(employee.getTerminationDate())) {
                            employee.setTerminationDate(dateVal);
                            return true;
                        }
                    }
                    case "manager" -> {
                        if (!Objects.equals(strVal, employee.getManagerRef())) {
                            employee.setManagerRef(strVal);
                            return true;
                        }
                    }
                }
            }

            // NonEmployee-specific fields
            if (identity instanceof NonEmployee nonEmployee) {
                switch (field) {
                    case "contractId" -> {
                        if (!Objects.equals(strVal, nonEmployee.getContractId())) {
                            nonEmployee.setContractId(strVal);
                            return true;
                        }
                    }
                    case "vendorName" -> {
                        if (!Objects.equals(strVal, nonEmployee.getVendorName())) {
                            nonEmployee.setVendorName(strVal);
                            return true;
                        }
                    }
                    case "contractEndDate" -> {
                        Date dateVal = toDate(newValue);
                        if (dateVal != null && !dateVal.equals(nonEmployee.getContractEndDate())) {
                            nonEmployee.setContractEndDate(dateVal);
                            return true;
                        }
                    }
                }
            }
        }

        // NonHumanIdentity fields
        if (identity instanceof NonHumanIdentity nhi) {
            switch (field) {
                case "serviceName" -> {
                    if (!Objects.equals(strVal, nhi.getServiceName())) {
                        nhi.setServiceName(strVal);
                        return true;
                    }
                }
                case "description" -> {
                    if (!Objects.equals(strVal, nhi.getDescription())) {
                        nhi.setDescription(strVal);
                        return true;
                    }
                }
                case "ownerTeam" -> {
                    if (!Objects.equals(strVal, nhi.getOwnerTeam())) {
                        nhi.setOwnerTeam(strVal);
                        return true;
                    }
                }
                case "riskLevel" -> {
                    if (!Objects.equals(strVal, nhi.getRiskLevel())) {
                        nhi.setRiskLevel(strVal);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // ========================================================================
    //  Internal: Type Conversion Utilities
    // ========================================================================

    @SuppressWarnings("unchecked")
    private String toString(Object value) {
        if (value == null) return null;
        // Handle List values (e.g., from connectors that wrap values in arrays)
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return null;
            // All single-char strings → join into one string (char-array artifact)
            if (list.size() > 1 && list.stream().allMatch(
                    item -> item instanceof String s && s.length() == 1)) {
                StringBuilder sb = new StringBuilder(list.size());
                for (Object item : list) sb.append((String) item);
                return sb.toString();
            }
            // Single-element list → unwrap
            if (list.size() == 1) {
                return toString(list.get(0));
            }
            // Multi-element list → join with comma
            return list.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Date toDate(Object value) {
        if (value == null) return null;

        if (value instanceof Date) return (Date) value;
        if (value instanceof Long) return new Date((Long) value);

        String strVal = value.toString().trim();
        if (strVal.isEmpty()) return null;

        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(strVal);
            } catch (ParseException ignored) {
                // Try next format
            }
        }

        logger.debug().message("Could not parse date value")
                .detail("value", strVal)
                .log();
        return null;
    }

    /**
     * Resolves a status value from connector data to the IdentityStatus enum.
     * Handles common status representations from connectors.
     */
    private IdentityStatus resolveStatus(Object value) {
        if (value == null) return IdentityStatus.ACTIVE;

        String strVal = value.toString().trim().toUpperCase();

        return switch (strVal) {
            case "ACTIVE", "ENABLED", "TRUE", "1", "YES" -> IdentityStatus.ACTIVE;
            case "DISABLED", "INACTIVE", "SUSPENDED", "FALSE", "0", "NO" -> IdentityStatus.INACTIVE;
            default -> {
                // Try direct enum match
                try {
                    yield IdentityStatus.valueOf(strVal);
                } catch (IllegalArgumentException e) {
                    yield IdentityStatus.ACTIVE; // Default for unknown values
                }
            }
        };
    }
}
