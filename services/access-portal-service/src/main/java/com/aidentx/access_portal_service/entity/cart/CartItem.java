package com.aidentx.access_portal_service.entity.cart;

import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import com.aidentx.access_portal_service.entity.tile.Tile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * Cart Item entity representing items added to user's cart.
 * Users add applications, internal roles, or entitlements to cart before submitting request.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
public class CartItem extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tile_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_tile")
    )
    private Tile tile;

    @Column(nullable = false, length = 50)
    private String itemType;

    @Column(nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private String itemName;

    @Column(columnDefinition = "TEXT")
    private String itemDescription;


    @Column(nullable = false, length = 50)
    private String requestType;

    @Column(columnDefinition = "TEXT")
    private String beneficiaryUserIds; // Comma-separated UUID string: "uuid1,uuid2,uuid3"

    @Column(columnDefinition = "TEXT")
    private String beneficiaryNames; // Comma-separated names: "John Doe,Jane Smith"

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CartStatus status;
}