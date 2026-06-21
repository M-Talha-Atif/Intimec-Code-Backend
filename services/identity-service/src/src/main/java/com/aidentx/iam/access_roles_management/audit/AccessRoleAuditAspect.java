package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AOP aspect for auditing all access role governance operations.
 *
 * <h3>Responsibilities (Thin Orchestrator)</h3>
 * <p>This class is solely responsible for:</p>
 * <ul>
 *   <li>Intercepting annotated methods via {@code @Around}</li>
 *   <li>Extracting method parameters (tenantId, roleId)</li>
 *   <li>Resolving the authenticated actor from Spring Security</li>
 *   <li>Coordinating before/after state capture via {@link AccessRoleAuditStateResolver}</li>
 *   <li>Building {@link AccessRoleAuditEvent} instances</li>
 *   <li>Publishing Kafka events via {@link AccessRoleAuditPublisher}</li>
 *   <li>Persisting DB audit history via {@link AccessRoleAuditLogService}</li>
 * </ul>
 *
 * <p>All database reads are delegated to {@link AccessRoleAuditStateResolver},
 * which runs within a proper transactional context. This separation ensures
 * entity resolution always works regardless of AOP advice ordering.</p>
 *
 * <h3>Error Handling</h3>
 * <p>All audit logic is wrapped in try-catch. Audit failures are logged but
 * <strong>never</strong> propagated to the caller.</p>
 */
@Aspect
@Component
public class AccessRoleAuditAspect {

    private final AccessRoleAuditStateResolver stateResolver;
    private final AccessRoleAuditPublisher auditPublisher;
    private final AccessRoleAuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    /**
     * Constructs the aspect with its collaborators.
     *
     * @param stateResolver handles all DB reads within a transactional context
     * @param auditPublisher publishes audit events to Kafka
     * @param auditLogService persists audit events to the DB audit table
     * @param objectMapper Jackson mapper for JSON serialization
     * @param loggerFactory unified logger factory
     */
    public AccessRoleAuditAspect(
            AccessRoleAuditStateResolver stateResolver,
            AccessRoleAuditPublisher auditPublisher,
            AccessRoleAuditLogService auditLogService,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.stateResolver = stateResolver;
        this.auditPublisher = auditPublisher;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAuditConstants.LOG_CATEGORY);
    }

    @Around("@annotation(auditAnnotation)")
    public Object auditRoleChange(
            ProceedingJoinPoint joinPoint,
            AuditAccessRoleChange auditAnnotation) throws Throwable {

        return switch (auditAnnotation.action()) {
            case ROLE_CREATED -> handleRoleCreated(joinPoint, auditAnnotation);
            case ROLE_UPDATED -> handleRoleUpdated(joinPoint, auditAnnotation);
            case ROLE_DELETED -> handleRoleDeleted(joinPoint, auditAnnotation);
            case ENTITLEMENTS_UPDATED -> handleEntitlementsUpdated(joinPoint, auditAnnotation);
            case CHILDREN_UPDATED -> handleChildrenUpdated(joinPoint, auditAnnotation);
        };
    }

    /**
     * Handles audit for role creation.
     *
     * <p>After the role is created, enriches the created state with linked
     * entitlements and child roles and writes the audit event to both Kafka
     * and the persistent role audit log table.</p>
     */
    private Object handleRoleCreated(
            ProceedingJoinPoint joinPoint,
            AuditAccessRoleChange annotation) throws Throwable {

        String tenantId = extractTenantId(joinPoint, annotation);

        Object result = joinPoint.proceed();

        try {
            AccessRoleResponse response = (AccessRoleResponse) result;
            UUID roleId = response.getId();

            ObjectNode stateAfter = (ObjectNode) objectMapper.valueToTree(response);
            if (isImportTriggered()) {
                stateAfter.put(AccessRoleAuditConstants.STATE_FIELD_CREATED_THROUGH, AccessRoleAuditConstants.TRIGGER_IMPORT);
            }

            List<UUID> entitlementIds = stateResolver.getCurrentEntitlementIds(roleId);
            if (!entitlementIds.isEmpty()) {
                stateAfter.set("entitlements", stateResolver.buildEntitlementArray(entitlementIds));
            }

            List<UUID> childIds = stateResolver.getCurrentChildIds(roleId);
            if (!childIds.isEmpty()) {
                stateAfter.set("childRoles", stateResolver.buildChildRoleArray(childIds));
            }

            AccessRoleAuditEvent event = AccessRoleAuditEvent.roleCreated(
                    resolveActorId(), resolveActorUsername(), tenantId,
                    uuidToString(response.getId()),
                    response.getName(),
                    response.getType(),
                    response.getStatus(),
                    response.getRiskLevel(),
                    response.getBusinessUnit(),
                    uuidToString(response.getOwnerId()),
                    stateAfter
            );

            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAuditConstants.ACTION_ROLE_CREATED, "unknown");
        }

        return result;
    }

    /**
     * Handles audit for role metadata update.
     *
     * <p>Captures full role state before and after the update, but excludes
     * computed/transient fields that should not appear in metadata update audit.</p>
     */
    private Object handleRoleUpdated(
            ProceedingJoinPoint joinPoint,
            AuditAccessRoleChange annotation) throws Throwable {

        String tenantId = extractTenantId(joinPoint, annotation);
        UUID roleId = extractRoleId(joinPoint, annotation);

        JsonNode stateBefore = stateResolver.captureRoleState(roleId);
        if (stateBefore instanceof ObjectNode beforeObject) {
            beforeObject.remove("entitlementCount");
        }

        Object result = joinPoint.proceed();

        try {
            AccessRoleResponse response = (AccessRoleResponse) result;
            JsonNode stateAfter = objectMapper.valueToTree(response);

            if (stateAfter instanceof ObjectNode afterObject) {
                afterObject.remove("entitlementCount");
            }

            AccessRoleAuditEvent event = AccessRoleAuditEvent.roleUpdated(
                    resolveActorId(), resolveActorUsername(), tenantId,
                    uuidToString(response.getId()),
                    response.getName(),
                    response.getType(),
                    response.getStatus(),
                    response.getRiskLevel(),
                    response.getBusinessUnit(),
                    uuidToString(response.getOwnerId()),
                    stateBefore, stateAfter
            );

            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAuditConstants.ACTION_ROLE_UPDATED, roleId.toString());
        }

        return result;
    }

    /**
     * Handles audit for role deletion.
     */
    private Object handleRoleDeleted(
            ProceedingJoinPoint joinPoint,
            AuditAccessRoleChange annotation) throws Throwable {

        String tenantId = extractTenantId(joinPoint, annotation);
        UUID roleId = extractRoleId(joinPoint, annotation);

        JsonNode stateBefore = stateResolver.captureRoleState(roleId);
        Optional<AccessRole> roleOpt = stateResolver.findRole(roleId);
        String roleName = roleOpt.map(AccessRole::getName).orElse("Unknown");
        String roleType = roleOpt.map(AccessRole::getType).orElse("unknown");
        String riskLevel = roleOpt.map(AccessRole::getRiskLevel).orElse(null);
        String businessUnit = roleOpt.map(AccessRole::getBusinessUnit).orElse(null);
        String ownerId = roleOpt.map(r -> uuidToString(r.getOwnerId())).orElse(null);

        Object result = joinPoint.proceed();

        try {
            AccessRoleAuditEvent event = AccessRoleAuditEvent.roleDeleted(
                    resolveActorId(), resolveActorUsername(), tenantId,
                    roleId.toString(), roleName, roleType,
                    riskLevel, businessUnit, ownerId,
                    stateBefore
            );

            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAuditConstants.ACTION_ROLE_DELETED, roleId.toString());
        }

        return result;
    }

    /**
     * Handles audit for role entitlement desired-state sync.
     */
    private Object handleEntitlementsUpdated(
            ProceedingJoinPoint joinPoint,
            AuditAccessRoleChange annotation) throws Throwable {

        String tenantId = extractTenantId(joinPoint, annotation);
        UUID roleId = extractRoleId(joinPoint, annotation);

        List<UUID> entIdsBefore = stateResolver.getCurrentEntitlementIds(roleId);
        Set<UUID> beforeSet = new LinkedHashSet<>(entIdsBefore);
        JsonNode stateBefore = stateResolver.entitlementsToJson(entIdsBefore);

        Optional<AccessRole> roleOpt = stateResolver.findRole(roleId);
        String roleName = roleOpt.map(AccessRole::getName).orElse("Unknown");
        String roleType = roleOpt.map(AccessRole::getType).orElse("unknown");

        Object result = joinPoint.proceed();

        try {
            List<UUID> entIdsAfter = stateResolver.getCurrentEntitlementIds(roleId);
            Set<UUID> afterSet = new LinkedHashSet<>(entIdsAfter);
            JsonNode stateAfter = stateResolver.entitlementsToJson(entIdsAfter);

            List<UUID> actuallyAdded = entIdsAfter.stream()
                    .filter(id -> !beforeSet.contains(id))
                    .collect(Collectors.toList());

            List<UUID> actuallyRemoved = entIdsBefore.stream()
                    .filter(id -> !afterSet.contains(id))
                    .collect(Collectors.toList());

            List<AccessRoleAuditEvent.EntitlementAuditDetail> addedDetails =
                    stateResolver.resolveEntitlementDetails(actuallyAdded);

            List<AccessRoleAuditEvent.EntitlementAuditDetail> removedDetails =
                    stateResolver.resolveEntitlementDetails(actuallyRemoved);

            List<UUID> allInvolved = new ArrayList<>(actuallyAdded);
            allInvolved.addAll(actuallyRemoved);
            String highestRisk = stateResolver.resolveHighestRiskFromEntitlements(allInvolved);

            AccessRoleAuditEvent event = AccessRoleAuditEvent.entitlementsUpdated(
                    resolveActorId(), resolveActorUsername(), tenantId,
                    roleId.toString(), roleName, roleType,
                    highestRisk, addedDetails, removedDetails,
                    stateBefore, stateAfter
            );

            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAuditConstants.ACTION_ENTITLEMENTS_UPDATED, roleId.toString());
        }

        return result;
    }

    /**
     * Handles audit for role child composition desired-state sync.
     */
    private Object handleChildrenUpdated(
            ProceedingJoinPoint joinPoint,
            AuditAccessRoleChange annotation) throws Throwable {

        String tenantId = extractTenantId(joinPoint, annotation);
        UUID roleId = extractRoleId(joinPoint, annotation);

        List<UUID> childIdsBefore = stateResolver.getCurrentChildIds(roleId);
        Set<UUID> beforeSet = new LinkedHashSet<>(childIdsBefore);
        JsonNode stateBefore = stateResolver.childRolesToJson(childIdsBefore);

        Optional<AccessRole> parentOpt = stateResolver.findRole(roleId);
        String roleName = parentOpt.map(AccessRole::getName).orElse("Unknown");
        String roleType = parentOpt.map(AccessRole::getType).orElse("unknown");

        List<AccessRoleAuditEvent.ChildRoleAuditDetail> preResolvedRemoveCandidates =
                stateResolver.resolveChildRoleDetails(childIdsBefore);

        Object result = joinPoint.proceed();

        try {
            SyncChildRolesResultDto syncResult = (SyncChildRolesResultDto) result;

            List<UUID> childIdsAfter = stateResolver.getCurrentChildIds(roleId);
            Set<UUID> afterSet = new LinkedHashSet<>(childIdsAfter);
            JsonNode stateAfter = stateResolver.childRolesToJson(childIdsAfter);

            List<UUID> actuallyAdded = childIdsAfter.stream()
                    .filter(id -> !beforeSet.contains(id))
                    .collect(Collectors.toList());

            List<UUID> actuallyRemoved = childIdsBefore.stream()
                    .filter(id -> !afterSet.contains(id))
                    .collect(Collectors.toList());

            List<AccessRoleAuditEvent.ChildRoleAuditDetail> addedDetails =
                    stateResolver.resolveChildRoleDetails(actuallyAdded);

            Set<String> removedIdStrings = actuallyRemoved.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toSet());

            List<AccessRoleAuditEvent.ChildRoleAuditDetail> removedDetails =
                    preResolvedRemoveCandidates.stream()
                            .filter(d -> removedIdStrings.contains(d.id()))
                            .collect(Collectors.toList());

            List<AccessRoleAuditEvent.FailedChildRoleAuditDetail> failedDetails =
                    stateResolver.resolveFailedChildDetails(syncResult.getFailed());

            List<UUID> allInvolved = new ArrayList<>(actuallyAdded);
            allInvolved.addAll(actuallyRemoved);
            syncResult.getFailed().forEach(f -> allInvolved.add(f.getChildRoleId()));
            String highestRisk = stateResolver.resolveHighestRiskFromRoles(allInvolved);

            AccessRoleAuditEvent event = AccessRoleAuditEvent.childrenUpdated(
                    resolveActorId(), resolveActorUsername(), tenantId,
                    roleId.toString(), roleName, roleType,
                    highestRisk, addedDetails, removedDetails, failedDetails,
                    stateBefore, stateAfter
            );

            publishAndPersist(event);
        } catch (Exception e) {
            logAuditFailure(e, AccessRoleAuditConstants.ACTION_CHILDREN_UPDATED, roleId.toString());
        }

        return result;
    }

    /**
     * Publishes the audit event to Kafka and persists it to the DB audit table.
     *
     * @param event the fully assembled role audit event
     */
    private void publishAndPersist(AccessRoleAuditEvent event) {
        auditPublisher.publish(event);
        auditLogService.persist(event);
    }

    private String extractTenantId(ProceedingJoinPoint joinPoint, AuditAccessRoleChange annotation) {
        return (String) joinPoint.getArgs()[annotation.tenantIdParamIndex()];
    }

    private UUID extractRoleId(ProceedingJoinPoint joinPoint, AuditAccessRoleChange annotation) {
        int index = annotation.roleIdParamIndex();
        if (index < 0) {
            return null;
        }
        return (UUID) joinPoint.getArgs()[index];
    }

    private String resolveActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String resolveActorUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private boolean isImportTriggered() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && Objects.equals(auth.getDetails(), AccessRoleAuditConstants.TRIGGER_IMPORT);
    }

    private String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    private void logAuditFailure(Exception e, String action, String roleId) {
        logger.error()
                .category(AccessRoleAuditConstants.LOG_CATEGORY)
                .message(AccessRoleAuditConstants.LOG_AUDIT_PUBLISH_FAILED)
                .details(Map.of(
                        AccessRoleAuditConstants.DETAIL_ACTION, action,
                        AccessRoleAuditConstants.DETAIL_ROLE_ID, roleId,
                        AccessRoleAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName(),
                        "exceptionMessage", e.getMessage() != null ? e.getMessage() : "null"
                ))
                .log();
    }
}