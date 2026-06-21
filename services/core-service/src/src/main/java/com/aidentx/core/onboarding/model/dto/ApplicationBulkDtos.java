package com.aidentx.core.onboarding.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk application lookup.
 *
 * <p>This DTO is optimized for cross-service communication where only
 * minimal application metadata is required (id + name).
 *
 * <p>Used by entitlement service to resolve resourceId → application name
 * without fetching full application payload.
 *
 * @author aidentx
 * @since 1.0
 */
public final class ApplicationBulkDtos {

    private ApplicationBulkDtos() {}

    /** Request payload containing application IDs. */
    public record BulkRequest(List<UUID> ids) {}

    /** Lightweight application projection. */
    public record ApplicationNameDto(
            UUID id,
            String name
    ) {}

    /** Response wrapper. */
    public record BulkResponse(
            List<ApplicationNameDto> applications
    ) {}
}