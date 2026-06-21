package com.aidentx.iam.entitlement_management.model.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for rule execution history (run-level).
 */
public class ExecutionHistoryResponse {

    private UUID id;
    private String tenantId;
    private UUID ruleId;
    private String ruleName;
    private UUID entitlementId;
    private String entitlementDisplayName;
    private UUID resourceId;
    private String triggerType;
    private String triggeredBy;
    private List<Map<String, Object>> conditionsSnapshot;
    private List<String> identityTypes;
    private String status;
    private int identitiesMatched;
    private int assignmentsCreated;
    private int assignmentsRevoked;
    private int skippedNoShadow;
    private int skippedAlreadyAssigned;
    private int errorCount;
    private String errorMessage;
    private Long startedAt;
    private Long completedAt;
    private Long durationMs;

    // --- Getters and Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public String getEntitlementDisplayName() { return entitlementDisplayName; }
    public void setEntitlementDisplayName(String entitlementDisplayName) { this.entitlementDisplayName = entitlementDisplayName; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public List<Map<String, Object>> getConditionsSnapshot() { return conditionsSnapshot; }
    public void setConditionsSnapshot(List<Map<String, Object>> conditionsSnapshot) { this.conditionsSnapshot = conditionsSnapshot; }

    public List<String> getIdentityTypes() { return identityTypes; }
    public void setIdentityTypes(List<String> identityTypes) { this.identityTypes = identityTypes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getIdentitiesMatched() { return identitiesMatched; }
    public void setIdentitiesMatched(int identitiesMatched) { this.identitiesMatched = identitiesMatched; }

    public int getAssignmentsCreated() { return assignmentsCreated; }
    public void setAssignmentsCreated(int assignmentsCreated) { this.assignmentsCreated = assignmentsCreated; }

    public int getAssignmentsRevoked() { return assignmentsRevoked; }
    public void setAssignmentsRevoked(int assignmentsRevoked) { this.assignmentsRevoked = assignmentsRevoked; }

    public int getSkippedNoShadow() { return skippedNoShadow; }
    public void setSkippedNoShadow(int skippedNoShadow) { this.skippedNoShadow = skippedNoShadow; }

    public int getSkippedAlreadyAssigned() { return skippedAlreadyAssigned; }
    public void setSkippedAlreadyAssigned(int skippedAlreadyAssigned) { this.skippedAlreadyAssigned = skippedAlreadyAssigned; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}