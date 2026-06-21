package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.entity.OrgUnit;

import java.util.UUID;

/**
 * Pure helpers for computing the materialized {@code path} stored on
 * {@link OrgUnit}. Paths are slash-delimited UUID strings beginning and
 * ending with {@code '/'}, e.g. {@code /a1b2.../c3d4.../}.
 */
public final class OrgPathService {

    /** Path of a brand-new root unit, given its own id. */
    public static String rootPath(UUID id) {
        return "/" + id + "/";
    }

    /** Path of a child unit, given the parent's path and the child's id. */
    public static String childPath(String parentPath, UUID childId) {
        if (parentPath == null || parentPath.isBlank()) {
            return rootPath(childId);
        }
        if (!parentPath.endsWith("/")) {
            parentPath = parentPath + "/";
        }
        return parentPath + childId + "/";
    }

    /**
     * Replace the parent prefix in {@code currentPath} when a unit is moved.
     * E.g. moving {@code /a/b/c/d/} from parent {@code /a/b/} to
     * {@code /x/y/} yields {@code /x/y/c/d/}.
     */
    public static String rebase(String currentPath, String oldParentPath, String newParentPath) {
        if (oldParentPath == null) oldParentPath = "/";
        if (newParentPath == null) newParentPath = "/";
        if (!currentPath.startsWith(oldParentPath)) {
            throw new IllegalStateException(
                    "Path " + currentPath + " does not start with " + oldParentPath);
        }
        return newParentPath + currentPath.substring(oldParentPath.length());
    }

    /** Number of '/' separators minus one — depth of the unit (root = 0). */
    public static int depthOf(String path) {
        if (path == null || path.length() < 2) return 0;
        int count = -1;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') count++;
        }
        return Math.max(0, count);
    }

    private OrgPathService() {}
}
