package com.aidentx.iam.role_management.controller;

import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleCreateDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleResponseDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleUpdateDto;
import com.aidentx.iam.role_management.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Creates a new, empty Business Role.
     * @param createDto The DTO containing the role's name, tenant, and description.
     * @return The newly created role.
     */
    @PostMapping
    public ResponseEntity<RoleResponseDto> createRole(@Valid @RequestBody RoleCreateDto createDto) {
        RoleResponseDto createdRole = roleService.createRole(createDto);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    /**
     * Retrieves a single Business Role by its UUID, including its composition.
     * @param roleId The UUID of the role.
     * @return The role DTO or 404 Not Found.
     */
    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> getRoleById(@PathVariable UUID roleId) {
        return roleService.findById(roleId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Retrieves a paginated list of all Business Roles.
     * @param pageable Standard Spring pagination parameters.
     * @return A page of roles.
     */
    @GetMapping
    public ResponseEntity<Page<RoleResponseDto>> getAllRoles(Pageable pageable) {
        return ResponseEntity.ok(roleService.findAll(pageable));
    }

    /**
     * Updates a Business Role's metadata and its composition of technical entitlements.
     * @param roleId The UUID of the role to update.
     * @param updateDto The DTO containing the new details and list of entitlement IDs.
     * @return The updated role.
     */
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponseDto> updateRole(@PathVariable UUID roleId, @Valid @RequestBody RoleUpdateDto updateDto) {
        return ResponseEntity.ok(roleService.updateRole(roleId, updateDto));
    }

    /**
     * Assigns a Business Role to an Identity. Triggers an asynchronous Kafka event.
     * @param roleId The UUID of the role to assign.
     * @param identityId The UUID of the identity receiving the role.
     * @param tenantId The tenant context for the operation.
     */
    @PostMapping("/{roleId}/assignments/{identityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRoleToIdentity(@PathVariable UUID roleId, @PathVariable UUID identityId, @RequestParam String tenantId) {
        roleService.assignRoleToIdentity(roleId, identityId, tenantId);
    }

    /**
     * Revokes a Business Role from an Identity. Triggers an asynchronous Kafka event.
     * @param roleId The UUID of the role to revoke.
     * @param identityId The UUID of the identity losing the role.
     * @param tenantId The tenant context for the operation.
     */
    @DeleteMapping("/{roleId}/assignments/{identityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeRoleFromIdentity(@PathVariable UUID roleId, @PathVariable UUID identityId, @RequestParam String tenantId) {
        roleService.revokeRoleFromIdentity(roleId, identityId, tenantId);
    }
}