package com.aidentx.access_portal_service.validation.cart;

import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.dto.request.cart.AddToCartRequestDTO;
import com.aidentx.access_portal_service.entity.cart.CartStatus;
import com.aidentx.access_portal_service.exception.DuplicateBeneficiaryException;
import com.aidentx.access_portal_service.repository.cart.CartRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validator for cart-related requests
 */
@Component
public class CartRequestValidator {


    private final CartRepository cartRepository;

    private final UserRoleRestClient userRoleRestClient;

    public CartRequestValidator(CartRepository cartRepository, UserRoleRestClient userRoleRestClient) {
        this.cartRepository = cartRepository;
        this.userRoleRestClient = userRoleRestClient;
    }

    /**
     * Validates add to cart request
     */
    public void validateAddToCartRequest(AddToCartRequestDTO request, String tenantId, UUID userId) {
        if (request == null) {
            throw new IllegalArgumentException("Add to cart request cannot be null");
        }

        validateTileId(request.getTileId());
        validateItemId(request.getItemId());
        validateItemType(request.getItemType());
        validateItemName(request.getItemName());
        validateRequestType(request.getRequestType());
        validateBeneficiaries(request.getRequestType(), request.getBeneficiaryUserIds(), request.getBeneficiaryNames()); // CHANGED
        validateTenantAndUser(tenantId, userId);

        // Effective duplicate check - tenant + item + beneficiary + status
        validateNoDuplicateAssignments(request, tenantId, userId);
    }


    /**
     * Validates no duplicate assignments based on effective duplicate definition:
     * - Same tenantId
     * - Same itemId (role)
     * - Same beneficiaryUserId
     * - Status: ACTIVE / PENDING / APPROVAL_IN_PROGRESS
     * <p>
     * Also checks if users already have the role assigned via role service.
     * <p>
     * IMPORTANT: Requester does NOT matter.
     * If User A requested Role X for User C, User B must NOT be able to request Role X for User C.
     */
    private void validateNoDuplicateAssignments(AddToCartRequestDTO request, String tenantId, UUID userId) {

        // Check 1: Verify if users already have the role assigned via role service
        if ("INTERNAL_ROLE".equalsIgnoreCase(request.getItemType())) {
            List<UUID> beneficiariesToCheck = new ArrayList<>();

            if ("SELF".equalsIgnoreCase(request.getRequestType())) {
                beneficiariesToCheck.add(userId);
            } else if ("OTHERS".equalsIgnoreCase(request.getRequestType()) &&
                    request.getBeneficiaryUserIds() != null) {
                beneficiariesToCheck.addAll(request.getBeneficiaryUserIds());
            }

            if (!beneficiariesToCheck.isEmpty()) {
                Map<UUID, Boolean> roleAssignments = userRoleRestClient.checkUsersHaveRole(
                        request.getItemId(),
                        beneficiariesToCheck
                );

                List<String> alreadyAssignedNames = new ArrayList<>();
                List<String> alreadyAssignedIds = new ArrayList<>();

                for (int i = 0; i < beneficiariesToCheck.size(); i++) {
                    UUID beneficiaryId = beneficiariesToCheck.get(i);
                    if (Boolean.TRUE.equals(roleAssignments.get(beneficiaryId))) {
                        if ("SELF".equalsIgnoreCase(request.getRequestType())) {
                            throw new IllegalArgumentException(
                                    "You already have this role assigned"
                            );
                        } else {
                            alreadyAssignedNames.add(request.getBeneficiaryNames().get(i));
                            alreadyAssignedIds.add(beneficiaryId.toString());
                        }
                    }
                }

                if (!alreadyAssignedNames.isEmpty()) {
                    String message = String.format(
                            "The following users already have this role assigned: %s",
                            String.join(", ", alreadyAssignedNames)
                    );
                    throw new DuplicateBeneficiaryException(
                            message,
                            alreadyAssignedNames,
                            alreadyAssignedIds
                    );
                }
            }
        }

        // Check 2: Additional check for SELF requests
        if ("SELF".equalsIgnoreCase(request.getRequestType())) {
            // Check if ANYONE has already requested this role for this user
            boolean alreadyRequested = cartRepository
                    .existsByBeneficiaryUserIdContainingAndItemIdAndTenantIdAndStatusIn(
                            userId.toString(),
                            request.getItemId(),
                            tenantId,
                            List.of(CartStatus.ACTIVE, CartStatus.PENDING)
                    );

            if (alreadyRequested) {
                throw new IllegalArgumentException(
                        "This role has already been requested for you by another user"
                );
            }

            // Check if this user has already requested this item for themselves
            boolean alreadySelfRequested = cartRepository
                    .existsByUserIdAndItemIdAndStatusInAndRequestType(
                            userId,
                            request.getItemId(),
                            List.of(CartStatus.ACTIVE, CartStatus.PENDING),
                            request.getRequestType()
                    );

            if (alreadySelfRequested) {
                throw new IllegalArgumentException(
                        "You have already requested this role for yourself"
                );
            }
        }

        // Check 3: For others requests
        if ("OTHERS".equalsIgnoreCase(request.getRequestType())) {
            List<UUID> beneficiaryUserIds = request.getBeneficiaryUserIds();
            List<String> beneficiaryNames = request.getBeneficiaryNames();

            if (beneficiaryUserIds == null || beneficiaryUserIds.isEmpty()) {
                return;
            }

            List<String> conflictingNames = new ArrayList<>();
            List<String> conflictingIds = new ArrayList<>();

            // Check each beneficiary
            for (int i = 0; i < beneficiaryUserIds.size(); i++) {
                UUID beneficiaryId = beneficiaryUserIds.get(i);

                // Find if this role+beneficiary combination exists in ACTIVE/PENDING carts
                boolean alreadyRequested = cartRepository
                        .existsByBeneficiaryUserIdContainingAndItemIdAndTenantIdAndStatusIn(
                                beneficiaryId.toString(),
                                request.getItemId(),
                                tenantId,
                                List.of(CartStatus.ACTIVE, CartStatus.PENDING)
                        );

                if (alreadyRequested) {
                    conflictingNames.add(beneficiaryNames.get(i));
                    conflictingIds.add(beneficiaryId.toString());
                }
            }

            if (!conflictingNames.isEmpty()) {
                String message = String.format(
                        "The following users already have a pending request for this role: %s",
                        String.join(", ", conflictingNames)
                );
                throw new DuplicateBeneficiaryException(message, conflictingNames, conflictingIds);
            }
        }
    }

    private void validateTileId(UUID tileId) {
        if (tileId == null) {
            throw new IllegalArgumentException("Tile ID is required");
        }
    }

    private void validateItemId(UUID itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("Item ID is required");
        }
    }

    private void validateItemType(String itemType) {
        if (!StringUtils.hasText(itemType)) {
            throw new IllegalArgumentException("Item type is required");
        }

        String[] allowedTypes = {"APPLICATION", "BUSINESS_ROLE", "INTERNAL_ROLE", "ENTITLEMENT"};
        boolean isValid = false;

        for (String allowedType : allowedTypes) {
            if (allowedType.equalsIgnoreCase(itemType)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException(
                    "Invalid item type. Must be one of: APPLICATION, BUSINESS_ROLE, INTERNAL_ROLE, ENTITLEMENT"
            );
        }
    }

    private void validateItemName(String itemName) {
        if (!StringUtils.hasText(itemName)) {
            throw new IllegalArgumentException("Item name is required");
        }

        if (itemName.length() > 255) {
            throw new IllegalArgumentException("Item name cannot exceed 255 characters");
        }
    }

    private void validateRequestType(String requestType) {
        if (!StringUtils.hasText(requestType)) {
            throw new IllegalArgumentException("Request type is required");
        }

        String[] allowedTypes = {"SELF", "OTHERS", "BULK"};
        boolean isValid = false;

        for (String allowedType : allowedTypes) {
            if (allowedType.equalsIgnoreCase(requestType)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException(
                    "Invalid request type. Must be one of: SELF, OTHERS, BULK"
            );
        }
    }

    /**
     * Validates beneficiary lists for OTHERS request type
     */
    private void validateBeneficiaries(String requestType, List<UUID> beneficiaryUserIds, List<String> beneficiaryNames) {

        if ("OTHERS".equalsIgnoreCase(requestType)) {
            // Validate beneficiary user IDs are provided
            if (beneficiaryUserIds == null || beneficiaryUserIds.isEmpty()) {
                throw new IllegalArgumentException("At least one beneficiary user ID is required for requests for others");
            }

            // Validate beneficiary names are provided
            if (beneficiaryNames == null || beneficiaryNames.isEmpty()) {
                throw new IllegalArgumentException("At least one beneficiary name is required for requests for others");
            }

            // Validate counts match
            if (beneficiaryUserIds.size() != beneficiaryNames.size()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Beneficiary user IDs count (%d) must match beneficiary names count (%d)",
                                beneficiaryUserIds.size(),
                                beneficiaryNames.size()
                        )
                );
            }

            // Validate no null values in lists
            if (beneficiaryUserIds.contains(null)) {
                throw new IllegalArgumentException("Beneficiary user IDs cannot contain null values");
            }

            // Validate each beneficiary name
            for (int i = 0; i < beneficiaryNames.size(); i++) {
                String name = beneficiaryNames.get(i);

                if (!StringUtils.hasText(name)) {
                    throw new IllegalArgumentException(
                            String.format("Beneficiary name at index %d cannot be empty", i)
                    );
                }

                if (name.length() > 255) {
                    throw new IllegalArgumentException(
                            String.format("Beneficiary name at index %d cannot exceed 255 characters", i)
                    );
                }
            }

            // Optional: Validate no duplicate beneficiary user IDs
            long uniqueCount = beneficiaryUserIds.stream().distinct().count();
            if (uniqueCount != beneficiaryUserIds.size()) {
                throw new IllegalArgumentException("Beneficiary user IDs cannot contain duplicates");
            }
        } else if ("SELF".equalsIgnoreCase(requestType)) {
            // Validate SELF requests don't have beneficiaries
            if (beneficiaryUserIds != null && !beneficiaryUserIds.isEmpty()) {
                throw new IllegalArgumentException("SELF request type cannot have beneficiary users");
            }

            if (beneficiaryNames != null && !beneficiaryNames.isEmpty()) {
                throw new IllegalArgumentException("SELF request type cannot have beneficiary names");
            }
        }
    }

    private void validateTenantAndUser(String tenantId, UUID userId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("Tenant ID is required");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}