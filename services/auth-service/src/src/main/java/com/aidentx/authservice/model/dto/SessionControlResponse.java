package com.aidentx.authservice.model.dto;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) for session control policy responses.
 * <p>
 * This class is used for returning session control policy data via REST endpoints.
 * Contains all session control policy fields with appropriate data representation
 * for API consumers.
 * </p>
 * 
 * <p>Note: Expiration times are returned in milliseconds as stored internally,
 * but also provide convenient minute/hour representations.</p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class SessionControlResponse {

    /**
     * Unique identifier for the session control policy.
     */
    private UUID id;

    /**
     * Tenant ID this session control policy belongs to.
     */
    private String tenantId;

    /**
     * JWT token expiration time in milliseconds.
     */
    private Long jwtExpirationMs;

    /**
     * JWT token expiration time in minutes (calculated from milliseconds).
     */
    private Integer jwtExpirationMinutes;

    /**
     * Refresh token expiration time in milliseconds.
     */
    private Long refreshTokenExpirationMs;

    /**
     * Refresh token expiration time in hours (calculated from milliseconds).
     */
    private Integer refreshTokenExpirationHours;

    /**
     * Whether to force logout all active sessions when user changes password.
     */
    private Boolean forceLogoutOnPasswordChange;

    /**
     * Default constructor.
     */
    public SessionControlResponse() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id                           session control policy ID
     * @param tenantId                     tenant identifier
     * @param jwtExpirationMs             JWT expiration in milliseconds
     * @param refreshTokenExpirationMs    refresh token expiration in milliseconds
     * @param forceLogoutOnPasswordChange whether to force logout on password change
     */
    public SessionControlResponse(UUID id, String tenantId, Long jwtExpirationMs, 
                                Long refreshTokenExpirationMs, Boolean forceLogoutOnPasswordChange) {
        this.id = id;
        this.tenantId = tenantId;
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtExpirationMinutes = (int) (jwtExpirationMs / 60000); // Convert ms to minutes
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshTokenExpirationHours = (int) (refreshTokenExpirationMs / 3600000); // Convert ms to hours
        this.forceLogoutOnPasswordChange = forceLogoutOnPasswordChange;
    }

    // Getters and Setters

    /**
     * Gets the unique identifier for this session control policy.
     *
     * @return the session control policy ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this session control policy.
     *
     * @param id the session control policy ID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the tenant ID this session control policy belongs to.
     *
     * @return the tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID this session control policy belongs to.
     *
     * @param tenantId the tenant ID
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the JWT token expiration time in milliseconds.
     *
     * @return JWT expiration in milliseconds
     */
    public Long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Sets the JWT token expiration time in milliseconds.
     * Also updates the minutes representation.
     *
     * @param jwtExpirationMs JWT expiration in milliseconds
     */
    public void setJwtExpirationMs(Long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtExpirationMinutes = jwtExpirationMs != null ? (int) (jwtExpirationMs / 60000) : null;
    }

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
     * Gets the refresh token expiration time in milliseconds.
     *
     * @return refresh token expiration in milliseconds
     */
    public Long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    /**
     * Sets the refresh token expiration time in milliseconds.
     * Also updates the hours representation.
     *
     * @param refreshTokenExpirationMs refresh token expiration in milliseconds
     */
    public void setRefreshTokenExpirationMs(Long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshTokenExpirationHours = refreshTokenExpirationMs != null ? (int) (refreshTokenExpirationMs / 3600000) : null;
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
        return "SessionControlResponse{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", jwtExpirationMs=" + jwtExpirationMs +
                ", jwtExpirationMinutes=" + jwtExpirationMinutes +
                ", refreshTokenExpirationMs=" + refreshTokenExpirationMs +
                ", refreshTokenExpirationHours=" + refreshTokenExpirationHours +
                ", forceLogoutOnPasswordChange=" + forceLogoutOnPasswordChange +
                '}';
    }
}
