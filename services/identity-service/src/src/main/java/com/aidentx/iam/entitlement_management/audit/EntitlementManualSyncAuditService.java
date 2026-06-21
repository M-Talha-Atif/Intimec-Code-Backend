package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentResult;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncEntitlementMemberFailure;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncEntitlementMemberResult;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersResponse;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;
import com.aidentx.iam.entitlement_management.repository.EntitlementAuditLogRepository;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Builds, stores, and publishes audit records for manual entitlement member syncs. */
@Service
public class EntitlementManualSyncAuditService {

    private record ActorDetails(String actorId, String username) {
    }

    private final EntitlementAuditLogRepository auditLogRepository;
    private final EntitlementManualSyncAuditPublisher publisher;
    private final IdentityRepository identityRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public EntitlementManualSyncAuditService(EntitlementAuditLogRepository auditLogRepository,
                                             EntitlementManualSyncAuditPublisher publisher,
                                             IdentityRepository identityRepository,
                                             ObjectMapper objectMapper,
                                             UnifiedLoggerFactory loggerFactory) {
        this.auditLogRepository = auditLogRepository;
        this.publisher = publisher;
        this.identityRepository = identityRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementAuditConstants.LOG_CATEGORY);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditManualSync(String tenantId,
                                Entitlement entitlement,
                                SyncManualEntitlementMembersResponse response,
                                String requestedBy) {
        try {
            if (entitlement == null || response == null) {
                return;
            }

            Map<UUID, Identity> identitiesById = loadIdentityDetails(tenantId, response);
            ActorDetails actor = resolveActor(tenantId, requestedBy);
            String entitlementName = friendlyLabel(entitlement.getDisplayName());
            String riskLevel = resolveEffectiveRiskLevel(identitiesById.values());

            List<EntitlementManualSyncAuditEvent.MemberAuditDetail> added = mapMembers(response.added(), identitiesById);
            List<EntitlementManualSyncAuditEvent.MemberAuditDetail> revoked = mapMembers(response.revoked(), identitiesById);
            List<EntitlementManualSyncAuditEvent.MemberAuditDetail> skipped = mapMembers(response.skipped(), identitiesById);
            List<EntitlementManualSyncAuditEvent.FailedMemberAuditDetail> failed = mapFailures(response.failed(), identitiesById);

            Map<String, Object> state = new LinkedHashMap<>();
            state.put(EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID, response.entitlementId());
            state.put(EntitlementAuditConstants.DETAIL_ENTITLEMENT_NAME, entitlementName);
            state.put(EntitlementAuditConstants.DETAIL_RISK_LEVEL,
                    riskLevel != null ? riskLevel : EntitlementAuditConstants.DETAIL_NOT_AVAILABLE);
            state.put(EntitlementAuditConstants.DETAIL_ADDED_COUNT, response.addedCount());
            state.put(EntitlementAuditConstants.DETAIL_REMOVED_COUNT, response.revokedCount());
            state.put(EntitlementAuditConstants.DETAIL_SKIPPED_COUNT, response.skippedCount());
            state.put(EntitlementAuditConstants.DETAIL_FAILED_COUNT, response.failedCount());
            state.put(EntitlementAuditConstants.DETAIL_TOTAL_DIRECT_COUNT, response.totalDirectCount());
            state.put(EntitlementAuditConstants.DETAIL_ADDED_MEMBERS, added);
            state.put(EntitlementAuditConstants.DETAIL_REMOVED_MEMBERS, revoked);
            state.put(EntitlementAuditConstants.DETAIL_SKIPPED_MEMBERS, skipped);
            state.put(EntitlementAuditConstants.DETAIL_FAILED_MEMBERS, failed);
            JsonNode stateAfter = objectMapper.valueToTree(state);

            EntitlementManualSyncAuditEvent event = EntitlementManualSyncAuditEvent.summary(
                    actor.actorId(),
                    actor.username(),
                    tenantId,
                    String.valueOf(response.entitlementId()),
                    entitlementName,
                    entitlement.getResourceId() != null ? entitlement.getResourceId().toString() : null,
                    riskLevel,
                    response.addedCount(),
                    response.revokedCount(),
                    response.skippedCount(),
                    response.failedCount(),
                    response.totalDirectCount(),
                    added,
                    revoked,
                    skipped,
                    failed,
                    stateAfter);

            EntitlementAuditLog saved = auditLogRepository.save(event.toAuditLog());
            logger.info()
                    .category(EntitlementAuditConstants.LOG_CATEGORY)
                    .message("Persisted entitlement manual member sync audit log")
                    .details(Map.of(
                            EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID, String.valueOf(response.entitlementId()),
                            "auditLogId", String.valueOf(saved.getId())))
                    .log();
            publisher.publish(event);
        } catch (Exception ex) {
            logger.error()
                    .category(EntitlementAuditConstants.LOG_CATEGORY)
                    .message("Failed to audit entitlement manual member sync")
                    .details(Map.of(
                            EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID,
                            response != null ? String.valueOf(response.entitlementId()) : EntitlementAuditConstants.DETAIL_NOT_AVAILABLE,
                            EntitlementAuditConstants.DETAIL_EXCEPTION_TYPE, ex.getClass().getSimpleName()))
                    .log();
        }
    }

    private Map<UUID, Identity> loadIdentityDetails(String tenantId, SyncManualEntitlementMembersResponse response) {
        Set<UUID> ids = Stream.of(
                        idsFromResults(response.added()),
                        idsFromResults(response.revoked()),
                        idsFromResults(response.skipped()),
                        idsFromFailures(response.failed()))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return identityRepository.findByTenantIdAndIdIn(tenantId, ids).stream()
                .collect(Collectors.toMap(Identity::getId, Function.identity(), (left, right) -> left));
    }

    private List<UUID> idsFromResults(List<SyncEntitlementMemberResult> results) {
        return Optional.ofNullable(results).orElse(List.of()).stream()
                .map(SyncEntitlementMemberResult::identityId)
                .toList();
    }

    private List<UUID> idsFromFailures(List<SyncEntitlementMemberFailure> failures) {
        return Optional.ofNullable(failures).orElse(List.of()).stream()
                .map(SyncEntitlementMemberFailure::identityId)
                .toList();
    }

    private List<EntitlementManualSyncAuditEvent.MemberAuditDetail> mapMembers(
            List<SyncEntitlementMemberResult> members,
            Map<UUID, Identity> identitiesById) {
        return Optional.ofNullable(members).orElse(List.of()).stream()
                .sorted(Comparator.comparing(member -> String.valueOf(member.identityId())))
                .map(member -> {
                    Identity identity = identitiesById.get(member.identityId());
                    return new EntitlementManualSyncAuditEvent.MemberAuditDetail(
                            String.valueOf(member.identityId()),
                            identityName(identity, member.identityId()),
                            identityType(identity),
                            mapAssignments(member.assignments()));
                })
                .toList();
    }

    private List<EntitlementManualSyncAuditEvent.FailedMemberAuditDetail> mapFailures(
            List<SyncEntitlementMemberFailure> failures,
            Map<UUID, Identity> identitiesById) {
        return Optional.ofNullable(failures).orElse(List.of()).stream()
                .sorted(Comparator.comparing(failure -> String.valueOf(failure.identityId())))
                .map(failure -> {
                    Identity identity = identitiesById.get(failure.identityId());
                    return new EntitlementManualSyncAuditEvent.FailedMemberAuditDetail(
                            String.valueOf(failure.identityId()),
                            identityName(identity, failure.identityId()),
                            identityType(identity),
                            failure.reason());
                })
                .toList();
    }

    private List<EntitlementManualSyncAuditEvent.AssignmentAuditDetail> mapAssignments(
            List<ManualEntitlementAssignmentResult> assignments) {
        return Optional.ofNullable(assignments).orElse(List.of()).stream()
                .map(assignment -> new EntitlementManualSyncAuditEvent.AssignmentAuditDetail(
                        assignment.assignmentId() != null ? assignment.assignmentId().toString() : null,
                        assignment.shadowId() != null ? assignment.shadowId().toString() : null,
                        assignment.assignmentStatus(),
                        assignment.action()))
                .toList();
    }

    private String identityName(Identity identity, UUID fallbackId) {
        if (identity instanceof HumanIdentity human) {
            return Stream.of(human.getDisplayName(), human.getUsername(), human.getEmail())
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .map(this::friendlyLabel)
                    .orElse(String.valueOf(fallbackId));
        }
        if (identity instanceof NonHumanIdentity nonHuman && nonHuman.getServiceName() != null) {
            return friendlyLabel(nonHuman.getServiceName());
        }
        return String.valueOf(fallbackId);
    }

    private String identityType(Identity identity) {
        return identity != null && identity.getIdentityType() != null ? identity.getIdentityType().name() : null;
    }

    private String resolveEffectiveRiskLevel(Collection<Identity> identities) {
        return Optional.ofNullable(identities).orElse(List.of()).stream()
                .map(this::identityRiskLevel)
                .map(this::normalizeRiskLevel)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(this::riskPriority))
                .orElse(AppConstants.AUDIT_RISK_LOW);
    }

    private String identityRiskLevel(Identity identity) {
        if (identity instanceof HumanIdentity human) {
            return human.getRiskLevel();
        }
        if (identity instanceof NonHumanIdentity nonHuman) {
            return nonHuman.getRiskLevel();
        }
        return null;
    }

    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        String normalized = riskLevel.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case AppConstants.AUDIT_RISK_HIGH -> AppConstants.AUDIT_RISK_HIGH;
            case AppConstants.AUDIT_RISK_MEDIUM -> AppConstants.AUDIT_RISK_MEDIUM;
            case AppConstants.AUDIT_RISK_LOW -> AppConstants.AUDIT_RISK_LOW;
            default -> null;
        };
    }

    private int riskPriority(String riskLevel) {
        return switch (riskLevel) {
            case AppConstants.AUDIT_RISK_HIGH -> 3;
            case AppConstants.AUDIT_RISK_MEDIUM -> 2;
            case AppConstants.AUDIT_RISK_LOW -> 1;
            default -> 0;
        };
    }

    private ActorDetails resolveActor(String tenantId, String requestedBy) {
        String requestedActorId = requestedBy != null && !requestedBy.isBlank() ? requestedBy.trim() : null;
        String authenticatedUsername = resolveAuthenticatedUsername();

        if (requestedActorId != null) {
            String username = authenticatedUsername != null
                    ? authenticatedUsername
                    : resolveIdentityActorName(tenantId, requestedActorId).orElse(friendlyLabel(requestedActorId));
            return new ActorDetails(requestedActorId, username);
        }

        if (authenticatedUsername != null) {
            return new ActorDetails(authenticatedUsername, authenticatedUsername);
        }

        return new ActorDetails(AppConstants.AUDIT_ACTOR_SYSTEM_ID, AppConstants.AUDIT_ACTOR_SYSTEM_USERNAME);
    }

    private String resolveAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return friendlyLabel(authentication.getName().trim());
        }
        return null;
    }

    private Optional<String> resolveIdentityActorName(String tenantId, String actorId) {
        if (tenantId == null || tenantId.isBlank() || actorId == null || !isUuid(actorId)) {
            return Optional.empty();
        }
        try {
            return identityRepository.findByTenantIdAndId(tenantId, UUID.fromString(actorId))
                    .map(identity -> identityName(identity, UUID.fromString(actorId)));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String friendlyLabel(String value) {
        if (value == null || value.isBlank() || value.contains("@") || isUuid(value.trim())) {
            return value;
        }
        String trimmed = value.trim().replace('_', ' ').replace('-', ' ');
        boolean normalizeCase = value.equals(value.toUpperCase(Locale.ROOT)) || value.contains("_") || value.contains("-");
        String base = normalizeCase ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
        return Stream.of(base.split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(token -> token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1))
                .collect(Collectors.joining(" "));
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}

