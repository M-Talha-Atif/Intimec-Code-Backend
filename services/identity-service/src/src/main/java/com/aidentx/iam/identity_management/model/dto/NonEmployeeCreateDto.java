package com.aidentx.iam.identity_management.model.dto;

import java.sql.Date;
import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new NonEmployee.
 * Contains all core non-employee fields plus a map for extended attributes.
 */
public record NonEmployeeCreateDto(
        @NotBlank @Email String email,
        @NotBlank String username,
        String password,
        String firstName,
        String lastName,
        @NotBlank String contractId,
        String vendorName,
        String riskLevel,
        Date contractEndDate,
        UUID sponsorId,
        IdentityStatus status,
        Map<String, Object> extendedAttributes) {
}
