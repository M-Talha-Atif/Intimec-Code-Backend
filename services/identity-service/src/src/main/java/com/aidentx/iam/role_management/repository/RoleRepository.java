package com.aidentx.iam.role_management.repository;

import com.aidentx.iam.role_management.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Finds a role by its display name within a specific tenant to enforce uniqueness.
     * @param displayName The name of the role.
     * @param tenantId The tenant the role belongs to.
     * @return An Optional containing the Role if found.
     */
    Optional<Role> findByDisplayNameAndTenantId(String displayName, String tenantId);
}