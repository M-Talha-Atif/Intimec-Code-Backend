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
 * Individual rule inside a visual filter for a tile.
 */
@Entity
@Table(name = "tile_visual_filter_rules")
@Getter
@Setter
public class TileVisualFilterRule extends TenantAwareEntity {

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
            name = "filter_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tile_visual_filter_rules_filter")
    )
    private TileVisualFilter filter;

    @Column(nullable = false)
    private String fieldName; // department, risk_level, etc.

    @Column(nullable = true)
    private String logicalOperator; // AND, OR, NOT

    @Column(nullable = false)
    private String operator; // EQ, NE, GT, LT, IN, NOT_IN

    @Column(nullable = false)
    private String value; // store as string or JSON list
}
