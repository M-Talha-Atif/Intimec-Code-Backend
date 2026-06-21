package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import com.aidentx.applicationpermissions.roles_management.model.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Action entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * actions in the role-based access control system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ActionRepository extends JpaRepository<Action, UUID> {

    /**
     * Find an action by its name.
     * 
     * @param actionName the name of the action
     * @return Optional containing the action if found
     */
    Optional<Action> findByActionName(String actionName);
    
    /**
     * Find actions by page ID.
     * 
     * @param pageId the page ID to search for
     * @return list of actions belonging to the page
     */
    List<Action> findByPageId(UUID pageId);
    
    /**
     * Find an action by name and page ID.
     * 
     * @param actionName the name of the action
     * @param pageId the page ID
     * @return Optional containing the action if found
     */
    Optional<Action> findByActionNameAndPageId(String actionName, UUID pageId);

    /**
     * Find actions by name pattern.
     * 
     * @param namePattern the pattern to search for in action names
     * @return list of matching actions
     */
    List<Action> findByActionNameContainingIgnoreCase(String namePattern);

    /**
     * Find all actions ordered by name.
     * 
     * @return list of actions ordered by name
     */
    List<Action> findAllByOrderByActionName();

    /**
     * Check if an action exists with the given name.
     * 
     * @param actionName the name to check
     * @return true if action exists, false otherwise
     */
    boolean existsByActionName(String actionName);
    
    // --- Multi-tenancy support methods ---
    
    /**
     * Find all system-wide (static) actions that are available to all tenants.
     * These actions cannot be deleted and are managed by the system.
     * 
     * @return list of system actions (tenantId is null)
     */
    List<Action> findByTenantIdIsNull();
    
    /**
     * Find all actions belonging to a specific tenant.
     * These actions can be customized and deleted by the tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of tenant-specific actions
     */
    List<Action> findByTenantId(String tenantId);
    
    /**
     * Find all actions available to a specific tenant (both system and tenant-specific).
     * This includes system-wide actions (tenantId = null) and tenant's custom actions.
     * 
     * @param tenantId the tenant identifier
     * @return list of all actions available to the tenant
     */
    @Query("SELECT a FROM Action a WHERE a.tenantId IS NULL OR a.tenantId = :tenantId ORDER BY a.actionName")
    List<Action> findAllAvailableToTenant(@Param("tenantId") String tenantId);
    
    /**
     * Find action by name and tenant (including system actions).
     * 
     * @param actionName the name of the action
     * @param tenantId the tenant identifier
     * @return Optional containing the action if found
     */
    @Query("SELECT a FROM Action a WHERE a.actionName = :actionName AND (a.tenantId IS NULL OR a.tenantId = :tenantId)")
    Optional<Action> findByActionNameAndTenant(@Param("actionName") String actionName, @Param("tenantId") String tenantId);
    
    /**
     * Check if a tenant can delete this action (only tenant-specific actions can be deleted).
     * 
     * @param actionId the action ID
     * @param tenantId the tenant identifier
     * @return true if the action belongs to the tenant and can be deleted
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Action a WHERE a.id = :actionId AND a.tenantId = :tenantId")
    boolean canTenantDeleteAction(@Param("actionId") UUID actionId, @Param("tenantId") String tenantId);

    /**
     * Find actions by endpoint and HTTP method.
     * 
     * @param endpoint the API endpoint (e.g., "/api/v1/audit-logs")
     * @param method the HTTP method enum (e.g., HttpMethod.GET)
     * @return list of actions matching the endpoint and method
     */
    List<Action> findByEndpointAndMethod(String endpoint, HttpMethod method);
    
    /**
     * Find actions by endpoint pattern matching and HTTP method.
     * Supports wildcards: /api/v1/identities/* matches /api/v1/identities/123
     * 
     * @param method the HTTP method enum (e.g., HttpMethod.GET)
     * @return list of all actions with the given method and non-null endpoint
     */
    @Query("SELECT a FROM Action a WHERE a.method = :method AND a.endpoint IS NOT NULL")
    List<Action> findAllByMethodWithEndpoint(@Param("method") HttpMethod method);
}