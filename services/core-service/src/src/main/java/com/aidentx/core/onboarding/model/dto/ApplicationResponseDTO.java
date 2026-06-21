package com.aidentx.core.onboarding.model.dto;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ApplicationResponseDTO(
                UUID id,
                OnboardingRequestDTO.DetailsDTO details,
                Map<String, Object> connector,
                OnboardingRequestDTO.SchemaDTO schema,
                OnboardingRequestDTO.CorrelationDTO correlation,
                OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings,
                OnboardingRequestDTO.AccessDTO access,
                OnboardingRequestDTO.SynchronizationPolicyDTO synchronizationPolicy,
                Instant createdAt,
                Instant updatedAt,
                long totalAccounts,
                long orphanCount) {

        /**
         * Backward-compatible constructor without stats (defaults to 0).
         */
        public ApplicationResponseDTO(
                        UUID id,
                        OnboardingRequestDTO.DetailsDTO details,
                        Map<String, Object> connector,
                        OnboardingRequestDTO.SchemaDTO schema,
                        OnboardingRequestDTO.CorrelationDTO correlation,
                        OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings,
                        OnboardingRequestDTO.AccessDTO access,
                        OnboardingRequestDTO.SynchronizationPolicyDTO synchronizationPolicy,
                        Instant createdAt,
                        Instant updatedAt) {
                this(id, details, connector, schema, correlation, attributeMappings,
                                access, synchronizationPolicy, createdAt, updatedAt, 0, 0);
        }
}
