package com.aidentx.iam.access_roles_management.service.export;

import java.util.List;

/**
 * Filter parameters forwarded from the export controller to the list-roles call
 * so the export observes the same filter semantics as the regular list endpoint.
 *
 * <p>All fields are optional; {@code null} or empty means "no filter".</p>
 */
public record AccessRoleExportFilters(
        List<String> types,
        List<String> statuses,
        List<String> businessUnits,
        List<String> riskLevels,
        String search
) {

    public static AccessRoleExportFilters empty() {
        return new AccessRoleExportFilters(null, null, null, null, null);
    }
}

