package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.entity.IdentityOrgAssignment;
import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord;
import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord.Outcome;
import com.aidentx.iam.organization_management.model.entity.OrgResolutionFailure;
import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.model.enums.AssignmentType;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.repository.IdentityOrgAssignmentRepository;
import com.aidentx.iam.organization_management.repository.OrgAssignmentRunRecordRepository;
import com.aidentx.iam.organization_management.repository.OrgResolutionFailureRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a free-text source attribute (typically {@code Employee.department})
 * to a FUNCTIONAL {@link OrgUnit} and keeps the primary
 * {@link IdentityOrgAssignment} row in sync.
 *
 * <p>Two return styles are exposed:
 * <ul>
 *   <li>{@link #syncPrimaryFunctionalAssignment(String, UUID, String, String, AssignmentType)}
 *       — legacy {@link Optional} for inline best-effort callers (import,
 *       HR sync). Failures still land in {@code org_resolution_failures}.</li>
 *   <li>{@link #syncPrimaryFunctionalAssignmentDetailed(String, UUID, String, String, AssignmentType, String)}
 *       — rich {@link OrgAssignmentResult} that also writes a per-identity
 *       row to {@code org_assignment_run_records} when a {@code taskRunId}
 *       is supplied, enabling the reconciliation run UI to render every
 *       outcome (assigned, reassigned, unchanged, failed).</li>
 * </ul>
 */
@Service
public class IdentityOrgSyncService {

    private static final Logger log = LoggerFactory.getLogger(IdentityOrgSyncService.class);

    private final OrgUnitRepository orgUnitRepository;
    private final IdentityOrgAssignmentRepository assignmentRepository;
    private final OrgResolutionFailureRepository failureRepository;
    private final OrgAssignmentRunRecordRepository runRecordRepository;

    public IdentityOrgSyncService(OrgUnitRepository orgUnitRepository,
                                  IdentityOrgAssignmentRepository assignmentRepository,
                                  OrgResolutionFailureRepository failureRepository,
                                  OrgAssignmentRunRecordRepository runRecordRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.assignmentRepository = assignmentRepository;
        this.failureRepository = failureRepository;
        this.runRecordRepository = runRecordRepository;
    }

    @Transactional
    public Optional<UUID> syncPrimaryFunctionalAssignment(String tenantId,
                                                          UUID identityId,
                                                          String departmentName,
                                                          String actor,
                                                          AssignmentType assignmentType) {
        OrgAssignmentResult result = syncPrimaryFunctionalAssignmentDetailed(
                tenantId, identityId, departmentName, actor, assignmentType, null);
        return Optional.ofNullable(result.resolvedOrgUnitId());
    }

    @Transactional
    public Optional<UUID> syncPrimaryFunctionalAssignment(String tenantId,
                                                          UUID identityId,
                                                          String departmentName,
                                                          String actor,
                                                          AssignmentType assignmentType,
                                                          String taskRunId) {
        OrgAssignmentResult result = syncPrimaryFunctionalAssignmentDetailed(
                tenantId, identityId, departmentName, actor, assignmentType, taskRunId);
        return Optional.ofNullable(result.resolvedOrgUnitId());
    }

    /**
     * Core resolution + assignment routine. Returns the structured outcome
     * for callers that need to audit per-identity behavior. When
     * {@code taskRunId} is non-null, a row is written to
     * {@code org_assignment_run_records} for every identity (success,
     * skip, or failure) so the run-details UI can render the full audit.
     */
    @Transactional
    public OrgAssignmentResult syncPrimaryFunctionalAssignmentDetailed(String tenantId,
                                                                       UUID identityId,
                                                                       String departmentName,
                                                                       String actor,
                                                                       AssignmentType assignmentType,
                                                                       String taskRunId) {
        if (tenantId == null || identityId == null) {
            return OrgAssignmentResult.skippedBlank();
        }
        if (departmentName == null || departmentName.isBlank()) {
            OrgAssignmentResult result = OrgAssignmentResult.skippedBlank();
            recordRunRow(tenantId, taskRunId, identityId, null, result);
            return result;
        }

        String value = departmentName.trim();
        try {
            List<OrgUnit> matches = orgUnitRepository.findActiveByNameCodeOrExternalId(
                    tenantId, HierarchyType.FUNCTIONAL, value);

            if (matches.isEmpty()) {
                log.warn("Org resolution: no active FUNCTIONAL org unit matched value='{}' (externalId|code|name) for tenant={} identity={}",
                        value, tenantId, identityId);
                String detail = "No active FUNCTIONAL org unit matched by externalId, code, or name";
                recordFailure(tenantId, identityId, value, "NO_MATCH", detail, taskRunId);
                OrgAssignmentResult result = new OrgAssignmentResult(
                        Outcome.FAILED_NO_MATCH, null, currentPrimaryOrgUnitId(tenantId, identityId), detail);
                recordRunRow(tenantId, taskRunId, identityId, value, result);
                return result;
            }
            if (matches.size() > 1) {
                log.warn("Org resolution: ambiguous value='{}' in tenant={} identity={} ({} matches by externalId|code|name) - skipping",
                        value, tenantId, identityId, matches.size());
                String detail = "Matched " + matches.size() + " active org units by externalId, code, or name";
                recordFailure(tenantId, identityId, value, "AMBIGUOUS", detail, taskRunId);
                OrgAssignmentResult result = new OrgAssignmentResult(
                        Outcome.FAILED_AMBIGUOUS, null, currentPrimaryOrgUnitId(tenantId, identityId), detail);
                recordRunRow(tenantId, taskRunId, identityId, value, result);
                return result;
            }

            OrgUnit target = matches.get(0);
            UUID previousPrimary = currentPrimaryOrgUnitId(tenantId, identityId);

            Optional<IdentityOrgAssignment> existing = assignmentRepository
                    .findByTenantIdAndIdentityIdAndOrgUnitIdAndHierarchyType(
                            tenantId, identityId, target.getId(), HierarchyType.FUNCTIONAL);

            if (existing.isPresent()) {
                IdentityOrgAssignment a = existing.get();
                boolean changed = false;
                if (!a.isPrimary()) { a.setPrimary(true); changed = true; }
                if (a.getAssignmentType() != assignmentType) { a.setAssignmentType(assignmentType); changed = true; }
                if (changed) {
                    a.setUpdatedBy(actor);
                    assignmentRepository.save(a);
                }
                failureRepository.markResolvedForIdentity(tenantId, identityId, Instant.now());
                Outcome outcome = changed ? Outcome.REASSIGNED : Outcome.UNCHANGED;
                String detail = changed
                        ? "Promoted existing assignment to primary on " + target.getName()
                        : "Already primary on " + target.getName();
                OrgAssignmentResult result = new OrgAssignmentResult(
                        outcome, target.getId(), previousPrimary, detail);
                recordRunRow(tenantId, taskRunId, identityId, value, result);
                return result;
            }

            assignmentRepository
                    .findByTenantIdAndIdentityIdAndHierarchyTypeAndPrimaryTrue(
                            tenantId, identityId, HierarchyType.FUNCTIONAL)
                    .ifPresent(prev -> {
                        prev.setPrimary(false);
                        prev.setUpdatedBy(actor);
                        assignmentRepository.save(prev);
                    });

            String sourceSystem = assignmentType == AssignmentType.GOVERNANCE_MOVER
                    ? "IGA_MOVER"
                    : "EMPLOYEE_DEPARTMENT_SYNC";

            IdentityOrgAssignment created = new IdentityOrgAssignment();
            created.setTenantId(tenantId);
            created.setIdentityId(identityId);
            created.setOrgUnitId(target.getId());
            created.setHierarchyType(HierarchyType.FUNCTIONAL);
            created.setAssignmentType(assignmentType);
            created.setPrimary(true);
            created.setValidFrom(Instant.now());
            created.setSourceSystem(sourceSystem);
            created.setCreatedBy(actor);
            created.setUpdatedBy(actor);
            assignmentRepository.save(created);

            failureRepository.markResolvedForIdentity(tenantId, identityId, Instant.now());

            Outcome outcome = previousPrimary == null ? Outcome.ASSIGNED : Outcome.REASSIGNED;
            String detail = previousPrimary == null
                    ? "Created primary assignment to " + target.getName()
                    : "Reassigned to " + target.getName();
            log.info("Org assignment {}: identity={} -> orgUnit={} ({}) assignmentType={}",
                    outcome, identityId, target.getId(), target.getName(), assignmentType);
            OrgAssignmentResult result = new OrgAssignmentResult(
                    outcome, target.getId(), previousPrimary, detail);
            recordRunRow(tenantId, taskRunId, identityId, value, result);
            return result;

        } catch (RuntimeException ex) {
            log.warn("Org resolution failed for identity {} / department '{}': {}",
                    identityId, departmentName, ex.getMessage());
            String detail = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            recordFailure(tenantId, identityId, value, "ERROR", detail, taskRunId);
            OrgAssignmentResult result = new OrgAssignmentResult(
                    Outcome.FAILED_ERROR, null, currentPrimaryOrgUnitId(tenantId, identityId), detail);
            recordRunRow(tenantId, taskRunId, identityId, value, result);
            return result;
        }
    }

    private UUID currentPrimaryOrgUnitId(String tenantId, UUID identityId) {
        try {
            return assignmentRepository
                    .findByTenantIdAndIdentityIdAndHierarchyTypeAndPrimaryTrue(
                            tenantId, identityId, HierarchyType.FUNCTIONAL)
                    .map(IdentityOrgAssignment::getOrgUnitId)
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Persist a single audit row capturing why a resolution attempt failed,
     * deduplicating against the most recent unresolved entry for the same
     * identity + value pair to avoid runaway growth on repeated failures.
     */
    private void recordFailure(String tenantId, UUID identityId, String value,
                               String reason, String detail, String taskRunId) {
        try {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.length() > 512) {
                trimmed = trimmed.substring(0, 512);
            }
            String safeDetail = detail;
            if (safeDetail != null && safeDetail.length() > 1024) {
                safeDetail = safeDetail.substring(0, 1024);
            }
            Optional<OrgResolutionFailure> existing = failureRepository
                    .findFirstByTenantIdAndIdentityIdAndAttemptedValueAndResolvedFalseOrderByOccurredAtDesc(
                            tenantId, identityId, trimmed);
            if (existing.isPresent()) {
                OrgResolutionFailure f = existing.get();
                f.setReason(reason);
                f.setDetail(safeDetail);
                f.setOccurredAt(Instant.now());
                if (taskRunId != null) {
                    f.setTaskRunId(taskRunId);
                }
                failureRepository.save(f);
                return;
            }
            OrgResolutionFailure f = new OrgResolutionFailure();
            f.setTenantId(tenantId);
            f.setIdentityId(identityId);
            f.setAttemptedValue(trimmed);
            f.setHierarchyType(HierarchyType.FUNCTIONAL);
            f.setReason(reason);
            f.setDetail(safeDetail);
            f.setOccurredAt(Instant.now());
            f.setResolved(false);
            f.setTaskRunId(taskRunId);
            failureRepository.save(f);
        } catch (RuntimeException ex) {
            log.debug("Failed to persist org-resolution failure audit: {}", ex.getMessage());
        }
    }

    /**
     * Append a per-identity row to the run audit table. No-op when no
     * {@code taskRunId} is provided (inline import / HR-sync calls).
     */
    private void recordRunRow(String tenantId, String taskRunId, UUID identityId,
                              String attemptedValue, OrgAssignmentResult result) {
        if (taskRunId == null || taskRunId.isBlank()) {
            return;
        }
        try {
            String trimmedValue = attemptedValue == null ? null : attemptedValue.trim();
            if (trimmedValue != null && trimmedValue.length() > 512) {
                trimmedValue = trimmedValue.substring(0, 512);
            }
            String safeDetail = result.detail();
            if (safeDetail != null && safeDetail.length() > 1024) {
                safeDetail = safeDetail.substring(0, 1024);
            }
            OrgAssignmentRunRecord row = new OrgAssignmentRunRecord();
            row.setTenantId(tenantId);
            row.setTaskRunId(taskRunId);
            row.setIdentityId(identityId);
            row.setAttemptedValue(trimmedValue);
            row.setOutcome(result.outcome());
            row.setResolvedOrgUnitId(result.resolvedOrgUnitId());
            row.setPreviousOrgUnitId(result.previousOrgUnitId());
            row.setDetail(safeDetail);
            row.setOccurredAt(Instant.now());
            runRecordRepository.save(row);
        } catch (RuntimeException ex) {
            log.debug("Failed to persist org-assignment run record: {}", ex.getMessage());
        }
    }
}
