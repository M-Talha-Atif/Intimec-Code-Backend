package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing account lockout policy configuration for a tenant.
 * <p>
 * This class maps to the <code>account_lock_policies</code> table and contains all account
 * lockout settings that control when user accounts should be temporarily locked due to
 * failed authentication attempts. It includes settings for attempt thresholds, lockout
 * duration, and progressive delay mechanisms.
 * </p>
 *
 * <p>Typical usage involves retrieving policy settings during authentication attempts
 * to determine if an account should be locked and for how long.</p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Entity
@Table(name = "account_lock_policies")
public class AccountLockPolicy {

    /**
     * Unique identifier for the account lock policy (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Maximum number of failed login attempts before account is locked.
     */
    @Column(name = "max_failed_login_attempts", nullable = false)
    private Integer maxFailedLoginAttempts;

    /**
     * Duration in seconds for which the account remains locked.
     */
    @Column(name = "lockout_duration_seconds", nullable = false)
    private Integer lockoutDurationSeconds;

    /**
     * Delay in seconds to increase lockout duration after each subsequent failure.
     * Set to 0 to disable progressive delay, or specify seconds to add per failure.
     */
    @Column(name = "increase_delay_after_failure_seconds", nullable = false)
    private Integer increaseDelayAfterFailureSeconds = 0;

    /**
     * Tenant ID this policy belongs to.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * The timestamp when this policy was created (epoch time in milliseconds).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt; // Epoch time in milliseconds

    /**
     * The timestamp when this policy was last updated (epoch time in milliseconds).
     */
    @Column(name = "updated_at")
    private Long updatedAt; // Epoch time in milliseconds

    /**
     * Default constructor required by JPA.
     */
    public AccountLockPolicy() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Constructor with tenant ID.
     *
     * @param tenantId the tenant ID this policy belongs to
     */
    public AccountLockPolicy(String tenantId) {
        this();
        this.tenantId = tenantId;
    }

    /**
     * Lifecycle callback to update the updatedAt timestamp before updates.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
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

    public Integer getLockoutDurationSeconds() {
        return lockoutDurationSeconds;
    }

    public void setLockoutDurationSeconds(Integer lockoutDurationSeconds) {
        this.lockoutDurationSeconds = lockoutDurationSeconds;
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
        return "AccountLockPolicy{" +
                "id=" + id +
                ", maxFailedLoginAttempts=" + maxFailedLoginAttempts +
                ", lockoutDurationSeconds=" + lockoutDurationSeconds +
                ", increaseDelayAfterFailureSeconds=" + increaseDelayAfterFailureSeconds +
                ", tenantId='" + tenantId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
