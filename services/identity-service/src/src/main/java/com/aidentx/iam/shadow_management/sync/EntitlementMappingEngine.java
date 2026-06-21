package com.aidentx.iam.shadow_management.sync;

import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.entitlement_management.policy.service.EntitlementGovernancePolicyService;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.aidentx.iam.identity_management.repository.NonEmployeeRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Entitlement Mapping Engine — transforms connector attributes into Entitlement
 * governance fields during inbound synchronization.
 *
 * <p>This is the entitlement-domain equivalent of {@link InboundMappingEngine}.
 * It reads {@link AttributeMappingDTO} rules from the
 * {@link EntitlementGovernancePolicyService} and applies them to raw connector
 * attributes to derive governance metadata for an entitlement entry.</p>
 *
 * <p>Mapping follows the MidPoint inbound pipeline pattern:</p>
 * <pre>
 *   Connector Attribute (source) → Mapping Rule → Entitlement Field (target)
 * </pre>
 *
 * <h3>Supported target fields</h3>
 * <ul>
 *   <li>{@code displayName} / {@code description} – plain strings</li>
 *   <li>{@code riskLevel} – normalised to Low / Medium / High / Critical</li>
 *   <li>{@code riskReason} – free text</li>
 *   <li>{@code lifecycle} – normalised to Active / Deprecated / Orphaned / Inactive</li>
 *   <li>{@code classification} – multi-value; each mapping rule contributes one label</li>
 *   <li>{@code identityType} – Employee / NonEmployee / NonHuman</li>
 *   <li>{@code privilegeLevel} – normalised to Read / Write / Admin / Privileged</li>
 *   <li>{@code requestable}, {@code requireApproval}, {@code allowedAutomatedGrant} – boolean</li>
 *   <li>{@code ownerId} – UUID; resolved from UUID-string, email, or employeeId</li>
 *   <li>{@code maxDurationMs}, {@code autoExpireDurationMs} – Long</li>
 * </ul>
 *
 * <p>Unrecognised target fields are collected into {@code customAttributes} for
 * future extensibility.</p>
 *
 * <h3>Caching</h3>
 * Mappings are cached per {@code "tenantId|resourceId"} to avoid repeated DB
 * round-trips during large batch sync runs. Call {@link #clearMappingsCache()}
 * between sync runs or after a policy update.
 */
@Component
public class EntitlementMappingEngine {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Target field names that map directly onto Entitlement entity fields. */
    public static final Set<String> KNOWN_ENTITLEMENT_FIELDS = Set.of(
            "displayName", "description",
            "riskLevel", "riskReason",
            "lifecycle", "classification",
            "identityType", "privilegeLevel",
            "requestable", "requireApproval", "allowedAutomatedGrant",
            "ownerId",
            "maxDurationMs", "autoExpireDurationMs"
    );

    /** Modes that indicate inbound direction (source → target). */
    private static final Set<String> INBOUND_MODES = Set.of(
            "inbound", "both", "", "default", "lowercase", "uppercase"
    );

    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> LIFECYCLES = Set.of("ACTIVE", "DEPRECATED", "ORPHANED", "INACTIVE");
    private static final Set<String> PRIVILEGE_LEVELS = Set.of("READ", "WRITE", "ADMIN", "PRIVILEGED");

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final IUnifiedLogger logger;
    private final EntitlementGovernancePolicyService policyService;
    private final EmployeeRepository employeeRepository;
    private final NonEmployeeRepository nonEmployeeRepository;

    private final ConcurrentHashMap<String, List<AttributeMappingDTO>> mappingsCache =
            new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------------

    /**
     * Immutable result carrying all governance fields derived from the mapping
     * rules. Any field that had no rule configured will be {@code null} (or
     * {@code false} for the boolean flags when using the entity defaults).
     */
    public record EntitlementMappingResult(
            String displayName,
            String description,
            String riskLevel,
            String riskReason,
            String lifecycle,
            List<String> classification,
            String identityType,
            String privilegeLevel,
            Boolean requestable,
            Boolean requireApproval,
            Boolean allowedAutomatedGrant,
            UUID ownerId,
            Long maxDurationMs,
            Long autoExpireDurationMs,
            Map<String, Object> customAttributes
    ) {}

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public EntitlementMappingEngine(
            UnifiedLoggerFactory loggerFactory,
            EntitlementGovernancePolicyService policyService,
            EmployeeRepository employeeRepository,
            NonEmployeeRepository nonEmployeeRepository) {
        this.logger = loggerFactory.getLogger(EntitlementMappingEngine.class);
        this.policyService = policyService;
        this.employeeRepository = employeeRepository;
        this.nonEmployeeRepository = nonEmployeeRepository;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Applies configured mapping rules to the given connector attributes and
     * returns the derived governance fields.
     *
     * <p>Returns {@link Optional#empty()} when no mapping rules are configured
     * for the resource, so the caller can skip the field-application step.</p>
     *
     * @param tenantId           tenant context
     * @param resourceId         application/resource UUID
     * @param connectorAttributes raw attributes from the connector object
     * @return optional mapping result; empty when no rules are configured
     */
    public Optional<EntitlementMappingResult> applyMappings(
            String tenantId,
            UUID resourceId,
            Map<String, Object> connectorAttributes) {

        List<AttributeMappingDTO> mappings = loadMappings(tenantId, resourceId.toString());
        if (mappings.isEmpty()) {
            logger.debug().message("No entitlement inbound mappings configured — skipping governance projection")
                    .detail("tenantId", tenantId)
                    .detail("resourceId", resourceId)
                    .log();
            return Optional.empty();
        }

        // Single-value fields accumulated in this map
        Map<String, Object> resolved = new LinkedHashMap<>();

        // Multi-value accumulator — used for classification (and any future list fields)
        Map<String, List<Object>> multiValue = new LinkedHashMap<>();

        for (AttributeMappingDTO mapping : mappings) {
            if (mapping.sourceAttributeName() == null || mapping.targetAttributeName() == null) {
                continue;
            }

            Object rawValue = resolveSourceValue(mapping.sourceAttributeName(), connectorAttributes);
            if (rawValue == null) {
                continue;
            }

            Object transformed = applyMode(rawValue, mapping.mode());
            String target = mapping.targetAttributeName();

            if ("classification".equals(target)) {
                // Always accumulate — a single mapping rule can contribute multiple labels
                if (transformed instanceof List<?> list) {
                    list.forEach(v -> multiValue.computeIfAbsent(target, k -> new ArrayList<>())
                            .add(v));
                } else {
                    multiValue.computeIfAbsent(target, k -> new ArrayList<>()).add(transformed);
                }
            } else {
                resolved.put(target, transformed);
            }
        }

        // Merge multi-value into resolved
        for (Map.Entry<String, List<Object>> entry : multiValue.entrySet()) {
            resolved.put(entry.getKey(), entry.getValue().stream()
                    .map(v -> v != null ? v.toString() : "")
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList()));
        }

        logger.debug().message("Entitlement attribute mapping applied")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .detail("resolvedFieldCount", resolved.size())
                .log();

        return Optional.of(buildResult(tenantId, resolved));
    }

    /**
     * Clears the mapping cache. Must be called when a policy is updated or
     * at the start of a new sync run to ensure fresh rules are used.
     */
    public void clearMappingsCache() {
        mappingsCache.clear();
        logger.debug().message("Cleared entitlement mappings cache").log();
    }

    /**
     * Clears the cached mappings for a single resource.
     * Used for targeted invalidation after a policy update via REST.
     */
    public void evictCacheForResource(String tenantId, UUID resourceId) {
        String cacheKey = tenantId + "|" + resourceId;
        mappingsCache.remove(cacheKey);
        logger.debug().message("Evicted entitlement mappings cache for resource")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .log();
    }

    // -----------------------------------------------------------------------
    // Internal: cache & loading
    // -----------------------------------------------------------------------

    private List<AttributeMappingDTO> loadMappings(String tenantId, String resourceId) {
        String cacheKey = tenantId + "|" + resourceId;
        return mappingsCache.computeIfAbsent(cacheKey, key -> {
            try {
                List<AttributeMappingDTO> all =
                        policyService.getAttributeMappings(tenantId, UUID.fromString(resourceId));

                List<AttributeMappingDTO> filtered = all.stream()
                        .filter(m -> m.sourceAttributeName() != null && m.targetAttributeName() != null)
                        .filter(m -> isInboundMode(m.mode()))
                        .collect(Collectors.toList());

                logger.debug().message("Loaded and cached entitlement mappings")
                        .detail("tenantId", tenantId)
                        .detail("resourceId", resourceId)
                        .detail("cachedCount", filtered.size())
                        .log();
                return Collections.unmodifiableList(filtered);
            } catch (Exception e) {
                logger.warn().message("Failed to load entitlement mappings")
                        .detail("tenantId", tenantId)
                        .detail("resourceId", resourceId)
                        .detail("error", e.getMessage())
                        .log();
                return Collections.emptyList();
            }
        });
    }

    private boolean isInboundMode(String mode) {
        if (mode == null) return true;
        return INBOUND_MODES.contains(mode.toLowerCase());
    }

    // -----------------------------------------------------------------------
    // Internal: attribute resolution
    // -----------------------------------------------------------------------

    /**
     * Resolves the value of a source attribute from the connector attributes map.
     * Handles ConnID multi-value lists (returns first element for single-value fields,
     * returns the whole list for list-type targets).
     */
    private Object resolveSourceValue(String sourceAttrName, Map<String, Object> attrs) {
        // Literal value prefix (e.g. "literal:Active")
        if (sourceAttrName.startsWith("literal:")) {
            return sourceAttrName.substring("literal:".length());
        }
        return attrs.get(sourceAttrName);
    }

    private Object applyMode(Object value, String mode) {
        if (value == null || mode == null) return value;
        String strVal = value instanceof String ? (String) value : value.toString();
        return switch (mode.toLowerCase()) {
            case "lowercase" -> strVal.toLowerCase();
            case "uppercase" -> strVal.toUpperCase();
            default -> value;
        };
    }

    // -----------------------------------------------------------------------
    // Internal: result construction
    // -----------------------------------------------------------------------

    private EntitlementMappingResult buildResult(String tenantId, Map<String, Object> fields) {
        Map<String, Object> custom = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!KNOWN_ENTITLEMENT_FIELDS.contains(entry.getKey())) {
                custom.put(entry.getKey(), entry.getValue());
            }
        }

        return new EntitlementMappingResult(
                getString(fields, "displayName"),
                getString(fields, "description"),
                normalizeRiskLevel(getString(fields, "riskLevel")),
                getString(fields, "riskReason"),
                normalizeLifecycle(getString(fields, "lifecycle")),
                getStringList(fields, "classification"),
                getString(fields, "identityType"),
                normalizePrivilegeLevel(getString(fields, "privilegeLevel")),
                getBoolean(fields, "requestable"),
                getBoolean(fields, "requireApproval"),
                getBoolean(fields, "allowedAutomatedGrant"),
                resolveOwnerId(tenantId, getString(fields, "ownerId")),
                getLong(fields, "maxDurationMs"),
                getLong(fields, "autoExpireDurationMs"),
                custom.isEmpty() ? null : Collections.unmodifiableMap(custom)
        );
    }

    // -----------------------------------------------------------------------
    // Internal: normalisation helpers
    // -----------------------------------------------------------------------

    private String normalizeRiskLevel(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase();
        return RISK_LEVELS.contains(upper)
                ? upper.charAt(0) + upper.substring(1).toLowerCase()
                : value.trim();
    }

    private String normalizeLifecycle(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase();
        return LIFECYCLES.contains(upper)
                ? upper.charAt(0) + upper.substring(1).toLowerCase()
                : value.trim();
    }

    private String normalizePrivilegeLevel(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase();
        return PRIVILEGE_LEVELS.contains(upper)
                ? upper.charAt(0) + upper.substring(1).toLowerCase()
                : value.trim();
    }

    // -----------------------------------------------------------------------
    // Internal: owner resolution
    // -----------------------------------------------------------------------

    /**
     * Resolves an owner identity UUID from the raw mapping value.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Parse directly as UUID string</li>
     *   <li>Look up Employee by email (tenantId-scoped)</li>
     *   <li>Look up NonEmployee by email (tenantId-scoped)</li>
     *   <li>Look up Employee by employeeId (tenantId-scoped)</li>
     * </ol>
     * Returns {@code null} if no identity is found, so the field stays unset
     * rather than failing the whole sync event.
     * </p>
     */
    private UUID resolveOwnerId(String tenantId, String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();

        // 1. Direct UUID parse
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID — fall through to identity lookup
        }

        // 2. Employee by email
        try {
            Optional<UUID> byEmail = employeeRepository
                    .findByTenantIdAndEmail(tenantId, trimmed)
                    .map(e -> e.getId());
            if (byEmail.isPresent()) return byEmail.get();
        } catch (Exception e) {
            logger.debug().message("Owner resolution via employee email failed")
                    .detail("value", trimmed)
                    .detail("error", e.getMessage())
                    .log();
        }

        // 3. NonEmployee by email
        try {
            Optional<UUID> byEmail = nonEmployeeRepository
                    .findByTenantIdAndEmail(tenantId, trimmed)
                    .map(ne -> ne.getId());
            if (byEmail.isPresent()) return byEmail.get();
        } catch (Exception e) {
            logger.debug().message("Owner resolution via non-employee email failed")
                    .detail("value", trimmed)
                    .detail("error", e.getMessage())
                    .log();
        }

        // 4. Employee by employeeId
        try {
            Optional<UUID> byEmpId = employeeRepository
                    .findByTenantIdAndEmployeeId(tenantId, trimmed)
                    .map(e -> e.getId());
            if (byEmpId.isPresent()) return byEmpId.get();
        } catch (Exception e) {
            logger.debug().message("Owner resolution via employeeId failed")
                    .detail("value", trimmed)
                    .detail("error", e.getMessage())
                    .log();
        }

        logger.warn().message("Could not resolve ownerId from connector value — field left unset")
                .detail("value", trimmed)
                .log();
        return null;
    }

    // -----------------------------------------------------------------------
    // Internal: type coercion utilities
    // -----------------------------------------------------------------------

    private String getString(Map<String, Object> fields, String key) {
        Object v = fields.get(key);
        if (v == null) return null;
        if (v instanceof String s) return s.isBlank() ? null : s;
        if (v instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first != null ? first.toString() : null;
        }
        return v.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> fields, String key) {
        Object v = fields.get(key);
        if (v == null) return null;
        if (v instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        String s = v.toString();
        // Support comma-separated single-value source attributes
        if (s.contains(",")) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList());
        }
        return List.of(s);
    }

    private Boolean getBoolean(Map<String, Object> fields, String key) {
        Object v = fields.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = v.toString().trim().toLowerCase();
        return switch (s) {
            case "true", "yes", "1", "on" -> Boolean.TRUE;
            case "false", "no", "0", "off" -> Boolean.FALSE;
            default -> null;
        };
    }

    private Long getLong(Map<String, Object> fields, String key) {
        Object v = fields.get(key);
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
