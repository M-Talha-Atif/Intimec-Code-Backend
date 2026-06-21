package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for team member data in the Responsibilities dashboard.
 * Contains list of team members with aggregate statistics.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamResponse {
    
    /**
     * List of team members (direct and indirect reports)
     */
    private List<TeamMemberDTO> teamMembers;
    
    /**
     * Total count of team members
     */
    private Integer totalCount;
    
    /**
     * Count of direct reports
     */
    private Integer directReportCount;
    
    /**
     * Count of indirect reports
     */
    private Integer indirectReportCount;
    
    /**
     * Count of active team members
     */
    private Integer activeCount;
    
    /**
     * Count of inactive team members
     */
    private Integer inactiveCount;
    
    /**
     * Average risk score across all team members
     */
    private Double averageRiskScore;
    
    /**
     * Success flag
     */
    private Boolean success;
    
    /**
     * Response message
     */
    private String message;
}
