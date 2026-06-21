package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO representing user information with role assignment status.
 * 
 * <p>Extends the base UserDto to include information about whether the user
 * has a specific role assigned. This is used for endpoints that need to
 * show all users in a tenant along with their role assignment status.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "User information with role assignment status")
public class UserWithRoleDto extends UserDto {

    @Schema(description = "Indicates whether the user has the specified role", example = "true")
    @JsonProperty("hasRole")
    private Boolean hasRole;

    /**
     * Default constructor.
     */
    public UserWithRoleDto() {
        super();
    }

    /**
     * Constructor that creates a UserWithRoleDto from a UserDto and hasRole status.
     * 
     * @param userDto the base user information
     * @param hasRole whether the user has the specified role
     */
    public UserWithRoleDto(UserDto userDto, Boolean hasRole) {
        super(userDto.getId(), userDto.getUsername(), userDto.getEmail(), 
              userDto.getFirstName(), userDto.getLastName(), 
              userDto.getDisplayName(), userDto.getStatus(), userDto.getTenantId(),
              convertToEpochMilli(userDto.getLastLoginAt()),
              convertToEpochMilli(userDto.getPasswordChangedAt()),
              userDto.getFailedLoginAttempts() != null ? userDto.getFailedLoginAttempts() : 0,
              convertToEpochMilli(userDto.getLockedUntil()),
              convertToEpochMilli(userDto.getEmailVerifiedAt()),
              userDto.getMfaEnabled() != null ? userDto.getMfaEnabled() : false,
              userDto.getIsPasswordExpired() != null ? userDto.getIsPasswordExpired() : false,
              userDto.getProfileData(),
              convertToEpochMilli(userDto.getCreatedAt()),
              convertToEpochMilli(userDto.getUpdatedAt()),
              userDto.getCreatedBy(),
              userDto.getUpdatedBy());
        this.hasRole = hasRole;
    }

    /**
     * Full constructor for creating UserWithRoleDto.
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
     * @param hasRole whether the user has the specified role
     */
    public UserWithRoleDto(String id, String username, String email, String firstName, String lastName, 
                          String displayName, String status, String tenantId, 
                          long lastLoginAt, long passwordChangedAt, int failedLoginAttempts, 
                          long lockedUntil, long emailVerifiedAt, boolean mfaEnabled, 
                          boolean isPasswordExpired, String profileData, long createdAt, long updatedAt,
                          String createdBy, String updatedBy, Boolean hasRole) {
        super(id, username, email, firstName, lastName, displayName, status, tenantId,
              lastLoginAt, passwordChangedAt, failedLoginAttempts, lockedUntil, emailVerifiedAt,
              mfaEnabled, isPasswordExpired, profileData, createdAt, updatedAt, createdBy, updatedBy);
        this.hasRole = hasRole;
    }

    /**
     * Helper method to convert LocalDateTime to epoch milliseconds.
     * 
     * @param dateTime the LocalDateTime to convert
     * @return epoch milliseconds or 0 if dateTime is null
     */
    private static long convertToEpochMilli(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() : 0;
    }

    // Getter and Setter for hasRole

    public Boolean getHasRole() {
        return hasRole;
    }

    public void setHasRole(Boolean hasRole) {
        this.hasRole = hasRole;
    }

    @Override
    public String toString() {
        return "UserWithRoleDto{" +
                "hasRole=" + hasRole +
                ", " + super.toString() +
                '}';
    }
}