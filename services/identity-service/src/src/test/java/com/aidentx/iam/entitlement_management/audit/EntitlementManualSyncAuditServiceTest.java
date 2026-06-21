package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentResult;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncEntitlementMemberFailure;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncEntitlementMemberResult;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersResponse;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;
import com.aidentx.iam.entitlement_management.repository.EntitlementAuditLogRepository;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementManualSyncAuditServiceTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private EntitlementAuditLogRepository auditLogRepository;

    @Mock
    private EntitlementManualSyncAuditPublisher publisher;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private EntitlementManualSyncAuditService auditService;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(EntitlementManualSyncAuditService.class), anyString())).thenReturn(logger);
        auditService = new EntitlementManualSyncAuditService(
                auditLogRepository,
                publisher,
                identityRepository,
                objectMapper,
                loggerFactory);
    }

    @Test
    void auditManualSyncPersistsAndPublishesMembersRiskUsernameAndFriendlyEntitlementName() {
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID addedIdentityId = UUID.randomUUID();
        UUID revokedIdentityId = UUID.randomUUID();
        UUID skippedIdentityId = UUID.randomUUID();
        UUID failedIdentityId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setResourceId(resourceId);
        entitlement.setDisplayName("ENTITLEMENT_HELLO");
        entitlement.setRiskLevel("low");

        SyncManualEntitlementMembersResponse response = new SyncManualEntitlementMembersResponse(
                entitlementId,
                2,
                1,
                1,
                1,
                1,
                2,
                List.of(new SyncEntitlementMemberResult(addedIdentityId, List.of(
                        new ManualEntitlementAssignmentResult(
                                addedIdentityId, null, assignmentId, "PENDING", "No account", "CREATED")))),
                List.of(new SyncEntitlementMemberResult(revokedIdentityId, List.of(
                        new ManualEntitlementAssignmentResult(
                                revokedIdentityId, UUID.randomUUID(), UUID.randomUUID(), "ASSIGNED", null, "REVOKED")))),
                List.of(new SyncEntitlementMemberResult(skippedIdentityId, List.of())),
                List.of(new SyncEntitlementMemberFailure(failedIdentityId, "Identity not found")));

        Employee actor = employee(actorId, "ALICE_ADMIN");
        NonHumanIdentity added = nonHumanIdentity(addedIdentityId, "JOHN_DOE", "high");
        Employee revoked = employee(revokedIdentityId, "JANE_DOE");
        Employee skipped = employee(skippedIdentityId, "SAM_SKIP");
        Employee failed = employee(failedIdentityId, "FAY_FAIL");

        when(identityRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection()))
                .thenReturn(List.of(added, revoked, skipped, failed));
        when(identityRepository.findByTenantIdAndId(TENANT_ID, actorId)).thenReturn(Optional.of(actor));
        when(auditLogRepository.save(any(EntitlementAuditLog.class))).thenAnswer(invocation -> {
            EntitlementAuditLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        auditService.auditManualSync(TENANT_ID, entitlement, response, actorId.toString());

        ArgumentCaptor<EntitlementAuditLog> logCaptor = ArgumentCaptor.forClass(EntitlementAuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        EntitlementAuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(savedLog.getEntitlementId()).isEqualTo(entitlementId);
        assertThat(savedLog.getEntitlementName()).isEqualTo("Entitlement Hello");
        assertThat(savedLog.getActorId()).isEqualTo(actorId.toString());
        assertThat(savedLog.getActorUsername()).isEqualTo("Alice Admin");
        assertThat(savedLog.getAction()).isEqualTo(EntitlementAuditConstants.ACTION_MEMBERS_MANUALLY_SYNCED);
        assertThat(savedLog.getAssignmentsCreated()).isEqualTo(1);
        assertThat(savedLog.getAssignmentsRevoked()).isEqualTo(1);

        JsonNode stateAfter = objectMapper.valueToTree(savedLog.getStateAfter());
        assertThat(stateAfter.get(EntitlementAuditConstants.DETAIL_RISK_LEVEL).asText()).isEqualTo("high");
        assertThat(stateAfter.get(EntitlementAuditConstants.DETAIL_ADDED_MEMBERS).get(0).get("identityName").asText())
                .isEqualTo("John Doe");
        assertThat(stateAfter.get(EntitlementAuditConstants.DETAIL_ADDED_MEMBERS).get(0).get("assignments").get(0).get("shadowId").isNull())
                .isTrue();
        assertThat(stateAfter.get(EntitlementAuditConstants.DETAIL_REMOVED_MEMBERS)).hasSize(1);
        assertThat(stateAfter.get(EntitlementAuditConstants.DETAIL_FAILED_MEMBERS).get(0).get("reason").asText())
                .isEqualTo("Identity not found");

        ArgumentCaptor<EntitlementManualSyncAuditEvent> eventCaptor = ArgumentCaptor.forClass(EntitlementManualSyncAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        EntitlementManualSyncAuditEvent event = eventCaptor.getValue();
        assertThat(event.actorUsername()).isEqualTo("Alice Admin");
        assertThat(event.entitlementName()).isEqualTo("Entitlement Hello");
        assertThat(event.riskLevel()).isEqualTo("high");
        assertThat(event.addedMembers()).singleElement().satisfies(member ->
                assertThat(member.identityName()).isEqualTo("John Doe"));
        assertThat(event.removedMembers()).hasSize(1);
        assertThat(event.skippedMembers()).hasSize(1);
        assertThat(event.failedMembers()).hasSize(1);
    }

    @Test
    void auditManualSyncUsesMediumWhenAllAffectedIdentityRisksAreMedium() {
        UUID entitlementId = UUID.randomUUID();
        UUID firstIdentityId = UUID.randomUUID();
        UUID secondIdentityId = UUID.randomUUID();
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setDisplayName("Medium Entitlement");
        entitlement.setRiskLevel("low");
        SyncManualEntitlementMembersResponse response = new SyncManualEntitlementMembersResponse(
                entitlementId, 2, 2, 0, 0, 0, 2,
                List.of(
                        new SyncEntitlementMemberResult(firstIdentityId, List.of()),
                        new SyncEntitlementMemberResult(secondIdentityId, List.of())),
                List.of(), List.of(), List.of());

        when(identityRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection()))
                .thenReturn(List.of(
                        nonHumanIdentity(firstIdentityId, "MEDIUM_ONE", "medium"),
                        nonHumanIdentity(secondIdentityId, "MEDIUM_TWO", "medium")));
        when(auditLogRepository.save(any(EntitlementAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.auditManualSync(TENANT_ID, entitlement, response, "admin_user");

        ArgumentCaptor<EntitlementManualSyncAuditEvent> eventCaptor = ArgumentCaptor.forClass(EntitlementManualSyncAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("medium");
        assertThat(eventCaptor.getValue().stateAfter().get(EntitlementAuditConstants.DETAIL_RISK_LEVEL).asText())
                .isEqualTo("medium");
    }

    @Test
    void auditManualSyncUsesHumanIdentityRiskLevelWhenPresent() {
        UUID entitlementId = UUID.randomUUID();
        UUID employeeIdentityId = UUID.randomUUID();
        UUID botIdentityId = UUID.randomUUID();
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setDisplayName("Human Risk Entitlement");
        entitlement.setRiskLevel("low");
        SyncManualEntitlementMembersResponse response = new SyncManualEntitlementMembersResponse(
                entitlementId, 2, 2, 0, 0, 0, 2,
                List.of(
                        new SyncEntitlementMemberResult(employeeIdentityId, List.of()),
                        new SyncEntitlementMemberResult(botIdentityId, List.of())),
                List.of(), List.of(), List.of());

        Employee highRiskEmployee = employee(employeeIdentityId, "HIGH_HUMAN");
        highRiskEmployee.setRiskLevel("high");
        when(identityRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection()))
                .thenReturn(List.of(highRiskEmployee, nonHumanIdentity(botIdentityId, "MEDIUM_BOT", "medium")));
        when(auditLogRepository.save(any(EntitlementAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.auditManualSync(TENANT_ID, entitlement, response, "admin_user");

        ArgumentCaptor<EntitlementManualSyncAuditEvent> eventCaptor = ArgumentCaptor.forClass(EntitlementManualSyncAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("high");
        assertThat(eventCaptor.getValue().stateAfter().get(EntitlementAuditConstants.DETAIL_RISK_LEVEL).asText())
                .isEqualTo("high");
    }

    @Test
    void auditManualSyncUsesLowWhenHighestAffectedIdentityRiskIsLowAndIgnoresEntitlementRisk() {
        UUID entitlementId = UUID.randomUUID();
        UUID lowIdentityId = UUID.randomUUID();
        UUID humanIdentityId = UUID.randomUUID();
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setDisplayName("High Entitlement");
        entitlement.setRiskLevel("high");
        SyncManualEntitlementMembersResponse response = new SyncManualEntitlementMembersResponse(
                entitlementId, 2, 1, 0, 1, 0, 2,
                List.of(new SyncEntitlementMemberResult(lowIdentityId, List.of())),
                List.of(),
                List.of(new SyncEntitlementMemberResult(humanIdentityId, List.of())),
                List.of());

        when(identityRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection()))
                .thenReturn(List.of(
                        nonHumanIdentity(lowIdentityId, "LOW_BOT", "low"),
                        employee(humanIdentityId, "HUMAN_MEMBER")));
        when(auditLogRepository.save(any(EntitlementAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.auditManualSync(TENANT_ID, entitlement, response, "admin_user");

        ArgumentCaptor<EntitlementManualSyncAuditEvent> eventCaptor = ArgumentCaptor.forClass(EntitlementManualSyncAuditEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().riskLevel()).isEqualTo("low");
        assertThat(eventCaptor.getValue().stateAfter().get(EntitlementAuditConstants.DETAIL_RISK_LEVEL).asText())
                .isEqualTo("low");
    }

    private Employee employee(UUID id, String displayName) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setTenantId(TENANT_ID);
        employee.setIdentityType(IdentityType.EMPLOYEE);
        employee.setEmployeeId("emp-" + id);
        employee.setUsername(displayName.toLowerCase());
        employee.setEmail(displayName.toLowerCase() + "@example.com");
        employee.setDisplayName(displayName);
        return employee;
    }

    private NonHumanIdentity nonHumanIdentity(UUID id, String serviceName, String riskLevel) {
        NonHumanIdentity identity = new NonHumanIdentity();
        identity.setId(id);
        identity.setTenantId(TENANT_ID);
        identity.setIdentityType(IdentityType.NON_HUMAN);
        identity.setServiceName(serviceName);
        identity.setRiskLevel(riskLevel);
        return identity;
    }
}
