package com.aidentx.access_portal_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity for all tenant-aware entities.
 * Includes tenantId, createdAt, updatedAt fields.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAwareEntity {

    @Column(nullable = false)
    private String tenantId;

    // Audit fields
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
