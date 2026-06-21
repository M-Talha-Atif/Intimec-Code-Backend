package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsHeatmapConstants;
import com.aidentx.core.log_management.model.dto.analytics.HeatmapCell;
import com.aidentx.core.log_management.model.dto.analytics.HeatmapResponse;
import com.aidentx.core.log_management.service.LogAnalyticsHeatmapService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsHeatmapService}.
 *
 * <p>Hour labels in the response are "HH:00" strings in the caller-supplied
 * timezone. A UTC bucket key of 2026-03-18T10:00:00Z becomes "15:00" for
 * Asia/Karachi (UTC+5) and "11:00" for Europe/Zurich (UTC+1).
 *
 * <p>Execution sequence:
 * <ol>
 *   <li>Resolve timezone to a {@link ZoneId}.</li>
 *   <li>Compute UTC epoch ms window start.</li>
 *   <li>Query 1: top-N sources by total count.</li>
 *   <li>Query 2: date_histogram by hour with nested source terms.</li>
 *   <li>Convert each UTC bucket key to a timezone-local "HH:00" label.</li>
 *   <li>Accumulate counts per (source, hourLabel) pair.</li>
 *   <li>Build sparse cell list ordered by source rank then hour ascending.</li>
 * </ol>
 *
 * <p>When rangeAll=true and the window spans multiple calendar days, multiple
 * UTC buckets can map to the same "HH:00" label. Counts are accumulated so
 * the cell shows total activity across all days for that hour slot.
 * Example: "15:00" on Monday had 10 events and "15:00" on Tuesday had 8.
 * The cell shows 18.
 */
@Service
public class LogAnalyticsHeatmapServiceImpl implements LogAnalyticsHeatmapService {

    /**
     * Formatter producing "HH:00" labels (e.g. "00:00", "09:00", "23:00").
     * Matches the format expected by the frontend heatmap column headers.
     */
    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("HH:00");

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsHeatmapServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsHeatmapService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HeatmapResponse computeHeatmap(
            int hours, int limit, boolean rangeAll, String timezone) {

        ZoneId zoneId = resolveZoneId(timezone);

        logger.info().message(LogAnalyticsHeatmapConstants.LOG_COMPUTATION_STARTED)
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_HOURS, rangeAll ? "all" : hours)
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_LIMIT, limit)
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_RANGE_ALL, rangeAll)
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_TIMEZONE, zoneId.getId())
                .log();

        long windowStartMs = resolveWindowStart(hours, rangeAll);

        List<String> topSources = fetchTopSources(limit, windowStartMs, rangeAll);

        if (topSources.isEmpty()) {
            logger.info().message(LogAnalyticsHeatmapConstants.LOG_COMPUTATION_COMPLETE)
                    .detail(LogAnalyticsHeatmapConstants.LOG_KEY_SOURCE_COUNT, 0)
                    .detail(LogAnalyticsHeatmapConstants.LOG_KEY_CELL_COUNT, 0)
                    .log();
            return new HeatmapResponse(hours, limit, topSources, new ArrayList<>());
        }

        // Map structure: source -> (hourLabel "HH:00" -> count)
        Map<String, Map<String, Long>> sourceHourCounts =
                fetchHourlyBreakdown(limit, windowStartMs, rangeAll, zoneId);

        List<HeatmapCell> cells = buildCells(topSources, sourceHourCounts);

        logger.info().message(LogAnalyticsHeatmapConstants.LOG_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_SOURCE_COUNT, topSources.size())
                .detail(LogAnalyticsHeatmapConstants.LOG_KEY_CELL_COUNT, cells.size())
                .log();

        return new HeatmapResponse(hours, limit, topSources, cells);
    }

    /**
     * Resolves the IANA timezone string to a {@link ZoneId}.
     * Falls back to UTC on null, blank, or unrecognised input.
     *
     * <p>Examples:
     * <pre>
     *   "Asia/Karachi"    -- ZoneId PKT (UTC+5, no DST)
     *   "Europe/Zurich"   -- ZoneId CET/CEST (UTC+1/+2 with DST)
     *   null              -- ZoneId UTC
     *   "Invalid/Zone"    -- ZoneId UTC (fallback)
     * </pre>
     *
     * @param timezone IANA timezone string; may be null
     * @return resolved ZoneId, never null
     */
    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(LogAnalyticsHeatmapConstants.DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            logger.warn().message("Invalid timezone supplied, falling back to UTC")
                    .detail(LogAnalyticsHeatmapConstants.LOG_KEY_TIMEZONE, timezone)
                    .log();
            return ZoneId.of(LogAnalyticsHeatmapConstants.DEFAULT_TIMEZONE);
        }
    }

    /**
     * Computes the UTC epoch ms window lower bound.
     *
     * @param hours    look-back window in hours
     * @param rangeAll when true, returns 0 (no lower bound)
     * @return UTC epoch ms lower bound
     */
    private long resolveWindowStart(int hours, boolean rangeAll) {
        if (rangeAll) {
            return 0L;
        }
        return System.currentTimeMillis()
                - ((long) hours * LogAnalyticsHeatmapConstants.MS_PER_HOUR);
    }

    /**
     * Issues a terms aggregation to identify the top-N sources.
     *
     * @param limit         terms agg size
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, omits range filter
     * @return ordered source names, rank 1 first; empty when no data
     */
    private List<String> fetchTopSources(int limit, long windowStartMs, boolean rangeAll) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", buildBaseQuery(windowStartMs, rangeAll));
        query.put("aggs", Map.of(
                LogAnalyticsHeatmapConstants.AGG_TOP_SOURCES,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsHeatmapConstants.FIELD_SOURCE_KEYWORD,
                        "size", limit))));

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsHeatmapConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsHeatmapConstants.LOG_OPENSEARCH_NULL)
                    .detail(LogAnalyticsHeatmapConstants.LOG_KEY_LIMIT, limit)
                    .log();
            return new ArrayList<>();
        }

        List<String> sources = new ArrayList<>();
        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsHeatmapConstants.AGG_TOP_SOURCES)
                .path("buckets");

        if (!buckets.isMissingNode() && buckets.isArray()) {
            for (JsonNode bucket : buckets) {
                String sourceName = bucket.path("key").asText();
                if (!sourceName.isBlank()) {
                    sources.add(sourceName);
                }
            }
        }

        return sources;
    }

    /**
     * Issues the date_histogram query with a nested source terms agg.
     * Returns a map of source name to ("HH:00" label to count).
     *
     * <p>Each UTC bucket key is converted to the target timezone using
     * {@link #HOUR_FORMATTER} to produce the "HH:00" label.
     *
     * <p>Example: bucket key 1742220000000 = 2026-03-17T12:00:00Z
     * <ul>
     *   <li>Asia/Karachi  (UTC+5) -- label "17:00"</li>
     *   <li>Europe/Zurich (UTC+1) -- label "13:00"</li>
     *   <li>UTC                   -- label "12:00"</li>
     * </ul>
     *
     * <p>When rangeAll=true, multiple calendar-day buckets can map to the
     * same "HH:00" label for the same source. Counts are accumulated via
     * merge so the cell reflects total activity across all days.
     *
     * @param limit         nested terms size
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, omits range filter
     * @param zoneId        target timezone for hour label conversion
     * @return source -> ("HH:00" label -> count) map; only non-zero entries
     */
    private Map<String, Map<String, Long>> fetchHourlyBreakdown(
            int limit, long windowStartMs, boolean rangeAll, ZoneId zoneId) {

        Map<String, Object> sourceAgg = Map.of("terms", Map.of(
                "field", LogAnalyticsHeatmapConstants.FIELD_SOURCE_KEYWORD,
                "size", limit));

        Map<String, Object> hourAgg = new HashMap<>();
        hourAgg.put("date_histogram", Map.of(
                "field", LogAnalyticsHeatmapConstants.FIELD_TIMESTAMP,
                "calendar_interval", "1h"));
        hourAgg.put("aggs", Map.of(
                LogAnalyticsHeatmapConstants.AGG_SOURCE_PER_HOUR, sourceAgg));

        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", buildBaseQuery(windowStartMs, rangeAll));
        query.put("aggs", Map.of(
                LogAnalyticsHeatmapConstants.AGG_HOURLY_BUCKETS, hourAgg));

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsHeatmapConstants.LOGS_INDEX, query);

        Map<String, Map<String, Long>> sourceHourCounts = new HashMap<>();

        if (result == null) {
            return sourceHourCounts;
        }

        JsonNode hourBuckets = result
                .path("aggregations")
                .path(LogAnalyticsHeatmapConstants.AGG_HOURLY_BUCKETS)
                .path("buckets");

        if (hourBuckets.isMissingNode() || !hourBuckets.isArray()) {
            return sourceHourCounts;
        }

        for (JsonNode hourBucket : hourBuckets) {
            long epochMs = hourBucket
                    .path(LogAnalyticsHeatmapConstants.BUCKET_KEY)
                    .asLong(0L);

            // Convert UTC epoch ms to "HH:00" label in the target timezone
            // Example: 1742220000000 UTC + Asia/Karachi = "17:00"
            ZonedDateTime zonedDateTime = Instant.ofEpochMilli(epochMs).atZone(zoneId);
            String hourLabel = zonedDateTime.format(HOUR_FORMATTER);

            JsonNode sourceBuckets = hourBucket
                    .path(LogAnalyticsHeatmapConstants.AGG_SOURCE_PER_HOUR)
                    .path("buckets");

            if (sourceBuckets.isMissingNode() || !sourceBuckets.isArray()) {
                continue;
            }

            for (JsonNode sourceBucket : sourceBuckets) {
                String sourceName = sourceBucket.path("key").asText();
                long count = sourceBucket.path("doc_count").asLong(0L);

                if (!sourceName.isBlank() && count > 0) {
                    // merge accumulates counts when multiple calendar-day buckets
                    // share the same "HH:00" label (rangeAll=true scenario)
                    sourceHourCounts
                            .computeIfAbsent(sourceName, k -> new HashMap<>())
                            .merge(hourLabel, count, Long::sum);
                }
            }
        }

        return sourceHourCounts;
    }

    /**
     * Builds the sparse cell list from the top-sources list and hourly data.
     *
     * <p>Only sources in {@code topSources} are included. Zero-count cells
     * are omitted. Cells are ordered by source rank (topSources order) then
     * hour label ascending so "00:00" comes before "23:00".
     *
     * <p>Example: topSources=["GatewayPermissionFilter","JwtUtil"],
     * data has GatewayPermissionFilter at "15:00" and "17:00",
     * JwtUtil at "17:00" only:
     * <pre>
     *   { source="GatewayPermissionFilter", hour="15:00", count=49 }
     *   { source="GatewayPermissionFilter", hour="17:00", count=68 }
     *   { source="JwtUtil",                 hour="17:00", count=12 }
     * </pre>
     *
     * @param topSources       ordered source names from the top-sources query
     * @param sourceHourCounts source -> ("HH:00" -> count) from histogram query
     * @return sparse cell list, never null
     */
    private List<HeatmapCell> buildCells(
            List<String> topSources,
            Map<String, Map<String, Long>> sourceHourCounts) {

        List<HeatmapCell> cells = new ArrayList<>();

        for (String source : topSources) {
            Map<String, Long> hourCounts =
                    sourceHourCounts.getOrDefault(source, Map.of());

            // Sort by hour label string ascending -- "00:00" < "09:00" < "23:00"
            // String sort works correctly for "HH:00" format because it is zero-padded
            hourCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry ->
                            cells.add(new HeatmapCell(
                                    source,
                                    entry.getKey(),
                                    entry.getValue())));
        }

        return cells;
    }

    /**
     * Builds the base query filter shared by both queries.
     * Returns match_all when rangeAll=true, otherwise a range filter.
     *
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, returns match_all
     * @return OpenSearch query clause
     */
    private Map<String, Object> buildBaseQuery(long windowStartMs, boolean rangeAll) {
        List<Object> mustClauses = new java.util.ArrayList<>();

        // Level filter -- the error heatmap shows error density by hour and source.
        // Only ERROR and WARN entries are relevant. Including INFO/DEBUG would make
        // the heatmap reflect general traffic volume rather than error patterns.
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsHeatmapConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsHeatmapConstants.LEVEL_ERROR,
                        LogAnalyticsHeatmapConstants.LEVEL_WARN))));

        if (!rangeAll) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsHeatmapConstants.FIELD_TIMESTAMP,
                    Map.of("gte", windowStartMs))));
        }

        // When rangeAll=true we still need the level filter so we always
        // use bool/must -- never fall back to match_all here
        return Map.of("bool", Map.of("must", mustClauses));
    }
}