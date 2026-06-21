package com.aidentx.iam.identity_management.model.dto;

import java.sql.Date;
import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for creating new Employee identities.
 *
 * <p>This record encapsulates all the required and optional data needed to create
 * a new employee identity in the IAM system. It includes standard employee attributes
 * as well as support for extended custom attributes defined at the tenant level.</p>
 *
 * <h2>Validation Rules:</h2>
 * <ul>
 *   <li><strong>email:</strong> Must be a valid email format and unique within tenant</li>
 *   <li><strong>username:</strong> Required, must be unique within tenant</li>
 *   <li><strong>employeeId:</strong> Required, must be unique within tenant</li>
 *   <li><strong>managerId:</strong> If provided, must reference an existing employee in the same tenant</li>
 *   <li><strong>password:</strong> Will be hashed before storage if provided</li>
 * </ul>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>Display name is auto-generated from firstName + lastName if both are provided</li>
 *   <li>Status defaults to ACTIVE if not specified</li>
 *   <li>Manager relationship creates a hierarchical reporting structure</li>
 *   <li>Extended attributes are validated against tenant's attribute definitions</li>
 * </ul>
 *
 * @param email Employee's email address (required, unique within tenant)
 * @param username Employee's username for system access (required, unique within tenant)
 * @param password Plain text password (will be hashed before storage)
 * @param firstName Employee's first name
 * @param lastName Employee's last name
 * @param employeeId Company employee identifier (required, unique within tenant)
 * @param jobTitle Employee's job title/position
 * @param department Employee's department
 * @param riskLevel Employee identity risk level (low, medium, high)
 * @param managerId UUID of the employee's manager (must exist in same tenant if provided)
 * @param hireDate Employee's hire date
 * @param status Initial status of the employee account (defaults to ACTIVE)
 * @param extendedAttributes Map of custom attributes defined for the tenant
 *
 * @since 1.0
 * @see IdentityStatus
 * @see EmployeeUpdateDto
 */
public record EmployeeCreateDto(
        @NotBlank @Email String email,
        @NotBlank String username,
        String password,
        String firstName,
        String lastName,
        @NotBlank String employeeId,
        String jobTitle,
        String department,
        String riskLevel,
        UUID managerId,
        Date hireDate,
        IdentityStatus status,
        Map<String, Object> extendedAttributes) {
}