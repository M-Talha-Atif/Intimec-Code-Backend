package com.aidentx.access_portal_service.service.impl;

import java.util.List;
import java.util.UUID;

/**
 * Workflow details for a single access request item.
 * Each item gets its own workflow event, even if multiple items share the same role.
 */
public class ItemWorkflowDetail {
    public final UUID itemId;          // AccessRequestItem.id (DB primary key)
    public final UUID roleId;          // The actual role UUID
    public final String roleName;
    public final List<UUID> userIds;
    public final List<String> userNames;
    public final String justification;
    public final Integer validityDays;

    public ItemWorkflowDetail(
            UUID itemId,
            UUID roleId,
            String roleName,
            List<UUID> userIds,
            List<String> userNames,
            String justification,
            Integer validityDays
    ) {
        this.itemId = itemId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.userIds = userIds;
        this.userNames = userNames;
        this.justification = justification;
        this.validityDays = validityDays;
    }
}
