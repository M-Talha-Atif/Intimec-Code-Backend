package com.aidentx.iam.access_roles_management.model.entity;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditLogConstants;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistent audit trail for all access role governance operations.
 *
 * <p>Captures role lifecycle actions and relationship sync operations
 * with append-only before/after state snapshots. This table complements
 * the Kafka audit pipeline by providing a tenant-scoped, queryable audit
 * history that can be served directly to the governance UI.</p>
 *
 * <p>Tracked actions include:</p>
 * <ul>
 *   <li>ROLE_CREATED</li>
 *   <li>ROLE_UPDATED</li>
 *   <li>ROLE_DELETED</li>
 *   <li>ENTITLEMENTS_UPDATED</li>
 *   <li>CHILDREN_UPDATED</li>
 * </ul>
 */
@Entity
@Table(name = AccessRoleAuditLogConstants.TABLE_ACCESS_ROLE_AUDIT_LOG, indexes = {
        @Index(name = AccessRoleAuditLogConstants.INDEX_TENANT_ROLE_CREATED,
                columnList = "tenant_id, role_id, created_at DESC"),
        @Index(name = AccessRoleAuditLogConstants.INDEX_TENANT_RULE_CREATED,
                columnList = "tenant_id, rule_id, created_at DESC"),
        @Index(name = AccessRoleAuditLogConstants.INDEX_TENANT_ACTION_CREATED,
                columnList = "tenant_id, action, created_at DESC"),
        @Index(name = AccessRoleAuditLogConstants.INDEX_TENANT_ACTOR_CREATED,
                columnList = "tenant_id, actor_username, created_at DESC"),
        @Index(name = AccessRoleAuditLogConstants.INDEX_TENANT_ROLETYPE_CREATED,
                columnList = "tenant_id, role_type, created_at DESC")
})
public class AccessRoleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // ===================================================================
    //  Assignment Rule Context
    // ===================================================================

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "rule_name")
    private String ruleName;

    // ===================================================================
    //  Role Context
    // ===================================================================

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "role_name")
    private String roleName;

    /**
     * Raw role subtype stored in the access role table.
     * Expected values include business, technical, functional, application.
     */
    @Column(name = "role_type")
    private String roleType;

    /**
     * User-friendly entity type shown to the UI.
     * Example: Business Role, Technical Role.
     */
    @Column(name = "entity_type")
    private String entityType;

    /**
     * User-friendly activity label shown to the UI.
     * Example: Business Role Updated, Technical Role Deleted.
     */
    @Column(name = "activity_label")
    private String activityLabel;

    // ===================================================================
    //  Action Context
    // ===================================================================

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "trigger_source", length = 50)
    private String triggerSource;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ===================================================================
    //  Actor
    // ===================================================================

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_username")
    private String actorUsername;

    // ===================================================================
    //  State Diff
    // ===================================================================

    @Type(JsonBinaryType.class)
    @Column(name = "state_before", columnDefinition = "jsonb")
    private Object stateBefore;

    @Type(JsonBinaryType.class)
    @Column(name = "state_after", columnDefinition = "jsonb")
    private Object stateAfter;

    @Type(JsonBinaryType.class)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private List<String> changedFields;

    // ===================================================================
    //  Relationship Context
    // ===================================================================

    @Type(JsonBinaryType.class)
    @Column(name = "added_children", columnDefinition = "jsonb")
    private Object addedChildren;

    @Type(JsonBinaryType.class)
    @Column(name = "removed_children", columnDefinition = "jsonb")
    private Object removedChildren;

    @Type(JsonBinaryType.class)
    @Column(name = "failed_children", columnDefinition = "jsonb")
    private Object failedChildren;

    @Type(JsonBinaryType.class)
    @Column(name = "added_entitlements", columnDefinition = "jsonb")
    private Object addedEntitlements;

    @Type(JsonBinaryType.class)
    @Column(name = "removed_entitlements", columnDefinition = "jsonb")
    private Object removedEntitlements;

    // ===================================================================
    //  Timestamp
    // ===================================================================

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now().toEpochMilli();
    }

    // ===================================================================
    //  Getters and Setters
    // ===================================================================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getActivityLabel() {
        return activityLabel;
    }

    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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