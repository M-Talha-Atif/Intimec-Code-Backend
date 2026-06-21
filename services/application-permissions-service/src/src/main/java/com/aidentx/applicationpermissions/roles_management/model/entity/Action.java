package com.aidentx.applicationpermissions.roles_management.model.entity;

import com.aidentx.applicationpermissions.roles_management.enums.HttpMethod;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Entity representing an Action that can be performed within the system.
 * 
 * <p>Actions define the specific operations that can be performed, such as
 * CREATE, READ, UPDATE, DELETE, or custom business-specific actions like
 * APPROVE, REJECT, EXPORT, etc.</p>
 * 
 * <p>Actions are used in conjunction with pages and conditional rules to
 * create fine-grained permissions. Each permission references an action
 * to specify what operation is being authorized.</p>
 * 
 * <p>Common action examples:</p>
 * <ul>
 *   <li>VIEW - Read access to resources</li>
 *   <li>EDIT - Modify existing resources</li>
 *   <li>CREATE - Create new resources</li>
 *   <li>DELETE - Remove resources</li>
 *   <li>APPROVE - Business workflow actions</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "action")
public class Action {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "action_name", nullable = false)
    private String actionName;
    
    @Column(name = "endpoint", length = 1000)
    private String endpoint;
    
    /**
     * HTTP method for this action (GET, POST, PUT, PATCH, DELETE).
     * Can be null for actions that are not directly tied to HTTP endpoints.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = true)
    private HttpMethod method;
    
    /**
     * ID of the page this action belongs to.
     */
    @Column(name = "page_id", nullable = true)
    private UUID pageId;
    
    /**
     * Tenant ID for multi-tenancy support.
     * When null, the action is system-wide/static and available to all tenants.
     * When not null, the action belongs to a specific tenant and can be customized/deleted.
     */
    @Column(name = "tenant_id", nullable = true)
    private String tenantId;

    // Constructors
    public Action() {}

    public Action(String actionName, String endpoint) {
        this.actionName = actionName;
        this.endpoint = endpoint;
    }
    
    public Action(String actionName, String endpoint, HttpMethod method) {
        this.actionName = actionName;
        this.endpoint = endpoint;
        this.method = method;
    }
    
    public Action(String actionName, String endpoint, HttpMethod method, String tenantId) {
        this.actionName = actionName;
        this.endpoint = endpoint;
        this.method = method;
        this.tenantId = tenantId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public HttpMethod getMethod() {
        return method;
    }
    
    public void setMethod(HttpMethod method) {
        this.method = method;
    }
    
    public UUID getPageId() {
        return pageId;
    }
    
    public void setPageId(UUID pageId) {
        this.pageId = pageId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    /**
     * Checks if this action is system-wide (static) or tenant-specific.
     * @return true if this is a system-wide action (tenantId is null), false if tenant-specific
     */
    public boolean isSystemAction() {
        return tenantId == null;
    }
}
