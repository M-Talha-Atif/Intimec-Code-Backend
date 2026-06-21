package com.aidentx.core.onboarding.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// This DTO represents the fixed structure of the "details" tab.
public record ManagedSystemDetailsDTO(
                String connectorId,
                String tenantId,
                String name,
                String ownerName,
                String ownerId,
                String type,
                List<String> category,
                boolean isAuthoritative,
                String identityType,
                String riskLevel,
                List<String> complianceLabels,
                String description,

                @NotNull(message = "Primary Schema ID is required") UUID primarySchemaId) {
}