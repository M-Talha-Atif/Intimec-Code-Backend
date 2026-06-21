package com.aidentx.iam.entitlement_management.model.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for rule execution detail (identity-level).
 */
public class ExecutionDetailResponse {

    private UUID id;
    private String tenantId;
    private UUID executionId;
    private UUID ruleId;
    private UUID identityId;
    private String identityDisplayName;
    private String outcome;
    private UUID shadowId;
    private Map<String, Object> matchEvidence;
    private String errorMessage;
    private Long processedAt;

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public String getIdentityDisplayName() { return identityDisplayName; }
    public void setIdentityDisplayName(String identityDisplayName) { this.identityDisplayName = identityDisplayName; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public UUID getShadowId() { return shadowId; }
    public void setShadowId(UUID shadowId) { this.shadowId = shadowId; }

    public Map<String, Object> getMatchEvidence() { return matchEvidence; }
    public void setMatchEvidence(Map<String, Object> matchEvidence) { this.matchEvidence = matchEvidence; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }
}