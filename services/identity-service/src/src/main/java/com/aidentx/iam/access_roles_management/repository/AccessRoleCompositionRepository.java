package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link AccessRoleComposition} join entity operations.
 */
@Repository
public interface AccessRoleCompositionRepository extends JpaRepository<AccessRoleComposition, UUID> {

    /**
     * Finds all child role links for a given parent role.
     *
     * @param parentRoleId the parent role ID
     * @return list of composition links
     */
    List<AccessRoleComposition> findByParentRoleId(UUID parentRoleId);

    /** Finds all child role links for multiple parent roles. */
    List<AccessRoleComposition> findByParentRoleIdIn(Collection<UUID> parentRoleIds);

    /**
     * Finds all parent role links for a given child role.
     *
     * @param childRoleId the child role ID
     * @return list of composition links
     */
    List<AccessRoleComposition> findByChildRoleId(UUID childRoleId);

    /** Finds all parent role links for multiple child roles. */
    List<AccessRoleComposition> findByChildRoleIdIn(Collection<UUID> childRoleIds);

    /** Counts child links where the role is the parent. */
    long countByParentRoleId(UUID parentRoleId);

    /** Counts parent links where the role is the child. */
    long countByChildRoleId(UUID childRoleId);

    /** Counts distinct parent roles that need closure refresh when this child role link is removed. */
    @Query("SELECT COUNT(DISTINCT c.parentRoleId) FROM AccessRoleComposition c WHERE c.childRoleId = :childRoleId")
    long countDistinctParentRoleIdByChildRoleId(@Param("childRoleId") UUID childRoleId);

    /**
     * Deletes all child links for a given parent role.
     *
     * @param parentRoleId the parent role ID
     */
    void deleteByParentRoleId(UUID parentRoleId);

    /**
     * Deletes all parent links for a given child role.
     *
     * @param childRoleId the child role ID
     */
    void deleteByChildRoleId(UUID childRoleId);

    /**
     * Deletes a specific parent-child link.
     *
     * @param parentRoleId the parent role ID
     * @param childRoleId  the child role ID
     */
    void deleteByParentRoleIdAndChildRoleId(UUID parentRoleId, UUID childRoleId);

    /**
     * Checks if a specific composition link already exists.
     *
     * @param parentRoleId the parent role ID
     * @param childRoleId  the child role ID
     * @return true if the link exists
     */
    boolean existsByParentRoleIdAndChildRoleId(UUID parentRoleId, UUID childRoleId);
}

