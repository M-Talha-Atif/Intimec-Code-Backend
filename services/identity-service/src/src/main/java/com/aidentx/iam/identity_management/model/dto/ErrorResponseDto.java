package com.aidentx.iam.identity_management.model.dto;

import java.time.Instant;

/**
 * DTO for error responses.
 */
public record ErrorResponseDto(
    String error,
    String message,
    Instant timestamp,
    String path
) {
    public static ErrorResponseDto of(String error, String message, String path) {
        return new ErrorResponseDto(error, message, Instant.now(), path);
    }
}
