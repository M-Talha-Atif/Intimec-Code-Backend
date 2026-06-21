package com.aidentx.iam.shadow_management.sync;

import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.events.IdentityEventPublisher;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.iam.identity_management.service.IdentityLifecycleStateMachine;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.aidentx.iam.identity_management.repository.HumanIdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.repository.NonEmployeeRepository;
import com.aidentx.iam.identity_management.repository.NonHumanIdentityRepository;
import com.aidentx.iam.organization_management.service.IdentityOrgSyncService;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Synchronization Reaction Handler — executes identity lifecycle actions
 * based on the synchronization situation of each shadow.
 * <p>
 * This follows the MidPoint synchronization reaction pattern:
 * <pre>
 *   Situation → Reaction → Identity Lifecycle Action
 *
 *   UNMATCHED  → addFocus     → Create new identity from inbound mappings + link
 *   UNLINKED   → linkFocus    → Link shadow to correlated identity + update attributes
 *   LINKED     → synchronize  → Update identity with fresh resource attributes
 *   DISPUTED   → (none)       → Flag for manual resolution
 *   DELETED    → unlinkFocus  → Unlink shadow + optionally disable identity
 * </pre>
 * <p>
 * Each reaction is atomic and transactional. Failed reactions don't affect
 * other shadows in the same sync run.
 */
@Component
public class SynchronizationReactionHandler {

    private final IUnifiedLogger logger;
    private final InboundMappingEngine mappingEngine;
    private final ExtendedAttributeService extendedAttributeService;
    private final EmployeeRepository employeeRepository;
    private final NonEmployeeRepository nonEmployeeRepository;
    private final NonHumanIdentityRepository nonHumanIdentityRepository;
    private final IdentityRepository identityRepository;
    private final HumanIdentityRepository humanIdentityRepository;
    private final ShadowRepository shadowRepository;
    private final IdentityEventPublisher identityEventPublisher;
    private final IdentityOrgSyncService identityOrgSyncService;

    public SynchronizationReactionHandler(
            UnifiedLoggerFactory loggerFactory,
            InboundMappingEngine mappingEngine,
            ExtendedAttributeService extendedAttributeService,
            EmployeeRepository employeeRepository,
            NonEmployeeRepository nonEmployeeRepository,
            NonHumanIdentityRepository nonHumanIdentityRepository,
            IdentityRepository identityRepository,
            HumanIdentityRepository humanIdentityRepository,
            ShadowRepository shadowRepository,
            IdentityEventPublisher identityEventPublisher,
            IdentityOrgSyncService identityOrgSyncService) {
        this.logger = loggerFactory.getLogger(SynchronizationReactionHandler.class);
        this.mappingEngine = mappingEngine;
        this.extendedAttributeService = extendedAttributeService;
        this.employeeRepository = employeeRepository;
        this.nonEmployeeRepository = nonEmployeeRepository;
        this.nonHumanIdentityRepository = nonHumanIdentityRepository;
        this.identityRepository = identityRepository;
        this.humanIdentityRepository = humanIdentityRepository;
        this.shadowRepository = shadowRepository;
        this.identityEventPublisher = identityEventPublisher;
        this.identityOrgSyncService = identityOrgSyncService;
    }

    /**
     * Phase 2 helper: after an identity save during inbound sync, mirror the
     * department string into a primary FUNCTIONAL org-unit assignment.
     * Best-effort — the underlying service swallows all errors so a failure
     * here can never break a shadow reconciliation.
     */
    private void syncOrgAssignmentIfEmployee(String tenantId, Identity identity) {
        if (identity instanceof Employee employee
                && employee.getId() != null
                && employee.getDepartment() != null
                && !employee.getDepartment().isBlank()) {
            identityOrgSyncService.syncPrimaryFunctionalAssignment(
                    tenantId, employee.getId(), employee.getDepartment(), "INBOUND_SYNC",
                    com.aidentx.iam.organization_management.model.enums.AssignmentType.HR_OF_RECORD);
        }
    }

    /**
     * Public accessor for the mapping engine — used by dry-run to compute attribute diffs.
     */
    public InboundMappingEngine getMappingEngine() {
        return mappingEngine;
    }

    /**
     * Public wrapper for resolveAction — used by dry-run to preview what reaction WOULD fire
     * without actually executing it.
     *
     * @param situation           The synchronization situation
     * @param configuredReactions Merged policy + task reactions (nullable)
     * @return The resolved action string (e.g., "add-focus", "link", "sync", "none")
     */
    public String resolveActionForPreview(
            Shadow.SynchronizationSituation situation,
            List<SynchronizationReactionDTO> configuredReactions) {
        return resolveAction(situation, configuredReactions);
    }

    // ========================================================================
    //  Reaction Result DTO
    // ========================================================================

    /**
     * Result of executing a synchronization reaction.
     */
    public record ReactionResult(
            ReactionOutcome outcome,
            UUID identityId,
            int attributesUpdated,
            String message
    ) {
        public enum ReactionOutcome {
            /** Identity was created and shadow linked */
            IDENTITY_CREATED,
            /** Shadow linked to existing identity + attributes updated */
            IDENTITY_LINKED,
            /** Identity attributes updated from fresh shadow data */
            IDENTITY_UPDATED,
            /** No identity changes (already up to date) */
            NO_CHANGE,
            /** Reaction skipped (no mappings, disputed, etc.) */
            SKIPPED,
            /** Reaction failed (duplicate, validation error, etc.) */
            FAILED
        }
    }

    // ========================================================================
    //  Public API — Situation-Based Reactions
    // ========================================================================

    /**
     * Executes the appropriate synchronization reaction based on the shadow's situation.
     * Resolves the action from configured reactions (policy-driven), falling back to
     * hardcoded defaults if no configuration exists.
     *
     * @param shadow             The shadow being processed
     * @param situation          The classified synchronization situation
     * @param matchedIds         Correlated identity IDs (from correlation engine)
     * @param tenantId           Tenant context
     * @param resourceId         Resource (application) ID
     * @param identityType       Target identity type
     * @param shadowAttrs        Shadow attributes (connector data)
     * @param configuredReactions Policy-driven reactions (nullable — falls back to defaults)
     * @return Result describing what action was taken
     */
    @Transactional
    public ReactionResult executeReaction(
            Shadow shadow,
            Shadow.SynchronizationSituation situation,
            List<UUID> matchedIds,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs,
            List<SynchronizationReactionDTO> configuredReactions) {

        // Resolve the action for this situation from configuration or defaults
        String action = resolveAction(situation, configuredReactions);

        logger.info().message("Executing synchronization reaction")
                .detail("shadowId", shadow.getId().toString())
                .detail("situation", situation.name())
                .detail("resolvedAction", action)
                .detail("configuredReactionsPresent", configuredReactions != null && !configuredReactions.isEmpty())
                .log();

        // If action is "none", skip entirely
        if (SynchronizationReactionDTO.ACTION_NONE.equals(action)) {
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.SKIPPED, null, 0,
                    "Reaction skipped per configuration: " + situation.name() + " → none");
        }

        // Dispatch to the appropriate reaction method based on resolved action
        return switch (action) {
            case SynchronizationReactionDTO.ACTION_ADD_FOCUS ->
                    reactToUnmatched(shadow, tenantId, resourceId, identityType, shadowAttrs);
            case SynchronizationReactionDTO.ACTION_LINK ->
                    reactToUnlinked(shadow, matchedIds, tenantId, resourceId, identityType, shadowAttrs);
            case SynchronizationReactionDTO.ACTION_SYNC ->
                    reactToLinked(shadow, tenantId, resourceId, identityType, shadowAttrs);
            case SynchronizationReactionDTO.ACTION_UNLINK ->
                    reactToDeleted(shadow);
            case SynchronizationReactionDTO.ACTION_INACTIVATE_FOCUS ->
                    reactToDeletedWithInactivation(shadow);
            case SynchronizationReactionDTO.ACTION_DELETE_FOCUS ->
                    reactToDeletedWithDeletion(shadow);
            default -> {
                logger.warn().message("Unknown reaction action, skipping")
                        .detail("action", action)
                        .detail("situation", situation.name())
                        .log();
                yield new ReactionResult(
                        ReactionResult.ReactionOutcome.SKIPPED, null, 0,
                        "Unknown action: " + action);
            }
        };
    }

    /**
     * Backward-compatible overload — uses hardcoded defaults (no configured reactions).
     */
    @Transactional
    public ReactionResult executeReaction(
            Shadow shadow,
            Shadow.SynchronizationSituation situation,
            List<UUID> matchedIds,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {
        return executeReaction(shadow, situation, matchedIds, tenantId, resourceId,
                identityType, shadowAttrs, null);
    }

    /**
     * Executes deletion reaction during reconciliation Phase B.
     * Called for shadows whose resource objects no longer exist.
     *
     * @param shadow             The shadow to mark as deleted
     * @param configuredReactions Policy-driven reactions (nullable — falls back to defaults)
     * @return Result describing what action was taken
     */
    @Transactional
    public ReactionResult executeDeletedReaction(Shadow shadow,
                                                  List<SynchronizationReactionDTO> configuredReactions) {
        String action = resolveAction(Shadow.SynchronizationSituation.DELETED, configuredReactions);

        if (SynchronizationReactionDTO.ACTION_NONE.equals(action)) {
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.SKIPPED, null, 0,
                    "DELETED reaction skipped per configuration");
        }

        return switch (action) {
            case SynchronizationReactionDTO.ACTION_UNLINK -> reactToDeleted(shadow);
            case SynchronizationReactionDTO.ACTION_INACTIVATE_FOCUS -> reactToDeletedWithInactivation(shadow);
            case SynchronizationReactionDTO.ACTION_DELETE_FOCUS -> reactToDeletedWithDeletion(shadow);
            default -> reactToDeleted(shadow);
        };
    }

    /**
     * Backward-compatible overload — uses hardcoded defaults (no configured reactions).
     */
    @Transactional
    public ReactionResult executeDeletedReaction(Shadow shadow) {
        return executeDeletedReaction(shadow, null);
    }

    // ========================================================================
    //  UNMATCHED Reaction: Create New Identity
    // ========================================================================

    /**
     * No matching identity found → Create a new identity from inbound mappings.
     * <p>
     * MidPoint equivalent: "addFocus" reaction.
     */
    private ReactionResult reactToUnmatched(
            Shadow shadow,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        logger.info().message("UNMATCHED reaction: creating new identity from import")
                .detail("shadowId", shadow.getId().toString())
                .detail("resourceUid", shadow.getResourceUid())
                .detail("identityType", identityType.name())
                .log();

        // Step 1: Use mapping engine to build the identity (polymorphic)
        Optional<Identity> maybeIdentity = mappingEngine.createIdentityFromShadow(
                tenantId, resourceId, identityType, shadowAttrs);

        if (maybeIdentity.isEmpty()) {
            logger.warn().message("Cannot create identity — insufficient mappings or data")
                    .detail("shadowId", shadow.getId().toString())
                    .log();
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.SKIPPED,
                    null, 0,
                    "Insufficient attribute mappings to create identity");
        }

        Identity identity = maybeIdentity.get();

        // Step 2: Check for duplicates — if found, opportunistically link
        // (MidPoint addFocus behavior: fall back to linking when identity already exists)
        Identity existingIdentity = findExistingIdentity(tenantId, identity, identityType);
        if (existingIdentity != null) {
            logger.info().message("Duplicate detected — opportunistically linking shadow to existing identity")
                    .detail("shadowId", shadow.getId().toString())
                    .detail("existingIdentityId", existingIdentity.getId().toString())
                    .log();

            shadow.setLinkedIdentityId(existingIdentity.getId());
            shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.LINKED);
            shadowRepository.save(shadow);

            // Update identity attributes from inbound mappings
            int changed = mappingEngine.updateIdentityFromShadow(
                    existingIdentity, tenantId, resourceId, identityType, shadowAttrs);
            if (changed > 0) {
                identityRepository.save(existingIdentity);
            }
            syncOrgAssignmentIfEmployee(tenantId, existingIdentity);

            saveExtendedAttributesForSync(existingIdentity.getId(), tenantId, resourceId, identityType, shadowAttrs);

            return new ReactionResult(
                    ReactionResult.ReactionOutcome.IDENTITY_LINKED,
                    existingIdentity.getId(), changed,
                    "Opportunistically linked to existing identity (duplicate detected)");
        }

        try {
            // Step 3: Persist the new identity via the base repository —
            // JPA JOINED inheritance handles routing to the correct child table.
            // UUID is generated client-side (UUID.randomUUID()), so save() returns the
            // entity with ID immediately — no flush needed.
            Identity saved = identityRepository.save(identity);

            // Step 4: Link the shadow to the new identity
            shadow.setLinkedIdentityId(saved.getId());
            shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.LINKED);
            shadowRepository.save(shadow);

            // Phase 2: mirror department -> primary FUNCTIONAL org-unit assignment.
            syncOrgAssignmentIfEmployee(tenantId, saved);

            // Step 5: Extract and save extended attributes
            Map<String, Object> extAttrs = mappingEngine.extractExtendedAttributes(
                    tenantId, resourceId, identityType, shadowAttrs);
            if (!extAttrs.isEmpty()) {
                try {
                    extendedAttributeService.saveExtendedAttributesForCreation(
                            saved.getId(), tenantId, identityType, extAttrs);
                    logger.info().message("Saved extended attributes for new identity")
                            .detail("identityId", saved.getId().toString())
                            .detail("extendedAttributeCount", String.valueOf(extAttrs.size()))
                            .log();
                } catch (Exception ex) {
                    // Non-fatal: extended attribute save failure should not block identity creation.
                    // Definitions may not exist yet — attributes will be saved on next sync
                    // once the admin defines them in the schema.
                    logger.warn().message("Failed to save extended attributes for new identity")
                            .detail("identityId", saved.getId().toString())
                            .detail("error", ex.getMessage())
                            .log();
                }
            }

            String identityLabel = describeIdentity(saved, identityType);

            logger.info().message("Identity created and linked from import")
                    .detail("identityId", saved.getId().toString())
                    .detail("shadowId", shadow.getId().toString())
                    .detail("identityType", identityType.name())
                    .detail("label", identityLabel)
                    .log();

            // Publish lifecycle event so auth-service provisions a login account for this identity.
            // This mirrors the event published by EmployeeCreationStrategy for API-created identities.
            // Non-human identities are skipped by the auth-service consumer automatically.
            identityEventPublisher.publishIdentityCreatedEvent(
                    saved, "SYSTEM_RECONCILIATION", "application-reconciliation");

            return new ReactionResult(
                    ReactionResult.ReactionOutcome.IDENTITY_CREATED,
                    saved.getId(), 0,
                    "Created " + identityType.name() + " identity: " + identityLabel);

        } catch (Exception e) {
            logger.error().message("Failed to create identity from import")
                    .detail("shadowId", shadow.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.FAILED,
                    null, 0,
                    "Identity creation failed: " + e.getMessage());
        }
    }

    // ========================================================================
    //  UNLINKED Reaction: Link + Update
    // ========================================================================

    /**
     * Correlation found exactly one match but shadow isn't linked yet → Link + update.
     * <p>
     * MidPoint equivalent: "linkFocus" reaction.
     */
    private ReactionResult reactToUnlinked(
            Shadow shadow,
            List<UUID> matchedIds,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        if (matchedIds == null || matchedIds.isEmpty()) {
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.SKIPPED, null, 0,
                    "No matched identity to link");
        }

        UUID identityId = matchedIds.get(0);

        logger.info().message("UNLINKED reaction: linking shadow to identity")
                .detail("shadowId", shadow.getId().toString())
                .detail("identityId", identityId.toString())
                .log();

        Optional<Identity> maybeIdentity = identityRepository.findByTenantIdAndId(tenantId, identityId);
        if (maybeIdentity.isEmpty()) {
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.FAILED, null, 0,
                    "Matched identity not found: " + identityId);
        }

        Identity identity = maybeIdentity.get();

        // Link the shadow
        shadow.setLinkedIdentityId(identityId);
        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.LINKED);
        shadowRepository.save(shadow);

        // Update identity attributes from inbound mappings
        int changed = mappingEngine.updateIdentityFromShadow(
                identity, tenantId, resourceId, identityType, shadowAttrs);

        if (changed > 0) {
            identityRepository.save(identity);
        }
        syncOrgAssignmentIfEmployee(tenantId, identity);

        // Update extended attributes
        saveExtendedAttributesForSync(identityId, tenantId, resourceId, identityType, shadowAttrs);

        logger.info().message("Shadow linked and identity updated")
                .detail("identityId", identityId.toString())
                .detail("shadowId", shadow.getId().toString())
                .detail("attributesUpdated", String.valueOf(changed))
                .log();

        // Publish identity.updated so auth-service can provision a user account if one was
        // never created (e.g., identity was imported before the reconciliation event fix).
        // The auth-service consumer gracefully creates the user when it doesn't exist.
        identityEventPublisher.publishIdentityUpdatedEvent(
                identity, "SYSTEM_RECONCILIATION");

        return new ReactionResult(
                ReactionResult.ReactionOutcome.IDENTITY_LINKED,
                identityId, changed,
                "Linked to identity and updated " + changed + " attributes");
    }

    // ========================================================================
    //  LINKED Reaction: Update Identity Attributes
    // ========================================================================

    /**
     * Shadow already linked → Update the linked identity with fresh resource attributes.
     * <p>
     * MidPoint equivalent: "synchronize" reaction (inbound attribute refresh).
     */
    private ReactionResult reactToLinked(
            Shadow shadow,
            String tenantId,
            String resourceId,
            IdentityType identityType,
            Map<String, Object> shadowAttrs) {

        UUID identityId = shadow.getLinkedIdentityId();
        if (identityId == null) {
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.SKIPPED, null, 0,
                    "Shadow marked LINKED but has no linkedIdentityId");
        }

        Optional<Identity> maybeIdentity = identityRepository.findByTenantIdAndId(tenantId, identityId);
        if (maybeIdentity.isEmpty()) {
            logger.warn().message("Linked identity not found — unlinking shadow")
                    .detail("shadowId", shadow.getId().toString())
                    .detail("identityId", identityId.toString())
                    .log();
            shadow.setLinkedIdentityId(null);
            shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.UNMATCHED);
            shadowRepository.save(shadow);
            return new ReactionResult(
                    ReactionResult.ReactionOutcome.FAILED, null, 0,
                    "Linked identity " + identityId + " not found — shadow unlinked");
        }

        Identity identity = maybeIdentity.get();

        // Don't update disabled identities from connector (preserve admin decisions)
        // But DO still refresh the shadow attributes

        int changed = mappingEngine.updateIdentityFromShadow(
                identity, tenantId, resourceId, identityType, shadowAttrs);

        if (changed > 0) {
            identityRepository.save(identity);
            logger.info().message("Linked identity attributes updated from resource")
                    .detail("identityId", identityId.toString())
                    .detail("attributesUpdated", String.valueOf(changed))
                    .log();
            // Publish identity.updated whenever attributes change — the auth-service consumer
            // keeps user fields in sync, and will create the user if one doesn't exist yet
            // (covers identities imported before the lifecycle event fix was deployed).
            identityEventPublisher.publishIdentityUpdatedEvent(
                    identity, "SYSTEM_RECONCILIATION");
        }
        syncOrgAssignmentIfEmployee(tenantId, identity);

        // Update extended attributes on every sync — values may have changed even if
        // core fields didn't. This ensures the EAV layer stays in sync with the source.
        saveExtendedAttributesForSync(identityId, tenantId, resourceId, identityType, shadowAttrs);

        return new ReactionResult(
                changed > 0 ? ReactionResult.ReactionOutcome.IDENTITY_UPDATED
                        : ReactionResult.ReactionOutcome.NO_CHANGE,
                identityId, changed,
                changed > 0 ? "Updated " + changed + " attributes"
                        : "Identity already up to date");
    }

    // ========================================================================
    //  DISPUTED Reaction: Flag for Manual Resolution
    // ========================================================================

    /**
     * Multiple correlation matches → do NOT auto-act, flag for admin review.
     */
    private ReactionResult reactToDisputed(Shadow shadow, List<UUID> matchedIds) {
        int candidateCount = matchedIds != null ? matchedIds.size() : 0;

        logger.warn().message("DISPUTED: multiple correlation matches — manual resolution required")
                .detail("shadowId", shadow.getId().toString())
                .detail("resourceUid", shadow.getResourceUid())
                .detail("candidateCount", String.valueOf(candidateCount))
                .log();

        // Shadow situation already set to DISPUTED by the correlation engine
        return new ReactionResult(
                ReactionResult.ReactionOutcome.SKIPPED,
                null, 0,
                "Disputed: " + candidateCount + " candidate identities — manual resolution required");
    }

    // ========================================================================
    //  DELETED Reaction: Unlink + Optionally Disable
    // ========================================================================

    /**
     * Extracts and saves extended attributes for an identity during sync (link or update).
     * Non-fatal: failures are logged but don't block the sync pipeline.
     * This ensures attribute definitions that don't exist yet are handled gracefully —
     * they will be persisted once the admin configures them in the schema.
     */
    private void saveExtendedAttributesForSync(
            UUID identityId, String tenantId, String resourceId,
            IdentityType identityType, Map<String, Object> shadowAttrs) {
        Map<String, Object> extAttrs = mappingEngine.extractExtendedAttributes(
                tenantId, resourceId, identityType, shadowAttrs);
        if (extAttrs.isEmpty()) {
            return;
        }
        try {
            extendedAttributeService.saveExtendedAttributesForUpdate(
                    identityId, tenantId, identityType, extAttrs);
            logger.debug().message("Updated extended attributes during sync")
                    .detail("identityId", identityId.toString())
                    .detail("extendedAttributeCount", String.valueOf(extAttrs.size()))
                    .log();
        } catch (Exception ex) {
            logger.warn().message("Failed to save extended attributes during sync")
                    .detail("identityId", identityId.toString())
                    .detail("error", ex.getMessage())
                    .log();
        }
    }

    /**
     * Account deleted from resource → Unlink shadow and optionally disable identity.
     * <p>
     * MidPoint equivalent: "deleteFocus" / "unlinkFocus" reaction.
     */
    private ReactionResult reactToDeleted(Shadow shadow) {
        UUID identityId = shadow.getLinkedIdentityId();

        // Mark shadow as DELETED
        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.DELETED);

        if (identityId != null) {
            // Unlink the shadow from its identity
            shadow.setLinkedIdentityId(null);

            // Disable the linked identity (policy-driven in production, always disable for now)
            Optional<Identity> maybeIdentity = identityRepository.findById(identityId);
            if (maybeIdentity.isPresent()) {
                Identity identity = maybeIdentity.get();

                // Only disable if the identity has no other linked shadows
                // (an identity may be linked to multiple resources)
                long otherShadows = shadowRepository.countByLinkedIdentityIdAndTenantId(
                        identityId, identity.getTenantId());

                if (otherShadows == 0) {
                    if (IdentityLifecycleStateMachine.isValidTransition(identity.getStatus(), IdentityStatus.INACTIVE)) {
                        identity.setStatus(IdentityStatus.INACTIVE);
                        identityRepository.save(identity);

                        logger.info().message("Identity disabled — last linked account deleted from resource")
                                .detail("identityId", identityId.toString())
                                .detail("shadowId", shadow.getId().toString())
                                .log();
                    } else {
                        logger.warn().message("Skipping INACTIVE transition — invalid lifecycle transition")
                                .detail("identityId", identityId.toString())
                                .detail("currentStatus", identity.getStatus().name())
                                .detail("targetStatus", IdentityStatus.INACTIVE.name())
                                .log();
                    }
                } else {
                    logger.info().message("Identity kept active — has other linked accounts")
                            .detail("identityId", identityId.toString())
                            .detail("otherShadows", String.valueOf(otherShadows))
                            .log();
                }
            }
        }

        shadowRepository.save(shadow);

        logger.info().message("DELETED reaction: shadow marked as deleted")
                .detail("shadowId", shadow.getId().toString())
                .detail("previousIdentityId", identityId != null ? identityId.toString() : "none")
                .log();

        return new ReactionResult(
                ReactionResult.ReactionOutcome.IDENTITY_UPDATED,
                identityId, 0,
                "Account deleted from resource — shadow unlinked" +
                        (identityId != null ? ", identity checked for disable" : ""));
    }

    // ========================================================================
    //  Internal: Duplicate Detection + Opportunistic Identity Lookup
    // ========================================================================

    /**
     * Finds an existing identity that would conflict with the given new identity.
     * Used for opportunistic linking: when addFocus detects a duplicate, we link
     * the shadow to the existing identity instead of failing.
     *
     * @return The existing identity if a duplicate is found, or null if no conflict.
     */
    private Identity findExistingIdentity(String tenantId, Identity identity, IdentityType identityType) {
        return findExistingIdentityInternal(tenantId, identity, identityType);
    }

    /**
     * Public read-only accessor for dry-run preview.
     * Allows the dry-run path to check whether opportunistic linking would occur.
     */
    public Identity findExistingIdentityForPreview(String tenantId, Identity identity, IdentityType identityType) {
        return findExistingIdentityInternal(tenantId, identity, identityType);
    }

    private Identity findExistingIdentityInternal(String tenantId, Identity identity, IdentityType identityType) {
        // HumanIdentity types — check username first, then email
        if (identity instanceof HumanIdentity human) {
            if (human.getUsername() != null) {
                if (identityType == IdentityType.EMPLOYEE) {
                    Optional<Employee> existing = employeeRepository.findByTenantIdAndUsername(tenantId, human.getUsername());
                    if (existing.isPresent()) return existing.get();
                } else if (identityType == IdentityType.NON_EMPLOYEE) {
                    Optional<NonEmployee> existing = nonEmployeeRepository.findByTenantIdAndUsername(tenantId, human.getUsername());
                    if (existing.isPresent()) return existing.get();
                }
            }
        }

        // Type-specific uniqueness (tenant-scoped)
        if (identity instanceof Employee employee && employee.getEmployeeId() != null) {
            Optional<Employee> existing = employeeRepository.findByTenantIdAndEmployeeId(tenantId, employee.getEmployeeId());
            if (existing.isPresent()) return existing.get();
        } else if (identity instanceof NonEmployee nonEmployee && nonEmployee.getContractId() != null) {
            Optional<NonEmployee> existing = nonEmployeeRepository.findByTenantIdAndContractId(tenantId, nonEmployee.getContractId());
            if (existing.isPresent()) return existing.get();
        } else if (identity instanceof NonHumanIdentity nhi && nhi.getServiceName() != null) {
            Optional<NonHumanIdentity> existing = nonHumanIdentityRepository.findByTenantIdAndServiceName(tenantId, nhi.getServiceName());
            if (existing.isPresent()) return existing.get();
        }

        return null;
    }

    /**
     * Produces a human-readable label for a saved identity based on its type.
     */
    private String describeIdentity(Identity identity, IdentityType identityType) {
        if (identity instanceof Employee emp) {
            return emp.getUsername() + " (employeeId: " + emp.getEmployeeId() + ")";
        } else if (identity instanceof NonEmployee ne) {
            return ne.getUsername() + " (contractId: " + ne.getContractId() + ")";
        } else if (identity instanceof NonHumanIdentity nhi) {
            return nhi.getServiceName();
        }
        return identity.getId().toString();
    }

    // ========================================================================
    //  Action Resolution — Policy-Driven with Defaults
    // ========================================================================

    /**
     * Resolves the action for a given situation from configured reactions.
     * Falls back to sensible defaults if no configuration exists.
     *
     * <p>Default mappings (align with MidPoint standard behavior):
     * <ul>
     *   <li>UNMATCHED → add-focus (create identity)</li>
     *   <li>UNLINKED → link (link + update)</li>
     *   <li>LINKED → sync (update attributes)</li>
     *   <li>DISPUTED → none (skip, require manual)</li>
     *   <li>DELETED → unlink (unlink + conditionally disable)</li>
     * </ul>
     */
    private String resolveAction(Shadow.SynchronizationSituation situation,
                                  List<SynchronizationReactionDTO> configuredReactions) {
        String situationKey = situation.name().toLowerCase();

        if (configuredReactions != null && !configuredReactions.isEmpty()) {
            for (SynchronizationReactionDTO reaction : configuredReactions) {
                if (reaction.situation() != null && reaction.situation().equalsIgnoreCase(situationKey)) {
                    return reaction.action();
                }
            }
        }

        // Hardcoded defaults
        return switch (situation) {
            case UNMATCHED -> SynchronizationReactionDTO.ACTION_ADD_FOCUS;
            case UNLINKED -> SynchronizationReactionDTO.ACTION_LINK;
            case LINKED -> SynchronizationReactionDTO.ACTION_SYNC;
            case DISPUTED -> SynchronizationReactionDTO.ACTION_NONE;
            case DELETED -> SynchronizationReactionDTO.ACTION_UNLINK;
        };
    }

    // ========================================================================
    //  DELETED Variant: Force-Inactivate Identity
    // ========================================================================

    /**
     * Account deleted → unlink shadow AND always inactivate the identity,
     * regardless of remaining linked shadows.
     */
    private ReactionResult reactToDeletedWithInactivation(Shadow shadow) {
        UUID identityId = shadow.getLinkedIdentityId();

        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.DELETED);

        if (identityId != null) {
            shadow.setLinkedIdentityId(null);
            Optional<Identity> maybeIdentity = identityRepository.findById(identityId);
            if (maybeIdentity.isPresent()) {
                Identity identity = maybeIdentity.get();
                if (IdentityLifecycleStateMachine.isValidTransition(identity.getStatus(), IdentityStatus.INACTIVE)) {
                    identity.setStatus(IdentityStatus.INACTIVE);
                    identityRepository.save(identity);

                    logger.info().message("DELETED+INACTIVATE: identity forcibly disabled")
                            .detail("identityId", identityId.toString())
                            .detail("shadowId", shadow.getId().toString())
                            .log();
                } else {
                    logger.warn().message("DELETED+INACTIVATE: skipping — invalid lifecycle transition")
                            .detail("identityId", identityId.toString())
                            .detail("currentStatus", identity.getStatus().name())
                            .detail("targetStatus", IdentityStatus.INACTIVE.name())
                            .log();
                }
            }
        }

        shadowRepository.save(shadow);
        return new ReactionResult(
                ReactionResult.ReactionOutcome.IDENTITY_UPDATED,
                identityId, 0,
                "Account deleted — shadow unlinked, identity inactivated");
    }

    // ========================================================================
    //  DELETED Variant: Delete Identity Entirely
    // ========================================================================

    /**
     * Account deleted → unlink shadow AND delete the identity entity.
     * Only used when configured explicitly (destructive action).
     */
    private ReactionResult reactToDeletedWithDeletion(Shadow shadow) {
        UUID identityId = shadow.getLinkedIdentityId();

        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.DELETED);

        if (identityId != null) {
            shadow.setLinkedIdentityId(null);

            // Only delete if identity has no other linked shadows
            long otherShadows = shadowRepository.countByLinkedIdentityIdAndTenantId(
                    identityId, shadow.getTenantId());

            if (otherShadows == 0) {
                identityRepository.deleteById(identityId);
                logger.info().message("DELETED+DELETE_FOCUS: identity permanently removed")
                        .detail("identityId", identityId.toString())
                        .detail("shadowId", shadow.getId().toString())
                        .log();
            } else {
                logger.info().message("DELETED+DELETE_FOCUS: identity kept — has other linked shadows")
                        .detail("identityId", identityId.toString())
                        .detail("otherShadows", String.valueOf(otherShadows))
                        .log();
            }
        }

        shadowRepository.save(shadow);
        return new ReactionResult(
                ReactionResult.ReactionOutcome.IDENTITY_UPDATED,
                identityId, 0,
                "Account deleted — shadow unlinked" +
                        (identityId != null ? ", identity deletion evaluated" : ""));
    }
}
