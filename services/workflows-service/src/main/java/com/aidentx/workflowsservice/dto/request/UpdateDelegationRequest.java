package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an existing delegation
 * 
 * All fields are optional - only provided fields will be updated.
 * Cannot change the fromUserId or toUserId after creation.
 * 
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDelegationRequest {

    /**
     * New start date for the delegation
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startDate;

    /**
     * New end date for the delegation
     */
    @Future(message = "End date must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endDate;

    /**
     * Updated reason for the delegation
     */
    private String reason;

    /**
     * Updated scope of the delegation
     */
    private ApprovalDelegation.DelegationScope scope;

    /**
     * Updated list of specific workflow IDs
     */
    private List<UUID> workflowIds;

    /**
     * Updated notification preference for delegate
     */
    private Boolean notifyDelegate;

    /**
     * Updated notification preference for delegator
     */
    private Boolean notifyDelegator;

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
