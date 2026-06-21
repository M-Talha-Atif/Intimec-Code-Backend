package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Module entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * modules for page organization.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

    /**
     * Find a module by its name.
     * 
     * @param moduleName the name of the module
     * @return Optional containing the module if found
     */
    Optional<Module> findByModuleName(String moduleName);

    /**
     * Find modules by name pattern.
     * 
     * @param namePattern the pattern to search for in module names
     * @return list of matching modules
     */
    List<Module> findByModuleNameContainingIgnoreCase(String namePattern);

    /**
     * Find all modules ordered by name.
     * 
     * @return list of modules ordered by name
     */
    List<Module> findAllByOrderByModuleName();

    /**
     * Check if a module exists with the given name.
     * 
     * @param moduleName the name to check
     * @return true if module exists, false otherwise
     */
    boolean existsByModuleName(String moduleName);
    
    // --- Multi-tenancy support methods ---
    
    /**
     * Find all system-wide (static) modules that are available to all tenants.
     * These modules cannot be deleted and are managed by the system.
     * 
     * @return list of system modules (tenantId is null)
     */
    List<Module> findByTenantIdIsNull();
    
    /**
     * Find all modules belonging to a specific tenant.
     * These modules can be customized and deleted by the tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of tenant-specific modules
     */
    List<Module> findByTenantId(String tenantId);
    
    /**
     * Find all modules available to a specific tenant (both system and tenant-specific).
     * This includes system-wide modules (tenantId = null) and tenant's custom modules.
     * 
     * @param tenantId the tenant identifier
     * @return list of all modules available to the tenant
     */
    @Query("SELECT m FROM Module m WHERE m.tenantId IS NULL OR m.tenantId = :tenantId ORDER BY m.moduleName")
    List<Module> findAllAvailableToTenant(@Param("tenantId") String tenantId);
    
    /**
     * Find module by name and tenant (including system modules).
     * 
     * @param moduleName the name of the module
     * @param tenantId the tenant identifier
     * @return Optional containing the module if found
     */
    @Query("SELECT m FROM Module m WHERE m.moduleName = :moduleName AND (m.tenantId IS NULL OR m.tenantId = :tenantId)")
    Optional<Module> findByModuleNameAndTenant(@Param("moduleName") String moduleName, @Param("tenantId") String tenantId);
    
    /**
     * Check if a tenant can delete this module (only tenant-specific modules can be deleted).
     * 
     * @param moduleId the module ID
     * @param tenantId the tenant identifier
     * @return true if the module belongs to the tenant and can be deleted
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Module m WHERE m.id = :moduleId AND m.tenantId = :tenantId")
    boolean canTenantDeleteModule(@Param("moduleId") UUID moduleId, @Param("tenantId") String tenantId);

}