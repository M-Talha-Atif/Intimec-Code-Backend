package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.ConditionalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ConditionalRule entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * conditional rules that restrict permission access.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ConditionalRuleRepository extends JpaRepository<ConditionalRule, UUID> {

    /**
     * Find all conditional rules for a specific permission.
     * 
     * @param permissionId the ID of the permission
     * @return list of conditional rules for the permission
     */
    List<ConditionalRule> findByPermissionId(UUID permissionId);

    /**
     * Find conditional rules by justification pattern.
     * 
     * @param justificationPattern the pattern to search for in rule justifications
     * @return list of matching conditional rules
     */
    List<ConditionalRule> findByJustificationContainingIgnoreCase(String justificationPattern);

    /**
     * Find conditional rules by condition script pattern.
     * 
     * @param scriptPattern the pattern to search for in condition scripts
     * @return list of matching conditional rules
     */
    List<ConditionalRule> findByConditionScriptContainingIgnoreCase(String scriptPattern);

    /**
     * Find all conditional rules ordered by permission ID and justification.
     * 
     * @return list of conditional rules ordered by permission and justification
     */
    List<ConditionalRule> findAllByOrderByPermissionIdAscJustificationAsc();

    /**
     * Count the number of conditional rules for a specific permission.
     * 
     * @param permissionId the ID of the permission
     * @return count of conditional rules
     */
    Long countByPermissionId(UUID permissionId);

    /**
     * Delete all conditional rules for a specific permission.
     * 
     * @param permissionId the ID of the permission
     */
    void deleteByPermissionId(UUID permissionId);

    /**
     * Find conditional rules that reference specific organizational elements.
     * 
     * @param organizationalElement the organizational element to search for (e.g., "department", "region", "globalOrg")
     * @return list of conditional rules that reference the organizational element
     */
    @Query("SELECT cr FROM ConditionalRule cr WHERE LOWER(cr.conditionScript) LIKE LOWER(CONCAT('%', :element, '%'))")
    List<ConditionalRule> findByOrganizationalElement(@Param("element") String organizationalElement);

    /**
     * Find conditional rules for permissions assigned to a specific role.
     * 
     * @param roleId the ID of the role
     * @return list of conditional rules for the role's permissions
     */
    @Query("SELECT cr FROM ConditionalRule cr " +
           "JOIN RolePermission rp ON cr.permissionId = rp.permissionId " +
           "WHERE rp.roleId = :roleId")
    List<ConditionalRule> findByRoleId(@Param("roleId") UUID roleId);
}