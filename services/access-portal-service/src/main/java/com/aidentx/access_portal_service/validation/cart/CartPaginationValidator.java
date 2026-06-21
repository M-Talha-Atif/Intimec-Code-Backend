package com.aidentx.access_portal_service.validation.cart;


import com.aidentx.access_portal_service.config.AccessPortalConstants;
import org.springframework.stereotype.Component;

/**
 * Validator for cart pagination parameters
 */
@Component
public class CartPaginationValidator {

    /**
     * Validates pagination parameters
     */
    public void validatePagination(int page, int size) {
        validatePageNumber(page);
        validatePageSize(size);
    }

    /**
     * Validates page number
     */
    public void validatePageNumber(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
    }

    /**
     * Validates page size
     */
    public void validatePageSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        if (size > AccessPortalConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page size cannot exceed " + AccessPortalConstants.MAX_PAGE_SIZE
            );
        }
    }

    /**
     * Validates sort direction
     */
    public void validateSortDirection(String sortDirection) {
        if (sortDirection == null) {
            throw new IllegalArgumentException("Sort direction cannot be null");
        }

        if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }
    }

    /**
     * Validates sort field
     */
    public void validateSortField(String sortField) {
        if (sortField == null || sortField.trim().isEmpty()) {
            throw new IllegalArgumentException("Sort field cannot be empty");
        }

        // Add allowed sort fields based on your CartItem entity
        String[] allowedSortFields = {
                "createdAt", "updatedAt", "itemName", "itemType",
                "requestType", "beneficiaryName"
        };

        boolean isValid = false;
        for (String allowedField : allowedSortFields) {
            if (allowedField.equalsIgnoreCase(sortField.trim())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException(
                    "Invalid sort field. Allowed fields: createdAt, updatedAt, itemName, " +
                            "itemType, requestType, beneficiaryName"
            );
        }
    }
}
