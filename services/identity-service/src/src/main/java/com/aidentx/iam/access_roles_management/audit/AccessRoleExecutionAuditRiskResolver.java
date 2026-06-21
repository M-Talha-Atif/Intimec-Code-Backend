package com.aidentx.iam.access_roles_management.audit;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Resolves normalized and aggregate risk levels for access-role execution audits.
 *
 * <p>Risk hierarchy is {@code high > medium > low}. Unknown or blank values are
 * treated as absent and ignored during highest-risk aggregation.</p>
 */
@Component
public class AccessRoleExecutionAuditRiskResolver {

    /**
     * Normalizes a raw risk-level value to {@code high}, {@code medium}, or {@code low}.
     *
     * @param riskLevel raw risk-level value
     * @return normalized risk level, or {@code null} when unknown/blank
     */
    public String normalize(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return null;
        }
        String normalized = riskLevel.trim().toLowerCase();
        return switch (normalized) {
            case "high", "medium", "low" -> normalized;
            default -> null;
        };
    }

    /**
     * Returns the highest risk level from the supplied collection.
     *
     * @param riskLevels risk levels to inspect
     * @return {@code high}, {@code medium}, {@code low}, or {@code null}
     */
    public String highestOf(Collection<String> riskLevels) {
        boolean hasMedium = false;
        boolean hasLow = false;
        if (riskLevels == null) {
            return null;
        }
        for (String riskLevel : riskLevels) {
            String normalized = normalize(riskLevel);
            if ("high".equals(normalized)) {
                return "high";
            }
            if ("medium".equals(normalized)) {
                hasMedium = true;
            } else if ("low".equals(normalized)) {
                hasLow = true;
            }
        }
        if (hasMedium) {
            return "medium";
        }
        if (hasLow) {
            return "low";
        }
        return null;
    }
}

