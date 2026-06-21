package com.aidentx.access_portal_service.entity.tile;


import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * Configurable items for tile access scope.
 * For example, selected applications, internal roles, or business roles.
 */
@Entity
@Table(name = "tile_access_scope_items")
@Getter
@Setter
public class TileAccessScopeItem extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tile_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tile_access_scope_items_tile")
    )
    private Tile tile;

    @Column(nullable = false)
    private String itemType; // APPLICATION, BUSINESS_ROLE, INTERNAL_ROLE, EN

    @Column(nullable = false)
    private UUID itemCode;

}

