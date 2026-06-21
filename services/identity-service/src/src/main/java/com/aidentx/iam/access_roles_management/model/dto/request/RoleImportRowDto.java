package com.aidentx.iam.access_roles_management.model.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * One access-role row read from a JSON or CSV import file.
 */
public class RoleImportRowDto extends CreateAccessRoleRequest {

    /** Optional stable key used to resolve childRoleRefs inside the same import file. */
    private String externalRef;

    /** Optional child role references resolved against other imported rows by externalRef. */
    private List<String> childRoleRefs;

    /** Optional identities that should receive this imported role with source IMPORT. */
    private List<UUID> identityIds;

    /** Optional assignment rules to create for this role after creation. */
    private List<CreateAccessRoleAssignmentRuleRequest> rules;

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public List<String> getChildRoleRefs() {
        return childRoleRefs;
    }

    public void setChildRoleRefs(List<String> childRoleRefs) {
        this.childRoleRefs = childRoleRefs;
    }

    public List<UUID> getIdentityIds() {
        return identityIds;
    }

    public void setIdentityIds(List<UUID> identityIds) {
        this.identityIds = identityIds;
    }

    public List<CreateAccessRoleAssignmentRuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<CreateAccessRoleAssignmentRuleRequest> rules) {
        this.rules = rules;
    }
}

