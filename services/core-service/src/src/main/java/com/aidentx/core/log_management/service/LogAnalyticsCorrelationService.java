package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.CorrelationChain;

import java.util.List;

/**
 * Service contract for computing correlation chain analysis.
 *
 * <p>A correlation chain is a group of log documents sharing the same
 * {@code correlationId}. Only chains with at least
 * {@link com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsCorrelationConstants#MIN_CHAIN_SIZE}
 * documents are included -- single-document correlationIds are not cascades.
 *
 * <p>Chains are ranked by document count descending so the busiest
 * (most likely most impactful) chains appear first.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsCorrelationService {

    /**
     * Returns the top-N correlation chains ordered by chain size descending.
     *
     * <p>Example: limit=2, rangeAll=true:
     * <pre>
     * [
     *   {
     *     "id": "req-17136fed-b7ed-4441-87d4-d59bc7b4730f",
     *     "rootCause": "LDAP bind failed: invalid credentials",
     *     "severity": "high",
     *     "timestamp": "2026-03-18T10:15:00.000Z",
     *     "affectedCount": 4,
     *     "chain": [...]
     *   },
     *   {
     *     "id": "req-b6a53e3b-2c33-4fb4-888a-f4ab943b6833",
     *     "rootCause": "Permission denied for role assignment",
     *     "severity": "medium",
     *     "timestamp": "2026-03-18T09:30:00.000Z",
     *     "affectedCount": 2,
     *     "chain": [...]
     *   }
     * ]
     * </pre>
     *
     * @param limit    number of top chains to return
     * @param hours    look-back window in hours; only used when rangeAll is false
     * @param rangeAll when true, covers full log history with no time filter
     * @return ordered list of correlation chains, longest chain first
     */
    List<CorrelationChain> computeCorrelationChains(int limit, int hours, boolean rangeAll);
}