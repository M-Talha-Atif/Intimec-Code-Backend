package com.aidentx.workflowsservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for approving or rejecting workflow approvals
 * Decision is determined by the endpoint (approve vs reject)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRejectRequest {

    /**
     * Optional comment explaining the decision
     */
    private String comments;
    
    /**
     * Optional evidence/documentation supporting the decision
     */
    private Map<String, Object> evidence;
}
