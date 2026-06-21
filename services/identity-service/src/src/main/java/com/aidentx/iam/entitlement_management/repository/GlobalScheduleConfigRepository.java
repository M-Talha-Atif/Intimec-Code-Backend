package com.aidentx.iam.entitlement_management.repository;

import com.aidentx.iam.entitlement_management.model.entity.GlobalScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlobalScheduleConfigRepository extends JpaRepository<GlobalScheduleConfig, UUID> {

    Optional<GlobalScheduleConfig> findByTenantId(String tenantId);
}