package com.aidentx.workflowsservice.client;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * REST client for calling Core Service notification endpoints.
 * Used to mark notifications as viewed when approvals are responded to.
 */
@Component
public class NotificationRestClient {

    private final IUnifiedLogger logger;
    private final RestTemplate restTemplate;
    private final String coreServiceUrl;

    public NotificationRestClient(
            RestTemplate restTemplate,
            @Value("${services.core-service.url:http://core-service:8091}") String coreServiceUrl,
            UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.coreServiceUrl = coreServiceUrl;
        this.logger = loggerFactory.getLogger(NotificationRestClient.class, "rest-client");
    }

    /**
     * Marks a notification as viewed for a specific user.
     *
     * @param notificationId the notification ID to mark as viewed
     * @param userId the user ID who should have the notification marked as viewed
     * @param tenantId the tenant ID
     * @return true if successful, false otherwise
     */
    public boolean markNotificationAsViewed(String notificationId, String userId, String tenantId) {
        try {
            String url = coreServiceUrl + "/api/v1/user-notifications/mark-viewed";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-ID", userId);
            headers.set("X-Tenant-ID", tenantId);

            Map<String, String> body = new HashMap<>();
            body.put("notificationId", notificationId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            logger.info()
                .message(LOG_MARKED_NOTIFICATION_VIEWED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("notificationId", notificationId)
                .detail("userId", userId)
                .log();
            return true;

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_MARK_NOTIFICATION_VIEWED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("notificationId", notificationId)
                .detail("userId", userId)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }

    /**
     * Marks all WORKFLOW_APPROVAL_REQUESTED notifications as viewed for a specific user.
     * Called automatically when a user responds to an approval (approve/reject).
     *
     * @param userId the user ID whose approval notifications should be marked as viewed
     * @param tenantId the tenant ID
     * @return true if successful, false otherwise
     */
    public boolean markAllApprovalNotificationsAsViewed(String userId, String tenantId) {
        try {
            String url = coreServiceUrl + "/api/v1/user-notifications/mark-approval-notifications-viewed";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-ID", userId);
            headers.set("X-Tenant-ID", tenantId);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            logger.info()
                .message(LOG_MARKED_ALL_APPROVAL_NOTIFICATIONS_VIEWED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("userId", userId)
                .log();
            return true;

        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_MARK_ALL_APPROVAL_NOTIFICATIONS_VIEWED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("userId", userId)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }
}
