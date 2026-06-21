package com.aidentx.workflowsservice.dto.response;

import com.aidentx.workflowsservice.entity.WorkflowApproval;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Workflow Approval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowApprovalDTO {

    private UUID id;
    private UUID executionId;
    private UUID nodeExecutionId;
    
    // Multi-approval tracking
    private UUID approvalGroupId;
    private String approvalPolicy; // ALL, ANY, MAJORITY, THRESHOLD
    private Integer requiredApprovals;

    private UUID approverId;
    private String approverType; // MANAGER, APP_OWNER, etc.

    private String decision; // PENDING, APPROVED, REJECTED
    private String comments;
    private UUID delegatedTo;
    private List<DelegationRecordDTO> delegationChain;

    private OffsetDateTime requestedAt;
    private OffsetDateTime respondedAt;
    private OffsetDateTime timeoutAt;

    private Map<String, Object> approvalContext;
    private Map<String, Object> evidenceBundle;

    /**
     * Factory method to convert Entity to DTO
     */
    public static WorkflowApprovalDTO from(WorkflowApproval approval) {
        if (approval == null) return null;

        return WorkflowApprovalDTO.builder()
                .id(approval.getId())
                .executionId(approval.getExecutionId())
                .nodeExecutionId(approval.getNodeExecutionId())
                .approvalGroupId(approval.getApprovalGroupId())
                .approvalPolicy(approval.getApprovalPolicy() != null ? approval.getApprovalPolicy().name() : null)
                .requiredApprovals(approval.getRequiredApprovals())
                .approverId(approval.getApproverId())
                .approverType(approval.getApproverType() != null ? approval.getApproverType().name() : null)
                .decision(approval.getDecision() != null ? approval.getDecision().name() : null)
                .comments(approval.getComments())
                .delegatedTo(approval.getDelegatedTo())
                .delegationChain(
                    approval.getDelegationChain() != null && !approval.getDelegationChain().isEmpty()
                        ? approval.getDelegationChain().stream()
                            .map(DelegationRecordDTO::from)
                            .collect(Collectors.toList())
                        : null
                )
                .requestedAt(approval.getRequestedAt())
                .respondedAt(approval.getRespondedAt())
                .timeoutAt(approval.getTimeoutAt())
                .approvalContext(approval.getApprovalContext())
                .evidenceBundle(approval.getEvidenceBundle())
                .build();
    }

    /**
     * DTO for delegation record in the delegation chain
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DelegationRecordDTO {
        private UUID fromApproverId;
        private UUID toApproverId;
        private OffsetDateTime delegatedAt;
        private String reason;
        private Integer sequenceNumber;

        public static DelegationRecordDTO from(WorkflowApproval.DelegationRecord record) {
            if (record == null) return null;

            return DelegationRecordDTO.builder()
                    .fromApproverId(record.getFromApproverId())
                    .toApproverId(record.getToApproverId())
                    .delegatedAt(record.getDelegatedAt())
                    .reason(record.getReason())
                    .sequenceNumber(record.getSequenceNumber())
                    .build();
        }
    }
}
