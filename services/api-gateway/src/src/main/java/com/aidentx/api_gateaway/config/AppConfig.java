package com.aidentx.api_gateaway.config;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Application configuration for the API Gateway.
 */
@Configuration
public class AppConfig {

    /**
     * Configures Netty's DNS resolver with a short TTL so the gateway
     * automatically re-resolves container hostnames after a restart,
     * preventing stale-IP "Connection refused" errors in Docker environments.
     *
     * Positive results are cached for at most 10 s; negative results for 3 s.
     * Round-robin selection spreads load evenly when multiple A records exist.
     */
    @Bean
    public HttpClientCustomizer dnsRefreshingHttpClientCustomizer() {
        return httpClient -> httpClient.resolver(spec -> spec
                .cacheMaxTimeToLive(Duration.ofSeconds(10))
                .cacheMinTimeToLive(Duration.ofSeconds(0))
                .cacheNegativeTimeToLive(Duration.ofSeconds(3))
                .roundRobinSelection(true)
        );
    }
}
