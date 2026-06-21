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
 * REST client for communicating with task-management-service.
 * Used to cascade-delete all tasks for an application when the application itself is deleted.
 */
@Service
public class TaskManagementServiceClient {

    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;

    @Value("${task-management-service.url:http://localhost:8096}")
    private String taskManagementServiceUrl;

    public TaskManagementServiceClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(TaskManagementServiceClient.class);
    }

    /**
     * Deletes all tasks belonging to an application.
     * Called during application deletion to prevent orphaned tasks.
     *
     * @param applicationId the application whose tasks should be deleted
     * @param tenantId      the tenant context
     */
    public void deleteAllTasksForApplication(UUID applicationId, String tenantId) {
        try {
            String url = taskManagementServiceUrl + "/api/v1/applications/" + applicationId + "/tasks";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Tenant-ID", tenantId);

            String authHeader = extractAuthorizationHeader();
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

            logger.info()
                    .message("Cascade-deleted all tasks for application")
                    .detail("applicationId", applicationId)
                    .detail("tenantId", tenantId)
                    .log();
        } catch (Exception e) {
            // Log but don't fail the application delete — tasks can be cleaned up separately
            logger.warn()
                    .message("Failed to cascade-delete tasks for application; continuing with application deletion")
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
