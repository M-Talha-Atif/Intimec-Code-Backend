package com.aidentx.core.audit_management.controller;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventDistributionConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventVolumeConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsKpisConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsRangeConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsTopActorsConstants;
import com.aidentx.core.audit_management.model.dto.analytics.AnalyticsKpisResponse;
import com.aidentx.core.audit_management.model.dto.analytics.EventDistributionEntry;
import com.aidentx.core.audit_management.model.dto.analytics.EventVolumeEntry;
import com.aidentx.core.audit_management.model.dto.analytics.TopActorEntry;
import com.aidentx.core.audit_management.service.AnalyticsEventDistributionService;
import com.aidentx.core.audit_management.service.AnalyticsEventVolumeService;
import com.aidentx.core.audit_management.service.AnalyticsKpisService;
import com.aidentx.core.audit_management.service.AnalyticsTopActorsService;
import com.aidentx.core.audit_management.validation.AnalyticsRequestValidator;
import com.aidentx.core.shared.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsHeatmapConstants;
import com.aidentx.core.audit_management.model.dto.analytics.HeatmapEntry;
import com.aidentx.core.audit_management.service.AnalyticsHeatmapService;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for audit analytics endpoints.
 *
 * <p>All endpoints are tenant-scoped. The tenant identifier is resolved from the
 * JWT Bearer token on every request. A user belonging to tenant A cannot observe
 * aggregates for tenant B regardless of the query parameters supplied.
 *
 * <p>All endpoints accept an optional {@code range} parameter:
 * <ul>
 *   <li>Omitted -- windowed behaviour; defaults to last 30 days unless
 *       {@code dateFrom} / {@code dateTo} are supplied.</li>
 *   <li>{@code range=all} -- disables date filtering entirely and returns
 *       data across the full tenant history. Cannot be combined with
 *       {@code dateFrom} or {@code dateTo}.</li>
 * </ul>
 *
 * <p>Base path: {@code /api/v1/audit-logs/analytics}
 *
 * @author aidentx
 * @since 1.0
 */
@RestController
@RequestMapping(AnalyticsKpisConstants.API_AUDIT_LOGS_ANALYTICS)
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AnalyticsKpisService analyticsKpisService;
    private final AnalyticsEventVolumeService analyticsEventVolumeService;
    private final AnalyticsEventDistributionService analyticsEventDistributionService;
    private final AnalyticsTopActorsService analyticsTopActorsService;
    private final AnalyticsHeatmapService analyticsHeatmapService;
    private final JwtUtil jwtUtil;

    public AnalyticsController(
            AnalyticsKpisService analyticsKpisService,
            AnalyticsEventVolumeService analyticsEventVolumeService,
            AnalyticsEventDistributionService analyticsEventDistributionService,
            AnalyticsTopActorsService analyticsTopActorsService,
            AnalyticsHeatmapService analyticsHeatmapService,
            JwtUtil jwtUtil) {
        this.analyticsKpisService = analyticsKpisService;
        this.analyticsEventVolumeService = analyticsEventVolumeService;
        this.analyticsEventDistributionService = analyticsEventDistributionService;
        this.analyticsTopActorsService = analyticsTopActorsService;
        this.analyticsHeatmapService = analyticsHeatmapService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Returns the three core KPI values for the analytics dashboard.
     *
     * <pre>
     *   GET /api/v1/audit-logs/analytics/kpis
     *   GET /api/v1/audit-logs/analytics/kpis?range=all
     *   GET /api/v1/audit-logs/analytics/kpis?dateFrom=2025-02-16T00:00:00Z&dateTo=2025-03-18T23:59:59Z
     * </pre>
     *
     * @param dateFrom optional window start, inclusive (ISO-8601 UTC)
     * @param dateTo   optional window end, inclusive (ISO-8601 UTC)
     * @param range    optional; pass "all" to disable date filtering
     * @param request  incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with {@link AnalyticsKpisResponse}
     */
    @GetMapping(AnalyticsKpisConstants.API_ANALYTICS_KPIS)
    public ResponseEntity<AnalyticsKpisResponse> getKpis(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(value = AnalyticsRangeConstants.RANGE_PARAM, required = false)
            String range,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info(
                AnalyticsKpisConstants.LOG_KPI_REQUEST_RECEIVED,
                AnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId);

        AnalyticsRequestValidator.validateTenantId(tenantId);
        AnalyticsRequestValidator.validateRange(range, dateFrom, dateTo);

        boolean rangeAll = isRangeAll(range);

        if (!rangeAll) {
            AnalyticsRequestValidator.validateDateRange(dateFrom, dateTo);
        }

        AnalyticsKpisResponse response =
                analyticsKpisService.computeKpis(tenantId, dateFrom, dateTo, rangeAll);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the daily event volume broken down by risk level.
     *
     * <pre>
     *   GET /api/v1/audit-logs/analytics/event-volume
     *   GET /api/v1/audit-logs/analytics/event-volume?range=all
     *   GET /api/v1/audit-logs/analytics/event-volume?dateFrom=2025-02-16T00:00:00Z&dateTo=2025-03-18T23:59:59Z
     * </pre>
     *
     * @param dateFrom optional window start, inclusive (ISO-8601 UTC)
     * @param dateTo   optional window end, inclusive (ISO-8601 UTC)
     * @param range    optional; pass "all" to disable date filtering
     * @param request  incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with ordered list of {@link EventVolumeEntry}
     */
    @GetMapping(AnalyticsEventVolumeConstants.EVENT_VOLUME_PATH)
    public ResponseEntity<List<EventVolumeEntry>> getEventVolume(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(value = AnalyticsRangeConstants.RANGE_PARAM, required = false)
            String range,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info(
                AnalyticsEventVolumeConstants.LOG_REQUEST_RECEIVED,
                AnalyticsEventVolumeConstants.LOG_KEY_TENANT_ID, tenantId);

        AnalyticsRequestValidator.validateTenantId(tenantId);
        AnalyticsRequestValidator.validateRange(range, dateFrom, dateTo);

        boolean rangeAll = isRangeAll(range);

        if (!rangeAll) {
            AnalyticsRequestValidator.validateDateRange(dateFrom, dateTo);
        }

        List<EventVolumeEntry> response =
                analyticsEventVolumeService.computeEventVolume(
                        tenantId, dateFrom, dateTo, rangeAll);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the event type distribution ranked by occurrence count.
     *
     * <pre>
     *   GET /api/v1/audit-logs/analytics/event-distribution
     *   GET /api/v1/audit-logs/analytics/event-distribution?range=all
     *   GET /api/v1/audit-logs/analytics/event-distribution?limit=5
     *   GET /api/v1/audit-logs/analytics/event-distribution?range=all&limit=5
     * </pre>
     *
     * @param dateFrom optional window start, inclusive (ISO-8601 UTC)
     * @param dateTo   optional window end, inclusive (ISO-8601 UTC)
     * @param range    optional; pass "all" to disable date filtering
     * @param limit    optional result cap; null = top 10, 0 = all, positive integer = top N
     * @param request  incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with ordered list of {@link EventDistributionEntry}
     */
    @GetMapping(AnalyticsEventDistributionConstants.EVENT_DISTRIBUTION_PATH)
    public ResponseEntity<List<EventDistributionEntry>> getEventDistribution(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(value = AnalyticsRangeConstants.RANGE_PARAM, required = false)
            String range,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info(
                AnalyticsEventDistributionConstants.LOG_REQUEST_RECEIVED,
                AnalyticsEventDistributionConstants.LOG_KEY_TENANT_ID, tenantId);

        AnalyticsRequestValidator.validateTenantId(tenantId);
        AnalyticsRequestValidator.validateRange(range, dateFrom, dateTo);
        AnalyticsRequestValidator.validateLimit(limit);

        boolean rangeAll = isRangeAll(range);

        if (!rangeAll) {
            AnalyticsRequestValidator.validateDateRange(dateFrom, dateTo);
        }

        List<EventDistributionEntry> response =
                analyticsEventDistributionService.computeEventDistribution(
                        tenantId, dateFrom, dateTo, limit, rangeAll);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns the top actors ranked by total event count.
     *
     * <pre>
     *   GET /api/v1/audit-logs/analytics/top-actors
     *   GET /api/v1/audit-logs/analytics/top-actors?range=all
     *   GET /api/v1/audit-logs/analytics/top-actors?limit=5
     *   GET /api/v1/audit-logs/analytics/top-actors?range=all&limit=0
     * </pre>
     *
     * @param dateFrom optional window start, inclusive (ISO-8601 UTC)
     * @param dateTo   optional window end, inclusive (ISO-8601 UTC)
     * @param range    optional; pass "all" to disable date filtering
     * @param limit    optional result cap; null = top 10, 0 = all, positive integer = top N
     * @param request  incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with ordered list of {@link TopActorEntry}
     */
    @GetMapping(AnalyticsTopActorsConstants.TOP_ACTORS_PATH)
    public ResponseEntity<List<TopActorEntry>> getTopActors(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(value = AnalyticsRangeConstants.RANGE_PARAM, required = false)
            String range,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info(
                AnalyticsTopActorsConstants.LOG_REQUEST_RECEIVED,
                AnalyticsTopActorsConstants.LOG_KEY_TENANT_ID, tenantId);

        AnalyticsRequestValidator.validateTenantId(tenantId);
        AnalyticsRequestValidator.validateRange(range, dateFrom, dateTo);
        AnalyticsRequestValidator.validateLimit(limit);

        boolean rangeAll = isRangeAll(range);

        if (!rangeAll) {
            AnalyticsRequestValidator.validateDateRange(dateFrom, dateTo);
        }

        List<TopActorEntry> response =
                analyticsTopActorsService.computeTopActors(
                        tenantId, dateFrom, dateTo, limit, rangeAll);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns true when the caller supplied {@code range=all} (case-insensitive).
     *
     * <p>This check is performed after {@link AnalyticsRequestValidator#validateRange}
     * has already confirmed the value is either null or a recognised range token,
     * so no additional null guard is needed here.
     *
     * @param range value of the range query parameter; may be null
     * @return true when range equals "all" ignoring case
     */
    private boolean isRangeAll(String range) {
        return AnalyticsRangeConstants.RANGE_ALL.equalsIgnoreCase(range);
    }

    /**
     * Extracts the tenant ID from the JWT Bearer token in the Authorization header.
     *
     * @param request incoming HTTP request
     * @return tenant ID extracted from the JWT claims
     * @throws RuntimeException when the Authorization header is absent or not Bearer-prefixed
     */
    private String extractTenantFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new RuntimeException("Authorization header is missing or invalid");
        }

        String token = authHeader.substring(BEARER_PREFIX_LENGTH);
        return jwtUtil.extractTenantId(token);
    }

    /**
     * Returns the change impact heatmap as a list of non-zero
     * (objectType, eventName, count) cells ordered by count descending.
     *
     * <p>Each entry represents one active cell in the matrix. Zero cells are
     * absent from the response -- the frontend fills them when rendering.
     *
     * <p>The {@code limit} parameter caps the number of (objectType, eventName)
     * pairs returned:
     * <ul>
     *   <li>Omitted -- top 50 pairs (default)</li>
     *   <li>Positive integer (e.g. 10, 100) -- that many pairs</li>
     *   <li>0 -- all distinct pairs</li>
     * </ul>
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/audit-logs/analytics/heatmap
     *       -- top 50 cells, last 30 days
     *
     *   GET /api/v1/audit-logs/analytics/heatmap?range=all
     *       -- top 50 cells, full history
     *
     *   GET /api/v1/audit-logs/analytics/heatmap?range=all&limit=0
     *       -- all cells, full history
     *
     *   GET /api/v1/audit-logs/analytics/heatmap
     *       ?dateFrom=2025-01-01T00:00:00Z
     *       &dateTo=2025-01-31T23:59:59Z
     *       &limit=10
     *       -- top 10 cells in January 2025
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * [
     *   { "objectType": "Role",       "eventName": "assign",    "count": 142 },
     *   { "objectType": "User",       "eventName": "assign",    "count": 97  },
     *   { "objectType": "Role",       "eventName": "provision", "count": 38  },
     *   { "objectType": "Permission", "eventName": "approve",   "count": 21  }
     * ]
     * </pre>
     *
     * @param dateFrom optional window start, inclusive (ISO-8601 UTC); defaults to 30 days ago
     * @param dateTo   optional window end, inclusive (ISO-8601 UTC); defaults to now
     * @param range    optional; pass "all" to disable date filtering
     * @param limit    optional result cap; null = top 50, 0 = all, positive integer = top N
     * @param request  incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with list of {@link HeatmapEntry} ordered by count descending
     */
    @GetMapping(AnalyticsHeatmapConstants.HEATMAP_PATH)
    public ResponseEntity<List<HeatmapEntry>> getHeatmap(
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(value = AnalyticsRangeConstants.RANGE_PARAM, required = false)
            String range,
            @RequestParam(value = "limit", required = false) Integer limit,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info(
                AnalyticsHeatmapConstants.LOG_REQUEST_RECEIVED,
                AnalyticsHeatmapConstants.LOG_KEY_TENANT_ID, tenantId);

        AnalyticsRequestValidator.validateTenantId(tenantId);
        AnalyticsRequestValidator.validateRange(range, dateFrom, dateTo);
        AnalyticsRequestValidator.validateLimit(limit);

        boolean rangeAll = isRangeAll(range);

        if (!rangeAll) {
            AnalyticsRequestValidator.validateDateRange(dateFrom, dateTo);
        }

        List<HeatmapEntry> response =
                analyticsHeatmapService.computeHeatmap(
                        tenantId, dateFrom, dateTo, limit, rangeAll);

        return ResponseEntity.ok(response);
    }
}