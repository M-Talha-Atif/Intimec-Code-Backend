package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Page entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * pages/resources in the role-based access control system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {

    /**
     * Find a page by its name.
     * 
     * @param pageName the name of the page
     * @return Optional containing the page if found
     */
    Optional<Page> findByPageName(String pageName);
    
    /**
     * Find a page by its path.
     * 
     * @param pagePath the path of the page (e.g., "/dashboard", "/settings/sso")
     * @return Optional containing the page if found
     */
    Optional<Page> findByPagePath(String pagePath);
    
    /**
     * Find pages by module ID.
     * 
     * @param moduleId the module ID to search for
     * @return list of pages belonging to the module
     */
    List<Page> findByModuleId(UUID moduleId);

    /**
     * Find pages by name pattern.
     * 
     * @param namePattern the pattern to search for in page names
     * @return list of matching pages
     */
    List<Page> findByPageNameContainingIgnoreCase(String namePattern);

    /**
     * Find all pages ordered by name.
     * 
     * @return list of pages ordered by name
     */
    List<Page> findAllByOrderByPageName();

    /**
     * Check if a page exists with the given name.
     * 
     * @param pageName the name to check
     * @return true if page exists, false otherwise
     */
    boolean existsByPageName(String pageName);
    
    // --- Multi-tenancy support methods ---
    
    /**
     * Find all system-wide (static) pages that are available to all tenants.
     * These pages cannot be deleted and are managed by the system.
     * 
     * @return list of system pages (tenantId is null)
     */
    List<Page> findByTenantIdIsNull();
    
    /**
     * Find all pages belonging to a specific tenant.
     * These pages can be customized and deleted by the tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of tenant-specific pages
     */
    List<Page> findByTenantId(String tenantId);
    
    /**
     * Find all pages available to a specific tenant (both system and tenant-specific).
     * This includes system-wide pages (tenantId = null) and tenant's custom pages.
     * 
     * @param tenantId the tenant identifier
     * @return list of all pages available to the tenant
     */
    @Query("SELECT p FROM Page p WHERE p.tenantId IS NULL OR p.tenantId = :tenantId ORDER BY p.pageName")
    List<Page> findAllAvailableToTenant(@Param("tenantId") String tenantId);
    
    /**
     * Find page by name and tenant (including system pages).
     * 
     * @param pageName the name of the page
     * @param tenantId the tenant identifier
     * @return Optional containing the page if found
     */
    @Query("SELECT p FROM Page p WHERE p.pageName = :pageName AND (p.tenantId IS NULL OR p.tenantId = :tenantId)")
    Optional<Page> findByPageNameAndTenant(@Param("pageName") String pageName, @Param("tenantId") String tenantId);
    
    /**
     * Check if a tenant can delete this page (only tenant-specific pages can be deleted).
     * 
     * @param pageId the page ID
     * @param tenantId the tenant identifier
     * @return true if the page belongs to the tenant and can be deleted
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Page p WHERE p.id = :pageId AND p.tenantId = :tenantId")
    boolean canTenantDeletePage(@Param("pageId") UUID pageId, @Param("tenantId") String tenantId);

}