package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for bulk role revocation operations.
 * 
 * @param roleId Role ID that was revoked
 * @param roleName Name of the role that was revoked
 * @param requestedUsers List of user IDs that were requested to be revoked
 * @param successfulRevocations List of user IDs where revocation was successful
 * @param failedRevocations List of user IDs where revocation failed
 * @param totalRequested Total number of users requested for revocation
 * @param totalSuccessful Total number of successful revocations
 * @param totalFailed Total number of failed revocations
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record BulkRoleRevocationResponse(
    UUID roleId,
    String roleName,
    List<UUID> requestedUsers,
    List<UUID> successfulRevocations,
    List<UUID> failedRevocations,
    int totalRequested,
    int totalSuccessful,
    int totalFailed
) {
    /**
     * Checks if all revocations were successful.
     * 
     * @return true if all requested revocations succeeded
     */
    public boolean allSuccessful() {
        return totalFailed == 0;
    }
    
    /**
     * Checks if any revocations were successful.
     * 
     * @return true if at least one revocation succeeded
     */
    public boolean anySuccessful() {
        return totalSuccessful > 0;
    }
}