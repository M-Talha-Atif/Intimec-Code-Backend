package com.aidentx.authservice.security;


/**
 * A simple record to hold the identity of the current user.
 */
public record UserContext(String userId, String username) {}