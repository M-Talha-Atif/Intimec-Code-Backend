package com.aidentx.access_portal_service.validation.tile;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.client.BusinessRoleRestClient;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.UUID;

/**
 * Validator for external service references (roles, applications, etc.)
 */
@Component
public class TileExternalReferenceValidator {

    private final UserRoleRestClient userRoleRestClient;
    private final BusinessRoleRestClient businessRoleRestClient;
    private final IUnifiedLogger logger;

    /**
     * Constructor with logger factory injection
     */
    @Autowired
    public TileExternalReferenceValidator(
            UserRoleRestClient userRoleRestClient,
            BusinessRoleRestClient businessRoleRestClient,
            UnifiedLoggerFactory loggerFactory) {
        this.userRoleRestClient = userRoleRestClient;
        this.businessRoleRestClient = businessRoleRestClient;
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.TILE_EXTERNAL_REF_VALIDATOR_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_EXTERNAL_VALIDATION
        );
    }

    /**
     * Validates if a role exists in the external role service
     *
     * @param roleId The role ID to validate
     * @param tenantId The tenant ID for context
     * @return true if role exists, false otherwise
     */
    public boolean validateRoleExists(UUID roleId, String tenantId) {
        logger.trace()
                .message("Validating role existence")
                .detail("roleId", roleId != null ? roleId.toString() : "null")
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_VALIDATION)
                .log();

        if (roleId == null || tenantId == null) {
            logger.debug()
                    .message("Role validation failed - null parameters")
                    .detail("roleIdNull", roleId == null ? "true" : "false")
                    .detail("tenantIdNull", tenantId == null ? "true" : "false")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_FAILED)
                    .log();
            return false;
        }

        try {
            logger.debug()
                    .message("Calling external role service for validation")
                    .detail("roleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                    .log();

            // Call external role service to validate role existence
            // boolean roleExists = userRoleRestClient.validateRole(roleId, tenantId);
            boolean roleExists = true; // Placeholder for actual implementation

            if (roleExists) {
                logger.trace()
                        .message("Role validation successful")
                        .detail("roleId", roleId.toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_VALIDATION_SUCCESS)
                        .log();
            } else {
                logger.warn()
                        .message("Role not found in external service")
                        .detail("roleId", roleId.toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_NOT_FOUND)
                        .log();
            }

            return roleExists;

        } catch (Exception e) {
            logger.error(e)
                    .message("External role service validation failed")
                    .detail("roleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_ERROR)
                    .log();
            return false;
        }
    }

    /**
     * Validates a set of role IDs against external service
     *
     * @param roleIds The set of role IDs to validate
     * @param tenantId The tenant ID for context
     * @throws IllegalArgumentException if any role is invalid
     */
    public void validateRoles(Set<UUID> roleIds, String tenantId) {
        logger.debug()
                .message("Validating multiple roles")
                .detail("roleCount", roleIds != null ? String.valueOf(roleIds.size()) : "0")
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION)
                .log();

        if (roleIds == null || roleIds.isEmpty()) {
            logger.trace()
                    .message("No roles to validate")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION_SUCCESS)
                    .log();
            return;
        }

        int validatedCount = 0;
        int failedCount = 0;

        for (UUID roleId : roleIds) {
            if (!validateRoleExists(roleId, tenantId)) {
                failedCount++;
                logger.error()
                        .message("Invalid role ID found during bulk validation")
                        .detail("roleId", roleId.toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION_ERROR)
                        .log();

                throw new IllegalArgumentException(
                        AccessPortalConstants.ERROR_INVALID_ROLE + roleId + " for tenant: " + tenantId
                );
            }
            validatedCount++;
        }

        logger.info()
                .message("Bulk role validation completed")
                .detail("totalRoles", String.valueOf(roleIds.size()))
                .detail("validatedCount", String.valueOf(validatedCount))
                .detail("failedCount", String.valueOf(failedCount))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Gets active role IDs for a user from external service
     *
     * @param userId The user ID
     * @param tenantId The tenant ID
     * @return Set of active role IDs for the user
     */
    public Set<UUID> getActiveUserRoleIds(UUID userId, String tenantId) {
        logger.debug()
                .message("Fetching active role IDs for user")
                .detail("userId", userId != null ? userId.toString() : "null")
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ROLES_FETCH)
                .log();

        if (userId == null || tenantId == null) {
            logger.warn()
                    .message("User ID or tenant ID is null, returning empty set")
                    .detail("userIdNull", userId == null ? "true" : "false")
                    .detail("tenantIdNull", tenantId == null ? "true" : "false")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_FAILED)
                    .log();
            return Set.of();
        }

        try {
            logger.trace()
                    .message("Calling external user role service")
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                    .log();

            Set<UUID> activeRoleIds = userRoleRestClient.getActiveRoleIds(userId, tenantId);

            logger.info()
                    .message("Successfully fetched active user roles")
                    .detail("userId", userId.toString())
                    .detail("roleCount", String.valueOf(activeRoleIds.size()))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ROLES_FETCH_SUCCESS)
                    .log();

            return activeRoleIds;

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to fetch active user roles from external service")
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ROLES_FETCH_ERROR)
                    .log();
            return Set.of();
        }
    }

    /**
     * Validates if an external item (application, business role, internal role) exists
     *
     * @param itemType The type of item (APPLICATION, BUSINESS_ROLE, INTERNAL_ROLE)
     * @param itemCode The item code/UUID
     * @param tenantId The tenant ID
     * @return true if item exists, false otherwise
     */
    public boolean validateExternalItemExists(String itemType, UUID itemCode, String tenantId) {
        logger.trace()
                .message("Validating external item existence")
                .detail("itemType", itemType)
                .detail("itemCode", itemCode != null ? itemCode.toString() : "null")
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_ITEM_VALIDATION)
                .log();

        if (itemType == null || itemCode == null || tenantId == null) {
            logger.debug()
                    .message("External item validation failed - null parameters")
                    .detail("itemTypeNull", itemType == null ? "true" : "false")
                    .detail("itemCodeNull", itemCode == null ? "true" : "false")
                    .detail("tenantIdNull", tenantId == null ? "true" : "false")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_FAILED)
                    .log();
            return false;
        }

        try {
            boolean itemExists = false;

            switch (itemType) {
                case AccessPortalConstants.ITEM_TYPE_APPLICATION:
                    logger.debug()
                            .message("Validating application existence")
                            .detail("applicationId", itemCode.toString())
                            .detail("tenantId", tenantId)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                            .log();

                    // Call application service
                    // itemExists = applicationClient.validateApplicationExists(itemCode, tenantId);
                    itemExists = true; // Placeholder
                    break;

                case AccessPortalConstants.ITEM_TYPE_BUSINESS_ROLE:
                    logger.debug()
                            .message("Validating business role existence")
                            .detail("businessRoleId", itemCode.toString())
                            .detail("tenantId", tenantId)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH)
                            .log();

                    itemExists = businessRoleRestClient.validateBusinessRoleExists(itemCode, tenantId);
                    break;

                case AccessPortalConstants.ITEM_TYPE_INTERNAL_ROLE:
                    logger.debug()
                            .message("Validating internal role existence")
                            .detail("internalRoleId", itemCode.toString())
                            .detail("tenantId", tenantId)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                            .log();

                    // Call internal role service
                    // itemExists = internalRoleClient.validateInternalRoleExists(itemCode, tenantId);
                    itemExists = true; // Placeholder
                    break;

                default:
                    logger.warn()
                            .message("Unknown item type for validation")
                            .detail("itemType", itemType)
                            .detail("itemCode", itemCode.toString())
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_ITEM_VALIDATION_ERROR)
                            .log();
                    return false;
            }

            if (itemExists) {
                logger.trace()
                        .message("External item validation successful")
                        .detail("itemType", itemType)
                        .detail("itemCode", itemCode.toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_ITEM_VALIDATION_SUCCESS)
                        .log();
            } else {
                logger.warn()
                        .message("External item not found")
                        .detail("itemType", itemType)
                        .detail("itemCode", itemCode.toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_ITEM_NOT_FOUND)
                        .log();
            }

            return itemExists;

        } catch (Exception e) {
            logger.error(e)
                    .message("External item validation service call failed")
                    .detail("itemType", itemType)
                    .detail("itemCode", itemCode.toString())
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_ERROR)
                    .log();
            return false;
        }
    }

    /**
     * Validates external items in bulk
     *
     * @param itemType The type of items
     * @param itemCodes The set of item codes to validate
     * @param tenantId The tenant ID
     * @throws IllegalArgumentException if any item is invalid
     */
    public void validateExternalItems(String itemType, Set<UUID> itemCodes, String tenantId) {
        logger.debug()
                .message("Validating external items in bulk")
                .detail("itemType", itemType)
                .detail("itemCount", itemCodes != null ? String.valueOf(itemCodes.size()) : "0")
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION)
                .log();

        if (itemCodes == null || itemCodes.isEmpty()) {
            logger.trace()
                    .message("No external items to validate")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION_SUCCESS)
                    .log();
            return;
        }

        int validatedCount = 0;
        int failedCount = 0;

        for (UUID itemCode : itemCodes) {
            if (!validateExternalItemExists(itemType, itemCode, tenantId)) {
                failedCount++;
                logger.error()
                        .message("Invalid external item found during bulk validation")
                        .detail("itemType", itemType)
                        .detail("itemCode", itemCode.toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION_ERROR)
                        .log();

                throw new IllegalArgumentException(
                        AccessPortalConstants.ERROR_INVALID_EXTERNAL_ITEM +
                                itemType + " ID: " + itemCode + " for tenant: " + tenantId
                );
            }
            validatedCount++;
        }

        logger.info()
                .message("Bulk external item validation completed")
                .detail("itemType", itemType)
                .detail("totalItems", String.valueOf(itemCodes.size()))
                .detail("validatedCount", String.valueOf(validatedCount))
                .detail("failedCount", String.valueOf(failedCount))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BULK_VALIDATION_SUCCESS)
                .log();
    }
}