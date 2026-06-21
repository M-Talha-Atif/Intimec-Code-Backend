package com.aidentx.applicationpermissions.roles_management.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for bulk role import operation.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Response for bulk role import operation")
public record BulkRoleImportResponse(
    
    @Schema(description = "List of successfully created roles", example = "[{...}]")
    List<RoleResponse> successfulImports,
    
    @Schema(description = "List of failed imports with error details", example = "[{...}]")
    List<RoleImportError> failedImports,
    
    @Schema(description = "Summary of the import operation")
    ImportSummary summary
) {
    
    /**
     * Represents a single role import error.
     */
    @Schema(description = "Details of a failed role import")
    public record RoleImportError(
        @Schema(description = "Name of the role that failed to import", example = "System Administrator")
        String roleName,
        
        @Schema(description = "Error message explaining why the import failed", example = "Role with this name already exists")
        String error,
        
        @Schema(description = "Index of the role in the original import array", example = "0")
        int index
    ) {}
    
    /**
     * Summary statistics for the import operation.
     */
    @Schema(description = "Summary statistics for the bulk import")
    public record ImportSummary(
        @Schema(description = "Total number of roles in the import request", example = "10")
        int totalRoles,
        
        @Schema(description = "Number of successfully imported roles", example = "8")
        int successCount,
        
        @Schema(description = "Number of failed imports", example = "2")
        int failureCount
    ) {}
}
