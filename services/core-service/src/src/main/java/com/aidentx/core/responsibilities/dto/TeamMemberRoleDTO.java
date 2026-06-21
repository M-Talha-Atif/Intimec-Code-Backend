package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing a role held by a team member.
 * Used in the Team Members view to show internal roles assigned to subordinates.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamMemberRoleDTO {
    
    /**
     * Role ID
     */
    private UUID id;
    
    /**
     * Role name
     */
    private String name;
    
    /**
     * Role type (SYSTEM, ADMINISTRATOR, REVIEWER, OPERATOR, VIEWER)
     */
    private String roleType;
    
    /**
     * Organizational scope (Organization, System, Application, Custom)
     */
    private String scope;
    
    /**
     * Whether this is a critical/sensitive role
     */
    private Boolean isCritical;
    
    /**
     * Risk level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String riskLevel;
    
    /**
     * Number of permissions in this role
     */
    private Integer permissionCount;
    
    /**
     * When the role was assigned to this user (ISO 8601)
     */
    private String assignedAt;
    
    /**
     * Who assigned the role
     */
    private String assignedBy;
    
    /**
     * Whether the role is currently active
     */
    private Boolean isActive;
}
