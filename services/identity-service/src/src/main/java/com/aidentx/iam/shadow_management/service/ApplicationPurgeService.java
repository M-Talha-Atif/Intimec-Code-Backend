package com.aidentx.iam.shadow_management.service;

import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.repository.RuleExecutionDetailRepository;
import com.aidentx.iam.entitlement_management.repository.RuleExecutionHistoryRepository;
import com.aidentx.iam.identity_management.repository.IdentityShardRepository;
import com.aidentx.iam.identity_management.repository.ShardRepository;
import com.aidentx.iam.shadow_management.repository.ShadowAttributeChangelogRepository;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Purges all identity-service data associated with a deleted application.
 *
 * <p>Deletion is performed in FK-safe order:</p>
 * <ol>
 *   <li>rule_execution_details (FK → rule_execution_history)</li>
 *   <li>rule_execution_history (FK → entitlement_assignment_rules)</li>
 *   <li>entitlement_assignment_rules</li>
 *   <li>entitlement_assignments (FK → entitlements + shadows)</li>
 *   <li>entitlements</li>
 *   <li>shadow_attribute_changelog (FK → shadows)</li>
 *   <li>identity_shards</li>
 *   <li>shards</li>
 *   <li>shadows</li>
 * </ol>
 *
 * <p>Called by core-service via {@code InternalApplicationController} during application deletion.</p>
 */
@Service
public class ApplicationPurgeService {

    private final RuleExecutionDetailRepository ruleExecutionDetailRepository;
    private final RuleExecutionHistoryRepository ruleExecutionHistoryRepository;
    private final EntitlementAssignmentRuleRepository entitlementAssignmentRuleRepository;
    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final EntitlementRepository entitlementRepository;
    private final ShadowAttributeChangelogRepository shadowAttributeChangelogRepository;
    private final IdentityShardRepository identityShardRepository;
    private final ShardRepository shardRepository;
    private final ShadowRepository shadowRepository;
    private final IUnifiedLogger logger;

    public ApplicationPurgeService(
            RuleExecutionDetailRepository ruleExecutionDetailRepository,
            RuleExecutionHistoryRepository ruleExecutionHistoryRepository,
            EntitlementAssignmentRuleRepository entitlementAssignmentRuleRepository,
            EntitlementAssignmentRepository entitlementAssignmentRepository,
            EntitlementRepository entitlementRepository,
            ShadowAttributeChangelogRepository shadowAttributeChangelogRepository,
            IdentityShardRepository identityShardRepository,
            ShardRepository shardRepository,
            ShadowRepository shadowRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.ruleExecutionDetailRepository = ruleExecutionDetailRepository;
        this.ruleExecutionHistoryRepository = ruleExecutionHistoryRepository;
        this.entitlementAssignmentRuleRepository = entitlementAssignmentRuleRepository;
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.entitlementRepository = entitlementRepository;
        this.shadowAttributeChangelogRepository = shadowAttributeChangelogRepository;
        this.identityShardRepository = identityShardRepository;
        this.shardRepository = shardRepository;
        this.shadowRepository = shadowRepository;
        this.logger = loggerFactory.getLogger(ApplicationPurgeService.class);
    }

    /**
     * Deletes all identity-service data associated with the given application (resourceId).
     *
     * @param resourceId the application UUID as String
     * @param tenantId   the tenant context
     */
    @Transactional
    public void purgeApplicationData(String resourceId, String tenantId) {
        UUID resourceUuid = UUID.fromString(resourceId);

        logger.info()
                .message("Starting identity-service purge for application")
                .detail("resourceId", resourceId)
                .detail("tenantId", tenantId)
                .log();

        // 1. rule_execution_details — must go before rule_execution_history (FK constraint)
        ruleExecutionDetailRepository.deleteAllByResourceIdAndTenantId(resourceId, tenantId);

        // 2. rule_execution_history — must go before entitlement_assignment_rules
        ruleExecutionHistoryRepository.deleteAllByResourceIdAndTenantId(resourceUuid, tenantId);

        // 3. entitlement_assignment_rules
        entitlementAssignmentRuleRepository.deleteAllByResourceIdAndTenantId(resourceUuid, tenantId);

        // 4. entitlement_assignments — must go before entitlements and shadows
        entitlementAssignmentRepository.deleteAllByResourceIdAndTenantId(resourceUuid, tenantId);

        // 5. entitlements
        entitlementRepository.deleteAllByResourceIdAndTenantId(resourceUuid, tenantId);

        // 6. shadow_attribute_changelog — must go before shadows
        shadowAttributeChangelogRepository.deleteAllByResourceIdAndTenantId(resourceId, tenantId);

        // 7. identity_shards (no tenantId on this entity)
        identityShardRepository.deleteAllByResourceId(resourceUuid);

        // 8. shards
        shardRepository.deleteAllByTenantIdAndResourceId(tenantId, resourceId);

        // 9. shadows — last, after all dependents are removed
        shadowRepository.deleteAllByResourceIdAndTenantId(resourceId, tenantId);

        logger.info()
                .message("Completed identity-service purge for application")
                .detail("resourceId", resourceId)
                .detail("tenantId", tenantId)
                .log();
    }
}
