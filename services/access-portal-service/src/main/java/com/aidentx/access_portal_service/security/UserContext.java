package com.aidentx.access_portal_service.security;

/**
 * User context record for workflow service.
 * Used to propagate user information in gRPC calls.
 */
public record UserContext(
    String tenantId,
    String userId,
    String username,
    String ipAddress
) {
}
