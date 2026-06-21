package com.aidentx.api_gateaway.filter;

import com.aidentx.api_gateaway.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Global filter that ensures X-Forwarded-For and X-Real-IP headers are properly
 * set for all requests forwarded to downstream services.
 * This is essential for IP geolocation and accurate client IP tracking.
 */
@Component
public class ForwardedHeadersFilter implements GlobalFilter, Ordered {

    private final IUnifiedLogger logger;

    public ForwardedHeadersFilter(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger("ForwardedHeadersFilter", AppConstants.LOG_CATEGORY_GATEWAY_FILTER);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIpAddress(request);

        // Get X-User-Agent if present (forwarded from frontend)
        String xUserAgent = request.getHeaders().getFirst("X-User-Agent");
        
        logger.trace()
                .message("Setting forwarded headers for downstream services")
                .detail("clientIp", clientIp)
                .detail("xUserAgent", xUserAgent)
                .detail("path", request.getURI().getPath())
                .log();

        // Add or update X-Forwarded-For, X-Real-IP, and X-User-Agent headers
        ServerHttpRequest.Builder builder = request.mutate()
                .header("X-Forwarded-For", clientIp)
                .header("X-Real-IP", clientIp);
        
        // Forward X-User-Agent if present
        if (xUserAgent != null && !xUserAgent.isEmpty()) {
            builder.header("X-User-Agent", xUserAgent);
        }
        
        ServerHttpRequest mutatedRequest = builder.build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Gets the client IP address from the request, handling proxy headers.
     * Checks standard proxy headers first, then falls back to remote address.
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        // Check if X-Forwarded-For already exists (from upstream proxy)
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(forwardedFor)) {
            // Take the first IP from the list (original client)
            return forwardedFor.contains(",") ? forwardedFor.split(",")[0].trim() : forwardedFor.trim();
        }

        // Check X-Real-IP
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }

        // Check other common proxy headers
        String[] otherHeaders = {
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_CLIENT_IP"
        };

        for (String header : otherHeaders) {
            String ip = request.getHeaders().getFirst(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }

        // Fall back to remote address
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        // Run after CorrelationIdFilter but before AuthFilter
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
