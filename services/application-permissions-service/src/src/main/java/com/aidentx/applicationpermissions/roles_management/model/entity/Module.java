package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing a Module for organizing pages and resources.
 * 
 * <p>Modules provide a way to organize related pages or resources together
 * for better navigation and management. This helps in creating logical
 * groupings of functionality within the application.</p>
 * 
 * <p>Modules can be used to:</p>
 * <ul>
 *   <li>Organize related pages under common categories</li>
 *   <li>Simplify navigation and user interface organization</li>
 *   <li>Apply bulk operations or permissions to related resources</li>
 *   <li>Create hierarchical structures for complex applications</li>
 * </ul>
 * 
 * <p>Example modules:</p>
 * <ul>
 *   <li>User Management - containing user-related pages</li>
 *   <li>Financial - containing financial reporting pages</li>
 *   <li>System Administration - containing system config pages</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "module_table")
public class Module {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "module_name", nullable = false)
    private String moduleName;
    
    /**
     * Tenant ID for multi-tenancy support.
     * When null, the module is system-wide/static and available to all tenants.
     * When not null, the module belongs to a specific tenant and can be customized/deleted.
     */
    @Column(name = "tenant_id", nullable = true)
    private String tenantId;

    // Constructors
    public Module() {}

    public Module(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public Module(String moduleName, String tenantId) {
        this.moduleName = moduleName;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    /**
     * Checks if this module is system-wide (static) or tenant-specific.
     * @return true if this is a system-wide module (tenantId is null), false if tenant-specific
     */
    public boolean isSystemModule() {
        return tenantId == null;
    }
}