package com.aidentx.configuration.audit;

/**
 * A simple principal object to store the authenticated user's ID and username.
 * This is stored in the Spring Security context after JWT validation.
 *
 * @param id       The unique identifier of the user (from JWT 'sub' claim).
 * @param username The username of the user (from JWT 'username' claim).
 */
public record AuditPrincipal(String id, String username) {
}