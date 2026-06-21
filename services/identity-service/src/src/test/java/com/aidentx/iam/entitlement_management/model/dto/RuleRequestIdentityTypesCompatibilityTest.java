package com.aidentx.iam.entitlement_management.model.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleRequestIdentityTypesCompatibilityTest {

    @Test
    void createRequestLegacyIdentityTypePopulatesIdentityTypesWhenMissing() {
        RuleCreateRequest request = new RuleCreateRequest();

        request.setLegacyIdentityType("NON_EMPLOYEE");

        assertThat(request.getIdentityTypes()).containsExactly("NON_EMPLOYEE");
    }

    @Test
    void createRequestLegacyIdentityTypeDoesNotOverrideExplicitIdentityTypes() {
        RuleCreateRequest request = new RuleCreateRequest();
        request.setIdentityTypes(List.of("EMPLOYEE", "NON_HUMAN"));

        request.setLegacyIdentityType("NON_EMPLOYEE");

        assertThat(request.getIdentityTypes()).containsExactly("EMPLOYEE", "NON_HUMAN");
    }

    @Test
    void updateRequestLegacyIdentityTypePopulatesIdentityTypesWhenMissing() {
        RuleUpdateRequest request = new RuleUpdateRequest();

        request.setLegacyIdentityType("EMPLOYEE");

        assertThat(request.getIdentityTypes()).containsExactly("EMPLOYEE");
    }
}
