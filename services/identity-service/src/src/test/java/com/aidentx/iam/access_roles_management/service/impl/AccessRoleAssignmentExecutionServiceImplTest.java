package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAssignmentExecutionAuditService;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecutionItem;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionItemRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentExecutionServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleAssignmentRuleRepository ruleRepository;

    @Mock
    private AccessRoleAssignmentExecutionRepository executionRepository;

    @Mock
    private AccessRoleAssignmentExecutionItemRepository executionItemRepository;

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private IdentityRoleAssignmentRepository assignmentRepository;

    @Mock
    private IdentityRoleAssignmentService assignmentService;

    @Mock
    private IdentityFieldRegistry fieldRegistry;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AccessRoleAssignmentExecutionAuditService executionAuditService;

    private AccessRoleAssignmentExecutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccessRoleAssignmentExecutionServiceImpl(
                ruleRepository,
                executionRepository,
                executionItemRepository,
                accessRoleRepository,
                assignmentRepository,
                assignmentService,
                fieldRegistry,
                entityManager,
                executionAuditService);
    }

    @Test
    void assignMatchedIdentityStillInvokesAssignmentServiceWhenRoleIsAlreadyActive() throws Exception {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        String sourceKey = ruleId.toString();

        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setId(ruleId);
        rule.setRoleId(roleId);

        IdentityRoleAssignment existing = new IdentityRoleAssignment();
        existing.setId(UUID.randomUUID());
        existing.setTenantId(TENANT_ID);
        existing.setIdentityId(identityId);
        existing.setRoleId(roleId);
        existing.setStatus(IdentityRoleAssignment.Status.ACTIVE);
        existing.setSourceType(IdentityRoleAssignment.SourceType.RULE);
        existing.setSourceKey(sourceKey);

        when(assignmentRepository.findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                TENANT_ID,
                identityId,
                roleId,
                IdentityRoleAssignment.SourceType.RULE,
                sourceKey)).thenReturn(Optional.of(existing));
        when(assignmentService.assignRole(
                TENANT_ID,
                identityId,
                roleId,
                IdentityRoleAssignment.SourceType.RULE,
                ruleId,
                sourceKey)).thenReturn(existing);

        Method method = AccessRoleAssignmentExecutionServiceImpl.class.getDeclaredMethod(
                "assignMatchedIdentity",
                String.class,
                AccessRoleAssignmentRule.class,
                UUID.class);
        method.setAccessible(true);

        AccessRoleAssignmentExecutionItem.Outcome outcome =
                (AccessRoleAssignmentExecutionItem.Outcome) method.invoke(service, TENANT_ID, rule, identityId);

        assertThat(outcome).isEqualTo(AccessRoleAssignmentExecutionItem.Outcome.ALREADY_ASSIGNED);
        verify(assignmentService).assignRole(
                TENANT_ID,
                identityId,
                roleId,
                IdentityRoleAssignment.SourceType.RULE,
                ruleId,
                sourceKey);
    }

    @Test
    void executeRuleFailureStillAdvancesCronSchedulingMetadata() {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setId(ruleId);
        rule.setTenantId(TENANT_ID);
        rule.setRoleId(roleId);
        rule.setEnabled(true);
        rule.setScheduleType(AccessRoleAssignmentRule.ScheduleType.CRON);
        rule.setScheduleTime("23:59");
        rule.setScheduleTimezone("UTC");

        when(ruleRepository.findByIdAndTenantId(ruleId, TENANT_ID)).thenReturn(Optional.of(rule));
        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.empty());
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleRepository.save(any(AccessRoleAssignmentRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccessRoleAssignmentExecution execution = service.executeRule(
                TENANT_ID,
                ruleId,
                com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution.TriggerType.CRON,
                "System");

        ArgumentCaptor<AccessRoleAssignmentRule> ruleCaptor = ArgumentCaptor.forClass(AccessRoleAssignmentRule.class);
        verify(ruleRepository).save(ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().getLastEvaluatedAt()).isNotNull();
        assertThat(ruleCaptor.getValue().getNextExecutionAt()).isNotNull();
        assertThat(rule.getLastEvaluatedAt()).isNotNull();
        assertThat(rule.getNextExecutionAt()).isNotNull();
        verify(executionAuditService).publishSingleExecutionAudit(TENANT_ID, execution, ruleId, "System");
    }

    @Test
    void executeRuleManualDoesNotPublishServiceOwnedExecutionAudit() {
        UUID ruleId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        AccessRoleAssignmentRule rule = new AccessRoleAssignmentRule();
        rule.setId(ruleId);
        rule.setTenantId(TENANT_ID);
        rule.setRoleId(roleId);
        rule.setEnabled(true);
        rule.setScheduleType(AccessRoleAssignmentRule.ScheduleType.MANUAL);

        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");

        when(ruleRepository.findByIdAndTenantId(ruleId, TENANT_ID)).thenReturn(Optional.of(rule));
        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleRepository.save(any(AccessRoleAssignmentRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.executeRule(
                TENANT_ID,
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.MANUAL,
                "john.doe");

        verify(executionAuditService, org.mockito.Mockito.never())
                .publishSingleExecutionAudit(org.mockito.Mockito.anyString(), org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void listExecutionsByRolePassesFiltersToRepository() {
        UUID roleId = UUID.randomUUID();
        Page<AccessRoleAssignmentExecution> expected = new PageImpl<>(List.of(new AccessRoleAssignmentExecution()));

        when(executionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expected);

        Page<AccessRoleAssignmentExecution> actual = service.listExecutionsByRole(
                TENANT_ID,
                roleId,
                AccessRoleAssignmentExecution.TriggerType.CRON,
                AccessRoleAssignmentExecution.ExecutionStatus.SUCCESS,
                100L,
                200L,
                1,
                25);

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<Specification<AccessRoleAssignmentExecution>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(executionRepository).findAll(
                specificationCaptor.capture(),
                pageableCaptor.capture());
        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startedAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startedAt").isDescending()).isTrue();
    }

    @Test
    void listExecutionsByRoleAllowsNullOptionalFilters() {
        UUID roleId = UUID.randomUUID();
        when(executionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<AccessRoleAssignmentExecution> actual = service.listExecutionsByRole(
                TENANT_ID,
                roleId,
                null,
                null,
                null,
                null,
                0,
                20);

        assertThat(actual).isEmpty();
        verify(executionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listExecutionsByRoleRejectsInvalidStartedAtRange() {
        assertThatThrownBy(() -> service.listExecutionsByRole(
                TENANT_ID,
                UUID.randomUUID(),
                null,
                null,
                200L,
                100L,
                0,
                20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startedFrom must be less than or equal to startedTo");
    }
}

