package com.aidentx.iam.security;

public record UserContext(String userId, String username, String ipAddress) {
}