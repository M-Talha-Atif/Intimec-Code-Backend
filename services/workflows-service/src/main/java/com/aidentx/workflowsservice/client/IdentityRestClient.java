package com.aidentx.workflowsservice.client;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * REST client for Identity Service operations.
 * Uses REST API since gRPC CreateEmployee is not yet implemented in identity-service.
 * 
 * Note: Identity service exposes gRPC for read operations (getById, exists, delete)
 * but employee creation is only available via REST API.
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class IdentityRestClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IdentityGrpcClient identityGrpcClient;
    private final IUnifiedLogger logger;

    @Value("${identity.service.rest.url:http://localhost:8094}")
    private String identityServiceUrl;

    public IdentityRestClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            IdentityGrpcClient identityGrpcClient,
            UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.identityGrpcClient = identityGrpcClient;
        this.logger = loggerFactory.getLogger(IdentityRestClient.class, "rest-client");
    }

    /**
     * Creates an employee identity via REST API.
     * Matches the exact structure from Postman collection.
     *
     * @param tenantId tenant ID
     * @param username unique username
     * @param email email address
     * @param firstName first name
     * @param lastName last name
     * @param employeeId employee ID
     * @param department department name
     * @param jobTitle job title
     * @param managerId manager's identity ID (optional)
     * @param hireDate hire date (optional)
     * @return created identity ID
     */
    public String createEmployee(
            String tenantId,
            String username,
            String email,
            String firstName,
            String lastName,
            String employeeId,
            String department,
            String jobTitle,
            String managerId,
            LocalDate hireDate) {
        
        try {
            // Build request body matching Postman collection structure
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("username", username);
            requestBody.put("email", email);
            requestBody.put("firstName", firstName);
            requestBody.put("lastName", lastName);
            requestBody.put("employeeId", employeeId);
            requestBody.put("department", department);
            requestBody.put("jobTitle", jobTitle);
            
            if (managerId != null) {
                requestBody.put("managerId", managerId);
            }
            
            if (hireDate != null) {
                requestBody.put("hireDate", hireDate.toString());
            }

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Extract JWT token from current request context and forward it
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        headers.set("Authorization", authHeader);
                        logger.trace()
                            .message(LOG_FORWARDING_JWT_TO_IDENTITY)
                            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                            .log();
                    } else {
                        logger.warn()
                            .message(LOG_NO_JWT_TOKEN_FOUND)
                            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                            .log();
                    }
                }
            } catch (Exception e) {
                logger.warn()
                    .message(LOG_COULD_NOT_EXTRACT_JWT)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("error", e.getMessage())
                    .log();
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Call REST API
            String url = identityServiceUrl + "/api/v1/identity/employee";
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String identityId = (String) response.getBody().get("id");
                logger.info()
                    .message(LOG_EMPLOYEE_CREATED_VIA_REST)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("identityId", identityId)
                    .detail("email", email)
                    .log();
                return identityId;
            } else {
                throw new RuntimeException("Failed to create employee: unexpected response");
            }

        } catch (HttpClientErrorException e) {
            logger.error()
                .message(LOG_HTTP_ERROR_CREATING_EMPLOYEE)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("statusCode", e.getStatusCode().toString())
                .detail("response", e.getResponseBodyAsString())
                .log();
            throw new RuntimeException("Failed to create employee: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error()
                .message(LOG_ERROR_CREATING_EMPLOYEE_VIA_REST)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            throw new RuntimeException("Failed to create employee: " + e.getMessage(), e);
        }
    }

    /**
     * Gets identity by ID using gRPC (this is implemented).
     *
     * @param identityId identity ID
     * @return true if identity exists
     */
    public boolean identityExists(String identityId) {
        return identityGrpcClient.identityExists(identityId);
    }

    /**
     * Deletes identity using gRPC (this is implemented).
     *
     * @param identityId identity ID to delete
     * @param tenantId tenant ID for multi-tenant isolation
     */
    public void deleteIdentity(String identityId, String tenantId) {
        identityGrpcClient.deleteIdentity(identityId, tenantId);
    }

    /**
     * Health check for identity service.
     *
     * @return true if service is reachable
     */
    public boolean isReachable() {
        try {
            String url = identityServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn()
                .message(LOG_IDENTITY_SERVICE_HEALTH_CHECK_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return false;
        }
    }
}
