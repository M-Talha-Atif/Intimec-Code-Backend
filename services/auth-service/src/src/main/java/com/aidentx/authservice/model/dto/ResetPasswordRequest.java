package com.aidentx.authservice.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for submitting a new password during password reset.
 * <p>
 * This class is used to encapsulate the new password provided by a user as part of the password reset process.
 * It is typically used as the request body in password reset endpoints. The newPassword field is validated
 * to ensure it is not blank.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     ResetPasswordRequest request = new ResetPasswordRequest("newSecurePassword123");
 * </pre>
 *
 * @author aidentx
 * @since 1.0
 */
public class ResetPasswordRequest {
    /**
     * The new password to be set for the user. Must not be blank.
     */
    @NotBlank
    private String newPassword;

    /**
     * Default constructor for frameworks that require it.
     */
    public ResetPasswordRequest() {
    }

    /**
     * Constructs a ResetPasswordRequest with the specified new password.
     *
     * @param newPassword the new password to set
     */
    public ResetPasswordRequest(String newPassword) {
        this.newPassword = newPassword;
    }

    /**
     * Gets the new password to be set for the user.
     *
     * @return the new password
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * Sets the new password to be set for the user.
     *
     * @param newPassword the new password
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
