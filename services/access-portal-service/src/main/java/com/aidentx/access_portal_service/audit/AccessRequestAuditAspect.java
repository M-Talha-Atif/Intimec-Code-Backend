package com.aidentx.access_portal_service.audit;

import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestResponseDTO;
import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestRepository;
import com.aidentx.access_portal_service.util.JwtUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.*;

/**
 * AOP Aspect for auditing Access Request operations (submit, withdraw).
 */
@Aspect
@Component
public class AccessRequestAuditAspect {

    private static final String LOG_CATEGORY = "access-request-audit";

    private final AccessRequestRepository requestRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final AccessPortalAuditPublisher auditPublisher;
    private final JwtUtils jwtUtils;
    private final UserRoleRestClient userRoleRestClient;

    public AccessRequestAuditAspect(
            AccessRequestRepository requestRepository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            AccessPortalAuditPublisher auditPublisher,
            UserRoleRestClient userRoleRestClient,
            JwtUtils jwtUtils
    ) {
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), LOG_CATEGORY);
        this.auditPublisher = auditPublisher;
        this.jwtUtils = jwtUtils;
        this.userRoleRestClient = userRoleRestClient;
        logger.info().message("AccessRequestAuditAspect initialized").log();
    }

    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("X-Tenant-Id");
            }
        } catch (Exception e) {
            logger.warn().message("Failed to get tenantId").detail("error", e.getMessage()).log();
        }
        return "tenant_001";
    }

    private UUID getCurrentUserId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String userId = attrs.getRequest().getHeader("X-User-Id");
                if (userId != null) return UUID.fromString(userId);
            }
        } catch (Exception e) {
            logger.warn().message("Failed to get userId").detail("error", e.getMessage()).log();
        }
        return null;
    }

    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return jwtUtils.extractUsername(request);
            }
        } catch (Exception e) {
            logger.error(e).message("Error extracting username").log();
        }
        return null;
    }

    private JsonNode accessRequestToJsonNode(AccessRequest request) {
        AccessRequestAuditDTO dto = AccessRequestAuditDTO.from(request);
        return objectMapper.valueToTree(dto);
    }

    /**
     * Audit: Submit Request
     */
    @AfterReturning(
            pointcut = "execution(* com.aidentx.access_portal_service.service.impl.AccessRequestServiceImpl.submitRequest(..))",
            returning = "result"
    )
    public void auditRequestSubmitted(JoinPoint joinPoint, Object result) {
        try {
            AccessRequestResponseDTO dto = (AccessRequestResponseDTO) result;
            AccessRequest request = requestRepository.findByIdWithItems(dto.getRequestId(), dto.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Access request not found: " + dto.getRequestId()));

            JsonNode stateAfter = accessRequestToJsonNode(request);
            UUID actorId = getCurrentUserId();

            AccessPortalAuditEvent event = actorId != null
                    ? AccessPortalAuditEvent.user(
                            actorId,
                            getCurrentUsername(),
                            getCurrentTenantId(),
                            request.getId(),
                            "ACCESS_REQUEST",
                            calculateRiskLevel(request),
                            "access-request-submitted",
                            null,
                            stateAfter,
                            null,
                            null
                    )
                    : AccessPortalAuditEvent.system(
                            getCurrentTenantId(),
                            request.getId(),
                            "ACCESS_REQUEST",
                            "access-request-submitted",
                            calculateRiskLevel(request),
                            null,
                            stateAfter
                    );

            auditPublisher.publish(event);

            logger.info()
                    .message("Published ACCESS_REQUEST_SUBMITTED event")
                    .objectId(request.getId().toString())
                    .log();
        } catch (Exception e) {
            logger.error(e).message("Failed to publish ACCESS_REQUEST_SUBMITTED event").log();
        }
    }

    /**
     * Audit: Withdraw Request
     */
    @Before(
            value = "execution(* com.aidentx.access_portal_service.service.impl.AccessRequestServiceImpl.withdrawRequest(..)) && args(tenantId, userId, requestId, ..)",
            argNames = "tenantId,userId,requestId"
    )
    public void auditRequestWithdrawn(String tenantId, UUID userId, UUID requestId) {
        try {
            AccessRequest request = requestRepository.findByIdWithItems(requestId, tenantId).orElse(null);
            JsonNode stateBefore = request != null ? accessRequestToJsonNode(request) : null;

            UUID actorId = getCurrentUserId();

            AccessPortalAuditEvent event = actorId != null
                    ? AccessPortalAuditEvent.user(
                            actorId,
                            getCurrentUsername(),
                            tenantId,
                            requestId,
                            "ACCESS_REQUEST",
                            request != null ? calculateRiskLevel(request) : "low",
                            "access-request-withdrawn",
                            stateBefore,
                            null,
                            null,
                            null
                    )
                    : AccessPortalAuditEvent.system(
                            tenantId,
                            requestId,
                            "ACCESS_REQUEST",
                            "access-request-withdrawn",
                            request != null ? calculateRiskLevel(request) : "low",
                            stateBefore,
                            null
                    );

            auditPublisher.publish(event);

            logger.info()
                    .message("Published ACCESS_REQUEST_WITHDRAWN event")
                    .objectId(requestId.toString())
                    .log();
        } catch (Exception e) {
            logger.error(e).message("Failed to publish ACCESS_REQUEST_WITHDRAWN event").log();
        }
    }

   /**
     * Calculate overall risk level for request based on role risk levels
     * Hierarchy: low < medium < high
     * Stops early if high is encountered
     */
    private String calculateRiskLevel(AccessRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return "low";
        }

        String highestRisk = "low";

        for (var item : request.getItems()) {
            // Only process INTERNAL_ROLE items
            if (!"INTERNAL_ROLE".equalsIgnoreCase(item.getItemType())) {
                continue;
            }

            try {
                // Fetch role details via API
                Map<String, Object> roleData = userRoleRestClient.getRoleById(item.getItemId());

                if (roleData != null && !roleData.isEmpty()) {
                    String riskLevel = ((String) roleData.getOrDefault("riskLevel", "Low")).toLowerCase();

                    // Early exit if high is found (highest possible)
                    if ("high".equals(riskLevel)) {
                        logger.debug()
                                .message("Found high risk role, stopping early")
                                .detail("roleId", item.getItemId().toString())
                                .detail("requestId", request.getId().toString())
                                .log();
                        return "high";
                    }

                    // Update highest risk, like if we found medium
                    if ("medium".equals(riskLevel) && "low".equals(highestRisk)) {
                        highestRisk = "medium";
                    }
                }
            } catch (Exception e) {
                logger.warn()
                        .message("Failed to fetch risk level for role, defaulting to low")
                        .detail("roleId", item.getItemId().toString())
                        .detail("error", e.getMessage())
                        .log();
            }
        }

        logger.debug()
                .message("Calculated request risk level")
                .detail("requestId", request.getId().toString())
                .detail("itemCount", String.valueOf(request.getItems().size()))
                .detail("highestRisk", highestRisk)
                .log();

        return highestRisk;
    }
}