package com.aidentx.iam.access_roles_management.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for bulk manual access-role grants to identities.
 */
public class ManualRoleMemberGrantRequest {

    @NotNull(message = "Identity IDs list is required")
    private List<@NotNull(message = "Identity ID is required") UUID> identityIds;

    public List<UUID> getIdentityIds() {
        return identityIds;
    }

    public void setIdentityIds(List<UUID> identityIds) {
        this.identityIds = identityIds;
    }
}

