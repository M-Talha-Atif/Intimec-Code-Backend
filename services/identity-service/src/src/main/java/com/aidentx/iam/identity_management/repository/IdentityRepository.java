package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import com.aidentx.iam.identity_management.model.enums.IdentityType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for the base Identity entity.
 * Enables polymorphic queries across all identity types.
 */
@Repository
public interface IdentityRepository extends JpaRepository<Identity, UUID>, JpaSpecificationExecutor<Identity> {

        /**
         * Finds an identity by tenant and ID for additional security.
         *
         * @param tenantId The tenant ID.
         * @param id       The identity ID.
         * @return An Optional containing the Identity if found.
         */
        Optional<Identity> findByTenantIdAndId(String tenantId, UUID id);

        /**
         * Finds multiple identities by tenant and ID set.
         *
         * @param tenantId The tenant ID.
         * @param ids      The identity IDs.
         * @return matching identities for the tenant.
         */
        java.util.List<Identity> findByTenantIdAndIdIn(String tenantId, Collection<UUID> ids);

        /**
         * Finds all identities for a specific tenant with pagination.
         *
         * @param tenantId The tenant ID.
         * @param pageable Pagination information.
         * @return Page of identities.
         */
        Page<Identity> findByTenantId(String tenantId, Pageable pageable);

        /**
         * Counts identities by tenant and identity type.
         *
         * @param tenantId     The tenant ID.
         * @param identityType The identity type enum.
         * @return Count of identities.
         */
        long countByTenantIdAndIdentityType(String tenantId, IdentityType identityType);

        /**
         * Counts identities by tenant, identity type, and status.
         *
         * @param tenantId     The tenant ID.
         * @param identityType The identity type enum.
         * @param status       The identity status enum.
         * @return Count of identities matching the criteria.
         */
        long countByTenantIdAndIdentityTypeAndStatus(String tenantId, IdentityType identityType, IdentityStatus status);

        /**
         * Finds all identities for a specific tenant and identity type.
         *
         * @param tenantId     The tenant ID.
         * @param identityType The identity type enum.
         * @return List of identities.
         */
        java.util.List<Identity> findByTenantIdAndIdentityType(String tenantId, IdentityType identityType);

        /**
         * Finds identities for a specific tenant and identity type with pagination.
         *
         * @param tenantId     The tenant ID.
         * @param identityType The identity type enum.
         * @param pageable     Pagination information.
         * @return Page of identities.
         */
        Page<Identity> findByTenantIdAndIdentityType(String tenantId, IdentityType identityType, Pageable pageable);
}