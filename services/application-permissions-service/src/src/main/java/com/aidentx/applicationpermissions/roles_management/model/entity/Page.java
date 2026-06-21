package com.aidentx.applicationpermissions.roles_management.model.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a Page or resource within the application.
 * 
 * <p>Pages represent the various screens, views, or resources in the application
 * that can be accessed by users. Each page can be associated with a group for
 * organizational purposes and can have multiple permissions defined for it.</p>
 * 
 * <p>Pages serve as the target resources for permissions, defining what parts
 * of the application users can interact with based on their assigned roles.</p>
 * 
 * <p>Example pages:</p>
 * <ul>
 *   <li>User Management Dashboard</li>
 *   <li>Financial Reports</li>
 *   <li>System Settings</li>
 *   <li>Audit Logs</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "page")
public class Page {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "page_name", nullable = false)
    private String pageName;
    
    /**
     * The URL path or route for this page.
     * Used for navigation and routing purposes.
     */
    @Column(name = "page_path", nullable = true)
    private String pagePath;
    
    /**
     * ID of the module this page belongs to.
     */
    @Column(name = "module_id", nullable = true)
    private UUID moduleId;
    
    /**
     * Tenant ID for multi-tenancy support.
     * When null, the page is system-wide/static and available to all tenants.
     * When not null, the page belongs to a specific tenant and can be customized/deleted.
     */
    @Column(name = "tenant_id", nullable = true)
    private String tenantId;

    // Constructors
    public Page() {}

    public Page(String pageName) {
        this.pageName = pageName;
    }
    
    public Page(String pageName, String tenantId) {
        this.pageName = pageName;
        this.tenantId = tenantId;
    }
    
    public Page(String pageName, String pagePath, String tenantId) {
        this.pageName = pageName;
        this.pagePath = pagePath;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
    
    public String getPagePath() {
        return pagePath;
    }
    
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }
    
    public UUID getModuleId() {
        return moduleId;
    }
    
    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }

    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    /**
     * Checks if this page is system-wide (static) or tenant-specific.
     * @return true if this is a system-wide page (tenantId is null), false if tenant-specific
     */
    public boolean isSystemPage() {
        return tenantId == null;
    }
}