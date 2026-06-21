package com.aidentx.authservice.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) for password policy responses.
 * <p>
 * This class is used for returning password policy data in REST API responses.
 * Contains all password policy fields and metadata.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class PasswordPolicyResponse {

    /**
     * Unique identifier for the password policy.
     */
    private UUID id;

    /**
     * Minimum password length requirement.
     */
    private Integer minPasswordLength;

    /**
     * Number of previous passwords to remember and prevent reuse.
     */
    private Integer passwordHistoryCount;

    /**
     * Whether uppercase letters are required in passwords.
     */
    private Boolean requireUppercase;

    /**
     * Whether lowercase letters are required in passwords.
     */
    private Boolean requireLowercase;

    /**
     * Whether digits are required in passwords.
     */
    private Boolean requireDigits;

    /**
     * Whether special characters are required in passwords.
     */
    private Boolean requireSpecialChars;

    /**
     * Whether to disallow username or email in passwords.
     */
    private Boolean disallowUsernameOrEmail;

    /**
     * List of banned substrings that cannot appear in passwords.
     */
    private List<String> bannedSubstrings;

    /**
     * Maximum number of days a password remains valid before expiring.
     */
    private Integer maxPasswordValidityDays;

    /**
     * Number of days before password expiration to start notifying the user.
     */
    private Integer passwordExpirationNotificationDays;

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
    public PasswordPolicyResponse() {
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

    public Integer getPasswordHistoryCount() {
        return passwordHistoryCount;
    }

    public void setPasswordHistoryCount(Integer passwordHistoryCount) {
        this.passwordHistoryCount = passwordHistoryCount;
    }

    public Boolean getRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(Boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public Boolean getRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(Boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
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

    public List<String> getBannedSubstrings() {
        return bannedSubstrings;
    }

    public void setBannedSubstrings(List<String> bannedSubstrings) {
        this.bannedSubstrings = bannedSubstrings;
    }

    public Boolean getDisallowUsernameOrEmail() {
        return disallowUsernameOrEmail;
    }

    public void setDisallowUsernameOrEmail(Boolean disallowUsernameOrEmail) {
        this.disallowUsernameOrEmail = disallowUsernameOrEmail;
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
        return "PasswordPolicyResponse{" +
                "id=" + id +
                ", minPasswordLength=" + minPasswordLength +
                ", passwordHistoryCount=" + passwordHistoryCount +
                ", requireUppercase=" + requireUppercase +
                ", requireLowercase=" + requireLowercase +
                ", requireDigits=" + requireDigits +
                ", requireSpecialChars=" + requireSpecialChars +
                ", disallowUsernameOrEmail=" + disallowUsernameOrEmail +
                ", bannedSubstrings=" + bannedSubstrings +
                ", maxPasswordValidityDays=" + maxPasswordValidityDays +
                ", passwordExpirationNotificationDays=" + passwordExpirationNotificationDays +
                ", tenantId='" + tenantId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
