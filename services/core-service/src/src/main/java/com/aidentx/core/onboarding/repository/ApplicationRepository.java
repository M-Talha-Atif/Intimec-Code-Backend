package com.aidentx.core.onboarding.repository;

import com.aidentx.core.onboarding.model.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query("SELECT a FROM Application a WHERE a.tenantId = :tenantId")
    Page<Application> findByTenantId(String tenantId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.tenantId = :tenantId")
    List<Application> findByTenantId(String tenantId);

    @Query("SELECT a FROM Application a WHERE a.tenantId = :tenantId AND a.associatedSchemaId = :schemaId")
    List<Application> findByTenantIdAndAssociatedSchemaId(String tenantId, UUID schemaId);

    @Query("SELECT a FROM Application a WHERE a.id = :id AND a.tenantId = :tenantId")
    Optional<Application> findByIdAndTenantId(UUID id, String tenantId);

    List<Application> findByIdInAndTenantId(List<UUID> ids, String tenantId);
}