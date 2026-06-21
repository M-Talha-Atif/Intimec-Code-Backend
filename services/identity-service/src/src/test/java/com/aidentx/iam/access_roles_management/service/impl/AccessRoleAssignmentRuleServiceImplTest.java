package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.mapper.AccessRoleAssignmentRuleMapper;
import com.aidentx.iam.access_roles_management.event.AccessRoleAssignmentRuleEvaluationEvent;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleGlobalScheduleUpdateRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleConfigResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleGlobalScheduleUpdateResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRuleResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRuleGlobalScheduleConfig;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleGlobalScheduleConfigRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleNormalizationService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentRuleServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleAssignmentRuleRepository ruleRepository;

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private AccessRoleAssignmentRuleMapper mapper;

    @Mock
    private com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry fieldRegistry;

    @Mock
    private AccessRoleAssignmentRuleNormalizationService normalizationService;

    @Mock
    private AccessRoleAssignmentRuleGlobalScheduleConfigRepository globalScheduleConfigRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    @Mock
    private EntityManager entityManager;

    private AccessRoleAssignmentRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAssignmentRuleServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleAssignmentRuleServiceImpl(
                ruleRepository,
                accessRoleRepository,
                mapper,
                fieldRegistry,
                normalizationService,
                globalScheduleConfigRepository,
                eventPublisher,
                loggerFactory,
                entityManager);
    }

    @Test
    void createRuleInheritsPersistedGlobalScheduleWhenRequestScheduleIsPresent() {
        UUID roleId = UUID.randomUUID();
        AccessRole role = role(roleId);
        AccessRoleAssignmentRuleGlobalScheduleConfig config = new AccessRoleAssignmentRuleGlobalScheduleConfig();
        config.setTenantId(TENANT_ID);
        config.setScheduleType(AccessRoleAssignmentRule.ScheduleType.CRON.name());
        config.setScheduleTime("02:00");
        config.setScheduleTimezone("UTC");

        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(ruleRepository.existsByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(false);
        when(normalizationService.normalizeIdentityTypes(any(), eq(true))).thenReturn(List.of("EMPLOYEE"));
        when(normalizationService.normalizeConditionGroups(any(), any())).thenReturn(List.of(Map.of("groupOperator", "AND")));
        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(config));
        when(ruleRepository.save(any(AccessRoleAssignmentRule.class))).thenAnswer(invocation -> {
            AccessRoleAssignmentRule saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(mapper.toResponse(any(AccessRoleAssignmentRule.class), eq(role))).thenAnswer(invocation -> {
            AccessRoleAssignmentRule saved = invocation.getArgument(0);
            AccessRoleAssignmentRuleResponse response = new AccessRoleAssignmentRuleResponse();
            response.setId(saved.getId());
            response.setRoleId(saved.getRoleId());
            response.setScheduleType(saved.getScheduleType().name());
            response.setScheduleTime(saved.getScheduleTime());
            response.setScheduleTimezone(saved.getScheduleTimezone());
            response.setNextExecutionAt(saved.getNextExecutionAt());
            return response;
        });

        CreateAccessRoleAssignmentRuleRequest request = new CreateAccessRoleAssignmentRuleRequest();
        request.setRoleId(roleId);
        request.setEnabled(true);
        request.setScheduleType("CRON");

        AccessRoleAssignmentRuleResponse response = service.createRule(TENANT_ID, request, "creator@example.com");

        assertThat(response.getScheduleType()).isEqualTo("CRON");
        assertThat(response.getScheduleTime()).isEqualTo("02:00");
        assertThat(response.getScheduleTimezone()).isEqualTo("UTC");
        assertThat(response.getNextExecutionAt()).isNotNull();
        verify(globalScheduleConfigRepository).findByTenantId(TENANT_ID);
    }

    @Test
    void createRulePublishesEvaluationEventAfterSave() {
        UUID roleId = UUID.randomUUID();
        AccessRole role = role(roleId);

        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(ruleRepository.existsByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(false);
        when(normalizationService.normalizeIdentityTypes(any(), eq(true))).thenReturn(List.of("EMPLOYEE"));
        when(normalizationService.normalizeConditionGroups(any(), any())).thenReturn(List.of(Map.of("groupOperator", "AND")));
        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(ruleRepository.save(any(AccessRoleAssignmentRule.class))).thenAnswer(invocation -> {
            AccessRoleAssignmentRule saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(mapper.toResponse(any(AccessRoleAssignmentRule.class), eq(role))).thenReturn(new AccessRoleAssignmentRuleResponse());

        CreateAccessRoleAssignmentRuleRequest request = new CreateAccessRoleAssignmentRuleRequest();
        request.setRoleId(roleId);

        service.createRule(TENANT_ID, request, "creator@example.com");

        ArgumentCaptor<AccessRoleAssignmentRuleEvaluationEvent> eventCaptor =
                ArgumentCaptor.forClass(AccessRoleAssignmentRuleEvaluationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(eventCaptor.getValue().getTriggerType())
                .isEqualTo(AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_CREATED);
        assertThat(eventCaptor.getValue().getTriggeredBy()).isEqualTo("creator@example.com");
        assertThat(eventCaptor.getValue().getScheduleType()).isEqualTo("MANUAL");
    }

    @Test
    void updateRulePublishesEvaluationEventAfterSave() {
        UUID roleId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        AccessRole role = role(roleId);
        AccessRoleAssignmentRule existing = new AccessRoleAssignmentRule();
        existing.setId(ruleId);
        existing.setTenantId(TENANT_ID);
        existing.setRoleId(roleId);
        existing.setEnabled(true);
        existing.setScheduleType(AccessRoleAssignmentRule.ScheduleType.MANUAL);
        existing.setIdentityTypes(List.of("EMPLOYEE"));
        existing.setConditionGroups(List.of(Map.of("groupOperator", "AND")));

        when(ruleRepository.findByIdAndTenantId(ruleId, TENANT_ID)).thenReturn(Optional.of(existing));
        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(ruleRepository.save(any(AccessRoleAssignmentRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(AccessRoleAssignmentRule.class), eq(role))).thenReturn(new AccessRoleAssignmentRuleResponse());

        UpdateAccessRoleAssignmentRuleRequest request = new UpdateAccessRoleAssignmentRuleRequest();
        request.setEnabled(false);

        service.updateRule(TENANT_ID, ruleId, request, "updater@example.com");

        ArgumentCaptor<AccessRoleAssignmentRuleEvaluationEvent> eventCaptor =
                ArgumentCaptor.forClass(AccessRoleAssignmentRuleEvaluationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRuleId()).isEqualTo(ruleId);
        assertThat(eventCaptor.getValue().getTriggerType())
                .isEqualTo(AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_UPDATED);
        assertThat(eventCaptor.getValue().getTriggeredBy()).isEqualTo("updater@example.com");
        assertThat(eventCaptor.getValue().getScheduleType()).isEqualTo("MANUAL");
    }

    @Test
    void updateRuleIgnoresRequestScheduleAndUsesGlobalSchedule() {
        UUID roleId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        AccessRole role = role(roleId);
        AccessRoleAssignmentRule existing = new AccessRoleAssignmentRule();
        existing.setId(ruleId);
        existing.setTenantId(TENANT_ID);
        existing.setRoleId(roleId);
        existing.setEnabled(true);
        existing.setScheduleType(AccessRoleAssignmentRule.ScheduleType.MANUAL);
        existing.setIdentityTypes(List.of("EMPLOYEE"));
        existing.setConditionGroups(List.of(Map.of("groupOperator", "AND")));

        AccessRoleAssignmentRuleGlobalScheduleConfig config = new AccessRoleAssignmentRuleGlobalScheduleConfig();
        config.setTenantId(TENANT_ID);
        config.setScheduleType(AccessRoleAssignmentRule.ScheduleType.CRON.name());
        config.setScheduleTime("03:15");
        config.setScheduleTimezone("UTC");

        when(ruleRepository.findByIdAndTenantId(ruleId, TENANT_ID)).thenReturn(Optional.of(existing));
        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(config));
        when(ruleRepository.save(any(AccessRoleAssignmentRule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(AccessRoleAssignmentRule.class), eq(role))).thenAnswer(invocation -> {
            AccessRoleAssignmentRule saved = invocation.getArgument(0);
            AccessRoleAssignmentRuleResponse response = new AccessRoleAssignmentRuleResponse();
            response.setScheduleType(saved.getScheduleType().name());
            response.setScheduleTime(saved.getScheduleTime());
            response.setScheduleTimezone(saved.getScheduleTimezone());
            return response;
        });

        UpdateAccessRoleAssignmentRuleRequest request = new UpdateAccessRoleAssignmentRuleRequest();
        request.setScheduleType("CRON");

        AccessRoleAssignmentRuleResponse response = service.updateRule(TENANT_ID, ruleId, request, "updater@example.com");

        ArgumentCaptor<AccessRoleAssignmentRule> savedRuleCaptor =
                ArgumentCaptor.forClass(AccessRoleAssignmentRule.class);
        verify(ruleRepository).save(savedRuleCaptor.capture());
        assertThat(savedRuleCaptor.getValue().getScheduleType()).isEqualTo(AccessRoleAssignmentRule.ScheduleType.CRON);
        assertThat(savedRuleCaptor.getValue().getScheduleTime()).isEqualTo("03:15");
        assertThat(savedRuleCaptor.getValue().getScheduleTimezone()).isEqualTo("UTC");
        assertThat(response.getScheduleType()).isEqualTo("CRON");
        assertThat(response.getScheduleTime()).isEqualTo("03:15");
        assertThat(response.getScheduleTimezone()).isEqualTo("UTC");
    }

    @Test
    void updateGlobalScheduleTypePersistsConfigAndRecalculatesCronRules() {
        AccessRoleAssignmentRuleGlobalScheduleUpdateRequest request = new AccessRoleAssignmentRuleGlobalScheduleUpdateRequest();
        request.setScheduleType("CRON");
        request.setScheduleTime("01:30");
        request.setScheduleTimezone("UTC");

        AccessRoleAssignmentRule cronRule = new AccessRoleAssignmentRule();
        cronRule.setId(UUID.randomUUID());
        cronRule.setTenantId(TENANT_ID);
        cronRule.setEnabled(true);
        cronRule.setScheduleType(AccessRoleAssignmentRule.ScheduleType.CRON);
        cronRule.setScheduleTime("01:30");
        cronRule.setScheduleTimezone("UTC");

        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(ruleRepository.updateGlobalScheduleForAllRules(
                eq(TENANT_ID),
                eq(AccessRoleAssignmentRule.ScheduleType.CRON),
                eq(null),
                eq("01:30"),
                eq(null),
                eq("UTC"),
                anyLong())).thenReturn(3);
        when(ruleRepository.findByTenantIdAndScheduleTypeOrderByPriorityAsc(
                TENANT_ID,
                AccessRoleAssignmentRule.ScheduleType.CRON)).thenReturn(List.of(cronRule));
        when(ruleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccessRoleAssignmentRuleGlobalScheduleUpdateResponse response = service.updateGlobalScheduleType(TENANT_ID, request);

        ArgumentCaptor<AccessRoleAssignmentRuleGlobalScheduleConfig> configCaptor =
                ArgumentCaptor.forClass(AccessRoleAssignmentRuleGlobalScheduleConfig.class);
        verify(globalScheduleConfigRepository).save(configCaptor.capture());
        assertThat(configCaptor.getValue().getScheduleType()).isEqualTo("CRON");
        assertThat(configCaptor.getValue().getScheduleTime()).isEqualTo("01:30");
        assertThat(configCaptor.getValue().getScheduleTimezone()).isEqualTo("UTC");
        assertThat(response.getRulesUpdated()).isEqualTo(3);
        assertThat(response.getScheduleType()).isEqualTo("CRON");
        assertThat(response.getNextExecutionPreview()).isNotBlank();
        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(ruleRepository).saveAll(any());
    }

    @Test
    void updateGlobalScheduleTypeClearsNextExecutionsForManualSchedule() {
        AccessRoleAssignmentRuleGlobalScheduleUpdateRequest request = new AccessRoleAssignmentRuleGlobalScheduleUpdateRequest();
        request.setScheduleType("MANUAL");

        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(ruleRepository.updateGlobalScheduleForAllRules(
                eq(TENANT_ID),
                eq(AccessRoleAssignmentRule.ScheduleType.MANUAL),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                anyLong())).thenReturn(4);
        when(ruleRepository.bulkUpdateNextExecutionAt(eq(TENANT_ID), eq(null), anyLong())).thenReturn(4);

        AccessRoleAssignmentRuleGlobalScheduleUpdateResponse response = service.updateGlobalScheduleType(TENANT_ID, request);

        assertThat(response.getScheduleType()).isEqualTo("MANUAL");
        assertThat(response.getRulesUpdated()).isEqualTo(4);
        assertThat(response.getNextExecutionPreview()).isNull();
        verify(ruleRepository).bulkUpdateNextExecutionAt(eq(TENANT_ID), eq(null), anyLong());
        verify(ruleRepository, never()).findByTenantIdAndScheduleTypeOrderByPriorityAsc(anyString(), any());
    }

    @Test
    void getGlobalScheduleConfigReturnsManualDefaultWhenNothingPersisted() {
        when(globalScheduleConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        AccessRoleAssignmentRuleGlobalScheduleConfigResponse response = service.getGlobalScheduleConfig(TENANT_ID);

        assertThat(response.getScheduleType()).isEqualTo("MANUAL");
        assertThat(response.getCronExpression()).isNull();
        assertThat(response.getScheduleTime()).isNull();
        assertThat(response.getScheduleDate()).isNull();
        assertThat(response.getScheduleTimezone()).isNull();
    }

    private AccessRole role(UUID roleId) {
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Role");
        return role;
    }
}

