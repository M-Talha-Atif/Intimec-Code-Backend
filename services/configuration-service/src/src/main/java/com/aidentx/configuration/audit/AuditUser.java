package com.aidentx.configuration.audit;

public record AuditUser(
        String id,
        String username,
        String ipAddress,
        String tenantId) {
    public static final AuditUser SYSTEM = new AuditUser("", "", "", "");
}