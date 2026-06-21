package com.aidentx.iam.organization_management.controller;

import com.aidentx.iam.organization_management.model.entity.OrgResolutionFailure;
import com.aidentx.iam.organization_management.repository.OrgResolutionFailureRepository;
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
import java.util.UUID;

/**
 * Admin-only read surface over the {@code org_resolution_failures} audit table.
 *
 * <p>Operators consume this endpoint to triage onboarding gaps surfaced by the
 * reconciliation org-assignment pass: missing org units, ambiguous names,
 * and per-identity exceptions captured by {@code IdentityOrgSyncService}.
 *
 * <p>Failure rows self-clear: a subsequent successful resolution for the same
 * identity flips {@code resolved=true} via
 * {@link OrgResolutionFailureRepository#markResolvedForIdentity}.
 */
@RestController
@RequestMapping("/api/v1/admin/org-resolution-failures")
public class OrgResolutionFailureController {

    public record OrgResolutionFailureView(
            UUID id,
            UUID identityId,
            String attemptedValue,
            String hierarchyType,
            String reason,
            String detail,
            Instant occurredAt,
            boolean resolved,
            Instant resolvedAt) {

        static OrgResolutionFailureView from(OrgResolutionFailure f) {
            return new OrgResolutionFailureView(
                    f.getId(),
                    f.getIdentityId(),
                    f.getAttemptedValue(),
                    f.getHierarchyType() == null ? null : f.getHierarchyType().name(),
                    f.getReason(),
                    f.getDetail(),
                    f.getOccurredAt(),
                    f.isResolved(),
                    f.getResolvedAt());
        }
    }

    private final OrgResolutionFailureRepository repository;
    private final JwtTenantIdService jwtTenantIdService;

    public OrgResolutionFailureController(OrgResolutionFailureRepository repository,
                                          JwtTenantIdService jwtTenantIdService) {
        this.repository = repository;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    @GetMapping
    public ResponseEntity<Page<OrgResolutionFailureView>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam(value = "resolved", defaultValue = "false") boolean resolved,
            @RequestParam(value = "taskRunId", required = false) String taskRunId,
            Pageable pageable) {
        String tenantId = resolveTenantId(tenantHeader);
        Page<OrgResolutionFailure> page = (taskRunId != null && !taskRunId.isBlank())
                ? repository.findByTenantIdAndTaskRunIdOrderByOccurredAtDesc(tenantId, taskRunId, pageable)
                : repository.findByTenantIdAndResolvedOrderByOccurredAtDesc(tenantId, resolved, pageable);
        return ResponseEntity.ok(page.map(OrgResolutionFailureView::from));
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
