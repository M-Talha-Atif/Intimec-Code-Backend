package com.aidentx.core.log_management.model.dto;

/**
 * DTO representing a request to update log level for a specific component.
 */
public class ComponentLogLevelUpdateRequest {
    
    private String componentName;
    private String loggerName;
    private String level;

    public ComponentLogLevelUpdateRequest() {}

    public ComponentLogLevelUpdateRequest(String componentName, String loggerName, String level) {
        this.componentName = componentName;
        this.loggerName = loggerName;
        this.level = level;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
