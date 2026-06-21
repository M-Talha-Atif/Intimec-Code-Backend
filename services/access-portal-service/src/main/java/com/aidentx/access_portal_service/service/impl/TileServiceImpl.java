package com.aidentx.access_portal_service.service.impl;

import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.tile.*;
import com.aidentx.access_portal_service.dto.response.tile.TileResponseDTO;
import com.aidentx.access_portal_service.entity.tile.*;
import com.aidentx.access_portal_service.repository.tile.*;
import com.aidentx.access_portal_service.service.TileService;
import com.aidentx.access_portal_service.validation.tile.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.aidentx.access_portal_service.mapper.TileMapper;

/**
 * Implementation of TileService using DTOs for request and response.
 */
@Service
@RequiredArgsConstructor
public class TileServiceImpl implements TileService {

    private final TileRepository tileRepository;
    private final UserRoleRestClient userRoleRestClient;
    private final TileMapper tileMapper;
    private final TileRequestValidator tileRequestValidator;
    private final TileVisibilityValidator tileVisibilityValidator;
    private final TileBusinessRulesValidator tileBusinessRulesValidator;
    private final TileExternalReferenceValidator tileExternalReferenceValidator;
    private final TileDuplicateValidator tileDuplicateValidator;
    private final IUnifiedLogger logger;

    /**
     * Constructor with all dependencies including logger factory
     * Note: @RequiredArgsConstructor won't work with UnifiedLoggerFactory,
     * so we need an explicit constructor
     */
    @Autowired
    public TileServiceImpl(
            TileRepository tileRepository,
            UserRoleRestClient userRoleRestClient,
            TileMapper tileMapper,
            TileRequestValidator tileRequestValidator,
            TileVisibilityValidator tileVisibilityValidator,
            TileBusinessRulesValidator tileBusinessRulesValidator,
            TileExternalReferenceValidator tileExternalReferenceValidator,
            TileDuplicateValidator tileDuplicateValidator,
            UnifiedLoggerFactory loggerFactory) {
        this.tileRepository = tileRepository;
        this.userRoleRestClient = userRoleRestClient;
        this.tileMapper = tileMapper;
        this.tileRequestValidator = tileRequestValidator;
        this.tileVisibilityValidator = tileVisibilityValidator;
        this.tileBusinessRulesValidator = tileBusinessRulesValidator;
        this.tileExternalReferenceValidator = tileExternalReferenceValidator;
        this.tileDuplicateValidator = tileDuplicateValidator;
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.TILE_SERVICE_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_TILE_SERVICE
        );
    }


    @Override
    @Transactional
    public TileResponseDTO createTile(String tenantId, UUID createdBy, TileRequestDTO requestDTO) {

        logger.info()
                .message("Creating new tile for tenant")
                .detail("tenantId", tenantId)
                .detail("createdBy", createdBy.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_CREATION_START)
                .log();

        tileRequestValidator.validateTileRequest(requestDTO);

        tileDuplicateValidator.validateNoDuplicate(tenantId, requestDTO, null);


        // validate visual filter rules
        if (requestDTO.getVisualFilters() != null) {
            requestDTO.getVisualFilters().forEach(filterDTO -> {
                if (filterDTO.getRules() != null) {
                    tileBusinessRulesValidator.validateVisualFilterRules(filterDTO.getRules());
                }
            });
        }

        // Validate external roles exist
        if (requestDTO.getAllowedRoles() != null) {
            Set<UUID> roleIds = requestDTO.getAllowedRoles().stream()
                    .map(TileAllowedRoleDTO::getRoleCode)
                    .collect(Collectors.toSet());
            tileExternalReferenceValidator.validateRoles(roleIds, tenantId);
        }
        Tile tile = tileMapper.toEntity(requestDTO);
        tile.setCreatedBy(createdBy);
        tile.setTenantId(tenantId);
        tile.setWorkflowId(requestDTO.getWorkflowId());


        // Add allowed roles using bidirectional relationship helpers
        if (requestDTO.getAllowedRoles() != null) {
            requestDTO.getAllowedRoles().forEach(roleDTO -> {
                tile.addAllowedRole(roleDTO.getRoleCode());
            });
        }

        // access scope items with validation based on tile configuration
        if (requestDTO.getAccessScopeItems() != null) {
            requestDTO.getAccessScopeItems().stream()
                    .filter(item -> tileRequestValidator.isValidAccessScopeItem(requestDTO, item))
                    .forEach(itemDTO -> {
                        tile.addAccessScopeItem(itemDTO.getItemType(), itemDTO.getItemCode());
                    });
        }

        if (requestDTO.getVisualFilters() != null) {
            requestDTO.getVisualFilters().forEach(filterDTO -> {
                // Use the helper method from Tile entity
                TileVisualFilter filter = new TileVisualFilter();
                filter.setTile(tile);
                filter.setFilterName(filterDTO.getFilterName());
                filter.setTenantId(tenantId);

                // Add rules using the new helper method
                if (filterDTO.getRules() != null) {
                    filterDTO.getRules().forEach(ruleDTO -> {
                        tile.addVisualFilterRule(filter, ruleDTO);
                    });
                }

                tile.getVisualFilters().add(filter);
            });
        }

        // Single save operation cascades to all child entities
        tileRepository.save(tile);

        logger.info()
                .message("Tile created successfully")
                .detail("tileId", tile.getId().toString())
                .detail("title", tile.getTitle())
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_CREATION_SUCCESS)
                .log();


        return tileMapper.toResponseForSingleTile(tile);
    }


    @Override
    @Transactional
    public TileResponseDTO updateTile(
            String tenantId,
            UUID tileId,
            UUID modifiedBy,
            TileRequestDTO dto
    ) {
        Tile tile = tileRepository.findByIdWithRelations(tileId)
                .orElseThrow(() -> new IllegalArgumentException("Tile not found: " + tileId));

        // Log current tile state BEFORE update
        logger.info()
                .message("Current tile state before update")
                .detail("tileId", tileId.toString())
                .detail("currentTitle", tile.getTitle())
                .detail("currentObjectFilteringEnabled", Boolean.toString(tile.isObjectFilteringEnabled()))
                .detail("currentTileActive", Boolean.toString(tile.isTileActive()))
                .objectType("tile-current-state")
                .log();

        logger.info()
                .message("Updating tile")
                .detail("tileId", tileId.toString())
                .detail("modifiedBy", modifiedBy.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_UPDATE_START)
                .log();

        logger.info()
                .message("Updating tile - received request DTO")
                .detail("tileId", tileId.toString())
                .detail("modifiedBy", modifiedBy.toString())
                .detail("dtoTitle", dto.getTitle())
                .detail("dtoActive", Boolean.toString(dto.isTileActive()))
                .detail("dtoCategory", dto.getCategory())
                .detail("dtoRiskLevel", dto.getMaximumRiskLevel())
                .detail("dtoAllowApplications", Boolean.toString(dto.isAllowApplications()))
                .detail("dtoAllowBusinessRoles", Boolean.toString(dto.isAllowBusinessRoles()))
                .detail("dtoAllowInternalRoles", Boolean.toString(dto.isAllowInternalRoles()))
                .detail("dtoRequireManagerApproval", Boolean.toString(dto.isRequireManagerApproval()))
                .detail("dtoAllowRequestForOthers", Boolean.toString(dto.isAllowRequestForOthers()))
                .detail("dtoObjectFilteringEnabled", Boolean.toString(dto.isObjectFilteringEnabled()))
                .detail("allowedRolesCount", dto.getAllowedRoles() != null ? String.valueOf(dto.getAllowedRoles().size()) : "0")
                .detail("accessScopeItemsCount", dto.getAccessScopeItems() != null ? String.valueOf(dto.getAccessScopeItems().size()) : "0")
                .detail("visualFiltersCount", dto.getVisualFilters() != null ? String.valueOf(dto.getVisualFilters().size()) : "0")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_UPDATE_START)
                .log();

        // Log visual filters details WITH RULES
        if (dto.getVisualFilters() != null && !dto.getVisualFilters().isEmpty()) {
            dto.getVisualFilters().forEach(filter -> {
                logger.info()
                        .message("Visual filter received")
                        .detail("tileId", tileId.toString())
                        .detail("filterName", filter.getFilterName())
                        .detail("rulesCount", filter.getRules() != null ? String.valueOf(filter.getRules().size()) : "0")
                        .objectType("tile-visual-filter")
                        .log();

                // Log each rule
                if (filter.getRules() != null && !filter.getRules().isEmpty()) {
                    filter.getRules().forEach(rule ->
                            logger.info()
                                    .message("Visual filter rule inside filter")
                                    .detail("tileId", tileId.toString())
                                    .detail("filterName", filter.getFilterName())
                                    .detail("fieldName", rule.getFieldName())
                                    .detail("operator", rule.getOperator())
                                    .detail("value", rule.getValue())
                                    .detail("logicalOperator", rule.getLogicalOperator() != null ? rule.getLogicalOperator() : "null")
                                    .objectType("tile-visual-filter-rule-detail")
                                    .log()
                    );
                } else {
                    logger.warn()
                            .message("Visual filter has NO RULES")
                            .detail("tileId", tileId.toString())
                            .detail("filterName", filter.getFilterName())
                            .objectType("tile-visual-filter-no-rules")
                            .log();
                }
            });
        } else {
            logger.info()
                    .message("No visual filters received")
                    .detail("tileId", tileId.toString())
                    .objectType("tile-visual-filter")
                    .log();
        }
        // Log visual filter rules details
        if (dto.getVisualFilterRules() != null && !dto.getVisualFilterRules().isEmpty()) {
            dto.getVisualFilterRules().forEach(rule ->
                    logger.info()
                            .message("Visual filter rule received")
                            .detail("tileId", tileId.toString())
                            .detail("fieldName", rule.getFieldName())
                            .detail("operator", rule.getOperator())
                            .detail("value", rule.getValue())
                            .objectType("tile-visual-filter-rule")
                            .log()
            );
        } else {
            logger.info()
                    .message("No visual filter rules received")
                    .detail("tileId", tileId.toString())
                    .objectType("tile-visual-filter-rule")
                    .log();
        }
        // CHECK FOR DUPLICATES (exclude current tile from check)
        tileDuplicateValidator.validateNoDuplicate(tenantId, dto, tileId);


        // Update scalar fields
        tile.setTitle(dto.getTitle());
        tile.setDescription(dto.getDescription());
        tile.setCategory(dto.getCategory());
        tile.setIcon(dto.getIcon());
        tile.setTileActive(dto.isTileActive());
        // manager approval
        tile.setRequireManagerApproval(dto.isRequireManagerApproval());
        tile.setWorkflowId(dto.getWorkflowId());
        // meta data
        tile.setMaximumRiskLevel(dto.getMaximumRiskLevel());
        tile.setAllowRequestForOthers(dto.isAllowRequestForOthers());
        tile.setObjectFilteringEnabled(dto.isObjectFilteringEnabled());
        tile.setAllowApplications(dto.isAllowApplications());
        tile.setAllowBusinessRoles(dto.isAllowBusinessRoles());
        tile.setAllowInternalRoles(dto.isAllowInternalRoles());
        tile.setAllowEntitlements(dto.isAllowEntitlements());

        // Log what's being set
        logger.debug()
                .message("Setting objectFilteringEnabled on tile entity")
                .detail("tileId", tileId.toString())
                .detail("objectFilteringEnabled", Boolean.toString(dto.isObjectFilteringEnabled()))
                .objectType("tile-field-setting")
                .log();

        tile.setLastModifiedBy(modifiedBy);
        tile.setUpdatedAt(OffsetDateTime.now());

        /* -------- Allowed Roles -------- */
        tile.getAllowedRoles().clear();
        if (dto.getAllowedRoles() != null) {
            dto.getAllowedRoles().forEach(roleDTO ->
                    tile.addAllowedRole(roleDTO.getRoleCode())
            );
        }

        /* -------- Access Scope Items -------- */
        tile.getAccessScopeItems().clear();
        if (dto.getAccessScopeItems() != null) {
            dto.getAccessScopeItems().stream()
                    .filter(item -> tileRequestValidator.isValidAccessScopeItem(dto, item))
                    .forEach(itemDTO ->
                            tile.addAccessScopeItem(
                                    itemDTO.getItemType(),
                                    itemDTO.getItemCode()
                            )
                    );
        }

        /* -------- Visual Filters -------- */
        tile.getVisualFilters().clear();
        if (dto.getVisualFilters() != null) {
            dto.getVisualFilters().forEach(filterDTO -> {

                TileVisualFilter filter = new TileVisualFilter();
                filter.setTile(tile);
                filter.setFilterName(filterDTO.getFilterName());
                filter.setTenantId(tile.getTenantId());

                if (filterDTO.getRules() != null) {
                    filterDTO.getRules().forEach(ruleDTO ->
                            tile.addVisualFilterRule(filter, ruleDTO)
                    );
                }

                tile.getVisualFilters().add(filter);
            });
        }

        tileRepository.save(tile);

        // Force refresh from database
        tileRepository.flush();
        Tile tileUpdated = tileRepository.findById(tileId).orElseThrow();

        // Log state AFTER save
        logger.info()
                .message("Tile state after save (before commit)")
                .detail("tileId", tileId.toString())
                .detail("objectFilteringEnabled", Boolean.toString(tileUpdated.isObjectFilteringEnabled()))
                .objectType("tile-after-save-state")
                .log();

        logger.info()
                .message("Tile updated successfully")
                .detail("tileId", tileId.toString())
                .detail("title", dto.getTitle())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_UPDATE_SUCCESS)
                .log();

        return tileMapper.toResponseForSingleTile(tile);
    }


    @Override
    @Transactional
    public void deleteTile(UUID tileId) {

        logger.info()
                .message("Deleting tile")
                .detail("tileId", tileId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_DELETION_START)
                .log();


        if (!tileRepository.existsById(tileId)) {

            logger.warn()
                    .message("Tile not found for deletion")
                    .detail("tileId", tileId.toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_NOT_FOUND)
                    .log();

            throw new IllegalArgumentException("Tile not found: " + tileId);
        }

        logger.info()
                .message("Tile deleted successfully")
                .detail("tileId", tileId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_DELETION_SUCCESS)
                .log();


        tileRepository.deleteById(tileId);
    }


    @Override
    @Transactional(readOnly = true)
    public TileResponseDTO getTileById(UUID tileId, UUID userId, String tenantId) {

        logger.info()
                .message("Fetching tile by ID")
                .detail("tileId", tileId.toString())
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_FETCH_START)
                .log();

        // Fetch basic tile information
        Tile tile = tileRepository.findById(tileId)
                .orElseThrow(() -> {
                    logger.warn()
                            .message("Tile not found with ID")
                            .detail("tileId", tileId.toString())
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_NOT_FOUND)
                            .log();
                    return new IllegalArgumentException("Tile not found: " + tileId);
                });

        logger.debug()
                .message("Found basic tile")
                .detail("tileId", tile.getId().toString())
                .detail("title", tile.getTitle())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_BASIC_FOUND)
                .log();


        // Fetch user's active roles from external service
        Set<UUID> activeUserRoleIds = userRoleRestClient.getActiveRoleIds(userId, tenantId);
        logger.debug()
                .message("User active roles count")
                .detail("userId", userId.toString())
                .detail("roleCount", String.valueOf(activeUserRoleIds.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ROLES_FETCHED)
                .log();

        // Fetch tile's allowed roles (needed for validation)
        Set<TileAllowedRole> allowedRoles = tileRepository.findAllowedRolesByTileId(tileId);
        tile.setAllowedRoles(allowedRoles);
        logger.debug()
                .message("Tile allowed roles count")
                .detail("tileId", tileId.toString())
                .detail("allowedRoleCount", String.valueOf(allowedRoles.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_ROLES_FETCHED)
                .log();


        // Single comprehensive validation call
        tileVisibilityValidator.validateTileAccess(tile, tenantId, userId, activeUserRoleIds);

        // Fetch complete tile with all relationships (access granted)
        Tile tileWithRelations = fetchCompleteTileWithRelations(tileId);

        // Map to DTO
        TileResponseDTO response = tileMapper.toResponseForSingleTile(tileWithRelations);

        logger.info()
                .message("Successfully fetched tile for user")
                .detail("tileId", tileId.toString())
                .detail("userId", userId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_FETCH_SUCCESS)
                .log();

        return response;
    }

    /**
     * Fetches complete tile with all relationships (only called after access validation)
     */
    private Tile fetchCompleteTileWithRelations(UUID tileId) {

        logger.debug()
                .message("Fetching complete tile with relationships")
                .detail("tileId", tileId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_RELATIONS_FETCH)
                .log();

        Tile tile = tileRepository.findByIdWithAllRelations(tileId);

        logger.debug()
                .message("Fetched complete tile with relationships")
                .detail("tileId", tileId.toString())
                .detail("allowedRolesCount", String.valueOf(tile.getAllowedRoles().size()))
                .detail("accessScopeItemsCount", String.valueOf(tile.getAccessScopeItems().size()))
                .detail("visualFiltersCount", String.valueOf(tile.getVisualFilters().size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_RELATIONS_FETCHED)
                .log();

        return tile;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TileResponseDTO> getAllTilesForTenant(
            String tenantId,
            UUID userId,
            Pageable pageable) {

        logger.info()
                .message("Fetching tiles for tenant")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILES_FETCH_START)
                .log();

        // Fetch active roleIds for user from external service
        Set<UUID> activeUserRoleIds = userRoleRestClient.getActiveRoleIds(userId, tenantId);

        logger.info()
                .message("Active roleIds for user")
                .detail("userId", userId.toString())
                .detail("roleCount", String.valueOf(activeUserRoleIds.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ACTIVE_ROLES)
                .log();

        // Fetch ALL tiles active and non active both
        List<Tile> allTiles = tileRepository.findAllByTenantId(tenantId);

        logger.info()
                .message("Total tiles fetched for tenant")
                .detail("tenantId", tenantId)
                .detail("totalTiles", String.valueOf(allTiles.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILES_FETCHED)
                .log();

        // Filter tiles based on user's roles and tile visibility
        List<TileResponseDTO> visibleTiles = allTiles.stream()
                .filter(tile -> tileVisibilityValidator.isTileVisibleToUser(tile, activeUserRoleIds))
                .map(tileMapper::toResponseForListOfTiles)
                .toList();

        logger.info()
                .message("Tiles visible to user")
                .detail("userId", userId.toString())
                .detail("visibleTilesCount", String.valueOf(visibleTiles.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILES_VISIBLE)
                .log();

        // Apply pagination to filtered results
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), visibleTiles.size());
        List<TileResponseDTO> pagedTiles = visibleTiles.subList(start, end);

        // Return with correct total count
        return new PageImpl<>(
                pagedTiles,
                pageable,
                visibleTiles.size() // Total visible tiles, not just current page
        );
    }
}
