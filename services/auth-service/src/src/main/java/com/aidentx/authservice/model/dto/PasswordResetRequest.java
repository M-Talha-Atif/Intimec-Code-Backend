package com.aidentx.authservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for requesting a password reset by email.
 * <p>
 * This class is used to encapsulate the email address of a user who is requesting
 * a password reset. It is typically used as the request body in password reset
 * endpoints. The email field is validated to ensure it is not blank and is a valid email address.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     PasswordResetRequest request = new PasswordResetRequest("user@example.com");
 * </pre>
 *
 * @author aidentx
 * @since 1.0
 */
public class PasswordResetRequest {
    /**
     * The email address of the user requesting a password reset.
     * Must not be blank and must be a valid email address.
     */
    @NotBlank
    @Email
    private String email;

    /**
     * Default constructor for frameworks that require it.
     */
    public PasswordResetRequest() {
    }

    /**
     * Constructs a PasswordResetRequest with the specified email address.
     *
     * @param email the email address of the user requesting a password reset
     */
    public PasswordResetRequest(String email) {
        this.email = email;
    }

    /**
     * Gets the email address of the user requesting a password reset.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user requesting a password reset.
     *
     * @param email the email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
