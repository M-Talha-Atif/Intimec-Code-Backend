package com.aidentx.iam.organization_management.controller;

import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentDto;
import com.aidentx.iam.organization_management.service.OrgUnitService;
import com.aidentx.iam.security.JwtTenantIdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Reverse view of identity-org assignments - lists every org unit a
 * given identity belongs to.
 */
@RestController
@RequestMapping("/api/v1/identities")
public class IdentityOrgAssignmentController {

    private final OrgUnitService service;
    private final JwtTenantIdService jwtTenantIdService;

    public IdentityOrgAssignmentController(OrgUnitService service, JwtTenantIdService jwtTenantIdService) {
        this.service = service;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    private String resolveTenantId(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) return headerValue;
        String fromJwt = jwtTenantIdService.extractTenantIdFromRequest();
        if (fromJwt == null) throw new IllegalStateException("Tenant id could not be resolved");
        return fromJwt;
    }

    @GetMapping("/{identityId}/org-units")
    public ResponseEntity<List<OrgAssignmentDto>> listForIdentity(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID identityId) {
        return ResponseEntity.ok(service.listAssignmentsForIdentity(resolveTenantId(tenantHeader), identityId));
    }
}
