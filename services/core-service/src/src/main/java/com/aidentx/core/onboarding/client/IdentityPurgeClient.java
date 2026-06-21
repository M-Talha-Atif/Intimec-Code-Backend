package com.aidentx.core.onboarding.client;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * REST client for communicating with identity-service internal endpoints.
 * Used to cascade-purge all identity data for an application when the application is deleted.
 */
@Service
public class IdentityPurgeClient {

    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;

    @Value("${identity-service.url:http://identity-service:8094}")
    private String identityServiceUrl;

    public IdentityPurgeClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(IdentityPurgeClient.class);
    }

    /**
     * Purges all identity-service data (shadows, entitlements, assignments, rules,
     * shards, changelogs) belonging to an application.
     * Called during application deletion to prevent orphaned data.
     *
     * @param applicationId the application whose data should be purged
     * @param tenantId      the tenant context
     */
    public void purgeApplicationData(UUID applicationId, String tenantId) {
        try {
            String url = identityServiceUrl + "/api/v1/internal/applications/" + applicationId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);

            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

            logger.info()
                    .message("Purged identity-service data for application")
                    .detail("applicationId", applicationId)
                    .detail("tenantId", tenantId)
                    .log();
        } catch (Exception e) {
            // Log but don't fail the application delete — data can be cleaned up separately
            logger.warn()
                    .message("Failed to purge identity-service data for application; continuing with application deletion")
                    .detail("applicationId", applicationId)
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    private String extractAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("Authorization");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
