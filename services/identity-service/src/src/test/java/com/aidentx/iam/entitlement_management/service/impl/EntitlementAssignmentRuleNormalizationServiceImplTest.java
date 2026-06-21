package com.aidentx.iam.entitlement_management.service.impl;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionDto;
import com.aidentx.iam.entitlement_management.model.dto.RuleConditionGroupDto;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class EntitlementAssignmentRuleNormalizationServiceImplTest {
    private IdentityFieldRegistry fieldRegistry;
    private EntitlementAssignmentRuleNormalizationServiceImpl service;
    @BeforeEach
    void setUp() {
        fieldRegistry = new IdentityFieldRegistry();
        fieldRegistry.init();
        service = new EntitlementAssignmentRuleNormalizationServiceImpl(fieldRegistry);
    }
    @Test
    void normalizeIdentityTypesDefaultsToEmployeeWhenMissing() {
        assertThat(service.normalizeIdentityTypes(null, true))
                .containsExactly("EMPLOYEE");
    }
    @Test
    void normalizeIdentityTypesUppercasesAndDeduplicatesValues() {
        assertThat(service.normalizeIdentityTypes(List.of("employee", "NON_EMPLOYEE", " employee "), false))
                .containsExactly("EMPLOYEE", "NON_EMPLOYEE");
    }
    @SuppressWarnings("unchecked")
    @Test
    void normalizeConditionGroupsResolvesFieldsAcrossSelectedIdentityTypes() {
        RuleConditionDto vendorCondition = new RuleConditionDto();
        vendorCondition.setField("vendorName");
        vendorCondition.setOperator("equals");
        vendorCondition.setValue("Acme");
        RuleConditionGroupDto group = new RuleConditionGroupDto();
        group.setConditions(List.of(vendorCondition));
        List<Map<String, Object>> normalized = service.normalizeConditionGroups(
                List.of(group),
                List.of("EMPLOYEE", "NON_EMPLOYEE"));
        Map<String, Object> normalizedGroup = normalized.get(0);
        List<Map<String, Object>> normalizedConditions = (List<Map<String, Object>>) normalizedGroup.get("conditions");
        assertThat(normalizedGroup)
                .containsEntry("groupOperator", "AND")
                .containsEntry("interGroupOperator", "AND")
                .containsEntry("negated", false);
        assertThat(normalizedConditions)
                .singleElement()
                .satisfies(condition -> assertThat((Map<String, Object>) condition)
                        .containsEntry("field", "vendorName")
                        .containsEntry("operator", "EQUALS")
                        .containsEntry("value", "Acme"));
    }
    @SuppressWarnings("unchecked")
    @Test
    void wrapLegacyConditionsCreatesSingleDefaultGroup() {
        RuleConditionDto serviceNameCondition = new RuleConditionDto();
        serviceNameCondition.setField("serviceName");
        serviceNameCondition.setOperator("contains");
        serviceNameCondition.setValue("svc");
        List<Map<String, Object>> wrapped = service.wrapLegacyConditions(
                List.of(serviceNameCondition),
                List.of("NON_HUMAN"));
        Map<String, Object> group = wrapped.get(0);
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) group.get("conditions");
        assertThat(group)
                .containsEntry("groupOperator", "AND")
                .containsEntry("interGroupOperator", "AND")
                .containsEntry("negated", false);
        assertThat((List<?>) group.get("conditionGroups")).isEmpty();
        assertThat(conditions)
                .singleElement()
                .satisfies(condition -> assertThat((Map<String, Object>) condition)
                        .containsEntry("field", "serviceName")
                        .containsEntry("operator", "CONTAINS")
                        .containsEntry("value", "svc"));
    }
    @Test
    void normalizeConditionGroupsRejectsUnknownFieldAcrossAllIdentityTypes() {
        RuleConditionDto invalidCondition = new RuleConditionDto();
        invalidCondition.setField("doesNotExist");
        invalidCondition.setOperator("equals");
        invalidCondition.setValue("x");
        RuleConditionGroupDto group = new RuleConditionGroupDto();
        group.setConditions(List.of(invalidCondition));
        assertThatThrownBy(() -> service.normalizeConditionGroups(List.of(group), List.of("EMPLOYEE", "NON_EMPLOYEE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doesNotExist");
    }
    @Test
    void fieldRegistryReturnsDiscoveredFieldsForNonEmployeeAndNonHumanIdentities() {
        assertThat(fieldRegistry.getFieldsForClass(NonEmployee.class))
                .contains("contractId", "vendorName")
                .doesNotContain("companyName", "contractType");
        assertThat(fieldRegistry.getFieldsForClass(NonHumanIdentity.class))
                .contains("serviceName", "ownerTeam")
                .doesNotContain("ownerIdentityId");
    }
}
