package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.shared.grpc.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a role member with assignment type information.
 * 
 * <p>This DTO extends UserDto to include the assignment type (MANUAL or DYNAMIC)
 * for users assigned to a specific role. Used for displaying role member overviews.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Role member information with assignment type")
public class RoleMemberDto extends UserDto {

    @Schema(description = "Assignment type (MANUAL or DYNAMIC)", example = "MANUAL")
    @JsonProperty("assignmentType")
    private AssignmentType assignmentType;

    /**
     * Default constructor.
     */
    public RoleMemberDto() {
        super();
    }

    /**
     * Constructor that creates a RoleMemberDto from a UserDto and assignment type.
     * 
     * @param userDto the base user information
     * @param assignmentType the assignment type (MANUAL or DYNAMIC)
     */
    public RoleMemberDto(UserDto userDto, AssignmentType assignmentType) {
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
        this.assignmentType = assignmentType;
    }

    /**
     * Gets the assignment type.
     * 
     * @return the assignment type (MANUAL or DYNAMIC)
     */
    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    /**
     * Sets the assignment type.
     * 
     * @param assignmentType the assignment type to set
     */
    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    /**
     * Helper method to convert LocalDateTime to epoch milliseconds.
     * 
     * @param dateTime the LocalDateTime to convert
     * @return epoch milliseconds or 0 if dateTime is null
     */
    private static long convertToEpochMilli(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        try {
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }
}
