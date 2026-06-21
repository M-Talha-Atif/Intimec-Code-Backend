package com.aidentx.iam.access_roles_management.service.import_jobs;
import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditConstants;
import com.aidentx.iam.access_roles_management.audit.RoleImportJobAuditConstants;
import com.aidentx.iam.access_roles_management.audit.RoleImportJobAuditEvent;
import com.aidentx.iam.access_roles_management.audit.RoleImportJobAuditPublisher;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleImportConstants;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.RoleImportRowDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJob;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJobItem;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemPhase;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportJobStatus;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobItemRepository;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleService;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleService;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * Background processor for persisted access-role import jobs.
 *
 * <p>The HTTP submit path stores each role row as a {@link RoleImportJobItem}
 * with status {@code QUEUED}. This worker is invoked by Kafka and turns those
 * queued rows into real access roles. It deliberately reuses the existing role,
 * composition, entitlement, assignment-rule, closure, and audit logic by calling
 * the same services used by normal access-role APIs.</p>
 *
 * <p>Transaction strategy: every row is processed in a separate
 * {@code REQUIRES_NEW} transaction through {@link TransactionTemplate}. If user
 * data is invalid, the row transaction is allowed to roll back first, then this
 * worker records the row failure in a second {@code REQUIRES_NEW} transaction.
 * This prevents rollback-only row failures from becoming full Kafka job failures.</p>
 */
@Service
public class RoleImportJobWorker {
    private final RoleImportJobRepository jobRepository;
    private final RoleImportJobItemRepository itemRepository;
    private final AccessRoleService accessRoleService;
    private final AccessRoleCompositionService compositionService;
    private final AccessRoleAssignmentRuleService ruleService;
    private final IdentityRoleAssignmentService identityRoleAssignmentService;
    private final RoleImportJobAuditPublisher auditPublisher;
    private final AccessRoleRepository roleRepository;
    private final EntitlementRepository entitlementRepository;
    private final IdentityRepository identityRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final TransactionTemplate tx;
    private final int batchSize;
    public RoleImportJobWorker(RoleImportJobRepository jobRepository,
                               RoleImportJobItemRepository itemRepository,
                               AccessRoleService accessRoleService,
                               AccessRoleCompositionService compositionService,
                               AccessRoleAssignmentRuleService ruleService,
                               IdentityRoleAssignmentService identityRoleAssignmentService,
                               RoleImportJobAuditPublisher auditPublisher,
                               AccessRoleRepository roleRepository,
                               EntitlementRepository entitlementRepository,
                               IdentityRepository identityRepository,
                               OrgUnitRepository orgUnitRepository,
                               ObjectMapper objectMapper,
                               PlatformTransactionManager transactionManager,
                               UnifiedLoggerFactory loggerFactory,
                               @Value("${" + AccessRoleImportConstants.CONFIG_BATCH_SIZE + ":" + AccessRoleImportConstants.BATCH_SIZE_DEFAULT + "}") int batchSize) {
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.accessRoleService = accessRoleService;
        this.compositionService = compositionService;
        this.ruleService = ruleService;
        this.identityRoleAssignmentService = identityRoleAssignmentService;
        this.auditPublisher = auditPublisher;
        this.roleRepository = roleRepository;
        this.entitlementRepository = entitlementRepository;
        this.identityRepository = identityRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleImportConstants.LOG_CATEGORY);
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.batchSize = Math.max(1, batchSize);
    }
    /**
     * Processes one import job end-to-end after a Kafka event is consumed.
     *
     * <p>The worker uses two phases:</p>
     * <ol>
     *     <li><b>CREATE</b>: create each role, attach direct entitlements/childRoleIds via existing create flow,
     *     and create assignment rules. Each item runs in its own {@code REQUIRES_NEW} transaction so one
     *     bad row does not roll back the whole job.</li>
     *     <li><b>WIRE</b>: resolve {@code childRoleRefs} between successfully imported rows after all created
     *     role IDs are known.</li>
     *     <li><b>ASSIGN</b>: assign successfully created/wired roles to row {@code identityIds} with source MANUAL.</li>
     * </ol>
     *
     * @param jobId import job id from {@link com.aidentx.iam.access_roles_management.event.RoleImportJobStartedEvent}
     */
    public void process(UUID jobId) {
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_PROCESS)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                .detail(AccessRoleImportConstants.DETAIL_BATCH_SIZE, String.valueOf(batchSize))
                .log();
        setRunning(jobId);
        ImportAssignmentAuditTracker assignmentTracker = new ImportAssignmentAuditTracker();
        try {
            processPreflight(jobId);
            processCreate(jobId);
            processWire(jobId);
            processAssignments(jobId, assignmentTracker);
            finish(jobId, null).ifPresent(job -> publishImportAudit(job, assignmentTracker));
        } catch (RuntimeException ex) {
            logger.error(ex)
                    .category(AccessRoleImportConstants.LOG_CATEGORY)
                    .message(AccessRoleImportConstants.LOG_JOB_FAILED)
                    .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                    .detail(AccessRoleImportConstants.DETAIL_ERROR_MESSAGE, ex.getMessage())
                    .log();
            finish(jobId, ex.getClass().getSimpleName() + ": " + ex.getMessage())
                    .ifPresent(job -> publishImportAudit(job, assignmentTracker));
            throw ex;
        }
    }

    /** Reference validation is non-blocking; missing records are recorded as row warnings later. */
    private void processPreflight(UUID jobId) {
        refresh(jobId);
    }

    private Set<UUID> existingEntitlementIds(String tenantId, List<UUID> entitlementIds) {
        Set<UUID> requestedIds = normalizeIds(entitlementIds);
        if (requestedIds.isEmpty()) {
            return Set.of();
        }
        return entitlementRepository.findAllById(requestedIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .map(entitlement -> entitlement.getId())
                .collect(Collectors.toSet());
    }

    private Set<UUID> existingIdentityIds(String tenantId, List<UUID> identityIds) {
        Set<UUID> requestedIds = normalizeIds(identityIds);
        if (requestedIds.isEmpty()) {
            return Set.of();
        }
        return identityRepository.findByTenantIdAndIdIn(tenantId, requestedIds).stream()
                .map(Identity::getId)
                .collect(Collectors.toSet());
    }

    private Set<UUID> existingChildRoleIds(String tenantId, List<UUID> childRoleIds) {
        Set<UUID> requestedIds = normalizeIds(childRoleIds);
        if (requestedIds.isEmpty()) {
            return Set.of();
        }
        return roleRepository.findByIdIn(requestedIds).stream()
                .filter(role -> tenantId.equals(role.getTenantId()))
                .map(role -> role.getId())
                .collect(Collectors.toSet());
    }

    private Set<UUID> normalizeIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<UUID> filterExistingIds(String fieldName, List<UUID> requestedIds, Set<UUID> existingIds, List<String> warnings) {
        Set<UUID> normalizedRequestedIds = normalizeIds(requestedIds);
        if (normalizedRequestedIds.isEmpty()) {
            return List.of();
        }
        List<UUID> missingIds = normalizedRequestedIds.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            warnings.add(fieldName + " not found " + missingIds + "; only existing values were applied.");
        }
        return normalizedRequestedIds.stream()
                .filter(existingIds::contains)
                .toList();
    }

    private void appendWarnings(RoleImportJobItem item, List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        List<String> allWarnings = new ArrayList<>();
        if (item.getWarningMessage() != null && !item.getWarningMessage().isBlank()) {
            allWarnings.add(item.getWarningMessage());
        }
        allWarnings.addAll(warnings);
        String warningMessage = String.join(" ", allWarnings);
        item.setWarningMessage(warningMessage.length() > 2000 ? warningMessage.substring(0, 2000) : warningMessage);
    }

    private void appendWarning(UUID itemId, String warning) {
        if (warning == null || warning.isBlank()) {
            return;
        }
        tx.executeWithoutResult(s -> {
            RoleImportJobItem item = itemRepository.findById(itemId).orElseThrow();
            appendWarnings(item, List.of(warning));
            itemRepository.save(item);
        });
    }

    private String rolePrefix(RoleImportRowDto row) {
        String name = row != null && row.getName() != null ? row.getName().trim() : null;
        return name != null && !name.isBlank() ? "Role '" + name + "'" : "This row";
    }

    /** Processes queued CREATE items in fixed-size batches until none remain. */
    private void processCreate(UUID jobId) {
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_CREATE_PHASE_START)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                .detail(AccessRoleImportConstants.DETAIL_BATCH_SIZE, String.valueOf(batchSize))
                .log();
        while (true) {
            List<RoleImportJobItem> batch = itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                    jobId, RoleImportItemPhase.CREATE, RoleImportItemStatus.QUEUED, Pageable.unpaged());
            if (batch.isEmpty()) return;
            logger.info()
                    .category(AccessRoleImportConstants.LOG_CATEGORY)
                    .message(AccessRoleImportConstants.LOG_CREATE_BATCH)
                    .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                    .detail(AccessRoleImportConstants.DETAIL_BATCH_COUNT, String.valueOf(batch.size()))
                    .log();
            for (RoleImportJobItem item : batch) {
                createOne(item.getId());
            }
            refresh(jobId);
        }
    }
    /**
     * Creates one role row and records row failure in a separate transaction.
     *
     * <p>The create flow can call nested services that mark the current
     * transaction rollback-only when user-supplied data is invalid, for example
     * when {@code childRoleIds} references a role that does not exist in the
     * deployed tenant database. Failure status must therefore be saved after the
     * failed row transaction has rolled back, not inside the same transaction.</p>
     */
    private void createOne(UUID itemId) {
        try {
            tx.executeWithoutResult(s -> createOneInTransaction(itemId));
        } catch (Exception ex) {
            markItemFailed(itemId, AccessRoleImportConstants.ERROR_PROCESSING, RoleImportItemPhase.CREATE,
                    AccessRoleImportConstants.LOG_CREATE_ITEM_FAILED, ex);
        }
    }
    /** Performs the successful create path inside one rollback-isolated row transaction. */
    private void createOneInTransaction(UUID itemId) {
        RoleImportJobItem item = itemRepository.findById(itemId).orElseThrow();
        RoleImportRowDto row = readRow(item);
        List<String> warnings = sanitizeCreateReferences(item, row);
        AccessRoleResponse created = createRoleWithImportActor(item, row);
        item.setRoleId(created.getId());
        if (row.getRules() != null) {
            for (CreateAccessRoleAssignmentRuleRequest rule : row.getRules()) {
                rule.setRoleId(created.getId());
                ruleService.createRule(item.getTenantId(), rule, actorUsernameFor(item.getJobId(), item.getTenantId()));
            }
        }
        item.setStatus(RoleImportItemStatus.SUCCEEDED);
        item.setErrorCode(null);
        item.setErrorMessage(null);
        appendWarnings(item, warnings);
        item.setPayload(objectMapper.valueToTree(row));
        item.setProcessedAt(Instant.now());
        itemRepository.save(item);
    }

    private List<String> sanitizeCreateReferences(RoleImportJobItem item, RoleImportRowDto row) {
        List<String> warnings = new ArrayList<>();
        String tenantId = item.getTenantId();

        if (row.getOrganization() != null && orgUnitRepository.findByIdAndTenantId(row.getOrganization(), tenantId).isEmpty()) {
            warnings.add("organization not found " + row.getOrganization() + "; role was created without organization.");
            row.setOrganization(null);
        }

        if (row.getOwnerId() != null && existingIdentityIds(tenantId, List.of(row.getOwnerId())).isEmpty()) {
            warnings.add("ownerId not found " + row.getOwnerId() + "; role was created without owner.");
            row.setOwnerId(null);
        }

        row.setEntitlementIds(filterExistingIds("entitlementIds", row.getEntitlementIds(), existingEntitlementIds(tenantId, row.getEntitlementIds()), warnings));
        row.setChildRoleIds(filterExistingIds("childRoleIds", row.getChildRoleIds(), existingChildRoleIds(tenantId, row.getChildRoleIds()), warnings));
        row.setIdentityIds(filterExistingIds("identityIds", row.getIdentityIds(), existingIdentityIds(tenantId, row.getIdentityIds()), warnings));
        row.setChildRoleRefs(filterResolvableChildRoleRefs(item, row.getChildRoleRefs(), warnings));

        return warnings;
    }

    private List<String> filterResolvableChildRoleRefs(RoleImportJobItem item,
                                                       List<String> childRoleRefs,
                                                       List<String> warnings) {
        if (childRoleRefs == null || childRoleRefs.isEmpty()) {
            return List.of();
        }

        List<String> resolvableRefs = new ArrayList<>();
        List<String> missingRefs = new ArrayList<>();
        List<String> failedRefs = new ArrayList<>();

        for (String rawRef : childRoleRefs) {
            String ref = rawRef == null ? null : rawRef.trim();
            if (ref == null || ref.isBlank()) {
                continue;
            }
            List<RoleImportJobItem> referencedItems = itemRepository
                    .findByJobIdAndExternalRefOrderByRowNumberAsc(item.getJobId(), ref);
            if (referencedItems.isEmpty()) {
                missingRefs.add(ref);
                continue;
            }
            RoleImportJobItem referencedItem = referencedItems.get(0);
            if (referencedItem.getStatus() == RoleImportItemStatus.FAILED) {
                failedRefs.add(ref + " (row " + referencedItem.getRowNumber() + ")");
                continue;
            }
            resolvableRefs.add(ref);
        }

        if (!missingRefs.isEmpty()) {
            warnings.add("childRoleRefs not found " + missingRefs + "; only resolvable childRoleRefs were linked.");
        }
        if (!failedRefs.isEmpty()) {
            warnings.add("childRoleRefs reference failed rows " + failedRefs + "; only resolvable childRoleRefs were linked.");
        }
        return resolvableRefs;
    }

    /** Creates roles with a temporary security context whose name is the import initiator username for audit. */
    private AccessRoleResponse createRoleWithImportActor(RoleImportJobItem item, RoleImportRowDto row) {
        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        String actorUsername = actorUsernameFor(item.getJobId(), item.getTenantId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(actorUsername, null, List.of());
        authentication.setDetails(AccessRoleAuditConstants.TRIGGER_IMPORT);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            return accessRoleService.createRole(item.getTenantId(), row);
        } finally {
            SecurityContext previousContext = SecurityContextHolder.createEmptyContext();
            previousContext.setAuthentication(previousAuthentication);
            SecurityContextHolder.setContext(previousContext);
        }
    }
    /**
     * Resolves import-local child references after CREATE completes.
     *
     * <p>Only rows that succeeded in CREATE are considered. Failed rows do not
     * participate in reference resolution because they have no created role ID.</p>
     */
    private void processWire(UUID jobId) {
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_WIRE_PHASE_START)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                .log();
        Map<String, UUID> refs = buildImportedRoleRefs(jobId);
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_WIRE_REF_MAP_BUILT)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                .detail(AccessRoleImportConstants.DETAIL_REF_COUNT, String.valueOf(refs.size()))
                .log();
        int lastRowNumber = 0;
        while (true) {
            List<RoleImportJobItem> batch = itemRepository
                    .findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                            jobId,
                            RoleImportItemPhase.CREATE,
                            RoleImportItemStatus.SUCCEEDED,
                            lastRowNumber,
                            PageRequest.of(0, batchSize));
            if (batch.isEmpty()) {
                refresh(jobId);
                return;
            }
            logger.info()
                    .category(AccessRoleImportConstants.LOG_CATEGORY)
                    .message(AccessRoleImportConstants.LOG_WIRE_BATCH)
                    .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                    .detail(AccessRoleImportConstants.DETAIL_BATCH_COUNT, String.valueOf(batch.size()))
                    .log();
            for (RoleImportJobItem item : batch) {
                lastRowNumber = item.getRowNumber();
                wireOne(item.getId(), refs);
            }
        }
    }
    /** Builds a lookup from import {@code externalRef} to the created role id. */
    private Map<String, UUID> buildImportedRoleRefs(UUID jobId) {
        Map<String, UUID> refs = new HashMap<>();
        int lastRowNumber = 0;
        while (true) {
            List<RoleImportJobItem> batch = itemRepository
                    .findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                            jobId,
                            RoleImportItemPhase.CREATE,
                            RoleImportItemStatus.SUCCEEDED,
                            lastRowNumber,
                            PageRequest.of(0, batchSize));
            if (batch.isEmpty()) return refs;
            for (RoleImportJobItem item : batch) {
                lastRowNumber = item.getRowNumber();
                if (item.getExternalRef() != null && item.getRoleId() != null) {
                    refs.put(item.getExternalRef().trim(), item.getRoleId());
                }
            }
        }
    }
    /** Links one successfully created parent role to imported child roles by {@code childRoleRefs}. */
    private void wireOne(UUID itemId, Map<String, UUID> refs) {
        try {
            tx.executeWithoutResult(s -> wireOneInTransaction(itemId, refs));
        } catch (Exception ex) {
            appendWarning(itemId, "childRoleRefs could not be fully linked: " + friendlyMessage(ex));
        }
    }
    /** Performs child-reference wiring inside one rollback-isolated row transaction. */
    private void wireOneInTransaction(UUID itemId, Map<String, UUID> refs) {
        RoleImportJobItem item = itemRepository.findById(itemId).orElseThrow();
        RoleImportRowDto row = readRow(item);
        if (row.getChildRoleRefs() == null || row.getChildRoleRefs().isEmpty()) return;
        List<String> warnings = new ArrayList<>();
        List<UUID> childIds = new ArrayList<>();
        for (String rawRef : row.getChildRoleRefs()) {
            String ref = rawRef == null ? null : rawRef.trim();
            if (ref == null || ref.isBlank()) {
                continue;
            }
            UUID id = refs.get(ref);
            if (id == null) {
                warnings.add("childRoleRefs not found or not created [" + ref + "]; only resolvable childRoleRefs were linked.");
            } else {
                childIds.add(id);
            }
        }
        if (childIds.isEmpty()) {
            appendWarnings(item, warnings);
            itemRepository.save(item);
            return;
        }
        AddChildRolesResultDto result = compositionService.addChildRoles(item.getTenantId(), item.getRoleId(), childIds);
        if (result.hasFailures()) {
            String failedChildren = result.getFailed().stream()
                    .map(failed -> failed.getChildRoleId() + ": " + failed.getReason())
                    .collect(Collectors.joining(", "));
            warnings.add("Some childRoleRefs failed to link [" + failedChildren + "].");
        }
        appendWarnings(item, warnings);
        itemRepository.save(item);
    }

    /** Assigns successfully imported roles to row identityIds after child-role wiring is complete. */
    private void processAssignments(UUID jobId, ImportAssignmentAuditTracker assignmentTracker) {
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_ASSIGN_PHASE_START)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                .log();
        int lastRowNumber = 0;
        while (true) {
            List<RoleImportJobItem> batch = itemRepository
                    .findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                            jobId,
                            RoleImportItemPhase.CREATE,
                            RoleImportItemStatus.SUCCEEDED,
                            lastRowNumber,
                            PageRequest.of(0, batchSize));
            if (batch.isEmpty()) {
                refresh(jobId);
                return;
            }
            logger.info()
                    .category(AccessRoleImportConstants.LOG_CATEGORY)
                    .message(AccessRoleImportConstants.LOG_ASSIGN_BATCH)
                    .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                    .detail(AccessRoleImportConstants.DETAIL_BATCH_COUNT, String.valueOf(batch.size()))
                    .log();
            for (RoleImportJobItem item : batch) {
                lastRowNumber = item.getRowNumber();
                assignOne(item.getId(), assignmentTracker);
            }
        }
    }

    private void assignOne(UUID itemId, ImportAssignmentAuditTracker assignmentTracker) {
        RoleImportJobItem item = itemRepository.findById(itemId).orElseThrow();
        RoleImportRowDto row = readRow(item);
        List<UUID> identityIds = row.getIdentityIds();
        if (identityIds == null || identityIds.isEmpty()) return;
        assignmentTracker.recordAttempted(identityIds.size());
        try {
            tx.executeWithoutResult(s -> assignOneInTransaction(itemId, item, row));
            identityIds.forEach(identityId -> assignmentTracker.recordSucceeded(item, row, identityId));
        } catch (Exception ex) {
            identityIds.forEach(identityId -> assignmentTracker.recordFailed(item, row, identityId, friendlyMessage(ex)));
            appendWarning(itemId, "identityIds could not be fully assigned: " + friendlyMessage(ex));
        }
    }

    private void assignOneInTransaction(UUID itemId, RoleImportJobItem expectedItem, RoleImportRowDto row) {
        RoleImportJobItem item = itemRepository.findById(itemId).orElse(expectedItem);
        if (item.getRoleId() == null) {
            throw new IllegalStateException("Cannot assign identityIds because imported roleId is missing");
        }
        Optional<RoleImportJob> job = jobRepository.findById(item.getJobId());
        String actorId = job.map(RoleImportJob::getActorId).orElse(null);
        String actorUsername = job.map(candidate -> resolveActorUsername(candidate, item.getTenantId())).orElse("role-import");
        for (UUID identityId : row.getIdentityIds()) {
            identityRoleAssignmentService.assignRole(
                    item.getTenantId(),
                    identityId,
                    item.getRoleId(),
                    IdentityRoleAssignment.SourceType.MANUAL,
                    null,
                    actorId,
                    actorUsername);
        }
    }

    /** Moves a pending job into RUNNING state and records the start timestamp. */
    private void setRunning(UUID jobId) {
        tx.executeWithoutResult(s -> jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() == RoleImportJobStatus.PENDING) {
                job.setStatus(RoleImportJobStatus.RUNNING);
                job.setStartedAt(Instant.now());
                jobRepository.save(job);
                logger.info()
                        .category(AccessRoleImportConstants.LOG_CATEGORY)
                        .message(AccessRoleImportConstants.LOG_JOB_RUNNING)
                        .detail(AccessRoleImportConstants.DETAIL_TENANT_ID, job.getTenantId())
                        .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                        .log();
            }
        }));
    }
    /** Refreshes job counters from item status counts. */
    private void refresh(UUID jobId) {
        tx.executeWithoutResult(s -> jobRepository.findById(jobId).ifPresent(this::refreshCounts));
    }

    /** Calculates final status, marks the job complete or failed, and returns it only once. */
    private Optional<RoleImportJob> finish(UUID jobId, String error) {
        Optional<RoleImportJob> completedJob = tx.execute(s -> jobRepository.findById(jobId)
                .filter(job -> job.getCompletedAt() == null)
                .map(job -> {
                    refreshCounts(job);
                    job.setCompletedAt(Instant.now());
                    if (error != null) {
                        job.setStatus(RoleImportJobStatus.FAILED);
                        job.setErrorMessage(error);
                    } else if (job.getFailedCount() > 0) {
                        job.setStatus(RoleImportJobStatus.COMPLETED_WITH_ERRORS);
                        job.setErrorMessage(null);
                    } else {
                        job.setStatus(RoleImportJobStatus.COMPLETED);
                        job.setErrorMessage(null);
                    }
                    jobRepository.save(job);
                    logger.info()
                            .category(AccessRoleImportConstants.LOG_CATEGORY)
                            .message(AccessRoleImportConstants.LOG_JOB_FINISHED)
                            .detail(AccessRoleImportConstants.DETAIL_TENANT_ID, job.getTenantId())
                            .detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
                            .detail(AccessRoleImportConstants.DETAIL_STATUS, job.getStatus().name())
                            .detail(AccessRoleImportConstants.DETAIL_SUCCEEDED_COUNT, String.valueOf(job.getSucceededCount()))
                            .detail(AccessRoleImportConstants.DETAIL_FAILED_COUNT, String.valueOf(job.getFailedCount()))
                            .detail(AccessRoleImportConstants.DETAIL_SKIPPED_COUNT, String.valueOf(job.getSkippedCount()))
                            .log();
                    return job;
                }));
        return completedJob;
    }

    /** Recomputes item counters from the item table so job progress remains DB-backed. */
    private void refreshCounts(RoleImportJob job) {
        UUID id = job.getId();
        job.setSucceededCount(itemRepository.countByJobIdAndRoleIdIsNotNull(id));
        job.setFailedCount(itemRepository.countByJobIdAndStatus(id, RoleImportItemStatus.FAILED));
        job.setSkippedCount(itemRepository.countByJobIdAndStatus(id, RoleImportItemStatus.SKIPPED));
        jobRepository.save(job);
    }
    /** Resolves the username of the actor originally stored on the job for rule creation/audit context. */
    private String actorUsernameFor(UUID jobId, String tenantId) {
        return jobRepository.findById(jobId)
                .map(job -> resolveActorUsername(job, tenantId))
                .orElse("role-import");
    }
    /** Deserializes the persisted row payload and converts unreadable payloads into a user-facing row error. */
    private RoleImportRowDto readRow(RoleImportJobItem item) {
        try {
            return objectMapper.treeToValue(item.getPayload(), RoleImportRowDto.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(AccessRoleImportConstants.USER_ERROR_PAYLOAD_UNREADABLE, ex);
        }
    }
    private void publishImportAudit(RoleImportJob job, ImportAssignmentAuditTracker assignmentTracker) {
        String actorUsername = resolveActorUsername(job, job.getTenantId());
        String riskLevel = resolveHighestImportRisk(job.getId());
        auditPublisher.publish(RoleImportJobAuditEvent.finished(
                job.getActorId(),
                actorUsername,
                job.getTenantId(),
                job.getId().toString(),
                job.getStatus().name(),
                riskLevel,
                buildAuditSummary(job, assignmentTracker)));
    }
    private ObjectNode buildAuditSummary(RoleImportJob job, ImportAssignmentAuditTracker assignmentTracker) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put(RoleImportJobAuditConstants.FIELD_JOB_ID, job.getId().toString());
        summary.put(RoleImportJobAuditConstants.FIELD_TENANT_ID, job.getTenantId());
        putIfNotNull(summary, RoleImportJobAuditConstants.FIELD_ACTOR_ID, job.getActorId());
        putIfNotNull(summary, RoleImportJobAuditConstants.FIELD_ACTOR_USERNAME,
                resolveActorUsername(job, job.getTenantId()));
        putIfNotNull(summary, RoleImportJobAuditConstants.FIELD_FORMAT, job.getFormat());
        summary.put(RoleImportJobAuditConstants.FIELD_STATUS, job.getStatus().name());
        putIfNotNull(summary, RoleImportJobAuditConstants.FIELD_RISK_LEVEL, resolveHighestImportRisk(job.getId()));
        putIfNotNull(summary, RoleImportJobAuditConstants.FIELD_ERROR_MESSAGE, job.getErrorMessage());
        putInstantIfNotNull(summary, RoleImportJobAuditConstants.FIELD_CREATED_AT, job.getCreatedAt());
        putInstantIfNotNull(summary, RoleImportJobAuditConstants.FIELD_STARTED_AT, job.getStartedAt());
        putInstantIfNotNull(summary, RoleImportJobAuditConstants.FIELD_COMPLETED_AT, job.getCompletedAt());
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            summary.put(RoleImportJobAuditConstants.FIELD_DURATION_MS,
                    Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis());
        }
        ObjectNode counts = summary.putObject(RoleImportJobAuditConstants.FIELD_COUNTS);
        counts.put(RoleImportJobAuditConstants.FIELD_TOTAL, job.getTotalCount());
        counts.put(RoleImportJobAuditConstants.FIELD_SUCCEEDED, job.getSucceededCount());
        counts.put(RoleImportJobAuditConstants.FIELD_FAILED, job.getFailedCount());
        counts.put(RoleImportJobAuditConstants.FIELD_SKIPPED, job.getSkippedCount());
        ObjectNode assignments = summary.putObject(RoleImportJobAuditConstants.FIELD_ASSIGNMENTS);
        assignments.put(RoleImportJobAuditConstants.FIELD_ATTEMPTED, assignmentTracker.attemptedCount());
        assignments.put(RoleImportJobAuditConstants.FIELD_SUCCEEDED, assignmentTracker.succeededAssignments().size());
        assignments.put(RoleImportJobAuditConstants.FIELD_FAILED, assignmentTracker.failedAssignments().size());
        ArrayNode createdRoles = summary.putArray(RoleImportJobAuditConstants.FIELD_CREATED_ROLES);
        ArrayNode failedRows = summary.putArray(RoleImportJobAuditConstants.FIELD_FAILED_ROWS);
        for (RoleImportJobItem item : itemRepository.findByJobId(job.getId(), Pageable.unpaged()).getContent()) {
            RoleImportRowDto row = tryReadRow(item);
            if (item.getRoleId() != null) {
                createdRoles.add(roleItemNode(item, row));
            }
            if (item.getStatus() == RoleImportItemStatus.FAILED) {
                failedRows.add(failedItemNode(item, row));
            }
        }
        ArrayNode succeededAssignments = summary.putArray(RoleImportJobAuditConstants.FIELD_SUCCEEDED_ASSIGNMENTS);
        assignmentTracker.succeededAssignments().forEach(succeededAssignments::add);
        ArrayNode failedAssignments = summary.putArray(RoleImportJobAuditConstants.FIELD_FAILED_ASSIGNMENTS);
        assignmentTracker.failedAssignments().forEach(failedAssignments::add);
        return summary;
    }
    private String resolveHighestImportRisk(UUID jobId) {
        String highest = null;
        for (RoleImportJobItem item : itemRepository.findByJobId(jobId, Pageable.unpaged()).getContent()) {
            RoleImportRowDto row = tryReadRow(item);
            highest = higherRisk(highest, row != null ? row.getRiskLevel() : null);
            if ("high".equals(highest)) {
                return highest;
            }
        }
        return highest != null ? highest : "low";
    }
    private String resolveActorUsername(RoleImportJob job, String tenantId) {
        if (job.getActorUsername() != null && !job.getActorUsername().isBlank()) {
            return job.getActorUsername();
        }
        return resolveActorUsername(tenantId, job.getActorId());
    }
    private String resolveActorUsername(String tenantId, String actorId) {
        if (actorId == null || actorId.isBlank()) {
            return "role-import";
        }
        try {
            UUID identityId = UUID.fromString(actorId);
            return identityRepository.findByTenantIdAndId(tenantId, identityId)
                    .map(this::identityDisplayName)
                    .filter(value -> value != null && !value.isBlank())
                    .orElse(actorId);
        } catch (IllegalArgumentException ex) {
            return actorId;
        }
    }
    private String identityDisplayName(Identity identity) {
        if (identity instanceof HumanIdentity human) {
            if (human.getUsername() != null && !human.getUsername().isBlank()) {
                return human.getUsername();
            }
            if (human.getDisplayName() != null && !human.getDisplayName().isBlank()) {
                return human.getDisplayName();
            }
            return human.getEmail();
        }
        if (identity instanceof NonHumanIdentity nonHuman) {
            return nonHuman.getServiceName();
        }
        return identity.getId() != null ? identity.getId().toString() : null;
    }
    private String higherRisk(String current, String candidate) {
        int currentRank = riskRank(current);
        int candidateRank = riskRank(candidate);
        return candidateRank > currentRank ? normalizeRisk(candidate) : normalizeRisk(current);
    }
    private int riskRank(String risk) {
        String normalized = normalizeRisk(risk);
        if ("high".equals(normalized)) return 3;
        if ("medium".equals(normalized)) return 2;
        if ("low".equals(normalized)) return 1;
        return 0;
    }
    private String normalizeRisk(String risk) {
        if (risk == null || risk.isBlank()) {
            return null;
        }
        String normalized = risk.trim().toLowerCase();
        if ("critical".equals(normalized) || "high".equals(normalized)) return "high";
        if ("medium".equals(normalized)) return "medium";
        if ("low".equals(normalized)) return "low";
        return null;
    }
    private ObjectNode roleItemNode(RoleImportJobItem item, RoleImportRowDto row) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(RoleImportJobAuditConstants.FIELD_ROW_NUMBER, item.getRowNumber());
        putIfNotNull(node, RoleImportJobAuditConstants.FIELD_EXTERNAL_REF, item.getExternalRef());
        putIfNotNull(node, RoleImportJobAuditConstants.FIELD_ROLE_ID, item.getRoleId() == null ? null : item.getRoleId().toString());
        if (row != null) {
            putIfNotNull(node, RoleImportJobAuditConstants.FIELD_ROLE_NAME, row.getName());
            putIfNotNull(node, RoleImportJobAuditConstants.FIELD_ROLE_TYPE, row.getType());
            putIfNotNull(node, RoleImportJobAuditConstants.FIELD_RISK_LEVEL, row.getRiskLevel());
        }
        return node;
    }
    private ObjectNode failedItemNode(RoleImportJobItem item, RoleImportRowDto row) {
        ObjectNode node = roleItemNode(item, row);
        putIfNotNull(node, RoleImportJobAuditConstants.FIELD_ERROR_CODE, item.getErrorCode());
        putIfNotNull(node, RoleImportJobAuditConstants.FIELD_REASON, item.getErrorMessage());
        return node;
    }
    private RoleImportRowDto tryReadRow(RoleImportJobItem item) {
        try {
            return readRow(item);
        } catch (RuntimeException ex) {
            return null;
        }
    }
    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }
    private void putInstantIfNotNull(ObjectNode node, String key, Instant value) {
        if (value != null) {
            node.put(key, value.toString());
        }
    }
    private final class ImportAssignmentAuditTracker {
        private int attemptedCount;
        private final List<ObjectNode> succeededAssignments = new ArrayList<>();
        private final List<ObjectNode> failedAssignments = new ArrayList<>();
        void recordAttempted(int count) {
            attemptedCount += count;
        }
        void recordSucceeded(RoleImportJobItem item, RoleImportRowDto row, UUID identityId) {
            succeededAssignments.add(assignmentNode(item, row, identityId, null));
        }
        void recordFailed(RoleImportJobItem item, RoleImportRowDto row, UUID identityId, String reason) {
            failedAssignments.add(assignmentNode(item, row, identityId, reason));
        }
        int attemptedCount() {
            return attemptedCount;
        }
        List<ObjectNode> succeededAssignments() {
            return succeededAssignments;
        }
        List<ObjectNode> failedAssignments() {
            return failedAssignments;
        }
        private ObjectNode assignmentNode(RoleImportJobItem item, RoleImportRowDto row, UUID identityId, String reason) {
            ObjectNode node = roleItemNode(item, row);
            node.put(RoleImportJobAuditConstants.FIELD_IDENTITY_ID, identityId.toString());
            putIfNotNull(node, RoleImportJobAuditConstants.FIELD_REASON, reason);
            return node;
        }
    }
    /** Saves failed row status after the failed create/wire transaction has already rolled back. */
    private void markItemFailed(UUID itemId, String errorCode, RoleImportItemPhase phase, String logMessage, Exception ex) {
        tx.executeWithoutResult(s -> {
            RoleImportJobItem item = itemRepository.findById(itemId).orElseThrow();
            RoleImportRowDto row = tryReadRow(item);
            logger.warn()
                    .category(AccessRoleImportConstants.LOG_CATEGORY)
                    .message(logMessage)
                    .detail(AccessRoleImportConstants.DETAIL_JOB_ID, item.getJobId().toString())
                    .detail(AccessRoleImportConstants.DETAIL_ITEM_ID, item.getId().toString())
                    .detail(AccessRoleImportConstants.DETAIL_TENANT_ID, item.getTenantId())
                    .detail(AccessRoleImportConstants.DETAIL_ROW_NUMBER, String.valueOf(item.getRowNumber()))
                    .detail(AccessRoleImportConstants.DETAIL_ERROR_CODE, errorCode)
                    .detail(AccessRoleImportConstants.DETAIL_ERROR_MESSAGE, ex.getMessage())
                    .log();
            fail(item, errorCode, phase, row, ex);
            item.setProcessedAt(Instant.now());
            itemRepository.save(item);
        });
    }
    /** Stores a safe, bounded, user-friendly error summary on a failed row. */
    private static void fail(RoleImportJobItem item, String code, RoleImportItemPhase phase, RoleImportRowDto row, Exception ex) {
        item.setStatus(RoleImportItemStatus.FAILED);
        item.setPhase(phase);
        item.setErrorCode(code);
        String msg = friendlyItemMessage(item, phase, row, ex);
        item.setErrorMessage(msg.length() > 2000 ? msg.substring(0, 2000) : msg);
    }
    /** Adds role name / created-role context around the low-level friendly message. */
    private static String friendlyItemMessage(RoleImportJobItem item, RoleImportItemPhase phase, RoleImportRowDto row, Exception ex) {
        String roleName = row != null && row.getName() != null && !row.getName().isBlank()
                ? "Role '" + row.getName().trim() + "'"
                : "This row";
        String reason = friendlyMessage(ex);
        if (item.getRoleId() != null && phase == RoleImportItemPhase.WIRE) {
            return roleName + " was created with roleId " + item.getRoleId()
                    + ", but child-role linking failed. " + reason;
        }
        if (item.getRoleId() != null && phase == RoleImportItemPhase.ASSIGN) {
            return roleName + " was created with roleId " + item.getRoleId()
                    + ", but identity assignment failed. " + reason;
        }
        if (phase == RoleImportItemPhase.CREATE) {
            return roleName + " could not be created. " + reason;
        }
        return roleName + " could not be fully processed. " + reason;
    }
    /** Maps low-level service exceptions into messages that explain the import-file fix. */
    private static String friendlyMessage(Exception ex) {
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        Optional<ConstraintViolationException> validation = causeOf(ex, ConstraintViolationException.class);
        if (validation.isPresent()) {
            String fields = validation.get().getConstraintViolations().stream()
                    .map(RoleImportJobWorker::constraintViolationMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            return AccessRoleImportConstants.USER_ERROR_CONSTRAINT_VIOLATION
                    + (fields.isBlank() ? "" : " Fields: " + fields + ".");
        }
        Optional<DataIntegrityViolationException> dataIntegrity = causeOf(ex, DataIntegrityViolationException.class);
        if (dataIntegrity.isPresent()) {
            return AccessRoleImportConstants.USER_ERROR_DATA_INTEGRITY + " Details: " + compactMessage(dataIntegrity.get());
        }
        if (msg.contains("Access role not found for tenant")) {
            return AccessRoleImportConstants.USER_ERROR_CHILD_ROLE_ID_NOT_FOUND + " Details: " + msg;
        }
        if (msg.contains("Unknown childRoleRef:")) {
            return String.format(AccessRoleImportConstants.USER_ERROR_UNKNOWN_CHILD_ROLE_REF,
                    msg.substring(msg.indexOf(':') + 1).trim());
        }
        if (msg.contains("already exists for this tenant")) {
            return AccessRoleImportConstants.USER_ERROR_DUPLICATE_ROLE_NAME + " Details: " + msg;
        }
        if (msg.contains("Some childRoleRefs failed to link")) {
            return AccessRoleImportConstants.USER_ERROR_CHILD_ROLE_LINK_FAILED;
        }
        if (msg.contains("Identity not found for tenant") || msg.contains("Identity ID is required")) {
            return AccessRoleImportConstants.USER_ERROR_IDENTITY_ASSIGNMENT_FAILED + " Details: " + msg;
        }
        if (msg.toLowerCase().contains("constraint") || msg.toLowerCase().contains("not-null")
                || msg.toLowerCase().contains("null value in column")) {
            return AccessRoleImportConstants.USER_ERROR_DATA_INTEGRITY + " Details: " + compactMessage(ex);
        }
        return AccessRoleImportConstants.USER_ERROR_UNEXPECTED_PROCESSING + " Details: " + msg;
    }
    private static String constraintViolationMessage(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() == null ? "field" : violation.getPropertyPath().toString();
        int lastDot = field.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < field.length() - 1) {
            field = field.substring(lastDot + 1);
        }
        return field + " " + violation.getMessage();
    }
    private static String compactMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage() != null ? root.getMessage() : throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message.replaceAll("\\s+", " ").trim();
    }
    private static <T extends Throwable> Optional<T> causeOf(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return Optional.of(type.cast(current));
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}
