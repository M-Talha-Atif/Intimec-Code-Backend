package com.aidentx.iam.entitlement_management.policy.controller;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.config.audit.AuditEventPublisher;
import com.aidentx.iam.config.audit.AuditUser;
import com.aidentx.iam.config.audit.IdentityAuditEvent;
import com.aidentx.iam.config.audit.SecurityContextService;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.EntitlementMappingPolicyDTO;
import com.aidentx.iam.entitlement_management.policy.service.EntitlementGovernancePolicyService;
import com.aidentx.iam.entitlement_management.service.EntitlementSchemaService;
import com.aidentx.iam.security.JwtTenantIdService;
import com.aidentx.iam.shadow_management.sync.EntitlementMappingEngine;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for entitlement governance policy management.
 *
 * <p>Provides endpoints for administrators to configure inbound attribute
 * mapping rules for entitlements imported from a given application
 * (resource). These rules are consumed by the {@link EntitlementMappingEngine}
 * during synchronization to populate canonical governance fields.</p>
 *
 * <h3>Base path</h3>
 * {@code /api/v1/entitlements}
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET  /policy/{resourceId}/mappings} – get current mapping rules</li>
 *   <li>{@code PUT  /policy/{resourceId}/mappings} – replace all mapping rules</li>
 *   <li>{@code DELETE /policy/{resourceId}} – remove the policy for an app</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/entitlements")
public class EntitlementGovernancePolicyController {

    private final EntitlementGovernancePolicyService policyService;
    private final EntitlementMappingEngine mappingEngine;
    private final IUnifiedLogger logger;
    private final AuditEventPublisher auditEventPublisher;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;
    private final EntitlementSchemaService entitlementSchemaService;
    private final JwtTenantIdService jwtTenantIdService;

    public EntitlementGovernancePolicyController(
            EntitlementGovernancePolicyService policyService,
            EntitlementMappingEngine mappingEngine,
            UnifiedLoggerFactory loggerFactory,
            AuditEventPublisher auditEventPublisher,
            SecurityContextService securityContextService,
            ObjectMapper objectMapper,
            EntitlementSchemaService entitlementSchemaService,
            JwtTenantIdService jwtTenantIdService) {
        this.policyService = policyService;
        this.mappingEngine = mappingEngine;
        this.logger = loggerFactory.getLogger(EntitlementGovernancePolicyController.class);
        this.auditEventPublisher = auditEventPublisher;
        this.securityContextService = securityContextService;
        this.objectMapper = objectMapper;
        this.entitlementSchemaService = entitlementSchemaService;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    // -----------------------------------------------------------------------
    // GET /policy/{resourceId}/mappings
    // -----------------------------------------------------------------------

    /**
     * Returns the full entitlement governance policy (attribute mappings) for
     * the specified application.
     */
    @GetMapping("/policy/{resourceId}/mappings")
    public ResponseEntity<EntitlementMappingPolicyDTO> getMappings(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader) {

        String tenantId = resolveTenantId(tenantIdHeader);

        logger.info().message("Retrieving entitlement governance policy")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .log();

        EntitlementMappingPolicyDTO policy = policyService.getPolicy(tenantId, resourceId);
        return ResponseEntity.ok(policy);
    }

    // -----------------------------------------------------------------------
    // PUT /policy/{resourceId}/mappings
    // -----------------------------------------------------------------------

    /**
     * Replaces all attribute mapping rules for the given application.
     *
     * <p>After a successful save the in-memory mapping cache for the resource is
     * immediately invalidated so the next sync run picks up the new rules.</p>
     */
    @PutMapping("/policy/{resourceId}/mappings")
    public ResponseEntity<EntitlementMappingPolicyDTO> updateMappings(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestBody EntitlementMappingPolicyDTO policyDto) {

        String tenantId = resolveTenantId(tenantIdHeader);

        logger.info().message("Updating entitlement governance policy")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .detail("mappingCount",
                        policyDto.attributeMappings() != null ? policyDto.attributeMappings().size() : 0)
                .log();

        // Capture state-before for audit diff
        EntitlementMappingPolicyDTO stateBefore = null;
        try {
            stateBefore = policyService.getPolicy(tenantId, resourceId);
        } catch (Exception ignored) {}

        EntitlementMappingPolicyDTO updated = policyService.updatePolicy(tenantId, resourceId, policyDto);

        // Invalidate the engine's cache so the next sync uses new rules
        mappingEngine.evictCacheForResource(tenantId, resourceId);

        publishAudit(
                AppConstants.AUDIT_EVENT_UPDATE,
                "ENTITLEMENT_ATTRIBUTE_MAPPING",
                resourceId.toString(),
                resourceId.toString(),
                tenantId,
                stateBefore,
                updated);

        return ResponseEntity.ok(updated);
    }

    // -----------------------------------------------------------------------
    // DELETE /policy/{resourceId}
    // -----------------------------------------------------------------------

    /**
     * Removes the entitlement governance policy for an application.
     * Typically invoked as part of application deletion.
     */
    @DeleteMapping("/policy/{resourceId}")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader) {

        String tenantId = resolveTenantId(tenantIdHeader);

        logger.info().message("Deleting entitlement governance policy")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .log();

        policyService.deletePoliciesByResource(tenantId, resourceId);
        mappingEngine.evictCacheForResource(tenantId, resourceId);

        publishAudit(
                AppConstants.AUDIT_EVENT_DELETE,
                "ENTITLEMENT_ATTRIBUTE_MAPPING",
                resourceId.toString(),
                resourceId.toString(),
                tenantId,
                null,
                null);

        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Entitlement schema endpoint
    // -----------------------------------------------------------------------

    /**
     * Returns the list of known target attribute names that the mapping engine
     * supports. Used by the frontend to populate the "Target Attribute" dropdown
     * when building mapping rules — identical UX to the identity schema attribute
     * selector.
     */
    @GetMapping("/policy/schema/attributes")
    public ResponseEntity<List<String>> getTargetAttributes(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader) {
        String tenantId = resolveTenantId(tenantIdHeader);

        LinkedHashSet<String> targets = new LinkedHashSet<>();
        targets.addAll(EntitlementMappingEngine.KNOWN_ENTITLEMENT_FIELDS);
        targets.addAll(entitlementSchemaService.getCustomAttributeKeys(tenantId));

        List<String> sortedTargets = targets.stream()
                .sorted()
                .toList();
        return ResponseEntity.ok(sortedTargets);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String resolveTenantId(String tenantIdHeader) {
        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            return tenantIdHeader;
        }
        return jwtTenantIdService.extractTenantIdFromRequest();
    }

    /** Fire-and-forget audit event; never throws so mutations are unaffected. */
    private void publishAudit(String action, String entityType, String entityId,
            String entityName, String tenantId, Object stateBefore, Object stateAfter) {
        try {
            AuditUser auditUser = securityContextService.getAuditUser();
            auditEventPublisher.publishEvent(IdentityAuditEvent.create(
                    auditUser.id(), auditUser.username(), auditUser.ipAddress(),
                    action, entityType, entityId, entityName, tenantId,
                    stateBefore != null ? objectMapper.valueToTree(stateBefore) : null,
                    stateAfter != null ? objectMapper.valueToTree(stateAfter) : null));
        } catch (Exception ex) {
            logger.warn().message("Failed to publish entitlement policy audit event: " + ex.getMessage()).log();
        }
    }
}
