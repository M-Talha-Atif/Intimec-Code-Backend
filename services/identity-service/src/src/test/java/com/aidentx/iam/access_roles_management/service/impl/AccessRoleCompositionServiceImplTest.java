package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.mapper.AccessRoleMapper;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleCompositionConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipDirection;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipMode;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleCompositionServiceImplTest {

    private static final String TENANT_ID = "tenant_001";

    @Mock
    private AccessRoleCompositionRepository compositionRepository;

    @Mock
    private AccessRoleEntitlementRepository entitlementLinkRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private AccessRoleRepository roleRepository;

    @Mock
    private AccessRoleMapper roleMapper;

    @Mock
    private RoleEntitlementClosureService closureService;

    @Mock
    private UnifiedLoggerFactory loggerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IUnifiedLogger logger;

    private final Map<UUID, AccessRole> roles = new LinkedHashMap<>();
    private final Map<UUID, Entitlement> entitlements = new LinkedHashMap<>();
    private final List<AccessRoleComposition> compositions = new ArrayList<>();
    private final List<AccessRoleEntitlement> roleEntitlements = new ArrayList<>();

    private AccessRoleCompositionServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(eq(AccessRoleCompositionServiceImpl.class), anyString())).thenReturn(logger);
        service = new AccessRoleCompositionServiceImpl(
                compositionRepository,
                entitlementLinkRepository,
                entitlementRepository,
                roleRepository,
                roleMapper,
                closureService,
                loggerFactory);
        wireRepositoryAnswers();
    }

    @Test
    void directModeReturnsRootEntitlementsDirectChildrenAndDirectParentsOnly() {
        UUID rootId = addRole("Root Role");
        UUID childId = addRole("Child Role");
        UUID grandChildId = addRole("Grandchild Role");
        UUID parentId = addRole("Parent Role");
        UUID entitlementId = addEntitlement("Root Entitlement");
        addComposition(rootId, childId);
        addComposition(childId, grandChildId);
        addComposition(parentId, rootId);
        addRoleEntitlement(rootId, entitlementId);
        AccessRoleRelationshipResponse response = service.getUnifiedRelationships(
                TENANT_ID, rootId, RoleRelationshipMode.DIRECT, RoleRelationshipDirection.BOTH);
        assertThat(response.maxDepth()).isEqualTo(1);
        assertThat(response.rootRole().id()).isEqualTo(rootId);
        assertThat(response.rootEntitlements()).extracting(AccessRoleRelationshipResponse.RelationshipEntitlement::entitlementId)
                .containsExactly(entitlementId);
        assertThat(response.childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().role().id()).isEqualTo(childId);
        assertThat(response.childRoles().getFirst().childRoles()).isEmpty();
        assertThat(response.parentRoles()).hasSize(1);
        assertThat(response.parentRoles().getFirst().role().id()).isEqualTo(parentId);
        assertThat(response.summary().directChildCount()).isEqualTo(1);
        assertThat(response.summary().directParentCount()).isEqualTo(1);
    }

    @Test
    void hierarchyModeTraversesChildrenButCapsAtDepthTen() {
        UUID rootId = addRole("Role 0");
        UUID previous = rootId;
        for (int i = 1; i <= 11; i++) {
            UUID next = addRole("Role " + i);
            addComposition(previous, next);
            previous = next;
        }
        AccessRoleRelationshipResponse response = service.getUnifiedRelationships(
                TENANT_ID, rootId, RoleRelationshipMode.HIERARCHY, RoleRelationshipDirection.CHILDREN);
        assertThat(response.maxDepth()).isEqualTo(10);
        AccessRoleRelationshipResponse.RoleRelationshipNode node = response.childRoles().getFirst();
        for (int depth = 1; depth < 10; depth++) {
            assertThat(node.depth()).isEqualTo(depth);
            assertThat(node.childRoles()).hasSize(1);
            node = node.childRoles().getFirst();
        }
        assertThat(node.depth()).isEqualTo(10);
        assertThat(node.childRoles()).isEmpty();
        assertThat(response.summary().totalChildRoleCount()).isEqualTo(10);
    }

    @Test
    void hierarchyModeWithBothDirectionReturnsNestedChildrenAndParents() {
        UUID rootId = addRole("Root Role");
        UUID childId = addRole("Child Role");
        UUID grandChildId = addRole("Grandchild Role");
        UUID parentId = addRole("Parent Role");
        UUID grandParentId = addRole("Grandparent Role");

        addComposition(rootId, childId);
        addComposition(childId, grandChildId);
        addComposition(parentId, rootId);
        addComposition(grandParentId, parentId);

        AccessRoleRelationshipResponse response = service.getUnifiedRelationships(
                TENANT_ID, rootId, RoleRelationshipMode.HIERARCHY, RoleRelationshipDirection.BOTH);

        assertThat(response.childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().role().id()).isEqualTo(childId);
        assertThat(response.childRoles().getFirst().childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().childRoles().getFirst().role().id()).isEqualTo(grandChildId);
        assertThat(response.parentRoles()).hasSize(1);
        assertThat(response.parentRoles().getFirst().role().id()).isEqualTo(parentId);
        assertThat(response.parentRoles().getFirst().parentRoles()).hasSize(1);
        assertThat(response.parentRoles().getFirst().parentRoles().getFirst().role().id()).isEqualTo(grandParentId);
        assertThat(response.summary().directChildCount()).isEqualTo(1);
        assertThat(response.summary().directParentCount()).isEqualTo(1);
        assertThat(response.summary().totalChildRoleCount()).isEqualTo(2);
        assertThat(response.summary().totalParentRoleCount()).isEqualTo(2);
    }

    @Test
    void conflictModeKeepsOnlyEntitlementsOverlappingAcrossRootAndDescendants() {
        UUID rootId = addRole("Root Role");
        UUID childId = addRole("Child Role");
        UUID grandChildId = addRole("Grandchild Role");
        UUID unrelatedChildId = addRole("Unrelated Child Role");
        UUID sharedEntitlementId = addEntitlement("Shared Entitlement");
        UUID rootOnlyEntitlementId = addEntitlement("Root Only Entitlement");
        UUID childOnlyEntitlementId = addEntitlement("Child Only Entitlement");
        UUID unrelatedEntitlementId = addEntitlement("Unrelated Entitlement");

        addComposition(rootId, childId);
        addComposition(childId, grandChildId);
        addComposition(rootId, unrelatedChildId);
        addRoleEntitlement(rootId, sharedEntitlementId);
        addRoleEntitlement(rootId, rootOnlyEntitlementId);
        addRoleEntitlement(childId, sharedEntitlementId);
        addRoleEntitlement(childId, childOnlyEntitlementId);
        addRoleEntitlement(grandChildId, sharedEntitlementId);
        addRoleEntitlement(grandChildId, childOnlyEntitlementId);
        addRoleEntitlement(unrelatedChildId, unrelatedEntitlementId);

        AccessRoleRelationshipResponse response = service.getUnifiedRelationships(
                TENANT_ID, rootId, RoleRelationshipMode.CONFLICT, RoleRelationshipDirection.CHILDREN);
        String expectedReason = conflictReason("Shared Entitlement", "Root Role", "Child Role", "Grandchild Role");

        assertThat(response.rootEntitlements()).extracting(AccessRoleRelationshipResponse.RelationshipEntitlement::entitlementId)
                .containsExactly(sharedEntitlementId);
        assertThat(response.rootEntitlements().getFirst().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().role().id()).isEqualTo(childId);
        assertThat(response.childRoles().getFirst().entitlements())
                .extracting(AccessRoleRelationshipResponse.RelationshipEntitlement::entitlementId)
                .containsExactly(sharedEntitlementId);
        assertThat(response.childRoles().getFirst().entitlements().getFirst().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.childRoles().getFirst().childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().childRoles().getFirst().entitlements())
                .extracting(AccessRoleRelationshipResponse.RelationshipEntitlement::entitlementId)
                .containsExactly(sharedEntitlementId);
        assertThat(response.childRoles().getFirst().childRoles().getFirst().entitlements().getFirst().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts()).hasSize(1);
        assertThat(response.conflicts().getFirst().entitlement().entitlementId()).isEqualTo(sharedEntitlementId);
        assertThat(response.conflicts().getFirst().entitlement().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts().getFirst().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts().getFirst().roles()).extracting(AccessRoleRelationshipResponse.RoleSummary::id)
                .containsExactly(rootId, childId, grandChildId);
        assertThat(response.summary().conflictEntitlementCount()).isEqualTo(1);
        assertThat(response.summary().totalEntitlementCount()).isEqualTo(1);
        assertThat(response.summary().totalChildRoleCount()).isEqualTo(2);
    }

    @Test
    void conflictModeKeepsOnlyBranchesThatLeadToRootDuplicateEntitlements() {
        UUID rootId = addRole("Root Role");
        UUID pathOnlyChildId = addRole("Path Only Child Role");
        UUID conflictingGrandChildId = addRole("Conflicting Grandchild Role");
        UUID unrelatedChildId = addRole("Unrelated Child Role");
        UUID sharedEntitlementId = addEntitlement("Shared Entitlement");
        UUID unrelatedEntitlementId = addEntitlement("Unrelated Entitlement");

        addComposition(rootId, pathOnlyChildId);
        addComposition(pathOnlyChildId, conflictingGrandChildId);
        addComposition(rootId, unrelatedChildId);
        addRoleEntitlement(rootId, sharedEntitlementId);
        addRoleEntitlement(conflictingGrandChildId, sharedEntitlementId);
        addRoleEntitlement(unrelatedChildId, unrelatedEntitlementId);

        AccessRoleRelationshipResponse response = service.getUnifiedRelationships(
                TENANT_ID, rootId, RoleRelationshipMode.CONFLICT, RoleRelationshipDirection.CHILDREN);
        String expectedReason = conflictReason("Shared Entitlement", "Root Role", "Conflicting Grandchild Role");

        assertThat(response.childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().role().id()).isEqualTo(pathOnlyChildId);
        assertThat(response.childRoles().getFirst().entitlements()).isEmpty();
        assertThat(response.childRoles().getFirst().childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().childRoles().getFirst().role().id()).isEqualTo(conflictingGrandChildId);
        assertThat(response.childRoles().getFirst().childRoles().getFirst().entitlements())
                .extracting(AccessRoleRelationshipResponse.RelationshipEntitlement::entitlementId)
                .containsExactly(sharedEntitlementId);
        assertThat(response.conflicts()).hasSize(1);
        assertThat(response.conflicts().getFirst().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts().getFirst().entitlement().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts().getFirst().roles()).extracting(AccessRoleRelationshipResponse.RoleSummary::id)
                .containsExactly(rootId, conflictingGrandChildId);
        assertThat(response.summary().totalChildRoleCount()).isEqualTo(2);
        assertThat(response.summary().totalRoleCount()).isEqualTo(3);
    }

    @Test
    void conflictModeWithBothDirectionReportsParentAndChildEntitlementConflicts() {
        UUID rootId = addRole("Root Role");
        UUID childId = addRole("Child Role");
        UUID parentId = addRole("Parent Role");
        UUID unrelatedChildId = addRole("Unrelated Child Role");
        UUID sharedEntitlementId = addEntitlement("Shared Entitlement");
        UUID unrelatedEntitlementId = addEntitlement("Unrelated Entitlement");

        addComposition(rootId, childId);
        addComposition(parentId, rootId);
        addComposition(rootId, unrelatedChildId);
        addRoleEntitlement(rootId, sharedEntitlementId);
        addRoleEntitlement(childId, sharedEntitlementId);
        addRoleEntitlement(parentId, sharedEntitlementId);
        addRoleEntitlement(unrelatedChildId, unrelatedEntitlementId);

        AccessRoleRelationshipResponse response = service.getUnifiedRelationships(
                TENANT_ID, rootId, RoleRelationshipMode.CONFLICT, RoleRelationshipDirection.BOTH);
        String expectedReason = conflictReason("Shared Entitlement", "Root Role", "Child Role", "Parent Role");

        assertThat(response.childRoles()).hasSize(1);
        assertThat(response.childRoles().getFirst().role().id()).isEqualTo(childId);
        assertThat(response.parentRoles()).hasSize(1);
        assertThat(response.parentRoles().getFirst().role().id()).isEqualTo(parentId);
        assertThat(response.conflicts()).hasSize(1);
        assertThat(response.conflicts().getFirst().entitlement().entitlementId()).isEqualTo(sharedEntitlementId);
        assertThat(response.conflicts().getFirst().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts().getFirst().entitlement().conflictReason())
                .isEqualTo(expectedReason);
        assertThat(response.conflicts().getFirst().roles()).extracting(AccessRoleRelationshipResponse.RoleSummary::id)
                .containsExactly(rootId, childId, parentId);
        assertThat(response.summary().directChildCount()).isEqualTo(1);
        assertThat(response.summary().directParentCount()).isEqualTo(1);
        assertThat(response.summary().totalChildRoleCount()).isEqualTo(1);
        assertThat(response.summary().totalParentRoleCount()).isEqualTo(1);
    }

    private void wireRepositoryAnswers() {
        when(roleRepository.findById(org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID roleId = invocation.getArgument(0, UUID.class);
                    return Optional.ofNullable(roles.get(roleId));
                });
        when(roleRepository.findByIdIn(anySet())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return ids.stream().map(roles::get).filter(java.util.Objects::nonNull).toList();
        });
        when(compositionRepository.findByParentRoleIdIn(anySet())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return compositions.stream().filter(link -> ids.contains(link.getParentRoleId())).toList();
        });
        lenient().when(compositionRepository.findByChildRoleIdIn(anySet())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return compositions.stream().filter(link -> ids.contains(link.getChildRoleId())).toList();
        });
        when(entitlementLinkRepository.findByRoleIdIn(org.mockito.ArgumentMatchers.anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return roleEntitlements.stream().filter(link -> ids.contains(link.getRoleId())).toList();
        });
        when(entitlementRepository.findAllById(org.mockito.ArgumentMatchers.anyIterable())).thenAnswer(invocation -> {
            Iterable<UUID> ids = invocation.getArgument(0);
            List<Entitlement> result = new ArrayList<>();
            ids.forEach(id -> {
                Entitlement entitlement = entitlements.get(id);
                if (entitlement != null) {
                    result.add(entitlement);
                }
            });
            return result;
        });
    }

    private UUID addRole(String name) {
        UUID id = UUID.randomUUID();
        AccessRole role = new AccessRole();
        role.setId(id);
        role.setTenantId(TENANT_ID);
        role.setName(name);
        role.setDisplayName(name);
        role.setType("business");
        role.setStatus("active");
        role.setRiskLevel("low");
        roles.put(id, role);
        return id;
    }

    private UUID addEntitlement(String displayName) {
        UUID id = UUID.randomUUID();
        Entitlement entitlement = new Entitlement();
        entitlement.setId(id);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setDisplayName(displayName);
        entitlement.setDescription(displayName + " description");
        entitlement.setResourceId(UUID.randomUUID());
        entitlement.setSourceUid(displayName.toLowerCase().replace(' ', '-'));
        entitlement.setObjectClass("group");
        entitlement.setKind("permission");
        entitlement.setLifecycle("Active");
        entitlement.setRiskLevel("low");
        entitlement.setPrivilegeLevel("standard");
        entitlements.put(id, entitlement);
        return id;
    }

    private void addComposition(UUID parentId, UUID childId) {
        AccessRoleComposition composition = new AccessRoleComposition();
        composition.setParentRoleId(parentId);
        composition.setChildRoleId(childId);
        compositions.add(composition);
    }

    private void addRoleEntitlement(UUID roleId, UUID entitlementId) {
        AccessRoleEntitlement link = new AccessRoleEntitlement();
        link.setRoleId(roleId);
        link.setEntitlementId(entitlementId);
        roleEntitlements.add(link);
    }

    private String conflictReason(String entitlementName, String... roleNames) {
        return String.format(
                AccessRoleCompositionConstants.CONFLICT_REASON_ENTITLEMENT_OVERLAP_WITH_ROLES,
                entitlementName,
                String.join(", ", roleNames));
    }
}
