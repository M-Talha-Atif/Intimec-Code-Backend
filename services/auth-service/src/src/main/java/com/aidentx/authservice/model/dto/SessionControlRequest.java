package com.aidentx.authservice.model.dto;

import com.aidentx.authservice.config.AppConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) for session control policy requests.
 * <p>
 * This class is used for creating and updating session control policies via REST endpoints.
 * Contains all necessary fields for session management configuration including JWT expiration,
 * refresh token expiration, and force logout behavior.
 * </p>
 * 
 * <p>Note: JWT expiration is provided in minutes and refresh token expiration in hours,
 * both are converted to milliseconds internally for storage.</p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class SessionControlRequest {

    /**
     * JWT token expiration time in minutes.
     * <p>
     * This value will be converted to milliseconds for internal storage.
     * Minimum value is 1 minute.
     * </p>
     */
    @NotNull(message = AppConstants.VALIDATION_JWT_EXPIRATION_MINUTES_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_JWT_EXPIRATION_MINUTES_MIN_VALUE)
    private Integer jwtExpirationMinutes;

    /**
     * Refresh token expiration time in hours.
     * <p>
     * This value will be converted to milliseconds for internal storage.
     * Minimum value is 1 hour.
     * </p>
     */
    @NotNull(message = AppConstants.VALIDATION_REFRESH_TOKEN_EXPIRATION_HOURS_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_REFRESH_TOKEN_EXPIRATION_HOURS_MIN_VALUE)
    private Integer refreshTokenExpirationHours;

    /**
     * Whether to force logout all active sessions when user changes password.
     * <p>
     * When enabled, password changes will invalidate all existing sessions,
     * requiring users to re-authenticate on all devices.
     * </p>
     */
    @NotNull(message = AppConstants.VALIDATION_FORCE_LOGOUT_ON_PASSWORD_CHANGE_REQUIRED)
    private Boolean forceLogoutOnPasswordChange;

    /**
     * Default constructor.
     */
    public SessionControlRequest() {
    }

    /**
     * Constructor with all fields.
     *
     * @param jwtExpirationMinutes          JWT expiration in minutes
     * @param refreshTokenExpirationHours   refresh token expiration in hours
     * @param forceLogoutOnPasswordChange   whether to force logout on password change
     */
    public SessionControlRequest(Integer jwtExpirationMinutes, Integer refreshTokenExpirationHours, 
                               Boolean forceLogoutOnPasswordChange) {
        this.jwtExpirationMinutes = jwtExpirationMinutes;
        this.refreshTokenExpirationHours = refreshTokenExpirationHours;
        this.forceLogoutOnPasswordChange = forceLogoutOnPasswordChange;
    }

    // Getters and Setters

    /**
     * Gets the JWT token expiration time in minutes.
     *
     * @return JWT expiration in minutes
     */
    public Integer getJwtExpirationMinutes() {
        return jwtExpirationMinutes;
    }

    /**
     * Sets the JWT token expiration time in minutes.
     *
     * @param jwtExpirationMinutes JWT expiration in minutes
     */
    public void setJwtExpirationMinutes(Integer jwtExpirationMinutes) {
        this.jwtExpirationMinutes = jwtExpirationMinutes;
    }

    /**
     * Gets the refresh token expiration time in hours.
     *
     * @return refresh token expiration in hours
     */
    public Integer getRefreshTokenExpirationHours() {
        return refreshTokenExpirationHours;
    }

    /**
     * Sets the refresh token expiration time in hours.
     *
     * @param refreshTokenExpirationHours refresh token expiration in hours
     */
    public void setRefreshTokenExpirationHours(Integer refreshTokenExpirationHours) {
        this.refreshTokenExpirationHours = refreshTokenExpirationHours;
    }

    /**
     * Gets whether to force logout on password change.
     *
     * @return true if force logout is enabled, false otherwise
     */
    public Boolean getForceLogoutOnPasswordChange() {
        return forceLogoutOnPasswordChange;
    }

    /**
     * Sets whether to force logout on password change.
     *
     * @param forceLogoutOnPasswordChange true to enable force logout, false otherwise
     */
    public void setForceLogoutOnPasswordChange(Boolean forceLogoutOnPasswordChange) {
        this.forceLogoutOnPasswordChange = forceLogoutOnPasswordChange;
    }

    @Override
    public String toString() {
        return "SessionControlRequest{" +
                "jwtExpirationMinutes=" + jwtExpirationMinutes +
                ", refreshTokenExpirationHours=" + refreshTokenExpirationHours +
                ", forceLogoutOnPasswordChange=" + forceLogoutOnPasswordChange +
                '}';
    }
}
