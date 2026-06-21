package com.aidentx.authservice.model.dto;

/**
 * Data Transfer Object (DTO) for updating a user's password.
 * <p>
 * This class encapsulates the old password and the new password provided by the user
 * when requesting a password update. It is typically used as the request body in password
 * update endpoints.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     UpdatePasswordRequest request = new UpdatePasswordRequest("oldPass123", "newPass456");
 * </pre>
 *
 * @author aidentx
 * @since 1.0
 */
public class UpdatePasswordRequest {
    /**
     * The user's current (old) password.
     */
    private String oldPassword;
    /**
     * The new password to be set for the user.
     */
    private String newPassword;

    /**
     * Default constructor for frameworks that require it.
     */
    public UpdatePasswordRequest() {
    }

    /**
     * Constructs an UpdatePasswordRequest with the specified old and new passwords.
     *
     * @param oldPassword the user's current password
     * @param newPassword the new password to set
     */
    public UpdatePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    /**
     * Gets the user's current (old) password.
     *
     * @return the old password
     */
    public String getOldPassword() {
        return oldPassword;
    }

    /**
     * Sets the user's current (old) password.
     *
     * @param oldPassword the old password
     */
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
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
