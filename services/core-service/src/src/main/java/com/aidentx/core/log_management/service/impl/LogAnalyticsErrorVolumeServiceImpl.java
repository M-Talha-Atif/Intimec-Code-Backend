package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants;
import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeEntry;
import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeResponse;
import com.aidentx.core.log_management.service.LogAnalyticsErrorVolumeService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsErrorVolumeService}.
 *
 * <p>All hour labels in the response are expressed in the caller-supplied
 * timezone. This means a user in Lahore (Asia/Karachi, UTC+5) will see
 * "15:00" for events that occurred at 10:00 UTC, which matches what they
 * see on their system clock. A user in Zurich (Europe/Zurich, UTC+2 in
 * winter) will see "12:00" for the same events.
 *
 * <p>How timezone conversion works:
 * <ol>
 *   <li>The window lower bound is computed in UTC epoch milliseconds using
 *       {@code System.currentTimeMillis()} -- this is always correct regardless
 *       of timezone because epoch ms is absolute.</li>
 *   <li>OpenSearch stores and returns all timestamps in UTC epoch ms.</li>
 *   <li>Each bucket key from the date_histogram is a UTC epoch ms value.
 *       We convert it to the target timezone to derive the hour label.</li>
 *   <li>The timeline walk also operates in the target timezone -- we compute
 *       the start ZonedDateTime and iterate hour by hour in that timezone,
 *       which correctly handles DST transitions (e.g. clocks going forward
 *       means one hour is skipped).</li>
 * </ol>
 *
 * <p>Example for Asia/Karachi (UTC+5), current time 15:27 PKT (10:27 UTC):
 * <pre>
 *   windowStart UTC = 10:27 yesterday = 09:27 UTC epoch yesterday
 *   first label     = "10:00" PKT (= 05:00 UTC yesterday)
 *   current label   = "15:00" PKT (= 10:00 UTC today)
 *   labels go left to right oldest to newest
 * </pre>
 *
 * <p>Example for Europe/Zurich (UTC+2 in summer), same UTC moment:
 * <pre>
 *   first label = "12:00" Zurich time
 *   current     = "17:00" Zurich time
 * </pre>
 */
@Service
public class LogAnalyticsErrorVolumeServiceImpl implements LogAnalyticsErrorVolumeService {

    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("HH:00");

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsErrorVolumeServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsErrorVolumeService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Execution sequence:
     * <ol>
     *   <li>Resolve timezone -- fall back to UTC on invalid input.</li>
     *   <li>Compute UTC epoch ms window start.</li>
     *   <li>Fetch top-N sources by total count in window.</li>
     *   <li>Fetch hourly date_histogram with nested source terms.</li>
     *   <li>Walk hours in the target timezone from window start to now,
     *       build gap-filled timeline with timezone-local labels.</li>
     * </ol>
     */
    @Override
    public ErrorVolumeResponse computeErrorVolume(
            int hours, int limit, boolean rangeAll, String timezone) {

        ZoneId zoneId = resolveZoneId(timezone);

        logger.info().message(LogAnalyticsErrorVolumeConstants.LOG_COMPUTATION_STARTED)
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_HOURS, hours)
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_LIMIT, limit)
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_RANGE_ALL, rangeAll)
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_TIMEZONE, zoneId.getId())
                .log();

        // windowStartMs is always UTC epoch ms -- absolute, timezone-independent
        long windowStartMs = resolveWindowStart(hours, rangeAll);

        List<String> topSources = fetchTopSources(limit, windowStartMs, rangeAll);

        if (topSources.isEmpty()) {
            logger.info().message(LogAnalyticsErrorVolumeConstants.LOG_COMPUTATION_COMPLETE)
                    .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_SOURCE_COUNT, 0)
                    .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_ENTRY_COUNT, hours)
                    .log();
            return buildEmptyResponse(hours, limit, topSources, zoneId, windowStartMs, rangeAll);
        }

        Map<String, Map<String, Long>> hourlyData =
                fetchHourlyBreakdown(limit, windowStartMs, rangeAll, zoneId);

        List<ErrorVolumeEntry> timeline =
                buildTimeline(hours, windowStartMs, topSources, hourlyData, zoneId, rangeAll);

        logger.info().message(LogAnalyticsErrorVolumeConstants.LOG_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_SOURCE_COUNT, topSources.size())
                .detail(LogAnalyticsErrorVolumeConstants.LOG_KEY_ENTRY_COUNT, timeline.size())
                .log();

        return new ErrorVolumeResponse(hours, limit, topSources, timeline);
    }

    /**
     * Resolves the IANA timezone string to a {@link ZoneId}.
     *
     * <p>Falls back to UTC on any invalid or blank input rather than throwing,
     * so a misconfigured frontend parameter never breaks the chart.
     *
     * <p>Examples:
     * <pre>
     *   "Asia/Karachi"    -- ZoneId for PKT (UTC+5, no DST)
     *   "Europe/Zurich"   -- ZoneId for CET/CEST (UTC+1/+2 with DST)
     *   "America/New_York"-- ZoneId for EST/EDT (UTC-5/-4 with DST)
     *   null or ""        -- falls back to UTC
     *   "Invalid/Zone"    -- falls back to UTC
     * </pre>
     *
     * @param timezone IANA timezone string from the request parameter; may be null
     * @return resolved ZoneId, never null
     */
    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(LogAnalyticsErrorVolumeConstants.DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            logger.warn().message("Invalid timezone supplied, falling back to UTC")
                    .detail("timezone", timezone)
                    .log();
            return ZoneId.of(LogAnalyticsErrorVolumeConstants.DEFAULT_TIMEZONE);
        }
    }

    /**
     * Computes the UTC epoch ms window start.
     *
     * <p>This is always an absolute UTC value -- timezones play no role here.
     * {@code System.currentTimeMillis()} is UTC epoch ms on every JVM on every
     * machine regardless of the server's local timezone setting.
     *
     * <p>Example: hours=24, now=2026-03-18T10:27:00Z
     * -- windowStart = 2026-03-17T10:27:00Z = 1742211620000
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
                - ((long) hours * LogAnalyticsErrorVolumeConstants.MS_PER_HOUR);
    }

    /**
     * Issues a terms aggregation to identify top-N sources by error count.
     * No timezone involved -- counts are absolute regardless of timezone.
     *
     * @param limit         number of top sources
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, omits timestamp filter
     * @return ordered source names, rank 1 first; empty when no data
     */
    private List<String> fetchTopSources(int limit, long windowStartMs, boolean rangeAll) {
        Map<String, Object> query = buildTopSourcesQuery(limit, windowStartMs, rangeAll);

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsErrorVolumeConstants.LOGS_INDEX, query);

        if (result == null) {
            return new ArrayList<>();
        }

        List<String> sources = new ArrayList<>();
        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsErrorVolumeConstants.AGG_TOP_SOURCES)
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
     * Issues the date_histogram query and returns results indexed by
     * timezone-local hour label.
     *
     * <p>Each bucket from OpenSearch carries a UTC epoch ms key. We convert
     * that key to the target timezone to derive the hour label. This is the
     * only place timezone conversion happens on the data path -- everything
     * else works with labels derived here.
     *
     * <p>Example: bucket key = 1742220000000 (= 2026-03-17T12:00:00Z)
     * <ul>
     *   <li>Asia/Karachi (UTC+5) -- label = "17:00"</li>
     *   <li>Europe/Zurich (UTC+1) -- label = "13:00"</li>
     *   <li>UTC -- label = "12:00"</li>
     * </ul>
     *
     * @param limit         nested terms size
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, omits timestamp filter
     * @param zoneId        target timezone for hour label conversion
     * @return map of timezone-local hour label to (source -> count)
     */
    private Map<String, Map<String, Long>> fetchHourlyBreakdown(
            int limit, long windowStartMs, boolean rangeAll, ZoneId zoneId) {

        Map<String, Object> query = buildHourlyQuery(limit, windowStartMs, rangeAll);

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsErrorVolumeConstants.LOGS_INDEX, query);

        Map<String, Map<String, Long>> hourlyData = new LinkedHashMap<>();

        if (result == null) {
            return hourlyData;
        }

        JsonNode hourBuckets = result
                .path("aggregations")
                .path(LogAnalyticsErrorVolumeConstants.AGG_HOURLY_BUCKETS)
                .path("buckets");

        if (hourBuckets.isMissingNode() || !hourBuckets.isArray()) {
            return hourlyData;
        }

        for (JsonNode hourBucket : hourBuckets) {
            long epochMs = hourBucket.path(LogAnalyticsErrorVolumeConstants.BUCKET_KEY).asLong(0L);

            // Convert UTC epoch ms to the target timezone to get the correct local hour label
            // This is what makes the chart show local time instead of UTC time
            ZonedDateTime zonedDateTime = Instant.ofEpochMilli(epochMs).atZone(zoneId);
            String hourLabel = zonedDateTime.format(HOUR_FORMATTER);

            Map<String, Long> sourceCounts = hourlyData.computeIfAbsent(
                    hourLabel, k -> new LinkedHashMap<>());

            JsonNode sourceBuckets = hourBucket
                    .path(LogAnalyticsErrorVolumeConstants.AGG_SOURCE_PER_HOUR)
                    .path("buckets");

            if (!sourceBuckets.isMissingNode() && sourceBuckets.isArray()) {
                for (JsonNode sourceBucket : sourceBuckets) {
                    String sourceName = sourceBucket.path("key").asText();
                    long count = sourceBucket.path("doc_count").asLong(0L);
                    // Use merge to accumulate -- when rangeAll=true multiple calendar
                    // days share the same hour-of-day label so we sum not overwrite
                    sourceCounts.merge(sourceName, count, Long::sum);
                }
            }
        }

        return hourlyData;
    }

    /**
     * Builds the gap-filled timeline walking hours in the target timezone.
     *
     * <p>The walk starts at the truncated hour of windowStart in the target
     * timezone and steps forward one hour at a time for {@code hours} steps.
     * Walking in the target timezone means DST transitions are handled
     * correctly -- if clocks jump forward 1 hour, that hour is skipped
     * automatically by {@code ZonedDateTime.plusHours(1)}.
     *
     * <p>Hours are ordered oldest first so the chart x-axis reads
     * chronologically left to right.
     *
     * <p>Example for Asia/Karachi, hours=3, windowStart=10:27 UTC
     * (= 15:27 PKT):
     * <pre>
     *   slot 0: 15:00 PKT  (windowStart truncated to hour in PKT)
     *   slot 1: 16:00 PKT
     *   slot 2: 17:00 PKT  (current hour in PKT at time of request)
     * </pre>
     *
     * @param hours         number of hour slots
     * @param windowStartMs UTC epoch ms lower bound
     * @param topSources    ordered source names
     * @param hourlyData    timezone-local label to source counts
     * @param zoneId        target timezone
     * @param rangeAll      when true, produces 24 entries for hours 0-23
     * @return ordered timeline, oldest first
     */
    private List<ErrorVolumeEntry> buildTimeline(
            int hours,
            long windowStartMs,
            List<String> topSources,
            Map<String, Map<String, Long>> hourlyData,
            ZoneId zoneId,
            boolean rangeAll) {

        List<ErrorVolumeEntry> timeline = new ArrayList<>();

        if (rangeAll) {
            // rangeAll: produce 24 entries for hours 00:00 through 23:00
            for (int hourOfDay = 0;
                 hourOfDay < LogAnalyticsErrorVolumeConstants.HOURS_IN_DAY;
                 hourOfDay++) {
                String hourLabel = String.format(
                        LogAnalyticsErrorVolumeConstants.HOUR_LABEL_FORMAT, hourOfDay);
                timeline.add(buildEntry(hourLabel, topSources, hourlyData));
            }
        } else {
            // Truncate windowStart to the start of its hour in the target timezone
            // Example: windowStart = 10:27 UTC in Asia/Karachi = 15:27 PKT
            //          truncated to hour = 15:00 PKT
            ZonedDateTime cursor = Instant.ofEpochMilli(windowStartMs)
                    .atZone(zoneId)
                    .truncatedTo(java.time.temporal.ChronoUnit.HOURS);

            for (int slot = 0; slot < hours; slot++) {
                String hourLabel = cursor.format(HOUR_FORMATTER);
                timeline.add(buildEntry(hourLabel, topSources, hourlyData));
                // plusHours handles DST correctly in the target timezone
                cursor = cursor.plusHours(1);
            }
        }

        return timeline;
    }

    /**
     * Builds a single {@link ErrorVolumeEntry} for the given hour label.
     * All top sources are present -- absent sources default to 0.
     *
     * @param hourLabel  timezone-local "HH:00" label
     * @param topSources top-N source names
     * @param hourlyData timezone-local label to source counts
     * @return populated entry
     */
    private ErrorVolumeEntry buildEntry(
            String hourLabel,
            List<String> topSources,
            Map<String, Map<String, Long>> hourlyData) {

        ErrorVolumeEntry entry = new ErrorVolumeEntry(hourLabel);
        Map<String, Long> sourceCounts = hourlyData.getOrDefault(hourLabel, Map.of());

        for (String source : topSources) {
            entry.setSourceCount(source, sourceCounts.getOrDefault(source, 0L));
        }

        return entry;
    }

    /**
     * Builds the top-sources terms aggregation query body.
     *
     * @param limit         terms size
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, omits timestamp range filter
     * @return OpenSearch request body
     */
    private Map<String, Object> buildTopSourcesQuery(
            int limit, long windowStartMs, boolean rangeAll) {

        Map<String, Object> query = new java.util.HashMap<>();
        query.put("size", 0);
        query.put("query", buildErrorFilterQuery(windowStartMs, rangeAll));
        query.put("aggs", Map.of(
                LogAnalyticsErrorVolumeConstants.AGG_TOP_SOURCES,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsErrorVolumeConstants.FIELD_SOURCE_KEYWORD,
                        "size", limit))));

        return query;
    }

    /**
     * Builds the hourly date_histogram query body with nested source terms.
     *
     * <p>Uses {@code calendar_interval: "1h"} so buckets align to UTC clock
     * hours. Timezone conversion is applied in the service when reading
     * bucket keys, not in the OpenSearch query itself -- this avoids
     * OpenSearch timezone handling inconsistencies.
     *
     * @param limit         nested terms size
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      when true, omits timestamp range filter
     * @return OpenSearch request body
     */
    private Map<String, Object> buildHourlyQuery(
            int limit, long windowStartMs, boolean rangeAll) {

        Map<String, Object> sourceAgg = Map.of("terms", Map.of(
                "field", LogAnalyticsErrorVolumeConstants.FIELD_SOURCE_KEYWORD,
                "size", limit));

        Map<String, Object> hourAgg = new java.util.HashMap<>();
        hourAgg.put("date_histogram", Map.of(
                "field", LogAnalyticsErrorVolumeConstants.FIELD_TIMESTAMP,
                "calendar_interval", "1h"));
        hourAgg.put("aggs", Map.of(
                LogAnalyticsErrorVolumeConstants.AGG_SOURCE_PER_HOUR, sourceAgg));

        Map<String, Object> query = new java.util.HashMap<>();
        query.put("size", 0);
        query.put("query", buildErrorFilterQuery(windowStartMs, rangeAll));
        query.put("aggs", Map.of(
                LogAnalyticsErrorVolumeConstants.AGG_HOURLY_BUCKETS, hourAgg));

        return query;
    }

    /**
     * Builds the bool/must filter for ERROR+WARN documents within the window.
     *
     * @param windowStartMs UTC epoch ms lower bound; 0 means no bound
     * @param rangeAll      when true, omits range filter
     * @return OpenSearch query clause
     */
    private Map<String, Object> buildErrorFilterQuery(long windowStartMs, boolean rangeAll) {
        List<Object> mustClauses = new java.util.ArrayList<>();

        // Level filter -- only ERROR and WARN contribute to error volume.
        // INFO and DEBUG are excluded because they represent normal operations,
        // not errors. Including them would inflate the chart and mask real spikes.
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsErrorVolumeConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsErrorVolumeConstants.LEVEL_ERROR,
                        LogAnalyticsErrorVolumeConstants.LEVEL_WARN))));


        if (!rangeAll) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsErrorVolumeConstants.FIELD_TIMESTAMP,
                    Map.of("gte", windowStartMs))));
        }

        return Map.of("bool", Map.of("must", mustClauses));
    }

    /**
     * Builds a zero-filled response when no top sources are found.
     *
     * @param hours         number of slots
     * @param limit         effective limit for response metadata
     * @param topSources    empty list
     * @param zoneId        target timezone for label generation
     * @param windowStartMs UTC epoch ms lower bound
     * @param rangeAll      affects slot count and label strategy
     * @return empty response with correct hour labels
     */
    private ErrorVolumeResponse buildEmptyResponse(
            int hours,
            int limit,
            List<String> topSources,
            ZoneId zoneId,
            long windowStartMs,
            boolean rangeAll) {

        List<ErrorVolumeEntry> timeline = buildTimeline(
                hours, windowStartMs, topSources, Map.of(), zoneId, rangeAll);

        return new ErrorVolumeResponse(hours, limit, topSources, timeline);
    }
}