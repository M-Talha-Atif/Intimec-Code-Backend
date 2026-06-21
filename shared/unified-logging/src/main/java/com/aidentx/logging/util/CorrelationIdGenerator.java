package com.aidentx.logging.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for generating and validating correlation IDs.
 * <p>
 * Correlation IDs are used to track related operations across different
 * services
 * and components in the microservice architecture. This implementation provides
 * a default "req-" prefix for standard HTTP requests.
 * </p>
 */
@Component
public class CorrelationIdGenerator {

    /**
     * Default prefix for correlation IDs generated for HTTP requests.
     */
    private static final String DEFAULT_PREFIX = "req";

    /**
     * A regex to validate the format "prefix-alphanumeric-and-hyphens".
     * A full UUID is composed of hexadecimal characters and hyphens, which this
     * pattern supports.
     */
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+-[a-fA-F0-9\\-]+$");

    /**
     * Generates a new correlation ID with the default "req-" prefix.
     * The ID consists of the prefix followed by a full Type 4 UUID to ensure global
     * uniqueness.
     *
     * @return A new correlation ID in the format "req-{full_uuid}".
     */
    public String generate() {
        return generate(DEFAULT_PREFIX);
    }

    /**
     * Generates a correlation ID with a custom prefix.
     *
     * @param prefix The prefix to use (e.g., "batch", "sync", "async"). Must not be
     *               blank.
     * @return A correlation ID in the format "{prefix}-{full_uuid}".
     */
    public String generate(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            throw new IllegalArgumentException("Prefix for correlation ID cannot be null or empty.");
        }
        // Use the full UUID for ensuring uniqueness across a distributed system
        // and avoiding ID collisions.
        return prefix.trim() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Validates if a given string is a plausible correlation ID.
     * <p>
     * A valid ID must not be blank and should generally follow the pattern of
     * a prefix, a hyphen, and an identifier.
     * </p>
     *
     * @param correlationId The correlation ID string to validate.
     * @return {@code true} if the ID is considered valid, {@code false} otherwise.
     */
    public boolean isValid(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            return false;
        }
        // Use a regex for reliable validation.
        return CORRELATION_ID_PATTERN.matcher(correlationId).matches();
    }
}