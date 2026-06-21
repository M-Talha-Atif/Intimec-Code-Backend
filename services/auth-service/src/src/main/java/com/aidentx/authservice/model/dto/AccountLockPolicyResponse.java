package com.aidentx.authservice.model.dto;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) for account lock policy responses.
 * <p>
 * This class is used for returning account lock policy data in REST API responses.
 * Contains all account lockout policy fields and metadata.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class AccountLockPolicyResponse {

    /**
     * Unique identifier for the account lock policy.
     */
    private UUID id;

    /**
     * Maximum number of failed login attempts before account is locked.
     */
    private Integer maxFailedLoginAttempts;

    /**
     * Duration in minutes for which the account remains locked.
     * This is converted from seconds stored in the database.
     */
    private Integer lockoutDurationMinutes;

    /**
     * Delay in seconds to increase lockout duration after each subsequent failure.
     * Set to 0 to disable progressive delay, or specify seconds to add per failure.
     */
    private Integer increaseDelayAfterFailureSeconds;

    /**
     * Tenant ID this policy belongs to.
     */
    private String tenantId;

    /**
     * The timestamp when this policy was created (epoch time in milliseconds).
     */
    private Long createdAt;

    /**
     * The timestamp when this policy was last updated (epoch time in milliseconds).
     */
    private Long updatedAt;

    /**
     * Default constructor.
     */
    public AccountLockPolicyResponse() {
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public void setMaxFailedLoginAttempts(Integer maxFailedLoginAttempts) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    public Integer getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(Integer lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public Integer getIncreaseDelayAfterFailureSeconds() {
        return increaseDelayAfterFailureSeconds;
    }

    public void setIncreaseDelayAfterFailureSeconds(Integer increaseDelayAfterFailureSeconds) {
        this.increaseDelayAfterFailureSeconds = increaseDelayAfterFailureSeconds;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    @Override
    public String toString() {
        return "AccountLockPolicyResponse{" +
                "id=" + id +
                ", maxFailedLoginAttempts=" + maxFailedLoginAttempts +
                ", lockoutDurationMinutes=" + lockoutDurationMinutes +
                ", increaseDelayAfterFailureSeconds=" + increaseDelayAfterFailureSeconds +
                ", tenantId='" + tenantId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
