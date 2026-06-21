package com.aidentx.iam.entitlement_management.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

/**
 * Calculates the next scheduled execution time for CRON-type rules.
 *
 * <p>Supports three scheduling modes:
 * <ul>
 *   <li><b>cronExpression</b>: Standard 6-field Spring cron (e.g., {@code 0 0 2 * * ?}).
 *       Parsed by {@link CronExpression} and evaluated against the rule's timezone.</li>
 *   <li><b>scheduleTime only</b>: Daily recurrence at a specific HH:mm in the rule's timezone.
 *       If today's time has passed, advances to tomorrow.</li>
 *   <li><b>scheduleTime + scheduleDate</b>: One-time execution at a specific date and time.
 *       Returns null if the scheduled moment has already passed (rule is done).</li>
 * </ul>
 *
 * <p>All calculations respect the rule's {@code scheduleTimezone}. If no timezone
 * is configured, UTC is used as the default.
 *
 * <p>Returns epoch milliseconds for storage in the {@code next_execution_at} column,
 * or null if no future execution is applicable.
 */
@Service
public class NextExecutionCalculator {

    private final IUnifiedLogger logger;

    @Autowired
    public NextExecutionCalculator(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(getClass(), RuleEvaluationConstants.LOGGER_MODULE_NAME);
    }

    /**
     * Calculates the next execution time for a rule.
     *
     * <p>Decision tree:
     * <ol>
     *   <li>If scheduleType is not CRON → return null</li>
     *   <li>If rule is disabled → return null</li>
     *   <li>If cronExpression is present → use Spring CronExpression</li>
     *   <li>If scheduleTime + scheduleDate → one-time calculation</li>
     *   <li>If scheduleTime only → daily recurrence</li>
     *   <li>Otherwise → return null</li>
     * </ol>
     *
     * @param rule the assignment rule
     * @return epoch millis of next execution, or null if not applicable
     */
    public Long calculate(EntitlementAssignmentRule rule) {
         if (rule.getScheduleType() != EntitlementAssignmentRule.ScheduleType.CRON) {
            return null;
        }

        if (!rule.isEnabled()) {
            return null;
        }

        ZoneId zone = resolveTimezone(rule.getScheduleTimezone());

        try {
            // Priority 1: cronExpression
            if (rule.getCronExpression() != null && !rule.getCronExpression().isBlank()) {
                return calculateFromCronExpression(rule.getCronExpression(), zone, rule.getId().toString());
            }

            // Priority 2: scheduleTime + optional scheduleDate
            if (rule.getScheduleTime() != null && !rule.getScheduleTime().isBlank()) {
                if (rule.getScheduleDate() != null && !rule.getScheduleDate().isBlank()) {
                    return calculateOneTime(rule.getScheduleTime(), rule.getScheduleDate(), zone);
                }
                return calculateDailyRecurrence(rule.getScheduleTime(), zone);
            }

            logger.warn()
                    .message("CRON rule has no cronExpression or scheduleTime — cannot calculate next execution")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .log();
            return null;

        } catch (Exception e) {
            logger.error()
                    .message("Failed to calculate next execution time")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
            return null;
        }
    }

    /**
     * Calculates the next execution using a Spring cron expression.
     *
     * <p>Spring's {@link CronExpression} uses 6 fields:
     * {@code second minute hour day-of-month month day-of-week}
     *
     * <p>Example: {@code 0 0 2 * * ?} = every day at 02:00:00
     *
     * @param expression the cron expression string
     * @param zone       the timezone to evaluate in
     * @param ruleId     for logging
     * @return epoch millis of next execution, or null if expression is invalid
     */
    private Long calculateFromCronExpression(String expression, ZoneId zone, String ruleId) {
        CronExpression cron = CronExpression.parse(expression);
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime next = cron.next(now);

        if (next == null) {
            logger.warn()
                    .message("Cron expression has no future execution")
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, ruleId)
                    .detail(RuleEvaluationConstants.DETAIL_CRON_EXPRESSION, expression)
                    .log();
            return null;
        }

        long epochMs = next.atZone(zone).toInstant().toEpochMilli();

        logger.debug()
                .message("Calculated next cron execution")
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, ruleId)
                .detail(RuleEvaluationConstants.DETAIL_CRON_EXPRESSION, expression)
                .detail("nextExecution", next.toString())
                .detail("timezone", zone.getId())
                .detail("epochMs", String.valueOf(epochMs))
                .log();

        return epochMs;
    }

    /**
     * Calculates the next execution for a daily recurrence at a fixed time.
     *
     * <p>If today's scheduled time has already passed, advances to tomorrow.
     *
     * <p>Example: scheduleTime="02:00", timezone=Asia/Karachi
     * <ul>
     *   <li>Current time: 01:30 PKT → next = today 02:00 PKT</li>
     *   <li>Current time: 03:00 PKT → next = tomorrow 02:00 PKT</li>
     * </ul>
     *
     * @param scheduleTime the time in HH:mm format
     * @param zone         the timezone
     * @return epoch millis of next execution
     */
    private Long calculateDailyRecurrence(String scheduleTime, ZoneId zone) {
        LocalTime time = LocalTime.parse(scheduleTime, DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate today = LocalDate.now(zone);
        LocalDateTime candidate = LocalDateTime.of(today, time);

        if (candidate.atZone(zone).toInstant().toEpochMilli() <= Instant.now().toEpochMilli()) {
            // Already passed today, schedule for tomorrow
            candidate = LocalDateTime.of(today.plusDays(1), time);
        }

        return candidate.atZone(zone).toInstant().toEpochMilli();
    }

    /**
     * Calculates execution time for a one-time schedule (date + time).
     *
     * <p>If the scheduled moment has already passed, returns null.
     * The rule effectively becomes "done" — it won't be picked up by the poller.
     *
     * @param scheduleTime the time in HH:mm format
     * @param scheduleDate the date in yyyy-MM-dd format
     * @param zone         the timezone
     * @return epoch millis of the scheduled moment, or null if already past
     */
    private Long calculateOneTime(String scheduleTime, String scheduleDate, ZoneId zone) {
        LocalTime time = LocalTime.parse(scheduleTime, DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate date = LocalDate.parse(scheduleDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDateTime scheduled = LocalDateTime.of(date, time);

        long epochMs = scheduled.atZone(zone).toInstant().toEpochMilli();

        if (epochMs <= Instant.now().toEpochMilli()) {
            // Already passed — one-time schedule is done
            return null;
        }

        return epochMs;
    }

    /**
     * Resolves the timezone from the rule's configuration.
     * Defaults to UTC if null, blank, or invalid.
     *
     * @param timezone the timezone string (IANA format)
     * @return the resolved ZoneId
     */
    private ZoneId resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of(RuleEvaluationConstants.DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            logger.warn()
                    .message("Invalid timezone, falling back to UTC")
                    .detail("invalidTimezone", timezone)
                    .log();
            return ZoneId.of(RuleEvaluationConstants.DEFAULT_TIMEZONE);
        }
    }
}