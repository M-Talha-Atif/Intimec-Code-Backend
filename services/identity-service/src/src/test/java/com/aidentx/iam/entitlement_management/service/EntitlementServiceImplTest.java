package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.entitlement_management.audit.EntitlementManualSyncAuditService;
import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.mapper.EntitlementMapper;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentRequest;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersRequest;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersResponse;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignment;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.dto.IdentityAccessBreakdownDto;
import com.aidentx.iam.identity_management.model.dto.IdentityEntitlementViewDto;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.iam.shadow_management.sync.EntitlementMappingEngine;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private EntitlementAssignmentRepository assignmentRepository;

    @Mock
    private EntitlementMapper entitlementMapper;

    @Mock
    private EntitlementMappingEngine mappingEngine;

    @Mock
    private ShadowRepository shadowRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private ApplicationClient applicationClient;

    @Mock
    private EntitlementSchemaService entitlementSchemaService;

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private IdentityRoleAssignmentRepository identityRoleAssignmentRepository;

    @Mock
    private AccessRoleAssignmentRuleRepository accessRoleAssignmentRuleRepository;

    @Mock
    private RoleEntitlementClosureRepository roleEntitlementClosureRepository;

    @Mock
    private EntitlementManualSyncAuditService manualSyncAuditService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private EntitlementServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(EntitlementServiceImpl.class)).thenReturn(logger);
        service = new EntitlementServiceImpl(
                entitlementRepository,
                assignmentRepository,
                entitlementMapper,
                mappingEngine,
                shadowRepository,
                identityRepository,
                applicationClient,
                entitlementSchemaService,
                accessRoleRepository,
                identityRoleAssignmentRepository,
                accessRoleAssignmentRuleRepository,
                roleEntitlementClosureRepository,
                manualSyncAuditService,
                loggerFactory);
    }

    @Test
    void getEntitlementMembersSearchesIdentityNameAndReturnsIdentityRiskLevel() {
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        EntitlementAssignment assignment = assignment(shadowId, entitlementId, resourceId, "DIRECT", null);
        Shadow shadow = accountShadow(shadowId, identityId, resourceId);
        NonHumanIdentity identity = nonHumanIdentity(identityId, "PRIVILEGED_BOT", "high");

        when(assignmentRepository.findByEntitlementIdAndTenantIdAndMemberSearch(
                entitlementId, TENANT_ID, "%privileged%", pageable))
                .thenReturn(new PageImpl<>(List.of(assignment), pageable, 1));
        when(shadowRepository.findAllById(anyCollection())).thenReturn(List.of(shadow));
        when(identityRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection())).thenReturn(List.of(identity));

        Page<?> result = service.getEntitlementMembers(entitlementId, TENANT_ID, "privileged", pageable);

        assertThat(result.getContent()).singleElement().satisfies(member -> {
            var dto = (com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementMemberDto) member;
            assertThat(dto.accountName()).isEqualTo("account-1");
            assertThat(dto.identityId()).isEqualTo(identityId);
            assertThat(dto.identityName()).isEqualTo("PRIVILEGED_BOT");
            assertThat(dto.identityRiskLevel()).isEqualTo("high");
            assertThat(dto.source()).isEqualTo("DIRECT");
        });
        verify(assignmentRepository).findByEntitlementIdAndTenantIdAndMemberSearch(
                entitlementId, TENANT_ID, "%privileged%", pageable);
    }

    @Test
    void getEntitlementMembersReturnsHumanIdentityRiskLevel() {
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        EntitlementAssignment assignment = assignment(shadowId, entitlementId, resourceId, "DIRECT", null);
        Shadow shadow = accountShadow(shadowId, identityId, resourceId);
        Employee identity = employee(identityId, "RISKY_EMPLOYEE", "medium");

        when(assignmentRepository.findByEntitlementIdAndTenantId(entitlementId, TENANT_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(assignment), pageable, 1));
        when(shadowRepository.findAllById(anyCollection())).thenReturn(List.of(shadow));
        when(identityRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection())).thenReturn(List.of(identity));

        Page<?> result = service.getEntitlementMembers(entitlementId, TENANT_ID, null, pageable);

        assertThat(result.getContent()).singleElement().satisfies(member -> {
            var dto = (com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementMemberDto) member;
            assertThat(dto.identityName()).isEqualTo("RISKY_EMPLOYEE");
            assertThat(dto.identityRiskLevel()).isEqualTo("medium");
        });
    }

    @Test
    void getEntitlementsByIdentityIdAddsRoleDirectAndInheritedPathProvenance() {
        UUID identityId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID directEntitlementId = UUID.randomUUID();
        UUID roleDirectEntitlementId = UUID.randomUUID();
        UUID inheritedEntitlementId = UUID.randomUUID();
        UUID parentRoleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();

        EntitlementAssignment directAssignment = assignment(shadowId, directEntitlementId, resourceId, "IMPORT", null);
        directAssignment.setAssignedByUserId("admin-1");
        directAssignment.setAssignedByUserName("Jane Admin");
        EntitlementAssignment roleDirectAssignment = assignment(shadowId, roleDirectEntitlementId, resourceId, "DYNAMIC", parentRoleId);
        EntitlementAssignment inheritedAssignment = assignment(shadowId, inheritedEntitlementId, resourceId, "DYNAMIC", parentRoleId);

        RoleEntitlementClosure directClosure = closure(parentRoleId, roleDirectEntitlementId, parentRoleId, true, 0, List.of(parentRoleId));
        RoleEntitlementClosure inheritedClosure = closure(parentRoleId, inheritedEntitlementId, childRoleId, false, 1, List.of(parentRoleId, childRoleId));

        when(shadowRepository.findShadowIdsByLinkedIdentityId(identityId, TENANT_ID)).thenReturn(List.of(shadowId));
        when(assignmentRepository.findByIdentityIdOrShadowIdInAndTenantId(
                eq(identityId), eq(List.of(shadowId)), eq(TENANT_ID), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(directAssignment, roleDirectAssignment, inheritedAssignment), PageRequest.of(0, 20), 3));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(
                entitlement(directEntitlementId, resourceId, "Imported Group"),
                entitlement(roleDirectEntitlementId, resourceId, "Parent Direct Group"),
                entitlement(inheritedEntitlementId, resourceId, "Child Group")));
        when(shadowRepository.findAllByIdInAndTenantId(List.of(shadowId), TENANT_ID)).thenReturn(List.of(shadow(shadowId, resourceId)));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(resourceId))).thenReturn(Map.of(resourceId, "Finance App"));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleIdInAndEntitlementIdIn(
                eq(TENANT_ID), anyCollection(), anyCollection())).thenReturn(List.of(directClosure, inheritedClosure));
        when(accessRoleRepository.findByIdIn(anyList())).thenReturn(List.of(
                role(parentRoleId, "Finance Manager"),
                role(childRoleId, "Finance Analyst")));

        Page<IdentityEntitlementViewDto> result = service.getEntitlementsByIdentityId(identityId, TENANT_ID, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(3);
        IdentityEntitlementViewDto imported = result.getContent().get(0);
        IdentityEntitlementViewDto roleDirect = result.getContent().get(1);
        IdentityEntitlementViewDto inherited = result.getContent().get(2);

        assertThat(imported.accessPathType()).isEqualTo("DIRECT_ENTITLEMENT");
        assertThat(imported.accessRoleId()).isNull();
        assertThat(imported.rolePath()).isEmpty();
        assertThat(imported.source()).isEqualTo("DIRECT");
        assertThat(imported.assignmentStatus()).isEqualTo("ASSIGNED");
        assertThat(imported.assignmentStatusReason()).isNull();
        assertThat(imported.assignedByUserId()).isEqualTo("admin-1");
        assertThat(imported.assignedByUserName()).isEqualTo("Jane Admin");

        assertThat(roleDirect.accessPathType()).isEqualTo("ROLE_DIRECT");
        assertThat(roleDirect.accessRoleId()).isEqualTo(parentRoleId);
        assertThat(roleDirect.accessRoleName()).isEqualTo("Finance Manager");
        assertThat(roleDirect.sourceRoleId()).isEqualTo(parentRoleId);
        assertThat(roleDirect.roleEntitlementDirect()).isTrue();
        assertThat(roleDirect.roleEntitlementDepth()).isZero();
        assertThat(roleDirect.rolePath()).extracting("roleName").containsExactly("Finance Manager");

        assertThat(inherited.accessPathType()).isEqualTo("ROLE_INHERITED_CHILD");
        assertThat(inherited.accessRoleId()).isEqualTo(parentRoleId);
        assertThat(inherited.accessRoleName()).isEqualTo("Finance Manager");
        assertThat(inherited.sourceRoleId()).isEqualTo(childRoleId);
        assertThat(inherited.sourceRoleName()).isEqualTo("Finance Analyst");
        assertThat(inherited.roleEntitlementDirect()).isFalse();
        assertThat(inherited.roleEntitlementDepth()).isEqualTo(1);
        assertThat(inherited.rolePath()).extracting("roleName").containsExactly("Finance Manager", "Finance Analyst");
    }

    @Test
    void getIdentityAccessBreakdownGroupsRoleEntitlementsKeepsDuplicatesAndSearchesRolesAndEntitlements() {
        UUID identityId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        EntitlementAssignment directAssignment = assignment(shadowId, entitlementId, resourceId, "DIRECT", null);
        directAssignment.setAssignedByUserId("admin-1");
        directAssignment.setAssignedByUserName("Jane Admin");
        EntitlementAssignment dynamicAssignment = assignment(shadowId, entitlementId, resourceId, "DYNAMIC", null);
        EntitlementAssignment roleDerivedAssignment = assignment(shadowId, entitlementId, resourceId, "ROLE_DERIVED", roleId);
        RoleEntitlementClosure closure = closure(roleId, entitlementId, roleId, true, 0, List.of(roleId));
        AccessRole role = role(roleId, "Finance Manager");
        IdentityRoleAssignment roleAssignment = identityRoleAssignment(identityId, roleId, IdentityRoleAssignment.SourceType.MANUAL, 50L);
        roleAssignment.setAssignedByUserId("role-admin-1");
        roleAssignment.setAssignedByUserName("Role Admin");

        when(shadowRepository.findShadowIdsByLinkedIdentityId(identityId, TENANT_ID)).thenReturn(List.of(shadowId));
        when(assignmentRepository.findByIdentityIdOrShadowIdInAndTenantId(
                eq(identityId), eq(List.of(shadowId)), eq(TENANT_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(directAssignment, dynamicAssignment, roleDerivedAssignment), PageRequest.of(0, 20), 3));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId, "Finance Entitlement")));
        when(shadowRepository.findAllByIdInAndTenantId(List.of(shadowId), TENANT_ID)).thenReturn(List.of(shadow(shadowId, resourceId)));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(resourceId))).thenReturn(Map.of(resourceId, "Finance App"));
        when(roleEntitlementClosureRepository.findByTenantIdAndRoleIdInAndEntitlementIdIn(
                eq(TENANT_ID), anyCollection(), anyCollection())).thenReturn(List.of(closure));
        when(accessRoleRepository.findByIdIn(anyList())).thenReturn(List.of(role));
        when(identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                TENANT_ID, identityId, IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(roleAssignment));

        IdentityAccessBreakdownDto result = service.getIdentityAccessBreakdown(identityId, TENANT_ID, null);

        assertThat(result.totalAssignments()).isEqualTo(3);
        assertThat(result.totalRoleEntitlements()).isEqualTo(1);
        assertThat(result.totalStandaloneEntitlements()).isEqualTo(2);
        assertThat(result.businessRoles()).singleElement().satisfies(roleGroup -> {
            assertThat(roleGroup.roleName()).isEqualTo("Finance Manager");
            assertThat(roleGroup.assignmentSource()).isEqualTo("Manual");
            assertThat(roleGroup.assignedByUserId()).isEqualTo("role-admin-1");
            assertThat(roleGroup.assignedByUserName()).isEqualTo("Role Admin");
            assertThat(roleGroup.entitlementCount()).isEqualTo(1);
            assertThat(roleGroup.entitlements()).singleElement().satisfies(entitlement -> {
                assertThat(entitlement.entitlementId()).isEqualTo(entitlementId);
                assertThat(entitlement.source()).isEqualTo("Role");
                assertThat(entitlement.accessPathType()).isEqualTo("Role Direct");
            });
        });
        assertThat(result.standaloneEntitlements()).extracting("source").containsExactly("Direct", "Dynamic");
        assertThat(result.standaloneEntitlements()).extracting("entitlementId").containsExactly(entitlementId, entitlementId);
        assertThat(result.standaloneEntitlements().get(0).assignedByUserId()).isEqualTo("admin-1");
        assertThat(result.standaloneEntitlements().get(0).assignedByUserName()).isEqualTo("Jane Admin");

        IdentityAccessBreakdownDto roleSearch = service.getIdentityAccessBreakdown(identityId, TENANT_ID, "manager");
        assertThat(roleSearch.businessRoles()).singleElement().satisfies(roleGroup ->
                assertThat(roleGroup.entitlementCount()).isEqualTo(1));
        assertThat(roleSearch.standaloneEntitlements()).isEmpty();

        IdentityAccessBreakdownDto entitlementSearch = service.getIdentityAccessBreakdown(identityId, TENANT_ID, "finance entitlement");
        assertThat(entitlementSearch.totalAssignments()).isEqualTo(3);
        assertThat(entitlementSearch.businessRoles()).singleElement().satisfies(roleGroup ->
                assertThat(roleGroup.entitlementCount()).isEqualTo(1));
        assertThat(entitlementSearch.standaloneEntitlements()).hasSize(2);
    }

    @Test
    void assignEntitlementToShadowStoresImportAsDirectAssignment() {
        UUID shadowId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(assignmentRepository.findByShadowIdAndEntitlementIdAndTenantIdAndSourceIgnoreCase(
                shadowId, entitlementId, TENANT_ID, "DIRECT"))
                .thenReturn(java.util.Optional.empty());

        service.assignEntitlementToShadow(shadowId, entitlementId, resourceId, TENANT_ID, "sync-1");

        ArgumentCaptor<EntitlementAssignment> assignmentCaptor = ArgumentCaptor.forClass(EntitlementAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getSource()).isEqualTo("DIRECT");
    }

    @Test
    void assignEntitlementMembersCreatesDirectAssignedAssignmentWhenTargetShadowExists() {
        UUID identityId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Finance Group");

        when(entitlementRepository.findById(entitlementId)).thenReturn(java.util.Optional.of(entitlement));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(accountShadow(shadowId, identityId, resourceId)));
        when(assignmentRepository.findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                TENANT_ID, identityId, entitlementId, "DIRECT")).thenReturn(java.util.Optional.empty());
        when(assignmentRepository.findByShadowIdAndEntitlementIdAndTenantIdAndSourceIgnoreCase(
                shadowId, entitlementId, TENANT_ID, "DIRECT"))
                .thenReturn(java.util.Optional.empty());
        when(assignmentRepository.save(org.mockito.ArgumentMatchers.any(EntitlementAssignment.class)))
                .thenAnswer(invocation -> {
                    EntitlementAssignment assignment = invocation.getArgument(0);
                    assignment.setId(UUID.randomUUID());
                    return assignment;
                });

        var response = service.assignEntitlementMembers(
                entitlementId,
                TENANT_ID,
                new ManualEntitlementAssignmentRequest(List.of(identityId)),
                "admin");

        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(response.pendingAssignments()).isZero();
        assertThat(response.assignments()).singleElement().satisfies(result -> {
            assertThat(result.identityId()).isEqualTo(identityId);
            assertThat(result.shadowId()).isEqualTo(shadowId);
            assertThat(result.assignmentStatus()).isEqualTo("ASSIGNED");
            assertThat(result.action()).isEqualTo("CREATED");
        });
        ArgumentCaptor<EntitlementAssignment> assignmentCaptor = ArgumentCaptor.forClass(EntitlementAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getSource()).isEqualTo("DIRECT");
        assertThat(assignmentCaptor.getValue().getStatus()).isEqualTo(EntitlementAssignment.AssignmentStatus.ASSIGNED);
        assertThat(assignmentCaptor.getValue().getAssignedByUserId()).isEqualTo("admin");
        assertThat(assignmentCaptor.getValue().getAssignedByUserName()).isEqualTo("admin");

        ArgumentCaptor<SyncManualEntitlementMembersResponse> auditResponseCaptor = ArgumentCaptor.forClass(SyncManualEntitlementMembersResponse.class);
        verify(manualSyncAuditService).auditManualSync(eq(TENANT_ID), eq(entitlement), auditResponseCaptor.capture(), eq("admin"));
        assertThat(auditResponseCaptor.getValue().addedCount()).isEqualTo(1);
        assertThat(auditResponseCaptor.getValue().skippedCount()).isZero();
        assertThat(auditResponseCaptor.getValue().added()).singleElement().satisfies(result -> {
            assertThat(result.identityId()).isEqualTo(identityId);
            assertThat(result.assignments()).singleElement().satisfies(assignmentResult ->
                    assertThat(assignmentResult.action()).isEqualTo("CREATED"));
        });
    }

    @Test
    void assignEntitlementMembersCreatesPendingDirectAssignmentWhenIdentityHasNoTargetShadow() {
        UUID identityId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Finance Group");

        when(entitlementRepository.findById(entitlementId)).thenReturn(java.util.Optional.of(entitlement));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID)).thenReturn(List.of());
        when(assignmentRepository.findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                TENANT_ID, identityId, entitlementId, "DIRECT")).thenReturn(java.util.Optional.empty());
        when(assignmentRepository.save(org.mockito.ArgumentMatchers.any(EntitlementAssignment.class)))
                .thenAnswer(invocation -> {
                    EntitlementAssignment assignment = invocation.getArgument(0);
                    assignment.setId(UUID.randomUUID());
                    return assignment;
                });

        var response = service.assignEntitlementMembers(
                entitlementId,
                TENANT_ID,
                new ManualEntitlementAssignmentRequest(List.of(identityId)),
                "admin");

        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(response.pendingAssignments()).isEqualTo(1);
        assertThat(response.assignments()).singleElement().satisfies(result -> {
            assertThat(result.identityId()).isEqualTo(identityId);
            assertThat(result.shadowId()).isNull();
            assertThat(result.assignmentStatus()).isEqualTo("PENDING");
            assertThat(result.assignmentStatusReason()).isEqualTo("Identity has no account on the target application");
        });
        ArgumentCaptor<EntitlementAssignment> assignmentCaptor = ArgumentCaptor.forClass(EntitlementAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getSource()).isEqualTo("DIRECT");
        assertThat(assignmentCaptor.getValue().getStatus()).isEqualTo(EntitlementAssignment.AssignmentStatus.PENDING);
        assertThat(assignmentCaptor.getValue().getAssignedByUserId()).isEqualTo("admin");
        assertThat(assignmentCaptor.getValue().getAssignedByUserName()).isEqualTo("admin");
    }

    @Test
    void assignEntitlementMembersBackfillsAssignedByMetadataWhenDirectAssignmentAlreadyExists() {
        UUID identityId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Finance Group");
        EntitlementAssignment existing = assignment(shadowId, entitlementId, resourceId, "DIRECT", null);

        when(entitlementRepository.findById(entitlementId)).thenReturn(java.util.Optional.of(entitlement));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(accountShadow(shadowId, identityId, resourceId)));
        when(assignmentRepository.findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                TENANT_ID, identityId, entitlementId, "DIRECT")).thenReturn(java.util.Optional.empty());
        when(assignmentRepository.findByShadowIdAndEntitlementIdAndTenantIdAndSourceIgnoreCase(
                shadowId, entitlementId, TENANT_ID, "DIRECT"))
                .thenReturn(java.util.Optional.of(existing));
        when(assignmentRepository.save(existing)).thenReturn(existing);

        var response = service.assignEntitlementMembers(
                entitlementId,
                TENANT_ID,
                new ManualEntitlementAssignmentRequest(List.of(identityId)),
                "admin-1",
                "Jane Admin");

        assertThat(response.assignmentsUpdated()).isEqualTo(1);
        assertThat(existing.getAssignedByUserId()).isEqualTo("admin-1");
        assertThat(existing.getAssignedByUserName()).isEqualTo("Jane Admin");
        verify(assignmentRepository).save(existing);
    }

    @Test
    void assignEntitlementMembersCreatesDirectAssignmentWhenDynamicAlreadyExists() {
        UUID identityId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Finance Group");
        EntitlementAssignment existing = assignment(shadowId, entitlementId, resourceId, "DYNAMIC", UUID.randomUUID());
        existing.setRuleId(UUID.randomUUID());

        when(entitlementRepository.findById(entitlementId)).thenReturn(java.util.Optional.of(entitlement));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(accountShadow(shadowId, identityId, resourceId)));
        when(assignmentRepository.findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                TENANT_ID, identityId, entitlementId, "DIRECT")).thenReturn(java.util.Optional.empty());
        when(assignmentRepository.findByShadowIdAndEntitlementIdAndTenantIdAndSourceIgnoreCase(
                shadowId, entitlementId, TENANT_ID, "DIRECT"))
                .thenReturn(java.util.Optional.empty());
        when(assignmentRepository.save(org.mockito.ArgumentMatchers.any(EntitlementAssignment.class)))
                .thenAnswer(invocation -> {
                    EntitlementAssignment assignment = invocation.getArgument(0);
                    assignment.setId(UUID.randomUUID());
                    return assignment;
                });

        var response = service.assignEntitlementMembers(
                entitlementId,
                TENANT_ID,
                new ManualEntitlementAssignmentRequest(List.of(identityId)),
                "admin");

        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(existing.getSource()).isEqualTo("DYNAMIC");
        assertThat(existing.getRuleId()).isNotNull();
        assertThat(existing.getAccessRoleId()).isNotNull();
        assertThat(response.assignments()).singleElement().satisfies(result ->
                assertThat(result.action()).isEqualTo("CREATED"));
    }

    @Test
    void getEntitlementsByIdentityIdReturnsPendingAssignmentWhenIdentityHasNoShadow() {
        UUID identityId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        EntitlementAssignment pendingAssignment = pendingAssignment(identityId, entitlementId, resourceId);

        when(shadowRepository.findShadowIdsByLinkedIdentityId(identityId, TENANT_ID)).thenReturn(List.of());
        when(assignmentRepository.findByIdentityIdAndTenantId(identityId, TENANT_ID, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(pendingAssignment), PageRequest.of(0, 20), 1));
        when(entitlementRepository.findAllById(anyCollection()))
                .thenReturn(List.of(entitlement(entitlementId, resourceId, "Pending Group")));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(resourceId))).thenReturn(Map.of(resourceId, "Finance App"));

        Page<IdentityEntitlementViewDto> result = service.getEntitlementsByIdentityId(identityId, TENANT_ID, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).singleElement().satisfies(view -> {
            assertThat(view.shadowId()).isNull();
            assertThat(view.accountName()).isEqualTo("Unknown");
            assertThat(view.entitlementId()).isEqualTo(entitlementId);
            assertThat(view.entitlementName()).isEqualTo("Pending Group");
            assertThat(view.source()).isEqualTo("DIRECT");
            assertThat(view.assignmentStatus()).isEqualTo("PENDING");
            assertThat(view.assignmentStatusReason()).isEqualTo("Identity has no account on the target application");
        });
    }

    @Test
    void syncManualEntitlementMembersAddsMissingRevokesOmittedAndSkipsExistingDirectMembers() {
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID existingIdentityId = UUID.randomUUID();
        UUID identityToAdd = UUID.randomUUID();
        UUID identityToRevoke = UUID.randomUUID();
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Finance Group");
        EntitlementAssignment existingDirect = pendingAssignment(existingIdentityId, entitlementId, resourceId);
        EntitlementAssignment revokedDirect = pendingAssignment(identityToRevoke, entitlementId, resourceId);

        when(entitlementRepository.findById(entitlementId)).thenReturn(java.util.Optional.of(entitlement));
        when(assignmentRepository.findStandaloneDirectAssignmentsByTenantIdAndEntitlementId(TENANT_ID, entitlementId))
                .thenReturn(List.of(existingDirect, revokedDirect));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityToAdd, TENANT_ID)).thenReturn(List.of());
        when(assignmentRepository.findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                TENANT_ID, identityToAdd, entitlementId, "DIRECT")).thenReturn(java.util.Optional.empty());
        when(assignmentRepository.save(org.mockito.ArgumentMatchers.any(EntitlementAssignment.class)))
                .thenAnswer(invocation -> {
                    EntitlementAssignment assignment = invocation.getArgument(0);
                    assignment.setId(UUID.randomUUID());
                    return assignment;
                });

        var response = service.syncManualEntitlementMembers(
                entitlementId,
                TENANT_ID,
                new SyncManualEntitlementMembersRequest(List.of(existingIdentityId, identityToAdd)),
                "admin");

        assertThat(response.addedCount()).isEqualTo(1);
        assertThat(response.revokedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(response.totalDirectCount()).isEqualTo(2);
        assertThat(response.added()).singleElement().satisfies(result ->
                assertThat(result.identityId()).isEqualTo(identityToAdd));
        assertThat(response.revoked()).singleElement().satisfies(result -> {
            assertThat(result.identityId()).isEqualTo(identityToRevoke);
            assertThat(result.assignments()).singleElement().satisfies(assignmentResult ->
                    assertThat(assignmentResult.action()).isEqualTo("REVOKED"));
        });
        verify(assignmentRepository).deleteAll(List.of(revokedDirect));
        verify(manualSyncAuditService).auditManualSync(TENANT_ID, entitlement, response, "admin");
    }

    @Test
    void syncManualEntitlementMembersDoesNotRevokeDynamicOrRoleDerivedAssignments() {
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID standaloneIdentityId = UUID.randomUUID();
        UUID roleIdentityId = UUID.randomUUID();
        Entitlement entitlement = entitlement(entitlementId, resourceId, "Finance Group");
        EntitlementAssignment standaloneDirect = pendingAssignment(standaloneIdentityId, entitlementId, resourceId);
        EntitlementAssignment roleDerivedDirect = pendingAssignment(roleIdentityId, entitlementId, resourceId);
        roleDerivedDirect.setAccessRoleId(UUID.randomUUID());

        when(entitlementRepository.findById(entitlementId)).thenReturn(java.util.Optional.of(entitlement));
        when(assignmentRepository.findStandaloneDirectAssignmentsByTenantIdAndEntitlementId(TENANT_ID, entitlementId))
                .thenReturn(List.of(standaloneDirect));

        var response = service.syncManualEntitlementMembers(
                entitlementId,
                TENANT_ID,
                new SyncManualEntitlementMembersRequest(List.of()),
                "admin");

        assertThat(response.revokedCount()).isEqualTo(1);
        ArgumentCaptor<List<EntitlementAssignment>> deletedAssignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).deleteAll(deletedAssignmentsCaptor.capture());
        assertThat(deletedAssignmentsCaptor.getValue()).containsExactly(standaloneDirect);
        assertThat(deletedAssignmentsCaptor.getValue()).doesNotContain(roleDerivedDirect);
    }

    private EntitlementAssignment assignment(UUID shadowId, UUID entitlementId, UUID resourceId, String source, UUID accessRoleId) {
        EntitlementAssignment assignment = new EntitlementAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setTenantId(TENANT_ID);
        assignment.setShadowId(shadowId);
        assignment.setEntitlementId(entitlementId);
        assignment.setResourceId(resourceId);
        assignment.setSource(source);
        assignment.setAccessRoleId(accessRoleId);
        assignment.setAssignedAt(100L);
        assignment.setUpdatedAt(200L);
        return assignment;
    }

    private EntitlementAssignment pendingAssignment(UUID identityId, UUID entitlementId, UUID resourceId) {
        EntitlementAssignment assignment = assignment(null, entitlementId, resourceId, "MANUAL", null);
        assignment.setIdentityId(identityId);
        assignment.setStatus(EntitlementAssignment.AssignmentStatus.PENDING);
        return assignment;
    }

    private Entitlement entitlement(UUID entitlementId, UUID resourceId, String displayName) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setResourceId(resourceId);
        entitlement.setDisplayName(displayName);
        entitlement.setDescription(displayName + " description");
        entitlement.setKind("group");
        entitlement.setObjectClass("__GROUP__");
        entitlement.setSourceUid("src-" + entitlementId);
        return entitlement;
    }

    private Shadow shadow(UUID shadowId, UUID resourceId) {
        Shadow shadow = new Shadow();
        shadow.setId(shadowId);
        shadow.setTenantId(TENANT_ID);
        shadow.setResourceId(resourceId.toString());
        shadow.setResourceUid("account-1");
        return shadow;
    }

    private Shadow accountShadow(UUID shadowId, UUID identityId, UUID resourceId) {
        Shadow shadow = shadow(shadowId, resourceId);
        shadow.setLinkedIdentityId(identityId);
        shadow.setKind("account");
        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.LINKED);
        return shadow;
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

    private AccessRole role(UUID roleId, String name) {
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName(name);
        role.setType("business");
        role.setStatus("active");
        role.setRiskLevel("medium");
        role.setBusinessUnit("Finance");
        return role;
    }

    private IdentityRoleAssignment identityRoleAssignment(UUID identityId,
                                                          UUID roleId,
                                                          IdentityRoleAssignment.SourceType sourceType,
                                                          Long assignedAt) {
        IdentityRoleAssignment assignment = new IdentityRoleAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setTenantId(TENANT_ID);
        assignment.setIdentityId(identityId);
        assignment.setRoleId(roleId);
        assignment.setSourceType(sourceType);
        assignment.setStatus(IdentityRoleAssignment.Status.ACTIVE);
        assignment.setAssignedAt(assignedAt);
        return assignment;
    }

    private NonHumanIdentity nonHumanIdentity(UUID identityId, String serviceName, String riskLevel) {
        NonHumanIdentity identity = new NonHumanIdentity();
        identity.setId(identityId);
        identity.setTenantId(TENANT_ID);
        identity.setIdentityType(IdentityType.NON_HUMAN);
        identity.setServiceName(serviceName);
        identity.setRiskLevel(riskLevel);
        return identity;
    }

    private Employee employee(UUID identityId, String displayName, String riskLevel) {
        Employee employee = new Employee();
        employee.setId(identityId);
        employee.setTenantId(TENANT_ID);
        employee.setIdentityType(IdentityType.EMPLOYEE);
        employee.setEmployeeId("emp-" + identityId);
        employee.setUsername(displayName.toLowerCase());
        employee.setEmail(displayName.toLowerCase() + "@example.com");
        employee.setDisplayName(displayName);
        employee.setRiskLevel(riskLevel);
        return employee;
    }
}

