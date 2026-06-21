package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.mapper.AccessRoleMapper;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleSaveSimulationRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDeleteImpactResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleGroupedMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberEntitlementResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMembersSummaryResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleSaveSimulationResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionItemRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAuditLogRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleEntitlementService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.auth.grpc.GetUsersByIdentityIdsRequest;
import com.aidentx.auth.grpc.GetUsersByIdentityIdsResponse;
import com.aidentx.auth.grpc.UserGrpcServiceGrpc;
import com.aidentx.auth.grpc.UserInfo;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleRepository roleRepository;

    @Mock
    private AccessRoleEntitlementRepository entitlementLinkRepository;

    @Mock
    private AccessRoleCompositionRepository compositionRepository;

    @Mock
    private AccessRoleEntitlementService entitlementService;

    @Mock
    private AccessRoleCompositionService compositionService;

    @Mock
    private RoleEntitlementAssignmentService roleEntitlementAssignmentService;

    @Mock
    private RoleEntitlementClosureService roleEntitlementClosureService;

    @Mock
    private IdentityRoleAssignmentRepository identityRoleAssignmentRepository;

    @Mock
    private RoleEntitlementClosureRepository roleEntitlementClosureRepository;

    @Mock
    private AccessRoleAssignmentRuleRepository assignmentRuleRepository;

    @Mock
    private AccessRoleAssignmentExecutionRepository assignmentExecutionRepository;

    @Mock
    private AccessRoleAssignmentExecutionItemRepository assignmentExecutionItemRepository;

    @Mock
    private AccessRoleAuditLogRepository auditLogRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @Mock
    private ApplicationClient applicationClient;

    @Mock
    private AccessRoleMapper roleMapper;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock
    private ShadowRepository shadowRepository;

    @Mock
    private EntitlementAssignmentRepository entitlementAssignmentRepository;

    @Mock
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub authUserClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleServiceImpl(
                roleRepository,
                entitlementLinkRepository,
                compositionRepository,
                entitlementService,
                compositionService,
                roleEntitlementAssignmentService,
                roleEntitlementClosureService,
                identityRoleAssignmentRepository,
                roleEntitlementClosureRepository,
                assignmentRuleRepository,
                assignmentExecutionRepository,
                assignmentExecutionItemRepository,
                auditLogRepository,
                entitlementRepository,
                identityRepository,
                orgUnitRepository,
                applicationClient,
                roleMapper,
                loggerFactory,
                shadowRepository,
                entitlementAssignmentRepository,
                authUserClient);
    }

    @Test
    void createRoleRequiresTenantScopedOrganizationAndReturnsIt() {
        UUID roleId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CreateAccessRoleRequest request = new CreateAccessRoleRequest();
        request.setName("Finance Manager");
        request.setType("business");
        request.setBusinessUnit("Finance");
        request.setOrganization(organizationId);

        AccessRole mapped = accessRole(roleId, request.getName(), request.getName());
        mapped.setOrganization(organizationId);
        AccessRoleResponse mappedResponse = new AccessRoleResponse();
        mappedResponse.setId(roleId);
        mappedResponse.setTenantId(TENANT_ID);
        mappedResponse.setName(request.getName());
        mappedResponse.setType(request.getType());
        mappedResponse.setOrganization(organizationId);

        when(orgUnitRepository.findByIdAndTenantId(organizationId, TENANT_ID)).thenReturn(Optional.of(orgUnit(organizationId)));
        when(roleRepository.findByTenantIdAndName(TENANT_ID, request.getName())).thenReturn(Optional.empty());
        when(roleMapper.createRequestToEntity(request)).thenReturn(mapped);
        when(roleRepository.save(mapped)).thenReturn(mapped);
        when(roleMapper.entityToResponse(mapped)).thenReturn(mappedResponse);

        AccessRoleResponse response = service.createRole(TENANT_ID, request);

        assertThat(response.getOrganization()).isEqualTo(organizationId);
        verify(orgUnitRepository).findByIdAndTenantId(organizationId, TENANT_ID);
        verify(roleRepository).save(mapped);
    }

    @Test
    void createRoleDefaultsOptionalFieldsWhenOnlyNameIsProvided() {
        UUID roleId = UUID.randomUUID();
        CreateAccessRoleRequest request = new CreateAccessRoleRequest();
        request.setName("Finance Manager");

        AccessRole mapped = accessRole(roleId, request.getName(), request.getName());
        mapped.setType("No type");
        mapped.setBusinessUnit("No business unit");
        mapped.setStatus("Enabled");
        mapped.setRequestable(false);
        mapped.setRequireApproval(false);
        AccessRoleResponse mappedResponse = new AccessRoleResponse();
        mappedResponse.setId(roleId);
        mappedResponse.setTenantId(TENANT_ID);
        mappedResponse.setName(request.getName());
        mappedResponse.setDisplayName(request.getName());
        mappedResponse.setType("No type");
        mappedResponse.setBusinessUnit("No business unit");
        mappedResponse.setStatus("Enabled");
        mappedResponse.setRequestable(false);
        mappedResponse.setRequireApproval(false);

        when(roleRepository.findByTenantIdAndName(TENANT_ID, request.getName())).thenReturn(Optional.empty());
        when(roleMapper.createRequestToEntity(request)).thenReturn(mapped);
        when(roleRepository.save(mapped)).thenReturn(mapped);
        when(roleMapper.entityToResponse(mapped)).thenReturn(mappedResponse);

        AccessRoleResponse response = service.createRole(TENANT_ID, request);

        assertThat(response.getDisplayName()).isEqualTo("Finance Manager");
        assertThat(response.getType()).isEqualTo("No type");
        assertThat(response.getBusinessUnit()).isEqualTo("No business unit");
        assertThat(response.getStatus()).isEqualTo("Enabled");
        assertThat(response.getRequestable()).isFalse();
        assertThat(response.getRequireApproval()).isFalse();
        assertThat(response.getOrganization()).isNull();
        assertThat(request.getType()).isEqualTo("No type");
        assertThat(request.getBusinessUnit()).isEqualTo("No business unit");
        verify(orgUnitRepository, never()).findByIdAndTenantId(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());
        verify(roleRepository).save(mapped);
    }

    @Test
    void updateRoleValidatesAndUpdatesOrganizationWhenProvided() {
        UUID roleId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        AccessRole existing = accessRole(roleId, "Finance Manager", "Finance Manager");
        existing.setOrganization(UUID.randomUUID());
        UpdateAccessRoleRequest request = new UpdateAccessRoleRequest();
        request.setOrganization(organizationId);
        AccessRoleResponse mappedResponse = new AccessRoleResponse();
        mappedResponse.setId(roleId);
        mappedResponse.setOrganization(organizationId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existing));
        when(orgUnitRepository.findByIdAndTenantId(organizationId, TENANT_ID)).thenReturn(Optional.of(orgUnit(organizationId)));
        doAnswer(invocation -> {
            UpdateAccessRoleRequest update = invocation.getArgument(0);
            AccessRole role = invocation.getArgument(1);
            role.setOrganization(update.getOrganization());
            return null;
        }).when(roleMapper).updateEntityFromRequest(request, existing);
        when(roleRepository.save(existing)).thenReturn(existing);
        when(roleMapper.entityToResponse(existing)).thenReturn(mappedResponse);

        AccessRoleResponse response = service.updateRole(TENANT_ID, roleId, request);

        assertThat(existing.getOrganization()).isEqualTo(organizationId);
        assertThat(response.getOrganization()).isEqualTo(organizationId);
        verify(orgUnitRepository).findByIdAndTenantId(organizationId, TENANT_ID);
    }

    @Test
    void getRoleMembersReturnsDirectAndChildRoleEntitlementPaths() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID directEntitlementId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID directResourceId = UUID.randomUUID();
        UUID inheritedResourceId = UUID.randomUUID();

        AccessRole role = accessRole(roleId, "Finance Parent Role", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child Role", "Payroll Child");
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Jane", "Doe", "Jane Doe");
        Entitlement directEntitlement = entitlement(directEntitlementId, directResourceId, "Direct Permission");
        Entitlement inheritedEntitlement = entitlement(inheritedEntitlementId, inheritedResourceId, "Inherited Permission");
        RoleEntitlementClosure directClosure = closure(roleId, directEntitlementId, roleId, true, 0, List.of(roleId));
        RoleEntitlementClosure inheritedClosure = closure(roleId, inheritedEntitlementId, childRoleId, false, 1, List.of(roleId, childRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(directClosure, inheritedClosure));
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));
        when(entitlementRepository.findAllById(anyList()))
                .thenReturn(List.of(directEntitlement, inheritedEntitlement));
        when(applicationClient.getApplicationNames(eq(TENANT_ID), anyList()))
                .thenReturn(Map.of(directResourceId, "Finance App", inheritedResourceId, "Payroll App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole));

        Page<AccessRoleMemberResponse> result = getRoleMembers(roleId);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(row -> {
            assertThat(row.assignmentId()).isEqualTo(assignmentId);
            assertThat(row.identityId()).isEqualTo(identityId);
            assertThat(row.identityName()).isEqualTo("Jane Doe");
            assertThat(row.identityType()).isEqualTo(IdentityType.EMPLOYEE.name());
            assertThat(row.roleId()).isEqualTo(roleId);
            assertThat(row.roleName()).isEqualTo("Finance Parent");
            assertThat(row.roleAssignedAt()).isEqualTo(1778000000000L);
        });

        AccessRoleMemberResponse directRow = result.getContent().stream()
                .filter(row -> directEntitlementId.equals(row.entitlementId()))
                .findFirst()
                .orElseThrow();
        assertThat(directRow.entitlementName()).isEqualTo("Direct Permission");
        assertThat(directRow.applicationName()).isEqualTo("Finance App");
        assertThat(directRow.accessPathType()).isEqualTo("Direct Role");
        assertThat(directRow.childRoleId()).isNull();
        assertThat(directRow.rolePath()).extracting(AccessRoleMemberResponse.RolePathNode::roleId)
                .containsExactly(roleId);

        AccessRoleMemberResponse inheritedRow = result.getContent().stream()
                .filter(row -> inheritedEntitlementId.equals(row.entitlementId()))
                .findFirst()
                .orElseThrow();
        assertThat(inheritedRow.entitlementName()).isEqualTo("Inherited Permission");
        assertThat(inheritedRow.applicationName()).isEqualTo("Payroll App");
        assertThat(inheritedRow.accessPathType()).isEqualTo("Child Role");
        assertThat(inheritedRow.childRoleId()).isEqualTo(childRoleId);
        assertThat(inheritedRow.childRoleName()).isEqualTo("Payroll Child");
        assertThat(inheritedRow.rolePath()).extracting(AccessRoleMemberResponse.RolePathNode::roleId)
                .containsExactly(roleId, childRoleId);
    }

    @Test
    void getRoleMembersAppliesSearchAndExactFiltersBeforePagination() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID directEntitlementId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID directResourceId = UUID.randomUUID();
        UUID inheritedResourceId = UUID.randomUUID();

        AccessRole role = accessRole(roleId, "Finance Parent Role", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child Role", "Payroll Child");
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Jane", "Doe", "Jane Doe");
        Entitlement directEntitlement = entitlement(directEntitlementId, directResourceId, "Direct Permission");
        Entitlement inheritedEntitlement = entitlement(inheritedEntitlementId, inheritedResourceId, "Inherited Permission");
        RoleEntitlementClosure directClosure = closure(roleId, directEntitlementId, roleId, true, 0, List.of(roleId));
        RoleEntitlementClosure inheritedClosure = closure(roleId, inheritedEntitlementId, childRoleId, false, 1, List.of(roleId, childRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(directClosure, inheritedClosure));
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));
        when(entitlementRepository.findAllById(anyList()))
                .thenReturn(List.of(directEntitlement, inheritedEntitlement));
        when(applicationClient.getApplicationNames(eq(TENANT_ID), anyList()))
                .thenReturn(Map.of(directResourceId, "Finance App", inheritedResourceId, "Payroll App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole));

        Page<AccessRoleMemberResponse> searchResult = service.getRoleMembers(
                TENANT_ID,
                roleId,
                "payroll",
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 1));

        assertThat(searchResult.getTotalElements()).isEqualTo(1);
        assertThat(searchResult.getContent()).singleElement().satisfies(row -> {
            assertThat(row.entitlementId()).isEqualTo(inheritedEntitlementId);
            assertThat(row.accessPathType()).isEqualTo("Child Role");
            assertThat(row.childRoleName()).isEqualTo("Payroll Child");
        });

        Page<AccessRoleMemberResponse> filterResult = service.getRoleMembers(
                TENANT_ID,
                roleId,
                null,
                List.of("employee"),
                null,
                List.of("direct_through_role"),
                List.of("rule"),
                List.of(directResourceId),
                List.of(directEntitlementId),
                PageRequest.of(0, 10));

        assertThat(filterResult.getTotalElements()).isEqualTo(1);
        assertThat(filterResult.getContent()).singleElement().satisfies(row -> {
            assertThat(row.entitlementId()).isEqualTo(directEntitlementId);
            assertThat(row.applicationId()).isEqualTo(directResourceId);
            assertThat(row.accessPathType()).isEqualTo("Direct Role");
            assertThat(row.assignmentSourceType()).isEqualTo("RULE");
        });
    }

    @Test
    void getRoleMembersSummaryRejectsNonPositiveActivityWindow() {
        UUID roleId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Finance Role", "Finance Role");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.getRoleMembersSummary(TENANT_ID, roleId, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("activityWindowDays must be greater than 0");
    }

    @Test
    void getRoleMembersSummaryReturnsActivityCountsAndDynamicManualMode() {
        UUID roleId = UUID.randomUUID();
        UUID identityIdOne = UUID.randomUUID();
        UUID identityIdTwo = UUID.randomUUID();
        UUID identityIdThree = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Finance Role", "Finance Role");
        IdentityRoleAssignment ruleAssignment = assignment(UUID.randomUUID(), identityIdOne, roleId, 1778000000000L);
        ruleAssignment.setSourceType(IdentityRoleAssignment.SourceType.RULE);
        IdentityRoleAssignment importAssignment = assignment(UUID.randomUUID(), identityIdTwo, roleId, 1778000000001L);
        importAssignment.setSourceType(IdentityRoleAssignment.SourceType.IMPORT);
        IdentityRoleAssignment manualAssignment = assignment(UUID.randomUUID(), identityIdThree, roleId, 1778000000002L);
        manualAssignment.setSourceType(IdentityRoleAssignment.SourceType.MANUAL);
        long recentLogin = Instant.now().minusSeconds(10 * 24 * 60 * 60).toEpochMilli();
        long staleLogin = Instant.now().minusSeconds(120 * 24 * 60 * 60).toEpochMilli();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(3L);
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE))
                .thenReturn(List.of(ruleAssignment, importAssignment, manualAssignment));
        when(authUserClient.getUsersByIdentityIds(org.mockito.ArgumentMatchers.any(GetUsersByIdentityIdsRequest.class)))
                .thenReturn(GetUsersByIdentityIdsResponse.newBuilder()
                        .setTenantId(TENANT_ID)
                        .addUsers(UserInfo.newBuilder()
                                .setIdentityId(identityIdOne.toString())
                                .setLastLoginAt(recentLogin)
                                .build())
                        .addUsers(UserInfo.newBuilder()
                                .setIdentityId(identityIdTwo.toString())
                                .setLastLoginAt(staleLogin)
                                .build())
                        .setTotalCount(2)
                        .build());

        AccessRoleMembersSummaryResponse response = service.getRoleMembersSummary(TENANT_ID, roleId, null);

        assertThat(response.roleId()).isEqualTo(roleId);
        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        assertThat(response.activityWindowDays()).isEqualTo(90);
        assertThat(response.activityCutoffAt()).isNotNull();
        assertThat(response.totalMembers()).isEqualTo(3);
        assertThat(response.activeMembers()).isEqualTo(1);
        assertThat(response.inactiveMembers()).isEqualTo(2);
        assertThat(response.assignmentMode()).isEqualTo("DYNAMIC + MANUAL");
    }

    @Test
    void getRoleMembersSummaryDefaultsAssignmentModeToManual() {
        UUID roleId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Finance Role", "Finance Role");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(0L);
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of());

        AccessRoleMembersSummaryResponse response = service.getRoleMembersSummary(TENANT_ID, roleId, null);

        assertThat(response.totalMembers()).isZero();
        assertThat(response.activeMembers()).isZero();
        assertThat(response.inactiveMembers()).isZero();
        assertThat(response.assignmentMode()).isEqualTo("MANUAL");
    }

    @Test
    void getRoleMembersFiltersByIdentityRiskLevel() {
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Service Role", null);
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        NonHumanIdentity identity = nonHumanIdentity(identityId, "svc-payroll", "HIGH");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));

        Page<AccessRoleMemberResponse> highRiskResult = service.getRoleMembers(
                TENANT_ID,
                roleId,
                null,
                null,
                List.of("high"),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10));

        assertThat(highRiskResult.getTotalElements()).isEqualTo(1);
        assertThat(highRiskResult.getContent()).singleElement().satisfies(row -> {
            assertThat(row.identityName()).isEqualTo("svc-payroll");
            assertThat(row.identityRiskLevel()).isEqualTo("HIGH");
        });

        Page<AccessRoleMemberResponse> lowRiskResult = service.getRoleMembers(
                TENANT_ID,
                roleId,
                null,
                null,
                List.of("low"),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10));

        assertThat(lowRiskResult.getTotalElements()).isZero();
        assertThat(lowRiskResult.getContent()).isEmpty();
    }

    @Test
    void getRoleMembersReturnsAssignmentRowWhenRoleHasNoEffectiveEntitlements() {
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Empty Role", null);
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "John", "Smith", null);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));

        Page<AccessRoleMemberResponse> result = getRoleMembers(roleId);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AccessRoleMemberResponse row = result.getContent().get(0);
        assertThat(row.identityName()).isEqualTo("John Smith");
        assertThat(row.roleName()).isEqualTo("Empty Role");
        assertThat(row.entitlementId()).isNull();
        assertThat(row.entitlementName()).isNull();
        assertThat(row.applicationName()).isNull();
        assertThat(row.accessPathType()).isEqualTo("No Entitlement");
        assertThat(row.rolePath()).isEmpty();
    }

    @Test
    void getRoleMembersPresentsStoredImportAssignmentsAsManual() {
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Imported Role", null);
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        assignment.setSourceType(IdentityRoleAssignment.SourceType.IMPORT);
        Employee identity = employee(identityId, "Import", "User", "Import User");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));

        Page<AccessRoleMemberResponse> manualResult = service.getRoleMembers(
                TENANT_ID,
                roleId,
                null,
                null,
                null,
                null,
                List.of("manual"),
                null,
                null,
                PageRequest.of(0, 10));
        Page<AccessRoleMemberResponse> importResult = service.getRoleMembers(
                TENANT_ID,
                roleId,
                null,
                null,
                null,
                null,
                List.of("import"),
                null,
                null,
                PageRequest.of(0, 10));

        assertThat(manualResult.getContent()).singleElement().satisfies(row ->
                assertThat(row.assignmentSourceType()).isEqualTo("MANUAL"));
        assertThat(importResult.getContent()).singleElement().satisfies(row ->
                assertThat(row.assignmentSourceType()).isEqualTo("MANUAL"));
    }

    @Test
    void getRoleMembersGroupedReturnsMemberSummaryWithEntitlementCountAndChildRoles() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID directEntitlementId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID directResourceId = UUID.randomUUID();
        UUID inheritedResourceId = UUID.randomUUID();

        AccessRole role = accessRole(roleId, "Finance Parent Role", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child Role", "Payroll Child");
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Jane", "Doe", "Jane Doe");
        RoleEntitlementClosure directClosure = closure(roleId, directEntitlementId, roleId, true, 0, List.of(roleId));
        RoleEntitlementClosure inheritedClosure = closure(roleId, inheritedEntitlementId, childRoleId, false, 1, List.of(roleId, childRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(directClosure, inheritedClosure));
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole));

        Page<AccessRoleGroupedMemberResponse> result = getRoleMembersGrouped(roleId);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(member -> {
            assertThat(member.assignmentId()).isEqualTo(assignmentId);
            assertThat(member.identityId()).isEqualTo(identityId);
            assertThat(member.identityName()).isEqualTo("Jane Doe");
            assertThat(member.roleName()).isEqualTo("Finance Parent");
            assertThat(member.assignmentSourceType()).isEqualTo("RULE");
            assertThat(member.entitlementCount()).isEqualTo(2);
            assertThat(member.childRoleNames()).containsExactly("Payroll Child");
            assertThat(member.assignmentPathSummary()).isEqualTo("Direct + inherited through 1 child role");
        });

        verify(entitlementRepository, never()).findAllById(anyList());
        verify(applicationClient, never()).getApplicationNames(eq(TENANT_ID), anyList());
    }

    @Test
    void getRoleMembersGroupedSearchesByEmployeeNameAndPaginatesAcrossMembers() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID assignmentOneId = UUID.randomUUID();
        UUID assignmentTwoId = UUID.randomUUID();
        UUID identityOneId = UUID.randomUUID();
        UUID identityTwoId = UUID.randomUUID();
        UUID entitlementOneId = UUID.randomUUID();
        UUID entitlementTwoId = UUID.randomUUID();
        UUID resourceOneId = UUID.randomUUID();
        UUID resourceTwoId = UUID.randomUUID();

        AccessRole role = accessRole(roleId, "Finance Parent Role", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child Role", "Payroll Child");
        IdentityRoleAssignment assignmentOne = assignment(assignmentOneId, identityOneId, roleId, 1778000002000L);
        IdentityRoleAssignment assignmentTwo = assignment(assignmentTwoId, identityTwoId, roleId, 1778000001000L);
        Employee identityOne = employee(identityOneId, "Jane", "Doe", "Jane Doe");
        Employee identityTwo = employee(identityTwoId, "John", "Smith", "John Smith");
        Entitlement directEntitlement = entitlement(entitlementOneId, resourceOneId, "Direct Permission");
        Entitlement inheritedEntitlement = entitlement(entitlementTwoId, resourceTwoId, "Payroll Permission");
        RoleEntitlementClosure directClosure = closure(roleId, entitlementOneId, roleId, true, 0, List.of(roleId));
        RoleEntitlementClosure inheritedClosure = closure(roleId, entitlementTwoId, childRoleId, false, 1, List.of(roleId, childRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignmentOne, assignmentTwo));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(directClosure, inheritedClosure));
        when(identityRepository.findAllById(Set.of(identityOneId, identityTwoId))).thenReturn(List.of(identityOne, identityTwo));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole));

        Page<AccessRoleGroupedMemberResponse> searchResult = service.getRoleMembersGrouped(
                TENANT_ID,
                roleId,
                "jane",
                PageRequest.of(0, 10));

        assertThat(searchResult.getTotalElements()).isEqualTo(1);
        assertThat(searchResult.getContent()).singleElement().satisfies(member ->
                assertThat(member.identityName()).isEqualTo("Jane Doe"));

        Page<AccessRoleGroupedMemberResponse> unmatchedSearchResult = service.getRoleMembersGrouped(
                TENANT_ID,
                roleId,
                "payroll",
                PageRequest.of(0, 10));

        assertThat(unmatchedSearchResult.getTotalElements()).isZero();

        Page<AccessRoleGroupedMemberResponse> pagedResult = service.getRoleMembersGrouped(
                TENANT_ID,
                roleId,
                null,
                PageRequest.of(0, 1));

        assertThat(pagedResult.getTotalElements()).isEqualTo(2);
        assertThat(pagedResult.getContent()).singleElement().satisfies(member -> {
            assertThat(member.assignmentId()).isEqualTo(assignmentOneId);
            assertThat(member.identityName()).isEqualTo("Jane Doe");
            assertThat(member.entitlementCount()).isEqualTo(2);
            assertThat(member.childRoleNames()).containsExactly("Payroll Child");
        });
    }

    @Test
    void getRoleMembersGroupedReturnsZeroEntitlementCountWhenRoleHasNoEffectiveEntitlements() {
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Empty Role", null);
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "John", "Smith", null);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));

        Page<AccessRoleGroupedMemberResponse> result = getRoleMembersGrouped(roleId);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement().satisfies(member -> {
            assertThat(member.assignmentId()).isEqualTo(assignmentId);
            assertThat(member.identityName()).isEqualTo("John Smith");
            assertThat(member.roleName()).isEqualTo("Empty Role");
            assertThat(member.entitlementCount()).isZero();
            assertThat(member.childRoleNames()).isEmpty();
            assertThat(member.assignmentPathSummary()).isEqualTo("Direct role assignment");
        });
    }

    @Test
    void getRoleMembersGroupedPresentsStoredImportAssignmentsAsManual() {
        UUID roleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Imported Role", null);
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        assignment.setSourceType(IdentityRoleAssignment.SourceType.IMPORT);
        Employee identity = employee(identityId, "Import", "User", "Import User");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));

        Page<AccessRoleGroupedMemberResponse> result = getRoleMembersGrouped(roleId);

        assertThat(result.getContent()).singleElement().satisfies(member ->
                assertThat(member.assignmentSourceType()).isEqualTo("MANUAL"));
    }

    @Test
    void getRoleMemberEntitlementsReturnsUserFriendlyPathDetails() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID directEntitlementId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID directResourceId = UUID.randomUUID();
        UUID inheritedResourceId = UUID.randomUUID();

        AccessRole role = accessRole(roleId, "Finance Parent Role", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child Role", "Payroll Child");
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Jane", "Doe", "Jane Doe");
        Entitlement directEntitlement = entitlement(directEntitlementId, directResourceId, "Direct Permission");
        Entitlement inheritedEntitlement = entitlement(inheritedEntitlementId, inheritedResourceId, "Inherited Permission");
        RoleEntitlementClosure directClosure = closure(roleId, directEntitlementId, roleId, true, 0, List.of(roleId));
        RoleEntitlementClosure inheritedClosure = closure(roleId, inheritedEntitlementId, childRoleId, false, 1, List.of(roleId, childRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByIdAndTenantIdAndRoleIdAndStatus(
                assignmentId, TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(Optional.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(directClosure, inheritedClosure));
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));
        when(entitlementRepository.findAllById(anyList()))
                .thenReturn(List.of(directEntitlement, inheritedEntitlement));
        when(applicationClient.getApplicationNames(eq(TENANT_ID), anyList()))
                .thenReturn(Map.of(directResourceId, "Finance App", inheritedResourceId, "Payroll App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole));

        Page<AccessRoleMemberEntitlementResponse> result = getRoleMemberEntitlements(roleId, assignmentId);

        assertThat(result.getTotalElements()).isEqualTo(2);
        AccessRoleMemberEntitlementResponse directRow = result.getContent().stream()
                .filter(row -> directEntitlementId.equals(row.entitlementId()))
                .findFirst()
                .orElseThrow();
        assertThat(directRow.accessPathLabel()).isEqualTo("Directly linked to this role");
        assertThat(directRow.inheritanceDepthLabel()).isEqualTo("Direct");
        assertThat(directRow.assignmentPath()).isEqualTo("Finance Parent");
        assertThat(directRow.childRoleName()).isNull();

        AccessRoleMemberEntitlementResponse inheritedRow = result.getContent().stream()
                .filter(row -> inheritedEntitlementId.equals(row.entitlementId()))
                .findFirst()
                .orElseThrow();
        assertThat(inheritedRow.accessPathType()).isEqualTo("Child Role");
        assertThat(inheritedRow.accessPathLabel()).isEqualTo("Inherited through child role Payroll Child");
        assertThat(inheritedRow.inheritanceDepthLabel()).isEqualTo("Inherited from 1 child role");
        assertThat(inheritedRow.childRoleName()).isEqualTo("Payroll Child");
        assertThat(inheritedRow.assignmentPath()).isEqualTo("Finance Parent → Payroll Child");
    }

    @Test
    void getRoleMemberEntitlementsAppliesSearchAndFilters() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID directEntitlementId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID directResourceId = UUID.randomUUID();
        UUID inheritedResourceId = UUID.randomUUID();

        AccessRole role = accessRole(roleId, "Finance Parent Role", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child Role", "Payroll Child");
        IdentityRoleAssignment assignment = assignment(assignmentId, identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Jane", "Doe", "Jane Doe");
        Entitlement directEntitlement = entitlement(directEntitlementId, directResourceId, "Direct Permission");
        Entitlement inheritedEntitlement = entitlement(inheritedEntitlementId, inheritedResourceId, "Payroll Permission");
        RoleEntitlementClosure directClosure = closure(roleId, directEntitlementId, roleId, true, 0, List.of(roleId));
        RoleEntitlementClosure inheritedClosure = closure(roleId, inheritedEntitlementId, childRoleId, false, 1, List.of(roleId, childRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByIdAndTenantIdAndRoleIdAndStatus(
                assignmentId, TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(Optional.of(assignment));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(directClosure, inheritedClosure));
        when(identityRepository.findAllById(Set.of(identityId))).thenReturn(List.of(identity));
        when(entitlementRepository.findAllById(List.of(directEntitlementId, inheritedEntitlementId)))
                .thenReturn(List.of(directEntitlement, inheritedEntitlement));
        when(applicationClient.getApplicationNames(eq(TENANT_ID), anyList()))
                .thenReturn(Map.of(directResourceId, "Finance App", inheritedResourceId, "Payroll App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole));

        Page<AccessRoleMemberEntitlementResponse> searchResult = service.getRoleMemberEntitlements(
                TENANT_ID,
                roleId,
                assignmentId,
                "payroll",
                null,
                null,
                null,
                PageRequest.of(0, 10));

        assertThat(searchResult.getTotalElements()).isEqualTo(1);
        assertThat(searchResult.getContent()).singleElement().satisfies(row -> {
            assertThat(row.entitlementId()).isEqualTo(inheritedEntitlementId);
            assertThat(row.childRoleName()).isEqualTo("Payroll Child");
        });

        Page<AccessRoleMemberEntitlementResponse> filterResult = service.getRoleMemberEntitlements(
                TENANT_ID,
                roleId,
                assignmentId,
                null,
                List.of("direct_through_role"),
                List.of(directResourceId),
                List.of(directEntitlementId),
                PageRequest.of(0, 10));

        assertThat(filterResult.getTotalElements()).isEqualTo(1);
        assertThat(filterResult.getContent()).singleElement().satisfies(row -> {
            assertThat(row.entitlementId()).isEqualTo(directEntitlementId);
            assertThat(row.applicationId()).isEqualTo(directResourceId);
            assertThat(row.accessPathType()).isEqualTo("Direct Role");
        });
    }

    @Test
    void getRoleMembersRejectsRoleFromAnotherTenant() {
        UUID roleId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Other Tenant Role", null);
        role.setTenantId("other-tenant");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> getRoleMembers(roleId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(roleId.toString());

        verify(roleRepository).findById(roleId);
    }

    @Test
    void deleteRoleRevokesAssignmentsDeletesOnlyLinksAndSelectedRole() {
        UUID roleId = UUID.randomUUID();
        UUID parentRoleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID assignmentRuleId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Role To Delete", "Role To Delete");
        IdentityRoleAssignment assignment = assignment(UUID.randomUUID(), identityId, roleId, 1778000000000L);
        assignment.setAssignmentRuleId(assignmentRuleId);
        AccessRoleComposition parentLink = composition(parentRoleId, roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(compositionRepository.findByChildRoleId(roleId)).thenReturn(List.of(parentLink));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));

        service.deleteRole(TENANT_ID, roleId);

        assertThat(assignment.getStatus()).isEqualTo(IdentityRoleAssignment.Status.REVOKED);
        assertThat(assignment.getRevokedAt()).isNotNull();
        InOrder revocationOrder = inOrder(identityRoleAssignmentRepository, roleEntitlementAssignmentService);
        revocationOrder.verify(identityRoleAssignmentRepository).saveAllAndFlush(List.of(assignment));
        revocationOrder.verify(roleEntitlementAssignmentService).revokeRoleEntitlements(TENANT_ID, identityId, roleId, assignmentRuleId);
        verify(entitlementLinkRepository).deleteByRoleId(roleId);
        verify(compositionRepository).deleteByParentRoleId(roleId);
        verify(compositionRepository).deleteByChildRoleId(roleId);
        verify(roleEntitlementClosureRepository).deleteByTenantIdAndRoleId(TENANT_ID, roleId);
        verify(roleEntitlementClosureService).rebuildRoleAndAncestors(TENANT_ID, parentRoleId);
        verify(roleRepository).delete(role);
        verify(roleRepository, never()).delete(accessRole(parentRoleId, "Parent", null));
        verify(roleRepository, never()).delete(accessRole(childRoleId, "Child", null));
    }

    @Test
    void getDeleteImpactReturnsCountsOnlyWithoutMutatingState() {
        UUID roleId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Role To Delete", "Role To Delete");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.countByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(14L);
        when(identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(12L);
        when(entitlementLinkRepository.countByRoleId(roleId)).thenReturn(3L);
        when(roleEntitlementClosureRepository.countByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(9L);
        when(roleEntitlementClosureRepository.countDistinctEntitlementIdByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(7L);
        when(compositionRepository.countByParentRoleId(roleId)).thenReturn(4L);
        when(compositionRepository.countByChildRoleId(roleId)).thenReturn(2L);
        when(compositionRepository.countDistinctParentRoleIdByChildRoleId(roleId)).thenReturn(2L);
        when(assignmentRuleRepository.countByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(1L);
        when(assignmentRuleRepository.countByTenantIdAndRoleIdAndEnabledTrue(TENANT_ID, roleId)).thenReturn(1L);
        when(assignmentExecutionRepository.countByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(5L);
        when(assignmentExecutionItemRepository.countByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(21L);
        when(auditLogRepository.countByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(8L);

        AccessRoleDeleteImpactResponse response = service.getDeleteImpact(TENANT_ID, roleId);

        assertThat(response.rolesDeletedCount()).isEqualTo(1);
        assertThat(response.activeAssignmentsRevoked()).isEqualTo(14);
        assertThat(response.distinctUsersAffected()).isEqualTo(12);
        assertThat(response.directEntitlementLinksRemoved()).isEqualTo(3);
        assertThat(response.effectiveEntitlementRowsRemoved()).isEqualTo(9);
        assertThat(response.distinctEffectiveEntitlementsAffected()).isEqualTo(7);
        assertThat(response.childRoleLinksRemoved()).isEqualTo(4);
        assertThat(response.parentRoleLinksRemoved()).isEqualTo(2);
        assertThat(response.parentRolesToRefresh()).isEqualTo(2);
        assertThat(response.assignmentRulesImpacted()).isEqualTo(1);
        assertThat(response.enabledAssignmentRulesImpacted()).isEqualTo(1);
        assertThat(response.executionRunsImpacted()).isEqualTo(5);
        assertThat(response.executionItemsImpacted()).isEqualTo(21);
        assertThat(response.auditRecordsImpacted()).isEqualTo(8);
        assertThat(response.totalLinksRemoved()).isEqualTo(9);
        verify(roleRepository, never()).delete(role);
        verify(identityRoleAssignmentRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void simulateRoleSaveImpactReportsAddedRemovedUsersEntitlementsRiskAndDoesNotMutate() {
        UUID roleId = UUID.randomUUID();
        UUID currentIdentityId = UUID.randomUUID();
        UUID proposedIdentityId = UUID.randomUUID();
        UUID currentEntitlementId = UUID.randomUUID();
        UUID proposedEntitlementId = UUID.randomUUID();
        UUID currentResourceId = UUID.randomUUID();
        UUID proposedResourceId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Finance Manager", "Finance Manager");
        IdentityRoleAssignment currentAssignment = assignment(UUID.randomUUID(), currentIdentityId, roleId, 1778000000000L);
        Employee currentIdentity = employee(currentIdentityId, "Current", "User", "Current User");
        currentIdentity.setRiskLevel("low");
        Employee proposedIdentity = employee(proposedIdentityId, "Proposed", "User", "Proposed User");
        proposedIdentity.setRiskLevel("medium");
        Entitlement currentEntitlement = entitlement(currentEntitlementId, currentResourceId, "Old Finance Group");
        currentEntitlement.setRiskLevel("low");
        Entitlement proposedEntitlement = entitlement(proposedEntitlementId, proposedResourceId, "High Finance Group");
        proposedEntitlement.setRiskLevel("critical");

        AccessRoleSaveSimulationRequest request = new AccessRoleSaveSimulationRequest();
        request.setRoleName("Finance Manager Updated");
        request.setIdentityIds(List.of(proposedIdentityId));
        request.setEntitlementIds(List.of(proposedEntitlementId));
        request.setChildRoleIds(List.of());

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(currentAssignment));
        when(entitlementLinkRepository.findByRoleId(roleId)).thenReturn(List.of(roleEntitlement(roleId, currentEntitlementId)));
        when(compositionRepository.findByParentRoleId(roleId)).thenReturn(List.of());
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(closure(roleId, currentEntitlementId, roleId, true, 0, List.of(roleId))));
        when(identityRepository.findAllById(anySet())).thenReturn(List.of(currentIdentity, proposedIdentity));
        when(entitlementRepository.findAllById(anySet())).thenReturn(List.of(currentEntitlement, proposedEntitlement));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(currentResourceId, proposedResourceId)))
                .thenReturn(Map.of(currentResourceId, "Old App", proposedResourceId, "Finance App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role));

        AccessRoleSaveSimulationResponse response = service.simulateRoleSaveImpact(TENANT_ID, roleId, request, false);

        assertThat(response.roleId()).isEqualTo(roleId);
        assertThat(response.roleName()).isEqualTo("Finance Manager Updated");
        assertThat(response.simulationType()).isEqualTo("ROLE_SAVE");
        assertThat(response.impactSeverity()).isEqualTo("High");
        assertThat(response.summary().totalAffectedUsers()).isEqualTo(2);
        assertThat(response.summary().usersAddedToRole()).isEqualTo(1);
        assertThat(response.summary().usersRemovedFromRole()).isEqualTo(1);
        assertThat(response.summary().addedEntitlements()).isEqualTo(1);
        assertThat(response.summary().removedEntitlements()).isEqualTo(1);
        assertThat(response.summary().directEntitlementChanges()).isEqualTo(2);
        assertThat(response.summary().childRoleChanges()).isZero();
        assertThat(response.summary().unchangedEntitlements()).isNull();
        assertThat(response.impactScoreDetails().formula()).contains("usersAddedToRole*5");
        assertThat(response.impactScoreDetails().weights()).containsEntry("usersAddedToRole", 5);
        assertThat(response.impactScoreDetails().weights()).containsEntry("removedEntitlements", 4);
        assertThat(response.impactScoreDetails().riskLevelUsed()).isEqualTo("High");
        assertThat(response.affectedEntitlements()).extracting("source").containsOnly("Role");
        assertThat(response.affectedEntitlements()).extracting("impactType")
                .containsExactlyInAnyOrder("Entitlement Added", "Entitlement Revoked");
        assertThat(response.affectedEntitlements()).extracting("riskLevel").contains("High");
        assertThat(response.affectedIdentities()).extracting("overallImpactType")
                .containsExactlyInAnyOrder("Role Added", "Role Removed");
        assertThat(response.affectedIdentities())
                .allSatisfy(identity -> assertThat(identity.impactTypes()).doesNotContain("Mixed"));

        verify(roleRepository, never()).save(org.mockito.Mockito.any(AccessRole.class));
        verify(identityRoleAssignmentRepository, never()).saveAllAndFlush(anyList());
        verify(entitlementLinkRepository, never()).deleteByRoleId(roleId);
        verify(roleEntitlementClosureRepository, never()).deleteByTenantIdAndRoleId(TENANT_ID, roleId);
    }

    @Test
    void simulateRoleSaveImpactIncludesUnchangedEntitlementsWhenRequested() {
        UUID roleId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Finance Viewer", "Finance Viewer");
        IdentityRoleAssignment assignment = assignment(UUID.randomUUID(), identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Stable", "User", "Stable User");
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Stable Finance Group");

        AccessRoleSaveSimulationRequest request = new AccessRoleSaveSimulationRequest();
        request.setIdentityIds(List.of(identityId));
        request.setEntitlementIds(List.of(entitlementId));
        request.setChildRoleIds(List.of());

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(entitlementLinkRepository.findByRoleId(roleId)).thenReturn(List.of(roleEntitlement(roleId, entitlementId)));
        when(compositionRepository.findByParentRoleId(roleId)).thenReturn(List.of());
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(closure(roleId, entitlementId, roleId, true, 0, List.of(roleId))));
        when(identityRepository.findAllById(anySet())).thenReturn(List.of(identity));
        when(entitlementRepository.findAllById(anySet())).thenReturn(List.of(entitlement));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(resourceId))).thenReturn(Map.of(resourceId, "Finance App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role));

        AccessRoleSaveSimulationResponse response = service.simulateRoleSaveImpact(TENANT_ID, roleId, request, true);

        assertThat(response.summary().totalAffectedUsers()).isZero();
        assertThat(response.summary().addedEntitlements()).isZero();
        assertThat(response.summary().removedEntitlements()).isZero();
        assertThat(response.summary().unchangedEntitlements()).isEqualTo(1);
        assertThat(response.affectedIdentities()).singleElement().satisfies(user -> {
            assertThat(user.overallImpactType()).isEqualTo("Role Unchanged");
            assertThat(user.impactTypes()).containsExactly("Role Unchanged", "Entitlement Unchanged");
            assertThat(user.roleImpact().impactType()).isEqualTo("Role Unchanged");
            assertThat(user.entitlementImpacts()).singleElement().satisfies(impact -> {
                assertThat(impact.impactType()).isEqualTo("Entitlement Unchanged");
                assertThat(impact.status()).isEqualTo("Already Assigned");
            });
        });
    }

    @Test
    void simulateRoleSaveImpactShowsAddedChildRoleDetailsForEachIdentity() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID secondChildRoleId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID secondInheritedEntitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        AccessRole role = accessRole(roleId, "Finance Parent", "Finance Parent");
        AccessRole childRole = accessRole(childRoleId, "Payroll Child", "Payroll Child");
        AccessRole secondChildRole = accessRole(secondChildRoleId, "Benefits Child", "Benefits Child");
        IdentityRoleAssignment assignment = assignment(UUID.randomUUID(), identityId, roleId, 1778000000000L);
        Employee identity = employee(identityId, "Child", "User", "Child User");
        Entitlement inheritedEntitlement = entitlement(inheritedEntitlementId, resourceId, "Payroll Group");
        Entitlement secondInheritedEntitlement = entitlement(secondInheritedEntitlementId, resourceId, "Benefits Group");

        AccessRoleSaveSimulationRequest request = new AccessRoleSaveSimulationRequest();
        request.setIdentityIds(List.of(identityId));
        request.setEntitlementIds(List.of());
        request.setChildRoleIds(List.of(childRoleId, secondChildRoleId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(assignment));
        when(entitlementLinkRepository.findByRoleId(roleId)).thenReturn(List.of());
        when(compositionRepository.findByParentRoleId(roleId)).thenReturn(List.of());
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, childRoleId))
                .thenReturn(List.of(closure(childRoleId, inheritedEntitlementId, childRoleId, true, 0, List.of(childRoleId))));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleId(TENANT_ID, secondChildRoleId))
                .thenReturn(List.of(closure(secondChildRoleId, secondInheritedEntitlementId, secondChildRoleId, true, 0, List.of(secondChildRoleId))));
        when(identityRepository.findAllById(anySet())).thenReturn(List.of(identity));
        when(entitlementRepository.findAllById(anySet())).thenReturn(List.of(inheritedEntitlement, secondInheritedEntitlement));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(resourceId))).thenReturn(Map.of(resourceId, "Payroll App"));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(role, childRole, secondChildRole));

        AccessRoleSaveSimulationResponse response = service.simulateRoleSaveImpact(TENANT_ID, roleId, request, false);

        assertThat(response.summary().childRoleChanges()).isEqualTo(2);
        assertThat(response.summary().addedEntitlements()).isEqualTo(2);
        assertThat(response.affectedIdentities()).singleElement().satisfies(user -> {
            assertThat(user.overallImpactType()).isEqualTo("Child Role Added");
            assertThat(user.impactTypes()).containsExactly("Child Role Added", "Entitlement Added");
            assertThat(user.childRoleImpacts()).hasSize(2);
            assertThat(user.childRoleImpacts()).extracting("childRoleName")
                    .containsExactlyInAnyOrder("Payroll Child", "Benefits Child");
            assertThat(user.childRoleImpacts()).allSatisfy(childImpact -> {
                assertThat(childImpact.impactType()).isEqualTo("Child Role Added");
                assertThat(childImpact.status()).isEqualTo("Will Be Inherited");
                assertThat(childImpact.currentlyInherited()).isFalse();
                assertThat(childImpact.willBeInherited()).isTrue();
            });
            assertThat(user.entitlementImpacts()).hasSize(2);
            assertThat(user.entitlementImpacts()).extracting("viaChildRoleName")
                    .containsExactlyInAnyOrder("Payroll Child", "Benefits Child");
        });
    }

    private Page<AccessRoleMemberResponse> getRoleMembers(UUID roleId) {
        return service.getRoleMembers(
                TENANT_ID,
                roleId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10));
    }

    private Page<AccessRoleGroupedMemberResponse> getRoleMembersGrouped(UUID roleId) {
        return service.getRoleMembersGrouped(
                TENANT_ID,
                roleId,
                null,
                PageRequest.of(0, 10));
    }

    private Page<AccessRoleMemberEntitlementResponse> getRoleMemberEntitlements(UUID roleId, UUID assignmentId) {
        return service.getRoleMemberEntitlements(
                TENANT_ID,
                roleId,
                assignmentId,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10));
    }

    private AccessRole accessRole(UUID id, String name, String displayName) {
        AccessRole role = new AccessRole();
        role.setId(id);
        role.setTenantId(TENANT_ID);
        role.setName(name);
        role.setDisplayName(displayName);
        role.setType("business");
        role.setStatus("active");
        role.setBusinessUnit("Finance");
        role.setOrganization(UUID.randomUUID());
        return role;
    }

    private OrgUnit orgUnit(UUID id) {
        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setId(id);
        orgUnit.setTenantId(TENANT_ID);
        orgUnit.setName("Finance");
        return orgUnit;
    }

    private IdentityRoleAssignment assignment(UUID id, UUID identityId, UUID roleId, Long assignedAt) {
        IdentityRoleAssignment assignment = new IdentityRoleAssignment();
        assignment.setId(id);
        assignment.setTenantId(TENANT_ID);
        assignment.setIdentityId(identityId);
        assignment.setRoleId(roleId);
        assignment.setStatus(IdentityRoleAssignment.Status.ACTIVE);
        assignment.setSourceType(IdentityRoleAssignment.SourceType.RULE);
        assignment.setSourceKey("rule-1");
        assignment.setAssignedAt(assignedAt);
        return assignment;
    }

    private AccessRoleComposition composition(UUID parentRoleId, UUID childRoleId) {
        AccessRoleComposition composition = new AccessRoleComposition();
        composition.setParentRoleId(parentRoleId);
        composition.setChildRoleId(childRoleId);
        return composition;
    }

    private AccessRoleEntitlement roleEntitlement(UUID roleId, UUID entitlementId) {
        AccessRoleEntitlement link = new AccessRoleEntitlement();
        link.setRoleId(roleId);
        link.setEntitlementId(entitlementId);
        return link;
    }

    private Employee employee(UUID id, String firstName, String lastName, String displayName) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setTenantId(TENANT_ID);
        employee.setIdentityType(IdentityType.EMPLOYEE);
        employee.setEmployeeId("emp-" + id);
        employee.setUsername(firstName.toLowerCase() + "." + lastName.toLowerCase());
        employee.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
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

    private Entitlement entitlement(UUID id, UUID resourceId, String displayName) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(id);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setResourceId(resourceId);
        entitlement.setSourceUid("source-" + id);
        entitlement.setDisplayName(displayName);
        return entitlement;
    }

    private RoleEntitlementClosure closure(UUID roleId,
                                           UUID entitlementId,
                                           UUID sourceRoleId,
                                           boolean direct,
                                           int depth,
                                           List<UUID> pathRoleIds) {
        RoleEntitlementClosure closure = new RoleEntitlementClosure();
        closure.setTenantId(TENANT_ID);
        closure.setRoleId(roleId);
        closure.setEntitlementId(entitlementId);
        closure.setSourceRoleId(sourceRoleId);
        closure.setDirect(direct);
        closure.setDepth(depth);
        closure.setPathRoleIds(pathRoleIds);
        return closure;
    }
}
