package com.aidentx.applicationpermissions.shared.grpc.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for user retrieval operations.
 * Contains a list of users and metadata about the operation.
 */
@Schema(description = AppConstants.USERS_RESPONSE_SCHEMA_DESCRIPTION)
public class UsersResponse {

    @Schema(description = AppConstants.USERS_RESPONSE_USERS_DESCRIPTION)
    @JsonProperty(AppConstants.USERS_RESPONSE_JSON_USERS)
    private List<UserDto> users;

    @Schema(description = AppConstants.USERS_RESPONSE_TOTAL_COUNT_DESCRIPTION, example = AppConstants.USERS_RESPONSE_TOTAL_COUNT_EXAMPLE)
    @JsonProperty(AppConstants.USERS_RESPONSE_JSON_TOTAL_COUNT)
    private int totalCount;

    @Schema(description = AppConstants.USERS_RESPONSE_TENANT_ID_DESCRIPTION, example = AppConstants.USERS_RESPONSE_TENANT_ID_EXAMPLE)
    @JsonProperty(AppConstants.USERS_RESPONSE_JSON_TENANT_ID)
    private String tenantId;

    @Schema(description = AppConstants.USERS_RESPONSE_SUCCESS_DESCRIPTION, example = AppConstants.USERS_RESPONSE_SUCCESS_EXAMPLE)
    @JsonProperty(AppConstants.USERS_RESPONSE_JSON_SUCCESS)
    private boolean success;

    @Schema(description = AppConstants.USERS_RESPONSE_MESSAGE_DESCRIPTION, example = AppConstants.USERS_RESPONSE_MESSAGE_EXAMPLE)
    @JsonProperty(AppConstants.USERS_RESPONSE_JSON_MESSAGE)
    private String message;

    /**
     * Default constructor for JSON deserialization.
     */
    public UsersResponse() {
    }

    /**
     * Constructor for successful response.
     * 
     * @param users list of users
     * @param totalCount total number of users
     * @param tenantId tenant ID that was queried
     */
    public UsersResponse(List<UserDto> users, int totalCount, String tenantId) {
        this.users = users;
        this.totalCount = totalCount;
        this.tenantId = tenantId;
        this.success = true;
        this.message = AppConstants.USERS_RETRIEVED_SUCCESSFULLY_MESSAGE;
    }

    /**
     * Constructor for error response.
     * 
     * @param tenantId tenant ID that was queried
     * @param message error message
     */
    public UsersResponse(String tenantId, String message) {
        this.users = List.of();
        this.totalCount = 0;
        this.tenantId = tenantId;
        this.success = false;
        this.message = message;
    }

    // Getters and Setters

    public List<UserDto> getUsers() {
        return users;
    }

    public void setUsers(List<UserDto> users) {
        this.users = users;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "UsersResponse{" +
                "users=" + (users != null ? users.size() + AppConstants.USERS_RESPONSE_USERS_SUFFIX : AppConstants.USERS_RESPONSE_NULL_USERS) +
                ", totalCount=" + totalCount +
                ", tenantId='" + tenantId + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}