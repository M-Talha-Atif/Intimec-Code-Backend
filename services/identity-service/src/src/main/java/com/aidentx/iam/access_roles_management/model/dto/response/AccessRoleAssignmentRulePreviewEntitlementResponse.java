package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.UUID;

/**
 * Entitlement-level row included in access-role rule preview responses.
 */
public class AccessRoleAssignmentRulePreviewEntitlementResponse {

    private UUID entitlementId;
    private String displayName;
    private UUID resourceId;
    private String sourceUid;
    private String objectClass;
    private String kind;
    private String riskLevel;
    private String sourceRoleName;
    private UUID sourceRoleId;
    private boolean direct;
    private int depth;
    private String previewAction;

    public String getSourceRoleName() {
        return sourceRoleName;
    }

    public void setSourceRoleName(String sourceRoleName) {
        this.sourceRoleName = sourceRoleName;
    }


    public UUID getEntitlementId() {
        return entitlementId;
    }

    public void setEntitlementId(UUID entitlementId) {
        this.entitlementId = entitlementId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getSourceUid() {
        return sourceUid;
    }

    public void setSourceUid(String sourceUid) {
        this.sourceUid = sourceUid;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public UUID getSourceRoleId() {
        return sourceRoleId;
    }

    public void setSourceRoleId(UUID sourceRoleId) {
        this.sourceRoleId = sourceRoleId;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getPreviewAction() {
        return AccessRoleAssignmentRulePreviewActionFormatter.toFriendlyLabel(previewAction);
    }

    public void setPreviewAction(String previewAction) {
        this.previewAction = previewAction;
    }
}

