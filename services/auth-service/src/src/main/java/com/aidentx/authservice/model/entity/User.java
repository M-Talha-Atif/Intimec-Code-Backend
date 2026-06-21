package com.aidentx.authservice.model.entity;

import com.aidentx.authservice.enums.UserStatus;
import com.aidentx.authservice.utils.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user in the authentication system.
 * <p>
 * This class maps to the <code>users</code> table and contains all relevant user information,
 * including credentials, profile data, status, and audit fields. It supports features such as
 * multi-factor authentication (MFA), account locking, and profile extensibility via JSON.
 * </p>
 *
 * <p>Typical usage involves creating, updating, and querying user records for authentication,
 * authorization, and user management operations.</p>
 *
 * @author aidentx
 * @since 1.0
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Unique identifier for the user (UUID).
     */
    @Id
    private UUID id;

    /**
     * Reference to the Identity service's identity record.
     * This links the auth user to their identity in the IAM system.
     * Nullable initially to support legacy users and migration scenarios.
     */
    @Column(name = "identity_id", unique = true)
    private UUID identityId;

    /**
     * Type of identity (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN).
     * Stored for quick filtering without joining to Identity service.
     */
    @Column(name = "identity_type", length = 20)
    private String identityType;


    /**
     * Unique username for the user.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Unique email address for the user.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Hashed password for the user.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * User's first name.
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * User's display name (can be shown in UI).
     */
    @Column(name = "display_name")
    private String displayName;

    /**
     * Status of the user (e.g., ACTIVE, INACTIVE, LOCKED).
     */
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    /**
     * Timestamp of the user's last login.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Timestamp when the user's password was last changed.
     */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    /**
     * Number of failed login attempts.
     */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    /**
     * Timestamp until which the user account is locked.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * Timestamp when the user's email was verified.
     */
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    /**
     * Whether multi-factor authentication (MFA) is enabled for the user.
     */
    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled;

    /**
     * Secret key for MFA (if enabled).
     */
    @Column(name = "mfa_secret")
    private String mfaSecret;

    /**
     * Indicates whether the user's password has expired and requires updating.
     * This field is set to true when the password policy expires and is extended,
     * and set to false when the user successfully updates their password.
     * Used for individual user-level password expiration tracking independent
     * of tenant-level policy timing.
     */
    @Column(name = "is_password_expired")
    private Boolean isPasswordExpired;

    /**
     * Extensible profile data stored as JSON (e.g., additional attributes).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode profileData;

    /**
     * Tenant ID that this user belongs to.
     * Used for multi-tenant support to isolate user data.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Timestamp when the user was created.
     */
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * Timestamp when the user was last updated.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * ID of the user who created this user record (for auditing).
     */
    @Column(name = "created_by")
    private UUID createdBy;

    /**
     * ID of the user who last updated this user record (for auditing).
     */
    @Column(name = "updated_by")
    private UUID updatedBy;

    // Constructors

    /**
     * Default constructor for frameworks that require it.
     */
    public User() {
    }

    /**
     * Constructs a User with all fields specified.
     *
     * @param id unique identifier for the user
     * @param username unique username
     * @param email unique email address
     * @param passwordHash hashed password
     * @param firstName user's first name
     * @param lastName user's last name
     * @param displayName user's display name
     * @param status user status
     * @param lastLoginAt last login timestamp
     * @param passwordChangedAt password last changed timestamp
     * @param failedLoginAttempts number of failed login attempts
     * @param lockedUntil account locked until timestamp
     * @param emailVerifiedAt email verified timestamp
     * @param mfaEnabled whether MFA is enabled
     * @param mfaSecret MFA secret key
     * @param profileData extensible profile data as JSON
     * @param tenantId tenant ID that this user belongs to
     * @param createdAt creation timestamp
     * @param updatedAt last updated timestamp
     * @param createdBy ID of the user who created this record
     * @param updatedBy ID of the user who last updated this record
     */
    public User(UUID id, UUID identityId, String identityType, String username, String email,
                String passwordHash, String firstName, String lastName, String displayName,
                UserStatus status, Instant lastLoginAt, Instant passwordChangedAt,
                Integer failedLoginAttempts, Instant lockedUntil, Instant emailVerifiedAt,
                Boolean mfaEnabled, String mfaSecret, Boolean isPasswordExpired,
                JsonNode profileData, String tenantId, Instant createdAt, Instant updatedAt,
                UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.identityId = identityId;
        this.identityType = identityType;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.passwordChangedAt = passwordChangedAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.emailVerifiedAt = emailVerifiedAt;
        this.mfaEnabled = mfaEnabled;
        this.mfaSecret = mfaSecret;
        this.isPasswordExpired = isPasswordExpired;
        this.profileData = profileData;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }


    // Getters and Setters

    /**
     * Gets the reference to the Identity service's identity record.
     *
     * @return the identity ID from Identity service
     */
    public UUID getIdentityId() {
        return identityId;
    }

    /**
     * Sets the reference to the Identity service's identity record.
     *
     * @param identityId the identity ID from Identity service
     */
    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    /**
     * Gets the identity type (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN).
     *
     * @return the identity type
     */
    public String getIdentityType() {
        return identityType;
    }

    /**
     * Sets the identity type.
     *
     * @param identityType the identity type
     */
    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    /**
     * Gets the unique identifier for the user.
     *
     * @return the user ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the user.
     *
     * @param id the user ID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the unique username for the user.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the unique username for the user.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the unique email address for the user.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the unique email address for the user.
     *
     * @param email the email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the hashed password for the user.
     *
     * @return the password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the hashed password for the user.
     *
     * @param passwordHash the password hash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Gets the user's first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     *
     * @param firstName the first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     *
     * @param lastName the last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the user's display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the user's display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the status of the user.
     *
     * @return the user status
     */
    public UserStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the user.
     *
     * @param status the user status
     */
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    /**
     * Gets the timestamp of the user's last login.
     *
     * @return the last login timestamp
     */
    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    /**
     * Sets the timestamp of the user's last login.
     *
     * @param lastLoginAt the last login timestamp
     */
    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Gets the timestamp when the user's password was last changed.
     *
     * @return the password changed timestamp
     */
    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    /**
     * Sets the timestamp when the user's password was last changed.
     *
     * @param passwordChangedAt the password changed timestamp
     */
    public void setPasswordChangedAt(Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    /**
     * Gets the number of failed login attempts.
     *
     * @return the failed login attempts
     */
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /**
     * Sets the number of failed login attempts.
     *
     * @param failedLoginAttempts the failed login attempts
     */
    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    /**
     * Gets the timestamp until which the user account is locked.
     *
     * @return the locked until timestamp
     */
    public Instant getLockedUntil() {
        return lockedUntil;
    }

    /**
     * Sets the timestamp until which the user account is locked.
     *
     * @param lockedUntil the locked until timestamp
     */
    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    /**
     * Gets the timestamp when the user's email was verified.
     *
     * @return the email verified timestamp
     */
    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    /**
     * Sets the timestamp when the user's email was verified.
     *
     * @param emailVerifiedAt the email verified timestamp
     */
    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    /**
     * Gets whether multi-factor authentication (MFA) is enabled for the user.
     *
     * @return true if MFA is enabled, false otherwise
     */
    public Boolean getMfaEnabled() {
        return mfaEnabled;
    }

    /**
     * Sets whether multi-factor authentication (MFA) is enabled for the user.
     *
     * @param mfaEnabled true if MFA is enabled, false otherwise
     */
    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    /**
     * Gets the secret key for MFA (if enabled).
     *
     * @return the MFA secret key
     */
    public String getMfaSecret() {
        return mfaSecret;
    }

    /**
     * Sets the secret key for MFA (if enabled).
     *
     * @param mfaSecret the MFA secret key
     */
    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    /**
     * Gets whether the user's password has expired.
     * 
     * @return true if the user's password has expired and requires updating, false otherwise
     */
    public Boolean getIsPasswordExpired() {
        return isPasswordExpired;
    }

    /**
     * Sets whether the user's password has expired.
     * This should be set to true when password policy expires and is extended,
     * and set to false when the user successfully updates their password.
     * 
     * @param isPasswordExpired true if the password has expired, false otherwise
     */
    public void setIsPasswordExpired(Boolean isPasswordExpired) {
        this.isPasswordExpired = isPasswordExpired;
    }

    /**
     * Gets the extensible profile data stored as JSON.
     *
     * @return the profile data as a JsonNode
     */
    public JsonNode getProfileData() {
        return profileData;
    }

    /**
     * Sets the extensible profile data stored as JSON.
     *
     * @param profileData the profile data as a JsonNode
     */
    public void setProfileData(JsonNode profileData) {
        this.profileData = profileData;
    }

    /**
     * Gets the tenant ID that this user belongs to.
     *
     * @return the tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID that this user belongs to.
     *
     * @param tenantId the tenant ID
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the timestamp when the user was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when the user was created.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp when the user was last updated.
     *
     * @return the last updated timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when the user was last updated.
     *
     * @param updatedAt the last updated timestamp
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the ID of the user who created this user record.
     *
     * @return the creator's user ID
     */
    public UUID getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the ID of the user who created this user record.
     *
     * @param createdBy the creator's user ID
     */
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the ID of the user who last updated this user record.
     *
     * @return the updater's user ID
     */
    public UUID getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Sets the ID of the user who last updated this user record.
     *
     * @param updatedBy the updater's user ID
     */
    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
