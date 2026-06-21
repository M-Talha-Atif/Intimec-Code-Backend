package com.aidentx.iam.organization_management.controller;

import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord;
import com.aidentx.iam.organization_management.model.entity.OrgAssignmentRunRecord.Outcome;
import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.repository.OrgAssignmentRunRecordRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import com.aidentx.iam.security.JwtTenantIdService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only surface over the {@code org_assignment_run_records} table that
 * powers the "Organization Reconciliation" section in the task-run details
 * page. Returns both a per-outcome summary and a paginated list of
 * per-identity rows, enriched with the identity display name and resolved
 * org unit name so the UI can render them without extra round trips.
 */
@RestController
@RequestMapping("/api/v1/admin/org-assignment-runs")
public class OrgAssignmentRunController {

    public record OrgAssignmentRunRowView(
            UUID id,
            UUID identityId,
            String identityDisplayName,
            String identityEmail,
            String attemptedValue,
            String outcome,
            UUID resolvedOrgUnitId,
            String resolvedOrgUnitName,
            UUID previousOrgUnitId,
            String previousOrgUnitName,
            String detail,
            Instant occurredAt) {}

    public record OrgAssignmentRunSummaryView(
            String taskRunId,
            long total,
            Map<String, Long> outcomeCounts) {}

    public record OrgAssignmentRunResponse(
            OrgAssignmentRunSummaryView summary,
            Page<OrgAssignmentRunRowView> rows) {}

    private final OrgAssignmentRunRecordRepository repository;
    private final IdentityRepository identityRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final JwtTenantIdService jwtTenantIdService;

    public OrgAssignmentRunController(OrgAssignmentRunRecordRepository repository,
                                      IdentityRepository identityRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      JwtTenantIdService jwtTenantIdService) {
        this.repository = repository;
        this.identityRepository = identityRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    @GetMapping
    public ResponseEntity<OrgAssignmentRunResponse> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam("taskRunId") String taskRunId,
            @RequestParam(value = "outcome", required = false) String outcomeFilter,
            Pageable pageable) {
        String tenantId = resolveTenantId(tenantHeader);

        Page<OrgAssignmentRunRecord> page;
        if (outcomeFilter != null && !outcomeFilter.isBlank()) {
            Outcome filter;
            try {
                filter = Outcome.valueOf(outcomeFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().build();
            }
            page = repository.findByTenantIdAndTaskRunIdAndOutcomeOrderByOccurredAtDesc(
                    tenantId, taskRunId, filter, pageable);
        } else {
            page = repository.findByTenantIdAndTaskRunIdOrderByOccurredAtDesc(
                    tenantId, taskRunId, pageable);
        }

        Set<UUID> identityIds = new HashSet<>();
        Set<UUID> orgUnitIds = new HashSet<>();
        for (OrgAssignmentRunRecord r : page.getContent()) {
            identityIds.add(r.getIdentityId());
            if (r.getResolvedOrgUnitId() != null) orgUnitIds.add(r.getResolvedOrgUnitId());
            if (r.getPreviousOrgUnitId() != null) orgUnitIds.add(r.getPreviousOrgUnitId());
        }

        Map<UUID, Identity> identityById = identityRepository.findAllById(identityIds).stream()
                .collect(Collectors.toMap(Identity::getId, i -> i));
        Map<UUID, OrgUnit> orgUnitById = orgUnitRepository.findAllById(orgUnitIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, o -> o));

        Page<OrgAssignmentRunRowView> rowPage = page.map(r -> toRow(r, identityById, orgUnitById));

        // Summary always reflects the FULL run, ignoring the outcome filter so the
        // chips show the global counts even when the user has drilled into one
        // bucket.
        List<Object[]> rawCounts = repository.countOutcomesByRun(tenantId, taskRunId);
        Map<String, Long> outcomeCounts = new java.util.LinkedHashMap<>();
        for (Outcome o : Outcome.values()) {
            outcomeCounts.put(o.name(), 0L);
        }
        long total = 0L;
        for (Object[] row : rawCounts) {
            Outcome o = (Outcome) row[0];
            Long count = (Long) row[1];
            outcomeCounts.put(o.name(), count);
            total += count;
        }

        OrgAssignmentRunSummaryView summary = new OrgAssignmentRunSummaryView(
                taskRunId, total, outcomeCounts);

        return ResponseEntity.ok(new OrgAssignmentRunResponse(summary, rowPage));
    }

    private OrgAssignmentRunRowView toRow(OrgAssignmentRunRecord r,
                                          Map<UUID, Identity> identityById,
                                          Map<UUID, OrgUnit> orgUnitById) {
        Identity identity = identityById.get(r.getIdentityId());
        String displayName = null;
        String email = null;
        if (identity instanceof HumanIdentity human) {
            displayName = human.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                String first = human.getFirstName() == null ? "" : human.getFirstName();
                String last = human.getLastName() == null ? "" : human.getLastName();
                String combined = (first + " " + last).trim();
                displayName = combined.isEmpty() ? null : combined;
            }
            email = human.getEmail();
        }

        OrgUnit resolved = r.getResolvedOrgUnitId() == null ? null : orgUnitById.get(r.getResolvedOrgUnitId());
        OrgUnit previous = r.getPreviousOrgUnitId() == null ? null : orgUnitById.get(r.getPreviousOrgUnitId());

        return new OrgAssignmentRunRowView(
                r.getId(),
                r.getIdentityId(),
                displayName,
                email,
                r.getAttemptedValue(),
                r.getOutcome().name(),
                r.getResolvedOrgUnitId(),
                resolved == null ? null : resolved.getName(),
                r.getPreviousOrgUnitId(),
                previous == null ? null : previous.getName(),
                r.getDetail(),
                r.getOccurredAt());
    }

    private String resolveTenantId(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) return headerValue;
        String fromJwt = jwtTenantIdService.extractTenantIdFromRequest();
        if (fromJwt == null) {
            throw new IllegalStateException("Tenant id could not be resolved from JWT or X-Tenant-Id header");
        }
        return fromJwt;
    }
}
