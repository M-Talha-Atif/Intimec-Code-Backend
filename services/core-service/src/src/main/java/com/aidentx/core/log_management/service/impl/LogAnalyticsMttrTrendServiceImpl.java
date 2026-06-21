package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsMttrTrendConstants;
import com.aidentx.core.log_management.model.dto.analytics.MttrTrendEntry;
import com.aidentx.core.log_management.service.LogAnalyticsMttrTrendService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsMttrTrendService}.
 *
 * <p>Issues one OpenSearch request covering the last 7 days. The request
 * uses a filter aggregation (restricting to resolved documents only) wrapping
 * a date_histogram (one bucket per calendar day in the caller's timezone)
 * with a scripted_metric inside each day bucket computing the average
 * resolution time.
 *
 * <p>Full OpenSearch query (timezone=Asia/Karachi):
 * <pre>
 * POST main-logs/_search
 * {
 *   "size": 0,
 *   "query": { "match_all": {} },
 *   "aggs": {
 *     "resolved_filter": {
 *       "filter": {
 *         "bool": {
 *           "must": [
 *             { "exists": { "field": "resolvedAt" } },
 *             { "range": { "resolvedAt": { "gte": windowStartMs } } }
 *           ]
 *         }
 *       },
 *       "aggs": {
 *         "daily_buckets": {
 *           "date_histogram": {
 *             "field": "resolvedAt",
 *             "calendar_interval": "1d",
 *             "time_zone": "Asia/Karachi",
 *             "min_doc_count": 0,
 *             "extended_bounds": {
 *               "min": windowStartMs,
 *               "max": nowMs
 *             }
 *           },
 *           "aggs": {
 *             "mttr_minutes": {
 *               "scripted_metric": {
 *                 "init_script":    "state.deltas = []",
 *                 "map_script":     "...",
 *                 "combine_script": "...",
 *                 "reduce_script":  "..."
 *               }
 *             }
 *           }
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Why time_zone is passed to date_histogram:
 * Without it, bucket boundaries are at UTC midnight. With timezone=Asia/Karachi
 * they align to PKT midnight (19:00 UTC previous day). This means "Mon" contains
 * events resolved between Mon 00:00 PKT and Mon 23:59 PKT rather than UTC-based.
 * The caller sees the same day label they would see on their local calendar.
 *
 * <p>Why extended_bounds is used:
 * Without it, days with zero resolved entries produce no bucket. The gap-filled
 * response is built by the service after parsing. extended_bounds forces
 * OpenSearch to return a bucket for every day even when doc_count=0, which
 * simplifies parsing and ensures exactly 7 entries are always present.
 *
 * <p>Scripted metric logic per day bucket:
 * <ul>
 *   <li>map: for each document compute resolvedAt - @timestamp in ms</li>
 *   <li>combine: sum deltas and count on each shard</li>
 *   <li>reduce: sum all shard totals, divide by count, convert to minutes</li>
 * </ul>
 *
 * <p>Result ordering: date_histogram returns buckets in ascending order
 * (oldest day first) which matches what the line chart expects.
 */
@Service
public class LogAnalyticsMttrTrendServiceImpl implements LogAnalyticsMttrTrendService {

    private static final DateTimeFormatter DAY_FORMATTER =
            DateTimeFormatter.ofPattern(
                    LogAnalyticsMttrTrendConstants.DAY_LABEL_FORMAT, Locale.ENGLISH);

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsMttrTrendServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsMttrTrendService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MttrTrendEntry> computeMttrTrend(String timezone) {
        ZoneId zoneId = resolveZoneId(timezone);

        long nowMs          = System.currentTimeMillis();
        long windowStartMs  = nowMs
                - (long) LogAnalyticsMttrTrendConstants.TREND_DAYS
                * LogAnalyticsMttrTrendConstants.MS_PER_DAY;

        logger.info().message(LogAnalyticsMttrTrendConstants.LOG_COMPUTATION_STARTED)
                .detail(LogAnalyticsMttrTrendConstants.LOG_KEY_TIMEZONE, zoneId.getId())
                .detail(LogAnalyticsMttrTrendConstants.LOG_KEY_WINDOW_START, windowStartMs)
                .log();

        Map<String, Object> query = buildQuery(zoneId, windowStartMs, nowMs);
        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsMttrTrendConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsMttrTrendConstants.LOG_OPENSEARCH_NULL)
                    .detail(LogAnalyticsMttrTrendConstants.LOG_KEY_TIMEZONE, timezone)
                    .log();
            return buildEmptyTrend(zoneId, windowStartMs);
        }

        List<MttrTrendEntry> trend = parseTrend(result, zoneId, windowStartMs);

        logger.info().message(LogAnalyticsMttrTrendConstants.LOG_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsMttrTrendConstants.LOG_KEY_ENTRY_COUNT, trend.size())
                .log();

        return trend;
    }

    /**
     * Resolves the IANA timezone string to a ZoneId.
     * Falls back to UTC on null, blank, or unrecognised input.
     *
     * @param timezone IANA timezone string; may be null
     * @return ZoneId, never null
     */
    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(LogAnalyticsMttrTrendConstants.DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            logger.warn().message("Invalid timezone, falling back to UTC")
                    .detail(LogAnalyticsMttrTrendConstants.LOG_KEY_TIMEZONE, timezone)
                    .log();
            return ZoneId.of(LogAnalyticsMttrTrendConstants.DEFAULT_TIMEZONE);
        }
    }

    /**
     * Builds the full OpenSearch aggregation query.
     *
     * <p>Structure:
     * <pre>
     *   match_all root query (no level filter -- MTTR applies to all resolved entries)
     *   -> resolved_filter (exists: resolvedAt AND range: resolvedAt >= windowStart)
     *      -> daily_buckets (date_histogram on resolvedAt, 1d interval, caller timezone)
     *         -> mttr_minutes (scripted_metric computing avg resolution time in minutes)
     * </pre>
     *
     * <p>extended_bounds ensures a bucket is returned for every day in the window
     * even when no entries were resolved that day. Without it, gaps produce no bucket
     * and the service would need to fill them manually.
     *
     * @param zoneId       target timezone for bucket boundaries
     * @param windowStartMs UTC epoch ms start of the 7-day window
     * @param nowMs        UTC epoch ms current time (used for extended_bounds max)
     * @return full OpenSearch request body
     */
    private Map<String, Object> buildQuery(ZoneId zoneId, long windowStartMs, long nowMs) {
        // Scripted metric that computes MTTR in minutes for a single day bucket.
        // Both resolvedAt and @timestamp are stored as epoch ms longs in OpenSearch.
        // doc['field'].value returns the raw long for numeric/date fields in Painless.
        Map<String, Object> scriptedMetric = new HashMap<>();
        scriptedMetric.put("init_script", "state.deltas = []");

        scriptedMetric.put("map_script",
                "if (doc['" + LogAnalyticsMttrTrendConstants.FIELD_RESOLVED_AT + "'].size() > 0 " +
                "&& doc['" + LogAnalyticsMttrTrendConstants.FIELD_TIMESTAMP + "'].size() > 0) { " +
                "  long resolvedMs = doc['" + LogAnalyticsMttrTrendConstants.FIELD_RESOLVED_AT + "'].value; " +
                "  long createdMs  = doc['" + LogAnalyticsMttrTrendConstants.FIELD_TIMESTAMP + "'].value; " +
                "  if (resolvedMs > createdMs) { state.deltas.add(resolvedMs - createdMs); } " +
                "}");

        scriptedMetric.put("combine_script",
                "double sum = 0; " +
                "for (d in state.deltas) { sum += d; } " +
                "return [sum, state.deltas.size()]");

        scriptedMetric.put("reduce_script",
                "double totalSum = 0; long totalCount = 0; " +
                "for (s in states) { " +
                "  if (s != null) { totalSum += s[0]; totalCount += (long) s[1]; } " +
                "} " +
                "if (totalCount == 0) return 0.0; " +
                "return Math.round((totalSum / totalCount / " +
                LogAnalyticsMttrTrendConstants.MS_PER_MINUTE + ") * 10.0) / 10.0");

        // date_histogram on resolvedAt with timezone alignment
        Map<String, Object> histogramConfig = new HashMap<>();
        histogramConfig.put("field",             LogAnalyticsMttrTrendConstants.FIELD_RESOLVED_AT);
        histogramConfig.put("calendar_interval", "1d");
        histogramConfig.put("time_zone",         zoneId.getId());
        histogramConfig.put("min_doc_count",     0);
        // Force a bucket for every day in the window even with zero resolved entries
        histogramConfig.put("extended_bounds", Map.of("min", windowStartMs, "max", nowMs));

        Map<String, Object> dailyBuckets = new HashMap<>();
        dailyBuckets.put("date_histogram", histogramConfig);
        dailyBuckets.put("aggs", Map.of(
                LogAnalyticsMttrTrendConstants.AGG_MTTR_SCRIPT,
                Map.of("scripted_metric", scriptedMetric)));

        // Filter: only documents that have been resolved AND fall within the window
        Map<String, Object> resolvedFilter = Map.of(
                "filter", Map.of("bool", Map.of(
                        "must", List.of(
                                Map.of("exists", Map.of(
                                        "field", LogAnalyticsMttrTrendConstants.FIELD_RESOLVED_AT)),
                                Map.of("range", Map.of(
                                        LogAnalyticsMttrTrendConstants.FIELD_RESOLVED_AT,
                                        Map.of("gte", windowStartMs)))))),
                "aggs", Map.of(LogAnalyticsMttrTrendConstants.AGG_DAILY_BUCKETS, dailyBuckets));

        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", Map.of("match_all", Map.of()));
        query.put("aggs", Map.of(
                LogAnalyticsMttrTrendConstants.AGG_RESOLVED_FILTER, resolvedFilter));

        return query;
    }

    /**
     * Parses the OpenSearch response into a list of MttrTrendEntry.
     *
     * <p>The date_histogram returns buckets with epoch ms keys. Each key is
     * converted to the target timezone to produce the day label ("Mon", "Tue" etc).
     *
     * <p>The scripted_metric value for each day is the pre-computed average
     * in minutes (the reduce_script already divides by MS_PER_MINUTE and rounds).
     * Days with zero resolved entries return a scripted_metric value of 0.0.
     *
     * <p>If the date_histogram returns more than TREND_DAYS buckets (possible
     * when the window spans 8 days due to timezone offset), only the last
     * TREND_DAYS buckets are kept.
     *
     * @param result       full OpenSearch response
     * @param zoneId       target timezone for day label conversion
     * @param windowStartMs window lower bound (used for fallback generation)
     * @return list of 7 entries ordered oldest-first
     */
    private List<MttrTrendEntry> parseTrend(
            JsonNode result, ZoneId zoneId, long windowStartMs) {

        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsMttrTrendConstants.AGG_RESOLVED_FILTER)
                .path(LogAnalyticsMttrTrendConstants.AGG_DAILY_BUCKETS)
                .path("buckets");

        if (buckets.isMissingNode() || !buckets.isArray() || buckets.size() == 0) {
            return buildEmptyTrend(zoneId, windowStartMs);
        }

        // Use LinkedHashMap to preserve bucket insertion order (oldest first)
        // then convert to list at the end
        Map<String, Double> dayMttrMap = new LinkedHashMap<>();

        for (JsonNode bucket : buckets) {
            long epochMs = bucket.path("key").asLong(0L);

            // Convert UTC epoch ms to the target timezone to get the day label
            // Example: 2026-03-23T19:00:00Z (UTC) = 2026-03-24T00:00:00 PKT = "Tue"
            ZonedDateTime zonedDate = Instant.ofEpochMilli(epochMs).atZone(zoneId);
            String dayLabel = zonedDate.format(DAY_FORMATTER);

            JsonNode mttrValueNode = bucket
                    .path(LogAnalyticsMttrTrendConstants.AGG_MTTR_SCRIPT)
                    .path("value");

            double mttrMinutes = (mttrValueNode.isNull() || mttrValueNode.isMissingNode())
                    ? 0.0
                    : mttrValueNode.asDouble(0.0);

            // Accumulate in case multiple buckets map to the same day label
            // (can happen at DST boundaries)
            dayMttrMap.merge(dayLabel, mttrMinutes, Double::sum);
        }

        // Keep only the last TREND_DAYS entries if more were returned
        List<MttrTrendEntry> trend = new ArrayList<>();
        List<Map.Entry<String, Double>> entries = new ArrayList<>(dayMttrMap.entrySet());

        int startIndex = Math.max(0, entries.size() - LogAnalyticsMttrTrendConstants.TREND_DAYS);
        for (int i = startIndex; i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            // Round to 1 decimal place for clean display
            double rounded = Math.round(entry.getValue() * 10.0) / 10.0;
            trend.add(new MttrTrendEntry(entry.getKey(), rounded));
        }

        // If fewer than TREND_DAYS entries were returned, the response is still
        // valid -- it means not enough days have resolved entries yet
        return trend;
    }

    /**
     * Builds a zero-filled 7-day trend when OpenSearch returns null or
     * no resolved entries exist.
     *
     * <p>Walks forward from windowStart in the target timezone, one day at
     * a time, producing 7 entries with mttr=0.0.
     *
     * @param zoneId       target timezone for day labels
     * @param windowStartMs window lower bound in UTC epoch ms
     * @return list of 7 zero-filled entries
     */
    private List<MttrTrendEntry> buildEmptyTrend(ZoneId zoneId, long windowStartMs) {
        List<MttrTrendEntry> trend = new ArrayList<>();
        ZonedDateTime cursor = Instant.ofEpochMilli(windowStartMs)
                .atZone(zoneId)
                .toLocalDate()
                .atStartOfDay(zoneId);

        for (int day = 0; day < LogAnalyticsMttrTrendConstants.TREND_DAYS; day++) {
            trend.add(new MttrTrendEntry(cursor.format(DAY_FORMATTER), 0.0));
            cursor = cursor.plusDays(1);
        }

        return trend;
    }
}