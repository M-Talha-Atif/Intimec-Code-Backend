package com.aidentx.applicationpermissions.roles_management.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for bulk importing roles from a JSON file.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Request to import multiple roles from JSON file")
public class BulkRoleImportRequest {

    /**
     * List of roles to be imported.
     */
    @Schema(description = "Array of roles to import", required = true)
    @NotEmpty(message = "At least one role must be provided for import")
    @Size(max = 100, message = "Maximum 100 roles can be imported at once")
    @Valid
    private List<CreateRoleRequest> roles;

    // Default constructor
    public BulkRoleImportRequest() {}

    // Constructor with parameters
    public BulkRoleImportRequest(List<CreateRoleRequest> roles) {
        this.roles = roles;
    }

    // Getters and Setters
    public List<CreateRoleRequest> getRoles() {
        return roles;
    }

    public void setRoles(List<CreateRoleRequest> roles) {
        this.roles = roles;
    }
}
