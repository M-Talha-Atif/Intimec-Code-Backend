package com.aidentx.core.audit_management.service;

import com.aidentx.core.audit_management.model.dto.analytics.TopActorEntry;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for computing the top actors activity summary.
 *
 * <p>Returns a ranked list of actors ordered by total event count descending.
 * Each entry includes the actor name, actor type, total events, failure count,
 * and a randomly generated risk score. The risk score is not persisted and is
 * regenerated on every request.
 *
 * <p>Limit behaviour follows the same contract as
 * {@link AnalyticsEventDistributionService}:
 * <ul>
 *   <li>null -- defaults to
 *       {@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsTopActorsConstants#DEFAULT_LIMIT}
 *       (top 10).</li>
 *   <li>Positive integer -- returns that many actors.</li>
 *   <li>{@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsTopActorsConstants#LIMIT_ALL}
 *       (0) -- returns all distinct actors.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface AnalyticsTopActorsService {

    /**
     * Returns the top actors ranked by total event count.
     *
     * <p>Example: tenant "acme" with default limit=10 produces:
     * <pre>
     * [
     *   { "name": "john.doe",    "type": "user",    "events": 412, "failures": 18, "riskScore": 67 },
     *   { "name": "svc-account", "type": "service", "events": 390, "failures": 2,  "riskScore": 23 },
     *   { "name": "jane.smith",  "type": "user",    "events": 310, "failures": 0,  "riskScore": 9  }
     * ]
     * </pre>
     *
     * @param tenantId non-blank tenant identifier from the caller's JWT
     * @param dateFrom optional window start (inclusive); null triggers default resolution
     * @param dateTo   optional window end (inclusive); null triggers default resolution
     * @param limit    null defaults to top 10; 0 returns all; positive integer returns top N
     * @param rangeAll if true, ignore dateFrom and dateTo              (ignored when rangeAll=true)
     * @return ordered list of top actor entries
     */
    List<TopActorEntry> computeTopActors(
            String tenantId, Instant dateFrom, Instant dateTo, Integer limit, boolean rangeAll);
}