package com.aidentx.applicationpermissions.shared.security;

public record UserContext(String userId, String username, String tenantId, String ipAddress) {
}