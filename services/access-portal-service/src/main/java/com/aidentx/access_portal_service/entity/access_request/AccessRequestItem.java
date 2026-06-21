package com.aidentx.access_portal_service.entity.access_request;


import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import com.aidentx.access_portal_service.entity.tile.Tile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "access_request_items")
@Getter
@Setter
public class AccessRequestItem extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "access_request_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_request_items_request")
    )
    private AccessRequest accessRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tile_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_request_items_tile")
    )
    private Tile tile;

    @Column(nullable = false, length = 50)
private String itemType;   // INTERNAL_ROLE, BUSINESS_ROLE (not present), APPLICATION (not to do currently), ENTITLEMENT (need to do)

    @Column(nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestTarget requestTarget; // SELF / OTHERS

    @Column(columnDefinition = "TEXT")
    private String beneficiaryUserIds; // Comma-separated UUID string

    @Column(columnDefinition = "TEXT")
    private String beneficiaryNames; // Comma-separated names

    // ===== USER INPUT FROM CART =====

    @Column(nullable = false)
    private Integer validityDays;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String justification;


      /**
     * CHANGED: OneToOne -> OneToMany
     * One request item can have multiple provisioning records
     * (one per role+user combination)
     */
    @OneToMany(
            mappedBy = "requestItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Fetch(FetchMode.SUBSELECT) // To avoid Cartesian product issues, it works by fetching in a separate query for collections
    private Set<RequestProvisioning> provisioning = new LinkedHashSet<>();


    /**
     * One request item can have multiple approval steps if it involves multiple roles/users
     */
    @OneToMany(
       mappedBy = "requestItem",
       cascade = CascadeType.ALL,
       orphanRemoval = true,
       fetch = FetchType.LAZY
   )
   @Fetch(FetchMode.SUBSELECT)
   private Set<RequestApprovalStep> approvalSteps = new LinkedHashSet<>();
}

