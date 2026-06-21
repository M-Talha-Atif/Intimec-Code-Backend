package com.aidentx.iam.policy.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.config.audit.AuditEventPublisher;
import com.aidentx.iam.config.audit.AuditUser;
import com.aidentx.iam.config.audit.IdentityAuditEvent;
import com.aidentx.iam.config.audit.SecurityContextService;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingGroupDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityTypePolicyDTO;
import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.service.IdentityTypePolicyService;
import com.aidentx.iam.policy.service.ResourceGovernancePolicyService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST Controller for Identity Mapping Policy Management.
 * Moved from Identity service to Core service as part of architectural
 * refactoring.
 * Handles all governance data operations including correlation rules, attribute
 * mappings, and source priorities.
 */
@RestController
@RequestMapping("/api/v1/identity")
public class GovernancePolicyController {

        private final ResourceGovernancePolicyService resourceGovernancePolicyService;
        private final IdentityTypePolicyService identityTypePolicyService;
        private final IUnifiedLogger logger;
        private final AuditEventPublisher auditEventPublisher;
        private final SecurityContextService securityContextService;
        private final ObjectMapper objectMapper;

        public GovernancePolicyController(ResourceGovernancePolicyService resourceGovernancePolicyService,
                        IdentityTypePolicyService identityTypePolicyService,
                        UnifiedLoggerFactory loggerFactory,
                        AuditEventPublisher auditEventPublisher,
                        SecurityContextService securityContextService,
                        ObjectMapper objectMapper) {
                this.resourceGovernancePolicyService = resourceGovernancePolicyService;
                this.identityTypePolicyService = identityTypePolicyService;
                this.logger = loggerFactory.getLogger(GovernancePolicyController.class);
                this.auditEventPublisher = auditEventPublisher;
                this.securityContextService = securityContextService;
                this.objectMapper = objectMapper;
        }

        /** Fire-and-forget audit event; never throws so mutations are unaffected. */
        private void publishAudit(String action, String entityType, String entityId,
                        String identityName, String tenantId,
                        Object stateBefore, Object stateAfter) {
                try {
                        AuditUser auditUser = securityContextService.getAuditUser();
                        auditEventPublisher.publishEvent(IdentityAuditEvent.create(
                                auditUser.id(), auditUser.username(), auditUser.ipAddress(),
                                action, entityType, entityId, identityName, tenantId,
                                stateBefore != null ? objectMapper.valueToTree(stateBefore) : null,
                                stateAfter != null ? objectMapper.valueToTree(stateAfter) : null));
                } catch (Exception ex) {
                        logger.warn().message("Failed to publish audit event: " + ex.getMessage()).log();
                }
        }

        /**
         * GET /identity/mapping/policies/{identityType}
         * Retrieves the complete identity mapping policy for the specified identity
         * type (without resource).
         */
        @GetMapping("/mapping/policies/{identityType}")
        public ResponseEntity<List<IdentityMappingPolicyDTO>> getPolicy(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Retrieving identity mapping policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<IdentityMappingPolicyDTO> policy = resourceGovernancePolicyService.getAllPoliciesForIdentityType(
                                tenantId,
                                identityType);
                return ResponseEntity.ok(policy);
        }

        /**
         * GET /priorities/policies/{identityType}
         * Retrieves the priorities policy for the specified identity type.
         */
        @GetMapping("/priorities/policies/{identityType}")
        public ResponseEntity<IdentityTypePolicyDTO> getPriorities(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Retrieving priorities policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                IdentityTypePolicyDTO policy = identityTypePolicyService.getIdentityTypePolicy(tenantId, identityType);
                return ResponseEntity.ok(policy);
        }

        /**
         * PUT /priorities/policies/{identityType}
         * Updates the priorities policy for the specified identity type.
         */
        @PutMapping("/priorities/policies/{identityType}")
        public ResponseEntity<IdentityTypePolicyDTO> updatePriorities(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId,
                        @RequestBody IdentityTypePolicyDTO policyDto) {
                logger.info().message("Updating priorities policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                IdentityTypePolicyDTO updatedPolicy = identityTypePolicyService.updateIdentityTypePolicy(tenantId,
                                identityType, policyDto);
                return ResponseEntity.ok(updatedPolicy);
        }

        /**
         * PUT /identity/mapping/policies/{identityType}
         * Updates the complete identity mapping policy for the specified schema.
         */
        @PutMapping("/mapping/policies/{identityType}")
        public ResponseEntity<IdentityMappingPolicyDTO> updatePolicy(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId,
                        @RequestBody IdentityMappingPolicyDTO policyDto) {
                logger.info().message("Updating identity mapping policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                // Extract resourceId from the policy DTO
                UUID resourceId = policyDto.resourceId();

                // Fetch the existing policy before mutating so we can include stateBefore in audit events
                List<IdentityMappingPolicyDTO> existingPolicies = null;
                try {
                        existingPolicies = resourceGovernancePolicyService.getAllPoliciesForIdentityType(tenantId, identityType);
                } catch (Exception ignored) {}
                final IdentityMappingPolicyDTO existingPolicy = existingPolicies != null
                        ? existingPolicies.stream()
                                .filter(p -> resourceId == null || resourceId.equals(p.resourceId()))
                                .findFirst().orElse(null)
                        : null;

                IdentityMappingPolicyDTO updatedPolicy = resourceGovernancePolicyService.updatePolicy(tenantId,
                                identityType,
                                resourceId,
                                null, // schemaId not available from REST endpoint, will be fetched from existing policy
                                policyDto);

                // Publish audit events for whichever sections were updated, including stateBefore for diff
                if (policyDto.correlationRules() != null && !policyDto.correlationRules().isEmpty()) {
                        Object prevRules = existingPolicy != null ? existingPolicy.correlationRules() : null;
                        publishAudit(AppConstants.AUDIT_EVENT_UPDATE, "CORRELATION_RULE",
                                        resourceId != null ? resourceId.toString() : identityType.name(),
                                        identityType.name(), tenantId, prevRules, policyDto.correlationRules());
                }
                if (policyDto.attributeMappings() != null && !policyDto.attributeMappings().isEmpty()) {
                        Object prevMappings = existingPolicy != null ? existingPolicy.attributeMappings() : null;
                        publishAudit(AppConstants.AUDIT_EVENT_UPDATE, "ATTRIBUTE_MAPPING",
                                        resourceId != null ? resourceId.toString() : identityType.name(),
                                        identityType.name(), tenantId, prevMappings, policyDto.attributeMappings());
                } else if (policyDto.attributeMappings() != null && policyDto.attributeMappings().isEmpty()) {
                        // All attribute mappings removed — fire a DELETE audit event.
                        // stateAfter is intentionally null; stateBefore holds the removed rows.
                        // The frontend uses stateBefore to resolve source system, impact, and detail view.
                        Object prevMappings = existingPolicy != null ? existingPolicy.attributeMappings() : null;
                        publishAudit(AppConstants.AUDIT_EVENT_DELETE, "ATTRIBUTE_MAPPING",
                                        resourceId != null ? resourceId.toString() : identityType.name(),
                                        identityType.name(), tenantId, prevMappings, null);
                }

                return ResponseEntity.ok(updatedPolicy);
        }

        /**
         * GET /identity/mapping/policies/{identityType}/attribute/mappings
         * Retrieves all attribute mappings for the specified schema.
         */
        @GetMapping("/mapping/policies/{identityType}/attribute/mappings")
        public ResponseEntity<List<AttributeMappingGroupDTO>> getAttributeMappings(
                        @PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Retrieving attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<AttributeMappingGroupDTO> mappings = resourceGovernancePolicyService.getAttributeMappingGroups(
                                tenantId,
                                identityType);
                return ResponseEntity.ok(mappings);
        }

        /**
         * GET /identity/mapping/policies/{identityType}/correlation/rules
         * Retrieves all correlation rules for the specified schema.
         */
        @GetMapping("/mapping/policies/{identityType}/correlation/rules")
        public ResponseEntity<List<CorrelationRuleDTO>> getCorrelationRules(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Retrieving correlation rules")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<CorrelationRuleDTO> rules = resourceGovernancePolicyService.getCorrelationRules(tenantId,
                                identityType);
                return ResponseEntity.ok(rules);
        }

        // --- Individual Update Endpoints ---

        /**
         * PUT /identity/mapping/policies/{identityType}/correlation/rules/{ruleId}
         * Updates a specific correlation rule by its unique ID.
         */
        @PutMapping("/mapping/policies/{identityType}/correlation/rules/{ruleId}")
        public ResponseEntity<CorrelationRuleDTO> updateCorrelationRule(@PathVariable IdentityType identityType,
                        @PathVariable UUID ruleId,
                        @RequestHeader("X-Tenant-ID") String tenantId,
                        @RequestBody CorrelationRuleDTO ruleDto) {
                logger.info().message("Updating specific correlation rule")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("ruleId", ruleId)
                                .log();

                // Extract resourceId from the rule DTO
                UUID resourceId = ruleDto.resourceId();
                CorrelationRuleDTO updatedRule = resourceGovernancePolicyService.updateCorrelationRule(tenantId,
                                identityType,
                                resourceId,
                                ruleId,
                                ruleDto);
                publishAudit(AppConstants.AUDIT_EVENT_UPDATE, "CORRELATION_RULE", ruleId.toString(),
                                identityType.name(), tenantId, null, updatedRule);
                return ResponseEntity.ok(updatedRule);
        }

        /**
         * PUT /identity/mapping/policies/{identityType}/attribute/mappings/{mappingId}
         * Updates a specific attribute mapping by its unique ID.
         */
        @PutMapping("/mapping/policies/{identityType}/attribute/mappings/{mappingId}")
        public ResponseEntity<AttributeMappingDTO> updateAttributeMapping(@PathVariable IdentityType identityType,
                        @PathVariable UUID mappingId,
                        @RequestHeader("X-Tenant-ID") String tenantId,
                        @RequestBody AttributeMappingDTO mappingDto) {
                logger.info().message("Updating specific attribute mapping")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("mappingId", mappingId)
                                .log();

                AttributeMappingDTO updatedMapping = resourceGovernancePolicyService.updateAttributeMapping(tenantId,
                                identityType,
                                mappingId, mappingDto);
                publishAudit(AppConstants.AUDIT_EVENT_UPDATE, "ATTRIBUTE_MAPPING", mappingId.toString(),
                                identityType.name(), tenantId, null, updatedMapping);
                return ResponseEntity.ok(updatedMapping);
        }

        // Removed endpoint for updating nested mapping rules; flat model has no rule
        // nesting.

        // --- Delete Endpoints ---

        /**
         * DELETE /identity/mapping/policies/{identityType}
         * Deletes the entire governance policy for the specified schema (all mappings,
         * rules, and priorities).
         */
        @DeleteMapping("/mapping/policies/{identityType}")
        public ResponseEntity<Void> deletePolicy(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Deleting identity mapping policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                resourceGovernancePolicyService.deletePolicies(tenantId, identityType);
                return ResponseEntity.noContent().build();
        }

        /**
         * DELETE /identity/mapping/policies/{identityType}/correlation/rules
         * Deletes all correlation rules for the specified schema.
         */
        @DeleteMapping("/mapping/policies/{identityType}/correlation/rules")
        public ResponseEntity<Void> deleteCorrelationRules(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Deleting correlation rules")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                resourceGovernancePolicyService.deleteCorrelationRules(tenantId, identityType);
                publishAudit(AppConstants.AUDIT_EVENT_DELETE, "CORRELATION_RULE",
                                java.util.UUID.randomUUID().toString(),
                                identityType.name(), tenantId,
                                java.util.Map.of("identityType", identityType.name(), "scope", "all"), null);
                return ResponseEntity.noContent().build();
        }

        /**
         * DELETE /identity/mapping/policies/{identityType}/correlation/rules/{ruleId}
         * Deletes a specific correlation rule by its unique ID.
         */
        @DeleteMapping("/mapping/policies/{identityType}/correlation/rules/{ruleId}")
        public ResponseEntity<Void> deleteCorrelationRule(@PathVariable IdentityType identityType,
                        @PathVariable UUID ruleId,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Deleting specific correlation rule")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("ruleId", ruleId)
                                .log();

                // Fetch the rule to get its resourceId
                List<CorrelationRuleDTO> allRules = resourceGovernancePolicyService.getCorrelationRules(tenantId,
                                identityType);
                UUID resourceId = allRules.stream()
                                .filter(rule -> rule.id() != null && rule.id().equals(ruleId))
                                .map(CorrelationRuleDTO::resourceId)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Correlation rule not found: " + ruleId));

                CorrelationRuleDTO deletedRule = allRules.stream()
                                .filter(rule -> rule.id() != null && rule.id().equals(ruleId))
                                .findFirst().orElse(null);
                resourceGovernancePolicyService.deleteCorrelationRule(tenantId, identityType, resourceId, ruleId);
                publishAudit(AppConstants.AUDIT_EVENT_DELETE, "CORRELATION_RULE", ruleId.toString(),
                                identityType.name(), tenantId, deletedRule, null);
                return ResponseEntity.noContent().build();
        }

        /**
         * DELETE /identity/mapping/policies/{identityType}/attribute/mappings
         * Deletes all attribute mappings for the specified schema.
         */
        @DeleteMapping("/mapping/policies/{identityType}/attribute/mappings")
        public ResponseEntity<Void> deleteAttributeMappings(@PathVariable IdentityType identityType,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Deleting attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                resourceGovernancePolicyService.deleteAttributeMappings(tenantId, identityType);
                publishAudit(AppConstants.AUDIT_EVENT_DELETE, "ATTRIBUTE_MAPPING",
                                java.util.UUID.randomUUID().toString(),
                                identityType.name(), tenantId,
                                java.util.Map.of("identityType", identityType.name(), "scope", "all"), null);
                return ResponseEntity.noContent().build();
        }

        /**
         * DELETE
         * /identity/mapping/policies/{identityType}/attribute/mappings/{mappingId}
         * Deletes a specific attribute mapping by its unique ID.
         */
        @DeleteMapping("/mapping/policies/{identityType}/attribute/mappings/{mappingId}")
        public ResponseEntity<Void> deleteAttributeMapping(@PathVariable IdentityType identityType,
                        @PathVariable UUID mappingId,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Deleting specific attribute mapping")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("mappingId", mappingId)
                                .log();

                // Fetch the mapping to get its resourceId
                List<AttributeMappingDTO> allMappings = resourceGovernancePolicyService.getAttributeMappings(tenantId,
                                identityType);
                UUID resourceId = allMappings.stream()
                                .filter(mapping -> mapping.id() != null && mapping.id().equals(mappingId))
                                .map(AttributeMappingDTO::sourceResourceId)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Attribute mapping not found: " + mappingId));

                AttributeMappingDTO deletedMapping = allMappings.stream()
                                .filter(mapping -> mapping.id() != null && mapping.id().equals(mappingId))
                                .findFirst().orElse(null);
                resourceGovernancePolicyService.deleteAttributeMapping(tenantId, identityType, resourceId, mappingId);
                // Wrap in a list so stateBefore in the audit log is always a JSON array
                // (consistent with the bulk-update audit events). Frontend helpers
                // all expect Array.isArray(stateBefore) === true.
                List<AttributeMappingDTO> deletedList = deletedMapping != null
                        ? java.util.List.of(deletedMapping) : java.util.List.of();
                publishAudit(AppConstants.AUDIT_EVENT_DELETE, "ATTRIBUTE_MAPPING", mappingId.toString(),
                                identityType.name(), tenantId, deletedList, null);
                return ResponseEntity.noContent().build();
        }

        // --- Synchronization Reactions Endpoints ---

        /**
         * GET /identity/mapping/policies/{identityType}/synchronization/reactions
         * Retrieves the configured synchronization reactions for a given identity type and resource.
         */
        @GetMapping("/mapping/policies/{identityType}/synchronization/reactions")
        public ResponseEntity<List<SynchronizationReactionDTO>> getSynchronizationReactions(
                        @PathVariable IdentityType identityType,
                        @RequestParam UUID resourceId,
                        @RequestHeader("X-Tenant-ID") String tenantId) {
                logger.info().message("Retrieving synchronization reactions")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("resourceId", resourceId)
                                .log();

                List<SynchronizationReactionDTO> reactions = resourceGovernancePolicyService
                                .getSynchronizationReactions(tenantId, identityType, resourceId);
                return ResponseEntity.ok(reactions != null ? reactions : List.of());
        }

        /**
         * PUT /identity/mapping/policies/{identityType}/synchronization/reactions
         * Updates the synchronization reactions for a given identity type and resource.
         */
        @PutMapping("/mapping/policies/{identityType}/synchronization/reactions")
        public ResponseEntity<List<SynchronizationReactionDTO>> updateSynchronizationReactions(
                        @PathVariable IdentityType identityType,
                        @RequestParam UUID resourceId,
                        @RequestHeader("X-Tenant-ID") String tenantId,
                        @RequestBody List<SynchronizationReactionDTO> reactions) {
                logger.info().message("Updating synchronization reactions")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("resourceId", resourceId)
                                .detail("reactionCount", reactions.size())
                                .log();

                resourceGovernancePolicyService.updateSynchronizationReactions(
                                tenantId, identityType, resourceId, reactions);
                return ResponseEntity.ok(reactions);
        }
}
