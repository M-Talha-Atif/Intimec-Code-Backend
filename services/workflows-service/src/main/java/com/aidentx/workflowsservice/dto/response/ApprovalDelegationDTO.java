package com.aidentx.workflowsservice.dto.response;

import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Approval Delegation
 * 
 * Represents a time-based delegation rule in the system.
 * Used for API responses when querying or creating delegations.
 * 
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalDelegationDTO {

    /**
     * Unique identifier of the delegation
     */
    private UUID id;

    /**
     * Tenant this delegation belongs to
     */
    private String tenantId;

    /**
     * User who is delegating their approvals (original approver)
     */
    private UUID fromUserId;

    /**
     * Name of the user delegating (populated from user service)
     */
    private String fromUserName;

    /**
     * User who will receive the delegated approvals (delegate)
     */
    private UUID toUserId;

    /**
     * Name of the delegate (populated from user service)
     */
    private String toUserName;

    /**
     * When the delegation period starts
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startDate;

    /**
     * When the delegation period ends
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endDate;

    /**
     * Reason for the delegation
     */
    private String reason;

    /**
     * Scope of the delegation
     */
    private ApprovalDelegation.DelegationScope scope;

    /**
     * List of specific workflow IDs (if scope is SPECIFIC_WORKFLOWS)
     */
    private List<UUID> workflowIds;

    /**
     * Whether the delegate receives notifications on routed approvals
     */
    private Boolean notifyDelegate;

    /**
     * Whether the delegator receives notifications when approvals are routed
     */
    private Boolean notifyDelegator;

    /**
     * Whether the delegation is currently active
     */
    private Boolean active;

    /**
     * Whether the delegation is currently in its effective date range
     */
    private Boolean currentlyEffective;

    /**
     * When this delegation was created
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    /**
     * Who created this delegation
     */
    private UUID createdBy;

    /**
     * When this delegation was last updated
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    /**
     * Who last updated this delegation
     */
    private UUID updatedBy;

    /**
     * Factory method to convert Entity to DTO
     * 
     * @param delegation The delegation entity
     * @return The DTO representation
     */
    public static ApprovalDelegationDTO from(ApprovalDelegation delegation) {
        if (delegation == null) {
            return null;
        }

        return ApprovalDelegationDTO.builder()
                .id(delegation.getId())
                .tenantId(delegation.getTenantId())
                .fromUserId(delegation.getFromUserId())
                .toUserId(delegation.getToUserId())
                .startDate(delegation.getStartDate())
                .endDate(delegation.getEndDate())
                .reason(delegation.getReason())
                .scope(delegation.getScope())
                .workflowIds(parseWorkflowIds(delegation.getWorkflowIds()))
                .notifyDelegate(delegation.getNotifyDelegate())
                .notifyDelegator(delegation.getNotifyDelegator())
                .active(delegation.getActive())
                .currentlyEffective(delegation.isEffective())
                .createdAt(delegation.getCreatedAt())
                .createdBy(delegation.getCreatedBy())
                .updatedAt(delegation.getUpdatedAt())
                .updatedBy(delegation.getUpdatedBy())
                .build();
    }

    /**
     * Factory method with user name enrichment
     * 
     * @param delegation The delegation entity
     * @param fromUserName Name of the delegator
     * @param toUserName Name of the delegate
     * @return The DTO representation with names
     */
    public static ApprovalDelegationDTO from(ApprovalDelegation delegation, String fromUserName, String toUserName) {
        ApprovalDelegationDTO dto = from(delegation);
        if (dto != null) {
            dto.setFromUserName(fromUserName);
            dto.setToUserName(toUserName);
        }
        return dto;
    }

    /**
     * Convert list of entities to list of DTOs
     * 
     * @param delegations List of delegation entities
     * @return List of DTOs
     */
    public static List<ApprovalDelegationDTO> fromList(List<ApprovalDelegation> delegations) {
        if (delegations == null || delegations.isEmpty()) {
            return Collections.emptyList();
        }
        return delegations.stream()
                .map(ApprovalDelegationDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * Parse comma-separated workflow IDs string into list of UUIDs
     */
    private static List<UUID> parseWorkflowIds(String workflowIdsStr) {
        if (workflowIdsStr == null || workflowIdsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(workflowIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}
