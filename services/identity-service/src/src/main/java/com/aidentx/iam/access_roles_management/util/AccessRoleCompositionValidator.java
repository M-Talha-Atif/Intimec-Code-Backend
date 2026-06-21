package com.aidentx.iam.access_roles_management.util;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;

import java.util.*;

/**
 * Utility class for validating role composition integrity.
 * <p>
 * Provides cycle detection to prevent circular dependencies
 * in the role hierarchy tree.
 * </p>
 */
public final class AccessRoleCompositionValidator {

    private AccessRoleCompositionValidator() {}

    /**
     * Checks whether adding a child to a parent would create a circular dependency.
     * <p>
     * Traverses the hierarchy upward from the parent to see if the proposed child
     * is already an ancestor of the parent (which would create a cycle).
     * </p>
     *
     * @param parentRoleId   the proposed parent role ID
     * @param childRoleId    the proposed child role ID
     * @param compositionRepo the composition repository for lookups
     * @return true if adding the child would create a cycle, false otherwise
     */
    public static boolean wouldCreateCycle(
            UUID parentRoleId,
            UUID childRoleId,
            AccessRoleCompositionRepository compositionRepo) {

        if (parentRoleId.equals(childRoleId)) {
            return true;
        }

        // BFS upward from parentRoleId — if we reach childRoleId, it's a cycle
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();
        queue.add(parentRoleId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            List<AccessRoleComposition> parentLinks = compositionRepo.findByChildRoleId(current);
            for (AccessRoleComposition link : parentLinks) {
                UUID ancestor = link.getParentRoleId();
                if (ancestor.equals(childRoleId)) {
                    return true;
                }
                queue.add(ancestor);
            }
        }

        return false;
    }
}

