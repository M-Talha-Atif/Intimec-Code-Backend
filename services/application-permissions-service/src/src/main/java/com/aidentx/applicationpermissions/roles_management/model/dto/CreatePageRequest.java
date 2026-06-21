package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new page.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class CreatePageRequest {

    @NotBlank(message = AppConstants.VALIDATION_PAGE_NAME_REQUIRED)
    @Size(min = 1, max = 100, message = AppConstants.VALIDATION_PAGE_NAME_LENGTH)
    private String pageName;
    
    @Size(max = 255, message = AppConstants.VALIDATION_PAGE_PATH_LENGTH)
    private String pagePath;

    // Constructors
    public CreatePageRequest() {}

    public CreatePageRequest(String pageName) {
        this.pageName = pageName;
    }
    
    public CreatePageRequest(String pageName, String pagePath) {
        this.pageName = pageName;
        this.pagePath = pagePath;
    }

    // Getters and Setters
    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
    
    public String getPagePath() {
        return pagePath;
    }
    
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }
}