package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new action.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class CreateActionRequest {

    @NotBlank(message = AppConstants.VALIDATION_ACTION_NAME_REQUIRED)
    @Size(min = 1, max = 100, message = AppConstants.VALIDATION_ACTION_NAME_LENGTH)
    private String actionName;

    @Size(max = 255, message = AppConstants.VALIDATION_ENDPOINT_MAX_LENGTH)
    private String endpoint;

    private HttpMethod method;

    // Constructors
    public CreateActionRequest() {}

    public CreateActionRequest(String actionName, String endpoint) {
        this.actionName = actionName;
        this.endpoint = endpoint;
    }

    public CreateActionRequest(String actionName, String endpoint, HttpMethod method) {
        this.actionName = actionName;
        this.endpoint = endpoint;
        this.method = method;
    }

    // Getters and Setters
    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }
}