package com.aidentx.access_portal_service.entity.tile;

import com.aidentx.access_portal_service.dto.request.tile.TileVisualFilterRuleDTO;
import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

/**
 * Core Tile entity representing an access portal tile configuration.
 */
@Entity
@Table(name = "tiles")
@Getter
@Setter
public class Tile extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;


    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String icon;

    @Column
    private String category;

    @Column(nullable = false)
    private boolean tileActive;

    @Column(nullable = false)
    private boolean requireManagerApproval;

    @Column
    private UUID workflowId; // Workflow to use when requireManagerApproval is true

    @Column
    private String maximumRiskLevel;

    @Column(nullable = false)
    private boolean allowRequestForOthers;

    @Column(nullable = false)
    private boolean objectFilteringEnabled;

    @Column(nullable = false)
    private boolean allowApplications;

    @Column(nullable = false)
    private boolean allowBusinessRoles;

    @Column(nullable = false)
    private boolean allowInternalRoles;

     @Column(nullable = false)
    private boolean allowEntitlements;

    // One-to-many relationships for internal foreign keys
    @OneToMany(mappedBy = "tile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TileAllowedRole> allowedRoles = new HashSet<>();

    @OneToMany(mappedBy = "tile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TileAccessScopeItem> accessScopeItems = new HashSet<>();

    @OneToMany(mappedBy = "tile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TileVisualFilter> visualFilters = new HashSet<>();

    public void addAllowedRole(UUID roleCode) {
        TileAllowedRole role = new TileAllowedRole();
        role.setTile(this);
        role.setTenantId(this.getTenantId()); // Set tenantId from parent
        role.setRoleCode(roleCode);
        this.allowedRoles.add(role);
    }

    public void addAccessScopeItem(String itemType, UUID itemCode) {
        TileAccessScopeItem item = new TileAccessScopeItem();

        item.setTile(this);
        item.setItemType(itemType);
        item.setItemCode(itemCode);
        item.setTenantId(this.getTenantId()); // Set tenantId from parent
        this.accessScopeItems.add(item);
    }

    public void addVisualFilterRule(TileVisualFilter filter, TileVisualFilterRuleDTO ruleDTO) {
        TileVisualFilterRule rule = new TileVisualFilterRule();
        rule.setFilter(filter);
        rule.setFieldName(ruleDTO.getFieldName());
        rule.setLogicalOperator(ruleDTO.getLogicalOperator());
        rule.setOperator(ruleDTO.getOperator());
        rule.setValue(ruleDTO.getValue());
        rule.setTenantId(this.getTenantId()); // Get tenantId from tile
        filter.getRules().add(rule);
    }
}
