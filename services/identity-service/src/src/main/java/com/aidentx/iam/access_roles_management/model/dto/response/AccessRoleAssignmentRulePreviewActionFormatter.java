package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Formats internal preview action codes for API responses.
 */
final class AccessRoleAssignmentRulePreviewActionFormatter {

    private AccessRoleAssignmentRulePreviewActionFormatter() {
    }

    static String toFriendlyLabel(String action) {
        if (action == null || action.isBlank()) {
            return action;
        }

        return Arrays.stream(action.trim().split("[_\\s-]+"))
                .filter(part -> !part.isBlank())
                .map(AccessRoleAssignmentRulePreviewActionFormatter::capitalize)
                .collect(Collectors.joining(" "));
    }

    private static String capitalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }
}
