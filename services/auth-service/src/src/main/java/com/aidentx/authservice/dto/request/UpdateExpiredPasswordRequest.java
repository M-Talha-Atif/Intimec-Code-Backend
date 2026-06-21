package com.aidentx.authservice.dto.request;

import com.aidentx.authservice.config.AppConstants;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating expired user passwords.
 * <p>
 * This DTO is specifically designed for the expired password update endpoint
 * and includes validation for the new password. The password will be validated
 * against the tenant's password policy dynamically. User identification is 
 * handled via JWT token extraction.
 * </p>
 *
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-09-11
 */
public class UpdateExpiredPasswordRequest {

    /**
     * The user's current (expired) password for verification.
     */
    @NotBlank(message = AppConstants.VALIDATION_CURRENT_PASSWORD_REQUIRED)
    private String currentPassword;

    /**
     * The new password to set for the user.
     * This will be validated against the tenant's password policy.
     */
    @NotBlank(message = AppConstants.VALIDATION_NEW_PASSWORD_REQUIRED)
    private String newPassword;

    /**
     * Confirmation of the new password to ensure accuracy.
     */
    @NotBlank(message = AppConstants.VALIDATION_PASSWORD_CONFIRMATION_REQUIRED)
    private String confirmPassword;

    /**
     * Default constructor.
     */
    public UpdateExpiredPasswordRequest() {
    }

    /**
     * Constructor with all fields.
     *
     * @param currentPassword the current password
     * @param newPassword the new password
     * @param confirmPassword the password confirmation
     */
    public UpdateExpiredPasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        return "UpdateExpiredPasswordRequest{" +
                "currentPassword='[REDACTED]'" +
                ", newPassword='[REDACTED]'" +
                ", confirmPassword='[REDACTED]'" +
                '}';
    }
}
