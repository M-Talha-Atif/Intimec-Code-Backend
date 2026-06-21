package com.aidentx.core.responsibilities.service;

import com.aidentx.core.responsibilities.client.ApplicationPermissionsServiceClient;
import com.aidentx.core.responsibilities.client.AuthServiceRestClient;
import com.aidentx.core.responsibilities.client.IdentityServiceRestClient;
import com.aidentx.core.responsibilities.client.UserServiceRestClient;
import com.aidentx.core.responsibilities.dto.TeamMemberDTO;
import com.aidentx.core.responsibilities.dto.TeamMemberRoleDTO;
import com.aidentx.core.responsibilities.dto.TeamResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing team member data and relationships.
 * Coordinates data from identity-service, auth-service, and application-permissions-service.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class TeamMemberService {
    
    private final IdentityServiceRestClient identityServiceRestClient;
    private final AuthServiceRestClient authServiceRestClient;
    private final ApplicationPermissionsServiceClient applicationPermissionsClient;
    private final UserServiceRestClient userServiceRestClient;
    private final IUnifiedLogger logger;
    
    public TeamMemberService(
            IdentityServiceRestClient identityServiceRestClient,
            AuthServiceRestClient authServiceRestClient,
            ApplicationPermissionsServiceClient applicationPermissionsClient,
            UserServiceRestClient userServiceRestClient,
            UnifiedLoggerFactory loggerFactory) {
        this.identityServiceRestClient = identityServiceRestClient;
        this.authServiceRestClient = authServiceRestClient;
        this.applicationPermissionsClient = applicationPermissionsClient;
        this.userServiceRestClient = userServiceRestClient;
        this.logger = loggerFactory.getLogger(TeamMemberService.class, "TeamMemberService");
    }
    
    /**
     * Get team members (subordinates) for a manager.
     * Fetches direct and indirect reports with comprehensive details.
     * 
     * @param managerIdentityId Current manager's identityId (from JWT)
     * @param tenantId Tenant ID
     * @param authToken JWT token for service-to-service calls
     * @return TeamResponse with team members and statistics
     */
    public TeamResponse getTeamMembers(UUID managerIdentityId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching team members for manager")
                .detail("managerIdentityId", managerIdentityId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            // Step 1: Get direct reports from identity-service
            List<Map<String, Object>> employees = identityServiceRestClient.getEmployeesByManager(
                managerIdentityId, 
                tenantId, 
                authToken
            );
            
            if (employees == null || employees.isEmpty()) {
                logger.info()
                    .message("No team members found for manager")
                    .detail("managerId", managerIdentityId.toString())
                    .log();
                
                return buildEmptyResponse("No team members found");
            }
            
            logger.info()
                .message("Found employees for manager")
                .detail("count", String.valueOf(employees.size()))
                .log();
            
            // Step 2: Build team member DTOs from employee data
            List<TeamMemberDTO> teamMembers = new ArrayList<>();
            
            for (Map<String, Object> employee : employees) {
                try {
                    TeamMemberDTO teamMember = buildTeamMemberDTO(employee, managerIdentityId, tenantId, authToken);
                    teamMembers.add(teamMember);
                    
                } catch (Exception e) {
                    logger.error(e)
                        .message("Failed to build team member DTO")
                        .detail("employee", employee.toString())
                        .log();
                }
            }
            
            // Step 6: Calculate statistics
            return buildTeamResponse(teamMembers);
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch team members")
                .detail("managerIdentityId", managerIdentityId.toString())
                .log();
            
            return buildEmptyResponse("Error fetching team members: " + e.getMessage());
        }
    }
    
    /**
     * Build a TeamMemberDTO from employee data with roles.
     */
    private TeamMemberDTO buildTeamMemberDTO(
            Map<String, Object> employee, 
            UUID managerIdentityId,
            String tenantId,
            String authToken) {
        
        // Get employee attributes from IdentityResponseDto structure
        // IdentityResponseDto has: id, tenantId, identityType, status, coreAttributes (Map), extendedAttributes (Map), createdAt, updatedAt
        @SuppressWarnings("unchecked")
        Map<String, Object> coreAttrs = employee.containsKey("coreAttributes") 
            ? (Map<String, Object>) employee.get("coreAttributes") 
            : new HashMap<>();
        
        // Extract identity ID (top-level field in IdentityResponseDto)
        UUID identityId = UUID.fromString(employee.get("id").toString());
        
        // Fetch complete user data from user-service using identity_id
        // This is critical because user_role table uses user.id, not identity.identity_id
        Map<String, Object> userData = userServiceRestClient.getUserByIdentityId(identityId, tenantId, authToken);
        UUID userId = null;
        String lastLoginAt = null;
        Boolean mfaEnabled = false;
        Integer activeSessions = 0;
        Integer failedLoginAttempts = 0;
        String userStatus = null;
        
        if (userData != null) {
            // Extract user_id
            Object userIdObj = userData.get("userId");
            if (userIdObj != null) {
                userId = UUID.fromString(userIdObj.toString());
            }
            
            // Extract authentication data
            lastLoginAt = userData.get("lastLoginAt") != null ? userData.get("lastLoginAt").toString() : null;
            mfaEnabled = userData.get("mfaEnabled") != null ? (Boolean) userData.get("mfaEnabled") : false;
            activeSessions = userData.get("activeSessions") != null ? ((Number) userData.get("activeSessions")).intValue() : 0;
            failedLoginAttempts = userData.get("failedLoginAttempts") != null ? ((Number) userData.get("failedLoginAttempts")).intValue() : 0;
            userStatus = userData.get("userStatus") != null ? userData.get("userStatus").toString() : null;
        }
        
        if (userId == null) {
            // Fallback to identity_id if user not found
            logger.warn()
                .message("Could not fetch user_id for identity, using identity_id as fallback")
                .detail("identityId", identityId.toString())
                .log();
            userId = identityId;
        }
        
        // Extract core attributes (from HumanIdentity: username, email, firstName, lastName, displayName)
        String email = getStringValue(coreAttrs, "email", "");
        String firstName = getStringValue(coreAttrs, "firstName", "");
        String lastName = getStringValue(coreAttrs, "lastName", "");
        String displayName = getStringValue(coreAttrs, "displayName", "");
        if (displayName.isEmpty()) {
            displayName = (firstName + " " + lastName).trim();
        }
        if (displayName.isEmpty() && !email.isEmpty()) {
            displayName = email.split("@")[0];
        }
        if (displayName.isEmpty()) {
            displayName = "Unknown";
        }
        
        String username = getStringValue(coreAttrs, "username", email);
        
        // Extract Employee attributes (from coreAttributes map)
        String employeeId = getStringValue(coreAttrs, "employeeId", "");
        String jobTitle = getStringValue(coreAttrs, "jobTitle", "");
        String department = getStringValue(coreAttrs, "department", "");
        
        // Extract status from top-level field in IdentityResponseDto
        String identityStatus = employee.containsKey("status") ? employee.get("status").toString() : "ACTIVE";
        
        // Extract dates
        String createdAtStr = employee.containsKey("createdAt") ? employee.get("createdAt").toString() : null;
        String hireDate = getStringValue(coreAttrs, "hireDate", createdAtStr);
        String terminationDate = getStringValue(coreAttrs, "terminationDate", null);
        
        // All employees returned from getEmployeesByManager are direct reports by definition
        boolean isDirectReport = true;
        
        // Calculate days since last login
        Integer daysSinceLastLogin = null;
        if (lastLoginAt != null && !lastLoginAt.isEmpty()) {
            try {
                java.time.Instant lastLoginInstant = java.time.Instant.parse(lastLoginAt);
                java.time.Instant now = java.time.Instant.now();
                long daysDiff = java.time.Duration.between(lastLoginInstant, now).toDays();
                daysSinceLastLogin = (int) daysDiff;
            } catch (Exception e) {
                logger.warn()
                    .message("Failed to parse lastLoginAt")
                    .detail("lastLoginAt", lastLoginAt)
                    .detail("error", e.getMessage())
                    .log();
            }
        }
        
        // Calculate risk score based on real data
        int riskScore = 10; // Default low risk
        if (failedLoginAttempts != null && failedLoginAttempts > 3) {
            riskScore += 20; // Multiple failed login attempts
        }
        if (!mfaEnabled) {
            riskScore += 15; // MFA not enabled
        }
        if (daysSinceLastLogin != null && daysSinceLastLogin > 90) {
            riskScore += 25; // Long time since last login
        }
        if ("LOCKED".equals(userStatus)) {
            riskScore += 30; // Account locked
        }
        // Cap at 100
        if (riskScore > 100) {
            riskScore = 100;
        }
        
        // Fetch roles for this user
        List<TeamMemberRoleDTO> roles = fetchUserRoles(userId, tenantId, authToken);
        int roleCount = roles != null ? roles.size() : 0;
        int permissionCount = roles != null ? roles.stream()
            .mapToInt(role -> role.getPermissionCount() != null ? role.getPermissionCount() : 0)
            .sum() : 0;
        
        return TeamMemberDTO.builder()
            // Identity
            .userId(userId)
            .identityId(identityId)
            .displayName(displayName)
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .username(username)
            // Employment
            .employeeId(employeeId)
            .jobTitle(jobTitle)
            .department(department)
            .hireDate(hireDate)
            .terminationDate(terminationDate)
            // Status - prefer user status over identity status
            .status(userStatus != null ? userStatus : ("ACTIVE".equals(identityStatus) ? "ACTIVE" : "INACTIVE"))
            .identityStatus(identityStatus)
            // Access - now using real data from auth-service
            .lastLogin(lastLoginAt)
            .createdAt(getStringValue(employee, "createdAt", null))
            .daysSinceLastLogin(daysSinceLastLogin)
            .activeSessions(activeSessions)
            // Hierarchy
            .isDirectReport(isDirectReport)
            .managerId(managerIdentityId)
            .managerName(null)
            // Security - now using real data from auth-service
            .riskScore(riskScore)
            .mfaEnabled(mfaEnabled)
            // Access summary
            .roleCount(roleCount)
            .permissionCount(permissionCount)
            .roles(roles)
            .build();
    }
    
    /**
     * Fetch and map roles for a user.
     */
    private List<TeamMemberRoleDTO> fetchUserRoles(UUID userId, String tenantId, String authToken) {
        try {
            List<Map<String, Object>> userRoles = applicationPermissionsClient.getUserRoles(userId, tenantId, authToken);
            
            if (userRoles == null || userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            
            return userRoles.stream()
                .map(this::mapToTeamMemberRoleDTO)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch roles for user")
                .detail("userId", userId.toString())
                .log();
            return Collections.emptyList();
        }
    }
    
    /**
     * Map UserRoleResponse to TeamMemberRoleDTO.
     */
    private TeamMemberRoleDTO mapToTeamMemberRoleDTO(Map<String, Object> userRole) {
        try {
            return TeamMemberRoleDTO.builder()
                .id(userRole.get("roleId") != null ? UUID.fromString(userRole.get("roleId").toString()) : null)
                .name(getStringValue(userRole, "roleName", "Unknown Role"))
                .roleType(getStringValue(userRole, "roleType", null))
                .scope("Organization") // Default scope
                .isCritical(getBooleanValue(userRole, "isCritical", false))
                .riskLevel(getStringValue(userRole, "riskLevel", null))
                .permissionCount(getIntValue(userRole, "permissionCount", 0))
                .assignedAt(getStringValue(userRole, "assignedAt", null))
                .assignedBy(null) // Not available in current response
                .isActive(getBooleanValue(userRole, "roleActive", true))
                .build();
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to map role")
                .detail("userRole", userRole.toString())
                .log();
            return null;
        }
    }
    
    /**
     * Calculate risk score for a user (simplified logic).
     * Factors: MFA status, last login, failed attempts, account status
     */
    private int calculateRiskScore(Map<String, Object> user, Integer daysSinceLastLogin) {
        int score = 0;
        
        // No MFA: +15 points
        if (!getBooleanValue(user, "mfaEnabled", false)) {
            score += 15;
        }
        
        // Long time since last login
        if (daysSinceLastLogin != null) {
            if (daysSinceLastLogin > 90) {
                score += 30;
            } else if (daysSinceLastLogin > 30) {
                score += 15;
            } else if (daysSinceLastLogin > 7) {
                score += 5;
            }
        }
        
        // Account locked: +25 points
        if ("LOCKED".equals(user.get("status"))) {
            score += 25;
        }
        
        // Failed login attempts
        if (user.containsKey("failedLoginAttempts")) {
            try {
                int attempts = Integer.parseInt(user.get("failedLoginAttempts").toString());
                score += Math.min(attempts * 5, 20); // Max 20 points
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return Math.min(score, 100); // Cap at 100
    }
    
    /**
     * Build team response with statistics.
     */
    private TeamResponse buildTeamResponse(List<TeamMemberDTO> teamMembers) {
        int totalCount = teamMembers.size();
        long directReportCount = teamMembers.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsDirectReport()))
            .count();
        long indirectReportCount = totalCount - directReportCount;
        long activeCount = teamMembers.stream()
            .filter(m -> "ACTIVE".equals(m.getStatus()))
            .count();
        long inactiveCount = totalCount - activeCount;
        double averageRiskScore = teamMembers.stream()
            .mapToInt(m -> m.getRiskScore() != null ? m.getRiskScore() : 0)
            .average()
            .orElse(0.0);
        
        return TeamResponse.builder()
            .teamMembers(teamMembers)
            .totalCount(totalCount)
            .directReportCount((int) directReportCount)
            .indirectReportCount((int) indirectReportCount)
            .activeCount((int) activeCount)
            .inactiveCount((int) inactiveCount)
            .averageRiskScore(averageRiskScore)
            .success(true)
            .message("Team members retrieved successfully")
            .build();
    }
    
    /**
     * Build empty response when no team members found.
     */
    private TeamResponse buildEmptyResponse(String message) {
        return TeamResponse.builder()
            .teamMembers(List.of())
            .totalCount(0)
            .directReportCount(0)
            .indirectReportCount(0)
            .activeCount(0)
            .inactiveCount(0)
            .averageRiskScore(0.0)
            .success(true)
            .message(message)
            .build();
    }
    
    // Helper methods
    
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
    
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
