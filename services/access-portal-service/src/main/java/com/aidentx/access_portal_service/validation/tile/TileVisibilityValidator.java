package com.aidentx.access_portal_service.validation.tile;

import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.entity.tile.TileAllowedRole;
import com.aidentx.access_portal_service.exception.AccessDeniedException;
import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;

/**
 * Validator for tile visibility and access control
 */
@Component
public class TileVisibilityValidator {

    private final IUnifiedLogger logger;

    /**
     * Constructor with logger factory injection
     */
    @Autowired
    public TileVisibilityValidator(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.TILE_VISIBILITY_VALIDATOR_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_TILE_ACCESS
        );
    }

    /**
     * Comprehensive tile access validation including tenant, active status, and roles
     */
    public void validateTileAccess(Tile tile, String userTenantId, UUID userId, Set<UUID> userRoleIds) {
        logger.debug()
                .message("Validating access for tile")
                .detail("tileId", tile != null ? tile.getId().toString() : "null")
                .detail("userId", userId.toString())
                .detail("userTenantId", userTenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_ACCESS_VALIDATION)
                .log();

        validateTileNotNull(tile);
        validateTenantAccess(tile, userTenantId, userId);
//        validateTileActive(tile);
        validateUserHasRequiredRoles(tile, userRoleIds, userId);

        logger.debug()
                .message("Access validation passed for user to tile")
                .detail("userId", userId.toString())
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_ACCESS_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates tile is not null
     */
    private void validateTileNotNull(Tile tile) {
        if (tile == null) {
            logger.error()
                    .message("Tile cannot be null for access validation")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Tile cannot be null");
        }

        logger.trace()
                .message("Tile not null check passed")
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_VALIDATION)
                .log();
    }

    /**
     * Validates tenant access for a tile
     */
    private void validateTenantAccess(Tile tile, String userTenantId, UUID userId) {
        logger.trace()
                .message("Validating tenant access for tile")
                .detail("tileId", tile.getId().toString())
                .detail("userTenantId", userTenantId)
                .detail("tileTenantId", tile.getTenantId())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TENANT_VALIDATION)
                .log();

        if (userTenantId == null) {
            logger.error()
                    .message("User tenant ID cannot be null for access validation")
                    .detail("userId", userId.toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TENANT_VALIDATION_ERROR)
                    .log();
            throw new SecurityException("Invalid tenant information");
        }

        if (!userTenantId.equals(tile.getTenantId())) {
            logger.warn()
                    .message("Tenant access denied for user trying to access tile from different tenant")
                    .detail("userId", userId.toString())
                    .detail("userTenantId", userTenantId)
                    .detail("tileId", tile.getId().toString())
                    .detail("tileTenantId", tile.getTenantId())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TENANT_ACCESS_DENIED)
                    .log();

            throw new SecurityException(
                    String.format("User %s from tenant %s cannot access tile from tenant %s",
                            userId, userTenantId, tile.getTenantId())
            );
        }

        logger.trace()
                .message("Tenant access check passed")
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TENANT_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates if tile is active
     */
    private void validateTileActive(Tile tile) {
        logger.trace()
                .message("Validating tile active status")
                .detail("tileId", tile.getId().toString())
                .detail("tileActive", Boolean.toString(tile.isTileActive()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_STATUS_VALIDATION)
                .log();

        if (!tile.isTileActive()) {
            logger.warn()
                    .message("Tile is not active")
                    .detail("tileId", tile.getId().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_INACTIVE)
                    .log();
            throw new IllegalArgumentException("Tile is not active: " + tile.getId());
        }

        logger.trace()
                .message("Tile active check passed")
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_STATUS_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates if user has required roles for the tile
     */
    private void validateUserHasRequiredRoles(Tile tile, Set<UUID> userRoleIds, UUID userId) {
        logger.trace()
                .message("Validating roles for tile")
                .detail("tileId", tile.getId().toString())
                .detail("userRoleCount", userRoleIds != null ? String.valueOf(userRoleIds.size()) : "0")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_VALIDATION)
                .log();

        if (tile.getAllowedRoles() == null || tile.getAllowedRoles().isEmpty()) {
            logger.trace()
                    .message("Tile has no roles configured, allowing access to all users")
                    .detail("tileId", tile.getId().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_NO_ROLES)
                    .log();
            return;
        }

        if (userRoleIds == null) {
            logger.error()
                    .message("User role IDs cannot be null for access validation")
                    .detail("userId", userId.toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_VALIDATION_ERROR)
                    .log();
            throw new SecurityException("Invalid user role information");
        }

        // Check if tile has allowed roles configured
//        if (tile.getAllowedRoles() == null || tile.getAllowedRoles().isEmpty()) {
//            logger.warn()
//                    .message("Tile has no allowed roles configured")
//                    .detail("tileId", tile.getId().toString())
//                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_NO_ROLES)
//                    .log();
//            throw new SecurityException("Access denied to tile: " + tile.getId());
//        }

        logger.trace()
                .message("Tile allowed roles count")
                .detail("tileId", tile.getId().toString())
                .detail("allowedRolesCount", String.valueOf(tile.getAllowedRoles().size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_ROLES_INFO)
                .log();

        // Check if user has any of the allowed roles
        boolean hasRequiredRole = tile.getAllowedRoles().stream()
                .map(TileAllowedRole::getRoleCode)
                .anyMatch(userRoleIds::contains);

        if (!hasRequiredRole) {
            logger.warn()
                    .message("User does not have required roles for tile")
                    .detail("userId", userId.toString())
                    .detail("tileId", tile.getId().toString())
                    .detail("userRoleIds", userRoleIds.toString())
                    .detail("allowedRoleIds", tile.getAllowedRoles().stream()
                            .map(TileAllowedRole::getRoleCode)
                            .map(UUID::toString)
                            .toList()
                            .toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_ACCESS_DENIED)
                    .log();

            throw new AccessDeniedException(
                    "You do not have permission to access this tile"
            );
        }

        logger.trace()
                .message("Role validation passed for user to tile")
                .detail("userId", userId.toString())
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Checks if a tile is visible to a user based on their roles
     */
    public boolean isTileVisibleToUser(Tile tile, Set<UUID> userRoleIds) {
        try {
            String tenantId = tile != null ? tile.getTenantId() : null;
            validateTileAccess(tile, tenantId, UUID.randomUUID(), userRoleIds);
            return true;
        } catch (Exception e) {
            logger.debug()
                    .message("Tile not visible to user")
                    .detail("tileId", tile != null ? tile.getId().toString() : "null")
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_VISIBILITY_CHECK)
                    .log();
            return false;
        }
    }

    /**
     * Validates if request for others is allowed for the tile
     */
    public void validateRequestForOthers(Tile tile, boolean isRequestForOthers) {
        logger.trace()
                .message("Validating request for others permission")
                .detail("tileId", tile.getId().toString())
                .detail("isRequestForOthers", Boolean.toString(isRequestForOthers))
                .detail("tileAllowsRequestForOthers", Boolean.toString(tile.isAllowRequestForOthers()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_REQUEST_FOR_OTHERS_VALIDATION)
                .log();

        if (isRequestForOthers && !tile.isAllowRequestForOthers()) {
            logger.warn()
                    .message("Request for others not allowed for tile")
                    .detail("tileId", tile.getId().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_REQUEST_FOR_OTHERS_DENIED)
                    .log();
            throw new IllegalArgumentException("Request for others is not allowed for this tile");
        }

        logger.trace()
                .message("Request for others validation passed")
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_REQUEST_FOR_OTHERS_VALIDATION_SUCCESS)
                .log();
    }
}