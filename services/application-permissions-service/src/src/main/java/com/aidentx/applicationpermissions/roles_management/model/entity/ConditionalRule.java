package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing a Conditional Rule that restricts permission access.
 * 
 * <p>Conditional Rules provide fine-grained access control by adding conditions
 * that must be satisfied for a permission to be granted. These rules can reference
 * organizational hierarchy, user attributes, or other contextual information.</p>
 * 
 * <p>Example conditions:</p>
 * <ul>
 *   <li>"user.department = 'Finance' OR user.globalOrg = 'HQ'"</li>
 *   <li>"user.region = 'US_EAST' AND permission.scope = 'regional'"</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "conditional_rule")
public class ConditionalRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
    
    @Column(name = "condition_script", nullable = true, columnDefinition = "TEXT")
    private String conditionScript;
    
    @Column(name = "justification",nullable = false ,columnDefinition = "TEXT")
    private String justification;

    // Constructors
    public ConditionalRule() {}

    public ConditionalRule(UUID permissionId, String conditionScript, String justification) {
        this.permissionId = permissionId;
        this.conditionScript = conditionScript;
        this.justification = justification;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }

    public String getConditionScript() {
        return conditionScript;
    }

    public void setConditionScript(String conditionScript) {
        this.conditionScript = conditionScript;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}