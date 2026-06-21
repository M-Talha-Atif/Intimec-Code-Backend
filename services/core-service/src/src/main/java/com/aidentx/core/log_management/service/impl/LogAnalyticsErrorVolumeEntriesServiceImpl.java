package com.aidentx.core.log_management.service.impl;

import static com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants.FIELD_LEVEL_KEYWORD;
import static com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants.LEVEL_ERROR;
import static com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants.LEVEL_WARN;

import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeEntriesResponse;
import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.service.LogAnalyticsErrorVolumeEntriesService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsErrorVolumeEntriesService}.
 *
 * <p>The core challenge this service solves is the reverse of what
 * {@code LogAnalyticsErrorVolumeServiceImpl} does:
 *
 * <p>Error volume chart computation (forward direction):
 * <pre>
 *   UTC epoch ms bucket key  -->  convert to timezone  -->  "HH:00" label
 *   1742205600000 UTC        -->  Asia/Karachi +5       -->  "15:00"
 * </pre>
 *
 * <p>Entry drilldown (reverse direction -- what this service does):
 * <pre>
 *   "HH:00" label + timezone  -->  find matching UTC epoch ms  -->  fetch docs
 *   "15:00" Asia/Karachi      -->  1742205600000 UTC           -->  query OpenSearch
 * </pre>
 *
 * <p>The reversal algorithm:
 * <ol>
 *   <li>Compute the window start: now - (windowHours * 3600000) in UTC.</li>
 *   <li>Convert windowStart to the target timezone.</li>
 *   <li>Truncate to the start of that hour in the target timezone.</li>
 *   <li>Walk forward one hour at a time (in the target timezone, which
 *       handles DST correctly via ZonedDateTime.plusHours).</li>
 *   <li>At each step, format the current ZonedDateTime as "HH:00" and
 *       compare to the requested hour label.</li>
 *   <li>When the label matches, that ZonedDateTime's toInstant() gives
 *       the correct UTC epoch ms for the start of that hour slot.</li>
 *   <li>End of slot = start + 1 hour - 1 ms.</li>
 * </ol>
 *
 * <p>Why walk forward instead of computing directly:
 * A direct calculation like "today at 15:00 in PKT, converted to UTC" would
 * produce today's 15:00 slot. But if current time is 16:00 PKT and the window
 * is 24 hours, the "15:00" slot in the chart is YESTERDAY's 15:00 not today's.
 * Walking forward from windowStart guarantees we find the correct calendar day.
 *
 * <p>Example walk for Asia/Karachi (UTC+5), window=24h, clicked hour="15:00":
 * <pre>
 *   now = 2026-03-18T16:30 PKT = 11:30 UTC
 *   windowStart = 2026-03-17T16:30 PKT = 11:30 UTC
 *   truncate to hour in PKT = 2026-03-17T16:00 PKT
 *
 *   step 0: 2026-03-17 16:00 PKT = "16:00"  -- no match
 *   step 1: 2026-03-17 17:00 PKT = "17:00"  -- no match
 *   ...
 *   step 23: 2026-03-18 15:00 PKT = "15:00" -- MATCH (yesterday within window)
 *   epoch ms = 2026-03-18T10:00:00Z = 1742292000000
 * </pre>
 *
 * <p>Source filter note:
 * Source values in OpenSearch are fully qualified class names such as
 * "com.aidentx.access_portal_service.event.consumer.AccessRequestEventConsumer".
 * The error volume chart's {@code sources} array also contains full class names
 * because that is what the terms aggregation on {@code source} returns.
 * The filter uses {@code terms} on {@code source} for exact match.
 * If the caller passes short names like "GatewayPermissionFilter", no documents
 * will match because the stored values are fully qualified. The caller must pass
 * the exact strings from {@code ErrorVolumeResponse.sources}.
 */
@Service
public class LogAnalyticsErrorVolumeEntriesServiceImpl
        implements LogAnalyticsErrorVolumeEntriesService {

    private static final String LOGS_INDEX = "main-logs";
    private static final String FIELD_TIMESTAMP = "@timestamp";
    private static final String FIELD_SOURCE_KEYWORD = "source";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final long MS_PER_HOUR = 3_600_000L;
    private static final long MS_PER_HOUR_MINUS_ONE = MS_PER_HOUR - 1L;
    private static final int MAX_WALK_STEPS = 25;
    private static final DateTimeFormatter HOUR_FORMATTER =
            DateTimeFormatter.ofPattern("HH:00");

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsErrorVolumeEntriesServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsErrorVolumeEntriesService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ErrorVolumeEntriesResponse fetchEntriesForHour(
            String hour,
            String timezone,
            List<String> sources,
            int windowHours,
            int page,
            int size) {

        logger.info()
                .message("Error volume entries request received")
                .detail("hour", hour)
                .detail("timezone", timezone)
                .detail("sources", sources)
                .detail("windowHours", windowHours)
                .detail("page", page)
                .detail("size", size)
                .log();

        ZoneId zoneId = resolveZoneId(timezone);
        long[] epochBounds = resolveEpochBoundsForHour(hour, zoneId, windowHours);

        if (epochBounds == null) {
            logger.warn()
                    .message("Could not resolve epoch bounds for hour label")
                    .detail("hour", hour)
                    .detail("timezone", timezone)
                    .detail("windowHours", windowHours)
                    .log();
            return buildEmptyResponse(hour, zoneId.getId(), 0L, 0L, sources, page, size);
        }

        long dateFrom = epochBounds[0];
        long dateTo = epochBounds[1];

        logger.info()
                .message("Resolved epoch bounds for hour slot")
                .detail("hour", hour)
                .detail("dateFrom", dateFrom)
                .detail("dateTo", dateTo)
                .detail("dateFromIso", Instant.ofEpochMilli(dateFrom).toString())
                .detail("dateToIso", Instant.ofEpochMilli(dateTo).toString())
                .log();

        Map<String, Object> query = buildSearchQuery(dateFrom, dateTo, sources, page, size);
        JsonNode result = openSearchService.searchDocuments(LOGS_INDEX, query);

        if (result == null) {
            logger.warn()
                    .message("OpenSearch returned null for error volume entries")
                    .detail("hour", hour)
                    .log();
            return buildEmptyResponse(hour, zoneId.getId(), dateFrom, dateTo, sources, page, size);
        }

        long totalElements = result.path("hits").path("total").path("value").asLong(0L);
        List<LogEntry> entries = extractLogEntries(result);
        PaginationMetadata pagination = buildPagination(page, size, totalElements);

        logger.info()
                .message("Error volume entries query complete")
                .detail("hour", hour)
                .detail("totalElements", totalElements)
                .detail("returnedEntries", entries.size())
                .log();

        return new ErrorVolumeEntriesResponse(
                hour,
                zoneId.getId(),
                dateFrom,
                dateTo,
                sources,
                entries,
                pagination);
    }

    /**
     * Resolves the IANA timezone string to a ZoneId.
     * Falls back to UTC on null, blank, or unrecognised input so a bad
     * timezone parameter never causes a 500 error.
     *
     * @param timezone IANA timezone string; may be null
     * @return ZoneId, never null
     */
    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            logger.warn()
                    .message("Invalid timezone, falling back to UTC")
                    .detail("timezone", timezone)
                    .log();
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    /**
     * Resolves a timezone-local "HH:00" hour label to a UTC epoch ms range
     * by walking forward from the window start in the target timezone.
     *
     * <p>The walk starts at the truncated-to-hour window start in the target
     * timezone and steps forward one hour at a time. At each step the current
     * ZonedDateTime is formatted as "HH:00" and compared to the requested label.
     * The first match within the window is used.
     *
     * <p>Why truncate before walking:
     * The window start has a non-zero minute component (e.g. 16:27 PKT).
     * Truncating to the hour gives us 16:00 PKT as the first candidate.
     * This is correct because the chart also produces hour slots at whole
     * hours -- the 16:00 slot covers 16:00:00 to 16:59:59.
     *
     * <p>Why at most MAX_WALK_STEPS steps:
     * A 24h window produces 24 slots (one per hour). We walk at most 25 steps
     * (one extra for safety) to avoid an infinite loop if the label is not
     * found (e.g. DST transition produces a 23-slot or 25-slot day).
     *
     * <p>Returns null when the label is not found within the window.
     * The controller treats null as a 404 response.
     *
     * @param hourLabel   "HH:00" label as shown on the chart e.g. "15:00"
     * @param zoneId      target timezone
     * @param windowHours number of hours in the look-back window
     * @return two-element array [epochFrom, epochTo] or null if not found
     */
    private long[] resolveEpochBoundsForHour(
            String hourLabel, ZoneId zoneId, int windowHours) {

        long windowStartMs = System.currentTimeMillis() - (long) windowHours * MS_PER_HOUR;

        // Convert window start to the target timezone and truncate to the hour.
        // Example: 11:30 UTC in Asia/Karachi = 16:30 PKT, truncated = 16:00 PKT.
        ZonedDateTime cursor = Instant.ofEpochMilli(windowStartMs)
                .atZone(zoneId)
                .truncatedTo(ChronoUnit.HOURS);

        for (int step = 0; step < MAX_WALK_STEPS; step++) {
            String candidateLabel = cursor.format(HOUR_FORMATTER);

            if (candidateLabel.equals(hourLabel)) {
                // Found the matching slot.
                // toInstant().toEpochMilli() gives the UTC epoch ms for this
                // timezone-local hour boundary regardless of the offset.
                long epochFrom = cursor.toInstant().toEpochMilli();
                long epochTo = epochFrom + MS_PER_HOUR_MINUS_ONE;
                return new long[]{epochFrom, epochTo};
            }

            // Step forward one hour in the target timezone.
            // ZonedDateTime.plusHours handles DST transitions correctly --
            // if clocks spring forward, one hour step may cross two clock hours.
            cursor = cursor.plusHours(1);
        }

        return null;
    }

    /**
     * Builds the OpenSearch query for documents within the epoch bounds
     * matching the requested sources.
     *
     * <p>When sources is non-empty a terms filter on source is added.
     * Source values must be fully qualified class names matching what is stored
     * in the index. Example:
     * "com.aidentx.core.security.filter.GatewayPermissionFilter"
     *
     * <p>Query shape with sources:
     * <pre>
     * {
     *   "size": size,
     *   "from": page * size,
     *   "track_total_hits": true,
     *   "sort": [{ "@timestamp": { "order": "desc" } }],
     *   "query": {
     *     "bool": {
     *       "must": [
     *         { "range": { "@timestamp": { "gte": dateFrom, "lte": dateTo } } },
     *         { "terms": { "source": ["source1", "source2"] } }
     *       ]
     *     }
     *   }
     * }
     * </pre>
     *
     * <p>Query shape without sources (all sources):
     * <pre>
     * {
     *   "size": size,
     *   "from": page * size,
     *   "track_total_hits": true,
     *   "sort": [{ "@timestamp": { "order": "desc" } }],
     *   "query": {
     *     "range": { "@timestamp": { "gte": dateFrom, "lte": dateTo } }
     *   }
     * }
     * </pre>
     *
     * @param dateFrom UTC epoch ms start of hour slot (inclusive)
     * @param dateTo   UTC epoch ms end of hour slot (inclusive)
     * @param sources  list of source values; empty = no filter
     * @param page     zero-based page number
     * @param size     page size
     * @return full OpenSearch request body
     */
    private Map<String, Object> buildSearchQuery(
            long dateFrom,
            long dateTo,
            List<String> sources,
            int page,
            int size) {

        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- the error volume chart only counts ERROR+WARN events.
        // Entries returned here must use the same filter so totalElements matches
        // the count shown in the chart bar that was clicked.
        mustClauses.add(Map.of("terms", Map.of(
                FIELD_LEVEL_KEYWORD,
                List.of(LEVEL_ERROR, LEVEL_WARN))));

        // Time range filter -- the hour slot bounds resolved from the clicked label
        mustClauses.add(Map.of("range", Map.of(
                FIELD_TIMESTAMP, Map.of(
                        "gte", dateFrom,
                        "lte", dateTo))));

        // Source filter -- only when specific sources were clicked on the chart
        if (sources != null && !sources.isEmpty()) {
            mustClauses.add(Map.of(
                    "terms", Map.of(FIELD_SOURCE_KEYWORD, sources)));
        }

        Map<String, Object> query = new HashMap<>();
        query.put("size", size);
        query.put("from", (long) page * size);
        query.put("track_total_hits", true);
        query.put("sort", List.of(Map.of(FIELD_TIMESTAMP, Map.of("order", "desc"))));
        query.put("query", Map.of("bool", Map.of("must", mustClauses)));

        return query;
    }

    /**
     * Extracts LogEntry objects from the hits.hits array of the OpenSearch response.
     * Document ID is read from hit._id, all content fields from hit._source.
     *
     * @param result the full OpenSearch response
     * @return list of LogEntry instances
     */
    private List<LogEntry> extractLogEntries(JsonNode result) {
        List<LogEntry> entries = new ArrayList<>();
        JsonNode hits = result.path("hits").path("hits");

        if (hits.isMissingNode() || !hits.isArray()) {
            return entries;
        }

        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            LogEntry entry = new LogEntry();
            entry.setId(hit.path("_id").asText(""));
            entry.setTimestamp(source.path(FIELD_TIMESTAMP).asLong(0L));
            entry.setLevel(source.path("level").asText(""));
            entry.setSource(source.path("source").asText(""));
            entry.setMessage(source.path("message").asText(""));
            entry.setCategory(source.path("category").asText(null));
            entry.setCorrelationId(source.path("correlationId").asText(null));
            entry.setMicroservice(source.path("microservice").asText(null));
            entries.add(entry);
        }

        return entries;
    }

    /**
     * Builds PaginationMetadata from total hits and page parameters.
     *
     * @param page          zero-based current page
     * @param size          page size
     * @param totalElements total matching document count
     * @return populated pagination metadata
     */
    private PaginationMetadata buildPagination(int page, int size, long totalElements) {
        int totalPages = size > 0
                ? (int) Math.ceil((double) totalElements / size)
                : 0;

        PaginationMetadata meta = new PaginationMetadata();
        meta.setPage(page);
        meta.setSize(size);
        meta.setTotalElements(totalElements);
        meta.setTotalPages(totalPages);
        meta.setHasNext(page < totalPages - 1);
        meta.setHasPrevious(page > 0);
        return meta;
    }

    /**
     * Returns an empty response for cases where no matching slot was found
     * or OpenSearch returned null.
     *
     * @param hour     the requested hour label
     * @param timezone the effective timezone string
     * @param dateFrom resolved epoch from (0 when resolution failed)
     * @param dateTo   resolved epoch to (0 when resolution failed)
     * @param sources  the requested source filter
     * @param page     current page
     * @param size     page size
     * @return empty response with zero pagination
     */
    private ErrorVolumeEntriesResponse buildEmptyResponse(
            String hour,
            String timezone,
            long dateFrom,
            long dateTo,
            List<String> sources,
            int page,
            int size) {

        return new ErrorVolumeEntriesResponse(
                hour,
                timezone,
                dateFrom,
                dateTo,
                sources,
                new ArrayList<>(),
                buildPagination(page, size, 0L));
    }
}