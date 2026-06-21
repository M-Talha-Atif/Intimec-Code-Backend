package com.aidentx.workflowsservice.security;

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
