package com.aidentx.iam.identity_management.model.dto;

import java.sql.Date;
import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

import jakarta.validation.constraints.Email;

/**
 * DTO for updating an existing NonEmployee.
 * All fields are optional since updates may only modify specific attributes.
 */
public record NonEmployeeUpdateDto(
        @Email String email,
        String username,
        String password,
        String firstName,
        String lastName,
        String contractId,
        String vendorName,
        String riskLevel,
        Date contractEndDate,
        UUID sponsorId,
        IdentityStatus status,
        Map<String, Object> extendedAttributes) {
}
