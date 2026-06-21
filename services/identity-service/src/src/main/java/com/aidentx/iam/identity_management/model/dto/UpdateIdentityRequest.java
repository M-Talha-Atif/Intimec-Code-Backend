package com.aidentx.iam.identity_management.model.dto;

import java.util.Map;

/**
 * Generic DTO for updating identities via gRPC.
 * This serves as a bridge between gRPC requests and the specific typed DTOs.
 */
public class UpdateIdentityRequest {
    private Map<String, Object> data;

    public UpdateIdentityRequest() {}

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
