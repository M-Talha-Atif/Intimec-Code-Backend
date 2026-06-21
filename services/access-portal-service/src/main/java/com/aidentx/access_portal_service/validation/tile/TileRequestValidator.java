package com.aidentx.access_portal_service.validation.tile;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.tile.TileRequestDTO;
import com.aidentx.access_portal_service.dto.request.tile.TileAccessScopeItemDTO;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validator for Tile creation and update requests
 */
@Component
public class TileRequestValidator {

    private final IUnifiedLogger logger;

    /**
     * Constructor with logger factory injection
     */
    @Autowired
    public TileRequestValidator(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.TILE_REQUEST_VALIDATOR_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_TILE_VALIDATION
        );
    }

    /**
     * Validates a tile request DTO for create/update operations
     *
     * @param requestDTO The tile request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateTileRequest(TileRequestDTO requestDTO) {
        logger.debug()
                .message("Validating tile request")
                .detail("requestDTO", requestDTO != null ? "present" : "null")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_REQUEST_VALIDATION)
                .log();

        if (requestDTO == null) {
            logger.error()
                    .message("Tile request cannot be null")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_REQUEST_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Tile request cannot be null");
        }

        validateTitle(requestDTO.getTitle());

        validateTileConfiguration(requestDTO);

        logger.debug()
                .message("Tile request validation completed successfully")
                .detail("title", requestDTO.getTitle())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_REQUEST_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates if an access scope item is allowed based on tile configuration
     *
     * @param requestDTO The tile request containing configuration
     * @param item The access scope item to validate
     * @return true if item is allowed, false otherwise
     */
    public boolean isValidAccessScopeItem(TileRequestDTO requestDTO, TileAccessScopeItemDTO item) {
        logger.trace()
                .message("Validating access scope item")
                .detail("itemType", item != null ? item.getItemType() : "null")
                .detail("requestPresent", requestDTO != null ? "true" : "false")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_SCOPE_ITEM_VALIDATION)
                .log();

        if (requestDTO == null || item == null || item.getItemType() == null) {
            logger.debug()
                    .message("Access scope item validation failed - null values")
                    .detail("requestNull", requestDTO == null ? "true" : "false")
                    .detail("itemNull", item == null ? "true" : "false")
                    .detail("itemTypeNull", item != null && item.getItemType() == null ? "true" : "false")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_FAILED)
                    .log();
            return false;
        }

        String itemType = item.getItemType();
        boolean isValid = false;

        if (AccessPortalConstants.ITEM_TYPE_APPLICATION.equals(itemType)) {
            isValid = requestDTO.isAllowApplications();
        } else if (AccessPortalConstants.ITEM_TYPE_BUSINESS_ROLE.equals(itemType)) {
            isValid = requestDTO.isAllowBusinessRoles();
        } else if (AccessPortalConstants.ITEM_TYPE_INTERNAL_ROLE.equals(itemType)) {
            isValid = requestDTO.isAllowInternalRoles();
        } else if (AccessPortalConstants.ITEM_TYPE_ENTITLEMENT.equals(itemType)) {
            isValid = requestDTO.isAllowEntitlements();
        }


        logger.trace()
                .message("Access scope item validation result")
                .detail("itemType", itemType)
                .detail("isValid", Boolean.toString(isValid))
                .detail("allowApplications", Boolean.toString(requestDTO.isAllowApplications()))
                .detail("allowBusinessRoles", Boolean.toString(requestDTO.isAllowBusinessRoles()))
                .detail("allowInternalRoles", Boolean.toString(requestDTO.isAllowInternalRoles()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_RESULT)
                .log();

        return isValid;
    }

    /**
     * Validates tile title
     *
     * @param title The title to validate
     * @throws IllegalArgumentException if title is invalid
     */
    public void validateTitle(String title) {
        logger.trace()
                .message("Validating tile title")
                .detail("title", title != null ? title : "null")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TITLE_VALIDATION)
                .log();

        if (!StringUtils.hasText(title)) {
            logger.error()
                    .message("Tile title is required")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TITLE_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException("Tile title is required");
        }

        if (title.length() > 255) {
            logger.error()
                    .message("Tile title exceeds maximum length")
                    .detail("titleLength", String.valueOf(title.length()))
                    .detail("maxLength", "255")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TITLE_LENGTH_ERROR)
                    .log();
            throw new IllegalArgumentException("Tile title cannot exceed 255 characters");
        }

        logger.trace()
                .message("Tile title validation passed")
                .detail("title", title)
                .detail("titleLength", String.valueOf(title.length()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TITLE_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates tile configuration consistency
     *
     * @param requestDTO The tile request to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateTileConfiguration(TileRequestDTO requestDTO) {
        logger.trace()
                .message("Validating tile configuration")
                .detail("accessScopeItemsCount",
                        requestDTO.getAccessScopeItems() != null ?
                                String.valueOf(requestDTO.getAccessScopeItems().size()) : "0")
                .detail("maximumRiskLevel", requestDTO.getMaximumRiskLevel())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_CONFIGURATION_VALIDATION)
                .log();

        if (requestDTO.isRequireManagerApproval() && requestDTO.getWorkflowId() == null) {
            throw new IllegalArgumentException(
                    "Workflow ID is required when manager approval is enabled"
            );
        }

        // Validate that if access scope items are provided,
        // the corresponding flags must be enabled
        if (requestDTO.getAccessScopeItems() != null && !requestDTO.getAccessScopeItems().isEmpty()) {
            logger.debug()
                    .message("Validating access scope items consistency")
                    .detail("totalItems", String.valueOf(requestDTO.getAccessScopeItems().size()))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_SCOPE_CONSISTENCY_CHECK)
                    .log();

            for (TileAccessScopeItemDTO item : requestDTO.getAccessScopeItems()) {
                String itemType = item.getItemType();

                logger.trace()
                        .message("Validating individual access scope item")
                        .detail("itemType", itemType)
                        .detail("itemCode", item.getItemCode() != null ? item.getItemCode().toString() : "null")
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INDIVIDUAL_ITEM_VALIDATION)
                        .log();

                if ("APPLICATION".equals(itemType) && !requestDTO.isAllowApplications()) {
                    logger.error()
                            .message("Cannot add APPLICATION items when allowApplications is false")
                            .detail("itemType", "APPLICATION")
                            .detail("allowApplications", "false")
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CONFIGURATION_INCONSISTENCY)
                            .log();
                    throw new IllegalArgumentException(
                            "Cannot add APPLICATION items when allowApplications is false"
                    );
                }

                if ("BUSINESS_ROLE".equals(itemType) && !requestDTO.isAllowBusinessRoles()) {
                    logger.error()
                            .message("Cannot add BUSINESS_ROLE items when allowBusinessRoles is false")
                            .detail("itemType", "BUSINESS_ROLE")
                            .detail("allowBusinessRoles", "false")
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CONFIGURATION_INCONSISTENCY)
                            .log();
                    throw new IllegalArgumentException(
                            "Cannot add BUSINESS_ROLE items when allowBusinessRoles is false"
                    );
                }

                if ("INTERNAL_ROLE".equals(itemType) && !requestDTO.isAllowInternalRoles()) {
                    logger.error()
                            .message("Cannot add INTERNAL_ROLE items when allowInternalRoles is false")
                            .detail("itemType", "INTERNAL_ROLE")
                            .detail("allowInternalRoles", "false")
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CONFIGURATION_INCONSISTENCY)
                            .log();
                    throw new IllegalArgumentException(
                            "Cannot add INTERNAL_ROLE items when allowInternalRoles is false"
                    );
                }
            }
        }

        // Validate maximum risk level if provided
        if (requestDTO.getMaximumRiskLevel() != null) {
            validateRiskLevel(requestDTO.getMaximumRiskLevel());
        }

        logger.trace()
                .message("Tile configuration validation completed successfully")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_CONFIGURATION_VALIDATION_SUCCESS)
                .log();
    }

    /**
     * Validates risk level value
     *
     * @param riskLevel The risk level to validate
     * @throws IllegalArgumentException if risk level is invalid
     */
    private void validateRiskLevel(String riskLevel) {
        logger.trace()
                .message("Validating risk level")
                .detail("riskLevel", riskLevel)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RISK_LEVEL_VALIDATION)
                .log();

        if (!StringUtils.hasText(riskLevel)) {
            logger.trace()
                    .message("Risk level is empty, skipping validation")
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RISK_LEVEL_SKIPPED)
                    .log();
            return;
        }

        String[] validRiskLevels = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
        boolean isValid = false;

        for (String validLevel : validRiskLevels) {
            if (validLevel.equalsIgnoreCase(riskLevel)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            logger.error()
                    .message("Invalid risk level provided")
                    .detail("providedRiskLevel", riskLevel)
                    .detail("validRiskLevels", String.join(", ", validRiskLevels))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RISK_LEVEL_VALIDATION_ERROR)
                    .log();
            throw new IllegalArgumentException(
                    "Invalid risk level. Must be one of: " + String.join(", ", validRiskLevels)
            );
        }

        logger.trace()
                .message("Risk level validation passed")
                .detail("riskLevel", riskLevel)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_RISK_LEVEL_VALIDATION_SUCCESS)
                .log();
    }
}