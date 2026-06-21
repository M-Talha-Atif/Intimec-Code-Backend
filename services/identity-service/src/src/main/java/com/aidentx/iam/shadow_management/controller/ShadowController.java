package com.aidentx.iam.shadow_management.controller;

import com.aidentx.iam.security.JwtTenantIdService;
import com.aidentx.iam.shadow_management.model.dto.*;
import com.aidentx.iam.shadow_management.model.entity.ShadowAttributeChangelog;
import com.aidentx.iam.shadow_management.repository.ShadowAttributeChangelogRepository;
import com.aidentx.iam.shadow_management.service.ShadowService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing Shadow objects in the Identity Governance and
 * Reconciliation Engine.
 *
 * <p>
 * This controller provides comprehensive REST API endpoints for shadow
 * management operations
 * including CRUD operations, linking/unlinking to identities, and advanced
 * search capabilities.
 * All operations are tenant-isolated and require proper authentication.
 * </p>
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/shadows")
public class ShadowController {

    private final ShadowService shadowService;
    private final ShadowAttributeChangelogRepository changelogRepository;
    private final JwtTenantIdService jwtTenantIdService;
    private final IUnifiedLogger logger;

    public ShadowController(ShadowService shadowService,
            ShadowAttributeChangelogRepository changelogRepository,
            JwtTenantIdService jwtTenantIdService,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.shadowService = shadowService;
        this.changelogRepository = changelogRepository;
        this.jwtTenantIdService = jwtTenantIdService;
        this.logger = unifiedLoggerFactory.getLogger(ShadowController.class);
    }

    /**
     * GET /api/v1/shadows - List shadows with optional filtering and pagination
     */
    @GetMapping
    public ResponseEntity<ShadowListResponse> listShadows(
            @RequestParam(name = "resourceId", required = false) String resourceUid,
            @RequestParam(required = false) String objectClass,
            @RequestParam(required = false) String situation,
            @RequestParam(required = false) Boolean linked,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String sort) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Listing shadows")
                .detail("tenantId", tenantId)
                .detail("resourceUid", resourceUid)
                .detail("objectClass", objectClass)
                .detail("situation", situation)
                .detail("linked", linked)
                .detail("page", page)
                .detail("size", size)
                .detail("sort", sort)
                .log();

        Page<ShadowResponse> shadowPage = shadowService.listShadows(
                resourceUid, objectClass, situation, linked, page, size, sort, tenantId);

        ShadowListResponse response = new ShadowListResponse();
        response.setShadows(shadowPage.getContent());
        response.setPagination(createPaginationMetadata(shadowPage));

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/shadows - Create a new shadow
     */
    @PostMapping
    public ResponseEntity<ShadowResponse> createShadow(@Valid @RequestBody ShadowCreateRequest createRequest) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Creating shadow")
                .detail("tenantId", tenantId)
                .detail("resourceUid", createRequest.getResourceUid())
                .log();

        ShadowResponse shadow = shadowService.createShadow(createRequest, tenantId);
        return ResponseEntity.status(201).body(shadow);
    }

    /**
     * GET /api/v1/shadows/{shadowId} - Get shadow by ID
     */
    @GetMapping("/{shadowId}")
    public ResponseEntity<ShadowResponse> getShadow(@PathVariable UUID shadowId) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Getting shadow by ID")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        ShadowResponse shadow = shadowService.getShadow(shadowId, tenantId);
        return ResponseEntity.ok(shadow);
    }

    /**
     * PUT /api/v1/shadows/{shadowId} - Update shadow
     */
    @PutMapping("/{shadowId}")
    public ResponseEntity<ShadowResponse> updateShadow(@PathVariable UUID shadowId,
            @Valid @RequestBody ShadowUpdateRequest updateRequest) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Updating shadow")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        ShadowResponse shadow = shadowService.updateShadow(shadowId, updateRequest, tenantId);
        return ResponseEntity.ok(shadow);
    }

    /**
     * DELETE /api/v1/shadows/{shadowId} - Delete shadow
     */
    @DeleteMapping("/{shadowId}")
    public ResponseEntity<Void> deleteShadow(@PathVariable UUID shadowId) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Deleting shadow")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        boolean deleted = shadowService.deleteShadow(shadowId, tenantId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/v1/shadows/{shadowId}/link - Link shadow to identity
     */
    @PostMapping("/{shadowId}/link")
    public ResponseEntity<ShadowResponse> linkShadowToIdentity(
            @PathVariable UUID shadowId,
            @Valid @RequestBody LinkRequest linkRequest) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Linking shadow to identity")
                .detail("shadowId", shadowId)
                .detail("identityId", linkRequest.getIdentityId())
                .detail("tenantId", tenantId)
                .log();

        ShadowResponse shadow = shadowService.linkShadowToIdentity(shadowId, linkRequest.getIdentityId(), tenantId);
        return ResponseEntity.ok(shadow);
    }

    /**
     * DELETE /api/v1/shadows/{shadowId}/link - Unlink shadow from identity
     */
    @DeleteMapping("/{shadowId}/link")
    public ResponseEntity<ShadowResponse> unlinkShadowFromIdentity(@PathVariable UUID shadowId) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        logger.debug().message("Unlinking shadow from identity")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        ShadowResponse shadow = shadowService.unlinkShadowFromIdentity(shadowId, tenantId);
        return ResponseEntity.ok(shadow);
    }

    /**
     * Helper method to create pagination metadata from Page
     */
    private PaginationMetadata createPaginationMetadata(Page<?> page) {
        PaginationMetadata metadata = new PaginationMetadata();
        metadata.setPage(page.getNumber());
        metadata.setSize(page.getSize());
        metadata.setTotalElements(page.getTotalElements());
        metadata.setTotalPages(page.getTotalPages());
        metadata.setHasNext(page.hasNext());
        metadata.setHasPrevious(page.hasPrevious());
        return metadata;
    }

    /**
     * GET /api/v1/shadows/{shadowId}/changelog - Get attribute change history for a shadow
     */
    @GetMapping("/{shadowId}/changelog")
    public ResponseEntity<?> getShadowChangelog(
            @PathVariable UUID shadowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<ShadowAttributeChangelog> changelog = changelogRepository
                .findByShadowIdOrderByChangedAtDesc(shadowId, PageRequest.of(page, size));

        return ResponseEntity.ok(Map.of(
                "data", changelog.getContent(),
                "pagination", Map.of(
                        "page", changelog.getNumber(),
                        "size", changelog.getSize(),
                        "totalElements", changelog.getTotalElements(),
                        "totalPages", changelog.getTotalPages(),
                        "hasNext", changelog.hasNext()
                )
        ));
    }

    /**
     * GET /api/v1/shadows/changelog/sync-run/{syncRunId} - Get all changes from a specific sync run
     */
    @GetMapping("/changelog/sync-run/{syncRunId}")
    public ResponseEntity<?> getSyncRunChangelog(
            @PathVariable String syncRunId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();

        Page<ShadowAttributeChangelog> changelog = changelogRepository
                .findByTenantIdAndSyncRunIdOrderByChangedAtDesc(tenantId, syncRunId, PageRequest.of(page, size));

        return ResponseEntity.ok(Map.of(
                "data", changelog.getContent(),
                "pagination", Map.of(
                        "page", changelog.getNumber(),
                        "size", changelog.getSize(),
                        "totalElements", changelog.getTotalElements(),
                        "totalPages", changelog.getTotalPages(),
                        "hasNext", changelog.hasNext()
                )
        ));
    }
}