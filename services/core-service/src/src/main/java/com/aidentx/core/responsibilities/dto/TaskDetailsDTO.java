package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed DTO for task information displayed in TaskDetailsModal.
 * Contains all 15+ IAM fields for rich context.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailsDTO {
    
    // Basic task info
    private String id;
    private String title;
    private String type;
    private String status;
    private String priority;
    private String risk;
    
    // Dates
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime dueAt;
    
    // People
    private UserInfo requester;
    private UserInfo assignee;
    
    // Workflow context
    private String workflowName;
    private String workflowInstanceId;
    private String approvalId;
    private String eventType; // For dynamic rendering: role.assigned, permission.bulk.assigned, etc.
    
    // Access Portal Context (Enterprise IAM fields)
    private String tileName;
    private String applicationName;
    private RoleInfo role;
    private String targetUsers;
    private Integer targetUserCount;
    private String justification;
    
    // Dynamic event-specific data
    private List<PermissionItem> permissionList;      // For permission grants/revokes
    private List<UserInfo> targetUsersList;           // For bulk role assignments
    private PermissionItem singlePermission;          // For single permission operations
    
    // Full context maps
    private Map<String, Object> approvalContext;
    private Map<String, Object> evidenceBundle;
    
    // Multi-approval info
    private String approvalGroupId;
    private String approvalPolicy;
    private Integer requiredApprovals;
    
    // Changes (for diff view)
    private List<AccessChange> changes;
    
    // SoD findings
    private List<SoDFinding> sodFindings;
    
    // Timeline
    private List<TimelineEvent> timeline;
    
    // Comments
    private List<Comment> comments;
    
    /**
     * User information
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
    
    /**
     * Role information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private String id;
        private String name;
        private String type;
        private String description;
    }
    
    /**
     * Access change item (for diff view)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessChange {
        private String id;
        private String type;
        private String action; // add, remove, modify
        private String description;
        private String before;
        private String after;
    }
    
    /**
     * Segregation of Duties finding
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoDFinding {
        private String id;
        private String severity; // low, medium, high, critical
        private String policyName;
        private String description;
        private String conflictingRole;
        private String mitigationAction;
    }
    
    /**
     * Timeline event
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private String id;
        private String type; // created, assigned, approved, rejected, delegated, commented
        private String actor;
        private String description;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private OffsetDateTime timestamp;
        private Map<String, Object> metadata;
    }
    
    /**
     * Comment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comment {
        private String id;
        private String author;
        private String content;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private OffsetDateTime timestamp;
    }
    
    /**
     * Permission item for single/bulk permission operations
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItem {
        private String actionId;
        private String actionName;        // e.g., "View", "Edit", "Delete"
        private String pageName;          // e.g., "Dashboard", "Workflows", "Users"
        private String permissionType;    // e.g., "READ", "WRITE", "DELETE"
        private String justification;
        private String conditionScript;
    }
}

