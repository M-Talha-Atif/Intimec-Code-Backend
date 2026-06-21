package com.aidentx.iam.contracts.dto.policy;

import java.util.UUID;

public record SourcePriorityDTO(
        UUID resourceId, // The immutable ID of the Application from core-service
        String sourceName, // Denormalized name for UI display
        int priority) {
}