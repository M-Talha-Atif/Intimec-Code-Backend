package com.aidentx.authservice.model.dto;

import com.aidentx.authservice.config.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) representing the login request payload.
 * Contains the username and password fields required for authentication.
 */
public class LoginRequest {

    /**
     * The username or identifier of the user attempting to log in.
     * This field must not be blank.
     */
    @NotBlank(message = AppConstants.USERNAME_REQUIRED_MSG)
    private String username;

    /**
     * The user's password.
     * This field must not be blank and must meet the minimum length requirement.
     */
    @NotBlank(message = AppConstants.PASSWORD_REQUIRED_MSG)
    @Size(min = AppConstants.PASSWORD_MIN_LENGTH, message = AppConstants.PASSWORD_MIN_LENGTH_MSG)
    private String password;

    /**
     * Default constructor for deserialization.
     */
    public LoginRequest() {
    }

    /**
     * Parameterized constructor to create a login request with provided credentials.
     *
     * @param username the username or identifier
     * @param password the user's password
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the user's identifier (username).
     *
     * @return the username
     */
    public String getIdentifier() {
        return username;
    }

    /**
     * Sets the user's identifier (username).
     *
     * @param identifier the username
     */
    public void setIdentifier(String identifier) {
        this.username = identifier;
    }

    /**
     * Gets the user's password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
