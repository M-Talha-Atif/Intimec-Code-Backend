package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves entity state and details from the database for audit events.
 *
 * <h3>Why This Exists as a Separate Service</h3>
 * <p>The {@link AccessRoleAuditAspect} intercepts service methods via AOP.
 * However, the aspect itself is a plain {@code @Component} — it does NOT
 * participate in Spring's transaction management. When the aspect makes
 * direct repository calls, those calls may execute <strong>outside</strong>
 * any active transaction, depending on the AOP advice ordering relative
 * to {@code @Transactional}.</p>
 *
 * <p>By extracting all DB reads into this {@code @Service} bean with
 * {@code @Transactional} methods, we guarantee that every repository call
 * runs within a proper transaction context:</p>
 * <ul>
 *   <li>If a business transaction is already active (aspect runs inside tx),
 *       these methods <strong>join</strong> the existing transaction via
 *       {@code Propagation.REQUIRED} and see all uncommitted changes
 *       through Hibernate's first-level cache.</li>
 *   <li>If no transaction is active (aspect runs outside tx, e.g. after
 *       the business tx committed), these methods create a
 *       <strong>new</strong> read-only transaction and see all committed
 *       data.</li>
 * </ul>
 *
 * <h3>This Fixes the "Unknown" Name Bug</h3>
 * <p>Without this service, repository calls in the aspect could fail silently
 * (returning empty results) when executed outside a transaction context,
 * because Hibernate requires an active Session/EntityManager to resolve
 * entities. Empty results → IDs not found in the map → "Unknown" fallback.</p>
 *
 * <h3>Single Responsibility</h3>
 * <p>This class is solely responsible for <strong>reading</strong> entity
 * state from the database and transforming it into audit-friendly formats.
 * It does NOT publish events, resolve actors, or handle AOP concerns.</p>
 */
@Service
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public class AccessRoleAuditStateResolver {

    private final AccessRoleRepository roleRepository;
    private final AccessRoleCompositionRepository compositionRepository;
    private final AccessRoleEntitlementRepository entitlementLinkRepository;
    private final EntitlementRepository entitlementRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    /**
     * Constructs the resolver with repository and serialization dependencies.
     *
     * @param roleRepository            reads role entities for state capture and name resolution
     * @param compositionRepository     reads composition links for child role ID lists
     * @param entitlementLinkRepository reads entitlement links for entitlement ID lists
     * @param entitlementRepository     reads entitlement entities for name and risk resolution
     * @param objectMapper              Jackson mapper for JSON serialization
     * @param loggerFactory             unified logger factory
     */
    public AccessRoleAuditStateResolver(
            AccessRoleRepository roleRepository,
            AccessRoleCompositionRepository compositionRepository,
            AccessRoleEntitlementRepository entitlementLinkRepository,
            EntitlementRepository entitlementRepository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.roleRepository = roleRepository;
        this.compositionRepository = compositionRepository;
        this.entitlementLinkRepository = entitlementLinkRepository;
        this.entitlementRepository = entitlementRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAuditConstants.LOG_CATEGORY);
    }

    // ===================================================================
    //  Role State Capture
    // ===================================================================

    /**
     * Captures the current state of a role entity as a JSON snapshot.
     *
     * @param roleId the role ID
     * @return JSON representation of the role, or null if not found
     */
    public JsonNode captureRoleState(UUID roleId) {
        try {
            Optional<AccessRole> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isPresent()) {
                return objectMapper.valueToTree(roleOpt.get());
            }
        } catch (Exception e) {
            logger.warn()
                    .category(AccessRoleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditConstants.LOG_STATE_CAPTURE_FAILED)
                    .details(Map.of(
                            AccessRoleAuditConstants.DETAIL_ROLE_ID, roleId.toString(),
                            AccessRoleAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
        return null;
    }

    /**
     * Finds a role by ID.
     *
     * @param roleId the role ID
     * @return the role optional
     */
    public Optional<AccessRole> findRole(UUID roleId) {
        return roleRepository.findById(roleId);
    }

    // ===================================================================
    //  Current ID Lists — Read from link tables
    // ===================================================================

    /**
     * Gets the current list of child role IDs for a parent role
     * from the composition link table.
     *
     * @param parentRoleId the parent role ID
     * @return list of child role UUIDs (may be empty)
     */
    public List<UUID> getCurrentChildIds(UUID parentRoleId) {
        return compositionRepository.findByParentRoleId(parentRoleId)
                .stream()
                .map(AccessRoleComposition::getChildRoleId)
                .collect(Collectors.toList());
    }

    /**
     * Gets the current list of entitlement IDs linked to a role
     * from the role-entitlement link table.
     *
     * @param roleId the role ID
     * @return list of entitlement UUIDs (may be empty)
     */
    public List<UUID> getCurrentEntitlementIds(UUID roleId) {
        return entitlementLinkRepository.findByRoleId(roleId)
                .stream()
                .map(AccessRoleEntitlement::getEntitlementId)
                .collect(Collectors.toList());
    }

    // ===================================================================
    //  State Serialization — Rich JSON with entity names
    //
    //  All methods in this section resolve entity details (names, types,
    //  risk levels) from the database so that audit state snapshots are
    //  human-readable. This is critical for compliance — auditors must
    //  see what was there, even if the entity is later deleted.
    // ===================================================================

    /**
     * Serializes child roles to a JSON object with full details.
     *
     * <p>Example output:</p>
     * <pre>{@code
     * {
     *   "childRoles": [
     *     {"id":"...", "name":"Report Viewer", "type":"technical", "riskLevel":"low"}
     *   ]
     * }
     * }</pre>
     *
     * @param childIds the child role UUIDs
     * @return JSON object with enriched child role array
     */
    public JsonNode childRolesToJson(List<UUID> childIds) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode arr = node.putArray("childRoles");

        if (childIds.isEmpty()) {
            return node;
        }

        Map<UUID, AccessRole> roleMap = loadRoleMap(childIds);

        for (UUID id : childIds) {
            ObjectNode childNode = objectMapper.createObjectNode();
            childNode.put("id", id.toString());
            AccessRole role = roleMap.get(id);
            if (role != null) {
                childNode.put("name", role.getName());
                childNode.put("type", role.getType());
                if (role.getRiskLevel() != null) {
                    childNode.put("riskLevel", role.getRiskLevel());
                }
            } else {
                childNode.put("name", "Unknown");
            }
            arr.add(childNode);
        }

        return node;
    }

    /**
     * Serializes entitlements to a JSON object with full details.
     *
     * <p>Example output:</p>
     * <pre>{@code
     * {
     *   "entitlements": [
     *     {"id":"...", "name":"SAP_FI_POSTING", "riskLevel":"high"}
     *   ]
     * }
     * }</pre>
     *
     * @param entitlementIds the entitlement UUIDs
     * @return JSON object with enriched entitlement array
     */
    public JsonNode entitlementsToJson(List<UUID> entitlementIds) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode arr = node.putArray("entitlements");

        if (entitlementIds.isEmpty()) {
            return node;
        }

        Map<UUID, Entitlement> entMap = loadEntitlementMap(entitlementIds);

        for (UUID id : entitlementIds) {
            ObjectNode entNode = objectMapper.createObjectNode();
            entNode.put("id", id.toString());
            Entitlement ent = entMap.get(id);
            if (ent != null) {
                entNode.put("name", ent.getDisplayName());
                if (ent.getRiskLevel() != null) {
                    entNode.put("riskLevel", ent.getRiskLevel());
                }
            } else {
                entNode.put("name", "Unknown");
            }
            arr.add(entNode);
        }

        return node;
    }

    /**
     * Builds a JSON array of entitlements for the enriched role creation stateAfter.
     *
     * @param entitlementIds the entitlement UUIDs to include
     * @return ArrayNode containing entitlement objects with id, name, riskLevel
     */
    public ArrayNode buildEntitlementArray(List<UUID> entitlementIds) {
        ArrayNode arr = objectMapper.createArrayNode();

        Map<UUID, Entitlement> entMap = loadEntitlementMap(entitlementIds);

        for (UUID id : entitlementIds) {
            ObjectNode entNode = objectMapper.createObjectNode();
            entNode.put("id", id.toString());
            Entitlement ent = entMap.get(id);
            if (ent != null) {
                entNode.put("name", ent.getDisplayName());
                if (ent.getRiskLevel() != null) {
                    entNode.put("riskLevel", ent.getRiskLevel());
                }
            } else {
                entNode.put("name", "Unknown");
            }
            arr.add(entNode);
        }

        return arr;
    }

    /**
     * Builds a JSON array of child roles for the enriched role creation stateAfter.
     *
     * @param childIds the child role UUIDs to include
     * @return ArrayNode containing child role objects with id, name, type, riskLevel
     */
    public ArrayNode buildChildRoleArray(List<UUID> childIds) {
        ArrayNode arr = objectMapper.createArrayNode();

        Map<UUID, AccessRole> roleMap = loadRoleMap(childIds);

        for (UUID id : childIds) {
            ObjectNode childNode = objectMapper.createObjectNode();
            childNode.put("id", id.toString());
            AccessRole role = roleMap.get(id);
            if (role != null) {
                childNode.put("name", role.getName());
                childNode.put("type", role.getType());
                if (role.getRiskLevel() != null) {
                    childNode.put("riskLevel", role.getRiskLevel());
                }
            } else {
                childNode.put("name", "Unknown");
            }
            arr.add(childNode);
        }

        return arr;
    }

    // ===================================================================
    //  Detail Resolution — Resolve entity names for audit event details
    // ===================================================================

    /**
     * Resolves full details for a list of child role IDs.
     * Fetches role entities in a single batch query (1 SQL query, not N).
     *
     * @param childRoleIds the child role UUIDs
     * @return list of audit details with names, types, and risk levels
     */
    public List<AccessRoleAuditEvent.ChildRoleAuditDetail> resolveChildRoleDetails(List<UUID> childRoleIds) {
        if (childRoleIds == null || childRoleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, AccessRole> roleMap = loadRoleMap(childRoleIds);

        return childRoleIds.stream()
                .map(id -> {
                    AccessRole role = roleMap.get(id);
                    if (role != null) {
                        return new AccessRoleAuditEvent.ChildRoleAuditDetail(
                                id.toString(), role.getName(), role.getType(), role.getRiskLevel());
                    }
                    return new AccessRoleAuditEvent.ChildRoleAuditDetail(
                            id.toString(), "Unknown", "unknown", null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Resolves details for failed child roles, including the failure reason.
     *
     * @param failures the failed child DTOs from the composition service
     * @return list of failed child audit details with names and reasons
     */
    public List<AccessRoleAuditEvent.FailedChildRoleAuditDetail> resolveFailedChildDetails(
            List<AddChildRolesResultDto.FailedChild> failures) {
        if (failures == null || failures.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> failedIds = failures.stream()
                .map(AddChildRolesResultDto.FailedChild::getChildRoleId)
                .collect(Collectors.toList());

        Map<UUID, AccessRole> roleMap = loadRoleMap(failedIds);

        return failures.stream()
                .map(f -> {
                    AccessRole role = roleMap.get(f.getChildRoleId());
                    String name = role != null ? role.getName() : "Unknown";
                    String type = role != null ? role.getType() : "unknown";
                    String risk = role != null ? role.getRiskLevel() : null;
                    return new AccessRoleAuditEvent.FailedChildRoleAuditDetail(
                            f.getChildRoleId().toString(), name, type, risk, f.getReason());
                })
                .collect(Collectors.toList());
    }

    /**
     * Resolves entitlement details from the database.
     *
     * @param entitlementIds the entitlement UUIDs to resolve
     * @return list of entitlement audit details with display names and risk levels
     */
    public List<AccessRoleAuditEvent.EntitlementAuditDetail> resolveEntitlementDetails(List<UUID> entitlementIds) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, Entitlement> entMap = loadEntitlementMap(entitlementIds);

        return entitlementIds.stream()
                .map(id -> {
                    Entitlement ent = entMap.get(id);
                    if (ent != null) {
                        return new AccessRoleAuditEvent.EntitlementAuditDetail(
                                id.toString(), ent.getDisplayName(), ent.getRiskLevel());
                    }
                    return new AccessRoleAuditEvent.EntitlementAuditDetail(
                            id.toString(), "Unknown", null);
                })
                .collect(Collectors.toList());
    }

    // ===================================================================
    //  Risk Resolution — Compute highest risk among affected entities
    //
    //  Risk hierarchy: high (2) > medium (1) > low (0) > null (-1)
    // ===================================================================

    /** Ordinal values for risk level comparison. */
    private static final int RISK_HIGH = 2;
    private static final int RISK_MEDIUM = 1;
    private static final int RISK_LOW = 0;

    /**
     * Resolves the highest risk level among a list of role IDs.
     *
     * @param roleIds the role UUIDs
     * @return the highest risk level string, or "low" if none
     */
    public String resolveHighestRiskFromRoles(List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return "low";
        }

        return roleRepository.findByIdIn(roleIds)
                .stream()
                .map(AccessRole::getRiskLevel)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(this::riskOrdinal))
                .orElse("low");
    }

    /**
     * Resolves the highest risk level among a list of entitlement IDs.
     *
     * @param entitlementIds the entitlement UUIDs
     * @return the highest risk level string, or "low" if none
     */
    public String resolveHighestRiskFromEntitlements(List<UUID> entitlementIds) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return "low";
        }

        return entitlementRepository.findAllById(entitlementIds)
                .stream()
                .map(Entitlement::getRiskLevel)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(this::riskOrdinal))
                .orElse("low");
    }

    /**
     * Maps a risk level string to a numeric ordinal for comparison.
     *
     * @param risk the risk level string (case-insensitive)
     * @return ordinal: high=2, medium=1, low=0, unknown=-1
     */
    private int riskOrdinal(String risk) {
        if (risk == null) return -1;
        return switch (risk.toLowerCase()) {
            case "high" -> RISK_HIGH;
            case "medium" -> RISK_MEDIUM;
            case "low" -> RISK_LOW;
            default -> -1;
        };
    }

    // ===================================================================
    //  Batch Loading Helpers — Single query per entity type
    //
    //  These methods centralize all batch DB reads. Every resolution
    //  method above calls one of these instead of making its own
    //  repository call, ensuring consistent behavior and easy debugging.
    // ===================================================================

    /**
     * Batch loads AccessRole entities by IDs and returns a lookup map.
     * Uses {@code findByIdIn} defined on {@link AccessRoleRepository}.
     *
     * @param ids the role UUIDs to load
     * @return map of UUID → AccessRole (empty entries mean role not found)
     */
    private Map<UUID, AccessRole> loadRoleMap(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AccessRole> roles = roleRepository.findByIdIn(ids);
        logger.debug()
                .category(AccessRoleAuditConstants.LOG_CATEGORY)
                .message("Batch loaded roles for audit")
                .detail("requestedCount", String.valueOf(ids.size()))
                .detail("foundCount", String.valueOf(roles.size()))
                .log();
        return roles.stream()
                .collect(Collectors.toMap(AccessRole::getId, r -> r));
    }

    /**
     * Batch loads Entitlement entities by IDs and returns a lookup map.
     * Uses {@code findAllById} inherited from {@link org.springframework.data.jpa.repository.JpaRepository}.
     *
     * @param ids the entitlement UUIDs to load
     * @return map of UUID → Entitlement (empty entries mean entitlement not found)
     */
    private Map<UUID, Entitlement> loadEntitlementMap(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Entitlement> entitlements = entitlementRepository.findAllById(ids);
        logger.debug()
                .category(AccessRoleAuditConstants.LOG_CATEGORY)
                .message("Batch loaded entitlements for audit")
                .detail("requestedCount", String.valueOf(ids.size()))
                .detail("foundCount", String.valueOf(entitlements.size()))
                .log();
        return entitlements.stream()
                .collect(Collectors.toMap(Entitlement::getId, e -> e));
    }
}