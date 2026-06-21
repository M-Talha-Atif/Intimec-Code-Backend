package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for WorkflowApproval data received from workflows-service.
 * Mirrors the structure of workflows-service's WorkflowApprovalDTO.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowApprovalDTO {
    
    private UUID id;
    private UUID executionId;
    private UUID nodeExecutionId;
    private UUID approverId;
    private String approverType;
    private String decision;
    
    private OffsetDateTime requestedAt;
    private OffsetDateTime respondedAt;
    private OffsetDateTime timeoutAt;
    
    private String comments;
    
    private Map<String, Object> approvalContext;
    private Map<String, Object> evidenceBundle;
    
    private UUID delegatedTo;
    private List<DelegationRecordDTO> delegationChain;
    private UUID approvalGroupId;
    private String approvalPolicy;
    private Integer requiredApprovals;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DelegationRecordDTO {
        private UUID fromApproverId;
        private UUID toApproverId;
        private OffsetDateTime delegatedAt;
        private String reason;
        private Integer sequenceNumber;
    }
}
