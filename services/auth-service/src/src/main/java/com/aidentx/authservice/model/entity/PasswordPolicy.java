package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Entity representing password policy configuration for a tenant.
 * <p>
 * This class maps to the <code>password_policies</code> table and contains all password
 * requirements and restrictions that must be enforced for users within a specific tenant.
 * It includes settings for password complexity, history tracking, and validity periods.
 * </p>
 *
 * <p>Typical usage involves retrieving policy settings during password creation, validation,
 * and update operations to ensure compliance with organizational security requirements.</p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Entity
@Table(name = "password_policies")
public class PasswordPolicy {

    /**
     * Unique identifier for the password policy (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Minimum required length for passwords.
     */
    @Column(name = "min_password_length", nullable = false)
    private Integer minPasswordLength;

    /**
     * Whether passwords must contain at least one lowercase letter.
     */
    @Column(name = "require_lowercase", nullable = false)
    private Boolean requireLowercase = true;

    /**
     * Whether passwords must contain at least one uppercase letter.
     */
    @Column(name = "require_uppercase", nullable = false)
    private Boolean requireUppercase = true;

    /**
     * Whether passwords must contain at least one numeric digit.
     */
    @Column(name = "require_number", nullable = false)
    private Boolean requireDigits = true;

    /**
     * Whether passwords must contain at least one special symbol.
     */
    @Column(name = "require_symbol", nullable = false)
    private Boolean requireSpecialChars = false;

    /**
     * Whether passwords cannot contain the username or email address.
     */
    @Column(name = "disallow_username_or_email", nullable = false)
    private Boolean disallowUsernameOrEmail = true;

    /**
     * List of banned substrings that cannot appear in passwords.
     * Stored as JSON array in the database.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "banned_substrings", columnDefinition = "jsonb")
    private List<String> bannedSubstrings;

    /**
     * Number of previous passwords to remember to prevent reuse.
     */
    @Column(name = "password_history_count", nullable = false)
    private Integer passwordHistoryCount;

    /**
     * Maximum number of days a password remains valid before requiring change.
     * Null means no expiration.
     */
    @Column(name = "max_password_validity_days")
    private Integer maxPasswordValidityDays;

    /**
     * Number of days before password expiration to start notifying the user.
     * Null means no notification.
     */
    @Column(name = "password_expiration_notification_days")
    private Integer passwordExpirationNotificationDays;

    /**
     * Epoch timestamp when this password policy expires.
     * Calculated as current time + (maxPasswordValidityDays * 24 * 60 * 60 * 1000).
     * When current time > this value, passwords are considered expired.
     */
    @Column(name = "policy_expires_at")
    private Long policyExpiresAt;

    /**
     * Flag to track if expiration notification emails have been sent for the current period.
     * Set to true when emails are sent, reset to false when policyExpiresAt is updated.
     */
    @Column(name = "is_email_sent", nullable = false)
    private Boolean isEmailSent = false;

    /**
     * Tenant ID this policy belongs to.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Timestamp when this policy was created (epoch time).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    /**
     * Timestamp when this policy was last updated (epoch time).
     */
    @Column(name = "updated_at")
    private Long updatedAt;

    /**
     * Default constructor required by JPA.
     */
    public PasswordPolicy() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Constructor with tenant ID.
     *
     * @param tenantId the tenant ID this policy belongs to
     */
    public PasswordPolicy(String tenantId) {
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

    public Integer getMinPasswordLength() {
        return minPasswordLength;
    }

    public void setMinPasswordLength(Integer minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }

    public Boolean getRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(Boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public Boolean getRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(Boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public Boolean getRequireDigits() {
        return requireDigits;
    }

    public void setRequireDigits(Boolean requireDigits) {
        this.requireDigits = requireDigits;
    }

    public Boolean getRequireSpecialChars() {
        return requireSpecialChars;
    }

    public void setRequireSpecialChars(Boolean requireSpecialChars) {
        this.requireSpecialChars = requireSpecialChars;
    }

    public Boolean getDisallowUsernameOrEmail() {
        return disallowUsernameOrEmail;
    }

    public void setDisallowUsernameOrEmail(Boolean disallowUsernameOrEmail) {
        this.disallowUsernameOrEmail = disallowUsernameOrEmail;
    }

    public List<String> getBannedSubstrings() {
        return bannedSubstrings;
    }

    public void setBannedSubstrings(List<String> bannedSubstrings) {
        this.bannedSubstrings = bannedSubstrings;
    }

    public Integer getPasswordHistoryCount() {
        return passwordHistoryCount;
    }

    public void setPasswordHistoryCount(Integer passwordHistoryCount) {
        this.passwordHistoryCount = passwordHistoryCount;
    }

    public Integer getMaxPasswordValidityDays() {
        return maxPasswordValidityDays;
    }

    public void setMaxPasswordValidityDays(Integer maxPasswordValidityDays) {
        this.maxPasswordValidityDays = maxPasswordValidityDays;
    }

    public Integer getPasswordExpirationNotificationDays() {
        return passwordExpirationNotificationDays;
    }

    public void setPasswordExpirationNotificationDays(Integer passwordExpirationNotificationDays) {
        this.passwordExpirationNotificationDays = passwordExpirationNotificationDays;
    }

    public Long getPolicyExpiresAt() {
        return policyExpiresAt;
    }

    public void setPolicyExpiresAt(Long policyExpiresAt) {
        this.policyExpiresAt = policyExpiresAt;
        // Reset email sent flag when policy expiration is updated (new period starts)
        this.isEmailSent = false;
    }

    public Boolean getIsEmailSent() {
        return isEmailSent;
    }

    public void setIsEmailSent(Boolean isEmailSent) {
        this.isEmailSent = isEmailSent;
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
        return "PasswordPolicy{" +
                "id=" + id +
                ", minPasswordLength=" + minPasswordLength +
                ", requireLowercase=" + requireLowercase +
                ", requireUppercase=" + requireUppercase +
                ", requireDigits=" + requireDigits +
                ", requireSpecialChars=" + requireSpecialChars +
                ", disallowUsernameOrEmail=" + disallowUsernameOrEmail +
                ", bannedSubstrings=" + bannedSubstrings +
                ", passwordHistoryCount=" + passwordHistoryCount +
                ", maxPasswordValidityDays=" + maxPasswordValidityDays +
                ", passwordExpirationNotificationDays=" + passwordExpirationNotificationDays +
                ", policyExpiresAt=" + policyExpiresAt +
                ", isEmailSent=" + isEmailSent +
                ", tenantId='" + tenantId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
