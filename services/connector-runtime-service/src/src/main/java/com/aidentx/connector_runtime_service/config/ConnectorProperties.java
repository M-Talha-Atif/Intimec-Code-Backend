package com.aidentx.connector_runtime_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized, type-safe configuration for the connector-runtime-service.
 *
 * <p>All timeout values are in <strong>milliseconds</strong> and can be
 * overridden at deployment time via environment variables or an externalized
 * {@code application.yml}, making them tunable per environment without a
 * code change or rebuild:
 *
 * <pre>
 *   CONNECTOR_TIMEOUT_SEARCH_MS   (default 600 000 ms = 10 min)
 *   CONNECTOR_TIMEOUT_TEST_MS     (default  30 000 ms = 30 sec)
 *   CONNECTOR_TIMEOUT_CREATE_MS   (default 120 000 ms =  2 min)
 *   CONNECTOR_TIMEOUT_UPDATE_MS   (default 120 000 ms =  2 min)
 *   CONNECTOR_TIMEOUT_DELETE_MS   (default 120 000 ms =  2 min)
 * </pre>
 *
 * <p>These timeouts are applied inside {@link com.aidentx.connector_runtime_service.helper.OperationsHelper}
 * via {@code APIConfiguration.setTimeout()} on every {@link org.identityconnectors.framework.api.ConnectorFacade}
 * that this service creates.  Without them, a half-open TCP connection to the
 * source system (LDAP/AD/etc.) causes {@code ConnectorFacade.search()} to
 * block permanently, hanging the Quartz thread in task-management-service for
 * days.
 */
@ConfigurationProperties(prefix = "connector")
public record ConnectorProperties(
        String directory,
        long scanIntervalMs,
        Timeout timeout) {

    /**
     * Per-operation timeouts applied to every ConnId {@code APIConfiguration}.
     *
     * <p>The ConnId framework uses these values to cap individual operation
     * calls ({@code SearchApiOp}, {@code TestApiOp}, etc.) regardless of
     * whether the underlying connector JAR or source system stalls.
     */
    public record Timeout(
            @DefaultValue("600000") long searchMs,
            @DefaultValue("30000")  long testMs,
            @DefaultValue("120000") long createMs,
            @DefaultValue("120000") long updateMs,
            @DefaultValue("120000") long deleteMs) {
    }
}
