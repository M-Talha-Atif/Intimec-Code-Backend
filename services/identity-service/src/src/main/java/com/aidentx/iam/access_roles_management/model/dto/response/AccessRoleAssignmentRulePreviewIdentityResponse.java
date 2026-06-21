package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Identity-level preview row showing what would happen to one identity.
 */
public class AccessRoleAssignmentRulePreviewIdentityResponse {

    private UUID identityId;
    private String identityDisplayName;
    private String identityType;
    private String previewAction;
    private boolean currentlyAssigned;
    private boolean matchesRule;
    private Map<String, Object> fieldValues;
    private Map<String, Object> matchEvidence;
    private List<AccessRoleAssignmentRulePreviewEntitlementResponse> entitlements;

    public UUID getIdentityId() { return identityId; }
    public void setIdentityId(UUID identityId) { this.identityId = identityId; }

    public String getIdentityDisplayName() { return identityDisplayName; }
    public void setIdentityDisplayName(String identityDisplayName) { this.identityDisplayName = identityDisplayName; }

    public String getIdentityType() { return identityType; }
    public void setIdentityType(String identityType) { this.identityType = identityType; }

    public String getPreviewAction() { return AccessRoleAssignmentRulePreviewActionFormatter.toFriendlyLabel(previewAction); }
    public void setPreviewAction(String previewAction) { this.previewAction = previewAction; }

    public boolean isCurrentlyAssigned() { return currentlyAssigned; }
    public void setCurrentlyAssigned(boolean currentlyAssigned) { this.currentlyAssigned = currentlyAssigned; }

    public boolean isMatchesRule() { return matchesRule; }
    public void setMatchesRule(boolean matchesRule) { this.matchesRule = matchesRule; }

    public Map<String, Object> getFieldValues() { return fieldValues; }
    public void setFieldValues(Map<String, Object> fieldValues) { this.fieldValues = fieldValues; }

    public Map<String, Object> getMatchEvidence() { return matchEvidence; }
    public void setMatchEvidence(Map<String, Object> matchEvidence) { this.matchEvidence = matchEvidence; }

    public List<AccessRoleAssignmentRulePreviewEntitlementResponse> getEntitlements() { return entitlements; }
    public void setEntitlements(List<AccessRoleAssignmentRulePreviewEntitlementResponse> entitlements) { this.entitlements = entitlements; }
}

