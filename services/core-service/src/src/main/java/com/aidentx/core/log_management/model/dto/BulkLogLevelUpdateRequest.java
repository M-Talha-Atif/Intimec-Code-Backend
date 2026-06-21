package com.aidentx.core.log_management.model.dto;

import java.util.List;

/**
 * DTO representing a bulk request to update log levels for multiple components.
 */
public class BulkLogLevelUpdateRequest {
    
    private List<ComponentLogLevelUpdateRequest> components;

    public BulkLogLevelUpdateRequest() {}

    public BulkLogLevelUpdateRequest(List<ComponentLogLevelUpdateRequest> components) {
        this.components = components;
    }

    public List<ComponentLogLevelUpdateRequest> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentLogLevelUpdateRequest> components) {
        this.components = components;
    }
}
