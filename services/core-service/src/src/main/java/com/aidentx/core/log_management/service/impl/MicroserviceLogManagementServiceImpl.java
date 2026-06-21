package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.operations.MicroserviceLogManagementConstants;
import com.aidentx.core.log_management.model.dto.BulkLogLevelUpdateRequest;
import com.aidentx.core.log_management.model.dto.ComponentDebugInfo;
import com.aidentx.core.log_management.model.dto.ComponentLogLevelUpdateRequest;
import com.aidentx.core.log_management.model.dto.DebugConfigurationResponse;
import com.aidentx.core.log_management.model.dto.LogLevelRequest;
import com.aidentx.core.log_management.service.LogLevelService;
import com.aidentx.core.log_management.service.MicroserviceLogManagementService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link MicroserviceLogManagementService}.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>On construction, all gateway URLs are injected from application properties
 *       and the full component registry is initialised with all 11 services.</li>
 *   <li>For read operations, each component is resolved either locally
 *       (serviceType = "core") or remotely (HTTP GET to the service's
 *       {@code /api/v1/logs/level} endpoint).</li>
 *   <li>For write operations, the request is normalised to a
 *       {@link ComponentLogLevelUpdateRequest} list, then each item is
 *       dispatched locally or via HTTP POST.</li>
 * </ol>
 *
 * <p>Registered components and their service types:
 * <pre>
 * | Component                     | Service Type            | Dispatch   |
 * |-------------------------------|-------------------------|------------|
 * | Core Service                  | core                    | local      |
 * | Authentication                | auth                    | remote     |
 * | Workflow Service              | workflow                | remote     |
 * | Application Permissions Svc   | application-permissions | remote     |
 * | Connector Runtime Service     | connector-runtime       | remote     |
 * | Configuration Service         | configuration           | remote     |
 * | Access Portal Service         | access-portal           | remote     |
 * | Identity Service              | identity                | remote     |
 * | LLM Service                   | llm                     | remote     |
 * | Task Management Service       | task-management         | remote     |
 * | API Gateway                   | api-gateway             | remote     |
 * </pre>
 *
 * <p>Failure behaviour: individual component failures are caught, logged at ERROR
 * level, and recorded as {@code false} in the aggregate success flag. They never
 * propagate as unchecked exceptions to callers.
 *
 * @author aidentx
 * @since 1.0
 */
@Service
public class MicroserviceLogManagementServiceImpl implements MicroserviceLogManagementService {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final IUnifiedLogger logger;
    private final RestTemplate restTemplate;
    private final LogLevelService localLogLevelService;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Injected gateway URLs
    // -------------------------------------------------------------------------

    /**
     * Base URL for the auth microservice.
     * Injected from {@code gateway.auth.service.url}.
     */
    @Value("${gateway.auth.service.url}")
    private String authServiceUrl;

    /**
     * Base URL for the core microservice.
     * Injected from {@code gateway.core.service.url}.
     */
    @Value("${gateway.core.service.url}")
    private String coreServiceUrl;

    /**
     * Base URL for the workflows microservice.
     * Injected from {@code gateway.workflow.service.url}.
     */
    @Value("${gateway.workflow.service.url}")
    private String workflowServiceUrl;

    /**
     * Base URL for the application-permissions microservice.
     * Injected from {@code gateway.application-permissions.service.url}.
     */
    @Value("${gateway.application-permissions.service.url}")
    private String applicationPermissionsServiceUrl;

    /**
     * Base URL for the connector-runtime microservice.
     * Injected from {@code gateway.connector-runtime.service.url}.
     */
    @Value("${gateway.connector-runtime.service.url}")
    private String connectorRuntimeServiceUrl;

    /**
     * Base URL for the configuration microservice.
     * Injected from {@code gateway.configuration.service.url}.
     */
    @Value("${gateway.configuration.service.url}")
    private String configurationServiceUrl;

    /**
     * Base URL for the access-portal microservice.
     * Injected from {@code gateway.access-portal.service.url}.
     */
    @Value("${gateway.access-portal.service.url}")
    private String accessPortalServiceUrl;

    /**
     * Base URL for the identity microservice.
     * Injected from {@code gateway.identity.service.url}.
     */
    @Value("${gateway.identity.service.url}")
    private String identityServiceUrl;

    /**
     * Base URL for the LLM microservice.
     * Injected from {@code gateway.llm.service.url}.
     */
    @Value("${gateway.llm.service.url}")
    private String llmServiceUrl;

    /**
     * Base URL for the task-management microservice.
     * Injected from {@code gateway.task-management.service.url}.
     */
    @Value("${gateway.task-management.service.url}")
    private String taskManagementServiceUrl;

    /**
     * Base URL for the API gateway.
     * Injected from {@code gateway.api-gateway.url}.
     */
    @Value("${api.gateway.url}")
    private String apiGatewayUrl;

    // -------------------------------------------------------------------------
    // Component registry
    // -------------------------------------------------------------------------

    /**
     * Full registry of all managed microservice components across the platform.
     *
     * <p>Each entry maps a human-readable component name to its service type
     * and logger name. Adding a new service requires: (1) a new property key,
     * (2) a new {@code @Value} field above, (3) a new entry here, and
     * (4) a new case in {@link #resolveServiceBaseUrl(String)}.
     *
     * <p>All components use the {@code ROOT} logger for uniform level control.
     */
    private final List<ComponentConfig> componentConfigs = Arrays.asList(
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_CORE_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_CORE_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_CORE),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_AUTH_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_AUTH_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_AUTH),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_WORKFLOW_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_WORKFLOW_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_WORKFLOW),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_APPLICATION_PERMISSIONS_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_APPLICATION_PERMISSIONS_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_APPLICATION_PERMISSIONS),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_CONNECTOR_RUNTIME_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_CONNECTOR_RUNTIME_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_CONNECTOR_RUNTIME),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_CONFIGURATION_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_CONFIGURATION_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_CONFIGURATION),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_ACCESS_PORTAL_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_ACCESS_PORTAL_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_ACCESS_PORTAL),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_IDENTITY_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_IDENTITY_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_IDENTITY),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_LLM_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_LLM_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_LLM),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_TASK_MANAGEMENT_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_TASK_MANAGEMENT_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_TASK_MANAGEMENT),
            new ComponentConfig(
                    MicroserviceLogManagementConstants.COMPONENT_API_GATEWAY_NAME,
                    MicroserviceLogManagementConstants.COMPONENT_API_GATEWAY_DESCRIPTION,
                    MicroserviceLogManagementConstants.SERVICE_TYPE_API_GATEWAY));

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs the service, wires dependencies, and logs startup configuration.
     *
     * <p>Note: all {@code @Value} fields are {@code null} at construction time
     * because Spring injects them after the constructor returns. The startup log
     * lines referencing those fields will print {@code null}; this is expected
     * Spring behaviour and does not affect runtime operation.
     *
     * @param restTemplate         HTTP client used for remote service calls
     * @param localLogLevelService in-process log level service for the core component
     * @param objectMapper         Jackson mapper used to normalise incoming request objects
     * @param loggerFactory        factory used to obtain the {@link IUnifiedLogger} instance
     */
    public MicroserviceLogManagementServiceImpl(
            RestTemplate restTemplate,
            LogLevelService localLogLevelService,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {

        this.restTemplate = restTemplate;
        this.localLogLevelService = localLogLevelService;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(
                MicroserviceLogManagementConstants.LOGGER_NAME);

        logger.info()
                .message(MicroserviceLogManagementConstants.LOG_SERVICE_INITIALIZED)
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_AUTH_SERVICE_URL, authServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CORE_SERVICE_URL, coreServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_WORKFLOW_SERVICE_URL, workflowServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_APPLICATION_PERMISSIONS_SERVICE_URL,
                        applicationPermissionsServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CONNECTOR_RUNTIME_SERVICE_URL,
                        connectorRuntimeServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CONFIGURATION_SERVICE_URL,
                        configurationServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_ACCESS_PORTAL_SERVICE_URL,
                        accessPortalServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_IDENTITY_SERVICE_URL, identityServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_LLM_SERVICE_URL, llmServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_TASK_MANAGEMENT_SERVICE_URL,
                        taskManagementServiceUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_API_GATEWAY_URL, apiGatewayUrl))
                .log();
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CONFIGURED_COMPONENTS,
                        componentConfigs.size()))
                .log();

        for (ComponentConfig config : componentConfigs) {
            logger.debug()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_COMPONENT_TYPE_LOGGER,
                            config.name, config.serviceType, config.loggerName))
                    .log();
        }
    }

    // -------------------------------------------------------------------------
    // Public API -- read
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Iterates every entry in {@link #componentConfigs}, resolves each
     * component's current log level (locally or remotely), and aggregates
     * the results into a {@link DebugConfigurationResponse}.
     *
     * @return fully populated {@link DebugConfigurationResponse}
     */
    @Override
    public DebugConfigurationResponse getDebugConfiguration() {
        logger.info()
                .message(MicroserviceLogManagementConstants.LOG_GETTING_DEBUG_CONFIG)
                .log();

        List<ComponentDebugInfo> components = new ArrayList<>();
        int activeComponents = 0;

        for (ComponentConfig config : componentConfigs) {
            logger.debug()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_PROCESSING_COMPONENT,
                            config.name, config.serviceType))
                    .log();

            ComponentDebugInfo info = resolveComponentDebugInfo(config);
            components.add(info);

            if (info.isActive()) {
                activeComponents++;
            }

            logger.debug()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_COMPONENT_LEVEL_ACTIVE,
                            config.name, info.getLevel(), info.isActive()))
                    .log();
        }

        DebugConfigurationResponse response = new DebugConfigurationResponse();
        response.setComponents(components);
        response.setTotalComponents(componentConfigs.size());

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_DEBUG_CONFIG_COMPLETE,
                        activeComponents, componentConfigs.size()))
                .log();

        return response;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #resolveComponentDebugInfo(ComponentConfig)} after
     * looking up the component by name. Returns {@code null} for unknown names.
     *
     * @param componentName exact display name of the component (case-sensitive)
     * @return {@link ComponentDebugInfo} or {@code null} when not found
     */
    @Override
    public ComponentDebugInfo getComponentDebugInfo(String componentName) {
        ComponentConfig config = findComponentConfig(componentName);
        if (config == null) {
            return null;
        }
        return resolveComponentDebugInfo(config);
    }

    // -------------------------------------------------------------------------
    // Public API -- write
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Request normalisation order:
     * <ol>
     *   <li>If already a {@link ComponentLogLevelUpdateRequest} -- dispatch directly.</li>
     *   <li>If a {@link Map} containing key {@code componentName} -- convert and dispatch.</li>
     *   <li>If a {@link List} -- convert each element and dispatch all.</li>
     *   <li>Otherwise -- attempt conversion to {@link BulkLogLevelUpdateRequest}
     *       and dispatch its {@code components} list.</li>
     * </ol>
     *
     * @param request polymorphic update payload
     * @return {@code true} if all updates succeeded; {@code false} otherwise
     */
    @Override
    public boolean updateComponentLogLevels(Object request) {
        try {
            if (request instanceof ComponentLogLevelUpdateRequest) {
                return dispatchComponentUpdate((ComponentLogLevelUpdateRequest) request);
            }

            if (request instanceof Map) {
                Map<?, ?> requestMap = (Map<?, ?>) request;
                if (requestMap.containsKey(MicroserviceLogManagementConstants.MAP_KEY_COMPONENT_NAME)) {
                    ComponentLogLevelUpdateRequest singleRequest =
                            objectMapper.convertValue(request, ComponentLogLevelUpdateRequest.class);
                    return dispatchComponentUpdate(singleRequest);
                }
            }

            List<ComponentLogLevelUpdateRequest> requests = new ArrayList<>();

            if (request instanceof List) {
                List<?> requestList = (List<?>) request;
                for (Object item : requestList) {
                    requests.add(objectMapper.convertValue(item, ComponentLogLevelUpdateRequest.class));
                }
            } else {
                BulkLogLevelUpdateRequest bulkRequest =
                        objectMapper.convertValue(request, BulkLogLevelUpdateRequest.class);
                requests = bulkRequest.getComponents();
            }

            boolean allSuccessful = true;
            for (ComponentLogLevelUpdateRequest componentRequest : requests) {
                if (!dispatchComponentUpdate(componentRequest)) {
                    allSuccessful = false;
                }
            }
            return allSuccessful;

        } catch (Exception e) {
            logger.error(e)
                    .message(MicroserviceLogManagementConstants.LOG_ERROR_NORMALISING_REQUEST)
                    .log();
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates every {@link ComponentConfig} and builds a synthetic
     * {@link ComponentLogLevelUpdateRequest} for each, then delegates to
     * {@link #dispatchComponentUpdate(ComponentLogLevelUpdateRequest)}.
     *
     * @param level target log level string, e.g. {@code "DEBUG"}
     * @return {@code true} if all components were updated; {@code false} otherwise
     */
    @Override
    public boolean updateLoggingLevelGlobally(String level) {
        boolean allSuccessful = true;
        for (ComponentConfig config : componentConfigs) {
            ComponentLogLevelUpdateRequest request = new ComponentLogLevelUpdateRequest();
            request.setComponentName(config.name);
            request.setLoggerName(config.loggerName);
            request.setLevel(level);
            if (!dispatchComponentUpdate(request)) {
                allSuccessful = false;
            }
        }
        return allSuccessful;
    }

    // -------------------------------------------------------------------------
    // Private -- resolution (read path)
    // -------------------------------------------------------------------------

    /**
     * Resolves the current log level and active status for a single component.
     *
     * <p>Resolution strategy:
     * <ul>
     *   <li>{@code serviceType = "core"} -- reads from {@link LogLevelService}
     *       in-process; no HTTP call is made.</li>
     *   <li>Any other {@code serviceType} -- issues an HTTP GET to
     *       {@code <serviceBaseUrl>/api/v1/logs/level}.</li>
     * </ul>
     *
     * <p>A component is considered active when its resolved level is not {@code "OFF"}.
     * On any exception, returns a fallback {@link ComponentDebugInfo} with
     * level {@code "UNKNOWN"} and {@code active = false}.
     *
     * @param config the component configuration entry to resolve
     * @return resolved {@link ComponentDebugInfo}; never {@code null}
     */
    private ComponentDebugInfo resolveComponentDebugInfo(ComponentConfig config) {
        logger.debug()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_DEBUG_INFO_FOR_COMPONENT,
                        config.name, config.serviceType, config.loggerName))
                .log();

        try {
            String level;

            if (MicroserviceLogManagementConstants.SERVICE_TYPE_CORE.equals(config.serviceType)) {
                level = resolveLocalLogLevel(config);
            } else {
                level = resolveRemoteLogLevel(config);
            }

            boolean active = !MicroserviceLogManagementConstants.LEVEL_OFF.equals(level);
            String serviceUrl = resolveServiceBaseUrl(config.serviceType);

            ComponentDebugInfo result = new ComponentDebugInfo(
                    config.name, config.description, level, serviceUrl, active);

            logger.debug()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_COMPONENT_FINAL_RESULT,
                            config.name, level, active))
                    .log();

            return result;

        } catch (Exception e) {
            logger.error(e)
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_ERROR_GETTING_DEBUG_INFO,
                            config.name, e.getMessage()))
                    .log();

            return new ComponentDebugInfo(
                    config.name,
                    config.description,
                    MicroserviceLogManagementConstants.LEVEL_UNKNOWN,
                    resolveServiceBaseUrl(config.serviceType),
                    false);
        }
    }

    /**
     * Resolves the current log level for a core (local) component.
     *
     * <p>Fetches the full level map from {@link LogLevelService} and returns
     * the value for {@link ComponentConfig#loggerName}, falling back to the
     * {@code ROOT} entry if the named logger is absent.
     *
     * @param config the component to resolve (must have {@code serviceType = "core"})
     * @return resolved level string; {@code "UNKNOWN"} when neither the logger
     *         name nor ROOT is present in the map
     */
    private String resolveLocalLogLevel(ComponentConfig config) {
        logger.debug()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_GETTING_LOCAL_LOG_LEVEL,
                        config.name))
                .log();

        Map<String, String> levels = localLogLevelService.getCurrentLogLevels();
        String level = levels.getOrDefault(
                config.loggerName,
                levels.getOrDefault(
                        MicroserviceLogManagementConstants.LOGGER_ROOT,
                        MicroserviceLogManagementConstants.LEVEL_UNKNOWN));

        logger.debug()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_LOCAL_COMPONENT_LEVEL,
                        config.name, level))
                .log();

        return level;
    }

    /**
     * Resolves the current log level for a remote component via HTTP GET.
     *
     * <p>Constructs the endpoint URL as {@code <serviceBaseUrl>/api/v1/logs/level},
     * issues a GET, and extracts the level for {@link ComponentConfig#loggerName}
     * from the returned {@code Map<String, String>}. Falls back to {@code ROOT}
     * and then to {@code "INFO"} when the named logger is absent.
     *
     * <p>Returns {@code "UNKNOWN"} when the service URL cannot be resolved or
     * the HTTP response is not 2xx.
     *
     * @param config the component to query (must have a non-core serviceType)
     * @return resolved level string; {@code "UNKNOWN"} on failure
     */
    private String resolveRemoteLogLevel(ComponentConfig config) {
        String serviceUrl = resolveServiceBaseUrl(config.serviceType);

        if (serviceUrl == null || serviceUrl.isEmpty()) {
            logger.error()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_SERVICE_URL_NOT_CONFIGURED,
                            config.name))
                    .log();
            return MicroserviceLogManagementConstants.LEVEL_UNKNOWN;
        }

        String endpointUrl = serviceUrl + (MicroserviceLogManagementConstants.SERVICE_TYPE_API_GATEWAY
        .equals(config.serviceType)
        ? MicroserviceLogManagementConstants.LOGS_ENDPOINT_API_GATEWAY
        : MicroserviceLogManagementConstants.LOGS_ENDPOINT);

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CALLING_SERVICE_FOR_COMPONENT,
                        endpointUrl, config.name))
                .log();

        ParameterizedTypeReference<Map<String, String>> typeRef =
                new ParameterizedTypeReference<Map<String, String>>() {};

        ResponseEntity<Map<String, String>> response =
                restTemplate.exchange(endpointUrl, HttpMethod.GET, null, typeRef);

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_SERVICE_RESPONSE_STATUS,
                        response.getStatusCode(), response.getBody() != null))
                .log();

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, String> levels = response.getBody();

            logger.debug()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_SERVICE_RETURNED_LEVELS,
                            levels.size()))
                    .log();

            String level = levels.getOrDefault(
                    config.loggerName,
                    levels.getOrDefault(
                            MicroserviceLogManagementConstants.LOGGER_ROOT,
                            MicroserviceLogManagementConstants.LEVEL_INFO));

            logger.info()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_COMPONENT_LEVEL_FROM_LOGGER,
                            config.name, level, config.loggerName))
                    .log();

            return level;
        }

        logger.warn()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_SERVICE_CALL_FAILED,
                        config.name))
                .log();

        return MicroserviceLogManagementConstants.LEVEL_UNKNOWN;
    }

    // -------------------------------------------------------------------------
    // Private -- dispatch (write path)
    // -------------------------------------------------------------------------

    /**
     * Dispatches a single component log level update to the correct target.
     *
     * <p>Special case: when {@code componentName} equals {@code "all"}
     * (case-insensitive), delegates to {@link #updateLoggingLevelGlobally(String)}.
     *
     * <p>For a resolved component:
     * <ul>
     *   <li>{@code serviceType = "core"} -- delegates to
     *       {@link LogLevelService#updateLogLevel(String, String)} in-process.</li>
     *   <li>All other types -- POSTs a {@link LogLevelRequest} to the remote endpoint.</li>
     * </ul>
     *
     * @param request contains {@code componentName}, {@code loggerName}, and {@code level}
     * @return {@code true} if the update was applied successfully; {@code false} otherwise
     */
    private boolean dispatchComponentUpdate(ComponentLogLevelUpdateRequest request) {
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_UPDATING_LOG_LEVEL,
                        request.getComponentName(), request.getLevel()))
                .log();

        if (MicroserviceLogManagementConstants.COMPONENT_NAME_ALL
                .equalsIgnoreCase(request.getComponentName())) {
            return updateLoggingLevelGlobally(request.getLevel());
        }

        ComponentConfig config = findComponentConfig(request.getComponentName());
        if (config == null) {
            logger.error()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_COMPONENT_CONFIG_NOT_FOUND,
                            request.getComponentName()))
                    .log();
            return false;
        }

        try {
            if (MicroserviceLogManagementConstants.SERVICE_TYPE_CORE.equals(config.serviceType)) {
                return applyLocalLogLevelUpdate(config, request.getLevel());
            }
            return applyRemoteLogLevelUpdate(config, request.getLevel());

        } catch (Exception e) {
            logger.error(e)
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_ERROR_UPDATING_COMPONENT,
                            request.getComponentName(), e.getMessage()))
                    .log();
            return false;
        }
    }

    /**
     * Applies a log level change to the in-process core component.
     *
     * @param config the resolved core component configuration
     * @param level  target log level string, e.g. {@code "DEBUG"}
     * @return {@code true} always (exceptions propagate to the caller)
     */
    private boolean applyLocalLogLevelUpdate(ComponentConfig config, String level) {
        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_UPDATING_LOCAL_COMPONENT,
                        config.name, config.loggerName, level))
                .log();

        localLogLevelService.updateLogLevel(config.loggerName, level);

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_LOCAL_UPDATE_SUCCESS,
                        config.name))
                .log();

        return true;
    }

    /**
     * Applies a log level change to a remote microservice via HTTP POST.
     *
     * <p>Constructs the endpoint URL as {@code <serviceBaseUrl>/api/v1/logs/level},
     * builds a {@link LogLevelRequest}, and POSTs it. Returns {@code true} when
     * the response status is 2xx.
     *
     * @param config the resolved remote component configuration
     * @param level  target log level string, e.g. {@code "WARN"}
     * @return {@code true} if the remote service responded with 2xx; {@code false} otherwise
     */
    private boolean applyRemoteLogLevelUpdate(ComponentConfig config, String level) {
        String serviceBaseUrl = resolveServiceBaseUrl(config.serviceType);

        if (serviceBaseUrl == null || serviceBaseUrl.isEmpty()) {
            logger.error()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_SERVICE_URL_NOT_CONFIGURED,
                            config.name))
                    .log();
            return false;
        }

     String endpointUrl = serviceBaseUrl + (MicroserviceLogManagementConstants.SERVICE_TYPE_API_GATEWAY
        .equals(config.serviceType)
        ? MicroserviceLogManagementConstants.LOGS_ENDPOINT_API_GATEWAY
        : MicroserviceLogManagementConstants.LOGS_ENDPOINT);

        LogLevelRequest logRequest = new LogLevelRequest();
        logRequest.setLoggerName(config.loggerName);
        logRequest.setLevel(level);

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_CALLING_SERVICE_TO_UPDATE,
                        endpointUrl, config.name, config.loggerName, level))
                .log();

        ResponseEntity<String> response =
                restTemplate.postForEntity(endpointUrl, logRequest, String.class);

        logger.info()
                .message(String.format(
                        MicroserviceLogManagementConstants.LOG_SERVICE_UPDATE_RESPONSE,
                        response.getStatusCode(), response.getBody()))
                .log();

        boolean success = response.getStatusCode().is2xxSuccessful();

        if (success) {
            logger.info()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_REMOTE_UPDATE_SUCCESS,
                            config.name))
                    .log();
        } else {
            logger.error()
                    .message(String.format(
                            MicroserviceLogManagementConstants.LOG_REMOTE_UPDATE_FAILED,
                            config.name, response.getStatusCode()))
                    .log();
        }

        return success;
    }

    // -------------------------------------------------------------------------
    // Private -- utilities
    // -------------------------------------------------------------------------

    /**
     * Looks up the {@link ComponentConfig} entry for the given display name.
     *
     * @param componentName the exact display name (case-sensitive)
     * @return matching {@link ComponentConfig}, or {@code null} when absent
     */
    private ComponentConfig findComponentConfig(String componentName) {
        return componentConfigs.stream()
                .filter(config -> config.name.equals(componentName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the injected gateway base URL for the given service type.
     *
     * <p>Returns an empty string for unknown types so callers can detect the
     * missing-URL condition uniformly without a null check.
     *
     * <p>Supported service types and their corresponding property keys:
     * <pre>
     * | serviceType             | @Value property key                              |
     * |-------------------------|--------------------------------------------------|
     * | core                    | gateway.core.service.url                         |
     * | auth                    | gateway.auth.service.url                         |
     * | workflow                | gateway.workflow.service.url                     |
     * | application-permissions | gateway.application-permissions.service.url      |
     * | connector-runtime       | gateway.connector-runtime.service.url            |
     * | configuration           | gateway.configuration.service.url                |
     * | access-portal           | gateway.access-portal.service.url                |
     * | identity                | gateway.identity.service.url                     |
     * | llm                     | gateway.llm.service.url                          |
     * | task-management         | gateway.task-management.service.url              |
     * | api-gateway             | gateway.api-gateway.url                          |
     * </pre>
     *
     * @param serviceType the service type routing key
     * @return the configured gateway base URL, or an empty string when unknown
     */
    private String resolveServiceBaseUrl(String serviceType) {
        switch (serviceType) {
            case MicroserviceLogManagementConstants.SERVICE_TYPE_CORE:
                return coreServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_AUTH:
                return authServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_WORKFLOW:
                return workflowServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_APPLICATION_PERMISSIONS:
                return applicationPermissionsServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_CONNECTOR_RUNTIME:
                return connectorRuntimeServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_CONFIGURATION:
                return configurationServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_ACCESS_PORTAL:
                return accessPortalServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_IDENTITY:
                return identityServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_LLM:
                return llmServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_TASK_MANAGEMENT:
                return taskManagementServiceUrl;
            case MicroserviceLogManagementConstants.SERVICE_TYPE_API_GATEWAY:
                return apiGatewayUrl;
            default:
                return "";
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Immutable value object holding the static configuration for a single
     * managed microservice component.
     *
     * <p>Fields:
     * <ul>
     *   <li>{@code name}        -- human-readable display name.</li>
     *   <li>{@code description} -- short description shown in the debug configuration UI.</li>
     *   <li>{@code serviceType} -- routing key used to select local vs remote dispatch
     *                             and to look up the gateway URL via
     *                             {@link #resolveServiceBaseUrl(String)}.</li>
     *   <li>{@code loggerName}  -- Logback logger name targeted by level changes;
     *                             always {@code "ROOT"} for uniform control.</li>
     * </ul>
     */
    private static class ComponentConfig {

        final String name;
        final String description;
        final String serviceType;
        final String loggerName;

        /**
         * Constructs a {@link ComponentConfig} with a fixed {@code ROOT} logger name.
         *
         * @param name        display name, e.g. {@code "Identity Service"}
         * @param description short description, e.g. {@code "User identity and profile management"}
         * @param serviceType routing key, e.g. {@code "identity"}
         */
        ComponentConfig(String name, String description, String serviceType) {
            this.name = name;
            this.description = description;
            this.serviceType = serviceType;
            this.loggerName = MicroserviceLogManagementConstants.LOGGER_ROOT;
        }
    }
}