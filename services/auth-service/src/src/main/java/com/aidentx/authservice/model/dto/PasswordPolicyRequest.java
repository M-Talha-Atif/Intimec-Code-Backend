package com.aidentx.authservice.model.dto;

import com.aidentx.authservice.service.Auth.AppConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Data Transfer Object (DTO) for password policy requests.
 * <p>
 * This class is used for creating and updating password policies via REST endpoints.
 * Contains all necessary fields for password complexity requirements and validation rules.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class PasswordPolicyRequest {

    /**
     * Minimum password length requirement.
     */
    @NotNull(message = AppConstants.VALIDATION_MIN_PASSWORD_LENGTH_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_MIN_PASSWORD_LENGTH_MIN_VALUE)
    private Integer minPasswordLength;

    /**
     * Number of previous passwords to remember and prevent reuse.
     */
    @NotNull(message = AppConstants.VALIDATION_PASSWORD_HISTORY_COUNT_REQUIRED)
    @Min(value = 0, message = AppConstants.VALIDATION_PASSWORD_HISTORY_COUNT_MIN_VALUE)
    private Integer passwordHistoryCount;

    /**
     * Whether uppercase letters are required in passwords.
     */
    @NotNull(message = AppConstants.VALIDATION_REQUIRE_UPPERCASE_REQUIRED)
    private Boolean requireUppercase;

    /**
     * Whether lowercase letters are required in passwords.
     */
    @NotNull(message = AppConstants.VALIDATION_REQUIRE_LOWERCASE_REQUIRED)
    private Boolean requireLowercase;

    /**
     * Whether digits are required in passwords.
     */
    @NotNull(message = AppConstants.VALIDATION_REQUIRE_DIGITS_REQUIRED)
    private Boolean requireDigits;

    /**
     * Whether special characters are required in passwords.
     */
    @NotNull(message = AppConstants.VALIDATION_REQUIRE_SPECIAL_CHARS_REQUIRED)
    private Boolean requireSpecialChars;

    /**
     * Whether to disallow username or email in passwords.
     */
    @NotNull(message = AppConstants.VALIDATION_DISALLOW_USERNAME_OR_EMAIL_REQUIRED)
    private Boolean disallowUsernameOrEmail;

    /**
     * List of banned substrings that cannot appear in passwords.
     * Can be null or empty if no restrictions apply.
     */
    private List<String> bannedSubstrings;

        /**
     * Maximum number of days a password remains valid before expiring.
     */
    @NotNull(message = AppConstants.VALIDATION_MAX_PASSWORD_VALIDITY_DAYS_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_MAX_PASSWORD_VALIDITY_DAYS_MIN_VALUE)
    private Integer maxPasswordValidityDays;

    /**
     * Number of days before password expiration to notify the user.
     */
    @NotNull(message = AppConstants.VALIDATION_PASSWORD_EXPIRATION_NOTIFICATION_DAYS_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_PASSWORD_EXPIRATION_NOTIFICATION_DAYS_MIN_VALUE)
    private Integer passwordExpirationNotificationDays;

    /**
     * Default constructor.
     */
    public PasswordPolicyRequest() {
    }

    // Getters and Setters

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

    @Override
    public String toString() {
        return "PasswordPolicyRequest{" +
                "minPasswordLength=" + minPasswordLength +
                ", passwordHistoryCount=" + passwordHistoryCount +
                ", requireUppercase=" + requireUppercase +
                ", requireLowercase=" + requireLowercase +
                ", requireDigits=" + requireDigits +
                ", requireSpecialChars=" + requireSpecialChars +
                ", disallowUsernameOrEmail=" + disallowUsernameOrEmail +
                ", bannedSubstrings=" + bannedSubstrings +
                ", maxPasswordValidityDays=" + maxPasswordValidityDays +
                ", passwordExpirationNotificationDays=" + passwordExpirationNotificationDays +
                '}';
    }
}
