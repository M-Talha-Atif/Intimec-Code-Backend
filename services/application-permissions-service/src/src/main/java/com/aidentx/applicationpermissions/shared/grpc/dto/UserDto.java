package com.aidentx.applicationpermissions.shared.grpc.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * DTO representing user information retrieved from auth-service via gRPC.
 * This class provides a clean API response format for user data including all User entity fields.
 */
@Schema(description = AppConstants.USER_DTO_SCHEMA_DESCRIPTION)
public class UserDto {

    @Schema(description = AppConstants.USER_DTO_ID_DESCRIPTION, example = AppConstants.USER_DTO_ID_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_ID)
    private String id;

    @Schema(description = AppConstants.USER_DTO_USERNAME_DESCRIPTION, example = AppConstants.USER_DTO_USERNAME_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_USERNAME)
    private String username;

    @Schema(description = AppConstants.USER_DTO_EMAIL_DESCRIPTION, example = AppConstants.USER_DTO_EMAIL_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_EMAIL)
    private String email;

    @Schema(description = AppConstants.USER_DTO_FIRST_NAME_DESCRIPTION, example = AppConstants.USER_DTO_FIRST_NAME_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_FIRST_NAME)
    private String firstName;

    @Schema(description = AppConstants.USER_DTO_LAST_NAME_DESCRIPTION, example = AppConstants.USER_DTO_LAST_NAME_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_LAST_NAME)
    private String lastName;

    @Schema(description = "User's display name", example = "John Doe")
    @JsonProperty("displayName")
    private String displayName;

    @Schema(description = "User account status", example = "ACTIVE")
    @JsonProperty("status")
    private String status;

    @Schema(description = AppConstants.USER_DTO_TENANT_ID_DESCRIPTION, example = AppConstants.USER_DTO_TENANT_ID_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_TENANT_ID)
    private String tenantId;

    @Schema(description = "Last login timestamp", example = "2025-06-25T12:00:00")
    @JsonProperty("lastLoginAt")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Password last changed timestamp", example = "2025-06-25T12:00:00")
    @JsonProperty("passwordChangedAt")
    private LocalDateTime passwordChangedAt;

    @Schema(description = "Number of failed login attempts", example = "0")
    @JsonProperty("failedLoginAttempts")
    private Integer failedLoginAttempts;

    @Schema(description = "Account locked until timestamp", example = "2025-06-25T12:00:00")
    @JsonProperty("lockedUntil")
    private LocalDateTime lockedUntil;

    @Schema(description = "Email verified timestamp", example = "2025-06-25T12:00:00")
    @JsonProperty("emailVerifiedAt")
    private LocalDateTime emailVerifiedAt;

    @Schema(description = "Whether MFA is enabled", example = "false")
    @JsonProperty("mfaEnabled")
    private Boolean mfaEnabled;

    @Schema(description = "Whether password has expired", example = "false")
    @JsonProperty("isPasswordExpired")
    private Boolean isPasswordExpired;

    @Schema(description = "Profile data as JSON string", example = "{}")
    @JsonProperty("profileData")
    private String profileData;

    @Schema(description = AppConstants.USER_DTO_CREATED_AT_DESCRIPTION, example = AppConstants.USER_DTO_CREATED_AT_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_CREATED_AT)
    private LocalDateTime createdAt;

    @Schema(description = AppConstants.USER_DTO_UPDATED_AT_DESCRIPTION, example = AppConstants.USER_DTO_UPDATED_AT_EXAMPLE)
    @JsonProperty(AppConstants.USER_DTO_JSON_UPDATED_AT)
    private LocalDateTime updatedAt;

    @Schema(description = "ID of user who created this record", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("createdBy")
    private String createdBy;

    @Schema(description = "ID of user who last updated this record", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("updatedBy")
    private String updatedBy;

    /**
     * Default constructor for JSON deserialization.
     */
    public UserDto() {
    }

    /**
     * Constructor to create UserDto from gRPC UserInfo with all fields.
     * 
     * @param id user ID
     * @param username user's username
     * @param email user's email
     * @param firstName user's first name
     * @param lastName user's last name
     * @param displayName user's display name
     * @param status user status
     * @param tenantId tenant ID
     * @param lastLoginAt last login timestamp in milliseconds since epoch
     * @param passwordChangedAt password changed timestamp in milliseconds since epoch
     * @param failedLoginAttempts number of failed login attempts
     * @param lockedUntil locked until timestamp in milliseconds since epoch
     * @param emailVerifiedAt email verified timestamp in milliseconds since epoch
     * @param mfaEnabled whether MFA is enabled
     * @param isPasswordExpired whether password has expired
     * @param profileData profile data as JSON string
     * @param createdAt creation timestamp in milliseconds since epoch
     * @param updatedAt update timestamp in milliseconds since epoch
     * @param createdBy ID of user who created this record
     * @param updatedBy ID of user who last updated this record
     */
    public UserDto(String id, String username, String email, String firstName, String lastName, 
                   String displayName, String status, String tenantId, 
                   long lastLoginAt, long passwordChangedAt, int failedLoginAttempts, 
                   long lockedUntil, long emailVerifiedAt, boolean mfaEnabled, 
                   boolean isPasswordExpired, String profileData, long createdAt, long updatedAt,
                   String createdBy, String updatedBy) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.status = status;
        this.tenantId = tenantId;
        this.lastLoginAt = lastLoginAt > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(lastLoginAt), ZoneOffset.UTC) : null;
        this.passwordChangedAt = passwordChangedAt > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(passwordChangedAt), ZoneOffset.UTC) : null;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(lockedUntil), ZoneOffset.UTC) : null;
        this.emailVerifiedAt = emailVerifiedAt > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(emailVerifiedAt), ZoneOffset.UTC) : null;
        this.mfaEnabled = mfaEnabled;
        this.isPasswordExpired = isPasswordExpired;
        this.profileData = profileData;
        this.createdAt = createdAt > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneOffset.UTC) : null;
        this.updatedAt = updatedAt > 0 ? LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneOffset.UTC) : null;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    /**
     * Legacy constructor for backward compatibility.
     * 
     * @param id user ID
     * @param username user's username
     * @param email user's email
     * @param firstName user's first name
     * @param lastName user's last name
     * @param tenantId tenant ID
     * @param createdAt creation timestamp in milliseconds since epoch
     * @param updatedAt update timestamp in milliseconds since epoch
     */
    public UserDto(String id, String username, String email, String firstName, String lastName, 
                   String tenantId, long createdAt, long updatedAt) {
        this(id, username, email, firstName, lastName, null, null, tenantId, 
             0, 0, 0, 0, 0, false, false, null, createdAt, updatedAt, null, null);
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }



    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public Boolean getMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public Boolean getIsPasswordExpired() {
        return isPasswordExpired;
    }

    public void setIsPasswordExpired(Boolean isPasswordExpired) {
        this.isPasswordExpired = isPasswordExpired;
    }

    public String getProfileData() {
        return profileData;
    }

    public void setProfileData(String profileData) {
        this.profileData = profileData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status='" + status + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", lastLoginAt=" + lastLoginAt +
                ", passwordChangedAt=" + passwordChangedAt +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", lockedUntil=" + lockedUntil +
                ", emailVerifiedAt=" + emailVerifiedAt +
                ", mfaEnabled=" + mfaEnabled +
                ", isPasswordExpired=" + isPasswordExpired +
                ", profileData='" + profileData + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}