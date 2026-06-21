package com.aidentx.iam.organization_management.controller;

import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgHistoryDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgMoveDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitUpdateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportRequest;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportResponse;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.model.enums.OrgUnitStatus;
import com.aidentx.iam.organization_management.model.enums.OrgUnitType;
import com.aidentx.iam.organization_management.service.OrgUnitService;
import com.aidentx.iam.security.JwtTenantIdService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST surface for the Organization Management module.
 *
 * <p>All endpoints are tenant-scoped. Tenant id is resolved from the JWT
 * (primary) with optional {@code X-Tenant-Id} header fallback.
 * The response shape of {@link OrgUnitDto} is a strict superset of the
 * frontend type {@code OrgNodeData}.
 */
@RestController
@RequestMapping("/api/v1/org-units")
public class OrgUnitController {

    private final OrgUnitService service;
    private final JwtTenantIdService jwtTenantIdService;

    public OrgUnitController(OrgUnitService service, JwtTenantIdService jwtTenantIdService) {
        this.service = service;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    private String resolveTenantId(String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) return headerValue;
        String fromJwt = jwtTenantIdService.extractTenantIdFromRequest();
        if (fromJwt == null) {
            throw new IllegalStateException("Tenant id could not be resolved from JWT or X-Tenant-Id header");
        }
        return fromJwt;
    }

    // ---- CRUD ----

    @GetMapping
    public ResponseEntity<Page<OrgUnitDto>> search(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam(required = false) HierarchyType hierarchyType,
            @RequestParam(required = false) OrgUnitType type,
            @RequestParam(required = false) OrgUnitStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(service.search(resolveTenantId(tenantHeader), hierarchyType, type, status, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrgUnitDto> getById(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(resolveTenantId(tenantHeader), id));
    }

    @PostMapping
    public ResponseEntity<OrgUnitDto> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @Valid @RequestBody OrgUnitCreateDto dto) {
        return ResponseEntity.ok(service.create(resolveTenantId(tenantHeader), dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrgUnitDto> update(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @RequestBody OrgUnitUpdateDto dto) {
        return ResponseEntity.ok(service.update(resolveTenantId(tenantHeader), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {
        service.softDelete(resolveTenantId(tenantHeader), id);
        return ResponseEntity.noContent().build();
    }

    // ---- Bulk import ----

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse> bulkImport(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun,
            @Valid @RequestBody BulkImportRequest body) {
        return ResponseEntity.ok(service.bulkImport(resolveTenantId(tenantHeader), body, dryRun));
    }

    // ---- Tree ----

    @GetMapping("/tree")
    public ResponseEntity<List<OrgUnitDto>> tree(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestParam(required = false) HierarchyType hierarchyType,
            @RequestParam(required = false) UUID rootId) {
        return ResponseEntity.ok(service.getTree(resolveTenantId(tenantHeader), hierarchyType, rootId));
    }

    @GetMapping("/{id}/subtree")
    public ResponseEntity<List<OrgUnitDto>> subtree(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @RequestParam(required = false) Integer maxDepth) {
        return ResponseEntity.ok(service.getSubtree(resolveTenantId(tenantHeader), id, maxDepth));
    }

    @GetMapping("/{id}/ancestors")
    public ResponseEntity<List<OrgUnitDto>> ancestors(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getAncestors(resolveTenantId(tenantHeader), id));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<OrgUnitDto>> children(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getChildren(resolveTenantId(tenantHeader), id));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<OrgUnitDto> move(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @RequestBody OrgMoveDto dto) {
        return ResponseEntity.ok(service.move(resolveTenantId(tenantHeader), id, dto.newParentId()));
    }

    // ---- History ----

    @GetMapping("/{id}/history")
    public ResponseEntity<Page<OrgHistoryDto>> history(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            Pageable pageable) {
        return ResponseEntity.ok(service.getHistory(resolveTenantId(tenantHeader), id, pageable));
    }

    // ---- Delegates ----

    @GetMapping("/{id}/delegates")
    public ResponseEntity<List<OrgDelegateDto>> listDelegates(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.listDelegates(resolveTenantId(tenantHeader), id));
    }

    @PostMapping("/{id}/delegates")
    public ResponseEntity<OrgDelegateDto> addDelegate(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @Valid @RequestBody OrgDelegateCreateDto dto) {
        return ResponseEntity.ok(service.addDelegate(resolveTenantId(tenantHeader), id, dto));
    }

    @DeleteMapping("/{id}/delegates/{delegateId}")
    public ResponseEntity<Void> revokeDelegate(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @PathVariable UUID delegateId) {
        service.revokeDelegate(resolveTenantId(tenantHeader), id, delegateId);
        return ResponseEntity.noContent().build();
    }

    // ---- Identity assignments ----

    @GetMapping("/{id}/identities")
    public ResponseEntity<List<OrgAssignmentDto>> listAssignments(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.listAssignments(resolveTenantId(tenantHeader), id));
    }

    @PostMapping("/{id}/identities")
    public ResponseEntity<OrgAssignmentDto> addAssignment(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @Valid @RequestBody OrgAssignmentCreateDto dto) {
        return ResponseEntity.ok(service.addAssignment(resolveTenantId(tenantHeader), id, dto));
    }

    @DeleteMapping("/{id}/identities/{identityId}")
    public ResponseEntity<Void> removeAssignment(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @PathVariable UUID id,
            @PathVariable UUID identityId,
            @RequestParam(required = false) HierarchyType hierarchyType) {
        service.removeAssignment(resolveTenantId(tenantHeader), id, identityId, hierarchyType);
        return ResponseEntity.noContent().build();
    }
}
