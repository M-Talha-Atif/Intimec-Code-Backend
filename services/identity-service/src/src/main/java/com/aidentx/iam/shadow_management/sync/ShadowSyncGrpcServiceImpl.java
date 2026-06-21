package com.aidentx.iam.shadow_management.sync;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.aidentx.dal.protos.shadow.*;
import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos;
import com.aidentx.iam.entitlement_management.service.EntitlementService;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.organization_management.model.enums.AssignmentType;
import com.aidentx.iam.organization_management.service.IdentityOrgSyncService;
import com.aidentx.iam.policy.service.ResourceGovernancePolicyService;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * gRPC Service implementing the Shadow Synchronization Pipeline.
 *
 * <p>This is the heart of the IGA import/reconciliation engine, following MidPoint's
 * synchronization patterns. For each resource object discovered during a connector
 * import task, this service:
 * <ol>
 *   <li>Creates or updates the shadow (persistent representation of the resource object)</li>
 *   <li>Evaluates correlation rules against existing identities</li>
 *   <li>Links the shadow to a matching identity (sets synchronization situation accordingly)</li>
 * </ol>
 *
 * <p>The task-management-service (orchestrator) calls this service via gRPC for each
 * account discovered during a scheduled import task.
 */
@GrpcService
public class ShadowSyncGrpcServiceImpl extends ShadowSyncServiceGrpc.ShadowSyncServiceImplBase {

    /**
     * Number of virtual threads processing records concurrently within a batch.
     * Bounded by the DB connection pool (HikariCP) — virtual threads park when
     * all connections are in use, so this controls max DB parallelism.
     *
     * Industry standard (MidPoint/SailPoint): 8-16 parallel workers per node.
     */
    private static final int PARALLEL_WORKERS = 10;

    private final ShadowRepository shadowRepository;
    private final IdentityRepository identityRepository;
    private final EmployeeRepository employeeRepository;
    private final ResourceGovernancePolicyService policyService;
    private final SynchronizationReactionHandler reactionHandler;
    private final EntitlementService entitlementService;
    private final ShadowAttributeChangeTracker changeTracker;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final IdentityOrgSyncService identityOrgSyncService;

    // Cache for sync reactions - same policy applies to all objects from same resource
    // Key: "tenantId|resourceId|identityType", Value: cached reactions (null means no reactions configured)
    private final ConcurrentHashMap<String, Optional<List<SynchronizationReactionDTO>>> reactionsCache = new ConcurrentHashMap<>();

    // Cache for full governance policy (correlation rules + attribute mappings)
    // Key: "tenantId|resourceId|identityType", Value: cached policy
    private final ConcurrentHashMap<String, Optional<IdentityMappingPolicyDTO>> policyCache = new ConcurrentHashMap<>();

    // G1: Entitlement catalog cache - sourceUid → entitlementId mapping per resource
    // Key: "tenantId|resourceId", Value: Map of sourceUid → entitlementId
    private final ConcurrentHashMap<String, Map<String, UUID>> entitlementCatalogCache = new ConcurrentHashMap<>();

    // G2: Assignment keys cache - tracks existing (shadowId, entitlementId) pairs per resource
    // Key: "tenantId|resourceId", Value: Set of "shadowId|entitlementId" strings
    private final ConcurrentHashMap<String, Set<String>> assignmentKeysCache = new ConcurrentHashMap<>();

    // G5: Entitlement sync dedup cache - tracks entitlements already processed in current sync run
    // Key: "tenantId|resourceId|sourceUid", Value: true (presence in set = already processed)
    // Prevents redundant DB lookups when connector sends duplicate group entries
    private final ConcurrentHashMap<String, Boolean> entitlementSyncDedupCache = new ConcurrentHashMap<>();

    // Tracks which syncRunIds have already initialized (cleared stale dedup entries).
    // On the first batch of a new sync run, we purge dedup keys for that resource so
    // entitlements from the prior run don't get silently skipped and miss their
    // lastSyncRunId stamp — which would cause the cleanup phase to delete them.
    private final ConcurrentHashMap<String, Boolean> initializedSyncRuns = new ConcurrentHashMap<>();

    // Tracks entitlement UIDs seen during a dry-run preview, keyed by
    // "tenantId|resourceId|syncRunId". Populated in processImportedObjectBatch
    // (dryRun=true, kind=entitlement) and consumed + cleared in finalizeSyncRun
    // (dryRun=true). Avoids false-positive "would be deleted" counts because
    // dry-run never stamps lastSyncRunId on entitlement rows.
    private final ConcurrentHashMap<String, Set<String>> dryRunEntitlementCache = new ConcurrentHashMap<>();

    // Identity creation serialization locks — prevents duplicate identity creation
    // when parallel batch workers process accounts with the same natural key concurrently.
    // Key: natural key (e.g., "tenant|username" or "tenant|employeeId")
    // MidPoint: single-threaded per resource. SailPoint: optimistic locking with retry.
    // Our approach: fine-grained per-identity locking (allows parallelism for different identities).
    private final ConcurrentHashMap<String, Object> identityCreationLocks = new ConcurrentHashMap<>();

    public ShadowSyncGrpcServiceImpl(
            ShadowRepository shadowRepository,
            IdentityRepository identityRepository,
            EmployeeRepository employeeRepository,
            ResourceGovernancePolicyService policyService,
            SynchronizationReactionHandler reactionHandler,
            EntitlementService entitlementService,
            ShadowAttributeChangeTracker changeTracker,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            IdentityOrgSyncService identityOrgSyncService) {
        this.shadowRepository = shadowRepository;
        this.identityRepository = identityRepository;
        this.employeeRepository = employeeRepository;
        this.policyService = policyService;
        this.reactionHandler = reactionHandler;
        this.entitlementService = entitlementService;
        this.changeTracker = changeTracker;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(ShadowSyncGrpcServiceImpl.class);
        this.identityOrgSyncService = identityOrgSyncService;
    }

    // ===================================================================
    //  ProcessImportedObject - Main sync pipeline entry point
    // ===================================================================

    @Override
    public void processImportedObject(
            ProcessImportedObjectRequest request,
            StreamObserver<ProcessImportedObjectResponse> responseObserver) {
        try {
            validateRequest(request);

            boolean dryRun = request.getDryRun();
            String tenantId = request.getTenantId();
            String resourceId = request.getResourceId();
            String resourceUid = request.getResourceUid();
            String objectClass = request.getObjectClass();
            Map<String, Object> attributes = structToMap(request.getAttributes());

            // Schema-based attribute filtering: if schema attribute names were provided,
            // retain only those attributes (plus system keys __UID__, __NAME__).
            // This ensures only schema-defined fields are stored — enterprise data hygiene.
            List<String> schemaAttrNames = request.getSchemaAttributeNamesList();
            if (schemaAttrNames != null && !schemaAttrNames.isEmpty()) {
                Set<String> allowedKeys = new HashSet<>(schemaAttrNames);
                // Always keep system identifiers — these are required for pipeline operation
                allowedKeys.add("__UID__");
                allowedKeys.add("__NAME__");
                attributes.keySet().retainAll(allowedKeys);
            }

            logger.info().message(dryRun ? "DRY-RUN: Processing imported object" : "Processing imported object")
                    .detail("tenantId", tenantId)
                    .detail("resourceId", resourceId)
                    .detail("resourceUid", resourceUid)
                    .detail("objectClass", objectClass)
                    .detail("dryRun", String.valueOf(dryRun))
                    .log();

            if (dryRun) {
                // Entitlement dry-run: skip identity correlation, just check shadow existence
                if ("entitlement".equalsIgnoreCase(request.getKind()) || "__GROUP__".equals(objectClass)) {
                    Optional<Shadow> existingShadow = shadowRepository
                            .findByResourceIdAndResourceUidAndTenantId(resourceId, resourceUid, tenantId);
                    boolean shadowExists = existingShadow.isPresent();
                    SynchronizationAction action = shadowExists
                            ? SynchronizationAction.SHADOW_UPDATED
                            : SynchronizationAction.SHADOW_CREATED;
                    responseObserver.onNext(ProcessImportedObjectResponse.newBuilder()
                            .setSuccess(true)
                            .setShadowId(shadowExists ? existingShadow.get().getId().toString() : "new-shadow-preview")
                            .setResourceUid(resourceUid)
                            .setSituation(SynchronizationSituation.LINKED)
                            .setAction(action)
                            .setMessage(String.format("DRY-RUN: Entitlement %s from %s",
                                    shadowExists ? "would update" : "would create", objectClass))
                            .build());
                    responseObserver.onCompleted();
                    return;
                }

                // Account dry-run: full correlation pipeline without persisting
                ProcessImportedObjectResponse response = processDryRun(
                        request, tenantId, resourceId, resourceUid, objectClass, attributes);
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // REAL execution (unchanged)

            // Step 1: Create or update shadow
            ShadowResult shadowResult = createOrUpdateShadow(
                    tenantId, resourceId, resourceUid, objectClass,
                    request.getKind(), attributes, request.getSyncRunId());

            // Step 1.5: Entitlement routing — objects with kind "entitlement" represent
            // entitlements (groups, roles, permissions, etc.). They get a shadow (for audit
            // trail) but skip identity correlation. Instead, we populate the entitlement catalog.
            // Supports both the legacy __GROUP__ object class and any custom entitlement class
            // configured in the application's schema.
            if ("entitlement".equalsIgnoreCase(request.getKind()) || "__GROUP__".equals(objectClass)) {
                processEntitlementObject(tenantId, resourceId, resourceUid, objectClass,
                        request.getKind(), attributes, request.getSyncRunId(), shadowResult,
                        request.getDisplayAttribute(), request.getDescriptionAttribute());
                responseObserver.onNext(ProcessImportedObjectResponse.newBuilder()
                        .setSuccess(true)
                        .setShadowId(shadowResult.shadow.getId().toString())
                        .setResourceUid(resourceUid)
                        .setSituation(SynchronizationSituation.LINKED)
                        .setAction(shadowResult.action == SynchronizationAction.SHADOW_CREATED
                                ? SynchronizationAction.SHADOW_CREATED
                                : SynchronizationAction.SHADOW_UPDATED)
                        .setMessage("Entitlement cataloged from " + objectClass)
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Step 1.6: Parse membership attributes (memberOf/groups) for __ACCOUNT__ objects.
            // This resolves group references to entitlement catalog entries and creates
            // EntitlementAssignment records. Unresolved references (group not yet imported)
            // are logged and will be resolved in the next import cycle.
            // G1: Use cached entitlement catalog map for O(1) lookups instead of DB query per membership
            UUID resourceUUID = UUID.fromString(resourceId);
            String catalogCacheKey = tenantId + "|" + resourceId;
            Map<String, UUID> entitlementCatalog = entitlementCatalogCache.computeIfAbsent(
                    catalogCacheKey,
                    key -> entitlementService.loadEntitlementCatalogMap(resourceUUID, tenantId)
            );
            entitlementService.processAccountMembershipsWithCache(
                    shadowResult.shadow.getId(),
                    resourceUUID,
                    tenantId,
                    request.getSyncRunId(),
                    attributes,
                    entitlementCatalog);

            // Step 2: If shadow is already LINKED, update attributes via inbound mappings
            if (shadowResult.shadow.getSynchronizationSituation() == Shadow.SynchronizationSituation.LINKED
                    && shadowResult.shadow.getLinkedIdentityId() != null) {

                // Determine if we should execute reactions (IMPORT/RECONCILIATION)
                boolean executeReactions = isReactionTaskType(request.getTaskType());

                if (executeReactions) {
                    // Execute LINKED reaction: update identity attributes from fresh connector data
                    String identityType = request.getIdentityType();
                    IdentityType idType = resolveIdentityType(identityType);
                    List<SynchronizationReactionDTO> policyReactions = loadConfiguredReactions(
                            tenantId, resourceId, identityType);
                    List<SynchronizationReactionDTO> taskReactions = convertProtoReactions(
                            request.getTaskReactionsList());
                    List<SynchronizationReactionDTO> reactions = mergeReactions(taskReactions, policyReactions);
                    // Compute attribute changes BEFORE the reaction executes
                    // (reaction mutates the identity, so diff must be captured first)
                    Optional<Identity> preReactionIdentity = identityRepository.findByTenantIdAndId(
                            tenantId, shadowResult.shadow.getLinkedIdentityId());
                    List<InboundMappingEngine.AttributeChange> attrChanges = preReactionIdentity.isPresent()
                            ? reactionHandler.getMappingEngine().computeAttributeChanges(
                                    preReactionIdentity.get(), tenantId, resourceId, idType, attributes)
                            : Collections.emptyList();

                    // Resolve which reaction fires (for the Reaction column)
                    String resolvedReaction = reactionHandler.resolveActionForPreview(
                            Shadow.SynchronizationSituation.LINKED, reactions);

                    SynchronizationReactionHandler.ReactionResult reactionResult =
                            reactionHandler.executeReaction(
                                    shadowResult.shadow,
                                    Shadow.SynchronizationSituation.LINKED,
                                    List.of(shadowResult.shadow.getLinkedIdentityId()),
                                    tenantId, resourceId, idType, attributes, reactions);

                    SynchronizationAction action = reactionResult.outcome() ==
                            SynchronizationReactionHandler.ReactionResult.ReactionOutcome.IDENTITY_UPDATED
                            ? SynchronizationAction.IDENTITY_UPDATED
                            : SynchronizationAction.ALREADY_LINKED;

                    ProcessImportedObjectResponse.Builder respBuilder = ProcessImportedObjectResponse.newBuilder()
                            .setSuccess(true)
                            .setShadowId(shadowResult.shadow.getId().toString())
                            .setSituation(SynchronizationSituation.LINKED)
                            .setAction(action)
                            .setMessage(reactionResult.message())
                            .setAttributesUpdatedCount(reactionResult.attributesUpdated())
                            .setResolvedReaction(resolvedReaction);

                    for (InboundMappingEngine.AttributeChange change : attrChanges) {
                        respBuilder.addAttributeChanges(AttributeChange.newBuilder()
                                .setFieldName(change.fieldName())
                                .setOldValue(change.oldValue())
                                .setNewValue(change.newValue())
                                .build());
                    }

                    if (shadowResult.shadow.getLinkedIdentityId() != null) {
                        respBuilder.setLinkedIdentityId(shadowResult.shadow.getLinkedIdentityId().toString());
                    }

                    responseObserver.onNext(respBuilder.build());
                    responseObserver.onCompleted();
                    return;
                }

                // No reactions to execute — just log and return
                logger.info().message("Shadow already linked, updated attributes")
                        .detail("shadowId", shadowResult.shadow.getId())
                        .detail("identityId", shadowResult.shadow.getLinkedIdentityId())
                        .log();

                responseObserver.onNext(buildResponse(
                        true,
                        shadowResult.shadow,
                        SynchronizationSituation.LINKED,
                        SynchronizationAction.ALREADY_LINKED,
                        "Shadow already linked to identity, attributes updated"));
                responseObserver.onCompleted();
                return;
            }

            // Step 3: Run correlation - find matching identities
            String identityType = request.getIdentityType();
            List<Identity> matches = runCorrelation(tenantId, resourceId, identityType, attributes);

            // Step 4: Determine situation and take action based on correlation results
            boolean executeReactions = isReactionTaskType(request.getTaskType());
            List<SyncReaction> taskReactionsProto = request.getTaskReactionsList();
            ProcessImportedObjectResponse response = processCorrelationResults(
                    shadowResult.shadow, matches, shadowResult.action,
                    executeReactions, tenantId, resourceId, identityType, attributes, taskReactionsProto);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            logger.warn().message("Invalid request: " + e.getMessage()).log();
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            logger.error(e).message("Failed to process imported object").log();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to process imported object: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ===================================================================
    //  DRY-RUN: Full pipeline evaluation without persistence
    // ===================================================================

    /**
     * Executes the full sync pipeline in dry-run mode:
     * 1. Checks if a shadow already exists (read-only lookup)
     * 2. Runs correlation against existing identities (read-only queries)
     * 3. Classifies situation (LINKED/UNMATCHED/UNLINKED/DISPUTED)
     * 4. Resolves which reaction WOULD fire from policy + task config
     * 5. Computes attribute changes that WOULD be applied
     * 6. Returns everything in the response WITHOUT persisting any changes
     */
    @Transactional(readOnly = true)
    private ProcessImportedObjectResponse processDryRun(
            ProcessImportedObjectRequest request,
            String tenantId, String resourceId, String resourceUid,
            String objectClass, Map<String, Object> attributes) {

        ProcessImportedObjectResponse.Builder builder = ProcessImportedObjectResponse.newBuilder()
                .setSuccess(true);

        // Step 1: Check if shadow already exists (read-only)
        Optional<Shadow> existingShadow = shadowRepository
                .findByResourceIdAndResourceUidAndTenantId(resourceId, resourceUid, tenantId);

        boolean shadowExists = existingShadow.isPresent();
        SynchronizationAction baseAction = shadowExists
                ? SynchronizationAction.SHADOW_UPDATED
                : SynchronizationAction.SHADOW_CREATED;

        builder.setAction(baseAction);

        if (shadowExists) {
            Shadow shadow = existingShadow.get();
            builder.setShadowId(shadow.getId().toString());

            // If shadow is already LINKED, compute update diff
            if (shadow.getSynchronizationSituation() == Shadow.SynchronizationSituation.LINKED
                    && shadow.getLinkedIdentityId() != null) {

                builder.setSituation(SynchronizationSituation.LINKED);
                builder.setLinkedIdentityId(shadow.getLinkedIdentityId().toString());

                // Resolve what reaction WOULD fire
                String identityType = request.getIdentityType();
                List<SynchronizationReactionDTO> policyReactions = loadConfiguredReactions(
                        tenantId, resourceId, identityType);
                List<SynchronizationReactionDTO> taskReactions = convertProtoReactions(
                        request.getTaskReactionsList());
                List<SynchronizationReactionDTO> reactions = mergeReactions(taskReactions, policyReactions);
                String resolvedAction = reactionHandler.resolveActionForPreview(
                        Shadow.SynchronizationSituation.LINKED, reactions);
                builder.setResolvedReaction(resolvedAction);

                // Compute attribute diff
                IdentityType idType = resolveIdentityType(identityType);
                Optional<Identity> maybeIdentity = identityRepository.findByTenantIdAndId(
                        tenantId, shadow.getLinkedIdentityId());
                if (maybeIdentity.isPresent()) {
                    List<InboundMappingEngine.AttributeChange> changes =
                            reactionHandler.getMappingEngine().computeAttributeChanges(
                                    maybeIdentity.get(), tenantId, resourceId, idType, attributes);
                    for (InboundMappingEngine.AttributeChange change : changes) {
                        builder.addAttributeChanges(AttributeChange.newBuilder()
                                .setFieldName(change.fieldName())
                                .setOldValue(change.oldValue())
                                .setNewValue(change.newValue())
                                .build());
                    }
                    builder.setAttributesUpdatedCount(changes.size());

                    // Match real execution: if attributes would change, action is IDENTITY_UPDATED
                    if (!changes.isEmpty()) {
                        builder.setAction(SynchronizationAction.IDENTITY_UPDATED);
                    }
                }

                builder.setMessage(String.format("DRY-RUN: Shadow exists, LINKED. Would execute '%s' reaction, %d attribute changes",
                        resolvedAction, builder.getAttributeChangesCount()));
                return builder.build();
            }
        } else {
            builder.setShadowId("new-shadow-preview");
        }

        // Step 2: Run correlation (read-only — queries only)
        String identityType = request.getIdentityType();
        List<Identity> matches = runCorrelation(tenantId, resourceId, identityType, attributes);

        // Step 3: Classify situation
        Shadow.SynchronizationSituation situation;
        if (matches.isEmpty()) {
            situation = Shadow.SynchronizationSituation.UNMATCHED;
        } else if (matches.size() == 1) {
            situation = Shadow.SynchronizationSituation.UNLINKED;
        } else {
            situation = Shadow.SynchronizationSituation.DISPUTED;
        }

        builder.setSituation(mapSituation(situation));

        // Step 4: Resolve the reaction that WOULD fire
        List<SynchronizationReactionDTO> policyReactions = loadConfiguredReactions(
                tenantId, resourceId, identityType);
        List<SynchronizationReactionDTO> taskReactions = convertProtoReactions(
                request.getTaskReactionsList());
        List<SynchronizationReactionDTO> reactions = mergeReactions(taskReactions, policyReactions);
        String resolvedAction = reactionHandler.resolveActionForPreview(situation, reactions);
        builder.setResolvedReaction(resolvedAction);

        // Step 5: Compute what WOULD happen based on situation + reaction
        IdentityType idType = resolveIdentityType(identityType);

        switch (situation) {
            case UNMATCHED -> {
                // Check for opportunistic linking: if a duplicate identity already exists,
                // real execution would link to it instead of creating a new one.
                // This mirrors the reactToUnmatched behavior post-fix.
                Optional<Identity> maybeNewIdentity = reactionHandler.getMappingEngine()
                        .createIdentityFromShadow(tenantId, resourceId, idType, attributes);
                Identity existingDuplicate = null;
                if (maybeNewIdentity.isPresent()) {
                    existingDuplicate = reactionHandler.findExistingIdentityForPreview(
                            tenantId, maybeNewIdentity.get(), idType);
                }

                if (existingDuplicate != null) {
                    // Would opportunistically link instead of creating
                    builder.setAction(SynchronizationAction.IDENTITY_LINKED)
                            .setLinkedIdentityId(existingDuplicate.getId().toString());
                    List<InboundMappingEngine.AttributeChange> changes =
                            reactionHandler.getMappingEngine().computeAttributeChanges(
                                    existingDuplicate, tenantId, resourceId, idType, attributes);
                    for (InboundMappingEngine.AttributeChange change : changes) {
                        builder.addAttributeChanges(AttributeChange.newBuilder()
                                .setFieldName(change.fieldName())
                                .setOldValue(change.oldValue())
                                .setNewValue(change.newValue())
                                .build());
                    }
                    builder.setAttributesUpdatedCount(changes.size());
                    builder.setMessage(String.format(
                            "DRY-RUN: No correlation match, but duplicate exists (%s). Would opportunistically link + update %d attributes",
                            existingDuplicate.getId(), changes.size()));
                } else {
                    builder.setAction(SynchronizationAction.IDENTITY_CREATED);
                    // Compute what identity would be created
                    List<InboundMappingEngine.AttributeChange> changes =
                            reactionHandler.getMappingEngine().computeCreationAttributes(
                                    tenantId, resourceId, idType, attributes);
                    for (InboundMappingEngine.AttributeChange change : changes) {
                        builder.addAttributeChanges(AttributeChange.newBuilder()
                                .setFieldName(change.fieldName())
                                .setOldValue("")
                                .setNewValue(change.newValue())
                                .build());
                    }
                    builder.setMessage(String.format(
                            "DRY-RUN: No match. Would '%s' → create identity with %d attributes",
                            resolvedAction, changes.size()));
                }
            }
            case UNLINKED -> {
                Identity matchedIdentity = matches.get(0);
                builder.setAction(SynchronizationAction.IDENTITY_LINKED)
                        .setLinkedIdentityId(matchedIdentity.getId().toString());
                // Compute attribute diff against matched identity
                List<InboundMappingEngine.AttributeChange> changes =
                        reactionHandler.getMappingEngine().computeAttributeChanges(
                                matchedIdentity, tenantId, resourceId, idType, attributes);
                for (InboundMappingEngine.AttributeChange change : changes) {
                    builder.addAttributeChanges(AttributeChange.newBuilder()
                            .setFieldName(change.fieldName())
                            .setOldValue(change.oldValue())
                            .setNewValue(change.newValue())
                            .build());
                }
                builder.setAttributesUpdatedCount(changes.size());
                builder.setMessage(String.format(
                        "DRY-RUN: Match found (%s). Would '%s' → link + update %d attributes",
                        matchedIdentity.getId(), resolvedAction, changes.size()));
            }
            case DISPUTED -> {
                builder.setAction(SynchronizationAction.DISPUTED_MATCH);
                String matchIds = matches.stream()
                        .map(i -> i.getId().toString())
                        .collect(Collectors.joining(", "));
                builder.setMessage(String.format(
                        "DRY-RUN: %d matches found (DISPUTED). Would '%s'. Candidates: %s",
                        matches.size(), resolvedAction, matchIds));
            }
            default -> builder.setMessage("DRY-RUN: Unknown situation");
        }

        return builder.build();
    }

    // ===================================================================
    //  Batch ProcessImportedObject — Parallel Virtual Thread Processing
    //
    //  Industry-standard IGA import architecture (MidPoint, SailPoint):
    //  - Each record processed through full pipeline independently
    //  - Multiple worker threads process records in parallel
    //  - Transaction per record (via @Transactional on reaction handler)
    //  - Parallelism bounded by DB connection pool (prevents saturation)
    //
    //  With 10 parallel virtual threads:
    //  100K records × ~21ms/record ÷ 10 workers = ~210 seconds (~3.5 min)
    // ===================================================================

    @Override
    public void processImportedObjectBatch(
            ProcessImportedObjectBatchRequest request,
            StreamObserver<ProcessImportedObjectBatchResponse> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            String resourceId = request.getResourceId();

            // On the first entitlement batch of a new sync run, clear stale dedup cache entries
            // for this resource. Without this, entries from a prior run cause entitlements to be
            // skipped (not stamped with the new syncRunId) and then deleted during cleanup.
            if (("entitlement".equalsIgnoreCase(request.getKind()) || "__GROUP__".equals(request.getObjectClass()))
                    && !request.getSyncRunId().isEmpty()) {
                String runKey = tenantId + "|" + resourceId + "|" + request.getSyncRunId();
                if (initializedSyncRuns.putIfAbsent(runKey, Boolean.TRUE) == null) {
                    String prefix = tenantId + "|" + resourceId + "|";
                    entitlementSyncDedupCache.keySet().removeIf(k -> k.startsWith(prefix));
                    logger.debug().message("Cleared entitlement dedup cache for new sync run")
                            .detail("resourceId", resourceId)
                            .detail("syncRunId", request.getSyncRunId())
                            .log();
                }
            }

            // Dry-run entitlement tracking: record every UID processed so finalizeSyncRun
            // can exclude them from the "would be deleted" query (dry-run never stamps rows).
            if (request.getDryRun()
                    && ("entitlement".equalsIgnoreCase(request.getKind()) || "__GROUP__".equals(request.getObjectClass()))
                    && !request.getSyncRunId().isEmpty()) {
                String cacheKey = tenantId + "|" + resourceId + "|" + request.getSyncRunId();
                Set<String> uidSet = dryRunEntitlementCache.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet());
                for (BatchObjectEntry entry : request.getObjectsList()) {
                    uidSet.add(entry.getResourceUid());
                }
            }

            // Build all single-object requests upfront
            List<BatchObjectEntry> entries = request.getObjectsList();
            List<Future<ProcessImportedObjectResponse>> futures = new ArrayList<>(entries.size());

            // Use a semaphore to bound parallelism to PARALLEL_WORKERS.
            // Virtual threads park (not block) when waiting — no platform thread waste.
            // This prevents DB connection pool exhaustion (HikariCP default: 10).
            Semaphore parallelismGate = new Semaphore(PARALLEL_WORKERS);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (BatchObjectEntry entry : entries) {
                    ProcessImportedObjectRequest singleRequest = ProcessImportedObjectRequest.newBuilder()
                            .setTenantId(tenantId)
                            .setResourceId(resourceId)
                            .setResourceUid(entry.getResourceUid())
                            .setObjectClass(request.getObjectClass())
                            .setKind(request.getKind())
                            .setIdentityType(request.getIdentityType())
                            .setAttributes(entry.getAttributes())
                            .setSyncRunId(request.getSyncRunId())
                            .setTaskType(request.getTaskType())
                            .addAllTaskReactions(request.getTaskReactionsList())
                            .setDryRun(request.getDryRun())
                            .setDisplayAttribute(request.getDisplayAttribute())
                            .setDescriptionAttribute(request.getDescriptionAttribute())
                            .addAllSchemaAttributeNames(request.getSchemaAttributeNamesList())
                            .build();

                    String resourceUid = entry.getResourceUid();

                    futures.add(executor.submit(() -> {
                        parallelismGate.acquire();
                        try {
                            CapturingObserver<ProcessImportedObjectResponse> captor = new CapturingObserver<>();
                            processImportedObject(singleRequest, captor);

                            ProcessImportedObjectResponse result = captor.getValue();
                            if (result != null) {
                                return result.toBuilder()
                                        .setResourceUid(resourceUid)
                                        .build();
                            } else if (captor.getError() != null) {
                                return ProcessImportedObjectResponse.newBuilder()
                                        .setSuccess(false)
                                        .setResourceUid(resourceUid)
                                        .setMessage("Failed: " + captor.getError().getMessage())
                                        .build();
                            }
                            return ProcessImportedObjectResponse.newBuilder()
                                    .setSuccess(false)
                                    .setResourceUid(resourceUid)
                                    .setMessage("No result produced")
                                    .build();
                        } finally {
                            parallelismGate.release();
                        }
                    }));
                }
            }
            // executor.close() blocks until all virtual threads complete

            // Collect results
            List<ProcessImportedObjectResponse> results = new ArrayList<>(futures.size());
            int successCount = 0;
            int errorCount = 0;

            for (Future<ProcessImportedObjectResponse> future : futures) {
                try {
                    ProcessImportedObjectResponse result = future.get();
                    results.add(result);
                    if (result.getSuccess()) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                } catch (ExecutionException e) {
                    errorCount++;
                    results.add(ProcessImportedObjectResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Worker failed: " + e.getCause().getMessage())
                            .build());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorCount++;
                    results.add(ProcessImportedObjectResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Worker interrupted")
                            .build());
                }
            }

            responseObserver.onNext(ProcessImportedObjectBatchResponse.newBuilder()
                    .setTotalProcessed(results.size())
                    .setSuccessCount(successCount)
                    .setErrorCount(errorCount)
                    .addAllResults(results)
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e).message("Batch processing failed").log();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Batch processing failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    /**
     * A simple StreamObserver that captures the first value or error.
     * Used internally to delegate batch items to the existing single-object pipeline.
     */
    private static class CapturingObserver<T> implements StreamObserver<T> {
        private T value;
        private Throwable error;

        @Override
        public void onNext(T v) {
            this.value = v;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            // no-op
        }

        public T getValue() {
            return value;
        }

        public Throwable getError() {
            return error;
        }
    }

    // ===================================================================
    //  FinalizeSyncRun - Mark absent shadows as DELETED
    // ===================================================================

    @Override
    @Transactional
    public void finalizeSyncRun(
            FinalizeSyncRunRequest request,
            StreamObserver<FinalizeSyncRunResponse> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            String resourceId = request.getResourceId();
            String syncRunId = request.getSyncRunId();
            SyncTaskType taskType = request.getTaskType();
            boolean dryRun = request.getDryRun();

            // For dry-run reconciliation: the caller passes every UID it processed
            // because processDryRun is @Transactional(readOnly=true) and never writes
            // lastSyncRunId to shadow rows.  Without this exclusion list every shadow
            // in the DB would appear "absent" and produce a false-positive deletion preview.
            java.util.Set<String> seenUidsForDryRun = (dryRun && !request.getSeenResourceUidsList().isEmpty())
                    ? new java.util.HashSet<>(request.getSeenResourceUidsList())
                    : java.util.Collections.emptySet();

            // Clear all caches at the end of a sync run so the next run picks up
            // any policy/mapping changes made between runs. Without this, stale
            // correlation rules, reaction configs, and attribute mappings persist
            // in the singleton gRPC service across runs.
            clearCaches();
            reactionHandler.getMappingEngine().clearMappingsCache();

            // Paginated deletion detection: process absent shadows in pages of 500 to prevent OOM.
            // If an entire application (100K accounts) is decommissioned, we never load all at once.
            // Deletion detection only runs for RECONCILIATION tasks — IMPORT tasks don't detect missing accounts.
            final int DELETION_PAGE_SIZE = 500;

            int deletedCount = 0;
            int identitiesDisabledCount = 0;
            long totalAbsent = 0;
            FinalizeSyncRunResponse.Builder responseBuilder = FinalizeSyncRunResponse.newBuilder();

            boolean runDeletionDetection = (taskType == SyncTaskType.RECONCILIATION);

            if (runDeletionDetection) {

            // Pre-load reactions once (they don't change per shadow)
            List<SynchronizationReactionDTO> policyReactions = loadConfiguredReactions(
                    tenantId, resourceId, request.getIdentityType());
            List<SynchronizationReactionDTO> taskReactions = convertProtoReactions(
                    request.getTaskReactionsList());
            List<SynchronizationReactionDTO> reactions = mergeReactions(taskReactions, policyReactions);

            org.springframework.data.domain.Page<Shadow> page;
            int pageNumber = 0;

            do {
                // For real execution: always fetch page 0 because processed shadows get their
                // situation changed to DELETED, so they drop out of the query results naturally.
                // For dry-run: advance the page offset since shadows aren't modified.
                int pageOffset = dryRun ? pageNumber : 0;
                // Scope deletion detection to __ACCOUNT__ shadows only.
                // __GROUP__ shadows are managed by entitlementService.finalizeEntitlementSyncRun.
                // Without this filter, group shadows not imported in this run would be
                // incorrectly flagged as DELETED.
                //
                // Dry-run mode with a non-empty seenUidsForDryRun: use the exclusion-aware
                // query so accounts that WERE scanned in this preview are not reported as absent.
                if (dryRun && !seenUidsForDryRun.isEmpty()) {
                    page = shadowRepository.findAbsentShadowsByObjectClassExcludingUids(
                            resourceId, tenantId, "__ACCOUNT__", seenUidsForDryRun,
                            org.springframework.data.domain.PageRequest.of(pageOffset, DELETION_PAGE_SIZE));
                } else {
                    page = shadowRepository.findAbsentShadowsByObjectClass(resourceId, tenantId,
                            "__ACCOUNT__", syncRunId,
                            org.springframework.data.domain.PageRequest.of(pageOffset, DELETION_PAGE_SIZE));
                }

                if (pageNumber == 0) {
                    totalAbsent = page.getTotalElements();
                    logger.info().message(dryRun ? "DRY-RUN: Finalizing sync run" : "Finalizing sync run")
                            .detail("tenantId", tenantId)
                            .detail("resourceId", resourceId)
                            .detail("taskType", taskType.name())
                            .detail("totalAbsentShadows", totalAbsent)
                            .detail("dryRun", String.valueOf(dryRun))
                            .log();
                }

                for (Shadow shadow : page.getContent()) {
                    deletedCount++;

                    if (dryRun) {
                        // DRY-RUN: Just record what WOULD be deleted without persisting
                        DeletionPreview.Builder previewBuilder = DeletionPreview.newBuilder()
                                .setShadowId(shadow.getId().toString())
                                .setResourceUid(shadow.getResourceUid());

                        if (shadow.getLinkedIdentityId() != null) {
                            previewBuilder.setLinkedIdentityId(shadow.getLinkedIdentityId().toString());
                        }

                        String resolvedAction = reactionHandler.resolveActionForPreview(
                                Shadow.SynchronizationSituation.DELETED, reactions);
                        previewBuilder.setResolvedReaction(resolvedAction);

                        responseBuilder.addDeletionPreviews(previewBuilder.build());
                        continue;
                    }

                    // REAL execution: mark as DELETED and execute reactions
                    shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.DELETED);
                    shadowRepository.save(shadow);

                    logger.info().message("Marked shadow as DELETED (not found in source)")
                            .detail("shadowId", shadow.getId())
                            .detail("resourceUid", shadow.getResourceUid())
                            .log();

                    // RECONCILIATION: Execute the DELETED reaction (unlink + disable identity)
                    if (isReactionTaskType(taskType) && shadow.getLinkedIdentityId() != null) {
                        try {
                            SynchronizationReactionHandler.ReactionResult result =
                                    reactionHandler.executeDeletedReaction(shadow, reactions);

                            if (result.outcome() ==
                                    SynchronizationReactionHandler.ReactionResult.ReactionOutcome.IDENTITY_UPDATED) {
                                identitiesDisabledCount++;
                            }

                            logger.info().message("Deletion reaction executed")
                                    .detail("shadowId", shadow.getId())
                                    .detail("outcome", result.outcome().name())
                                    .detail("message", result.message())
                                    .log();
                        } catch (Exception ex) {
                            logger.error(ex).message("Failed to execute deletion reaction")
                                    .detail("shadowId", shadow.getId())
                                    .log();
                        }
                    }
                }

                pageNumber++;

                // Real execution: keep fetching page 0 until empty (shadows are modified and drop out).
                // Dry-run: advance through pages since shadows are not modified.
            } while (page.hasContent());

            } // end if (runDeletionDetection)

            // ---------------------------------------------------------------
            //  Entitlement Cleanup
            //  Runs only when entitlements were imported in this run
            //  (request.getSkipEntitlementCleanup() == false).
            //  Skipping this when importEntitlements=false prevents wiping
            //  entitlements/assignments whose lastSyncRunId belongs to a
            //  prior task run that did import entitlements.
            // ---------------------------------------------------------------
            int deletedEntitlements = 0;
            int revokedAssignments = 0;
            if (!request.getSkipEntitlementCleanup()) {
                if (dryRun) {
                    // DRY-RUN: preview what WOULD be deleted without mutations.
                    // Read entitlement UIDs recorded during dry-run processImportedObjectBatch
                    // calls (dry-run never stamps syncRunId on rows, so we need the in-memory
                    // set to exclude entitlements that were actually scanned this preview run).
                    String entCacheKey = tenantId + "|" + resourceId + "|" + syncRunId;
                    java.util.Set<String> seenEntitlementUids = dryRunEntitlementCache.remove(entCacheKey);
                    if (seenEntitlementUids == null) seenEntitlementUids = java.util.Collections.emptySet();
                    List<Object[]> entitlementPreviews = entitlementService.previewEntitlementCleanup(
                            UUID.fromString(resourceId), tenantId, syncRunId, seenEntitlementUids);
                    for (Object[] row : entitlementPreviews) {
                        UUID entId = (UUID) row[0];
                        String sourceUid = (String) row[1];
                        String displayName = row[2] != null ? (String) row[2] : "";
                        String objectClass = row[3] != null ? (String) row[3] : "";
                        long assignmentCount = (Long) row[4];
                        deletedEntitlements++;
                        revokedAssignments += (int) assignmentCount;

                        responseBuilder.addEntitlementDeletionPreviews(
                                EntitlementDeletionPreview.newBuilder()
                                        .setEntitlementId(entId.toString())
                                        .setSourceUid(sourceUid)
                                        .setDisplayName(displayName)
                                        .setObjectClass(objectClass)
                                        .setAssignmentCount((int) assignmentCount)
                                        .build());
                    }
                } else {
                    // REAL execution: delete absent entitlements + stale assignments
                    int[] entitlementStats = entitlementService.finalizeEntitlementSyncRun(
                            UUID.fromString(resourceId), tenantId, syncRunId,
                            request.getSkipAssignmentCleanup());
                    deletedEntitlements = entitlementStats[0];
                    revokedAssignments = entitlementStats[1];
                }
            }

            // ---------------------------------------------------------------
            //  Manager Reference Resolution (F4)
            //  After all identities are created/updated, resolve raw manager
            //  references (e.g., employeeId "MGR007") to actual Identity FK
            //  relationships. This handles the circular dependency where
            //  the manager identity may not exist during initial account import.
            // ---------------------------------------------------------------
            int managersResolved = 0;
            if (!dryRun) {
                managersResolved = resolveManagerReferences(tenantId);
            }

            // ---------------------------------------------------------------
            //  Org Assignment Reconciliation (RECONCILIATION only, real run)
            //  After the connector pull has refreshed every employee's
            //  Employee.department, re-resolve each employee's primary
            //  FUNCTIONAL org assignment against the current org tree.
            //  Picks up newly created org units, retries previously failed
            //  resolutions, and clears stale OrgResolutionFailure rows when
            //  the value now resolves. Independent of the inline best-effort
            //  call performed during import.
            // ---------------------------------------------------------------
            int orgAssignmentsResolved = 0;
            int orgAssignmentsFailed = 0;
            if (!dryRun && taskType == SyncTaskType.RECONCILIATION) {
                int[] orgStats = reconcileOrgAssignments(tenantId, syncRunId);
                orgAssignmentsResolved = orgStats[0];
                orgAssignmentsFailed = orgStats[1];
            }

            responseObserver.onNext(responseBuilder
                    .setSuccess(true)
                    .setDeletedCount(deletedCount)
                    .setTotalShadows((int) totalAbsent)
                    .setIdentitiesDisabled(identitiesDisabledCount)
                    .setDeletedEntitlements(deletedEntitlements)
                    .setRevokedAssignments(revokedAssignments)
                    .setMessage(String.format(
                            "%s: %d shadows %s, %d identities %s, %d entitlements %s, %d assignments %s, %d managers resolved, %d org assignments resolved, %d org resolutions failed",
                            dryRun ? "DRY-RUN" : "Sync finalized",
                            deletedCount,
                            dryRun ? "would be marked DELETED" : "marked DELETED",
                            identitiesDisabledCount,
                            dryRun ? "would be disabled" : "disabled",
                            deletedEntitlements,
                            dryRun ? "would be removed" : "removed",
                            revokedAssignments,
                            dryRun ? "would be revoked" : "revoked",
                            managersResolved,
                            orgAssignmentsResolved,
                            orgAssignmentsFailed))
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e).message("Failed to finalize sync run").log();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to finalize sync run: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ===================================================================
    //  GetShadowsByResource - Query shadows for frontend display
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public void getShadowsByResource(
            GetShadowsByResourceRequest request,
            StreamObserver<GetShadowsByResourceResponse> responseObserver) {
        try {
            String tenantId = request.getTenantId();
            String resourceId = request.getResourceId();
            int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 20;
            int pageOffset = Math.max(0, request.getPageOffset());

            Specification<Shadow> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
                predicates.add(cb.equal(root.get("resourceId"), resourceId));

                if (!request.getObjectClass().isEmpty()) {
                    predicates.add(cb.equal(root.get("objectClass"), request.getObjectClass()));
                }
                if (!request.getSituationFilter().isEmpty()) {
                    try {
                        Shadow.SynchronizationSituation sit =
                                Shadow.SynchronizationSituation.valueOf(request.getSituationFilter());
                        predicates.add(cb.equal(root.get("synchronizationSituation"), sit));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (request.getLinkedOnly()) {
                    predicates.add(cb.isNotNull(root.get("linkedIdentityId")));
                }
                if (!request.getSearchFilter().isEmpty()) {
                    String pattern = "%" + request.getSearchFilter().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("resourceUid")), pattern));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<Shadow> page = shadowRepository.findAll(spec, PageRequest.of(pageOffset / pageSize, pageSize));

            GetShadowsByResourceResponse.Builder responseBuilder = GetShadowsByResourceResponse.newBuilder()
                    .setTotalCount(page.getTotalElements())
                    .setHasMore(page.hasNext());

            for (Shadow shadow : page.getContent()) {
                responseBuilder.addShadows(toShadowDTO(shadow));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error(e).message("Failed to get shadows by resource").log();
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get shadows: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // ===================================================================
    //  Entitlement Catalog — Group Import Processing
    // ===================================================================

    /**
     * Processes an entitlement object by populating the entitlement catalog.
     * Supports any entitlement object class (e.g., __GROUP__, roles, groupOfNames).
     * This is called after the shadow has been created/updated.
     * Groups represent entitlements (AD groups, SAP roles, etc.) — they are
     * cataloged here so the IGA platform can build role models, access reviews,
     * and SoD policies.
     * 
     * G5: Uses dedup cache to skip redundant DB lookups for duplicate group entries
     * from the connector (e.g., same group appearing in multiple pages).
     */
    private void processEntitlementObject(
            String tenantId, String resourceId, String resourceUid,
            String objectClass, String kind, Map<String, Object> attributes,
            String syncRunId, ShadowResult shadowResult,
            String displayAttributeName, String descriptionAttributeName) {

        // G5: Check dedup cache - skip if already processed in this sync run
        String dedupKey = tenantId + "|" + resourceId + "|" + resourceUid;
        if (entitlementSyncDedupCache.putIfAbsent(dedupKey, Boolean.TRUE) != null) {
            // Already processed - skip redundant DB call
            logger.debug().message("Skipping duplicate entitlement (already processed in sync run)")
                    .detail("resourceUid", resourceUid)
                    .detail("resourceId", resourceId)
                    .log();
            return;
        }

        // Extract displayName using schema-configured attribute, falling back to ConnID conventions
        String displayName = null;
        if (displayAttributeName != null && !displayAttributeName.isEmpty()) {
            displayName = extractStringAttribute(attributes, displayAttributeName);
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = extractStringAttribute(attributes, "__NAME__");
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = extractStringAttribute(attributes, "cn");
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = resourceUid; // fallback to UID
        }

        // Extract description using schema-configured attribute, falling back to "description" key
        String description = null;
        if (descriptionAttributeName != null && !descriptionAttributeName.isEmpty()) {
            description = extractStringAttribute(attributes, descriptionAttributeName);
        }
        if (description == null || description.isEmpty()) {
            description = extractStringAttribute(attributes, "description");
        }

        EntitlementDtos.EntitlementSyncDto syncDto = new EntitlementDtos.EntitlementSyncDto(
                tenantId,
                UUID.fromString(resourceId),
                resourceUid,
                displayName,
                description,
                objectClass,
                kind != null && !kind.isEmpty() ? kind : "entitlement",
                attributes,
                syncRunId
        );

        entitlementService.createOrUpdateFromSync(syncDto);

        logger.debug().message("Cataloged entitlement from group object")
                .detail("resourceUid", resourceUid)
                .detail("displayName", displayName)
                .detail("objectClass", objectClass)
                .log();
    }

    /**
     * Extracts a string attribute from the connector attributes map.
     * Handles both single values and lists (returns first value).
     */
    private String extractStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        if (value instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first != null ? first.toString() : null;
        }
        return value.toString();
    }

    // ===================================================================
    //  Shadow Creation/Update Logic
    // ===================================================================

    private record ShadowResult(Shadow shadow, SynchronizationAction action) {}

    private ShadowResult createOrUpdateShadow(
            String tenantId, String resourceId, String resourceUid,
            String objectClass, String kind, Map<String, Object> attributes,
            String syncRunId) {

        Optional<Shadow> existing = shadowRepository
                .findByResourceIdAndResourceUidAndTenantId(resourceId, resourceUid, tenantId);

        if (existing.isPresent()) {
            // Update existing shadow
            Shadow shadow = existing.get();

            // Capture attribute changes BEFORE overwriting (audit trail)
            Map<String, Object> oldAttributes = shadow.getAttributes() != null
                    ? new LinkedHashMap<>(shadow.getAttributes()) : null;

            shadow.setAttributes(attributes);
            shadow.setLastSynchronizedAt(Instant.now().toEpochMilli());
            shadow.setLastSyncRunId(syncRunId);
            Shadow saved = shadowRepository.save(shadow);

            // Record attribute-level diff for compliance audit
            changeTracker.trackChanges(saved.getId(), tenantId, resourceId,
                    syncRunId, oldAttributes, attributes);

            logger.debug().message("Updated existing shadow")
                    .detail("shadowId", saved.getId())
                    .detail("resourceUid", resourceUid)
                    .log();

            return new ShadowResult(saved, SynchronizationAction.SHADOW_UPDATED);
        } else {
            // Create new shadow
            Shadow shadow = new Shadow();
            shadow.setTenantId(tenantId);
            shadow.setResourceId(resourceId);
            shadow.setResourceUid(resourceUid);
            shadow.setObjectClass(objectClass);
            shadow.setKind(kind != null && !kind.isEmpty() ? kind : "account");
            shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.UNMATCHED);
            shadow.setAttributes(attributes);
            shadow.setLastSynchronizedAt(Instant.now().toEpochMilli());
            shadow.setLastSyncRunId(syncRunId);
            Shadow saved = shadowRepository.save(shadow);

            logger.info().message("Created new shadow")
                    .detail("shadowId", saved.getId())
                    .detail("resourceUid", resourceUid)
                    .log();

            return new ShadowResult(saved, SynchronizationAction.SHADOW_CREATED);
        }
    }

    // ===================================================================
    //  Correlation Engine
    // ===================================================================

    /**
     * Evaluates correlation rules against existing identities to find matches.
     *
     * <p>Correlation rules are configured per-application during onboarding and stored
     * in the governance policy service. The rules define how account attributes from the
     * connector map to identity attributes for matching.
     *
     * <p>For example, a correlation rule might say:
     * "Match where connector 'email' EQUALS identity 'email'"
     *
     * @param tenantId     The tenant ID
     * @param resourceId   The application/resource ID
     * @param identityType The identity type (e.g., "EMPLOYEE")
     * @param accountAttrs The account attributes from the connector
     * @return List of matching identities (empty if no match, multiple if ambiguous)
     */
    private List<Identity> runCorrelation(
            String tenantId, String resourceId, String identityType,
            Map<String, Object> accountAttrs) {

        if (identityType == null || identityType.isEmpty()) {
            logger.warn().message("No identity type specified, skipping correlation")
                    .detail("resourceId", resourceId)
                    .log();
            return Collections.emptyList();
        }

        try {
            // Fetch the governance policy for this application (CACHED)
            IdentityType idType = IdentityType.fromValue(identityType.toUpperCase());
            IdentityMappingPolicyDTO policy = getCachedPolicy(tenantId, resourceId, identityType, idType);

            if (policy == null || policy.correlationRules() == null || policy.correlationRules().isEmpty()) {
                return Collections.emptyList();
            }

            // Order rules by explicit priority (lower = earlier). Rules without an
            // explicit priority fall back to their declared array position so legacy
            // policies keep their previous behaviour. Disabled rules are skipped.
            List<CorrelationRuleDTO> orderedRules = new java.util.ArrayList<>(policy.correlationRules());
            final int[] fallbackOrder = { 0 };
            java.util.Map<CorrelationRuleDTO, Integer> declaredOrder = new java.util.IdentityHashMap<>();
            for (CorrelationRuleDTO r : orderedRules) {
                declaredOrder.put(r, fallbackOrder[0]++);
            }
            orderedRules.sort((a, b) -> {
                int pa = a.priority() != null ? a.priority() : Integer.MAX_VALUE;
                int pb = b.priority() != null ? b.priority() : Integer.MAX_VALUE;
                if (pa != pb) return Integer.compare(pa, pb);
                return Integer.compare(declaredOrder.get(a), declaredOrder.get(b));
            });

            // Evaluate each correlation rule
            for (CorrelationRuleDTO rule : orderedRules) {
                if (Boolean.FALSE.equals(rule.enabled())) {
                    logger.debug().message("Skipping disabled correlation rule")
                            .detail("resourceId", resourceId)
                            .detail("ruleId", rule.id())
                            .log();
                    continue;
                }
                List<Identity> matches = evaluateCorrelationRule(tenantId, idType, rule, accountAttrs);
                logger.info().message("Correlation rule evaluated")
                        .detail("resourceId", resourceId)
                        .detail("priority", rule.priority())
                        .detail("matchCount", matches.size())
                        .detail("ruleField", rule.ruleGroup() != null && rule.ruleGroup().rules() != null && !rule.ruleGroup().rules().isEmpty()
                                ? rule.ruleGroup().rules().get(0).field() : "unknown")
                        .log();
                if (!matches.isEmpty()) {
                    return matches;
                }
            }

            return Collections.emptyList();

        } catch (Exception e) {
            logger.warn().message("Correlation evaluation failed, treating as UNMATCHED")
                    .detail("resourceId", resourceId)
                    .detail("error", e.getMessage())
                    .log();
            return Collections.emptyList();
        }
    }

    /**
     * Gets the cached governance policy for a resource.
     * PERFORMANCE: Caches policy to avoid repeated DB lookups during batch sync.
     */
    private IdentityMappingPolicyDTO getCachedPolicy(
            String tenantId, String resourceId, String identityType, IdentityType idType) {
        String cacheKey = tenantId + "|" + resourceId + "|" + identityType;

        Optional<IdentityMappingPolicyDTO> cached = policyCache.computeIfAbsent(cacheKey, key -> {
            try {
                IdentityMappingPolicyDTO policy = policyService.getPolicy(
                        tenantId, idType, UUID.fromString(resourceId));
                logger.debug().message("Cached governance policy for resource")
                        .detail("resourceId", resourceId)
                        .detail("identityType", identityType)
                        .detail("hasCorrelationRules", policy != null && policy.correlationRules() != null)
                        .log();
                return Optional.ofNullable(policy);
            } catch (Exception e) {
                logger.warn().message("Failed to load governance policy")
                        .detail("resourceId", resourceId)
                        .detail("error", e.getMessage())
                        .log();
                return Optional.empty();
            }
        });

        return cached.orElse(null);
    }

    /**
     * Evaluates a single correlation rule against identities.
     *
     * <p>The rule structure follows the query-builder format from the frontend:
     * <pre>
     * {
     *   "combinator": "and",
     *   "rules": [
     *     { "field": "email", "operator": "=", "valueSource": "account.email" }
     *   ]
     * }
     * </pre>
     */
    private List<Identity> evaluateCorrelationRule(
            String tenantId, IdentityType identityType,
            CorrelationRuleDTO rule, Map<String, Object> accountAttrs) {

        if (rule.ruleGroup() == null || rule.ruleGroup().rules() == null) {
            return Collections.emptyList();
        }

        OnboardingRequestDTO.RuleGroupDTO ruleGroup = rule.ruleGroup();

        // Build a JPA Specification from the correlation rules
        Specification<Identity> spec = buildCorrelationSpecification(
                tenantId, identityType, ruleGroup, accountAttrs);

        if (spec == null) {
            return Collections.emptyList();
        }

        return identityRepository.findAll(spec);
    }

    /**
     * Builds a JPA Specification from the correlation rule group.
     * Supports nested rule groups with AND/OR combinators.
     */
    private Specification<Identity> buildCorrelationSpecification(
            String tenantId, IdentityType identityType,
            OnboardingRequestDTO.RuleGroupDTO ruleGroup,
            Map<String, Object> accountAttrs) {

        // Base specification: always filter by tenant and identity type
        Specification<Identity> baseSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.equal(root.get("identityType"), identityType));

        // Build rule specifications
        Specification<Identity> ruleSpec = buildRuleGroupSpec(ruleGroup, accountAttrs);

        if (ruleSpec == null) {
            return null;
        }

        return baseSpec.and(ruleSpec);
    }

    private Specification<Identity> buildRuleGroupSpec(
            OnboardingRequestDTO.RuleGroupDTO ruleGroup,
            Map<String, Object> accountAttrs) {

        if (ruleGroup.rules() == null || ruleGroup.rules().isEmpty()) {
            return null;
        }

        String combinator = ruleGroup.combinator() != null ? ruleGroup.combinator().toLowerCase() : "and";
        List<Specification<Identity>> specs = new ArrayList<>();

        for (OnboardingRequestDTO.RuleDTO rule : ruleGroup.rules()) {
            Specification<Identity> ruleSpec;

            if (rule.rules() != null && !rule.rules().isEmpty()) {
                // Nested rule group
                OnboardingRequestDTO.RuleGroupDTO nestedGroup =
                        new OnboardingRequestDTO.RuleGroupDTO(rule.combinator(), rule.rules());
                ruleSpec = buildRuleGroupSpec(nestedGroup, accountAttrs);
            } else {
                // Leaf rule - build attribute match
                ruleSpec = buildLeafRuleSpec(rule, accountAttrs);
            }

            if (ruleSpec != null) {
                if (rule.not() != null && rule.not()) {
                    specs.add(Specification.not(ruleSpec));
                } else {
                    specs.add(ruleSpec);
                }
            }
        }

        if (specs.isEmpty()) {
            return null;
        }

        // Combine with AND or OR
        Specification<Identity> combined = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            if ("or".equals(combinator)) {
                combined = combined.or(specs.get(i));
            } else {
                combined = combined.and(specs.get(i));
            }
        }

        return combined;
    }

    /**
     * Builds a specification for a single leaf rule.
     *
     * <p>Maps correlation rule fields to JPA entity paths.
     * The "field" is the identity attribute name (e.g., "email", "username", "employeeId").
     * The "valueSource" points to the account attribute value (e.g., "account.email").
     * The "value" can be a literal value as a fallback.
     */
    private Specification<Identity> buildLeafRuleSpec(
            OnboardingRequestDTO.RuleDTO rule,
            Map<String, Object> accountAttrs) {

        String identityField = rule.field();
        String operator = rule.operator() != null ? rule.operator() : "=";

        // Resolve the value to match against
        String matchValue = resolveRuleValue(rule, accountAttrs);
        if (matchValue == null || matchValue.isEmpty()) {
            return null;
        }

        return (root, query, cb) -> {
            // Map the identity field to the correct JPA path
            // Handle fields on joined tables (Employee, HumanIdentity)
            jakarta.persistence.criteria.Path<String> path = resolveIdentityFieldPath(root, identityField);
            if (path == null) {
                return cb.disjunction(); // No match possible
            }

            return switch (operator.toLowerCase()) {
                case "=", "equals", "eq" -> cb.equal(cb.lower(path), matchValue.toLowerCase());
                case "contains", "like" -> cb.like(cb.lower(path), "%" + matchValue.toLowerCase() + "%");
                case "startswith", "starts_with" -> cb.like(cb.lower(path), matchValue.toLowerCase() + "%");
                case "endswith", "ends_with" -> cb.like(cb.lower(path), "%" + matchValue.toLowerCase());
                case "!=", "not_equals", "neq" -> cb.notEqual(cb.lower(path), matchValue.toLowerCase());
                default -> cb.equal(cb.lower(path), matchValue.toLowerCase());
            };
        };
    }

    /**
     * Resolves the value to match against from the correlation rule.
     *
     * <p>The frontend (react-querybuilder) stores rules as:
     * <ul>
     *   <li>{@code valueSource = "value"} → the {@code value} field contains an
     *       account attribute name (e.g., "__UID__", "email"). Look it up in accountAttrs.</li>
     *   <li>{@code valueSource = "account.xxx"} → dot-notation reference to a specific
     *       account attribute.</li>
     *   <li>No valueSource → {@code value} is a literal string to match against.</li>
     * </ul>
     */
    private String resolveRuleValue(OnboardingRequestDTO.RuleDTO rule, Map<String, Object> accountAttrs) {
        String valueSource = rule.valueSource();

        if ("value".equals(valueSource)) {
            // react-querybuilder convention: "value" field holds the account attribute name
            String accountField = rule.value();
            if (accountField == null || accountField.isEmpty()) return null;
            Object val = accountAttrs.get(accountField);
            return val != null ? val.toString() : null;
        }

        // Try valueSource as a dot-notation path (e.g., "account.email")
        if (valueSource != null && !valueSource.isEmpty()) {
            String source = valueSource;
            // Strip the "account." prefix if present
            if (source.startsWith("account.")) {
                String accountField = source.substring("account.".length());
                Object val = accountAttrs.get(accountField);
                return val != null ? val.toString() : null;
            }
            // Direct field reference
            Object val = accountAttrs.get(source);
            return val != null ? val.toString() : null;
        }

        // Fall back to literal value
        if (rule.value() != null && !rule.value().isEmpty()) {
            return rule.value();
        }

        return null;
    }

    /**
     * Maps an identity attribute field name to the correct JPA path,
     * handling the joined table inheritance model.
     *
     * <p>Identity hierarchy:
     * <ul>
     *   <li>Identity (base): tenantId, identityType, status</li>
     *   <li>HumanIdentity: username, email, firstName, lastName, displayName</li>
     *   <li>Employee: employeeId, jobTitle, department, manager</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private jakarta.persistence.criteria.Path<String> resolveIdentityFieldPath(
            jakarta.persistence.criteria.Root<Identity> root, String fieldName) {

        return switch (fieldName.toLowerCase()) {
            // Base Identity fields
            case "status" -> root.get("status");

            // HumanIdentity fields (via JOINED inheritance - JPA handles automatically)
            case "username" -> root.get("username");
            case "email" -> root.get("email");
            case "firstname", "first_name" -> root.get("firstName");
            case "lastname", "last_name" -> root.get("lastName");
            case "displayname", "display_name" -> root.get("displayName");

            // Employee fields
            case "employeeid", "employee_id" -> root.get("employeeId");
            case "jobtitle", "job_title" -> root.get("jobTitle");
            case "department" -> root.get("department");

            default -> {
                logger.debug().message("Unknown identity field for correlation: " + fieldName).log();
                yield null;
            }
        };
    }

    // ===================================================================
    //  Correlation Result Processing
    // ===================================================================

    private ProcessImportedObjectResponse processCorrelationResults(
            Shadow shadow, List<Identity> matches, SynchronizationAction baseAction,
            boolean executeReactions, String tenantId, String resourceId,
            String identityType, Map<String, Object> attributes,
            List<SyncReaction> taskReactionsProto) {

        // Classify the synchronization situation
        Shadow.SynchronizationSituation situation;
        if (matches.isEmpty()) {
            situation = Shadow.SynchronizationSituation.UNMATCHED;
        } else if (matches.size() == 1) {
            situation = Shadow.SynchronizationSituation.UNLINKED;
        } else {
            situation = Shadow.SynchronizationSituation.DISPUTED;
        }

        // Save the classified situation on the shadow
        shadow.setSynchronizationSituation(situation);
        shadowRepository.save(shadow);

        List<UUID> matchedIds = matches.stream()
                .map(Identity::getId)
                .collect(Collectors.toList());

        // If this is an IMPORT or RECONCILIATION task, execute the synchronization reaction
        if (executeReactions) {
            IdentityType idType = resolveIdentityType(identityType);
            List<SynchronizationReactionDTO> policyReactions = loadConfiguredReactions(
                    tenantId, resourceId, identityType);
            List<SynchronizationReactionDTO> taskReactions = convertProtoReactions(taskReactionsProto);
            List<SynchronizationReactionDTO> reactions = mergeReactions(taskReactions, policyReactions);

            // Resolve the reaction string for display
            String resolvedReaction = reactionHandler.resolveActionForPreview(situation, reactions);

            // For UNMATCHED situations in parallel batch processing, serialize identity creation
            // per natural key to prevent duplicate Identity records when two threads process
            // accounts for the same person concurrently. Re-run correlation inside the lock
            // to catch identities created by concurrent threads.
            if (situation == Shadow.SynchronizationSituation.UNMATCHED) {
                // Compute creation attributes before reaction (for attribute changes display)
                List<InboundMappingEngine.AttributeChange> creationChanges =
                        reactionHandler.getMappingEngine().computeCreationAttributes(
                                tenantId, resourceId, idType, attributes);

                String naturalKey = deriveNaturalKey(tenantId, attributes);
                if (naturalKey != null) {
                    Object lock = identityCreationLocks.computeIfAbsent(naturalKey, k -> new Object());
                    synchronized (lock) {
                        // Re-run correlation to detect identities created by concurrent threads
                        List<Identity> recheck = runCorrelation(tenantId, resourceId, identityType, attributes);
                        if (!recheck.isEmpty()) {
                            // Another thread already created/linked — reclassify
                            situation = recheck.size() == 1
                                    ? Shadow.SynchronizationSituation.UNLINKED
                                    : Shadow.SynchronizationSituation.DISPUTED;
                            shadow.setSynchronizationSituation(situation);
                            shadowRepository.save(shadow);
                            matchedIds = recheck.stream().map(Identity::getId).collect(Collectors.toList());
                            resolvedReaction = reactionHandler.resolveActionForPreview(situation, reactions);
                        }
                        SynchronizationReactionHandler.ReactionResult reactionResult =
                                reactionHandler.executeReaction(
                                        shadow, situation, matchedIds,
                                        tenantId, resourceId, idType, attributes, reactions);
                        // If opportunistically linked, compute update diff instead of creation changes
                        List<InboundMappingEngine.AttributeChange> finalChanges = creationChanges;
                        if (reactionResult.outcome() == SynchronizationReactionHandler.ReactionResult.ReactionOutcome.IDENTITY_LINKED
                                && shadow.getLinkedIdentityId() != null) {
                            Optional<Identity> linked = identityRepository.findByTenantIdAndId(
                                    tenantId, shadow.getLinkedIdentityId());
                            if (linked.isPresent()) {
                                finalChanges = reactionHandler.getMappingEngine().computeAttributeChanges(
                                        linked.get(), tenantId, resourceId, idType, attributes);
                            }
                        }
                        return buildReactionResponse(shadow, situation, reactionResult,
                                resolvedReaction, finalChanges);
                    }
                }

                SynchronizationReactionHandler.ReactionResult reactionResult =
                        reactionHandler.executeReaction(
                                shadow, situation, matchedIds,
                                tenantId, resourceId, idType, attributes, reactions);
                // If opportunistically linked, compute update diff instead of creation changes
                List<InboundMappingEngine.AttributeChange> finalChanges = creationChanges;
                if (reactionResult.outcome() == SynchronizationReactionHandler.ReactionResult.ReactionOutcome.IDENTITY_LINKED
                        && shadow.getLinkedIdentityId() != null) {
                    Optional<Identity> linked = identityRepository.findByTenantIdAndId(
                            tenantId, shadow.getLinkedIdentityId());
                    if (linked.isPresent()) {
                        finalChanges = reactionHandler.getMappingEngine().computeAttributeChanges(
                                linked.get(), tenantId, resourceId, idType, attributes);
                    }
                }
                return buildReactionResponse(shadow, situation, reactionResult,
                        resolvedReaction, finalChanges);
            }

            // UNLINKED: compute attribute changes against the matched identity
            List<InboundMappingEngine.AttributeChange> attrChanges = Collections.emptyList();
            if (situation == Shadow.SynchronizationSituation.UNLINKED && !matchedIds.isEmpty()) {
                Optional<Identity> matchedIdentity = identityRepository.findByTenantIdAndId(
                        tenantId, matchedIds.get(0));
                if (matchedIdentity.isPresent()) {
                    attrChanges = reactionHandler.getMappingEngine().computeAttributeChanges(
                            matchedIdentity.get(), tenantId, resourceId, idType, attributes);
                }
            }

            SynchronizationReactionHandler.ReactionResult reactionResult =
                    reactionHandler.executeReaction(
                            shadow, situation, matchedIds,
                            tenantId, resourceId, idType, attributes, reactions);

            return buildReactionResponse(shadow, situation, reactionResult,
                    resolvedReaction, attrChanges);
        }

        // No reactions — classification only (legacy behavior)
        if (matches.isEmpty()) {
            logger.info().message("No correlation match found")
                    .detail("shadowId", shadow.getId())
                    .detail("resourceUid", shadow.getResourceUid())
                    .log();

            return buildResponse(true, shadow,
                    SynchronizationSituation.UNMATCHED,
                    SynchronizationAction.NO_MATCH,
                    "No matching identity found via correlation rules");

        } else if (matches.size() == 1) {
            // Legacy auto-link behavior (no reaction handler)
            Identity matchedIdentity = matches.get(0);
            shadow.setLinkedIdentityId(matchedIdentity.getId());
            shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.LINKED);
            shadowRepository.save(shadow);

            logger.info().message("Correlation matched, shadow auto-linked")
                    .detail("shadowId", shadow.getId())
                    .detail("identityId", matchedIdentity.getId())
                    .log();

            return buildResponse(true, shadow,
                    SynchronizationSituation.LINKED,
                    SynchronizationAction.AUTO_LINKED,
                    "Shadow auto-linked to identity " + matchedIdentity.getId());

        } else {
            String matchIds = matches.stream()
                    .map(i -> i.getId().toString())
                    .collect(Collectors.joining(", "));

            logger.warn().message("Multiple correlation matches found (DISPUTED)")
                    .detail("shadowId", shadow.getId())
                    .detail("matchCount", matches.size())
                    .detail("matchIds", matchIds)
                    .log();

            return buildResponse(true, shadow,
                    SynchronizationSituation.DISPUTED,
                    SynchronizationAction.DISPUTED_MATCH,
                    "Multiple identities matched (" + matches.size() + "): " + matchIds);
        }
    }

    /**
     * Maps a reaction outcome to the appropriate proto SynchronizationAction.
     */
    private SynchronizationAction mapReactionOutcome(
            SynchronizationReactionHandler.ReactionResult.ReactionOutcome outcome,
            Shadow.SynchronizationSituation situation) {
        return switch (outcome) {
            case IDENTITY_CREATED -> SynchronizationAction.IDENTITY_CREATED;
            case IDENTITY_LINKED -> SynchronizationAction.IDENTITY_LINKED;
            case IDENTITY_UPDATED -> SynchronizationAction.IDENTITY_UPDATED;
            case NO_CHANGE -> SynchronizationAction.ALREADY_LINKED;
            case SKIPPED -> SynchronizationAction.REACTION_SKIPPED;
            case FAILED -> situation == Shadow.SynchronizationSituation.DISPUTED
                    ? SynchronizationAction.DISPUTED_MATCH
                    : SynchronizationAction.NO_MATCH;
        };
    }

    /**
     * Checks if the task type requires executing synchronization reactions.
     * DISCOVERY mode skips reactions — it only creates/updates shadows and evaluates correlation.
     */
    private boolean isReactionTaskType(SyncTaskType taskType) {
        return taskType == SyncTaskType.IMPORT || taskType == SyncTaskType.RECONCILIATION;
    }

    /**
     * Safely resolves an identity type string to the IdentityType enum.
     */
    private IdentityType resolveIdentityType(String identityType) {
        if (identityType == null || identityType.isEmpty()) {
            return IdentityType.EMPLOYEE; // Default
        }
        try {
            return IdentityType.fromValue(identityType.toUpperCase());
        } catch (Exception e) {
            return IdentityType.EMPLOYEE;
        }
    }

    // ===================================================================
    //  Manager Reference Resolution (F4)
    // ===================================================================

    /**
     * Builds a ProcessImportedObjectResponse from a reaction result.
     * Extracted to avoid duplication between normal and serialized (locked) paths.
     */
    private ProcessImportedObjectResponse buildReactionResponse(
            Shadow shadow,
            Shadow.SynchronizationSituation situation,
            SynchronizationReactionHandler.ReactionResult reactionResult) {
        return buildReactionResponse(shadow, situation, reactionResult, null, Collections.emptyList());
    }

    private ProcessImportedObjectResponse buildReactionResponse(
            Shadow shadow,
            Shadow.SynchronizationSituation situation,
            SynchronizationReactionHandler.ReactionResult reactionResult,
            String resolvedReaction,
            List<InboundMappingEngine.AttributeChange> attrChanges) {

        SynchronizationAction action = mapReactionOutcome(reactionResult.outcome(), situation);
        // Use the detected (pre-reaction) situation, not the post-reaction shadow state.
        // IGA standard: the Situation column must show what was FOUND during correlation
        // (e.g. UNLINKED), not the outcome after the reaction ran (e.g. LINKED).
        // The Action and Reaction columns already describe what happened next.
        SynchronizationSituation protoSituation = mapSituation(situation);

        ProcessImportedObjectResponse.Builder builder = ProcessImportedObjectResponse.newBuilder()
                .setSuccess(reactionResult.outcome() !=
                        SynchronizationReactionHandler.ReactionResult.ReactionOutcome.FAILED)
                .setShadowId(shadow.getId().toString())
                .setSituation(protoSituation)
                .setAction(action)
                .setMessage(reactionResult.message())
                .setAttributesUpdatedCount(reactionResult.attributesUpdated());

        if (resolvedReaction != null && !resolvedReaction.isEmpty()) {
            builder.setResolvedReaction(resolvedReaction);
        }

        if (attrChanges != null) {
            for (InboundMappingEngine.AttributeChange change : attrChanges) {
                builder.addAttributeChanges(AttributeChange.newBuilder()
                        .setFieldName(change.fieldName())
                        .setOldValue(change.oldValue())
                        .setNewValue(change.newValue())
                        .build());
            }
        }

        if (shadow.getLinkedIdentityId() != null) {
            builder.setLinkedIdentityId(shadow.getLinkedIdentityId().toString());
        }
        if (reactionResult.identityId() != null) {
            builder.setCreatedIdentityId(reactionResult.identityId().toString());
        }

        logger.info().message("Sync reaction executed")
                .detail("shadowId", shadow.getId())
                .detail("situation", situation.name())
                .detail("outcome", reactionResult.outcome().name())
                .detail("message", reactionResult.message())
                .log();

        return builder.build();
    }

    /**
     * Derives a natural key from account attributes for identity creation serialization.
     * Uses the most common uniqueness indicators: username, employeeId, email.
     * Returns null if no natural key can be derived (serialization skipped — rare).
     */
    private String deriveNaturalKey(String tenantId, Map<String, Object> attributes) {
        // Try username first (most common identity key)
        for (String key : List.of("username", "sAMAccountName", "uid", "__NAME__")) {
            Object val = attributes.get(key);
            if (val != null && !val.toString().isBlank()) {
                return tenantId + "|username|" + val.toString().toLowerCase();
            }
        }
        // Try employeeId
        Object empId = attributes.get("employeeId");
        if (empId == null) empId = attributes.get("employeeNumber");
        if (empId != null && !empId.toString().isBlank()) {
            return tenantId + "|employeeId|" + empId.toString().toLowerCase();
        }
        // Try email
        for (String key : List.of("email", "mail", "emailAddress")) {
            Object val = attributes.get(key);
            if (val != null && !val.toString().isBlank()) {
                return tenantId + "|email|" + val.toString().toLowerCase();
            }
        }
        return null;
    }

    /**
     * Resolves raw manager references on Employee entities to actual Identity FK relationships.
     * During import, the connector provides a manager reference (e.g., employeeId "MGR007",
     * username, or email). This method runs after all accounts are imported and resolves
     * those string references to the actual Identity entities.
     *
     * @param tenantId Tenant context
     * @return Number of manager relationships resolved
     */
    @Transactional
    private int resolveManagerReferences(String tenantId) {
        List<Employee> unresolvedEmployees = employeeRepository.findByTenantIdAndManagerRefIsNotNullAndManagerIsNull(tenantId);
        if (unresolvedEmployees.isEmpty()) {
            return 0;
        }

        int resolved = 0;
        for (Employee employee : unresolvedEmployees) {
            String ref = employee.getManagerRef();

            // Try to resolve by employeeId first (most common), then username, then email
            Optional<Employee> manager = employeeRepository.findByTenantIdAndEmployeeId(tenantId, ref);
            if (manager.isEmpty()) {
                manager = employeeRepository.findByTenantIdAndUsername(tenantId, ref);
            }
            if (manager.isEmpty()) {
                manager = employeeRepository.findByTenantIdAndEmail(tenantId, ref);
            }

            if (manager.isPresent() && !manager.get().getId().equals(employee.getId())) {
                employee.setManager(manager.get());
                employeeRepository.save(employee);
                resolved++;
            } else if (manager.isEmpty()) {
                logger.debug().message("Manager reference unresolved — manager identity not found")
                        .detail("employeeId", employee.getEmployeeId())
                        .detail("managerRef", ref)
                        .detail("tenantId", tenantId)
                        .log();
            }
        }

        if (resolved > 0) {
            logger.info().message("Resolved manager references")
                    .detail("tenantId", tenantId)
                    .detail("resolved", String.valueOf(resolved))
                    .detail("total", String.valueOf(unresolvedEmployees.size()))
                    .log();
        }

        return resolved;
    }

    /**
     * Reconciliation pass: re-resolve every employee's primary FUNCTIONAL org
     * assignment using {@link IdentityOrgSyncService}, which applies the
     * three-way {@code externalId | code | name} lookup and audits failures
     * to {@code org_resolution_failures}.
     *
     * <p>Runs in the finalize phase of RECONCILIATION tasks only. Idempotent
     * by construction (the underlying upsert is a no-op when the assignment
     * already points to the correct unit). Paginated to keep memory bounded
     * for large tenants.
     *
     * @return {@code [resolved, failed]} counts for inclusion in the finalize summary
     */
    @Transactional
    private int[] reconcileOrgAssignments(String tenantId, String taskRunId) {
        final int PAGE_SIZE = 500;
        int resolved = 0;
        int failed = 0;
        int pageNumber = 0;
        org.springframework.data.domain.Page<Employee> page;

        do {
            page = employeeRepository.findByTenantIdWithDepartment(
                    tenantId,
                    org.springframework.data.domain.PageRequest.of(pageNumber, PAGE_SIZE));

            for (Employee employee : page.getContent()) {
                Optional<UUID> result = identityOrgSyncService.syncPrimaryFunctionalAssignment(
                        tenantId,
                        employee.getId(),
                        employee.getDepartment(),
                        "RECONCILIATION",
                        AssignmentType.HR_OF_RECORD,
                        taskRunId);
                if (result.isPresent()) {
                    resolved++;
                } else {
                    failed++;
                }
            }
            pageNumber++;
        } while (page.hasNext());

        if (resolved > 0 || failed > 0) {
            logger.info().message("Reconciled org assignments")
                    .detail("tenantId", tenantId)
                    .detail("taskRunId", taskRunId == null ? "" : taskRunId)
                    .detail("resolved", String.valueOf(resolved))
                    .detail("failed", String.valueOf(failed))
                    .log();
        }

        return new int[]{resolved, failed};
    }

    // ===================================================================
    //  Helper Methods
    // ===================================================================

    private void validateRequest(ProcessImportedObjectRequest request) {
        if (request.getTenantId().isEmpty()) {
            throw new IllegalArgumentException("tenant_id is required");
        }
        if (request.getResourceId().isEmpty()) {
            throw new IllegalArgumentException("resource_id is required");
        }
        if (request.getResourceUid().isEmpty()) {
            throw new IllegalArgumentException("resource_uid is required");
        }
        if (request.getObjectClass().isEmpty()) {
            throw new IllegalArgumentException("object_class is required");
        }
    }

    private ProcessImportedObjectResponse buildResponse(
            boolean success, Shadow shadow,
            SynchronizationSituation situation,
            SynchronizationAction action,
            String message) {

        ProcessImportedObjectResponse.Builder builder = ProcessImportedObjectResponse.newBuilder()
                .setSuccess(success)
                .setShadowId(shadow.getId().toString())
                .setSituation(situation)
                .setAction(action)
                .setMessage(message);

        if (shadow.getLinkedIdentityId() != null) {
            builder.setLinkedIdentityId(shadow.getLinkedIdentityId().toString());
        }

        return builder.build();
    }

    private ShadowDTO toShadowDTO(Shadow shadow) {
        ShadowDTO.Builder builder = ShadowDTO.newBuilder()
                .setId(shadow.getId().toString())
                .setTenantId(shadow.getTenantId())
                .setResourceId(shadow.getResourceId())
                .setResourceUid(shadow.getResourceUid())
                .setObjectClass(shadow.getObjectClass());

        if (shadow.getKind() != null) {
            builder.setKind(shadow.getKind());
        }
        if (shadow.getLinkedIdentityId() != null) {
            builder.setLinkedIdentityId(shadow.getLinkedIdentityId().toString());
        }

        // Map situation enum
        builder.setSituation(mapSituation(shadow.getSynchronizationSituation()));

        // Map attributes to Struct
        if (shadow.getAttributes() != null && !shadow.getAttributes().isEmpty()) {
            try {
                builder.setAttributes(mapToStruct(shadow.getAttributes()));
            } catch (Exception e) {
                logger.debug().message("Failed to convert shadow attributes to Struct").log();
            }
        }

        if (shadow.getCreatedAt() != null) {
            builder.setCreatedAt(shadow.getCreatedAt());
        }
        if (shadow.getUpdatedAt() != null) {
            builder.setUpdatedAt(shadow.getUpdatedAt());
        }
        if (shadow.getLastSynchronizedAt() != null) {
            builder.setLastSynchronizedAt(shadow.getLastSynchronizedAt());
        }

        return builder.build();
    }

    private SynchronizationSituation mapSituation(Shadow.SynchronizationSituation situation) {
        return switch (situation) {
            case UNMATCHED -> SynchronizationSituation.UNMATCHED;
            case UNLINKED -> SynchronizationSituation.UNLINKED;
            case LINKED -> SynchronizationSituation.LINKED;
            case DELETED -> SynchronizationSituation.DELETED;
            case DISPUTED -> SynchronizationSituation.DISPUTED;
        };
    }

    /**
     * Converts a Protobuf Struct to a Map.
     */
    private Map<String, Object> structToMap(Struct struct) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (struct == null) {
            return map;
        }
        for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
            map.put(entry.getKey(), valueToObject(entry.getValue()));
        }
        // Normalize attribute values: flatten single-element string lists
        // and join character-array lists into strings. This handles legacy data
        // from connectors that declare attributes as char type.
        return normalizeAttributes(map);
    }

    /**
     * Normalizes attribute values to ensure consistent types.
     * - Single-element lists containing a String are unwrapped to just the String.
     * - Lists of single-character strings are joined into a single String.
     */
    private Map<String, Object> normalizeAttributes(Map<String, Object> attrs) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            normalized.put(entry.getKey(), normalizeValue(entry.getValue()));
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeValue(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return value;
        }
        // Check if all elements are single-character strings (char-array artifact)
        boolean allSingleChars = list.stream().allMatch(
                item -> item instanceof String s && s.length() == 1);
        if (allSingleChars && list.size() > 1) {
            StringBuilder sb = new StringBuilder(list.size());
            for (Object item : list) {
                sb.append((String) item);
            }
            return sb.toString();
        }
        // Single-element list — unwrap to scalar
        if (list.size() == 1) {
            return list.get(0);
        }
        return value;
    }

    private Object valueToObject(Value value) {
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case NUMBER_VALUE -> value.getNumberValue();
            case BOOL_VALUE -> value.getBoolValue();
            case NULL_VALUE -> null;
            case LIST_VALUE -> value.getListValue().getValuesList().stream()
                    .map(this::valueToObject).collect(Collectors.toList());
            case STRUCT_VALUE -> structToMap(value.getStructValue());
            default -> null;
        };
    }

    /**
     * Converts a Map to a Protobuf Struct.
     */
    private Struct mapToStruct(Map<String, Object> map) {
        Struct.Builder builder = Struct.newBuilder();
        if (map == null) {
            return builder.build();
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.putFields(entry.getKey(), objectToValue(entry.getValue()));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Value objectToValue(Object obj) {
        if (obj == null) {
            return Value.newBuilder()
                    .setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
        }
        if (obj instanceof String s) {
            return Value.newBuilder().setStringValue(s).build();
        }
        if (obj instanceof Number n) {
            return Value.newBuilder().setNumberValue(n.doubleValue()).build();
        }
        if (obj instanceof Boolean b) {
            return Value.newBuilder().setBoolValue(b).build();
        }
        if (obj instanceof List<?> list) {
            com.google.protobuf.ListValue.Builder listBuilder = com.google.protobuf.ListValue.newBuilder();
            for (Object item : list) {
                listBuilder.addValues(objectToValue(item));
            }
            return Value.newBuilder().setListValue(listBuilder.build()).build();
        }
        if (obj instanceof Map<?, ?> map) {
            return Value.newBuilder().setStructValue(mapToStruct((Map<String, Object>) map)).build();
        }
        return Value.newBuilder().setStringValue(obj.toString()).build();
    }

    // ===================================================================
    //  Load Configured Reactions from Governance Policy
    // ===================================================================

    /**
     * Loads the synchronization reactions configured for a given resource and identity type.
     * Returns null if no reactions are configured (falls back to defaults in the handler).
     * 
     * PERFORMANCE: Uses caching to avoid repeated DB lookups during batch sync.
     * The same policy applies to all 100K+ objects from the same resource.
     */
    private List<SynchronizationReactionDTO> loadConfiguredReactions(
            String tenantId, String resourceId, String identityType) {
        if (identityType == null || identityType.isEmpty() || resourceId == null) {
            return null;
        }

        String cacheKey = tenantId + "|" + resourceId + "|" + identityType;

        // Use computeIfAbsent for thread-safe lazy loading
        Optional<List<SynchronizationReactionDTO>> cached = reactionsCache.computeIfAbsent(cacheKey, key -> {
            try {
                IdentityType idType = IdentityType.fromValue(identityType.toUpperCase());
                List<SynchronizationReactionDTO> reactions = policyService.getSynchronizationReactions(
                        tenantId, idType, UUID.fromString(resourceId));
                logger.debug().message("Cached sync reactions for resource")
                        .detail("resourceId", resourceId)
                        .detail("identityType", identityType)
                        .detail("reactionsCount", reactions != null ? reactions.size() : 0)
                        .log();
                return Optional.ofNullable(reactions);
            } catch (Exception e) {
                logger.debug().message("Could not load configured reactions, using defaults")
                        .detail("resourceId", resourceId)
                        .detail("identityType", identityType)
                        .detail("error", e.getMessage())
                        .log();
                return Optional.empty();
            }
        });

        return cached.orElse(null);
    }

    /**
     * Clears all caches. Call between sync runs or when policies change.
     */
    public void clearCaches() {
        reactionsCache.clear();
        policyCache.clear();
        entitlementCatalogCache.clear();
        assignmentKeysCache.clear();
        entitlementSyncDedupCache.clear();
        initializedSyncRuns.clear();
        identityCreationLocks.clear();
        logger.debug().message("Cleared sync caches (reactions + policy + entitlement + dedup + locks)").log();
    }

    /**
     * @deprecated Use clearCaches() instead
     */
    @Deprecated
    public void clearReactionsCache() {
        clearCaches();
    }

    /**
     * Converts proto SyncReaction list to SynchronizationReactionDTO list.
     */
    private List<SynchronizationReactionDTO> convertProtoReactions(List<SyncReaction> protoReactions) {
        if (protoReactions == null || protoReactions.isEmpty()) {
            return Collections.emptyList();
        }
        return protoReactions.stream()
                .map(r -> new SynchronizationReactionDTO(r.getSituation(), r.getAction()))
                .collect(Collectors.toList());
    }

    /**
     * Merges task-level reactions with policy-level reactions.
     * Task reactions take priority over policy reactions for the same situation.
     *
     * @param taskReactions  The task-level reactions (from gRPC request)
     * @param policyReactions The policy-level reactions (from governance policy)
     * @return Merged list with task reactions taking priority
     */
    private List<SynchronizationReactionDTO> mergeReactions(
            List<SynchronizationReactionDTO> taskReactions,
            List<SynchronizationReactionDTO> policyReactions) {

        // If no task reactions, use policy reactions
        if (taskReactions == null || taskReactions.isEmpty()) {
            return policyReactions;
        }

        // If no policy reactions, use task reactions
        if (policyReactions == null || policyReactions.isEmpty()) {
            return taskReactions;
        }

        // Merge: task reactions override policy for same situation
        Map<String, SynchronizationReactionDTO> merged = new LinkedHashMap<>();

        // Add policy reactions first
        for (SynchronizationReactionDTO reaction : policyReactions) {
            merged.put(reaction.situation().toUpperCase(), reaction);
        }

        // Override with task reactions
        for (SynchronizationReactionDTO reaction : taskReactions) {
            merged.put(reaction.situation().toUpperCase(), reaction);
        }

        return new ArrayList<>(merged.values());
    }
}
