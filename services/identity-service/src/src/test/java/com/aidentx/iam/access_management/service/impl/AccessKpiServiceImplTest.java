package com.aidentx.iam.access_management.service.impl;

import com.aidentx.iam.access_management.config.constants.AccessKpiConstants;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessKpiServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private EntitlementAssignmentRepository entitlementAssignmentRepository;

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private IdentityRoleAssignmentRepository identityRoleAssignmentRepository;

    @Mock
    private RoleEntitlementClosureRepository roleEntitlementClosureRepository;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private AccessKpiServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(AccessKpiServiceImpl.class, "access-management")).thenReturn(logger);
        service = new AccessKpiServiceImpl(
                entitlementAssignmentRepository,
                accessRoleRepository,
                identityRoleAssignmentRepository,
                roleEntitlementClosureRepository,
                loggerFactory);
    }

    @Test
    void getAccessKpisCalculatesLiveMetricsFromRepositoryAggregates() {
        when(entitlementAssignmentRepository.countByTenantId(TENANT_ID)).thenReturn(200L);
        when(entitlementAssignmentRepository.countByTenantIdAndSourceIgnoreCase(
                TENANT_ID, AccessKpiConstants.SOURCE_ROLE_DERIVED)).thenReturn(146L);
        when(entitlementAssignmentRepository.countStandaloneAssignmentsByTenantIdAndSourceIn(
                TENANT_ID,
                List.of(AccessKpiConstants.SOURCE_DIRECT, AccessKpiConstants.SOURCE_MANUAL, AccessKpiConstants.SOURCE_IMPORT)))
                .thenReturn(42L);
        when(accessRoleRepository.countByTenantId(TENANT_ID)).thenReturn(50L);
        when(accessRoleRepository.countByTenantIdAndStatusIgnoreCase(TENANT_ID, AccessKpiConstants.ROLE_STATUS_ACTIVE))
                .thenReturn(20L);
        when(roleEntitlementClosureRepository.sumDistinctEntitlementMembershipsForRolesByTenantIdAndStatus(
                TENANT_ID, AccessKpiConstants.ROLE_STATUS_ACTIVE)).thenReturn(168L);
        when(entitlementAssignmentRepository.countEntitlementsWithoutAssignments(TENANT_ID)).thenReturn(11L);
        when(accessRoleRepository.countRolesWithoutActiveMembers(TENANT_ID, IdentityRoleAssignment.Status.ACTIVE))
                .thenReturn(20L);
        when(identityRoleAssignmentRepository.countRolesWithAtLeastDistinctIdentityMembers(
                TENANT_ID, IdentityRoleAssignment.Status.ACTIVE.name(), AccessKpiConstants.REUSED_ROLE_MIN_MEMBER_COUNT))
                .thenReturn(32L);

        var response = service.getAccessKpis(TENANT_ID);

        assertThat(response.getRoleCoverageRatio().getValue()).isEqualByComparingTo(new BigDecimal("73.00"));
        assertThat(response.getRoleCoverageRatio().getUnit()).isEqualTo(AccessKpiConstants.UNIT_PERCENT);
        assertThat(response.getDirectAccessExposure().getValue()).isEqualByComparingTo(new BigDecimal("42"));
        assertThat(response.getDirectAccessExposure().getUnit()).isEqualTo(AccessKpiConstants.UNIT_COUNT);
        assertThat(response.getRoleComplexityIndex().getValue()).isEqualByComparingTo(new BigDecimal("8.40"));
        assertThat(response.getRoleComplexityIndex().getUnit()).isEqualTo(AccessKpiConstants.UNIT_AVERAGE);
        assertThat(response.getOrphanedAccessObjects().getValue()).isEqualByComparingTo(new BigDecimal("31"));
        assertThat(response.getAccessReuseScore().getValue()).isEqualByComparingTo(new BigDecimal("64.00"));
        assertThat(response.getAccessReuseScore().getDelta()).isNull();
        assertThat(response.getAccessReuseScore().getDeltaDirection()).isEqualTo(AccessKpiConstants.DELTA_DIRECTION_NONE);
        assertThat(response.getAccessReuseScore().getPeriodLabel()).isEqualTo(AccessKpiConstants.PERIOD_LABEL_CURRENT);

        verify(identityRoleAssignmentRepository).countRolesWithAtLeastDistinctIdentityMembers(
                TENANT_ID, IdentityRoleAssignment.Status.ACTIVE.name(), AccessKpiConstants.REUSED_ROLE_MIN_MEMBER_COUNT);
    }

    @Test
    void getAccessKpisReturnsZeroRatiosWhenDenominatorsAreZero() {
        when(entitlementAssignmentRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
        when(entitlementAssignmentRepository.countByTenantIdAndSourceIgnoreCase(
                TENANT_ID, AccessKpiConstants.SOURCE_ROLE_DERIVED)).thenReturn(0L);
        when(entitlementAssignmentRepository.countStandaloneAssignmentsByTenantIdAndSourceIn(
                TENANT_ID,
                List.of(AccessKpiConstants.SOURCE_DIRECT, AccessKpiConstants.SOURCE_MANUAL, AccessKpiConstants.SOURCE_IMPORT)))
                .thenReturn(0L);
        when(accessRoleRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
        when(accessRoleRepository.countByTenantIdAndStatusIgnoreCase(TENANT_ID, AccessKpiConstants.ROLE_STATUS_ACTIVE))
                .thenReturn(0L);
        when(roleEntitlementClosureRepository.sumDistinctEntitlementMembershipsForRolesByTenantIdAndStatus(
                TENANT_ID, AccessKpiConstants.ROLE_STATUS_ACTIVE)).thenReturn(0L);
        when(entitlementAssignmentRepository.countEntitlementsWithoutAssignments(TENANT_ID)).thenReturn(0L);
        when(accessRoleRepository.countRolesWithoutActiveMembers(TENANT_ID, IdentityRoleAssignment.Status.ACTIVE))
                .thenReturn(0L);
        when(identityRoleAssignmentRepository.countRolesWithAtLeastDistinctIdentityMembers(
                TENANT_ID, IdentityRoleAssignment.Status.ACTIVE.name(), AccessKpiConstants.REUSED_ROLE_MIN_MEMBER_COUNT))
                .thenReturn(0L);

        var response = service.getAccessKpis(TENANT_ID);

        assertThat(response.getRoleCoverageRatio().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getRoleComplexityIndex().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getAccessReuseScore().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOrphanedAccessObjects().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

