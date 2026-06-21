package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.mapper.AccessRoleAuditLogMapper;
import com.aidentx.iam.access_roles_management.model.dto.AccessRoleAuditHistoryDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAuditLogRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAuditLogServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleAuditLogRepository repository;

    @Mock
    private AccessRoleAuditLogMapper mapper;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleAuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleAuditLogServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleAuditLogServiceImpl(repository, mapper, loggerFactory);
    }

    @Test
    void findAuditHistoryWithSearchUsesRoleAndActorSearchQuery() {
        UUID roleId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        AccessRoleAuditLog auditLog = new AccessRoleAuditLog();
        AccessRoleAuditHistoryDto historyDto = new AccessRoleAuditHistoryDto();
        historyDto.setActorUsername("imadmin");

        when(repository.searchByTenantIdAndRoleIdAndActor(TENANT_ID, roleId, "imadmin", pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(mapper.entityToHistoryDto(auditLog)).thenReturn(historyDto);

        Page<AccessRoleAuditHistoryDto> result = service.findAuditHistory(
                TENANT_ID,
                roleId,
                null,
                " imadmin ",
                pageable);

        verify(repository).searchByTenantIdAndRoleIdAndActor(TENANT_ID, roleId, "imadmin", pageable);
        assertThat(result.getContent()).singleElement()
                .extracting(AccessRoleAuditHistoryDto::getActorUsername)
                .isEqualTo("imadmin");
    }

    @Test
    void findAuditHistoryWithActionAndSearchUsesCombinedRoleQuery() {
        UUID roleId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        AccessRoleAuditLog auditLog = new AccessRoleAuditLog();
        AccessRoleAuditHistoryDto historyDto = new AccessRoleAuditHistoryDto();

        when(repository.searchByTenantIdAndRoleIdAndActionAndActor(
                TENANT_ID,
                roleId,
                "ROLE_CREATED",
                "imadmin",
                pageable)).thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(mapper.entityToHistoryDto(auditLog)).thenReturn(historyDto);

        Page<AccessRoleAuditHistoryDto> result = service.findAuditHistory(
                TENANT_ID,
                roleId,
                " ROLE_CREATED ",
                "imadmin",
                pageable);

        verify(repository).searchByTenantIdAndRoleIdAndActionAndActor(
                TENANT_ID,
                roleId,
                "ROLE_CREATED",
                "imadmin",
                pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findRuleAuditHistoryWithoutActionUsesRuleQueryAndMapsRuleFields() {
        UUID ruleId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        AccessRoleAuditLog auditLog = new AccessRoleAuditLog();
        auditLog.setRuleId(ruleId);
        auditLog.setRuleName("Dynamic Rule - Finance Manager");

        AccessRoleAuditHistoryDto historyDto = new AccessRoleAuditHistoryDto();
        historyDto.setRuleId(ruleId);
        historyDto.setRuleName("Dynamic Rule - Finance Manager");

        when(repository.findByTenantIdAndRuleId(TENANT_ID, ruleId, pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(mapper.entityToHistoryDto(auditLog)).thenReturn(historyDto);

        Page<AccessRoleAuditHistoryDto> result = service.findRuleAuditHistory(TENANT_ID, ruleId, null, null, pageable);

        verify(repository).findByTenantIdAndRuleId(TENANT_ID, ruleId, pageable);
        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getRuleId()).isEqualTo(ruleId);
            assertThat(dto.getRuleName()).isEqualTo("Dynamic Rule - Finance Manager");
        });
    }

    @Test
    void findRuleAuditHistoryWithActionUsesRuleAndActionQuery() {
        UUID ruleId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        AccessRoleAuditLog auditLog = new AccessRoleAuditLog();
        AccessRoleAuditHistoryDto historyDto = new AccessRoleAuditHistoryDto();

        when(repository.findByTenantIdAndRuleIdAndAction(TENANT_ID, ruleId, "ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED", pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(mapper.entityToHistoryDto(auditLog)).thenReturn(historyDto);

        Page<AccessRoleAuditHistoryDto> result = service.findRuleAuditHistory(
                TENANT_ID,
                ruleId,
                "ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED",
                null,
                pageable);

        verify(repository).findByTenantIdAndRuleIdAndAction(TENANT_ID, ruleId, "ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED", pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findRuleAuditHistoryWithSearchUsesRuleAndActorSearchQuery() {
        UUID ruleId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        AccessRoleAuditLog auditLog = new AccessRoleAuditLog();
        AccessRoleAuditHistoryDto historyDto = new AccessRoleAuditHistoryDto();
        historyDto.setActorUsername("admin.user@example.com");

        when(repository.searchByTenantIdAndRuleIdAndActor(TENANT_ID, ruleId, "admin", pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(mapper.entityToHistoryDto(auditLog)).thenReturn(historyDto);

        Page<AccessRoleAuditHistoryDto> result = service.findRuleAuditHistory(
                TENANT_ID,
                ruleId,
                null,
                " admin ",
                pageable);

        verify(repository).searchByTenantIdAndRuleIdAndActor(TENANT_ID, ruleId, "admin", pageable);
        assertThat(result.getContent()).singleElement()
                .extracting(AccessRoleAuditHistoryDto::getActorUsername)
                .isEqualTo("admin.user@example.com");
    }

    @Test
    void findRuleAuditHistoryWithActionAndSearchUsesCombinedQuery() {
        UUID ruleId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        AccessRoleAuditLog auditLog = new AccessRoleAuditLog();
        AccessRoleAuditHistoryDto historyDto = new AccessRoleAuditHistoryDto();

        when(repository.searchByTenantIdAndRuleIdAndActionAndActor(
                TENANT_ID,
                ruleId,
                "ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED",
                "talha",
                pageable)).thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));
        when(mapper.entityToHistoryDto(auditLog)).thenReturn(historyDto);

        Page<AccessRoleAuditHistoryDto> result = service.findRuleAuditHistory(
                TENANT_ID,
                ruleId,
                " ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED ",
                "talha",
                pageable);

        verify(repository).searchByTenantIdAndRuleIdAndActionAndActor(
                TENANT_ID,
                ruleId,
                "ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED",
                "talha",
                pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}

