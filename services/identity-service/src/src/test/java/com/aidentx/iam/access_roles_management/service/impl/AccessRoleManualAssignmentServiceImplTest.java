package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.audit.AccessRoleManualAssignmentAuditService;
import com.aidentx.iam.access_roles_management.model.dto.response.ManualRoleMemberGrantResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleManualAssignmentServiceImplTest {

    private static final String TENANT_ID = "tenant-1";
    private static final String MANUAL_SOURCE_KEY = IdentityRoleAssignment.SourceType.MANUAL.name();

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private IdentityRoleAssignmentRepository assignmentRepository;

    @Mock
    private IdentityRoleAssignmentService identityRoleAssignmentService;

    @Mock
    private RoleEntitlementAssignmentService roleEntitlementAssignmentService;

    @Mock
    private RoleEntitlementResolutionService roleEntitlementResolutionService;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private EntitlementAssignmentRepository entitlementAssignmentRepository;

    @Mock
    private ShadowRepository shadowRepository;

    @Mock
    private AccessRoleManualAssignmentAuditService auditService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleManualAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleManualAssignmentServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleManualAssignmentServiceImpl(
                accessRoleRepository,
                identityRepository,
                assignmentRepository,
                identityRoleAssignmentService,
                roleEntitlementAssignmentService,
                roleEntitlementResolutionService,
                entitlementRepository,
                entitlementAssignmentRepository,
                shadowRepository,
                auditService,
                loggerFactory);
    }

    @Test
    void grantRoleToMembersReturnsAddedSkippedAndFailedMembersAndPublishesSummaryAudit() {
        UUID roleId = UUID.randomUUID();
        UUID addedIdentityId = UUID.randomUUID();
        UUID skippedIdentityId = UUID.randomUUID();
        UUID missingIdentityId = UUID.randomUUID();

        AccessRole role = role(roleId);
        Employee addedIdentity = employee(addedIdentityId, "Jane Doe");
        NonHumanIdentity skippedIdentity = nonHuman(skippedIdentityId, "Payroll Bot");
        IdentityRoleAssignment existingActiveAssignment = new IdentityRoleAssignment();
        existingActiveAssignment.setStatus(IdentityRoleAssignment.Status.ACTIVE);

        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRepository.findByTenantIdAndIdIn(TENANT_ID, List.of(addedIdentityId, skippedIdentityId, missingIdentityId)))
                .thenReturn(List.of(addedIdentity, skippedIdentity));
        when(assignmentRepository.findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                TENANT_ID,
                addedIdentityId,
                roleId,
                IdentityRoleAssignment.SourceType.MANUAL,
                MANUAL_SOURCE_KEY)).thenReturn(Optional.empty());
        when(assignmentRepository.findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                TENANT_ID,
                skippedIdentityId,
                roleId,
                IdentityRoleAssignment.SourceType.MANUAL,
                MANUAL_SOURCE_KEY)).thenReturn(Optional.of(existingActiveAssignment));

        ManualRoleMemberGrantResultDto result = service.grantRoleToMembers(
                TENANT_ID,
                roleId,
                List.of(addedIdentityId, skippedIdentityId, missingIdentityId, addedIdentityId),
                "admin-1",
                "Jane Admin");

        assertThat(result.getRequestedCount()).isEqualTo(3);
        assertThat(result.getAdded()).singleElement().satisfies(identity -> {
            assertThat(identity.getIdentityId()).isEqualTo(addedIdentityId);
            assertThat(identity.getIdentityName()).isEqualTo("Jane Doe");
            assertThat(identity.getIdentityType()).isEqualTo("EMPLOYEE");
        });
        assertThat(result.getSkipped()).singleElement().satisfies(identity -> {
            assertThat(identity.getIdentityId()).isEqualTo(skippedIdentityId);
            assertThat(identity.getIdentityName()).isEqualTo("Payroll Bot");
            assertThat(identity.getIdentityType()).isEqualTo("NON_HUMAN");
        });
        assertThat(result.getFailed()).singleElement().satisfies(identity -> {
            assertThat(identity.getIdentityId()).isEqualTo(missingIdentityId);
            assertThat(identity.getReason()).isEqualTo("Identity not found for tenant");
        });

        verify(identityRoleAssignmentService).assignRole(
                TENANT_ID,
                addedIdentityId,
                roleId,
                IdentityRoleAssignment.SourceType.MANUAL,
                null,
                MANUAL_SOURCE_KEY,
                "admin-1",
                "Jane Admin");
        verify(identityRoleAssignmentService).assignRole(
                TENANT_ID,
                skippedIdentityId,
                roleId,
                IdentityRoleAssignment.SourceType.MANUAL,
                null,
                MANUAL_SOURCE_KEY,
                "admin-1",
                "Jane Admin");
        verify(auditService).publishManualGrantAudit(TENANT_ID, role, result);
    }

    @Test
    void grantRoleToMembersThrowsWhenRoleIsMissingForTenant() {
        UUID roleId = UUID.randomUUID();
        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantRoleToMembers(TENANT_ID, roleId, List.of(UUID.randomUUID())))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(roleId.toString());

        verify(identityRepository, never()).findByTenantIdAndIdIn(anyString(), org.mockito.ArgumentMatchers.anyCollection());
        verify(auditService, never()).publishManualGrantAudit(eq(TENANT_ID), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private AccessRole role(UUID roleId) {
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");
        role.setRiskLevel("high");
        return role;
    }

    private Employee employee(UUID identityId, String displayName) {
        Employee employee = new Employee();
        employee.setId(identityId);
        employee.setTenantId(TENANT_ID);
        employee.setIdentityType(IdentityType.EMPLOYEE);
        employee.setDisplayName(displayName);
        return employee;
    }

    private NonHumanIdentity nonHuman(UUID identityId, String serviceName) {
        NonHumanIdentity identity = new NonHumanIdentity();
        identity.setId(identityId);
        identity.setTenantId(TENANT_ID);
        identity.setIdentityType(IdentityType.NON_HUMAN);
        identity.setServiceName(serviceName);
        return identity;
    }
}

