package com.aidentx.authservice.enums;

/**
 * Enum representing the status of a user account in the authentication system.
 * <p>
 * Possible values:
 * <ul>
 *   <li>ACTIVE - The user account is active and can authenticate.</li>
 *   <li>INACTIVE - The user account is disabled or deactivated.</li>
 *   <li>LOCKED - The user account is locked due to security reasons.</li>
 *   <li>PENDING_VERIFICATION - The user account is awaiting email or admin verification.</li>
 * </ul>
 */
public enum UserStatus {
    ACTIVE, INACTIVE, LOCKED, PENDING_VERIFICATION
}