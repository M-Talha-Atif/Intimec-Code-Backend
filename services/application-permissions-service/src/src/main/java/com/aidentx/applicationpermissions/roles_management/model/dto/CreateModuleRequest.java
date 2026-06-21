package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new module.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class CreateModuleRequest {

    @NotBlank(message = AppConstants.VALIDATION_MODULE_NAME_REQUIRED)
    @Size(min = 1, max = 100, message = AppConstants.VALIDATION_MODULE_NAME_LENGTH)
    private String moduleName;

    // Constructors
    public CreateModuleRequest() {}

    public CreateModuleRequest(String moduleName) {
        this.moduleName = moduleName;
    }

    // Getters and Setters
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}