package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleEntitlementClosureServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private RoleEntitlementClosureRepository closureRepository;

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private AccessRoleEntitlementRepository entitlementRepository;

    @Mock
    private AccessRoleCompositionRepository compositionRepository;

    private RoleEntitlementClosureServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleEntitlementClosureServiceImpl(
                closureRepository,
                accessRoleRepository,
                entitlementRepository,
                compositionRepository);
    }

    @Test
    void rebuildRoleClosureStoresDirectEntitlements() {
        stubSaveAll();
        UUID roleId = UUID.randomUUID();
        UUID entitlementOne = UUID.randomUUID();
        UUID entitlementTwo = UUID.randomUUID();
        stubTenantRoles(roleId);
        when(entitlementRepository.findByRoleId(roleId)).thenReturn(List.of(
                entitlement(roleId, entitlementOne),
                entitlement(roleId, entitlementTwo)));
        when(compositionRepository.findByParentRoleId(roleId)).thenReturn(List.of());

        List<RoleEntitlementClosure> rows = service.rebuildRoleClosure(TENANT_ID, roleId);

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(row.getRoleId()).isEqualTo(roleId);
            assertThat(row.getSourceRoleId()).isEqualTo(roleId);
            assertThat(row.isDirect()).isTrue();
            assertThat(row.getDepth()).isZero();
            assertThat(row.getPathRoleIds()).containsExactly(roleId);
        });
        assertThat(rows).extracting(RoleEntitlementClosure::getEntitlementId)
                .containsExactlyInAnyOrder(entitlementOne, entitlementTwo);
        verify(closureRepository).deleteByTenantIdAndRoleId(TENANT_ID, roleId);
    }

    @Test
    void rebuildRoleClosureIncludesChildAndDeepHierarchyEntitlements() {
        stubSaveAll();
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID grandChildId = UUID.randomUUID();
        UUID parentEntitlement = UUID.randomUUID();
        UUID childEntitlement = UUID.randomUUID();
        UUID grandChildEntitlement = UUID.randomUUID();
        stubTenantRoles(parentId, childId, grandChildId);

        when(entitlementRepository.findByRoleId(parentId)).thenReturn(List.of(entitlement(parentId, parentEntitlement)));
        when(entitlementRepository.findByRoleId(childId)).thenReturn(List.of(entitlement(childId, childEntitlement)));
        when(entitlementRepository.findByRoleId(grandChildId)).thenReturn(List.of(entitlement(grandChildId, grandChildEntitlement)));
        when(compositionRepository.findByParentRoleId(parentId)).thenReturn(List.of(composition(parentId, childId)));
        when(compositionRepository.findByParentRoleId(childId)).thenReturn(List.of(composition(childId, grandChildId)));
        when(compositionRepository.findByParentRoleId(grandChildId)).thenReturn(List.of());

        List<RoleEntitlementClosure> rows = service.rebuildRoleClosure(TENANT_ID, parentId);

        assertThat(rows).hasSize(3);
        assertThat(rowFor(rows, parentEntitlement).isDirect()).isTrue();
        assertThat(rowFor(rows, childEntitlement).isDirect()).isFalse();
        assertThat(rowFor(rows, childEntitlement).getDepth()).isEqualTo(1);
        assertThat(rowFor(rows, childEntitlement).getPathRoleIds()).containsExactly(parentId, childId);
        assertThat(rowFor(rows, grandChildEntitlement).isDirect()).isFalse();
        assertThat(rowFor(rows, grandChildEntitlement).getDepth()).isEqualTo(2);
        assertThat(rowFor(rows, grandChildEntitlement).getPathRoleIds()).containsExactly(parentId, childId, grandChildId);
    }

    @Test
    void rebuildRoleClosureProtectsAgainstExistingCycles() {
        stubSaveAll();
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID parentEntitlement = UUID.randomUUID();
        UUID childEntitlement = UUID.randomUUID();
        stubTenantRoles(parentId, childId);

        when(entitlementRepository.findByRoleId(parentId)).thenReturn(List.of(entitlement(parentId, parentEntitlement)));
        when(entitlementRepository.findByRoleId(childId)).thenReturn(List.of(entitlement(childId, childEntitlement)));
        when(compositionRepository.findByParentRoleId(parentId)).thenReturn(List.of(composition(parentId, childId)));
        when(compositionRepository.findByParentRoleId(childId)).thenReturn(List.of(composition(childId, parentId)));

        List<RoleEntitlementClosure> rows = service.rebuildRoleClosure(TENANT_ID, parentId);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(RoleEntitlementClosure::getEntitlementId)
                .containsExactlyInAnyOrder(parentEntitlement, childEntitlement);
    }

    @Test
    void refreshAfterEntitlementsChangedRebuildsRoleAndAncestors() {
        UUID grandParentId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        stubTenantRoles(grandParentId, parentId, childId);

        when(compositionRepository.findByChildRoleId(childId)).thenReturn(List.of(composition(parentId, childId)));
        when(compositionRepository.findByChildRoleId(parentId)).thenReturn(List.of(composition(grandParentId, parentId)));
        when(compositionRepository.findByChildRoleId(grandParentId)).thenReturn(List.of());
        when(compositionRepository.findByParentRoleId(childId)).thenReturn(List.of());
        when(compositionRepository.findByParentRoleId(parentId)).thenReturn(List.of(composition(parentId, childId)));
        when(compositionRepository.findByParentRoleId(grandParentId)).thenReturn(List.of(composition(grandParentId, parentId)));
        when(entitlementRepository.findByRoleId(childId)).thenReturn(List.of());
        when(entitlementRepository.findByRoleId(parentId)).thenReturn(List.of());
        when(entitlementRepository.findByRoleId(grandParentId)).thenReturn(List.of());

        Set<UUID> rebuiltRoleIds = service.refreshAfterEntitlementsChanged(TENANT_ID, childId);

        assertThat(rebuiltRoleIds).containsExactly(childId, parentId, grandParentId);
        ArgumentCaptor<UUID> roleCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(closureRepository, org.mockito.Mockito.times(3)).deleteByTenantIdAndRoleId(org.mockito.Mockito.eq(TENANT_ID), roleCaptor.capture());
        assertThat(roleCaptor.getAllValues()).containsExactly(childId, parentId, grandParentId);
    }

    private RoleEntitlementClosure rowFor(List<RoleEntitlementClosure> rows, UUID entitlementId) {
        return rows.stream()
                .filter(row -> entitlementId.equals(row.getEntitlementId()))
                .findFirst()
                .orElseThrow();
    }

    private void stubTenantRoles(UUID... roleIds) {
        for (UUID roleId : roleIds) {
            when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role(roleId)));
        }
    }

    private void stubSaveAll() {
        when(closureRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AccessRole role(UUID roleId) {
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("role-" + roleId);
        role.setType("business");
        role.setStatus("active");
        return role;
    }

    private AccessRoleEntitlement entitlement(UUID roleId, UUID entitlementId) {
        AccessRoleEntitlement entitlement = new AccessRoleEntitlement();
        entitlement.setRoleId(roleId);
        entitlement.setEntitlementId(entitlementId);
        return entitlement;
    }

    private AccessRoleComposition composition(UUID parentRoleId, UUID childRoleId) {
        AccessRoleComposition composition = new AccessRoleComposition();
        composition.setParentRoleId(parentRoleId);
        composition.setChildRoleId(childRoleId);
        return composition;
    }
}

