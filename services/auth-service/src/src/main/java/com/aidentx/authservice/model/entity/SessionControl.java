package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing session control configuration for a tenant.
 * <p>
 * This class maps to the <code>session_controls</code> table and contains all session
 * management settings including JWT token expiration times, refresh token expiration,
 * and force logout behavior on password change. Each tenant can have one session control policy.
 * </p>
 *
 * <p>Typical usage involves retrieving session control settings during authentication,
 * token generation, and session management operations to enforce tenant-specific
 * security policies.</p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Entity
@Table(name = "session_controls")
public class SessionControl {

    /**
     * Unique identifier for the session control policy (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Tenant ID this session control policy belongs to.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * JWT token expiration time in milliseconds.
     * <p>
     * Defines how long JWT access tokens remain valid before requiring refresh.
     * </p>
     */
    @Column(name = "jwt_expiration_ms", nullable = false)
    private Long jwtExpirationMs;

    /**
     * Refresh token expiration time in milliseconds.
     * <p>
     * Defines how long refresh tokens remain valid before requiring re-authentication.
     * </p>
     */
    @Column(name = "refresh_token_expiration_ms", nullable = false)
    private Long refreshTokenExpirationMs;

    /**
     * Whether to force logout all active sessions when user changes password.
     * <p>
     * When enabled, password changes will invalidate all existing sessions,
     * requiring users to re-authenticate on all devices.
     * </p>
     */
    @Column(name = "force_logout_on_password_change", nullable = false)
    private Boolean forceLogoutOnPasswordChange = true;

    /**
     * Default constructor.
     */
    public SessionControl() {
    }

    /**
     * Constructor with all fields.
     *
     * @param tenantId                      the tenant identifier
     * @param jwtExpirationMs              JWT token expiration in milliseconds
     * @param refreshTokenExpirationMs     refresh token expiration in milliseconds
     * @param forceLogoutOnPasswordChange  whether to force logout on password change
     */
    public SessionControl(String tenantId, Long jwtExpirationMs, Long refreshTokenExpirationMs, 
                         Boolean forceLogoutOnPasswordChange) {
        this.tenantId = tenantId;
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
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
     *
     * @param jwtExpirationMs JWT expiration in milliseconds
     */
    public void setJwtExpirationMs(Long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
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
     *
     * @param refreshTokenExpirationMs refresh token expiration in milliseconds
     */
    public void setRefreshTokenExpirationMs(Long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
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
        return "SessionControl{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", jwtExpirationMs=" + jwtExpirationMs +
                ", refreshTokenExpirationMs=" + refreshTokenExpirationMs +
                ", forceLogoutOnPasswordChange=" + forceLogoutOnPasswordChange +
                '}';
    }
}
