package com.aidentx.logging.filter;

import com.aidentx.logging.config.UnifiedLoggingProperties;
import com.aidentx.logging.util.CorrelationIdGenerator;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * A servlet filter that manages the Correlation ID for incoming HTTP requests.
 * <p>
 * This filter is the "server-side" entry point for distributed tracing. It inspects
 * incoming requests for a correlation ID header. If one is found, it's used.
 * If not, a new one is generated. The ID is then placed into the SLF4J
 * Mapped Diagnostic Context (MDC) to be automatically included in all subsequent logs
 * for the current request thread.
 * </p>
 * It also adds the correlation ID to the HTTP response headers.
 * <p>
 * This filter is only enabled when servlet classes are available on the classpath.
 * For reactive applications, use the reactive correlation ID filter instead.
 * </p>
 */
@Component
@Order(1) // Ensure this filter runs before most other filters.
@ConditionalOnProperty(prefix = "unified.logging.correlation-id", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
public class CorrelationIdFilter implements Filter {

    /**
     * The key used to store the correlation ID in the SLF4J MDC.
     * This constant can be shared with other components that need to access the MDC.
     */
    public static final String MDC_KEY_CORRELATION_ID = "correlationId";

    private final CorrelationIdGenerator correlationIdGenerator;
    private final UnifiedLoggingProperties properties;

    /**
     * Constructs the filter using constructor-based dependency injection.
     * This is the preferred method over field injection.
     *
     * @param correlationIdGenerator The utility to generate new correlation IDs.
     * @param properties The centralized logging configuration properties.
     */
    public CorrelationIdFilter(CorrelationIdGenerator correlationIdGenerator, UnifiedLoggingProperties properties) {
        this.correlationIdGenerator = correlationIdGenerator;
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        final String correlationIdHeaderName = properties.getCorrelationId().getHeaderName();

        try {
            // Determine the correlation ID
            String correlationId = httpRequest.getHeader(correlationIdHeaderName);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                if (properties.getCorrelationId().isAutoGenerate()) {
                    correlationId = correlationIdGenerator.generate();
                }
            }

            if (correlationId != null) {
                // Set the ID in the MDC for the current thread.
                MDC.put(MDC_KEY_CORRELATION_ID, correlationId);
                // Add the ID to the response header so the client can see it.
                httpResponse.setHeader(correlationIdHeaderName, correlationId);
            }

            // Continue the filter chain.
            chain.doFilter(request, response);
        } finally {
            // CRITICAL: Always clean up the MDC after the request is complete
            // to prevent the correlation ID from leaking to other threads.
            MDC.remove(MDC_KEY_CORRELATION_ID);
        }
    }
}