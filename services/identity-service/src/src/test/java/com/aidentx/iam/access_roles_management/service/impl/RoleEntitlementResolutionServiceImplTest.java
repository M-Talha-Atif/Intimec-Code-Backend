package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleEntitlementResolutionServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private RoleEntitlementClosureRepository closureRepository;

    @Mock
    private RoleEntitlementClosureService closureService;

    @Mock
    private AccessRoleRepository accessRoleRepository;

    private RoleEntitlementResolutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleEntitlementResolutionServiceImpl(closureRepository, closureService, accessRoleRepository);
    }

    @Test
    void resolveEffectiveEntitlementIdsUsesDistinctClosureQuery() {
        UUID roleId = UUID.randomUUID();
        UUID entitlementOne = UUID.randomUUID();
        UUID entitlementTwo = UUID.randomUUID();
        stubTenantRole(roleId);
        when(closureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of(new com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure()));
        when(closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleId(TENANT_ID, roleId))
                .thenReturn(List.of(entitlementOne, entitlementTwo));

        List<UUID> entitlements = service.resolveEffectiveEntitlementIds(TENANT_ID, roleId);

        assertThat(entitlements).containsExactly(entitlementOne, entitlementTwo);
    }

    @Test
    void resolveInheritedEntitlementIdsDeduplicatesDirectEntitlements() {
        UUID roleId = UUID.randomUUID();
        UUID directEntitlement = UUID.randomUUID();
        UUID inheritedEntitlement = UUID.randomUUID();
        stubTenantRole(roleId);
        when(closureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of(new com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure()));
        when(closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleIdAndDirect(TENANT_ID, roleId, true))
                .thenReturn(List.of(directEntitlement));
        when(closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleIdAndDirect(TENANT_ID, roleId, false))
                .thenReturn(List.of(directEntitlement, inheritedEntitlement));

        List<UUID> inherited = service.resolveInheritedEntitlementIds(TENANT_ID, roleId);

        assertThat(inherited).containsExactly(inheritedEntitlement);
    }

    @Test
    void resolveEffectiveEntitlementIdsRebuildsWhenClosureIsMissing() {
        UUID roleId = UUID.randomUUID();
        stubTenantRole(roleId);
        when(closureRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());
        when(closureRepository.findDistinctEntitlementIdsByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(List.of());

        List<UUID> entitlements = service.resolveEffectiveEntitlementIds(TENANT_ID, roleId);

        assertThat(entitlements).isEmpty();
        verify(closureService).rebuildRoleClosure(TENANT_ID, roleId);
    }

    private void stubTenantRole(UUID roleId) {
        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("role-" + roleId);
        role.setType("business");
        role.setStatus("active");
        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
    }
}

