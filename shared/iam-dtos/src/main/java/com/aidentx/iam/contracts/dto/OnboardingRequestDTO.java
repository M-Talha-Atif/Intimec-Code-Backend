package com.aidentx.iam.contracts.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OnboardingRequestDTO(
		@NotNull DetailsDTO details,
		@NotNull Map<String, Object> connector,
		SchemaDTO schema,
		CorrelationDTO correlation,
		AttributeMappingPolicyDTO attributeMappings,
		AccessDTO access,
		SynchronizationPolicyDTO synchronizationPolicy,
		String iconPath) {

	public enum ObjectTypeKind {
		account, entitlement, generic
	}

	public record DetailsDTO(
			@NotBlank String name,
			List<OwnerDTO> owner,
			List<OwnerDTO> businessOwner,
			@NotBlank String connectorId,
			List<String> category,
			@NotNull Boolean isAuthoritative,
			String identityType,
			String riskLevel,
			List<String> complianceLabels,
			List<ScopeDTO> scopes,
			DepartmentDTO department,
			String description,
			String schemaId) {
	}

	public record OwnerDTO(String id, String name, String kind) {
	}

	public record ScopeDTO(String name, String id) {
	}

	public record DepartmentDTO(String name, String id) {
	}

	public record SchemaDTO(List<SchemaDefinitionDTO> definitions) {
	}

	public record SchemaDefinitionDTO(
			String objectClass,
			ObjectTypeKind type,
			String displayAttribute,
			String descriptionAttribute,
			List<SchemaAttributeDTO> attributes,
			List<String> capabilities) {
	}

	public record SchemaAttributeDTO(
			@NotBlank String name,
			@NotBlank String type,
			String category,
			String description,
			List<String> flags) {
	}

	public record CorrelationDTO(
			@NotBlank String mode,
			RuleGroupDTO ruleGroup,
			ScriptDTO script) {
	}

	public record RuleGroupDTO(
			@NotBlank String combinator,
			@NotNull List<RuleDTO> rules) {
	}

	public record RuleDTO(
			String combinator,
			Boolean not,
			List<RuleDTO> rules,
			String field,
			String operator,
			String valueSource,
			String value) {
	}

	public record ScriptDTO(String data, String lang) {
	}

	public record AccessDTO(
			Boolean timeBoundAccess,
			Boolean accessReview,
			Boolean justInTimeAccess,
			Boolean peerDeviationDetector,
			TimeBoundSettingsDTO timeBoundSettings) {
	}

	public record TimeBoundSettingsDTO(
			Integer defaultExpiryPeriod,
			String autoExtensionPolicy) {
	}

	public record AttributeMappingPolicyDTO(
			@NotNull List<OnboardingAttributeMappingDTO> attributeMappings) {
	}

	public record OnboardingAttributeMappingDTO(
			String sourceAttributeName,
			@NotBlank String targetAttributeName,
			String mode,
			ScriptDTO script) {
	}

	public record AttributeSourcePriorityDTO(
			@NotBlank String targetAttribute,
			@NotNull List<ResourcePriorityDTO> priorities) {
	}

	public record ResourcePriorityDTO(
			@NotNull UUID resourceId,
			@NotBlank String sourceAttribute,
			@NotNull Integer priority) {
	}

	/**
	 * Synchronization policy configuration for onboarding.
	 * Defines what actions to take for each synchronization situation.
	 */
	public record SynchronizationPolicyDTO(
			List<SynchronizationReactionDTO> reactions) {
	}

	/**
	 * A single synchronization reaction mapping.
	 * Maps a situation (UNMATCHED, UNLINKED, LINKED, DELETED, DISPUTED) to an action.
	 */
	public record SynchronizationReactionDTO(
			@NotBlank String situation,
			@NotBlank String action) {
	}
}