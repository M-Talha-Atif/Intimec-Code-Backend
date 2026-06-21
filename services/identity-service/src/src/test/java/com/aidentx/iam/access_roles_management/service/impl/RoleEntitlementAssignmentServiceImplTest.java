package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignment;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleEntitlementAssignmentServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private RoleEntitlementResolutionService resolutionService;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private EntitlementAssignmentRepository entitlementAssignmentRepository;

    @Mock
    private ShadowRepository shadowRepository;

    @Mock
    private IdentityRoleAssignmentRepository identityRoleAssignmentRepository;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private RoleEntitlementAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(RoleEntitlementAssignmentServiceImpl.class), anyString())).thenReturn(logger);
        service = new RoleEntitlementAssignmentServiceImpl(
                resolutionService,
                entitlementRepository,
                entitlementAssignmentRepository,
                shadowRepository,
                identityRoleAssignmentRepository,
                loggerFactory);
        lenient().when(entitlementAssignmentRepository.findPendingByTenantIdAndIdentityIdInAndEntitlementIdInAndSource(
                eq(TENANT_ID), anyCollection(), anyCollection(), anyString())).thenReturn(List.of());
    }

    @Test
    void applyRoleEntitlementsAssignsDirectAndInheritedEntitlementsToMatchingAccountShadows() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID entitlementOneId = UUID.randomUUID();
        UUID entitlementTwoId = UUID.randomUUID();
        UUID entitlementThreeId = UUID.randomUUID();
        UUID resourceOneId = UUID.randomUUID();
        UUID resourceTwoId = UUID.randomUUID();
        UUID shadowOneId = UUID.randomUUID();
        UUID shadowTwoId = UUID.randomUUID();

        Entitlement entitlementOne = entitlement(entitlementOneId, resourceOneId);
        Entitlement entitlementTwo = entitlement(entitlementTwoId, resourceOneId);
        Entitlement entitlementThree = entitlement(entitlementThreeId, resourceTwoId);
        Shadow shadowOne = shadow(shadowOneId, identityId, resourceOneId.toString(), "account", Shadow.SynchronizationSituation.LINKED);
        Shadow shadowTwo = shadow(shadowTwoId, identityId, resourceTwoId.toString(), "account", Shadow.SynchronizationSituation.LINKED);
        Shadow ignoredShadow = shadow(UUID.randomUUID(), identityId, resourceOneId.toString(), "group", Shadow.SynchronizationSituation.LINKED);
        EntitlementAssignment existing = entitlementAssignment(shadowOneId, entitlementOneId, "ROLE_DERIVED");
        existing.setAccessRoleId(roleId);

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId))
                .thenReturn(List.of(entitlementOneId, entitlementTwoId, entitlementThreeId));
        when(entitlementRepository.findAllById(anyCollection()))
                .thenReturn(List.of(entitlementOne, entitlementTwo, entitlementThree));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadowOne, shadowTwo, ignoredShadow));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of(existing));

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, ruleId, IdentityRoleAssignment.SourceType.RULE);

        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        List<EntitlementAssignment> savedAssignments = assignmentsCaptor.getValue();
        assertThat(savedAssignments).hasSize(2);
        assertThat(savedAssignments)
                .extracting(EntitlementAssignment::getEntitlementId)
                .containsExactlyInAnyOrder(entitlementTwoId, entitlementThreeId);
        assertThat(savedAssignments)
                .extracting(EntitlementAssignment::getShadowId)
                .containsExactlyInAnyOrder(shadowOneId, shadowTwoId);
        assertThat(savedAssignments).allSatisfy(assignment -> {
            assertThat(assignment.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(assignment.getRuleId()).isEqualTo(ruleId);
            assertThat(assignment.getAccessRoleId()).isEqualTo(roleId);
            assertThat(assignment.getSource()).isEqualTo("ROLE_DERIVED");
        });
    }

    @Test
    void revokeRoleEntitlementsDeletesOnlyAssignmentsNotRetainedByOtherActiveRoles() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID otherRoleId = UUID.randomUUID();
        UUID entitlementOneId = UUID.randomUUID();
        UUID entitlementTwoId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId))
                .thenReturn(List.of(entitlementOneId, entitlementTwoId));
        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, otherRoleId))
                .thenReturn(List.of(entitlementTwoId));
        when(entitlementRepository.findAllById(anyCollection()))
                .thenReturn(List.of(entitlement(entitlementOneId, resourceId), entitlement(entitlementTwoId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of(
                        entitlementAssignment(shadowId, entitlementOneId, "ROLE_DERIVED"),
                        entitlementAssignment(shadowId, entitlementTwoId, "ROLE_DERIVED")));
        when(identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                TENANT_ID,
                identityId,
                IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(activeAssignment(identityId, otherRoleId)));

        service.revokeRoleEntitlements(TENANT_ID, identityId, roleId, null);

        ArgumentCaptor<List<EntitlementAssignment>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).deleteAll(deleteCaptor.capture());
        List<EntitlementAssignment> deletedAssignments = deleteCaptor.getValue();
        assertThat(deletedAssignments).hasSize(1);
        assertThat(deletedAssignments.get(0).getEntitlementId()).isEqualTo(entitlementOneId);
    }

    @Test
    void revokeRoleEntitlementsDeletesPendingAssignmentsWhenNoOtherRoleRetainsEntitlement() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        EntitlementAssignment pendingAssignment = pendingEntitlementAssignment(identityId, entitlementId, resourceId, "ROLE_DERIVED");

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID)).thenReturn(List.of());
        when(entitlementAssignmentRepository.findPendingByTenantIdAndIdentityIdInAndEntitlementIdInAndSource(
                eq(TENANT_ID), anyCollection(), anyCollection(), anyString())).thenReturn(List.of(pendingAssignment));
        when(identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                TENANT_ID,
                identityId,
                IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of());

        service.revokeRoleEntitlements(TENANT_ID, identityId, roleId, null);

        ArgumentCaptor<List<EntitlementAssignment>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).deleteAll(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactly(pendingAssignment);
    }

    @Test
    void revokeRoleEntitlementsKeepsAssignmentsRetainedByManualAssignmentForSameRole() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementOneId = UUID.randomUUID();
        UUID entitlementTwoId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId))
                .thenReturn(List.of(entitlementOneId, entitlementTwoId));
        when(entitlementRepository.findAllById(anyCollection()))
                .thenReturn(List.of(entitlement(entitlementOneId, resourceId), entitlement(entitlementTwoId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of(
                        entitlementAssignment(shadowId, entitlementOneId, "ROLE_DERIVED"),
                        entitlementAssignment(shadowId, entitlementTwoId, "ROLE_DERIVED")));
        when(identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                TENANT_ID,
                identityId,
                IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of(activeManualAssignment(identityId, roleId)));

        service.revokeRoleEntitlements(TENANT_ID, identityId, roleId, null);

        verify(entitlementAssignmentRepository, never()).deleteAll(anyCollection());
    }

    @Test
    void revokeRoleEntitlementsDoesNotDeleteDirectAssignments() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of(entitlementAssignment(shadowId, entitlementId, "DIRECT")));
        when(identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                TENANT_ID,
                identityId,
                IdentityRoleAssignment.Status.ACTIVE)).thenReturn(List.of());

        service.revokeRoleEntitlements(TENANT_ID, identityId, roleId, null);

        verify(entitlementAssignmentRepository, never()).deleteAll(anyCollection());
    }

    @Test
    void applyRoleEntitlementsStampsRoleDerivedSourceForImportRoleAssignments() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of());

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, null, IdentityRoleAssignment.SourceType.IMPORT);

        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        assertThat(assignmentsCaptor.getValue()).singleElement().satisfies(assignment -> {
            assertThat(assignment.getSource()).isEqualTo("ROLE_DERIVED");
            assertThat(assignment.getAccessRoleId()).isEqualTo(roleId);
            assertThat(assignment.getRuleId()).isNull();
        });
    }

    @Test
    void applyRoleEntitlementsStampsRoleDerivedSourceForManualRoleAssignments() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of());

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, null, IdentityRoleAssignment.SourceType.MANUAL);

        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        assertThat(assignmentsCaptor.getValue()).singleElement().satisfies(assignment -> {
            assertThat(assignment.getSource()).isEqualTo("ROLE_DERIVED");
            assertThat(assignment.getAccessRoleId()).isEqualTo(roleId);
            assertThat(assignment.getRuleId()).isNull();
        });
    }

    @Test
    void applyRoleEntitlementsCreatesPendingAssignmentWhenIdentityHasNoEligibleAccountShadows() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(UUID.randomUUID(), identityId, resourceId.toString(), "group", Shadow.SynchronizationSituation.LINKED)));

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, null, IdentityRoleAssignment.SourceType.RULE);

        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        assertThat(assignmentsCaptor.getValue()).singleElement().satisfies(assignment -> {
            assertThat(assignment.getShadowId()).isNull();
            assertThat(assignment.getIdentityId()).isEqualTo(identityId);
            assertThat(assignment.getEntitlementId()).isEqualTo(entitlementId);
            assertThat(assignment.getResourceId()).isEqualTo(resourceId);
            assertThat(assignment.getStatus()).isEqualTo(EntitlementAssignment.AssignmentStatus.PENDING);
            assertThat(assignment.getSource()).isEqualTo("ROLE_DERIVED");
            assertThat(assignment.getAccessRoleId()).isEqualTo(roleId);
        });
    }

    @Test
    void applyRoleEntitlementsPromotesExistingPendingAssignmentWhenEligibleAccountShadowAppears() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        EntitlementAssignment pendingAssignment = pendingEntitlementAssignment(identityId, entitlementId, resourceId, "ROLE_DERIVED");

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of());
        when(entitlementAssignmentRepository.findPendingByTenantIdAndIdentityIdInAndEntitlementIdInAndSource(
                eq(TENANT_ID), anyCollection(), anyCollection(), anyString())).thenReturn(List.of(pendingAssignment));

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, null, IdentityRoleAssignment.SourceType.RULE);

        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        assertThat(assignmentsCaptor.getValue()).containsExactly(pendingAssignment);
        assertThat(pendingAssignment.getShadowId()).isEqualTo(shadowId);
        assertThat(pendingAssignment.getIdentityId()).isEqualTo(identityId);
        assertThat(pendingAssignment.getStatus()).isEqualTo(EntitlementAssignment.AssignmentStatus.ASSIGNED);
        assertThat(pendingAssignment.getAccessRoleId()).isEqualTo(roleId);
    }

    @Test
    void applyRoleEntitlementsRepairsExistingRoleDerivedAssignmentMissingAccessRoleId() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();
        EntitlementAssignment existing = entitlementAssignment(shadowId, entitlementId, "ROLE_DERIVED");

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(eq(TENANT_ID), anyCollection(), anyCollection(), anyString()))
                .thenReturn(List.of(existing));

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, null, IdentityRoleAssignment.SourceType.RULE);

        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        assertThat(assignmentsCaptor.getValue()).containsExactly(existing);
        assertThat(existing.getAccessRoleId()).isEqualTo(roleId);
    }

    @Test
    void applyRoleEntitlementsCreatesRoleDerivedAssignmentEvenWhenDirectAndDynamicRowsExist() {
        UUID identityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID shadowId = UUID.randomUUID();

        when(resolutionService.resolveEffectiveEntitlementIds(TENANT_ID, roleId)).thenReturn(List.of(entitlementId));
        when(entitlementRepository.findAllById(anyCollection())).thenReturn(List.of(entitlement(entitlementId, resourceId)));
        when(shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, TENANT_ID))
                .thenReturn(List.of(shadow(shadowId, identityId, resourceId.toString(), "account", Shadow.SynchronizationSituation.LINKED)));
        when(entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(
                eq(TENANT_ID), anyCollection(), anyCollection(), eq("ROLE_DERIVED"))).thenReturn(List.of());

        service.applyRoleEntitlements(TENANT_ID, identityId, roleId, null, IdentityRoleAssignment.SourceType.RULE);

        verify(entitlementAssignmentRepository).findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(
                eq(TENANT_ID), anyCollection(), anyCollection(), eq("ROLE_DERIVED"));
        ArgumentCaptor<List<EntitlementAssignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(entitlementAssignmentRepository).saveAll(assignmentsCaptor.capture());
        assertThat(assignmentsCaptor.getValue()).singleElement().satisfies(assignment -> {
            assertThat(assignment.getShadowId()).isEqualTo(shadowId);
            assertThat(assignment.getIdentityId()).isEqualTo(identityId);
            assertThat(assignment.getEntitlementId()).isEqualTo(entitlementId);
            assertThat(assignment.getSource()).isEqualTo("ROLE_DERIVED");
            assertThat(assignment.getAccessRoleId()).isEqualTo(roleId);
        });
    }

    private Entitlement entitlement(UUID entitlementId, UUID resourceId) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setResourceId(resourceId);
        entitlement.setDisplayName("entitlement-" + entitlementId);
        entitlement.setSourceUid("src-" + entitlementId);
        return entitlement;
    }

    private Shadow shadow(UUID shadowId,
                          UUID identityId,
                          String resourceId,
                          String kind,
                          Shadow.SynchronizationSituation situation) {
        Shadow shadow = new Shadow();
        shadow.setId(shadowId);
        shadow.setTenantId(TENANT_ID);
        shadow.setLinkedIdentityId(identityId);
        shadow.setResourceId(resourceId);
        shadow.setKind(kind);
        shadow.setSynchronizationSituation(situation);
        shadow.setResourceUid("uid-" + shadowId);
        shadow.setObjectClass("__ACCOUNT__");
        return shadow;
    }

    private EntitlementAssignment entitlementAssignment(UUID shadowId, UUID entitlementId, String source) {
        EntitlementAssignment assignment = new EntitlementAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setTenantId(TENANT_ID);
        assignment.setShadowId(shadowId);
        assignment.setEntitlementId(entitlementId);
        assignment.setSource(source);
        return assignment;
    }

    private EntitlementAssignment pendingEntitlementAssignment(UUID identityId, UUID entitlementId, UUID resourceId, String source) {
        EntitlementAssignment assignment = entitlementAssignment(null, entitlementId, source);
        assignment.setIdentityId(identityId);
        assignment.setResourceId(resourceId);
        assignment.setStatus(EntitlementAssignment.AssignmentStatus.PENDING);
        return assignment;
    }

    private IdentityRoleAssignment activeAssignment(UUID identityId, UUID roleId) {
        IdentityRoleAssignment assignment = new IdentityRoleAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setTenantId(TENANT_ID);
        assignment.setIdentityId(identityId);
        assignment.setRoleId(roleId);
        assignment.setStatus(IdentityRoleAssignment.Status.ACTIVE);
        assignment.setSourceType(IdentityRoleAssignment.SourceType.RULE);
        assignment.setSourceKey(roleId.toString());
        return assignment;
    }

    private IdentityRoleAssignment activeManualAssignment(UUID identityId, UUID roleId) {
        IdentityRoleAssignment assignment = activeAssignment(identityId, roleId);
        assignment.setSourceType(IdentityRoleAssignment.SourceType.MANUAL);
        assignment.setSourceKey(IdentityRoleAssignment.SourceType.MANUAL.name());
        return assignment;
    }
}

