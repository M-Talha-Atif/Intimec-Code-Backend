package com.aidentx.iam.access_roles_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * UI-friendly audit history DTO for access role governance history.
 *
 * <p>This DTO is intended for direct consumption by the frontend history
 * screen. Unlike the persistence entity, it exposes user-friendly activity
 * labels and preserves structured relationship change context.</p>
 */
public class AccessRoleAuditHistoryDto {

    private UUID id;
    private UUID ruleId;
    private String ruleName;
    private UUID roleId;
    private String roleName;
    private String roleType;
    private String entityType;
    private String action;
    private String activityLabel;
    private String triggerSource;
    private String description;
    private String actorId;
    private String actorUsername;
    private Object stateBefore;
    private Object stateAfter;
    private List<String> changedFields;
    private Object addedChildren;
    private Object removedChildren;
    private Object failedChildren;
    private Object addedEntitlements;
    private Object removedEntitlements;
    private Long createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActivityLabel() {
        return activityLabel;
    }

    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public Object getStateBefore() {
        return stateBefore;
    }

    public void setStateBefore(Object stateBefore) {
        this.stateBefore = stateBefore;
    }

    public Object getStateAfter() {
        return stateAfter;
    }

    public void setStateAfter(Object stateAfter) {
        this.stateAfter = stateAfter;
    }

    public List<String> getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(List<String> changedFields) {
        this.changedFields = changedFields;
    }

    public Object getAddedChildren() {
        return addedChildren;
    }

    public void setAddedChildren(Object addedChildren) {
        this.addedChildren = addedChildren;
    }

    public Object getRemovedChildren() {
        return removedChildren;
    }

    public void setRemovedChildren(Object removedChildren) {
        this.removedChildren = removedChildren;
    }

    public Object getFailedChildren() {
        return failedChildren;
    }

    public void setFailedChildren(Object failedChildren) {
        this.failedChildren = failedChildren;
    }

    public Object getAddedEntitlements() {
        return addedEntitlements;
    }

    public void setAddedEntitlements(Object addedEntitlements) {
        this.addedEntitlements = addedEntitlements;
    }

    public Object getRemovedEntitlements() {
        return removedEntitlements;
    }

    public void setRemovedEntitlements(Object removedEntitlements) {
        this.removedEntitlements = removedEntitlements;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}

