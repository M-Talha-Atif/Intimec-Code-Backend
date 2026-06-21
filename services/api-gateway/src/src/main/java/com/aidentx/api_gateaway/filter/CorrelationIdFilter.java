package com.aidentx.api_gateaway.filter;

import com.aidentx.api_gateaway.config.AppConstants;
import com.aidentx.logging.config.UnifiedLoggingProperties;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.logging.util.CorrelationIdGenerator;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * A reactive GlobalFilter that manages the Correlation ID for incoming HTTP requests.
 * <p>
 * This filter is the "server-side" entry point for distributed tracing in a reactive environment.
 * It inspects incoming requests for a correlation ID header. If one is found, it's used.
 * If not, a new one is generated. The ID is then propagated through the reactive context.
 * </p>
 */
@Component("reactiveCorrelationIdFilter")
@ConditionalOnProperty(prefix = "unified.logging.correlation-id", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_KEY = "correlationId";

    private final IUnifiedLogger logger;
    private final CorrelationIdGenerator correlationIdGenerator;
    private final UnifiedLoggingProperties properties;

    public CorrelationIdFilter(CorrelationIdGenerator correlationIdGenerator, UnifiedLoggingProperties properties, UnifiedLoggerFactory loggerFactory) {
        this.correlationIdGenerator = correlationIdGenerator;
        this.properties = properties;
        this.logger = loggerFactory.getLogger("CorrelationIdFilter", AppConstants.LOG_CATEGORY_CORRELATION_FILTER);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String correlationIdHeaderName = properties.getCorrelationId().getHeaderName();
        String correlationId = exchange.getRequest().getHeaders().getFirst(correlationIdHeaderName);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            if (properties.getCorrelationId().isAutoGenerate()) {
                correlationId = correlationIdGenerator.generate();
                logger.trace().message(AppConstants.LOG_CORRELATION_ID_GENERATED).detail("id", correlationId).log();
            }
        } else {
            logger.trace().message(AppConstants.LOG_CORRELATION_ID_RECEIVED).detail("id", correlationId).log();
        }

        if (correlationId != null) {
            exchange.getResponse().getHeaders().add(correlationIdHeaderName, correlationId);
            logger.trace().message(AppConstants.LOG_CORRELATION_ID_SET).detail("id", correlationId).log();

            // Assign correlationId to a final variable for use in lambdas
            final String finalCorrelationId = correlationId;

            // Propagate correlation ID through reactive context and bridge to MDC
            return chain.filter(exchange)
                    .contextWrite(ctx -> ctx.put(CORRELATION_ID_KEY, finalCorrelationId))
                    .doFirst(() -> MDC.put(CORRELATION_ID_KEY, finalCorrelationId))
                    .doFinally(signalType -> MDC.remove(CORRELATION_ID_KEY));
        }

        // Continue the filter chain without a correlation ID.
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run this filter before any others to ensure correlation ID is always present.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}