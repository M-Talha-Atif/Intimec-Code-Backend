package com.aidentx.access_portal_service.client;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for Business Roles managed by identity-service
 * ({@code access_roles_management} module).
 * <p>
 * Endpoint:
 * <pre>
 *   GET {identity.service.rest.url}/api/v1/access-roles/{roleId}
 *   Headers: X-Tenant-Id, Authorization: Bearer ...
 *   Response: ApiResponse&lt;AccessRoleDetailResponse&gt; (data wrapped under "data" key)
 * </pre>
 * <p>
 * Mirrors {@link UserRoleRestClient} conventions:
 * - Forwards JWT + tenant headers from incoming request context
 * - Uses unified structured logging via {@link IUnifiedLogger}
 * - All string literals sourced from {@link AccessPortalConstants}
 *
 * @author aidentx
 * @version 1.0
 */
@Service
public class BusinessRoleRestClient {

    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;

    @Value("${identity.service.rest.url:http://localhost:8094}")
    private String identityServiceUrl;

    @Autowired
    public BusinessRoleRestClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_EXTERNAL_SERVICE
        );
    }

    /**
     * Fetch a business (access) role by its ID.
     *
     * @param roleId   business role UUID
     * @param tenantId tenant context
     * @return business role detail map (data key from ApiResponse wrapper); empty map on non-2xx
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBusinessRoleById(UUID roleId, String tenantId) {
        logger.debug()
                .message("Fetching business role by ID from identity service")
                .detail("businessRoleId", roleId != null ? roleId.toString() : "null")
                .detail("tenantId", tenantId)
                .detail("serviceUrl", identityServiceUrl)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH)
                .log();

        if (roleId == null) {
            return Collections.emptyMap();
        }

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(tenantId));
            String url = identityServiceUrl + AccessPortalConstants.IDENTITY_ACCESS_ROLE_BY_ID_PATH;

            logger.trace()
                    .message("Executing GET request to fetch business role")
                    .detail("url", url)
                    .detail("businessRoleId", roleId.toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_HTTP_REQUEST)
                    .log();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    roleId
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> roleData = (Map<String, Object>) body.get("data");

                if (roleData == null) {
                    roleData = Collections.emptyMap();
                }

                logger.debug()
                        .message("Successfully fetched business role by ID")
                        .detail("businessRoleId", roleId.toString())
                        .detail("tenantId", tenantId)
                        .detail("displayName", String.valueOf(roleData.get("displayName")))
                        .detail("statusCode", response.getStatusCode().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_SUCCESS)
                        .log();

                return roleData;
            }

            logger.warn()
                    .message("Unexpected response while fetching business role by ID")
                    .detail("businessRoleId", roleId.toString())
                    .detail("statusCode", response.getStatusCode().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_WARNING)
                    .log();

            return Collections.emptyMap();

        } catch (Exception e) {
            logger.error()
                    .message("Failed to fetch business role by ID from identity service")
                    .detail("businessRoleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("serviceUrl", identityServiceUrl)
                    .detail("errorMessage", e.getMessage())
                    .detail("errorType", e.getClass().getSimpleName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_ERROR)
                    .log();

            throw new RuntimeException("Business role fetch by ID failed for: " + roleId, e);
        }
    }

    /**
     * Validate that a business role exists in identity-service for the given tenant.
     *
     * @param roleId   business role UUID
     * @param tenantId tenant context
     * @return true when identity-service returns a non-empty payload for this role
     */
    public boolean validateBusinessRoleExists(UUID roleId, String tenantId) {
        if (roleId == null || tenantId == null) {
            logger.debug()
                    .message("Business role validation skipped - null parameters")
                    .detail("roleIdNull", String.valueOf(roleId == null))
                    .detail("tenantIdNull", String.valueOf(tenantId == null))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_FAILED)
                    .log();
            return false;
        }

        try {
            Map<String, Object> data = getBusinessRoleById(roleId, tenantId);
            boolean exists = data != null && !data.isEmpty() && data.get("id") != null;

            logger.trace()
                    .message("Business role existence check completed")
                    .detail("businessRoleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("exists", String.valueOf(exists))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_SUCCESS)
                    .log();

            return exists;
        } catch (Exception e) {
            logger.warn()
                    .message("Business role existence check failed")
                    .detail("businessRoleId", roleId.toString())
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_ERROR)
                    .log();
            return false;
        }
    }

    /**
     * Builds HTTP headers, forwarding JWT and tenant header from the incoming request.
     */
    private HttpHeaders buildHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (tenantId != null && !tenantId.isEmpty()) {
            headers.set(AccessPortalConstants.IDENTITY_HEADER_TENANT_ID, tenantId);
        }

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    headers.set("Authorization", authHeader);
                    logger.trace()
                            .message("Forwarding JWT token to identity service")
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_JWT_FORWARDING)
                            .log();
                }

                // Fallback: pick up X-Tenant-ID from request if not explicitly supplied
                if (tenantId == null || tenantId.isEmpty()) {
                    String requestTenant = request.getHeader(AccessPortalConstants.IDENTITY_HEADER_TENANT_ID);
                    if (requestTenant != null && !requestTenant.isEmpty()) {
                        headers.set(AccessPortalConstants.IDENTITY_HEADER_TENANT_ID, requestTenant);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn()
                    .message("Unable to forward request headers to identity service")
                    .detail("errorMessage", e.getMessage())
                    .detail("errorType", e.getClass().getSimpleName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_JWT_FORWARDING_ERROR)
                    .log();
        }

        return headers;
    }
}

