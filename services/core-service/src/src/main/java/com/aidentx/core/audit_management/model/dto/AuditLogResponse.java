package com.aidentx.core.audit_management.model.dto;

import com.aidentx.core.audit_management.model.entity.AuditLog;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Response DTO for audit logs API that includes both the paginated results
 * and metadata arrays for frontend dropdowns.
 */
public class AuditLogResponse {
    
    /**
     * The paginated audit log results
     */
    private Page<AuditLog> logs;
    
    /**
     * All unique events from the database for dropdown population
     */
    private List<String> availableEvents;
    
    /**
     * All unique object types from the database for dropdown population
     */
    private List<String> availableObjectTypes;
    
    /**
     * All unique risk levels from the database for dropdown population
     */
    private List<String> availableRisks;
    
    /**
     * All unique statuses from the database for dropdown population
     */
    private List<String> availableStatuses;
    
    public AuditLogResponse() {}
    
    public AuditLogResponse(Page<AuditLog> logs, List<String> availableEvents, 
                           List<String> availableObjectTypes, List<String> availableRisks,
                           List<String> availableStatuses) {
        this.logs = logs;
        this.availableEvents = availableEvents;
        this.availableObjectTypes = availableObjectTypes;
        this.availableRisks = availableRisks;
        this.availableStatuses = availableStatuses;
    }
    
    public Page<AuditLog> getLogs() {
        return logs;
    }
    
    public void setLogs(Page<AuditLog> logs) {
        this.logs = logs;
    }
    
    public List<String> getAvailableEvents() {
        return availableEvents;
    }
    
    public void setAvailableEvents(List<String> availableEvents) {
        this.availableEvents = availableEvents;
    }
    
    public List<String> getAvailableObjectTypes() {
        return availableObjectTypes;
    }
    
    public void setAvailableObjectTypes(List<String> availableObjectTypes) {
        this.availableObjectTypes = availableObjectTypes;
    }
    
    public List<String> getAvailableRisks() {
        return availableRisks;
    }
    
    public void setAvailableRisks(List<String> availableRisks) {
        this.availableRisks = availableRisks;
    }
    
    public List<String> getAvailableStatuses() {
        return availableStatuses;
    }
    
    public void setAvailableStatuses(List<String> availableStatuses) {
        this.availableStatuses = availableStatuses;
    }
}
