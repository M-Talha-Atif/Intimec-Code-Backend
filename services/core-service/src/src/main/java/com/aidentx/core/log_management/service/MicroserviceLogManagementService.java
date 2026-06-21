package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.ComponentDebugInfo;
import com.aidentx.core.log_management.model.dto.DebugConfigurationResponse;

/**
 * Service contract for managing log levels across all registered microservices.
 *
 * <p>Supports three operations:
 * <ol>
 *   <li>Retrieve current debug configuration for all components.</li>
 *   <li>Update the log level for one component, a list of components, or all components.</li>
 *   <li>Query the current debug state for a single named component.</li>
 * </ol>
 *
 * <p>Component resolution behaviour:
 * <ul>
 *   <li>Components with {@code serviceType = "core"} are resolved locally via
 *       {@link com.aidentx.core.log_management.service.LogLevelService}.</li>
 *   <li>All other service types are resolved remotely via HTTP using the
 *       configured gateway URL for that service type.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface MicroserviceLogManagementService {

    /**
     * Retrieves the current debug configuration for every registered microservice component.
     *
     * <p>Each component entry includes its name, description, current log level,
     * service URL, and whether it is active (i.e. log level is not {@code OFF}).
     *
     * <p>Example response structure:
     * <pre>
     * {
     *   "totalComponents": 2,
     *   "components": [
     *     { "name": "Authentication", "level": "INFO",  "active": true,  "serviceUrl": "http://auth:8080" },
     *     { "name": "Core Service",   "level": "DEBUG", "active": true,  "serviceUrl": "http://core:8081" }
     *   ]
     * }
     * </pre>
     *
     * @return populated {@link DebugConfigurationResponse} containing all component debug states
     */
    DebugConfigurationResponse getDebugConfiguration();

    /**
     * Updates the log level for one or more microservice components.
     *
     * <p>Accepts three request shapes at runtime:
     * <ul>
     *   <li>{@link com.aidentx.core.log_management.model.dto.ComponentLogLevelUpdateRequest}
     *       -- single component update.</li>
     *   <li>{@link java.util.List} of
     *       {@link com.aidentx.core.log_management.model.dto.ComponentLogLevelUpdateRequest}
     *       -- bulk update list.</li>
     *   <li>{@link com.aidentx.core.log_management.model.dto.BulkLogLevelUpdateRequest}
     *       -- wrapper object containing a {@code components} list.</li>
     *   <li>{@link java.util.Map} with a {@code componentName} key
     *       -- treated as a single component update in map form.</li>
     * </ul>
     *
     * <p>When {@code componentName} is {@code "all"} (case-insensitive), the update
     * is applied globally to every registered component via
     * {@link #updateLoggingLevelGlobally(String)}.
     *
     * @param request the log level update payload; may be a single request, a list,
     *                a bulk wrapper, or a raw map
     * @return {@code true} if every component update succeeded; {@code false} if any
     *         individual update failed
     */
    boolean updateComponentLogLevels(Object request);

    /**
     * Retrieves the current debug information for a single named component.
     *
     * <p>Example: {@code getComponentDebugInfo("Authentication")} returns the
     * live log level and active status fetched from the auth service.
     *
     * @param componentName the exact display name of the component (case-sensitive),
     *                      e.g. {@code "Authentication"} or {@code "Core Service"}
     * @return the {@link ComponentDebugInfo} for the component, or {@code null}
     *         when no component with the given name is registered
     */
    ComponentDebugInfo getComponentDebugInfo(String componentName);

    /**
     * Applies the given log level to every registered microservice component.
     *
     * <p>Iterates all component configurations and delegates each to
     * {@code updateComponentLogLevel}. A single component failure does not
     * abort the remaining updates; the final return value reflects whether
     * all components succeeded.
     *
     * <p>Valid level values follow Logback/Log4j2 conventions:
     * {@code TRACE}, {@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR}, {@code OFF}.
     *
     * @param level the target log level string to apply globally, e.g. {@code "DEBUG"}
     * @return {@code true} if every component was updated successfully;
     *         {@code false} if one or more components failed
     */
    boolean updateLoggingLevelGlobally(String level);
}