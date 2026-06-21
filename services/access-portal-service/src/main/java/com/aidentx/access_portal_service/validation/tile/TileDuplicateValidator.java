package com.aidentx.access_portal_service.validation.tile;


import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.tile.TileRequestDTO;
import com.aidentx.access_portal_service.dto.request.tile.TileAccessScopeItemDTO;
import com.aidentx.access_portal_service.dto.request.tile.TileAllowedRoleDTO;
import com.aidentx.access_portal_service.dto.request.tile.TileVisualFilterDTO;
import com.aidentx.access_portal_service.dto.request.tile.TileVisualFilterRuleDTO;
import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.repository.tile.TileRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates tile uniqueness by comparing composite attributes
 */
@Component
public class TileDuplicateValidator {

    private final TileRepository tileRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public TileDuplicateValidator(
            TileRepository tileRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.tileRepository = tileRepository;
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.TILE_DUPLICATE_VALIDATOR_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_TILE_VALIDATION
        );
    }

    /**
     * Validates that no duplicate tile exists
     * @param tenantId Tenant context
     * @param requestDTO Tile data to check
     * @param excludeTileId Tile ID to exclude (for updates)
     * @throws IllegalArgumentException if duplicate found
     */
    public void validateNoDuplicate(String tenantId, TileRequestDTO requestDTO, UUID excludeTileId) {
        logger.debug()
                .message("Checking for duplicate tiles")
                .detail("tenantId", tenantId)
                .detail("title", requestDTO.getTitle())
                .detail("excludeTileId", excludeTileId != null ? excludeTileId.toString() : "none")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_DUPLICATE_CHECK)
                .log();

        // Find potential duplicates based on title and tenant
        List<Tile> potentialDuplicates = tileRepository.findByTenantIdAndTitle(tenantId, requestDTO.getTitle());

        // Filter out the tile being updated
        if (excludeTileId != null) {
            potentialDuplicates = potentialDuplicates.stream()
                    .filter(tile -> !tile.getId().equals(excludeTileId))
                    .collect(Collectors.toList());
        }

        if (potentialDuplicates.isEmpty()) {
            logger.debug()
                    .message("No potential duplicates found")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_DUPLICATE_CHECK_PASSED)
                    .log();
            return;
        }

        // Check each potential duplicate for exact match
        for (Tile existingTile : potentialDuplicates) {
            if (isDuplicateTile(existingTile, requestDTO)) {
                logger.warn()
                        .message("Duplicate tile detected")
                        .detail("existingTileId", existingTile.getId().toString())
                        .detail("title", requestDTO.getTitle())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_DUPLICATE_FOUND)
                        .log();

                throw new IllegalArgumentException(
                        String.format("A tile with identical configuration already exists (ID: %s). " +
                                "At least one attribute must be different.", existingTile.getId())
                );
            }
        }

        logger.debug()
                .message("No duplicate tiles found")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_DUPLICATE_CHECK_PASSED)
                .log();
    }

    /**
     * Comprehensive duplicate check comparing all meaningful attributes
     */
    private boolean isDuplicateTile(Tile existing, TileRequestDTO request) {
        // Compare basic attributes
        if (!Objects.equals(existing.getTitle(), request.getTitle()) ||
                !Objects.equals(existing.getDescription(), request.getDescription()) ||
                !Objects.equals(existing.getIcon(), request.getIcon()) ||
                !Objects.equals(existing.getCategory(), request.getCategory()) ||
                existing.isTileActive() != request.isTileActive() ||
                existing.isRequireManagerApproval() != request.isRequireManagerApproval() ||
                !Objects.equals(existing.getMaximumRiskLevel(), request.getMaximumRiskLevel()) ||
                existing.isAllowRequestForOthers() != request.isAllowRequestForOthers() ||
                existing.isObjectFilteringEnabled() != request.isObjectFilteringEnabled() ||
                existing.isAllowApplications() != request.isAllowApplications() ||
                existing.isAllowBusinessRoles() != request.isAllowBusinessRoles() ||
                existing.isAllowInternalRoles() != request.isAllowInternalRoles()) {

            return false; // At least one basic attribute differs
        }

        // Compare allowed roles
        if (!areAllowedRolesEqual(existing, request)) {
            return false;
        }

        // Compare access scope items
        if (!areAccessScopeItemsEqual(existing, request)) {
            return false;
        }

        // Compare visual filters
        if (!areVisualFiltersEqual(existing, request)) {
            return false;
        }

        // All attributes match - this is a duplicate
        return true;
    }

    /**
     * Compare allowed roles
     */
    private boolean areAllowedRolesEqual(Tile existing, TileRequestDTO request) {
        Set<UUID> existingRoles = existing.getAllowedRoles().stream()
                .map(role -> role.getRoleCode())
                .collect(Collectors.toSet());

        Set<UUID> requestRoles = request.getAllowedRoles() != null
                ? request.getAllowedRoles().stream()
                .map(TileAllowedRoleDTO::getRoleCode)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return existingRoles.equals(requestRoles);
    }

    /**
     * Compare access scope items
     */
    private boolean areAccessScopeItemsEqual(Tile existing, TileRequestDTO request) {
        Set<AccessScopeItemKey> existingItems = existing.getAccessScopeItems().stream()
                .map(item -> new AccessScopeItemKey(item.getItemType(), item.getItemCode()))
                .collect(Collectors.toSet());

        Set<AccessScopeItemKey> requestItems = request.getAccessScopeItems() != null
                ? request.getAccessScopeItems().stream()
                .map(item -> new AccessScopeItemKey(item.getItemType(), item.getItemCode()))
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return existingItems.equals(requestItems);
    }

    /**
     * Compare visual filters and their rules
     */
    private boolean areVisualFiltersEqual(Tile existing, TileRequestDTO request) {
        Set<VisualFilterKey> existingFilters = existing.getVisualFilters().stream()
                .map(filter -> new VisualFilterKey(
                        filter.getFilterName(),
                        filter.getRules().stream()
                                .map(rule -> new FilterRuleKey(
                                        rule.getFieldName(),
                                        rule.getLogicalOperator(),
                                        rule.getOperator(),
                                        rule.getValue()
                                ))
                                .collect(Collectors.toSet())
                ))
                .collect(Collectors.toSet());

        Set<VisualFilterKey> requestFilters = request.getVisualFilters() != null
                ? request.getVisualFilters().stream()
                .map(filter -> new VisualFilterKey(
                        filter.getFilterName(),
                        filter.getRules() != null
                                ? filter.getRules().stream()
                                .map(rule -> new FilterRuleKey(
                                        rule.getFieldName(),
                                        rule.getLogicalOperator(),
                                        rule.getOperator(),
                                        rule.getValue()
                                ))
                                .collect(Collectors.toSet())
                                : Collections.emptySet()
                ))
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return existingFilters.equals(requestFilters);
    }

    // Helper classes for comparison
    private static class AccessScopeItemKey {
        private final String itemType;
        private final UUID itemCode;

        public AccessScopeItemKey(String itemType, UUID itemCode) {
            this.itemType = itemType;
            this.itemCode = itemCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AccessScopeItemKey that = (AccessScopeItemKey) o;
            return Objects.equals(itemType, that.itemType) &&
                    Objects.equals(itemCode, that.itemCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemType, itemCode);
        }
    }

    private static class FilterRuleKey {
        private final String fieldName;
        private final String logicalOperator;
        private final String operator;
        private final String value;

        public FilterRuleKey(String fieldName, String logicalOperator, String operator, String value) {
            this.fieldName = fieldName;
            this.logicalOperator = logicalOperator;
            this.operator = operator;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterRuleKey that = (FilterRuleKey) o;
            return Objects.equals(fieldName, that.fieldName) &&
                    Objects.equals(logicalOperator, that.logicalOperator) &&
                    Objects.equals(operator, that.operator) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldName, logicalOperator, operator, value);
        }
    }

    private static class VisualFilterKey {
        private final String filterName;
        private final Set<FilterRuleKey> rules;

        public VisualFilterKey(String filterName, Set<FilterRuleKey> rules) {
            this.filterName = filterName;
            this.rules = rules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VisualFilterKey that = (VisualFilterKey) o;
            return Objects.equals(filterName, that.filterName) &&
                    Objects.equals(rules, that.rules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filterName, rules);
        }
    }
}
