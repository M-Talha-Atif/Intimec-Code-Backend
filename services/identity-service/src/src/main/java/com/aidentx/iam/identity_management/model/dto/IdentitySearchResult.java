package com.aidentx.iam.identity_management.model.dto;

/**
 * Simplified identity information for search results.
 * Contains just enough information for task assignment and user reference.
 */
public class IdentitySearchResult {

    /**
     * UUID of the identity
     */
    private String id;

    /**
     * Display name (for HumanIdentity: displayName or firstName + lastName)
     */
    private String name;

    /**
     * Type of identity: EMPLOYEE, NON_EMPLOYEE, NON_HUMAN
     */
    private String type;

    /**
     * Email address (if available, for HumanIdentity)
     */
    private String email;

    /**
     * Username (if available, for HumanIdentity)
     */
    private String username;

    /**
     * Whether the identity is active
     */
    private boolean isActive;

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Creation timestamp (epoch milliseconds)
     */
    private long createdAt;

    // Constructors
    public IdentitySearchResult() {
    }

    public IdentitySearchResult(String id, String name, String type, String email, String username,
            boolean isActive, String tenantId, long createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.email = email;
        this.username = username;
        this.isActive = isActive;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
