package com.aidentx.iam.identity_management.model.dto;

import java.sql.Date;
import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

import jakarta.validation.constraints.Email;

/**
 * DTO for updating an existing Employee.
 * All fields are optional since updates may only modify specific attributes.
 */
public record EmployeeUpdateDto(
        @Email String email,
        String username,
        String password,
        String firstName,
        String lastName,
        String employeeId,
        String jobTitle,
        String department,
        String riskLevel,
        UUID managerId,
        Date hireDate,
        IdentityStatus status,
        Map<String, Object> extendedAttributes) {
}
