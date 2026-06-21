package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRuleGlobalScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for tenant-scoped access-role assignment-rule global schedule config.
 */
@Repository
public interface AccessRoleAssignmentRuleGlobalScheduleConfigRepository
        extends JpaRepository<AccessRoleAssignmentRuleGlobalScheduleConfig, UUID> {

    /**
     * Finds the persisted global schedule config for a tenant.
     *
     * @param tenantId tenant identifier
     * @return persisted config when present
     */
    Optional<AccessRoleAssignmentRuleGlobalScheduleConfig> findByTenantId(String tenantId);
}

