package com.aidentx.core.onboarding.repository;

import com.aidentx.core.onboarding.model.entity.ManagedAccount;
import com.aidentx.core.onboarding.model.enums.ManagedAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface ManagedAccountRepository extends JpaRepository<ManagedAccount, UUID> {

        /**
         * Finds a single ManagedAccount by its unique composite key.
         * Used by the reconciliation loop in AggregationService.
         */
        Optional<ManagedAccount> findByResourceIdAndNativeIdentifier(UUID resourceId, String nativeIdentifier);

        /**
         * Streams all accounts for a resource that were not seen during the current
         * aggregation run.
         * Used by AggregationService to detect deletions.
         */
        @Query("SELECT m FROM ManagedAccount m WHERE m.resourceId = :resourceId AND m.lastSeenAt < :runStartedAt")
        Stream<ManagedAccount> streamAllByResourceIdAndLastSeenBefore(
                        @Param("resourceId") UUID resourceId,
                        @Param("runStartedAt") Instant runStartedAt);

        /**
         * Finds all accounts for a given tenant and resource with a specific status.
         * Used by CorrelationService to find UNMATCHED accounts.
         */
        @Query("SELECT m FROM ManagedAccount m WHERE m.tenantId = ?1 AND m.resourceId = ?2 AND m.status = ?3")
        Stream<ManagedAccount> findAllByTenantIdAndResourceIdAndStatus(String tenantId, UUID resourceId,
                        ManagedAccountStatus status);

        /**
         * Finds all accounts linked to a specific Identity with a given status.
         * Used by ProvisioningService for the Leaver process.
         */
        List<ManagedAccount> findAllByIdentityIdAndStatus(UUID identityId, ManagedAccountStatus status);

        /**
         * This query now includes the mandatory tenantId parameter to ensure
         * data is correctly partitioned.
         * 
         * Streams all accounts for a specific tenant and resource that were NOT seen
         * during the current aggregation run. This is used to efficiently detect and
         * mark accounts for deletion. Must be called within a @Transactional context.
         */
        @Query("SELECT m FROM ManagedAccount m WHERE m.tenantId = :tenantId AND m.resourceId = :resourceId AND m.lastSeenAt < :runStartedAt")
        Stream<ManagedAccount> streamAllByTenantIdAndResourceIdAndLastSeenBefore(
                        @Param("tenantId") String tenantId,
                        @Param("resourceId") UUID resourceId,
                        @Param("runStartedAt") Instant runStartedAt);

        long countByTenantIdAndResourceId(String tenantId, UUID resourceId);

        long countByTenantIdAndResourceIdAndStatus(String tenantId, UUID resourceId, ManagedAccountStatus status);

        /** Delete all managed accounts for a resource within a tenant (cascade delete on application removal). */
        @Modifying
        @Query("DELETE FROM ManagedAccount m WHERE m.resourceId = :resourceId AND m.tenantId = :tenantId")
        void deleteAllByResourceIdAndTenantId(@Param("resourceId") UUID resourceId, @Param("tenantId") String tenantId);
}