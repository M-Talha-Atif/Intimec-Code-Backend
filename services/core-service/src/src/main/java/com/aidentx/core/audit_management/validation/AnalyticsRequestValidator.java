package com.aidentx.core.audit_management.validation;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventDistributionConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsKpisConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsRangeConstants;

import java.time.Instant;

/**
 * Stateless validation utility for analytics endpoint request parameters.
 *
 * <p>Each method performs one focused check and throws
 * {@link IllegalArgumentException} on violation. The global exception handler
 * is expected to map that to HTTP 400 with the message as the response body.
 *
 * <p>Example controller usage:
 * <pre>
 *   AnalyticsRequestValidator.validateTenantId(tenantId);
 *   AnalyticsRequestValidator.validateRange(range, dateFrom, dateTo);
 *   AnalyticsRequestValidator.validateDateRange(dateFrom, dateTo);
 * </pre>
 */
public final class AnalyticsRequestValidator {

    private AnalyticsRequestValidator() {
        // Utility class -- no instantiation
    }

    /**
     * Verifies that the tenant identifier extracted from the JWT is present.
     *
     * <p>A blank tenantId reaching the service layer would cause all COUNT
     * queries to run without a tenant filter, which is a data-isolation
     * violation. This guard makes that failure explicit at the boundary.
     *
     * @param tenantId value extracted from the JWT claim; may be null
     * @throws IllegalArgumentException when tenantId is null or blank
     */
    public static void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                    AnalyticsKpisConstants.VALIDATION_TENANT_ID_BLANK);
        }
    }

    /**
     * Verifies that the supplied date range is logically ordered.
     *
     * <p>Validation is only applied when both parameters are present.
     * A one-sided window (only dateFrom or only dateTo) is a valid
     * open-ended query and is not rejected here.
     *
     * <p>This method should only be called after {@link #validateRange}
     * confirms that {@code range=all} is not active. When {@code range=all}
     * is present, date parameters are ignored entirely and this check is
     * skipped in the controller.
     *
     * @param dateFrom window start, inclusive; may be null
     * @param dateTo   window end, inclusive; may be null
     * @throws IllegalArgumentException when both are present and dateFrom is after dateTo
     */
    public static void validateDateRange(Instant dateFrom, Instant dateTo) {
        if (dateFrom == null || dateTo == null) {
            return;
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException(
                    AnalyticsKpisConstants.VALIDATION_DATE_FROM_AFTER_DATE_TO);
        }
    }

    /**
     * Validates the {@code range} query parameter and its interaction with
     * {@code dateFrom} and {@code dateTo}.
     *
     * <p>Accepted values:
     * <ul>
     *   <li>null -- range parameter was omitted; windowed behaviour applies</li>
     *   <li>{@link AnalyticsRangeConstants#RANGE_ALL} ("all") -- disables date
     *       filtering; must not be combined with dateFrom or dateTo</li>
     * </ul>
     *
     * <p>Any other non-null value is rejected with an explicit error message
     * listing the supported values.
     *
     * <p>Combining {@code range=all} with date parameters is rejected rather
     * than silently ignoring the dates. Silent coercion would hide caller
     * mistakes and make the API behaviour harder to reason about.
     *
     * @param range    value of the range query parameter; may be null
     * @param dateFrom value of the dateFrom query parameter; may be null
     * @param dateTo   value of the dateTo query parameter; may be null
     * @throws IllegalArgumentException when range is an unrecognised value, or
     *                                  when range=all is combined with date parameters
     */
    public static void validateRange(String range, Instant dateFrom, Instant dateTo) {
        if (range == null) {
            return;
        }

        if (!range.equalsIgnoreCase(AnalyticsRangeConstants.RANGE_ALL)) {
            throw new IllegalArgumentException(
                    AnalyticsRangeConstants.VALIDATION_RANGE_UNKNOWN);
        }

        if (dateFrom != null || dateTo != null) {
            throw new IllegalArgumentException(
                    AnalyticsRangeConstants.VALIDATION_RANGE_ALL_WITH_DATES);
        }
    }

    /**
     * Validates the limit parameter for endpoints that support result-count capping.
     *
     * <p>Valid values:
     * <ul>
     *   <li>null -- caller omitted the parameter; default will be applied by the service</li>
     *   <li>0 -- return all results (sentinel for no limit)</li>
     *   <li>Any integer >= 1 -- return that many results</li>
     * </ul>
     *
     * @param limit caller-supplied limit; may be null
     * @throws IllegalArgumentException when limit is a negative integer
     */
    public static void validateLimit(Integer limit) {
        if (limit == null) {
            return;
        }
        if (limit < AnalyticsEventDistributionConstants.LIMIT_ALL) {
            throw new IllegalArgumentException(
                    AnalyticsEventDistributionConstants.VALIDATION_LIMIT_INVALID);
        }
    }
}