package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEvaluationTransactionHelperTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntitlementAssignmentRuleRepository ruleRepository;

    @Mock
    private IdentityFieldRegistry fieldRegistry;

    @Mock
    private EntitlementAssignmentRuleNormalizationService normalizationService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    @Mock
    private Query query;

    private RuleEvaluationTransactionHelper helper;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(RuleEvaluationTransactionHelper.class), anyString())).thenReturn(logger);
        helper = new RuleEvaluationTransactionHelper(
                entityManager,
                ruleRepository,
                fieldRegistry,
                normalizationService,
                loggerFactory);
    }

    @Test
    void evaluateDisabledRuleRevokesDynamicAssignmentsForEntitlementWithoutAssigning() {
        UUID ruleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        EntitlementAssignmentRule rule = rule(ruleId, entitlementId, false);
        ExecutionHistoryCollector collector = collector(rule);

        when(ruleRepository.findByTenantIdAndEntitlementIdAndEnabledTrueOrderByPriorityAsc(TENANT_ID, entitlementId))
                .thenReturn(List.of());
        when(entityManager.createNativeQuery(RuleEvaluationConstants.NATIVE_REVOKE_ALL_FOR_ENTITLEMENT)).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(3);
        when(ruleRepository.save(rule)).thenReturn(rule);

        RuleEvaluationResult result = helper.evaluateRuleInTransaction(TENANT_ID, rule, collector);

        assertThat(result.getIdentitiesMatched()).isZero();
        assertThat(result.getAssignmentsCreated()).isZero();
        assertThat(result.getAssignmentsRevoked()).isEqualTo(3);
        verify(entityManager).createNativeQuery(RuleEvaluationConstants.NATIVE_REVOKE_ALL_FOR_ENTITLEMENT);
        verify(query).setParameter("tenantId", TENANT_ID);
        verify(query).setParameter("entitlementId", entitlementId);
        verify(normalizationService, never()).normalizeIdentityTypes(any(), eq(true));
        verify(ruleRepository).save(rule);
    }

    @Test
    void revokeDynamicAssignmentsForEntitlementKeepsAssignmentsForMatchedIdentities() {
        UUID entitlementId = UUID.randomUUID();
        UUID identityOneId = UUID.randomUUID();
        UUID identityTwoId = UUID.randomUUID();

        when(entityManager.createNativeQuery(RuleEvaluationConstants.NATIVE_BULK_REVOKE)).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        int revoked = helper.revokeDynamicAssignmentsForEntitlement(
                TENANT_ID, entitlementId, Set.of(identityOneId, identityTwoId));

        assertThat(revoked).isEqualTo(1);
        verify(entityManager).createNativeQuery(RuleEvaluationConstants.NATIVE_BULK_REVOKE);
        verify(query).setParameter("tenantId", TENANT_ID);
        verify(query).setParameter("entitlementId", entitlementId);
        ArgumentCaptor<List<UUID>> matchedIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(query).setParameter(eq("matchedIdentityIds"), matchedIdsCaptor.capture());
        assertThat(matchedIdsCaptor.getValue()).containsExactlyInAnyOrder(identityOneId, identityTwoId);
    }

    private ExecutionHistoryCollector collector(EntitlementAssignmentRule rule) {
        return new ExecutionHistoryCollector(
                TENANT_ID,
                rule.getId(),
                rule.getName(),
                rule.getEntitlementId(),
                "Entitlement",
                rule.getResourceId(),
                RuleExecutionHistory.TriggerType.MANUAL,
                "tester",
                rule.getConditions(),
                rule.getIdentityTypes());
    }

    private EntitlementAssignmentRule rule(UUID ruleId, UUID entitlementId, boolean enabled) {
        EntitlementAssignmentRule rule = new EntitlementAssignmentRule();
        rule.setId(ruleId);
        rule.setTenantId(TENANT_ID);
        rule.setName("Dynamic Rule");
        rule.setEntitlementId(entitlementId);
        rule.setResourceId(UUID.randomUUID());
        rule.setEnabled(enabled);
        rule.setIdentityTypes(List.of("EMPLOYEE"));
        rule.setConditions(List.of());
        return rule;
    }
}

