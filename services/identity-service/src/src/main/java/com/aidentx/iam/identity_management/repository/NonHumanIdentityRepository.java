package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NonHumanIdentityRepository extends JpaRepository<NonHumanIdentity, UUID> {
    Optional<NonHumanIdentity> findByServiceName(String serviceName);

    /**
     * Finds a non-human identity by tenant and service name.
     *
     * @param tenantId The tenant ID.
     * @param serviceName The service name to search for.
     * @return An Optional containing the NonHumanIdentity if found.
     */
    Optional<NonHumanIdentity> findByTenantIdAndServiceName(String tenantId, String serviceName);
}