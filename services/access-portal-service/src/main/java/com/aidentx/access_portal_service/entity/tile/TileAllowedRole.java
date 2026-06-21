package com.aidentx.access_portal_service.entity.tile;

import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * Allowed roles for a specific tile.
 */
@Entity
@Table(name = "tile_allowed_roles")
@Getter
@Setter
public class TileAllowedRole extends TenantAwareEntity {

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
            foreignKey = @ForeignKey(name = "fk_tile_allowed_roles_tile")
    )
    private Tile tile;

    @Column(nullable = false)
    private UUID roleCode;
}
