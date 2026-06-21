package com.aidentx.iam.config.audit;

public record AuditUser(
        String id,
        String username,
        String ipAddress) {
    // A constant for system-level or unidentifiable calls
    // Using a proper UUID format for the system user ID
    public static final AuditUser SYSTEM = new AuditUser("00000000-0000-0000-0000-000000000000", "system", "127.0.0.1");
}
