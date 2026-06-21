package com.aidentx.authservice.enums;

/**
 * Enum representing the status of a user's session.
 * <p>
 * Possible values:
 * <ul>
 *   <li>ACTIVE - The session is currently active and valid.</li>
 *   <li>EXPIRED - The session has expired due to inactivity or token expiration.</li>
 *   <li>TERMINATED - The session was explicitly terminated by the user or system.</li>
 * </ul>
 */
public enum SessionStatus {
    ACTIVE, EXPIRED, TERMINATED
}
