package com.aidentx.iam.identity_management.model.dto;

import java.util.Map;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

/**
 * Generic DTO for creating identities via gRPC.
 * This serves as a bridge between gRPC requests and the specific typed DTOs.
 */
public class CreateIdentityRequest {
    private UUID primarySchemaId;
    private String tenantId;
    private IdentityStatus status;
    private UUID createdBy;
    private Map<String, Object> data;

    public CreateIdentityRequest() {}

    public UUID getPrimarySchemaId() {
        return primarySchemaId;
    }

    public void setPrimarySchemaId(UUID primarySchemaId) {
        this.primarySchemaId = primarySchemaId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public IdentityStatus getStatus() {
        return status;
    }

    public void setStatus(IdentityStatus status) {
        this.status = status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
