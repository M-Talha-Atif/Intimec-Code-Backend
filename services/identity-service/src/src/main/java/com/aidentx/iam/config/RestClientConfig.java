package com.aidentx.iam.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST client settings in the Identities Service.
 * 
 * <p>This configuration class provides comprehensive REST client setup for
 * communication with external services, particularly the core-service for
 * schema definition retrieval and field classification operations.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Configurable connection and read timeouts for reliable service communication</li>
 *   <li>Optimized HTTP client factory with appropriate timeout settings</li>
 *   <li>RestTemplate bean configuration for service-to-service communication</li>
 *   <li>Comprehensive logging for monitoring and debugging REST operations</li>
 * </ul>
 * 
 * <p>The configuration supports external property-based timeout customization
 * through application properties, enabling environment-specific tuning for
 * different deployment scenarios (development, staging, production).</p>
 *
 * @author aidentx Development Team
 * @since 1.0
 * @see RestTemplate
 * @see SimpleClientHttpRequestFactory
 */
@Configuration
public class RestClientConfig {

    private final IUnifiedLogger logger;

    @Value("${core-service.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${core-service.read.timeout:10000}")
    private int readTimeout;

    /**
     * Constructs a new REST client configuration with unified logging support.
     * 
     * @param loggerFactory the unified logger factory for creating structured loggers
     */
    public RestClientConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger("RestClientConfig", AppConstants.LOG_CATEGORY_REST_CLIENT);
        this.logger.debug().message(AppConstants.LOG_DEBUG_CONFIGURATION_VALUE).detail("component", "RestClientConfig initialized").log();
    }

    /**
     * Creates a configured RestTemplate bean for HTTP communication with external services.
     * 
     * <p>Configures a RestTemplate with optimized timeout settings for reliable
     * service-to-service communication. The configuration includes:</p>
     * 
     * <ul>
     *   <li>Connection timeout for establishing HTTP connections</li>
     *   <li>Read timeout for receiving response data</li>
     *   <li>Simple HTTP request factory for lightweight client operations</li>
     * </ul>
     * 
     * <p>Timeout values are configurable through application properties to support
     * different environments and network conditions. The RestTemplate is primarily
     * used for core-service integration to retrieve schema definitions and perform
     * field classification operations.</p>
     * 
     * @return Configured RestTemplate instance ready for service communication
     */
    @Bean
    public RestTemplate restTemplate() {
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY).detail("method", "restTemplate").log();
        
        logger.info().message(AppConstants.LOG_REST_CLIENT_CONFIG_INITIALIZED).log();
        
        logger.debug().message(AppConstants.LOG_REST_CLIENT_CONFIG_DETAILS)
                .detail("connectionTimeout", connectionTimeout)
                .detail("readTimeout", readTimeout)
                .log();
        
        logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP).detail("step", "Creating HTTP request factory").log();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP).detail("step", "Configuring connection timeout").log();
        factory.setConnectTimeout(connectionTimeout);
        
        logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP).detail("step", "Configuring read timeout").log();
        factory.setReadTimeout(readTimeout);
        
        logger.debug().message(AppConstants.LOG_REST_CLIENT_FACTORY_CREATED)
                .detail("connectionTimeout", connectionTimeout)
                .detail("readTimeout", readTimeout)
                .log();
        
        logger.debug().message(AppConstants.LOG_DEBUG_PROCESSING_STEP).detail("step", "Creating RestTemplate instance").log();
        RestTemplate restTemplate = new RestTemplate(factory);
        
        logger.info().message(AppConstants.LOG_REST_CLIENT_RESTTEMPLATE_CREATED)
                .detail("connectionTimeoutMs", connectionTimeout)
                .detail("readTimeoutMs", readTimeout)
                .log();
        
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_EXIT).detail("method", "restTemplate").log();
        
        return restTemplate;
    }
}
