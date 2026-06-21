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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a visual filter group for a tile.
 */
@Entity
@Table(name = "tile_visual_filters")
@Getter
@Setter
public class TileVisualFilter extends TenantAwareEntity {

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
            foreignKey = @ForeignKey(name = "fk_tile_visual_filters_tile")
    )
    private Tile tile;


    @Column(nullable = false)
    private String filterName; // e.g., Department Filter, Risk Level Filter


    // One-to-many relationship to rules
    @OneToMany(mappedBy = "filter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TileVisualFilterRule> rules = new ArrayList<>();

}
