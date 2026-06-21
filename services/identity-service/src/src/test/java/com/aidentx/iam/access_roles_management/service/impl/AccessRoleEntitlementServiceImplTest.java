package com.aidentx.iam.access_roles_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.ApplicationEntitlementGroupDto;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleAssignmentResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.GroupedRoleEntitlementsDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AccessRoleEntitlementServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleEntitlementRepository entitlementLinkRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private EntitlementAssignmentRepository assignmentRepository;

    @Mock
    private ApplicationClient applicationClient;

    @Mock
    private AccessRoleRepository roleRepository;

    @Mock
    private IdentityRoleAssignmentRepository identityRoleAssignmentRepository;

    @Mock
    private RoleEntitlementClosureRepository closureRepository;

    @Mock
    private RoleEntitlementClosureService closureService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessRoleEntitlementServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleEntitlementServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleEntitlementServiceImpl(
                entitlementLinkRepository,
                entitlementRepository,
                assignmentRepository,
                applicationClient,
                roleRepository,
                identityRoleAssignmentRepository,
                closureRepository,
                closureService,
                loggerFactory);
    }

    @Test
    void addRolesToEntitlementLinksMultipleRolesAndSkipsExistingDuplicates() {
        UUID entitlementId = UUID.randomUUID();
        UUID roleOneId = UUID.randomUUID();
        UUID roleTwoId = UUID.randomUUID();

        when(entitlementRepository.findById(entitlementId)).thenReturn(Optional.of(entitlement(entitlementId)));
        when(roleRepository.findByIdIn(List.of(roleOneId, roleTwoId))).thenReturn(List.of(role(roleOneId), role(roleTwoId)));
        when(entitlementLinkRepository.existsByRoleIdAndEntitlementId(roleOneId, entitlementId)).thenReturn(false);
        when(entitlementLinkRepository.existsByRoleIdAndEntitlementId(roleTwoId, entitlementId)).thenReturn(true);

        EntitlementRoleAssignmentResponse response = service.addRolesToEntitlement(
                TENANT_ID, entitlementId, List.of(roleOneId, roleTwoId, roleOneId));

        ArgumentCaptor<AccessRoleEntitlement> linkCaptor = ArgumentCaptor.forClass(AccessRoleEntitlement.class);
        verify(entitlementLinkRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getRoleId()).isEqualTo(roleOneId);
        assertThat(linkCaptor.getValue().getEntitlementId()).isEqualTo(entitlementId);
        verify(closureService).refreshAfterEntitlementsChanged(TENANT_ID, roleOneId);
        verify(closureService, never()).refreshAfterEntitlementsChanged(TENANT_ID, roleTwoId);
        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.uniqueRequestedCount()).isEqualTo(2);
        assertThat(response.addedCount()).isEqualTo(1);
        assertThat(response.skippedAlreadyAssignedCount()).isEqualTo(1);
        assertThat(response.roleIds()).containsExactly(roleOneId, roleTwoId);
    }

    @Test
    void removeRolesFromEntitlementDeletesExistingLinksAndSkipsMissingLinks() {
        UUID entitlementId = UUID.randomUUID();
        UUID roleOneId = UUID.randomUUID();
        UUID roleTwoId = UUID.randomUUID();

        when(entitlementRepository.findById(entitlementId)).thenReturn(Optional.of(entitlement(entitlementId)));
        when(roleRepository.findByIdIn(List.of(roleOneId, roleTwoId))).thenReturn(List.of(role(roleOneId), role(roleTwoId)));
        when(entitlementLinkRepository.findByEntitlementId(entitlementId))
                .thenReturn(List.of(link(roleOneId, entitlementId)));

        EntitlementRoleAssignmentResponse response = service.removeRolesFromEntitlement(
                TENANT_ID, entitlementId, List.of(roleOneId, roleTwoId));

        verify(entitlementLinkRepository).deleteByRoleIdInAndEntitlementId(List.of(roleOneId), entitlementId);
        verify(closureService).refreshAfterEntitlementsChanged(TENANT_ID, roleOneId);
        verify(closureService, never()).refreshAfterEntitlementsChanged(TENANT_ID, roleTwoId);
        assertThat(response.removedCount()).isEqualTo(1);
        assertThat(response.skippedNotAssignedCount()).isEqualTo(1);
        assertThat(response.skippedRoleIds()).containsExactly(roleTwoId);
    }

    @Test
    void syncRolesForEntitlementAddsRemovesAndKeepsUnchangedRoles() {
        UUID entitlementId = UUID.randomUUID();
        UUID existingRoleId = UUID.randomUUID();
        UUID removedRoleId = UUID.randomUUID();
        UUID addedRoleId = UUID.randomUUID();

        when(entitlementRepository.findById(entitlementId)).thenReturn(Optional.of(entitlement(entitlementId)));
        when(roleRepository.findByIdIn(List.of(existingRoleId, addedRoleId))).thenReturn(List.of(role(existingRoleId), role(addedRoleId)));
        when(entitlementLinkRepository.findByEntitlementId(entitlementId))
                .thenReturn(List.of(link(existingRoleId, entitlementId), link(removedRoleId, entitlementId)));

        EntitlementRoleAssignmentResponse response = service.syncRolesForEntitlement(
                TENANT_ID, entitlementId, List.of(existingRoleId, addedRoleId));

        verify(entitlementLinkRepository).deleteByRoleIdInAndEntitlementId(List.of(removedRoleId), entitlementId);
        verify(closureService).refreshAfterEntitlementsChanged(TENANT_ID, addedRoleId);
        verify(closureService).refreshAfterEntitlementsChanged(TENANT_ID, removedRoleId);
        assertThat(response.addedRoleIds()).containsExactly(addedRoleId);
        assertThat(response.removedRoleIds()).containsExactly(removedRoleId);
        assertThat(response.unchangedRoleIds()).containsExactly(existingRoleId);
        assertThat(response.addedCount()).isEqualTo(1);
        assertThat(response.removedCount()).isEqualTo(1);
        assertThat(response.unchangedCount()).isEqualTo(1);
    }

    @Test
    void getRolesForEntitlementReturnsPagedTenantRoleRows() {
        UUID entitlementId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AccessRoleEntitlement link = link(roleId, entitlementId);
        link.setId(linkId);
        link.setCreatedAt(Instant.ofEpochMilli(1234));

        when(entitlementRepository.findById(entitlementId)).thenReturn(Optional.of(entitlement(entitlementId)));
        when(entitlementLinkRepository.findByEntitlementId(entitlementId)).thenReturn(List.of(link));
        when(roleRepository.findByIdIn(List.of(roleId))).thenReturn(List.of(role(roleId)));

        Page<EntitlementRoleResponse> response = service.getRolesForEntitlement(
                TENANT_ID, entitlementId, PageRequest.of(0, 20));

        assertThat(response.getTotalElements()).isEqualTo(1);
        EntitlementRoleResponse row = response.getContent().get(0);
        assertThat(row.linkId()).isEqualTo(linkId);
        assertThat(row.entitlementId()).isEqualTo(entitlementId);
        assertThat(row.roleId()).isEqualTo(roleId);
        assertThat(row.roleName()).isEqualTo("role-" + roleId);
        assertThat(row.assignedAt()).isEqualTo(1234);
    }

    @Test
    void getEntitlementDistributionReturnsUniqueDirectAndInheritedCounts() {
        UUID roleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID directReadEntitlementId = UUID.randomUUID();
        UUID sharedAdminEntitlementId = UUID.randomUUID();
        UUID inheritedWriteEntitlementId = UUID.randomUUID();
        UUID appOneId = UUID.randomUUID();
        UUID appTwoId = UUID.randomUUID();

        AccessRole role = role(roleId);
        role.setDisplayName(null);
        AccessRole childRole = role(childRoleId);
        childRole.setName("Finance Analyst");
        childRole.setDisplayName(null);

        Entitlement directRead = entitlement(directReadEntitlementId, appOneId, "Write");
        Entitlement sharedAdmin = entitlement(sharedAdminEntitlementId, appOneId, "ADMIN");
        Entitlement inheritedWrite = entitlement(inheritedWriteEntitlementId, appTwoId, "WRITE");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                TENANT_ID,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE)).thenReturn(7L);
        when(closureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of(
                closure(roleId, directReadEntitlementId, roleId, true, 0),
                closure(roleId, sharedAdminEntitlementId, roleId, true, 0),
                closure(roleId, sharedAdminEntitlementId, childRoleId, false, 1),
                closure(roleId, inheritedWriteEntitlementId, childRoleId, false, 1)
        ));
        when(entitlementRepository.findAllById(any())).thenReturn(List.of(
                directRead,
                sharedAdmin,
                inheritedWrite
        ));
        when(applicationClient.getApplicationNames(eq(TENANT_ID), anyList())).thenReturn(Map.of(
                appOneId, "Active Directory",
                appTwoId, "SAP"
        ));
        when(roleRepository.findByIdIn(anySet())).thenReturn(List.of(childRole));

        AccessRoleEntitlementDistributionResponse response =
                service.getEntitlementDistribution(TENANT_ID, roleId);

        assertThat(response.roleId()).isEqualTo(roleId);
        assertThat(response.roleDisplayName()).isEqualTo(role.getName());
        assertThat(response.summary().directEntitlementCount()).isEqualTo(2);
        assertThat(response.summary().childRoleEntitlementCount()).isEqualTo(2);
        assertThat(response.summary().effectiveEntitlementCount()).isEqualTo(3);
        assertThat(response.summary().directApplicationCount()).isEqualTo(1);
        assertThat(response.summary().childRoleApplicationCount()).isEqualTo(2);
        assertThat(response.summary().effectiveApplicationCount()).isEqualTo(2);
        assertThat(response.summary().childRoleCount()).isEqualTo(1);
        assertThat(response.summary().assignedUserCount()).isEqualTo(7);

        assertThat(response.direct().privilegeLevelDistribution())
                .extracting(AccessRoleEntitlementDistributionResponse.PrivilegeLevelDistribution::privilegeLevel)
                .containsExactlyInAnyOrder("Write", "Admin");
        assertThat(response.inheritedFromChildRoles().privilegeLevelDistribution())
                .extracting(AccessRoleEntitlementDistributionResponse.PrivilegeLevelDistribution::privilegeLevel)
                .containsExactlyInAnyOrder("Admin", "Write");
        assertThat(response.effectiveTotal().privilegeLevelDistribution())
                .extracting(AccessRoleEntitlementDistributionResponse.PrivilegeLevelDistribution::privilegeLevel)
                .containsExactlyInAnyOrder("Admin", "Write");
        assertThat(response.effectiveTotal().privilegeLevelDistribution())
                .filteredOn(distribution -> distribution.privilegeLevel().equals("Write"))
                .extracting(AccessRoleEntitlementDistributionResponse.PrivilegeLevelDistribution::count)
                .containsExactly(2L);

        assertThat(response.childRoleBreakdown()).hasSize(1);
        assertThat(response.childRoleBreakdown().get(0).childRoleId()).isEqualTo(childRoleId);
        assertThat(response.childRoleBreakdown().get(0).childRoleDisplayName()).isEqualTo("Finance Analyst");
        assertThat(response.childRoleBreakdown().get(0).entitlementCount()).isEqualTo(2);
        assertThat(response.childRoleBreakdown().get(0).applicationCount()).isEqualTo(2);
    }

    @Test
    void getGroupedEntitlementDetailsSearchesByApplicationNameBeforePagination() {
        UUID roleId = UUID.randomUUID();
        UUID financeAppId = UUID.randomUUID();
        UUID hrAppId = UUID.randomUUID();
        UUID financeReadId = UUID.randomUUID();
        UUID financeWriteId = UUID.randomUUID();
        UUID hrReadId = UUID.randomUUID();
        Entitlement financeRead = entitlement(financeReadId, financeAppId, "Read");
        Entitlement financeWrite = entitlement(financeWriteId, financeAppId, "Write");
        Entitlement hrRead = entitlement(hrReadId, hrAppId, "Read");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId)));
        when(entitlementLinkRepository.findByRoleId(roleId)).thenReturn(List.of(
                link(roleId, hrReadId),
                link(roleId, financeReadId),
                link(roleId, financeWriteId)));
        when(entitlementRepository.findDistinctResourceIdsByEntitlementIds(List.of(hrReadId, financeReadId, financeWriteId)))
                .thenReturn(List.of(hrAppId, financeAppId));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(hrAppId, financeAppId)))
                .thenReturn(Map.of(hrAppId, "Human Resources", financeAppId, "Finance Application"));
        when(entitlementRepository.findAllById(List.of(hrReadId, financeReadId, financeWriteId)))
                .thenReturn(List.of(hrRead, financeRead, financeWrite));
        when(assignmentRepository.countByEntitlementIds(anyList())).thenReturn(List.of());

        GroupedRoleEntitlementsDto response = service.getGroupedEntitlementDetails(
                roleId,
                TENANT_ID,
                PageRequest.of(0, 1),
                PageRequest.of(0, 10),
                "finance");

        assertThat(response.totalApplications()).isEqualTo(1);
        assertThat(response.totalEntitlements()).isEqualTo(2);
        assertThat(response.applications().getContent()).singleElement().satisfies(group -> {
            assertThat(group.resourceId()).isEqualTo(financeAppId);
            assertThat(group.resourceName()).isEqualTo("Finance Application");
            assertThat(group.entitlementCount()).isEqualTo(2);
            assertThat(group.entitlements().getTotalElements()).isEqualTo(2);
        });
    }

    @Test
    void getGroupedEntitlementDetailsSearchesByEntitlementFieldsBeforePagination() {
        UUID roleId = UUID.randomUUID();
        UUID financeAppId = UUID.randomUUID();
        UUID payrollAppId = UUID.randomUUID();
        UUID financeReadId = UUID.randomUUID();
        UUID payrollAdminId = UUID.randomUUID();
        Entitlement financeRead = entitlement(financeReadId, financeAppId, "Read");
        financeRead.setDisplayName("FINANCE_READ");
        Entitlement payrollAdmin = entitlement(payrollAdminId, payrollAppId, "Admin");
        payrollAdmin.setDisplayName("PAYROLL_ADMIN");
        payrollAdmin.setDescription("Payroll approval entitlement");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId)));
        when(entitlementLinkRepository.findByRoleId(roleId)).thenReturn(List.of(
                link(roleId, financeReadId),
                link(roleId, payrollAdminId)));
        when(entitlementRepository.findDistinctResourceIdsByEntitlementIds(List.of(financeReadId, payrollAdminId)))
                .thenReturn(List.of(financeAppId, payrollAppId));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(financeAppId, payrollAppId)))
                .thenReturn(Map.of(financeAppId, "Finance Application", payrollAppId, "Workday"));
        when(entitlementRepository.findAllById(List.of(financeReadId, payrollAdminId)))
                .thenReturn(List.of(financeRead, payrollAdmin));
        when(assignmentRepository.countByEntitlementIds(anyList())).thenReturn(List.of());

        GroupedRoleEntitlementsDto response = service.getGroupedEntitlementDetails(
                roleId,
                TENANT_ID,
                PageRequest.of(0, 1),
                PageRequest.of(0, 10),
                "payroll");

        assertThat(response.totalApplications()).isEqualTo(1);
        assertThat(response.totalEntitlements()).isEqualTo(1);
        ApplicationEntitlementGroupDto group = response.applications().getContent().get(0);
        assertThat(group.resourceId()).isEqualTo(payrollAppId);
        assertThat(group.resourceName()).isEqualTo("Workday");
        assertThat(group.entitlementCount()).isEqualTo(1);
        assertThat(group.entitlements().getContent()).singleElement()
                .satisfies(entitlement -> assertThat(entitlement.displayName()).isEqualTo("PAYROLL_ADMIN"));
    }

    @Test
    void compareRoleEntitlementsReturnsOverlappingDirectAndInheritedPresence() {
        UUID roleAId = UUID.randomUUID();
        UUID roleBId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Object[] overlapRow = new Object[]{entitlementId, 1L, 1L, 2L, 0L, 1L, 1L};
        Entitlement entitlement = entitlement(entitlementId, appId, "Admin");
        entitlement.setDisplayName("FINANCE_ADMIN");
        entitlement.setRiskLevel("high");

        when(roleRepository.findById(roleAId)).thenReturn(Optional.of(role(roleAId)));
        when(roleRepository.findById(roleBId)).thenReturn(Optional.of(role(roleBId)));
        when(closureRepository.findOverlappingEntitlementSummaries(
                TENANT_ID,
                roleAId,
                roleBId,
                "ALL",
                pageable)).thenReturn(new PageImpl<>(List.<Object[]>of(overlapRow), pageable, 1));
        when(entitlementRepository.findAllById(any())).thenReturn(List.of(entitlement));
        when(applicationClient.getApplicationNames(TENANT_ID, List.of(appId)))
                .thenReturn(Map.of(appId, "Finance App"));

        AccessRoleEntitlementComparisonResponse response = service.compareRoleEntitlements(
                TENANT_ID,
                roleAId,
                roleBId,
                null,
                pageable);

        assertThat(response.sourceFilter()).isEqualTo("ALL");
        assertThat(response.totalOverlappingEntitlements()).isEqualTo(1);
        assertThat(response.roleA().roleId()).isEqualTo(roleAId);
        assertThat(response.roleB().roleId()).isEqualTo(roleBId);
        assertThat(response.entitlements().getTotalElements()).isEqualTo(1);
        assertThat(response.entitlements().getContent()).singleElement().satisfies(row -> {
            assertThat(row.entitlementId()).isEqualTo(entitlementId);
            assertThat(row.displayName()).isEqualTo("FINANCE_ADMIN");
            assertThat(row.applicationName()).isEqualTo("Finance App");
            assertThat(row.riskLevel()).isEqualTo("high");
            assertThat(row.roleA().direct()).isTrue();
            assertThat(row.roleA().inherited()).isTrue();
            assertThat(row.roleA().accessTypes()).containsExactly("DIRECT", "INHERITED");
            assertThat(row.roleA().sourceRoleCount()).isEqualTo(2);
            assertThat(row.roleB().direct()).isFalse();
            assertThat(row.roleB().inherited()).isTrue();
            assertThat(row.roleB().accessTypes()).containsExactly("INHERITED");
            assertThat(row.roleB().sourceRoleCount()).isEqualTo(1);
        });
    }

    @Test
    void compareRoleEntitlementsNormalizesSourceFilterBeforeQuerying() {
        UUID roleAId = UUID.randomUUID();
        UUID roleBId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(1, 5);

        when(roleRepository.findById(roleAId)).thenReturn(Optional.of(role(roleAId)));
        when(roleRepository.findById(roleBId)).thenReturn(Optional.of(role(roleBId)));
        when(closureRepository.findOverlappingEntitlementSummaries(
                TENANT_ID,
                roleAId,
                roleBId,
                "DIRECT",
                pageable)).thenReturn(Page.empty(pageable));

        AccessRoleEntitlementComparisonResponse response = service.compareRoleEntitlements(
                TENANT_ID,
                roleAId,
                roleBId,
                " direct ",
                pageable);

        assertThat(response.sourceFilter()).isEqualTo("DIRECT");
        assertThat(response.entitlements()).isEmpty();
    }

    private Entitlement entitlement(UUID entitlementId) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setDisplayName("entitlement-" + entitlementId);
        entitlement.setResourceId(UUID.randomUUID());
        entitlement.setSourceUid("source-" + entitlementId);
        return entitlement;
    }

    private Entitlement entitlement(UUID entitlementId, UUID resourceId, String privilegeLevel) {
        Entitlement entitlement = entitlement(entitlementId);
        entitlement.setResourceId(resourceId);
        entitlement.setPrivilegeLevel(privilegeLevel);
        return entitlement;
    }

    private AccessRole role(UUID roleId) {
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("role-" + roleId);
        role.setDisplayName("Role " + roleId);
        role.setType("business");
        role.setStatus("active");
        role.setRiskLevel("medium");
        role.setBusinessUnit("Finance");
        return role;
    }

    private AccessRoleEntitlement link(UUID roleId, UUID entitlementId) {
        AccessRoleEntitlement link = new AccessRoleEntitlement();
        link.setRoleId(roleId);
        link.setEntitlementId(entitlementId);
        return link;
    }

    private RoleEntitlementClosure closure(UUID roleId,
                                           UUID entitlementId,
                                           UUID sourceRoleId,
                                           boolean direct,
                                           int depth) {
        RoleEntitlementClosure closure = new RoleEntitlementClosure();
        closure.setTenantId(TENANT_ID);
        closure.setRoleId(roleId);
        closure.setEntitlementId(entitlementId);
        closure.setSourceRoleId(sourceRoleId);
        closure.setDirect(direct);
        closure.setDepth(depth);
        closure.setPathRoleIds(List.of(roleId, sourceRoleId));
        return closure;
    }
}
