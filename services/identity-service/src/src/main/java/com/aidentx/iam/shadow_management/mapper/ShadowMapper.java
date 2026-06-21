package com.aidentx.iam.shadow_management.mapper;

import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.model.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Shadow entity and DTOs.
 */
@Component
public class ShadowMapper {

    /**
     * Converts Shadow entity to ShadowResponse DTO.
     *
     * @param shadow The shadow entity
     * @return ShadowResponse DTO
     */
    public ShadowResponse toShadowResponse(Shadow shadow) {
        if (shadow == null) {
            return null;
        }

        ShadowResponse response = new ShadowResponse();
        response.setId(shadow.getId());
        response.setResourceId(shadow.getResourceId());
        response.setResourceUid(shadow.getResourceUid());
        response.setObjectClass(shadow.getObjectClass());

        // Map kind
        if (shadow.getKind() != null) {
            response.setKind(shadow.getKind());
        }

        response.setLinkedIdentityId(shadow.getLinkedIdentityId());

        // Map synchronization situation
        response.setSynchronizationSituation(shadow.getSynchronizationSituation().name());

        // Attributes — direct Map access, no parsing needed
        response.setAttributes(shadow.getAttributesSafe());

        // Map metadata
        ShadowMetadata metadata = new ShadowMetadata();
        metadata.setCreatedAt(toOffsetDateTime(shadow.getCreatedAt()));
        metadata.setUpdatedAt(toOffsetDateTime(shadow.getUpdatedAt()));
        metadata.setLastSynchronizedAt(toOffsetDateTime(shadow.getLastSynchronizedAt()));
        metadata.setLastProvisionedAt(toOffsetDateTime(shadow.getLastProvisionedAt()));
        response.setMetadata(metadata);

        return response;
    }

    /**
     * Converts a list of Shadow entities to ShadowResponse DTOs.
     *
     * @param shadows List of shadow entities
     * @return List of ShadowResponse DTOs
     */
    public List<ShadowResponse> toShadowResponseList(List<Shadow> shadows) {
        if (shadows == null) {
            return null;
        }
        return shadows.stream()
                .map(this::toShadowResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converts an epoch timestamp to OffsetDateTime.
     *
     * <p>Handles both epoch-milliseconds and epoch-seconds values transparently.
     * Values below 10^11 (~year 5138 in seconds, year 1973 in milliseconds) are
     * treated as epoch seconds and converted to milliseconds before parsing.
     * Values at or above 10^11 are treated as epoch milliseconds.
     *
     * <p>This heuristic is needed because some shadows in the database were
     * persisted with epoch-second timestamps (before @PrePersist was standardised
     * to use toEpochMilli()).
     *
     * @param epochValue Epoch seconds or epoch milliseconds
     * @return OffsetDateTime or null
     */
    private OffsetDateTime toOffsetDateTime(Long epochValue) {
        if (epochValue == null) {
            return null;
        }
        // Threshold: 10^11 ms = ~year 1973. Any stored value below this is epoch seconds.
        long millis = epochValue < 100_000_000_000L ? epochValue * 1000L : epochValue;
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }

    /**
     * Converts OffsetDateTime to epoch milliseconds.
     *
     * @param dateTime OffsetDateTime
     * @return Epoch milliseconds or null
     */
    public Long toEpochMilli(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toInstant().toEpochMilli();
    }

    /**
     * Creates GetShadowsByIdentityResponse from list of shadows.
     *
     * @param identityId The identity ID
     * @param shadows    List of shadows
     * @return GetShadowsByIdentityResponse
     */
    public GetShadowsByIdentityResponse toGetShadowsByIdentityResponse(
            java.util.UUID identityId,
            List<Shadow> shadows) {
        GetShadowsByIdentityResponse response = new GetShadowsByIdentityResponse();
        response.setIdentityId(identityId);
        response.setShadows(toShadowResponseList(shadows));
        response.setTotalCount(shadows != null ? shadows.size() : 0);
        return response;
    }

    /**
     * Creates ShadowListResponse from page of shadows.
     *
     * @param shadows       List of shadows in current page
     * @param totalElements Total number of elements
     * @param pageNumber    Current page number (0-indexed)
     * @param pageSize      Page size
     * @return ShadowListResponse
     */
    public ShadowListResponse toShadowListResponse(
            List<Shadow> shadows,
            long totalElements,
            int pageNumber,
            int pageSize) {

        // Calculate pagination metadata
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = pageNumber < totalPages - 1;
        boolean hasPrevious = pageNumber > 0;

        PaginationMetadata pagination = new PaginationMetadata(
                pageNumber,
                pageSize,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious);

        return new ShadowListResponse(toShadowResponseList(shadows), pagination);
    }

    /**
     * Partially updates an existing shadow entity from a DTO.
     * Only updates non-null fields.
     *
     * @param shadow   Existing shadow entity
     * @param response DTO with updated values
     */
    public void updateShadowFromDto(Shadow shadow, ShadowResponse response) {
        if (response == null) {
            return;
        }

        if (response.getResourceUid() != null) {
            shadow.setResourceUid(response.getResourceUid());
        }
        if (response.getObjectClass() != null) {
            shadow.setObjectClass(response.getObjectClass());
        }
        if (response.getKind() != null) {
            shadow.setKind(response.getKind());
        }
        if (response.getLinkedIdentityId() != null) {
            shadow.setLinkedIdentityId(response.getLinkedIdentityId());
        }
        if (response.getSynchronizationSituation() != null) {
            shadow.setSynchronizationSituation(
                    Shadow.SynchronizationSituation.valueOf(response.getSynchronizationSituation()));
        }
        if (response.getAttributes() != null) {
            shadow.setAttributes(response.getAttributes());
        }
    }
}
