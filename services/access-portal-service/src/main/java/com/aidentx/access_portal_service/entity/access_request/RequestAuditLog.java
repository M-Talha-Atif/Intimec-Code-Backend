package com.aidentx.access_portal_service.entity.access_request;

import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity for auditing all changes to an access request.
 * Provides complete audit trail for compliance and debugging.
 */
@Entity
@Table(name = "access_request_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAuditLog extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "access_request_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_audit_log_request")
    )
    private AccessRequest accessRequest;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(nullable = false, length = 200)
    private String performedBy;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 50)
    private String previousStatus;

    @Column(length = 50)
    private String newStatus;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

}