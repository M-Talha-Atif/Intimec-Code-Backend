package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessRoleAssignmentRuleExpressionParserTest {

    private final AccessRoleAssignmentRuleExpressionParser parser = new AccessRoleAssignmentRuleExpressionParser();

    @Test
    void parsesUiExpressionIntoSingleAndGroup() {
        List<AccessRoleAssignmentRuleConditionGroupDto> groups = parser.parse(
                "user.department == \"Finance\" && user.title.contains(\"Manager\")");

        assertThat(groups).hasSize(1);
        AccessRoleAssignmentRuleConditionGroupDto group = groups.get(0);
        assertThat(group.getGroupOperator()).isEqualTo("AND");
        assertThat(group.getConditions()).hasSize(2);
        assertThat(group.getConditions().get(0).getField()).isEqualTo("department");
        assertThat(group.getConditions().get(0).getOperator()).isEqualTo("EQUALS");
        assertThat(group.getConditions().get(0).getValue()).isEqualTo("Finance");
        assertThat(group.getConditions().get(1).getField()).isEqualTo("jobTitle");
        assertThat(group.getConditions().get(1).getOperator()).isEqualTo("CONTAINS");
        assertThat(group.getConditions().get(1).getValue()).isEqualTo("Manager");
    }

    @Test
    void parsesTopLevelOrIntoSiblingGroups() {
        List<AccessRoleAssignmentRuleConditionGroupDto> groups = parser.parse(
                "user.department == \"Finance\" || user.department == \"Accounting\"");

        assertThat(groups).hasSize(2);
        assertThat(groups).extracting(AccessRoleAssignmentRuleConditionGroupDto::getInterGroupOperator)
                .containsExactly("OR", "OR");
        assertThat(groups.get(1).getConditions().get(0).getValue()).isEqualTo("Accounting");
    }

    @Test
    void rejectsBlankExpression() {
        assertThatThrownBy(() -> parser.parse(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rule expression is required");
    }
}

