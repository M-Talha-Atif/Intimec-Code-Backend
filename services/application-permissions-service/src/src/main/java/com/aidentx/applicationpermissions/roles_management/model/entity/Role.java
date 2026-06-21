package com.aidentx.applicationpermissions.roles_management.model.entity;

import com.aidentx.applicationpermissions.roles_management.enums.RiskLevel;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a Role in the role-based access control system.
 * 
 * <p>A Role defines a set of permissions that can be assigned to users.
 * It consists of a role type (hierarchical level), a descriptive name,
 * and is scoped to a specific tenant for multi-tenancy support.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "role")
public class Role {
    
    /**
     * Unique identifier for the role.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    /**
     * The tenant identifier that owns this role.
     * Used for multi-tenancy isolation and security.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    /**
     * The hierarchical type of this role (ADMINISTRATOR, REVIEWER, OPERATOR, VIEWER).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType;
    
    /**
     * The human-readable name of the role.
     * Maximum length: 128 characters.
     */
    @Column(name = "role_name", nullable = false, length = 128)
    private String roleName;
    
    /**
     * Optional description explaining the purpose and scope of this role.
     */
    @Column(name = "description")
    private String description;

    /**
     * Business justification for creating this role.
     * Explains why this role is needed and what business purpose it serves.
     */
    @Column(name = "business_justification")
    private String businessJustification;

    /**
     * Flag indicating whether owner approval is required for this role assignment.
     */
    @Column(name = "require_owner_approval", nullable = false)
    private Boolean requireOwnerApproval = false;

    /**
     * Tags associated with this role for categorization and filtering.
     * Stored as a collection of strings.
     */
    @ElementCollection
    @CollectionTable(name = "role_tags", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "tag")
    private List<String> tags;

    /**
     * Risk level associated with this role (LOW, MEDIUM, HIGH).
     * Used to categorize roles based on their potential security impact.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel = RiskLevel.Low;

    /**
     * Flag indicating whether this role is active.
     * Inactive roles cannot be assigned to users.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Timestamp when this role was created (epoch time in milliseconds).
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    /**
     * Timestamp when this role was last updated (epoch time in milliseconds).
     */
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    /**
     * Name of the user who created this role.
     * Extracted from JWT token during role creation.
     */
    @Column(name = "created_by_name", nullable = false)
    private String createdByName;

    /**
     * ID of the user who created this role.
     * Extracted from JWT token during role creation.
     */
    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    /**
     * Name of the user who last updated this role.
     * Extracted from JWT token during role update.
     */
    @Column(name = "updated_by_name")
    private String updatedByName;

    /**
     * ID of the user who last updated this role.
     * Extracted from JWT token during role update.
     */
    @Column(name = "updated_by_id")
    private UUID updatedById;

    /**
     * Default constructor for JPA.
     */
    public Role() {}

    /**
     * Constructor to create a new Role with specified values.
     * 
     * @param tenantId the tenant identifier
     * @param roleType the hierarchical role type
     * @param roleName the human-readable name of the role
     * @param description optional description of the role
     * @param businessJustification business justification for the role
     * @param requireOwnerApproval whether owner approval is required
     * @param tags tags associated with the role
     * @param riskLevel the risk level of the role
     * @param isActive whether the role is active
     * @param createdByName name of the user who created this role
     * @param createdById ID of the user who created this role
     */
    public Role(String tenantId, RoleType roleType, String roleName, String description, 
                String businessJustification, Boolean requireOwnerApproval, List<String> tags,
                RiskLevel riskLevel, Boolean isActive, String createdByName, UUID createdById) {
        this.tenantId = tenantId;
        this.roleType = roleType;
        this.roleName = roleName;
        this.description = description;
        this.businessJustification = businessJustification;
        this.requireOwnerApproval = requireOwnerApproval != null ? requireOwnerApproval : false;
        this.tags = tags;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.Low;
        this.isActive = isActive != null ? isActive : true;
        this.createdByName = createdByName;
        this.createdById = createdById;
        
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
    }

    // Getters and Setters
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

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }

    public Boolean getRequireOwnerApproval() {
        return requireOwnerApproval;
    }

    public void setRequireOwnerApproval(Boolean requireOwnerApproval) {
        this.requireOwnerApproval = requireOwnerApproval != null ? requireOwnerApproval : false;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.Low;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public UUID getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(UUID updatedById) {
        this.updatedById = updatedById;
    }
}