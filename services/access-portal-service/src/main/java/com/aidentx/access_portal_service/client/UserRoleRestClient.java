package com.aidentx.access_portal_service.client;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST client for Application Permissions / User Roles service.
 * <p>
 * Responsibilities:
 * - Fetch user-role mappings
 * - Forward JWT token for authorization
 * - Support multi-tenant access
 * <p>
 * Real-world usage:
 * - Validate user permissions before workflow execution
 * - Fetch roles for access control decisions
 *
 * @author aidentx
 * @version 1.0
 */
@Service
public class UserRoleRestClient {

    private final RestTemplate restTemplate;
    private final IUnifiedLogger logger;

    @Value("${permissions.service.rest.url:http://localhost:8081}")
    private String permissionsServiceUrl;

    @Autowired
    public UserRoleRestClient(RestTemplate restTemplate, UnifiedLoggerFactory loggerFactory) {
        this.restTemplate = restTemplate;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_EXTERNAL_SERVICE
        );
    }

    /**
     * Fetch all user-role mappings.
     * <p>
     * Endpoint:
     * GET /api/v1/user-roles
     *
     * @return list of user-role records
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllUserRoles(String userId) {
        logger.debug()
                .message("Fetching all user roles from permissions service")
                .detail("serviceUrl", permissionsServiceUrl)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                .log();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            String url = permissionsServiceUrl + "/api/v1/user-roles/user/{userId}";

            logger.trace()
                    .message("Executing GET request to permissions service")
                    .detail("url", url)
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_HTTP_REQUEST)
                    .log();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    userId
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> userRoles =
                        (List<Map<String, Object>>) body.getOrDefault("data", Collections.emptyList());

                logger.debug()
                        .message("Successfully fetched user roles")
                        .detail("roleCount", String.valueOf(userRoles.size()))
                        .detail("statusCode", response.getStatusCode().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_SUCCESS)
                        .log();

                return userRoles;
            }

            logger.warn()
                    .message("Unexpected response while fetching user roles")
                    .detail("statusCode", response.getStatusCode().toString())
                    .detail("hasBody", String.valueOf(response.getBody() != null))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_WARNING)
                    .log();

            return Collections.emptyList();

        } catch (Exception e) {
            logger.error()
                    .message("Failed to fetch user roles from permissions service")
                    .detail("serviceUrl", permissionsServiceUrl)
                    .detail("errorMessage", e.getMessage())
                    .detail("errorType", e.getClass().getSimpleName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_ERROR)
                    .log();
            throw new RuntimeException("User roles service call failed", e);
        }
    }

    /**
     * Fetch roles for a specific user.
     * <p>
     * Real-world example:
     * - Used during login to build GrantedAuthorities
     *
     * @param userId identity/user ID
     * @return list of role names
     */
    public List<String> getRolesByUserId(String userId) {
        logger.debug()
                .message("Fetching roles for user")
                .detail("userId", userId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ROLES_FETCH)
                .log();

        List<String> roles = getAllUserRoles(userId).stream()
                .map(role -> (String) role.get("roleName"))
                .distinct()
                .toList();

        logger.debug()
                .message("Retrieved roles for user")
                .detail("userId", userId)
                .detail("roleCount", String.valueOf(roles.size()))
                .detail("roles", String.join(", ", roles))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_ROLES_RETRIEVED)
                .log();

        return roles;
    }

    /**
     * Check if a user has a specific role.
     * <p>
     * Example:
     * hasRole(userId, "Super Admin")
     *
     * @param userId   user identity ID
     * @param roleName role name
     * @return true if assigned
     */
    public boolean hasRole(String userId, String roleName) {
        logger.trace()
                .message("Checking if user has specific role")
                .detail("userId", userId)
                .detail("roleName", roleName)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_CHECK)
                .log();

        boolean hasRole = getRolesByUserId(userId)
                .stream()
                .anyMatch(roleName::equalsIgnoreCase);

        logger.trace()
                .message("Role check completed")
                .detail("userId", userId)
                .detail("roleName", roleName)
                .detail("hasRole", String.valueOf(hasRole))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_CHECK_RESULT)
                .log();

        return hasRole;
    }

    /**
     * Builds HTTP headers and forwards JWT token
     * from the incoming request.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    headers.set("Authorization", authHeader);
                    logger.trace()
                            .message("Forwarding JWT token to permissions service")
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_JWT_FORWARDING)
                            .log();
                } else {
                    logger.trace()
                            .message("No valid Authorization header found")
                            .detail("hasAuthHeader", String.valueOf(authHeader != null))
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_JWT_FORWARDING)
                            .log();
                }
            } else {
                logger.trace()
                        .message("No request context available for JWT forwarding")
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_JWT_FORWARDING)
                        .log();
            }
        } catch (Exception e) {
            logger.warn()
                    .message("Unable to forward JWT token")
                    .detail("errorMessage", e.getMessage())
                    .detail("errorType", e.getClass().getSimpleName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_JWT_FORWARDING_ERROR)
                    .log();
        }

        return headers;
    }

    /**
     * Fetch active role IDs for a specific user in a tenant.
     *
     * @param userId   user identity ID
     * @param tenantId tenant identifier
     * @return set of active role IDs
     */
    @SuppressWarnings("unchecked")
    public Set<UUID> getActiveRoleIds(UUID userId, String tenantId) {
        logger.debug()
                .message("Fetching active role IDs for user")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACTIVE_ROLES_FETCH)
                .log();

        Set<UUID> activeRoleIds = getAllUserRoles(String.valueOf(userId)).stream()
                .filter(role -> tenantId.equals(role.get("tenantId")))
                .filter(role -> Boolean.TRUE.equals(role.get("roleActive")))
                .map(role -> {
                    try {
                        return UUID.fromString((String) role.get("roleId"));
                    } catch (Exception e) {
                        logger.warn()
                                .message("Failed to parse role ID as UUID")
                                .detail("roleId", String.valueOf(role.get("roleId")))
                                .detail("userId", userId.toString())
                                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_PARSING_ERROR)
                                .log();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        logger.debug()
                .message("Retrieved active role IDs for user")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .detail("activeRoleCount", String.valueOf(activeRoleIds.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACTIVE_ROLES_RETRIEVED)
                .log();

        return activeRoleIds;
    }

    /**
     * Health check for permissions service.
     *
     * @return true if service is reachable
     */
    public boolean isReachable() {
        logger.trace()
                .message("Performing health check on permissions service")
                .detail("serviceUrl", permissionsServiceUrl)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_HEALTH_CHECK)
                .log();

        try {
            String url = permissionsServiceUrl + "/actuator/health";
            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            boolean isReachable = response.getStatusCode().is2xxSuccessful();

            logger.debug()
                    .message("Permissions service health check completed")
                    .detail("isReachable", String.valueOf(isReachable))
                    .detail("statusCode", response.getStatusCode().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_HEALTH_CHECK_RESULT)
                    .log();

            return isReachable;

        } catch (Exception e) {
            logger.warn()
                    .message("Permissions service health check failed")
                    .detail("serviceUrl", permissionsServiceUrl)
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_HEALTH_CHECK_FAILED)
                    .log();
            return false;
        }
    }

    /**
     * Fetch a single role by its ID.
     * <p>
     * Endpoint:
     * GET /api/v1/roles/{roleId}
     *
     * @param roleId the role UUID
     * @return role details map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRoleById(UUID roleId) {
        logger.debug()
                .message("Fetching role by ID from permissions service")
                .detail("roleId", roleId.toString())
                .detail("serviceUrl", permissionsServiceUrl)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                .log();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            String url = permissionsServiceUrl + "/api/v1/roles/{roleId}";

            logger.trace()
                    .message("Executing GET request to fetch role")
                    .detail("url", url)
                    .detail("roleId", roleId.toString())
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

                logger.debug()
                        .message("Successfully fetched role by ID")
                        .detail("roleId", roleId.toString())
                        .detail("roleName", String.valueOf(roleData.get("roleName")))
                        .detail("statusCode", response.getStatusCode().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_SUCCESS)
                        .log();

                return roleData;
            }

            logger.warn()
                    .message("Unexpected response while fetching role by ID")
                    .detail("roleId", roleId.toString())
                    .detail("statusCode", response.getStatusCode().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_WARNING)
                    .log();

            return Collections.emptyMap();

        } catch (Exception e) {
            logger.error()
                    .message("Failed to fetch role by ID from permissions service")
                    .detail("roleId", roleId.toString())
                    .detail("serviceUrl", permissionsServiceUrl)
                    .detail("errorMessage", e.getMessage())
                    .detail("errorType", e.getClass().getSimpleName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_ERROR)
                    .log();

            throw new RuntimeException("Role fetch by ID failed for: " + roleId, e);
        }
    }


    /**
     * Fetch users who have a specific role.
     * <p>
     * Endpoint:
     * GET /api/v1/roles/{roleId}/users
     *
     * @param roleId the role UUID
     * @param page   page number (0-indexed)
     * @param size   page size
     * @param status status filter (ALL, ACTIVE, INACTIVE)
     * @param search search query for username, displayName, or email
     * @return response containing users with role assignment status
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUsersByRole(UUID roleId, Integer page, Integer size, String status, String search) {
        logger.debug()
                .message("Fetching users by role from permissions service")
                .detail("roleId", roleId.toString())
                .detail("page", String.valueOf(page))
                .detail("size", String.valueOf(size))
                .detail("status", status)
                .detail("search", search)
                .detail("serviceUrl", permissionsServiceUrl)
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_CALL)
                .log();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

            String url = permissionsServiceUrl + "/api/v1/roles/{roleId}/users?page={page}&size={size}&status={status}";
            if (search != null && !search.trim().isEmpty()) {
                url += "&search={search}";
            }

            logger.trace()
                    .message("Executing GET request to fetch users by role")
                    .detail("url", url)
                    .detail("roleId", roleId.toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_HTTP_REQUEST)
                    .log();

            Map<String, Object> uriVariables = new HashMap<>();
            uriVariables.put("roleId", roleId);
            uriVariables.put("page", page);
            uriVariables.put("size", size);
            uriVariables.put("status", status);
            if (search != null && !search.trim().isEmpty()) {
                uriVariables.put("search", search);
            }

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    uriVariables
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                logger.debug()
                        .message("Successfully fetched users by role")
                        .detail("roleId", roleId.toString())
                        .detail("totalCount", String.valueOf(body.get("totalCount")))
                        .detail("usersWithRoleCount", String.valueOf(body.get("usersWithRoleCount")))
                        .detail("statusCode", response.getStatusCode().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_SUCCESS)
                        .log();

                return body;
            }

            logger.warn()
                    .message("Unexpected response while fetching users by role")
                    .detail("roleId", roleId.toString())
                    .detail("statusCode", response.getStatusCode().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_WARNING)
                    .log();

            return Collections.emptyMap();

        } catch (Exception e) {
            logger.error()
                    .message("Failed to fetch users by role from permissions service")
                    .detail("roleId", roleId.toString())
                    .detail("serviceUrl", permissionsServiceUrl)
                    .detail("errorMessage", e.getMessage())
                    .detail("errorType", e.getClass().getSimpleName())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_EXTERNAL_SERVICE_ERROR)
                    .log();

            throw new RuntimeException("Users by role fetch failed for: " + roleId, e);
        }
    }

    /**
     * Check if specific users already have a role assigned.
     *
     * @param roleId  the role UUID
     * @param userIds list of user IDs to check
     * @return map of userId to boolean indicating if they have the role
     */
    public Map<UUID, Boolean> checkUsersHaveRole(UUID roleId, List<UUID> userIds) {
        logger.debug()
                .message("Checking if users have role")
                .detail("roleId", roleId.toString())
                .detail("userCount", String.valueOf(userIds.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_CHECK)
                .log();

        Map<UUID, Boolean> results = new HashMap<>();

        try {
            // Fetch all users with this role
            Map<String, Object> response = getUsersByRole(roleId, 0, 1000, "ACTIVE", null);

            if (response.get("users") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> users = (List<Map<String, Object>>) response.get("users");

                // Extract user IDs who have the role
                Set<String> usersWithRole = users.stream()
                        .filter(user -> Boolean.TRUE.equals(user.get("hasRole")))
                        .map(user -> (String) user.get("id"))
                        .collect(Collectors.toSet());

                // Check each requested user
                for (UUID userId : userIds) {
                    results.put(userId, usersWithRole.contains(userId.toString()));
                }
            }
            

            logger.debug()
                    .message("Completed role check for users")
                    .detail("roleId", roleId.toString())
                    .detail("checkedCount", String.valueOf(results.size()))
                    .detail("usersWithRole", String.valueOf(results.values().stream().filter(Boolean::booleanValue).count()))
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_CHECK_RESULT)
                    .log();

            return results;

        } catch (Exception e) {
            logger.error()
                    .message("Failed to check users for role")
                    .detail("roleId", roleId.toString())
                    .detail("errorMessage", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_CHECK_ERROR)
                    .log();

            // Return all false on error
            for (UUID userId : userIds) {
                results.put(userId, false);
            }
            return results;
        }
    }
}