package com.aidentx.workflowsservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * REST client for Core Service Email operations.
 * Used to send custom emails for workflow approvals when email notification is enabled.
 *
 * @author aidentx Development Team
 * @version 1.0
 */
import io.github.resilience4j.retry.annotation.Retry;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@Service
public class EmailRestClient {

    private final IUnifiedLogger logger;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${core.service.rest.url:http://localhost:8091}")
    private String coreServiceUrl;

    public EmailRestClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(EmailRestClient.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Sends a custom email via core-service email API.
     *
     * @param tenantId tenant ID
     * @param recipientEmail recipient's email address
     * @param subject email subject
     * @param body email body (HTML or plain text)
     */
    @CircuitBreaker(name = "coreService", fallbackMethod = "sendEmailFallback")
    @Retry(name = "coreService")
    public void sendEmail(
            String tenantId,
            String recipientEmail,
            String subject,
            String body) {
        
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("recipientEmail", recipientEmail);
            requestBody.put("subject", subject);
            requestBody.put("body", body);
            requestBody.put("tenantId", tenantId);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Extract JWT token from current request context and forward it
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    String authHeader = attrs.getRequest().getHeader("Authorization");
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_COULD_NOT_EXTRACT_AUTH_HEADER)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("error", e.getMessage())
                    .log();
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call core-service email API
            String url = coreServiceUrl + "/api/notifications/email/send";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info()
                    .message(LOG_EMAIL_SENT_SUCCESSFULLY)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("recipientEmail", recipientEmail)
                    .log();
            } else {
                logger.error()
                    .message(LOG_FAILED_SEND_EMAIL)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("recipientEmail", recipientEmail)
                    .detail("statusCode", response.getStatusCode().toString())
                    .detail("response", response.getBody())
                    .log();
            }

        } catch (Exception e) {
            logger.error()
                .message(LOG_EXCEPTION_SENDING_EMAIL)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("recipientEmail", recipientEmail)
                .detail("error", e.getMessage())
                .log();
            throw e; // Re-throw to trigger circuit breaker/retry
        }
    }

    /**
     * Fallback method when core-service email API is unavailable.
     */
    private void sendEmailFallback(
            String tenantId,
            String recipientEmail,
            String subject,
            String body,
            Exception e) {
        logger.error()
            .message(LOG_CORE_SERVICE_EMAIL_UNAVAILABLE)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("recipientEmail", recipientEmail)
            .detail("error", e.getMessage())
            .log();
        logger.warn()
            .message(LOG_EMAIL_FALLBACK)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .detail("subject", subject)
            .detail("recipient", recipientEmail)
            .log();
        // In production: Store email in database for later retry or use backup email service
    }

    /**
     * Health check to verify core-service email API connectivity.
     *
     * @return true if core-service is reachable
     */
    public boolean isEmailServiceReachable() {
        try {
            String url = coreServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error()
                .message(LOG_CORE_SERVICE_HEALTH_CHECK_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }
}
