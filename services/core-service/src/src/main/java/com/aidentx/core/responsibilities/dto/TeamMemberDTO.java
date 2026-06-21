package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing a team member (subordinate) in the Responsibilities dashboard.
 * Contains identity, employment, and access information for direct and indirect reports.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamMemberDTO {
    
    /**
     * User ID from auth-service
     */
    private UUID userId;
    
    /**
     * Identity ID from identity-service
     */
    private UUID identityId;
    
    /**
     * Full display name of the team member
     */
    private String displayName;
    
    /**
     * First name
     */
    private String firstName;
    
    /**
     * Last name
     */
    private String lastName;
    
    /**
     * Email address
     */
    private String email;
    
    /**
     * Username for login
     */
    private String username;
    
    /**
     * Employee ID from HR system
     */
    private String employeeId;
    
    /**
     * Job title / position
     */
    private String jobTitle;
    
    /**
     * Department name
     */
    private String department;
    
    /**
     * User status (ACTIVE, INACTIVE, LOCKED, etc.)
     */
    private String status;
    
    /**
     * Identity status (ACTIVE, INACTIVE, TERMINATED, etc.)
     */
    private String identityStatus;
    
    /**
     * Last login timestamp (ISO 8601)
     */
    private String lastLogin;
    
    /**
     * Account created timestamp (ISO 8601)
     */
    private String createdAt;
    
    /**
     * Hire date (ISO 8601 date only)
     */
    private String hireDate;
    
    /**
     * Termination date if applicable (ISO 8601 date only)
     */
    private String terminationDate;
    
    /**
     * Whether the team member is a direct report (true) or indirect report (false)
     */
    private Boolean isDirectReport;
    
    /**
     * Risk score calculated based on access, behavior, and compliance
     * Range: 0-100, higher means higher risk
     */
    private Integer riskScore;
    
    /**
     * Count of internal/system roles held by this team member
     */
    private Integer roleCount;
    
    /**
     * Count of total permissions across all roles
     */
    private Integer permissionCount;
    
    /**
     * List of internal roles held by this team member
     * Only populated when explicitly requested
     */
    private List<TeamMemberRoleDTO> roles;
    
    /**
     * Manager's identity ID (useful for indirect reports)
     */
    private UUID managerId;
    
    /**
     * Manager's display name (useful for indirect reports)
     */
    private String managerName;
    
    /**
     * Whether MFA is enabled for this user
     */
    private Boolean mfaEnabled;
    
    /**
     * Number of days since last login (null if never logged in)
     */
    private Integer daysSinceLastLogin;
    
    /**
     * Number of active sessions
     */
    private Integer activeSessions;
}
