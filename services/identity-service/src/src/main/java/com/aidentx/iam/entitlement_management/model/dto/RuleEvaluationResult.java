package com.aidentx.iam.entitlement_management.model.dto;

import java.util.UUID;

/**
 * Result of evaluating a single entitlement assignment rule.
 *
 * <p>Contains counts of matched identities, created/revoked assignments,
 * and error information if the evaluation failed.
 *
 * <p>Used by both single-rule and bulk evaluation endpoints to report
 * what happened during rule processing.
 */
public class RuleEvaluationResult {

    private UUID ruleId;
    private String ruleName;
    private int identitiesMatched;
    private int assignmentsCreated;
    private int assignmentsRevoked;
    private int skippedNoShadow;
    private boolean success = true;
    private String errorMessage;
    private long durationMs;

    // --- Getters and Setters ---

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public int getIdentitiesMatched() { return identitiesMatched; }
    public void setIdentitiesMatched(int identitiesMatched) { this.identitiesMatched = identitiesMatched; }

    public int getAssignmentsCreated() { return assignmentsCreated; }
    public void setAssignmentsCreated(int assignmentsCreated) { this.assignmentsCreated = assignmentsCreated; }

    public int getAssignmentsRevoked() { return assignmentsRevoked; }
    public void setAssignmentsRevoked(int assignmentsRevoked) { this.assignmentsRevoked = assignmentsRevoked; }

    public int getSkippedNoShadow() { return skippedNoShadow; }
    public void setSkippedNoShadow(int skippedNoShadow) { this.skippedNoShadow = skippedNoShadow; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    /**
     * Factory method to create a failed result.
     */
    public static RuleEvaluationResult failed(UUID ruleId, String ruleName, String error, long durationMs) {
        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setRuleId(ruleId);
        result.setRuleName(ruleName);
        result.setSuccess(false);
        result.setErrorMessage(error);
        result.setDurationMs(durationMs);
        return result;
    }
}