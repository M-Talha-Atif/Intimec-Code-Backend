package com.aidentx.core.log_management.controller;

import java.util.ArrayList;
import java.util.List;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsCorrelationConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorCategoryConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsHeatmapConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsKpiDrilldownConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsKpisConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsMttrTrendConstants;
import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsRecurringPatternsConstants;
import com.aidentx.core.log_management.model.dto.analytics.CorrelationChain;
import com.aidentx.core.log_management.model.dto.analytics.ErrorCategoryEntriesResponse;
import com.aidentx.core.log_management.model.dto.analytics.ErrorCategoryEntry;
import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeEntriesResponse;
import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeResponse;
import com.aidentx.core.log_management.model.dto.analytics.HeatmapResponse;
import com.aidentx.core.log_management.model.dto.analytics.KpiDrilldownResponse;
import com.aidentx.core.log_management.model.dto.analytics.LogAnalyticsKpisResponse;
import com.aidentx.core.log_management.model.dto.analytics.MttrTrendEntry;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternEntriesResponse;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternEntry;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternsResponse;
import com.aidentx.core.log_management.service.LogAnalyticsCorrelationService;
import com.aidentx.core.log_management.service.LogAnalyticsErrorCategoryEntriesService;
import com.aidentx.core.log_management.service.LogAnalyticsErrorCategoryService;
import com.aidentx.core.log_management.service.LogAnalyticsErrorVolumeEntriesService;
import com.aidentx.core.log_management.service.LogAnalyticsErrorVolumeService;
import com.aidentx.core.log_management.service.LogAnalyticsHeatmapService;
import com.aidentx.core.log_management.service.LogAnalyticsKpiDrilldownService;
import com.aidentx.core.log_management.service.LogAnalyticsKpisService;
import com.aidentx.core.log_management.service.LogAnalyticsMttrTrendService;
import com.aidentx.core.log_management.service.LogAnalyticsRecurringPatternEntriesService;
import com.aidentx.core.log_management.service.LogAnalyticsRecurringPatternsService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for log analytics endpoints.
 *
 * <p>All endpoints query the main-logs OpenSearch index and are scoped to the
 * caller's tenant. The tenant identifier is extracted from the JWT Bearer token
 * on every request, consistent with the pattern in {@link LogLevelController}
 * and {@link LogsFilteringController}.
 *
 * <p>Base path: {@code /api/v1/logs/analytics}
 *
 * @author aidentx
 * @since 1.0
 */
@RestController
@RequestMapping(LogAnalyticsKpisConstants.API_LOG_ANALYTICS_BASE)
public class LogAnalyticsController {

    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final LogAnalyticsKpisService logAnalyticsKpisService;
    private final LogAnalyticsErrorVolumeService logAnalyticsErrorVolumeService;
    private final LogAnalyticsErrorCategoryService logAnalyticsErrorCategoryService;
    private final LogAnalyticsHeatmapService logAnalyticsHeatmapService;
    private final LogAnalyticsCorrelationService logAnalyticsCorrelationService;
    private final LogAnalyticsKpiDrilldownService logAnalyticsKpiDrilldownService;
    private final LogAnalyticsErrorVolumeEntriesService logAnalyticsErrorVolumeEntriesService;
    private final LogAnalyticsErrorCategoryEntriesService logAnalyticsErrorCategoryEntriesService;
    private final LogAnalyticsRecurringPatternsService logAnalyticsRecurringPatternsService;
    private final LogAnalyticsRecurringPatternEntriesService logAnalyticsRecurringPatternEntriesService;
    private final LogAnalyticsMttrTrendService logAnalyticsMttrTrendService;


    private final IUnifiedLogger logger;


    // Constructor
    public LogAnalyticsController(
            LogAnalyticsKpisService logAnalyticsKpisService,
            LogAnalyticsErrorVolumeService logAnalyticsErrorVolumeService,
            LogAnalyticsErrorCategoryService logAnalyticsErrorCategoryService,
            LogAnalyticsHeatmapService logAnalyticsHeatmapService,
            LogAnalyticsCorrelationService logAnalyticsCorrelationService,
            LogAnalyticsKpiDrilldownService logAnalyticsKpiDrilldownService,
            LogAnalyticsErrorVolumeEntriesService logAnalyticsErrorVolumeEntriesService,
            LogAnalyticsErrorCategoryEntriesService logAnalyticsErrorCategoryEntriesService,
            LogAnalyticsRecurringPatternsService logAnalyticsRecurringPatternsService,
            LogAnalyticsRecurringPatternEntriesService logAnalyticsRecurringPatternEntriesService,
            LogAnalyticsMttrTrendService logAnalyticsMttrTrendService,
            UnifiedLoggerFactory loggerFactory) {
        this.logAnalyticsKpisService = logAnalyticsKpisService;
        this.logAnalyticsErrorVolumeService = logAnalyticsErrorVolumeService;
        this.logAnalyticsErrorCategoryService = logAnalyticsErrorCategoryService;
        this.logAnalyticsHeatmapService = logAnalyticsHeatmapService;
        this.logAnalyticsCorrelationService = logAnalyticsCorrelationService;
        this.logAnalyticsKpiDrilldownService = logAnalyticsKpiDrilldownService;
        this.logAnalyticsErrorVolumeEntriesService = logAnalyticsErrorVolumeEntriesService;
        this.logAnalyticsErrorCategoryEntriesService = logAnalyticsErrorCategoryEntriesService;
        this.logAnalyticsRecurringPatternsService = logAnalyticsRecurringPatternsService;
        this.logAnalyticsRecurringPatternEntriesService = logAnalyticsRecurringPatternEntriesService;
        this.logAnalyticsMttrTrendService = logAnalyticsMttrTrendService;
        this.logger = loggerFactory.getLogger("LogAnalyticsController", "log-analytics");
    }

    /**
     * Returns the five core KPI values for the log analytics dashboard.
     *
     * <p>All five metrics are derived from a single OpenSearch aggregation
     * request, so response time is bounded by one OpenSearch round-trip.
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/logs/analytics/kpis
     *       -- all KPIs with 24h window for totalErrors
     *
     *   GET /api/v1/logs/analytics/kpis?range=all
     *       -- all KPIs across the full log history, no date filter applied
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * {
     *   "totalErrors24h": 247,
     *   "criticalFailures": 18,
     *   "avgResponseTimeMs": 2800.0,
     *   "mttrMinutes": 32.0,
     *   "recurringPatterns": 5
     * }
     * </pre>
     *
     * @param range   optional; pass "all" to disable all date filtering
     * @param request incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with {@link LogAnalyticsKpisResponse}
     */
    @GetMapping(LogAnalyticsKpisConstants.API_LOG_ANALYTICS_KPIS)
    public ResponseEntity<LogAnalyticsKpisResponse> getKpis(
            @RequestParam(value = LogAnalyticsKpisConstants.RANGE_PARAM, required = false) String range,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info().message(LogAnalyticsKpisConstants.LOG_KPI_REQUEST_RECEIVED)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_RANGE, range != null ? range : "default")
                .log();

        validateTenantId(tenantId);
        validateRange(range);

        // If range is null, use "24h" as default (matches original behavior)
        String effectiveRange = range != null ? range : LogAnalyticsKpisConstants.RANGE_24H;

        LogAnalyticsKpisResponse response =
                logAnalyticsKpisService.computeKpis(tenantId, effectiveRange);

        return ResponseEntity.ok(response);
    }

    private void validateRange(String range) {
        if (range == null) {
            return; // null is valid, will use default
        }

        boolean isValid = LogAnalyticsKpisConstants.RANGE_24H.equals(range) ||
                LogAnalyticsKpisConstants.RANGE_30D.equals(range) ||
                LogAnalyticsKpisConstants.RANGE_60D.equals(range) ||
                LogAnalyticsKpisConstants.RANGE_90D.equals(range) ||
                LogAnalyticsKpisConstants.RANGE_ALL.equals(range);

        if (!isValid) {
            throw new IllegalArgumentException(LogAnalyticsKpisConstants.VALIDATION_RANGE_UNKNOWN);
        }
    }

    /**
     * Validates that the tenant identifier is present and non-blank.
     *
     * @param tenantId value extracted from the JWT; may be null
     * @throws IllegalArgumentException when tenantId is null or blank
     */
    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                    LogAnalyticsKpisConstants.VALIDATION_TENANT_ID_BLANK);
        }
    }


    /**
     * Returns the error volume timeline broken down by top-N sources.
     *
     * <p>Each entry in the timeline represents one clock hour. The response
     * includes a {@code sources} array telling the frontend which dynamic keys
     * to render as Area components.
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/logs/analytics/error-volume
     *       -- last 24 hours, top 5 sources (both defaults)
     *
     *   GET /api/v1/logs/analytics/error-volume?hours=48&limit=10
     *       -- last 48 hours, top 10 sources
     *
     *   GET /api/v1/logs/analytics/error-volume?range=all&limit=5
     *       -- full history, top 5 sources, bucketed by hour-of-day
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * {
     *   "hours": 24,
     *   "limit": 5,
     *   "sources": ["JwtAuthenticationFilter", "GatewayPermissionFilter"],
     *   "timeline": [
     *     { "hour": "00:00", "JwtAuthenticationFilter": 0, "GatewayPermissionFilter": 2 },
     *     { "hour": "01:00", "JwtAuthenticationFilter": 3, "GatewayPermissionFilter": 0 },
     *     ...
     *   ]
     * }
     * </pre>
     *
     * @param hours   optional look-back window in hours; default 24, max 168
     * @param limit   optional number of top sources; default 5, max 20
     * @param range   optional; pass "all" to cover full log history
     * @param request incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with {@link ErrorVolumeResponse}
     */
    @GetMapping(LogAnalyticsErrorVolumeConstants.API_ERROR_VOLUME)
    public ResponseEntity<ErrorVolumeResponse> getErrorVolume(
            @RequestParam(value = "hours", required = false) Integer hours,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "range", required = false) String range,
            @RequestParam(value = "timezone", required = false) String timezone,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info().message(LogAnalyticsErrorVolumeConstants.LOG_REQUEST_RECEIVED)
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_TENANT_ID, tenantId)
                .log();

        validateRange(range);

        int resolvedHours = resolveHours(hours);
        int resolvedLimit = resolveSourceLimit(limit);
        boolean rangeAll = LogAnalyticsErrorVolumeConstants.RANGE_ALL.equalsIgnoreCase(range);

        // timezone null/blank defaults to UTC inside the service
        ErrorVolumeResponse response =
                logAnalyticsErrorVolumeService.computeErrorVolume(
                        resolvedHours, resolvedLimit, rangeAll, timezone);

        return ResponseEntity.ok(response);
    }

    /**
     * Resolves the hours parameter to its effective value.
     * Null defaults to DEFAULT_HOURS. Out-of-range values are clamped.
     */
    private int resolveHours(Integer hours) {
        if (hours == null) {
            return LogAnalyticsErrorVolumeConstants.DEFAULT_HOURS;
        }
        if (hours < LogAnalyticsErrorVolumeConstants.MINIMUM_HOURS) {
            throw new IllegalArgumentException(
                    LogAnalyticsErrorVolumeConstants.VALIDATION_HOURS_OUT_OF_RANGE);
        }
        if (hours > LogAnalyticsErrorVolumeConstants.MAXIMUM_HOURS) {
            throw new IllegalArgumentException(
                    LogAnalyticsErrorVolumeConstants.VALIDATION_HOURS_OUT_OF_RANGE);
        }
        return hours;
    }

    /**
     * Resolves the limit parameter to its effective value.
     * Null defaults to DEFAULT_SOURCE_LIMIT. Out-of-range values are rejected.
     */
    private int resolveSourceLimit(Integer limit) {
        if (limit == null) {
            return LogAnalyticsErrorVolumeConstants.DEFAULT_SOURCE_LIMIT;
        }
        if (limit < LogAnalyticsErrorVolumeConstants.MINIMUM_SOURCE_LIMIT
                || limit > LogAnalyticsErrorVolumeConstants.MAXIMUM_SOURCE_LIMIT) {
            throw new IllegalArgumentException(
                    LogAnalyticsErrorVolumeConstants.VALIDATION_LIMIT_OUT_OF_RANGE);
        }
        return limit;
    }


    /**
     * Returns the error category distribution ranked by event count descending.
     *
     * <p>Categories are the actual values stored in the {@code category} field
     * of log documents. No hardcoded category mapping is applied -- the top-N
     * real categories from the index are returned directly.
     *
     * <p>Default behaviour: {@code range=all} (full history), top 10 categories.
     * Supply {@code hours} to narrow to a time window.
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/logs/analytics/error-category-distribution
     *       -- top 10 categories, full history (both defaults)
     *
     *   GET /api/v1/logs/analytics/error-category-distribution?limit=6
     *       -- top 6 categories, full history
     *
     *   GET /api/v1/logs/analytics/error-category-distribution?hours=24&limit=10
     *       -- top 10 categories in the last 24 hours
     *
     *   GET /api/v1/logs/analytics/error-category-distribution?hours=48
     *       -- top 10 categories in the last 48 hours
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * [
     *   { "category": "Security-Auth",            "count": 312 },
     *   { "category": "SECURITY",                 "count": 198 },
     *   { "category": "NOTIFICATIONS_MANAGEMENT", "count": 87  },
     *   { "category": "PROVISIONING",             "count": 54  }
     * ]
     * </pre>
     *
     * @param limit   optional cap on number of categories; default 10, max 50
     * @param hours   optional look-back window in hours; when omitted, range=all applies
     * @param range   optional; pass "all" explicitly to cover full history
     * @param request incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with ordered list of {@link ErrorCategoryEntry}
     */
    @GetMapping(LogAnalyticsErrorCategoryConstants.API_ERROR_CATEGORY)
    public ResponseEntity<List<ErrorCategoryEntry>> getErrorCategoryDistribution(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "hours", required = false) Integer hours,
            @RequestParam(value = "range", required = false) String range,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info().message(LogAnalyticsErrorCategoryConstants.LOG_REQUEST_RECEIVED)
                .detail(LogAnalyticsErrorCategoryConstants.LOG_KEY_LIMIT, limit)
                .log();

        validateCategoryRange(range);

        int resolvedLimit = resolveCategoryLimit(limit);
        int resolvedHours = resolveCategoryHours(hours);

        // rangeAll is true when caller passes range=all OR omits both range and hours.
        // Supplying hours without range implies a windowed query (rangeAll=false).
        boolean rangeAll = resolveRangeAll(range, hours);

        List<ErrorCategoryEntry> response =
                logAnalyticsErrorCategoryService.computeErrorCategoryDistribution(
                        resolvedLimit, resolvedHours, rangeAll);

        return ResponseEntity.ok(response);
    }

    /**
     * Determines whether the query should cover full history or a time window.
     *
     * <p>Logic:
     * <ul>
     *   <li>range=all and hours omitted -- rangeAll=true (full history)</li>
     *   <li>range=all and hours supplied -- rangeAll=true (range=all wins)</li>
     *   <li>range omitted and hours omitted -- rangeAll=true (default)</li>
     *   <li>range omitted and hours supplied -- rangeAll=false (time window)</li>
     * </ul>
     *
     * <p>Example: {@code ?hours=24} -- rangeAll=false, window of 24 hours.
     * Example: {@code ?range=all&hours=24} -- rangeAll=true, hours ignored.
     * Example: no params -- rangeAll=true, full history.
     *
     * @param range caller-supplied range parameter; may be null
     * @param hours caller-supplied hours parameter; may be null
     * @return true when full history should be covered
     */
    private boolean resolveRangeAll(String range, Integer hours) {
        if (LogAnalyticsErrorCategoryConstants.RANGE_ALL.equalsIgnoreCase(range)) {
            return true;
        }
        // No range param and no hours param -- default to full history
        if (range == null && hours == null) {
            return true;
        }
        // hours supplied without range=all -- apply time window
        return false;
    }

    /**
     * Resolves and validates the limit parameter.
     *
     * @param limit caller-supplied limit; may be null
     * @return DEFAULT_LIMIT when null; supplied value when in range
     * @throws IllegalArgumentException when outside allowed range
     */
    private int resolveCategoryLimit(Integer limit) {
        if (limit == null) {
            return LogAnalyticsErrorCategoryConstants.DEFAULT_LIMIT;
        }
        if (limit < LogAnalyticsErrorCategoryConstants.MINIMUM_LIMIT
                || limit > LogAnalyticsErrorCategoryConstants.MAXIMUM_LIMIT) {
            throw new IllegalArgumentException(
                    LogAnalyticsErrorCategoryConstants.VALIDATION_LIMIT_OUT_OF_RANGE);
        }
        return limit;
    }

    /**
     * Resolves and validates the hours parameter.
     * Returns DEFAULT_LIMIT as a safe default when null since hours is only
     * used when rangeAll is false -- the value is irrelevant when rangeAll=true.
     *
     * @param hours caller-supplied hours; may be null
     * @return 24 when null; supplied value when in range
     * @throws IllegalArgumentException when outside allowed range
     */
    private int resolveCategoryHours(Integer hours) {
        if (hours == null) {
            return 24;
        }
        if (hours < LogAnalyticsErrorCategoryConstants.MINIMUM_HOURS
                || hours > LogAnalyticsErrorCategoryConstants.MAXIMUM_HOURS) {
            throw new IllegalArgumentException(
                    LogAnalyticsErrorCategoryConstants.VALIDATION_HOURS_OUT_OF_RANGE);
        }
        return hours;
    }

    /**
     * Validates the range parameter for the category endpoint.
     * Accepts null or "all". Any other value is rejected.
     *
     * @param range caller-supplied range value; may be null
     * @throws IllegalArgumentException when range is an unrecognised value
     */
    private void validateCategoryRange(String range) {
        if (range == null) {
            return;
        }
        if (!range.equalsIgnoreCase(LogAnalyticsErrorCategoryConstants.RANGE_ALL)) {
            throw new IllegalArgumentException(
                    LogAnalyticsErrorCategoryConstants.VALIDATION_RANGE_UNKNOWN);
        }
    }

    /**
     * Returns the hour x source heatmap for log events.
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/logs/analytics/heatmap
     *       -- top 10 sources, last 24 hours, UTC labels
     *
     *   GET /api/v1/logs/analytics/heatmap?hours=48&limit=5&timezone=Asia/Karachi
     *       -- top 5 sources, last 48 hours, PKT labels
     *
     *   GET /api/v1/logs/analytics/heatmap?range=all&limit=10&timezone=Europe/Zurich
     *       -- top 10 sources, full history, Zurich labels
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * {
     *   "hours": 24,
     *   "limit": 2,
     *   "sources": ["GatewayPermissionFilter", "JwtUtil"],
     *   "data": [
     *     { "source": "GatewayPermissionFilter", "hour": 15, "count": 49 },
     *     { "source": "GatewayPermissionFilter", "hour": 17, "count": 68 },
     *     { "source": "JwtUtil",                 "hour": 17, "count": 12 }
     *   ]
     * }
     * </pre>
     *
     * @param hours    optional look-back window in hours; default 24, max 168
     * @param limit    optional top-N source rows; default 10, max 20
     * @param range    optional; pass "all" to cover full history
     * @param timezone optional IANA timezone for hour integers; default UTC
     * @param request  incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with {@link HeatmapResponse}
     */
    @GetMapping(LogAnalyticsHeatmapConstants.API_HEATMAP)
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @RequestParam(value = "hours", required = false) Integer hours,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "range", required = false) String range,
            @RequestParam(value = "timezone", required = false) String timezone,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info().message(LogAnalyticsHeatmapConstants.LOG_REQUEST_RECEIVED)
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_TIMEZONE,
                        timezone != null ? timezone : "UTC")
                .log();

        validateHeatmapRange(range);

        int resolvedHours = resolveHeatmapHours(hours);
        int resolvedLimit = resolveHeatmapLimit(limit);
        boolean rangeAll = LogAnalyticsHeatmapConstants.RANGE_ALL.equalsIgnoreCase(range);

        HeatmapResponse response =
                logAnalyticsHeatmapService.computeHeatmap(
                        resolvedHours, resolvedLimit, rangeAll, timezone);

        return ResponseEntity.ok(response);
    }

    private int resolveHeatmapHours(Integer hours) {
        if (hours == null) return LogAnalyticsHeatmapConstants.DEFAULT_HOURS;
        if (hours < LogAnalyticsHeatmapConstants.MINIMUM_HOURS
                || hours > LogAnalyticsHeatmapConstants.MAXIMUM_HOURS) {
            throw new IllegalArgumentException(
                    LogAnalyticsHeatmapConstants.VALIDATION_HOURS_OUT_OF_RANGE);
        }
        return hours;
    }

    private int resolveHeatmapLimit(Integer limit) {
        if (limit == null) return LogAnalyticsHeatmapConstants.DEFAULT_SOURCE_LIMIT;
        if (limit < LogAnalyticsHeatmapConstants.MINIMUM_SOURCE_LIMIT
                || limit > LogAnalyticsHeatmapConstants.MAXIMUM_SOURCE_LIMIT) {
            throw new IllegalArgumentException(
                    LogAnalyticsHeatmapConstants.VALIDATION_LIMIT_OUT_OF_RANGE);
        }
        return limit;
    }

    private void validateHeatmapRange(String range) {
        if (range == null) return;
        if (!range.equalsIgnoreCase(LogAnalyticsHeatmapConstants.RANGE_ALL)) {
            throw new IllegalArgumentException(
                    LogAnalyticsHeatmapConstants.VALIDATION_RANGE_UNKNOWN);
        }
    }

    /**
     * Returns the paginated log entries that were counted for a specific KPI card.
     *
     * <p>Clicking a KPI card on the frontend calls this endpoint with the
     * matching {@code type} so the user can inspect exactly which log entries
     * contributed to the number shown.
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/logs/analytics/kpi-drilldown?type=total_errors&page=0&size=20
     *   GET /api/v1/logs/analytics/kpi-drilldown?type=critical&page=0&size=20
     *   GET /api/v1/logs/analytics/kpi-drilldown?type=slow_responses&page=0&size=20
     *   GET /api/v1/logs/analytics/kpi-drilldown?type=recurring&page=0&size=20
     * </pre>
     *
     * <p>Example response (type=critical, 2 results):
     * <pre>
     * {
     *   "type": "critical",
     *   "logs": [
     *     { "id": "abc", "level": "ERROR", "source": "JwtUtil", "message": "...", ... }
     *   ],
     *   "pagination": {
     *     "page": 0, "size": 20, "totalElements": 198, "totalPages": 10,
     *     "hasNext": true, "hasPrevious": false
     *   }
     * }
     * </pre>
     *
     * @param type  the drilldown type; one of total_errors, critical, slow_responses, recurring
     * @param page  zero-based page number; default 0
     * @param size  entries per page; default 20, max 100
     * @param range optional; pass "all" to cover full history
     * @return HTTP 200 with {@link KpiDrilldownResponse}
     */
    @GetMapping(LogAnalyticsKpiDrilldownConstants.API_KPI_DRILLDOWN)
    public ResponseEntity<KpiDrilldownResponse> getKpiDrilldown(
            @RequestParam(value = "type") String type,
            @RequestParam(value = "page",
                    defaultValue = "" + LogAnalyticsKpiDrilldownConstants.DEFAULT_PAGE) int page,
            @RequestParam(value = "size",
                    defaultValue = "" + LogAnalyticsKpiDrilldownConstants.DEFAULT_SIZE) int size,
            @RequestParam(value = "range", required = false) String range,
            HttpServletRequest request) {

        validateKpiDrilldownParams(type, page, size, range);

        KpiDrilldownResponse response =
                logAnalyticsKpiDrilldownService.fetchDrilldown(
                        type, page, size, range);

        return ResponseEntity.ok(response);
    }

    private void validateKpiDrilldownParams(String type, int page, int size, String range) {
        // Validate type
        boolean validType = LogAnalyticsKpiDrilldownConstants.TYPE_TOTAL_ERRORS.equals(type) ||
                LogAnalyticsKpiDrilldownConstants.TYPE_CRITICAL.equals(type) ||
                LogAnalyticsKpiDrilldownConstants.TYPE_SLOW_RESPONSES.equals(type) ||
                LogAnalyticsKpiDrilldownConstants.TYPE_RECURRING.equals(type);
        if (!validType) {
            throw new IllegalArgumentException(LogAnalyticsKpiDrilldownConstants.VALIDATION_TYPE_REQUIRED);
        }

        // Validate page
        if (page < 0) {
            throw new IllegalArgumentException(LogAnalyticsKpiDrilldownConstants.VALIDATION_PAGE_NEGATIVE);
        }

        // Validate size
        if (size < LogAnalyticsKpiDrilldownConstants.MINIMUM_SIZE ||
                size > LogAnalyticsKpiDrilldownConstants.MAXIMUM_SIZE) {
            throw new IllegalArgumentException(LogAnalyticsKpiDrilldownConstants.VALIDATION_SIZE_OUT_OF_RANGE);
        }

        // Validate range if provided
        if (range != null) {
            boolean validRange = LogAnalyticsKpiDrilldownConstants.RANGE_24H.equals(range) ||
                    LogAnalyticsKpiDrilldownConstants.RANGE_30D.equals(range) ||
                    LogAnalyticsKpiDrilldownConstants.RANGE_60D.equals(range) ||
                    LogAnalyticsKpiDrilldownConstants.RANGE_90D.equals(range) ||
                    LogAnalyticsKpiDrilldownConstants.RANGE_ALL.equals(range);
            if (!validRange) {
                throw new IllegalArgumentException(LogAnalyticsKpiDrilldownConstants.VALIDATION_RANGE_UNKNOWN);
            }
        }
    }


    /**
     * Validates all three parameters for the KPI drilldown endpoint.
     *
     * @param type the requested drilldown type
     * @param page the requested page number
     * @param size the requested page size
     * @throws IllegalArgumentException when any parameter is invalid
     */
    private void validateKpiDrilldownParams(String type, int page, int size) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    LogAnalyticsKpiDrilldownConstants.VALIDATION_TYPE_REQUIRED);
        }
        if (page < 0) {
            throw new IllegalArgumentException(
                    LogAnalyticsKpiDrilldownConstants.VALIDATION_PAGE_NEGATIVE);
        }
        if (size < LogAnalyticsKpiDrilldownConstants.MINIMUM_SIZE
                || size > LogAnalyticsKpiDrilldownConstants.MAXIMUM_SIZE) {
            throw new IllegalArgumentException(
                    LogAnalyticsKpiDrilldownConstants.VALIDATION_SIZE_OUT_OF_RANGE);
        }
    }

    /**
     * Returns the paginated log entries for a specific hour slot in the error
     * volume timeline.
     *
     * <p>The chart renders hour labels in the caller's local timezone. When a
     * user clicks a bar, the frontend sends the label and timezone back so the
     * backend can resolve the correct UTC time window and fetch matching entries.
     *
     * <p>Common Postman test calls:
     * <pre>
     *   -- All sources at 15:00 PKT in last 24h window:
     *   GET /api/v1/logs/analytics/error-volume-entries
     *       ?hour=15:00
     *       &timezone=Asia/Karachi
     *
     *   -- Specific sources at 15:00 PKT:
     *   GET /api/v1/logs/analytics/error-volume-entries
     *       ?hour=15:00
     *       &timezone=Asia/Karachi
     *       &sources=GatewayPermissionFilter,JwtAuthenticationFilter
     *
     *   -- Different window size:
     *   GET /api/v1/logs/analytics/error-volume-entries
     *       ?hour=15:00
     *       &timezone=Asia/Karachi
     *       &windowHours=48
     *       &page=0
     *       &size=20
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * {
     *   "hour": "15:00",
     *   "timezone": "Asia/Karachi",
     *   "resolvedDateFrom": 1742205600000,
     *   "resolvedDateTo":   1742209199999,
     *   "sources": ["GatewayPermissionFilter"],
     *   "logs": [...],
     *   "pagination": { "page": 0, "size": 20, "totalElements": 9067, ... }
     * }
     * </pre>
     *
     * @param hour        the "HH:00" label as shown on the chart e.g. "15:00"
     * @param timezone    IANA timezone string used to render the chart; default UTC
     * @param sources     optional comma-separated source names; omit for all sources
     * @param windowHours look-back window in hours matching what the chart used; default 24
     * @param page        zero-based page number; default 0
     * @param size        entries per page; default 20
     * @param request     incoming HTTP request for JWT extraction
     * @return HTTP 200 with {@link ErrorVolumeEntriesResponse},
     * HTTP 404 when the hour label cannot be resolved within the window
     */
    @GetMapping("/error-volume-entries")
    public ResponseEntity<ErrorVolumeEntriesResponse> getErrorVolumeEntries(
            @RequestParam(value = "hour") String hour,
            @RequestParam(value = "timezone", defaultValue = "UTC") String timezone,
            @RequestParam(value = "sources", required = false) String sources,
            @RequestParam(value = "windowHours", defaultValue = "24") int windowHours,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {

        validateErrorVolumeEntriesParams(hour, windowHours, page, size);

        // Parse comma-separated sources into a list.
        // Empty string and missing parameter both produce an empty list
        // which means no source filter -- return all sources in that hour.
        List<String> sourceList = (sources != null && !sources.isBlank())
                ? List.of(sources.split(","))
                : new ArrayList<>();

        ErrorVolumeEntriesResponse response =
                logAnalyticsErrorVolumeEntriesService.fetchEntriesForHour(
                        hour.trim(),
                        timezone,
                        sourceList,
                        windowHours,
                        page,
                        size);

        // If epoch bounds could not be resolved the dateFrom will be 0
        // indicating the hour label was not found within the window.
        if (response.getResolvedDateFrom() == 0L && !response.getLogs().isEmpty() == false
                && response.getPagination().getTotalElements() == 0L
                && !response.getHour().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Validates parameters for the error volume entries endpoint.
     *
     * @param hour        must be non-blank in "HH:00" format
     * @param windowHours must be between 1 and 168
     * @param page        must be >= 0
     * @param size        must be between 1 and 100
     */
    private void validateErrorVolumeEntriesParams(
            String hour, int windowHours, int page, int size) {

        if (hour == null || hour.isBlank()) {
            throw new IllegalArgumentException(
                    "hour parameter is required e.g. hour=15:00");
        }
        if (!hour.matches("^\\d{2}:00$")) {
            throw new IllegalArgumentException(
                    "hour must be in HH:00 format e.g. 15:00");
        }
        if (windowHours < 1 || windowHours > 168) {
            throw new IllegalArgumentException(
                    "windowHours must be between 1 and 168");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }


    /**
     * Returns the paginated log entries for a specific category value clicked
     * on the Error Category Distribution bar chart.
     *
     * <p>The category value must be the exact string as stored in the
     * {@code category} field of the log document. The values come directly
     * from what the chart shows -- they are the raw category.keyword values
     * returned by the chart's terms aggregation.
     *
     * <p>Postman test calls:
     * <pre>
     *   -- All time (matches default chart which uses range=all):
     *   GET /api/v1/logs/analytics/error-category-entries
     *       ?category=Security-Auth
     *
     *   -- Last 24 hours only:
     *   GET /api/v1/logs/analytics/error-category-entries
     *       ?category=Security-Auth&hours=24
     *
     *   -- Pagination:
     *   GET /api/v1/logs/analytics/error-category-entries
     *       ?category=SECURITY&page=1&size=20
     *
     *   -- Other category values from the chart:
     *   GET /api/v1/logs/analytics/error-category-entries?category=PERMISSION_CHECK
     *   GET /api/v1/logs/analytics/error-category-entries?category=GRPC_SERVICE
     *   GET /api/v1/logs/analytics/error-category-entries?category=NOTIFICATIONS_MANAGEMENT
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * {
     *   "category": "Security-Auth",
     *   "logs": [
     *     { "id": "abc", "level": "ERROR", "source": "JwtUtil", "message": "...", ... }
     *   ],
     *   "pagination": {
     *     "page": 0, "size": 20, "totalElements": 312,
     *     "totalPages": 16, "hasNext": true, "hasPrevious": false
     *   }
     * }
     * </pre>
     *
     * @param category exact category value from the chart e.g. "Security-Auth"
     * @param hours    optional time window in hours; 0 or omitted means all time
     * @param page     zero-based page number; default 0
     * @param size     entries per page; default 20, max 100
     * @param request  incoming HTTP request for JWT extraction
     * @return HTTP 200 with {@link ErrorCategoryEntriesResponse}
     */
    @GetMapping("/error-category-entries")
    public ResponseEntity<ErrorCategoryEntriesResponse> getErrorCategoryEntries(
            @RequestParam(value = "category") String category,
            @RequestParam(value = "hours", defaultValue = "0") int hours,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {

        validateErrorCategoryEntriesParams(category, hours, page, size);

        ErrorCategoryEntriesResponse response =
                logAnalyticsErrorCategoryEntriesService.fetchEntriesForCategory(
                        category.trim(),
                        hours,
                        page,
                        size);

        return ResponseEntity.ok(response);
    }

    /**
     * Validates parameters for the error category entries endpoint.
     *
     * @param category must be non-blank
     * @param hours    must be between 0 and 168 (0 means no time filter)
     * @param page     must be >= 0
     * @param size     must be between 1 and 100
     */
    private void validateErrorCategoryEntriesParams(
            String category, int hours, int page, int size) {

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "category parameter is required e.g. category=Security-Auth");
        }
        if (hours < 0 || hours > 168) {
            throw new IllegalArgumentException(
                    "hours must be between 0 and 168 (0 means no time filter)");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    /**
     * Returns the recurring message patterns ordered by occurrence count.
     *
     * @param page zero-based page number; default 0
     * @param size entries per page; default 5
     * @param hours optional look-back window; 0 or omitted means all history
     * @return HTTP 200 with list of {@link RecurringPatternEntry}
     */
    @GetMapping(LogAnalyticsRecurringPatternsConstants.API_RECURRING_PATTERNS)
    public ResponseEntity<RecurringPatternsResponse> getRecurringPatterns(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "hours", defaultValue = "" + LogAnalyticsRecurringPatternsConstants.DEFAULT_HOURS) int hours,
            HttpServletRequest request) {

        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > LogAnalyticsRecurringPatternsConstants.MAXIMUM_LIMIT) {
            throw new IllegalArgumentException(
                    LogAnalyticsRecurringPatternsConstants.VALIDATION_LIMIT_OUT_OF_RANGE);
        }
        if (hours < LogAnalyticsRecurringPatternsConstants.MINIMUM_HOURS
                || hours > LogAnalyticsRecurringPatternsConstants.MAXIMUM_HOURS) {
            throw new IllegalArgumentException(
                    LogAnalyticsRecurringPatternsConstants.VALIDATION_HOURS_OUT_OF_RANGE);
        }

        return ResponseEntity.ok(
                logAnalyticsRecurringPatternsService.computeRecurringPatterns(page, size, hours));
    }

    /**
     * Returns paginated log entries for a specific repeated message string.
     *
     * <p>The message parameter must be the exact string from a
     * {@link RecurringPatternEntry} -- it is matched byte-for-byte against
     * message.keyword in OpenSearch. The totalElements in the response will
     * match the occurrences count on the pattern card when hours=0.
     *
     * <p>Postman test calls:
     * <pre>
     *   GET /api/v1/logs/analytics/recurring-pattern-entries
     *       ?message=Permission denied for user
     *
     *   GET /api/v1/logs/analytics/recurring-pattern-entries
     *       ?message=Permission denied for user&hours=24
     *
     *   GET /api/v1/logs/analytics/recurring-pattern-entries
     *       ?message=Permission denied for user&page=1&size=20
     * </pre>
     *
     * @param message exact message string from a RecurringPatternEntry
     * @param hours   optional look-back window; 0 or omitted means all history
     * @param page    zero-based page number; default 0
     * @param size    entries per page; default 20, max 100
     * @return HTTP 200 with {@link RecurringPatternEntriesResponse}
     */
    @GetMapping("/recurring-pattern-entries")
    public ResponseEntity<RecurringPatternEntriesResponse> getRecurringPatternEntries(
            @RequestParam(value = "message") String message,
            @RequestParam(value = "hours", defaultValue = "0") int hours,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpServletRequest request) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "message parameter is required");
        }
        if (hours < 0 || hours > 168) {
            throw new IllegalArgumentException(
                    "hours must be between 0 and 168");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        return ResponseEntity.ok(
                logAnalyticsRecurringPatternEntriesService.fetchEntriesForMessage(
                        message.trim(), hours, page, size));
    }

    /**
     * Returns the 7-day MTTR trend with one data point per calendar day.
     *
     * <p>Only entries that have a resolvedAt field contribute to MTTR.
     * Days with no resolved entries return mttr=0.0.
     *
     * <p>Postman test calls:
     * <pre>
     *   GET /api/v1/logs/analytics/mttr-trend
     *       -- 7 days in UTC (default)
     *
     *   GET /api/v1/logs/analytics/mttr-trend?timezone=Asia/Karachi
     *       -- 7 days with day boundaries aligned to PKT midnight
     *
     *   GET /api/v1/logs/analytics/mttr-trend?timezone=Europe/Zurich
     *       -- 7 days with day boundaries aligned to CET midnight
     * </pre>
     *
     * <p>Example response:
     * <pre>
     * [
     *   { "day": "Thu", "mttr": 45.0 },
     *   { "day": "Fri", "mttr": 38.0 },
     *   { "day": "Sat", "mttr": 0.0  },
     *   { "day": "Sun", "mttr": 52.0 },
     *   { "day": "Mon", "mttr": 33.0 },
     *   { "day": "Tue", "mttr": 28.0 },
     *   { "day": "Wed", "mttr": 25.0 }
     * ]
     * </pre>
     *
     * @param timezone optional IANA timezone string; default UTC
     * @return HTTP 200 with list of 7 {@link MttrTrendEntry} oldest day first
     */
    @GetMapping(LogAnalyticsMttrTrendConstants.API_MTTR_TREND)
    public ResponseEntity<List<MttrTrendEntry>> getMttrTrend(
            @RequestParam(value = "timezone", required = false) String timezone,
            HttpServletRequest request) {

        logger.info().message(LogAnalyticsMttrTrendConstants.LOG_REQUEST_RECEIVED)
                .detail(LogAnalyticsMttrTrendConstants.LOG_KEY_TIMEZONE,
                        timezone != null ? timezone : "UTC")
                .log();

        return ResponseEntity.ok(
                logAnalyticsMttrTrendService.computeMttrTrend(timezone));
    }


    /**
     * Extracts the tenant ID from the JWT Bearer token in the Authorization header.
     *
     * @param request incoming HTTP request
     * @return tenant ID string from the JWT claims
     * @throws RuntimeException when the Authorization header is absent or malformed
     */
    private String extractTenantFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new RuntimeException("Authorization header is missing or invalid");
        }

        String token = authHeader.substring(BEARER_PREFIX_LENGTH);

        // JwtUtil is not available in the log_management module.
        // Extract tenantId from the JWT payload directly using the same
        // claim key used across all other controllers in this module.
        return extractTenantIdFromToken(token);
    }

    /**
     * Decodes the JWT payload and extracts the tenantId claim.
     *
     * <p>This module does not have a direct dependency on JwtUtil from the
     * shared security module. The payload is Base64-decoded and the tenantId
     * field is read from the JSON claims. This matches the approach used
     * in other controllers in the log_management module.
     *
     * @param token the raw JWT string without the "Bearer " prefix
     * @return tenantId claim value from the JWT payload
     * @throws RuntimeException when the token is malformed or the claim is absent
     */
    private String extractTenantIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Malformed JWT token");
            }

            String payload = new String(
                    java.util.Base64.getUrlDecoder().decode(parts[1]));

            com.fasterxml.jackson.databind.JsonNode claims =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(payload);

            String tenantId = claims.path("tenantId").asText("");
            if (tenantId.isBlank()) {
                throw new RuntimeException("tenantId claim missing from JWT");
            }
            return tenantId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract tenantId from JWT", e);
        }
    }

    /**
     * Returns the top-N correlation chains ordered by chain size descending.
     *
     * <p>A correlation chain is a group of log documents sharing the same
     * {@code correlationId} with at least 2 documents. Each chain includes
     * the root cause message, severity, and ordered step list.
     *
     * <p>Common frontend calls:
     * <pre>
     *   GET /api/v1/logs/analytics/correlation-chains
     *       -- top 10 chains, full history (both defaults)
     *
     *   GET /api/v1/logs/analytics/correlation-chains?limit=5
     *       -- top 5 chains, full history
     *
     *   GET /api/v1/logs/analytics/correlation-chains?hours=24&limit=10
     *       -- top 10 chains from the last 24 hours
     * </pre>
     *
     * @param limit   optional cap on number of chains; default 10, max 50
     * @param hours   optional look-back window in hours; when omitted, range=all applies
     * @param range   optional; pass "all" to cover full history explicitly
     * @param request incoming HTTP request; used only for JWT extraction
     * @return HTTP 200 with ordered list of {@link CorrelationChain}
     */
    @GetMapping(LogAnalyticsCorrelationConstants.API_CORRELATION_CHAINS)
    public ResponseEntity<List<CorrelationChain>> getCorrelationChains(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "hours", required = false) Integer hours,
            @RequestParam(value = "range", required = false) String range,
            HttpServletRequest request) {

        String tenantId = extractTenantFromRequest(request);

        logger.info().message(LogAnalyticsCorrelationConstants.LOG_REQUEST_RECEIVED)
                .detail(LogAnalyticsCorrelationConstants.LOG_KEY_LIMIT, limit)
                .log();

        validateCorrelationRange(range);

        int resolvedLimit = resolveCorrelationLimit(limit);
        int resolvedHours = resolveCorrelationHours(hours);
        boolean rangeAll = resolveCorrelationRangeAll(range, hours);

        List<CorrelationChain> response =
                logAnalyticsCorrelationService.computeCorrelationChains(
                        resolvedLimit, resolvedHours, rangeAll);

        return ResponseEntity.ok(response);
    }

    private boolean resolveCorrelationRangeAll(String range, Integer hours) {
        if (LogAnalyticsCorrelationConstants.RANGE_ALL.equalsIgnoreCase(range)) {
            return true;
        }
        if (range == null && hours == null) {
            return true;
        }
        return false;
    }

    private int resolveCorrelationLimit(Integer limit) {
        if (limit == null) {
            return LogAnalyticsCorrelationConstants.DEFAULT_LIMIT;
        }
        if (limit < LogAnalyticsCorrelationConstants.MINIMUM_LIMIT
                || limit > LogAnalyticsCorrelationConstants.MAXIMUM_LIMIT) {
            throw new IllegalArgumentException(
                    LogAnalyticsCorrelationConstants.VALIDATION_LIMIT_OUT_OF_RANGE);
        }
        return limit;
    }

    private int resolveCorrelationHours(Integer hours) {
        if (hours == null) {
            return 24;
        }
        if (hours < LogAnalyticsCorrelationConstants.MINIMUM_HOURS
                || hours > LogAnalyticsCorrelationConstants.MAXIMUM_HOURS) {
            throw new IllegalArgumentException(
                    LogAnalyticsCorrelationConstants.VALIDATION_HOURS_OUT_OF_RANGE);
        }
        return hours;
    }

    private void validateCorrelationRange(String range) {
        if (range == null) {
            return;
        }
        if (!range.equalsIgnoreCase(LogAnalyticsCorrelationConstants.RANGE_ALL)) {
            throw new IllegalArgumentException(
                    LogAnalyticsCorrelationConstants.VALIDATION_RANGE_UNKNOWN);
        }
    }
}