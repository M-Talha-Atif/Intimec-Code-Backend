package com.aidentx.logging.interceptor;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * A Spring RestTemplate interceptor that propagates the Correlation ID.
 * <p>
 * This interceptor reads the correlation ID from the current thread's
 * MDC (Mapped Diagnostic Context) and adds it as a header to any outgoing
 * HTTP request made by a RestTemplate. This is the "client-side" part of
 * distributed tracing, ensuring that the trace context is passed from one
 * microservice to the next.
 * </p>
 * This class is not intended to be used directly but is configured automatically
 * by {@link com.aidentx.logging.config.UnifiedLoggingAutoConfiguration}.
 */
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    /**
     * The name of the HTTP header used to carry the correlation ID.
     * e.g., "X-Correlation-ID"
     */
    private final String headerName;

    /**
     * Constructs the interceptor.
     *
     * @param headerName The name of the header to use for the correlation ID.
     */
    public CorrelationIdInterceptor(String headerName) {
        if (headerName == null || headerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Header name cannot be null or empty.");
        }
        this.headerName = headerName;
    }

    /**
     * The core interception logic.
     * <p>
     * This method is invoked by Spring before a RestTemplate request is executed.
     * It retrieves the correlation ID from the MDC and, if present, adds it
     * to the request's headers before allowing the request to proceed.
     * </p>
     *
     * @param request   The HTTP request to be executed.
     * @param body      The body of the request.
     * @param execution The execution context, used to proceed with the request.
     * @return The HTTP response from the downstream service.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body, @NonNull ClientHttpRequestExecution execution) throws IOException {
        // Retrieve the correlation ID from the MDC. This was set by the CorrelationIdFilter on the incoming request.
        final String correlationId = MDC.get("correlationId");

        if (correlationId != null) {
            // Add the correlation ID to the headers of the outgoing request.
            request.getHeaders().add(this.headerName, correlationId);
        }

        // Proceed with executing the request.
        return execution.execute(request, body);
    }
}