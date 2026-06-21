package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a time-based approval delegation
 * 
 * This allows a user to delegate their approval responsibilities to another user
 * for a specified time period. The delegation can be scoped to all workflows,
 * specific workflow types, or specific workflow instances.
 * 
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDelegationRequest {

    /**
     * The user ID who will receive delegated approval requests
     * Required: The delegate must be a valid user in the same tenant
     */
    @NotNull(message = "Delegate user ID is required")
    private UUID toUserId;

    /**
     * When the delegation period starts
     * Must be in the future or current time
     */
    @NotNull(message = "Start date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startDate;

    /**
     * When the delegation period ends
     * Must be after the start date
     */
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endDate;

    /**
     * Reason for the delegation (e.g., "Vacation", "Medical leave", "Conference attendance")
     */
    private String reason;

    /**
     * Scope of the delegation
     * - ALL: Delegate all approval requests
     * - WORKFLOW: Delegate workflow-type-based approvals
     * - SPECIFIC_WORKFLOWS: Delegate only for specific workflow IDs
     * 
     * Default: ALL
     */
    @Builder.Default
    private ApprovalDelegation.DelegationScope scope = ApprovalDelegation.DelegationScope.ALL;

    /**
     * List of specific workflow IDs for SPECIFIC_WORKFLOWS scope
     * Only applicable when scope is SPECIFIC_WORKFLOWS
     */
    private List<UUID> workflowIds;

    /**
     * Whether to notify the delegate when approvals are routed to them
     * Default: true
     */
    @Builder.Default
    private Boolean notifyDelegate = true;

    /**
     * Whether to notify the delegator when their approvals are routed to the delegate
     * Default: false
     */
    @Builder.Default
    private Boolean notifyDelegator = false;

    /**
     * Convert workflow ID list to comma-separated string for storage
     */
    public String getWorkflowIdsAsString() {
        if (workflowIds == null || workflowIds.isEmpty()) {
            return null;
        }
        return workflowIds.stream()
                .map(UUID::toString)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }
}
