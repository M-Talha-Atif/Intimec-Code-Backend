package com.aidentx.iam.access_roles_management.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared read/query logic for access-role assignment-rule preview and scans.
 */
public interface AccessRoleAssignmentRuleQueryService {

    Set<UUID> findAllMatchingIdentityIds(String tenantId,
                                         List<String> identityTypes,
                                         List<Map<String, Object>> conditionGroups);

    Map<UUID, IdentitySnapshot> loadIdentitySnapshots(String tenantId,
                                                      Collection<UUID> identityIds,
                                                      List<String> identityTypes);

    Map<String, Object> buildEvidence(IdentitySnapshot snapshot,
                                      List<Map<String, Object>> conditionGroups,
                                      boolean matched,
                                      String errorMessage);

    record IdentitySnapshot(UUID id,
                            String displayName,
                            String identityType,
                            Map<String, Object> fieldValues) {
    }
}

