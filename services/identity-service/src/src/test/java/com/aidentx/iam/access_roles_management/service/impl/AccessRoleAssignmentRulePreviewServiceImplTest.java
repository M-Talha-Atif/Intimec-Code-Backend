package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRulePreviewRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRulePreviewResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleExpressionParser;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleNormalizationService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleQueryService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleAssignmentRulePreviewServiceImplTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private AccessRoleRepository accessRoleRepository;

    @Mock
    private AccessRoleAssignmentRuleRepository ruleRepository;

    @Mock
    private IdentityRoleAssignmentRepository assignmentRepository;

    @Mock
    private RoleEntitlementResolutionService roleEntitlementResolutionService;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private AccessRoleAssignmentRuleExpressionParser expressionParser;

    @Mock
    private AccessRoleAssignmentRuleNormalizationService normalizationService;

    @Mock
    private AccessRoleAssignmentRuleQueryService queryService;

    private AccessRoleAssignmentRulePreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccessRoleAssignmentRulePreviewServiceImpl(
                accessRoleRepository,
                ruleRepository,
                assignmentRepository,
                roleEntitlementResolutionService,
                entitlementRepository,
                expressionParser,
                normalizationService,
                queryService);
    }

    @Test
    void previewRuleCalculatesAddsRemovesAndAlreadyAssignedWithoutWriting() {
        UUID roleId = UUID.randomUUID();
        UUID addIdentityId = UUID.randomUUID();
        UUID keepIdentityId = UUID.randomUUID();
        UUID removeIdentityId = UUID.randomUUID();
        UUID entitlementId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        AccessRole role = new AccessRole();
        role.setId(roleId);
        role.setTenantId(TENANT_ID);
        role.setName("Finance Manager");
        role.setType("business");

        AccessRoleAssignmentRulePreviewRequest request = new AccessRoleAssignmentRulePreviewRequest();
        request.setRoleId(roleId);
        request.setExpression("user.department == \"Finance\"");
        request.setIdentityTypes(List.of("EMPLOYEE"));
        request.setPage(0);
        request.setSize(10);

        AccessRoleAssignmentRuleConditionGroupDto group = new AccessRoleAssignmentRuleConditionGroupDto(
                "AND",
                "AND",
                false,
                List.of(new AccessRoleAssignmentRuleConditionDto("department", "EQUALS", "Finance", null)),
                null);
        List<Map<String, Object>> normalizedGroups = List.of(Map.of(
                "groupOperator", "AND",
                "interGroupOperator", "AND",
                "negated", false,
                "conditions", List.of(Map.of("field", "department", "operator", "EQUALS", "value", "Finance"))));

        IdentityRoleAssignment keep = activeAssignment(keepIdentityId, roleId);
        IdentityRoleAssignment remove = activeAssignment(removeIdentityId, roleId);
        RoleEntitlementClosure closure = closure(roleId, entitlementId);
        Entitlement entitlement = entitlement(entitlementId, resourceId);

        Map<UUID, AccessRoleAssignmentRuleQueryService.IdentitySnapshot> snapshots = new LinkedHashMap<>();
        snapshots.put(addIdentityId, snapshot(addIdentityId, "Alice Finance"));
        snapshots.put(removeIdentityId, snapshot(removeIdentityId, "Bob Former Finance"));
        snapshots.put(keepIdentityId, snapshot(keepIdentityId, "Carol Finance"));

        when(accessRoleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(ruleRepository.findByTenantIdAndRoleId(TENANT_ID, roleId)).thenReturn(Optional.empty());
        when(expressionParser.parse(request.getExpression())).thenReturn(List.of(group));
        when(normalizationService.normalizeIdentityTypes(List.of("EMPLOYEE"), false)).thenReturn(List.of("EMPLOYEE"));
        when(normalizationService.normalizeConditionGroups(List.of(group), List.of("EMPLOYEE"))).thenReturn(normalizedGroups);
        when(queryService.findAllMatchingIdentityIds(TENANT_ID, List.of("EMPLOYEE"), normalizedGroups))
                .thenReturn(new LinkedHashSet<>(List.of(addIdentityId, keepIdentityId)));
        when(assignmentRepository.findByTenantIdAndRoleIdAndStatus(TENANT_ID, roleId, IdentityRoleAssignment.Status.ACTIVE))
                .thenReturn(List.of(keep, remove));
        when(roleEntitlementResolutionService.resolveEffectiveEntitlementMappings(TENANT_ID, roleId)).thenReturn(List.of(closure));
        when(entitlementRepository.findAllById(any())).thenReturn(List.of(entitlement));
        when(queryService.loadIdentitySnapshots(eq(TENANT_ID), any(), eq(List.of("EMPLOYEE")))).thenReturn(snapshots);
        when(queryService.buildEvidence(any(), eq(normalizedGroups), any(Boolean.class), eq(null))).thenReturn(Map.of("overallMatch", true));

        AccessRoleAssignmentRulePreviewResponse response = service.previewRule(TENANT_ID, request);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getValidationMessage()).isEqualTo("Rule Validation Successful");
        assertThat(response.getTotalMatched()).isEqualTo(2);
        assertThat(response.getWillBeAdded()).isEqualTo(1);
        assertThat(response.getWillBeRemoved()).isEqualTo(1);
        assertThat(response.getAlreadyAssigned()).isEqualTo(1);
        assertThat(response.getTotalAffected()).isEqualTo(2);
        assertThat(response.getPreviewPopulation()).hasSize(3);
        assertThat(response.getPreviewPopulation()).extracting("previewAction")
                .containsExactly("Will Add", "Will Remove", "Already Assigned");
        assertThat(response.getPreviewPopulation().get(0).getEntitlements()).extracting("previewAction")
                .containsExactly("Will Add");
        assertThat(response.getEntitlementSummary().getEffectiveEntitlementCount()).isEqualTo(1);
        assertThat(response.getEntitlementSummary().getEntitlements()).hasSize(1);
        verify(queryService).findAllMatchingIdentityIds(TENANT_ID, List.of("EMPLOYEE"), normalizedGroups);
    }

    private IdentityRoleAssignment activeAssignment(UUID identityId, UUID roleId) {
        IdentityRoleAssignment assignment = new IdentityRoleAssignment();
        assignment.setTenantId(TENANT_ID);
        assignment.setIdentityId(identityId);
        assignment.setRoleId(roleId);
        assignment.setStatus(IdentityRoleAssignment.Status.ACTIVE);
        return assignment;
    }

    private RoleEntitlementClosure closure(UUID roleId, UUID entitlementId) {
        RoleEntitlementClosure closure = new RoleEntitlementClosure();
        closure.setTenantId(TENANT_ID);
        closure.setRoleId(roleId);
        closure.setSourceRoleId(roleId);
        closure.setEntitlementId(entitlementId);
        closure.setDirect(true);
        closure.setDepth(0);
        return closure;
    }

    private Entitlement entitlement(UUID entitlementId, UUID resourceId) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(entitlementId);
        entitlement.setTenantId(TENANT_ID);
        entitlement.setDisplayName("FINANCE_READ");
        entitlement.setResourceId(resourceId);
        entitlement.setSourceUid("finance-read");
        entitlement.setKind("group");
        entitlement.setObjectClass("__GROUP__");
        entitlement.setRiskLevel("Low");
        return entitlement;
    }

    private AccessRoleAssignmentRuleQueryService.IdentitySnapshot snapshot(UUID identityId, String displayName) {
        return new AccessRoleAssignmentRuleQueryService.IdentitySnapshot(
                identityId,
                displayName,
                "EMPLOYEE",
                Map.of("displayName", displayName, "department", "Finance"));
    }
}

