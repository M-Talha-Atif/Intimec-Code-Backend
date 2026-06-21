package com.aidentx.iam.entitlement_management.util;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;

/**
 * Utility for escaping SQL LIKE wildcard characters in user-provided values.
 *
 * <p>Prevents wildcard injection where user input like {@code %admin%}
 * would unintentionally match all rows in a LIKE query.
 *
 * <p>Escapes three characters:
 * <ul>
 *   <li>{@code %} → {@code \%} (multi-char wildcard)</li>
 *   <li>{@code _} → {@code \_} (single-char wildcard)</li>
 *   <li>{@code \} → {@code \\} (the escape char itself)</li>
 * </ul>
 *
 * <p>Must be used together with JPA's {@code cb.like(path, pattern, '\\')}
 * escape clause so the database knows {@code \} is the escape character.
 */
public final class LikePatternEscaper {

    private LikePatternEscaper() {
        // Prevent instantiation
    }

    /**
     * Escapes SQL LIKE wildcard characters in the input string.
     *
     * @param input the raw user-provided value (may contain %, _, \)
     * @return the escaped string safe for use in LIKE patterns
     */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace(String.valueOf(RuleEvaluationConstants.LIKE_ESCAPE_CHAR),
                        String.valueOf(RuleEvaluationConstants.LIKE_ESCAPE_CHAR) + RuleEvaluationConstants.LIKE_ESCAPE_CHAR)
                .replace("%", RuleEvaluationConstants.LIKE_ESCAPE_CHAR + "%")
                .replace("_", RuleEvaluationConstants.LIKE_ESCAPE_CHAR + "_");
    }
}