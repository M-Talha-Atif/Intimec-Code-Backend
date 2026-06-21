package com.aidentx.core.notifications_management.config.audit;

/**
 * Record representing user information for audit purposes.
 * 
 * @param id the user ID
 * @param username the username
 * @param ipAddress the user's IP address
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
public record AuditUser(
        String id,
        String username,
        String ipAddress
) {
}
