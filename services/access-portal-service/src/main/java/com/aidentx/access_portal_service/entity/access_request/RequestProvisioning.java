package com.aidentx.access_portal_service.entity.access_request;

import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity tracking the provisioning status of an approved access request.
 * Links access requests to actual system provisioning.
 * Each record represents provisioning for ONE (role + user) combination.
 */
@Entity
@Table(name = "access_request_provisioning", indexes = {
    @Index(name = "idx_provisioning_request_role_user",
           columnList = "access_request_id, target_user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestProvisioning extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "access_request_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_provisioning_request_item")
    )
    private AccessRequestItem requestItem;

    /**
     * Direct reference to access request for easier querying
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "access_request_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_provisioning_access_request")
    )
    private AccessRequest accessRequest;

    /**
     * Target user receiving this role
     */
    @Column(nullable = false, length = 100)
    private String targetUserId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProvisioningStatus status = ProvisioningStatus.PENDING;

    @Column
    private OffsetDateTime startedAt;

    @Column
    private OffsetDateTime completedAt;

    @Column(length = 100)
    private String targetSystem;

    @Column(length = 200)
    private String externalTicketId;

    @Column
    private Integer retryCount;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String provisioningDetails;

    @Column
    private OffsetDateTime scheduledProvisioningAt;

    @Column
    private OffsetDateTime accessExpiresAt;

    @Column
    private UUID provisionedByUserId;

    @Column(length = 200)
    private String provisionedByUserName;

}