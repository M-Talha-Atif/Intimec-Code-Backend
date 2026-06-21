package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ManageRoleRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void entitlementRequestRejectsNullList() {
        ManageRoleEntitlementsRequest request = new ManageRoleEntitlementsRequest();

        Set<ConstraintViolation<ManageRoleEntitlementsRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Entitlement IDs list is required");
    }

    @Test
    void entitlementRequestRejectsNullElement() {
        ManageRoleEntitlementsRequest request = new ManageRoleEntitlementsRequest();
        request.setEntitlementIds(Arrays.asList(UUID.randomUUID(), null));

        Set<ConstraintViolation<ManageRoleEntitlementsRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Entitlement ID is required");
    }

    @Test
    void entitlementRequestAllowsEmptyAndDuplicateLists() {
        UUID entitlementId = UUID.randomUUID();
        ManageRoleEntitlementsRequest emptyRequest = new ManageRoleEntitlementsRequest();
        emptyRequest.setEntitlementIds(List.of());
        ManageRoleEntitlementsRequest duplicateRequest = new ManageRoleEntitlementsRequest();
        duplicateRequest.setEntitlementIds(List.of(entitlementId, entitlementId));

        assertThat(VALIDATOR.validate(emptyRequest)).isEmpty();
        assertThat(VALIDATOR.validate(duplicateRequest)).isEmpty();
    }

    @Test
    void compositionRequestRejectsNullList() {
        ManageRoleCompositionsRequest request = new ManageRoleCompositionsRequest();

        Set<ConstraintViolation<ManageRoleCompositionsRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Child role IDs list is required");
    }

    @Test
    void compositionRequestRejectsNullElement() {
        ManageRoleCompositionsRequest request = new ManageRoleCompositionsRequest();
        request.setChildRoleIds(Arrays.asList(UUID.randomUUID(), null));

        Set<ConstraintViolation<ManageRoleCompositionsRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Child role ID is required");
    }

    @Test
    void compositionRequestAllowsEmptyAndDuplicateLists() {
        UUID childRoleId = UUID.randomUUID();
        ManageRoleCompositionsRequest emptyRequest = new ManageRoleCompositionsRequest();
        emptyRequest.setChildRoleIds(List.of());
        ManageRoleCompositionsRequest duplicateRequest = new ManageRoleCompositionsRequest();
        duplicateRequest.setChildRoleIds(List.of(childRoleId, childRoleId));

        assertThat(VALIDATOR.validate(emptyRequest)).isEmpty();
        assertThat(VALIDATOR.validate(duplicateRequest)).isEmpty();
    }

    @Test
    void manualGrantRequestRejectsNullList() {
        ManualRoleMemberGrantRequest request = new ManualRoleMemberGrantRequest();

        Set<ConstraintViolation<ManualRoleMemberGrantRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Identity IDs list is required");
    }

    @Test
    void manualGrantRequestRejectsNullElement() {
        ManualRoleMemberGrantRequest request = new ManualRoleMemberGrantRequest();
        request.setIdentityIds(Arrays.asList(UUID.randomUUID(), null));

        Set<ConstraintViolation<ManualRoleMemberGrantRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Identity ID is required");
    }

    @Test
    void manualGrantRequestAllowsEmptyAndDuplicateLists() {
        UUID identityId = UUID.randomUUID();
        ManualRoleMemberGrantRequest emptyRequest = new ManualRoleMemberGrantRequest();
        emptyRequest.setIdentityIds(List.of());
        ManualRoleMemberGrantRequest duplicateRequest = new ManualRoleMemberGrantRequest();
        duplicateRequest.setIdentityIds(List.of(identityId, identityId));

        assertThat(VALIDATOR.validate(emptyRequest)).isEmpty();
        assertThat(VALIDATOR.validate(duplicateRequest)).isEmpty();
    }
}

