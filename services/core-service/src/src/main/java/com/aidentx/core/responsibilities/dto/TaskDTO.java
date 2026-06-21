package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for task representation in the Responsibilities page.
 * Maps workflow approvals and other governance tasks to a unified task model.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    
    /**
     * Unique task identifier (maps to approval ID for workflow approvals)
     */
    private String id;
    
    /**
     * Task title (human-readable summary)
     */
    private String title;
    
    /**
     * Task type: workflow, certification, delegation, role_approval
     */
    private String type;
    
    /**
     * Due date/deadline for the task
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime dueDate;
    
    /**
     * Task status: pending, in_progress, completed
     */
    private String status;
    
    /**
     * Priority level: low, medium, high, critical
     */
    private String priority;
    
    /**
     * User ID to whom this task is assigned
     */
    private String assignedTo;
    
    /**
     * Risk level associated with this task
     */
    private String risk;
    
    /**
     * Requester information
     */
    private UserInfo requester;
    
    /**
     * Brief description/justification
     */
    private String description;
    
    /**
     * Created timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;
    
    /**
     * Decision made on the task (for processed tasks): APPROVED, REJECTED, TIMEOUT
     */
    private String decision;
    
    /**
     * Timestamp when decision was made (for processed tasks)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime respondedAt;
    
    /**
     * Comments/reason provided with the decision
     */
    private String comments;
    
    /**
     * Role name associated with this task (for filtering)
     */
    private String roleName;
    
    /**
     * Workflow name associated with this task (for filtering)
     */
    private String workflowName;
    
    /**
     * Workflow instance/execution ID (for grouping related approval tasks)
     */
    private String workflowInstanceId;
    
    /**
     * Approver type/role: MANAGER, ROLE_OWNER, APP_OWNER, etc.
     */
    private String approverType;
    
    /**
     * Nested user information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String name;
        private String email;
        private String department;
    }
}
