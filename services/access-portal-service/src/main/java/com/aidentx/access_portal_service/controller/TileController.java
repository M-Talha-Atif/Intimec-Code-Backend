package com.aidentx.access_portal_service.controller;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.tile.TileRequestDTO;
import com.aidentx.access_portal_service.dto.response.ApiResponse;
import com.aidentx.access_portal_service.dto.response.tile.TileAccessScopeResponseDTO;
import com.aidentx.access_portal_service.dto.response.tile.TileDeletionImpactDTO;
import com.aidentx.access_portal_service.dto.response.tile.TileResponseDTO;
import com.aidentx.access_portal_service.service.TileAccessScopeService;
import com.aidentx.access_portal_service.service.TileService;
import com.aidentx.access_portal_service.service.impl.TileDeletionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_TENANT_ID;
import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_USER_ID;
import static com.aidentx.access_portal_service.util.RequestContextUtil.resolveUserId;
import static com.aidentx.access_portal_service.config.AccessPortalConstants.*;

@RestController
@RequestMapping(API_BASE_TILES)
@RequiredArgsConstructor
@Tag(name = TAG_TILE_API, description = TAG_TILE_API_DESC)
public class TileController {

    private final TileService tileService;
    private final TileAccessScopeService tileAccessScopeService;
    private final TileDeletionServiceImpl tileDeletionService;

    /**
     * Create a new tile.
     */
    @PostMapping
    @Operation(summary = OP_CREATE_TILE, description = OP_CREATE_TILE_DESC)
    public ResponseEntity<ApiResponse<TileResponseDTO>> createTile(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @Valid @RequestBody TileRequestDTO requestDTO
    ) {
        UUID createdBy = resolveUserId(userIdHeader);

        TileResponseDTO createdTile =
                tileService.createTile(tenantIdHeader, createdBy, requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(AccessPortalConstants.SUCCESS_TILE_CREATED, createdTile));
    }

    /**
     * Update an existing tile.
     */
    @PutMapping(API_TILE_BY_ID)
    @Operation(summary = OP_UPDATE_TILE, description = OP_UPDATE_TILE_DESC)
    public ResponseEntity<ApiResponse<TileResponseDTO>> updateTile(
            @PathVariable UUID tileId,
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = AccessPortalConstants.HEADER_USER_ID, required = false) String userIdHeader,
            @Valid @RequestBody TileRequestDTO requestDTO
    ) {
        UUID modifiedBy = resolveUserId(userIdHeader);

        TileResponseDTO updatedTile =
                tileService.updateTile(tenantIdHeader, tileId, modifiedBy, requestDTO);

        return ResponseEntity.ok(
                ApiResponse.success(AccessPortalConstants.SUCCESS_TILE_UPDATED, updatedTile)
        );
    }


    /**
     * Get tile by ID.
     */
    @GetMapping(API_TILE_BY_ID)
    @Operation(summary = OP_GET_TILE, description = OP_GET_TILE_DESC)
    public ResponseEntity<ApiResponse<TileResponseDTO>> getTile(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @PathVariable UUID tileId) {

        TileResponseDTO tile = tileService.getTileById(tileId, resolveUserId(userIdHeader), tenantIdHeader);
        return ResponseEntity.ok(ApiResponse.success(tile));
    }

    /**
     * List all tiles for a tenant.
     */
    @GetMapping
    @Operation(summary = OP_LIST_TILES, description = OP_LIST_TILES_DESC)
    public ResponseEntity<ApiResponse<Page<TileResponseDTO>>> listTiles(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<TileResponseDTO> tiles =
                tileService.getAllTilesForTenant(tenantIdHeader, resolveUserId(userIdHeader), pageable);

        return ResponseEntity.ok(ApiResponse.success(tiles));
    }

    // List all access scope items for a tile

    @GetMapping(API_TILE_ACCESS_SCOPE)
    @Operation(
            summary = OP_GET_TILE_ACCESS_SCOPE,
            description = OP_GET_TILE_ACCESS_SCOPE_DESC
    )
    public ResponseEntity<ApiResponse<TileAccessScopeResponseDTO>> getTileAccessScope(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @PathVariable UUID tileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        TileAccessScopeResponseDTO response =
                tileAccessScopeService.getAccessScopeItemsForTile(tileId, resolveUserId(userIdHeader), tenantIdHeader, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get deletion impact analysis for a tile.
     */
    @GetMapping("/{tileId}/deletion-impact")
    @Operation(
            summary = "Analyze Tile Deletion Impact",
            description = "Analyze the impact of deleting a tile on cart items and access requests"
    )
    public ResponseEntity<ApiResponse<TileDeletionImpactDTO>> getTileDeletionImpact(
            @PathVariable UUID tileId
    ) {
        TileDeletionImpactDTO impact = tileDeletionService.analyzeDeletionImpact(tileId);

        return ResponseEntity.ok(
                ApiResponse.<TileDeletionImpactDTO>builder()
                        .success(true)
                        .data(impact)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * Delete a tile with validation.
     */
    @DeleteMapping(API_TILE_BY_ID)
    @Operation(summary = OP_DELETE_TILE, description = OP_DELETE_TILE_DESC)
    public ResponseEntity<ApiResponse<Void>> deleteTile(@PathVariable UUID tileId) {
        // Check impact first
        TileDeletionImpactDTO impact = tileDeletionService.analyzeDeletionImpact(tileId);

        if (!impact.isCanDelete()) {
            throw new IllegalStateException(impact.getDeleteBlockReason());
        }

        // Proceed with deletion
        tileService.deleteTile(tileId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message(AccessPortalConstants.SUCCESS_TILE_DELETED)
                         .timestamp(LocalDateTime.now())
                        .build()
        );
    }

}
