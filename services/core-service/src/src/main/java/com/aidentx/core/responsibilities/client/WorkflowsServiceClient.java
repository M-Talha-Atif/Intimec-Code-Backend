package com.aidentx.core.responsibilities.client;

import com.aidentx.core.responsibilities.dto.WorkflowApprovalDTO;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for communicating with workflows-service.
 * Fetches approval data for the Responsibilities page.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Service
public class WorkflowsServiceClient {
    
    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;
    
    @Value("${workflows-service.url:http://localhost:8083}")
    private String workflowsServiceUrl;
    
    public WorkflowsServiceClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(WorkflowsServiceClient.class, "WorkflowsServiceClient");
    }
    
    /**
     * Extract Authorization header from current request context.
     * 
     * @return Authorization header value (e.g., "Bearer xxx...") or null
     */
    private String extractAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("Authorization");
            }
        } catch (Exception e) {
            logger.warn()
                .message("Failed to extract Authorization header from request context")
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }
    
    /**
     * Get pending approvals for a user.
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @return List of pending approvals
     */
    public List<WorkflowApprovalDTO> getPendingApprovals(UUID userId, String tenantId, String authToken) {
        try {
            logger.info()
                .message("Fetching pending approvals from workflows-service")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = UriComponentsBuilder
                .fromHttpUrl(workflowsServiceUrl + "/api/v1/approvals/my-pending")
                .queryParam("userId", userId)
                .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", userId.toString());
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<WorkflowApprovalDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<WorkflowApprovalDTO>>() {}
            );
            
            List<WorkflowApprovalDTO> approvals = response.getBody();
            
            logger.info()
                .message("Successfully fetched approvals")
                .detail("count", approvals != null ? approvals.size() : 0)
                .log();
            
            return approvals != null ? approvals : List.of();
            
        } catch (Exception e) {
            logger.error()
                .message("Failed to fetch pending approvals from workflows-service")
                .detail("userId", userId.toString())
                .detail("error", e.getMessage())
                .log();
            // Propagate the error so frontend can show proper error message and retry option
            throw new RuntimeException("Failed to fetch pending approvals: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get processed (historical) approvals for a user.
     * Returns all completed approvals: APPROVED, REJECTED, TIMEOUT, etc.
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param authToken JWT Authorization token
     * @param decision Optional filter: APPROVED, REJECTED, TIMEOUT, or null for all
     * @return List of processed approvals
     */
    public List<WorkflowApprovalDTO> getProcessedApprovals(UUID userId, String tenantId, String authToken, String decision) {
        try {
            logger.info()
                .message("Fetching approval history from workflows-service")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .detail("decisionFilter", decision != null ? decision : "all")
                .log();
            
            var urlBuilder = UriComponentsBuilder
                .fromHttpUrl(workflowsServiceUrl + "/api/v1/approvals/my-history")
                .queryParam("userId", userId);
            
            if (decision != null && !decision.isEmpty() && !"all".equalsIgnoreCase(decision)) {
                urlBuilder.queryParam("decision", decision);
            }
            
            String url = urlBuilder.toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", userId.toString());
            headers.set("X-Tenant-ID", tenantId);
            if (authToken != null && !authToken.isEmpty()) {
                headers.set("Authorization", authToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<WorkflowApprovalDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<WorkflowApprovalDTO>>() {}
            );
            
            List<WorkflowApprovalDTO> approvals = response.getBody();
            
            logger.info()
                .message("Successfully fetched approval history")
                .detail("count", approvals != null ? approvals.size() : 0)
                .log();
            
            return approvals != null ? approvals : List.of();
            
        } catch (Exception e) {
            logger.error()
                .message("Failed to fetch approval history from workflows-service")
                .detail("userId", userId.toString())
                .detail("decisionFilter", decision != null ? decision : "all")
                .detail("error", e.getMessage())
                .log();
            // Propagate the error so frontend can show proper error message and retry option
            throw new RuntimeException("Failed to fetch approval history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get approval details by ID.
     * 
     * @param approvalId Approval ID
     * @param userId User ID (for authorization)
     * @param tenantId Tenant ID
     * @return Approval details
     */
    public WorkflowApprovalDTO getApprovalDetails(UUID approvalId, UUID userId, String tenantId) {
        try {
            logger.info()
                .message("Fetching approval details from workflows-service")
                .detail("approvalId", approvalId.toString())
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .log();
            
            String url = workflowsServiceUrl + "/api/v1/approvals/" + approvalId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", userId.toString());
            headers.set("X-Tenant-ID", tenantId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<WorkflowApprovalDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                WorkflowApprovalDTO.class
            );
            
            logger.info()
                .message("Successfully fetched approval details")
                .detail("approvalId", approvalId.toString())
                .log();
            
            return response.getBody();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to fetch approval details from workflows-service")
                .detail("approvalId", approvalId.toString())
                .log();
            throw new RuntimeException("Failed to fetch approval details", e);
        }
    }
    
    /**
     * Approve an approval request.
     * 
     * @param approvalId Approval ID
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param comment Optional comment
     */
    public void approveApproval(UUID approvalId, UUID userId, String tenantId, String username, String comment) {
        try {
            logger.info()
                .message("Approving approval via workflows-service")
                .detail("approvalId", approvalId.toString())
                .detail("userId", userId.toString())
                .detail("username", username)
                .detail("tenantId", tenantId)
                .log();
            
            String url = workflowsServiceUrl + "/api/v1/approvals/" + approvalId + "/approve";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", userId.toString());
            if (username != null && !username.trim().isEmpty()) {
                headers.set("X-Username", username);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Forward JWT token for tenant validation
            String authHeader = extractAuthorizationHeader();
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            
            Map<String, String> body = new HashMap<>();
            if (comment != null && !comment.trim().isEmpty()) {
                body.put("comments", comment);
            }
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            
            logger.info()
                .message("Successfully approved approval")
                .detail("approvalId", approvalId.toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to approve approval via workflows-service")
                .detail("approvalId", approvalId.toString())
                .log();
            throw new RuntimeException("Failed to approve approval", e);
        }
    }
    
    /**
     * Reject an approval request.
     * 
     * @param approvalId Approval ID
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param comment Required comment explaining rejection
     */
    public void rejectApproval(UUID approvalId, UUID userId, String tenantId, String username, String comment) {
        try {
            logger.info()
                .message("Rejecting approval via workflows-service")
                .detail("approvalId", approvalId.toString())
                .detail("userId", userId.toString())
                .detail("username", username)
                .detail("tenantId", tenantId)
                .log();
            
            if (comment == null || comment.trim().isEmpty()) {
                throw new IllegalArgumentException("Comment is required for rejection");
            }
            
            String url = workflowsServiceUrl + "/api/v1/approvals/" + approvalId + "/reject";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", userId.toString());
            if (username != null && !username.trim().isEmpty()) {
                headers.set("X-Username", username);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Forward JWT token for tenant validation
            String authHeader = extractAuthorizationHeader();
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            
            Map<String, String> body = new HashMap<>();
            body.put("comments", comment);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            
            logger.info()
                .message("Successfully rejected approval")
                .detail("approvalId", approvalId.toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to reject approval via workflows-service")
                .detail("approvalId", approvalId.toString())
                .log();
            throw new RuntimeException("Failed to reject approval", e);
        }
    }
}
