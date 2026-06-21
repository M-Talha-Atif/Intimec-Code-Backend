package com.aidentx.access_portal_service.entity.access_request;


import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.time.OffsetDateTime;

@Entity
@Table(name = "access_requests")
@Getter
@Setter
public class AccessRequest extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID requestedByUserId;

    @Column(nullable = false)
    private String requestedByUserName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String overallJustification;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.DRAFT;

    @Column(nullable = false)
    private OffsetDateTime requestedAt;


    @OneToMany(
            mappedBy = "accessRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT) // To avoid Cartesian product issues, it works by fetching in a separate query for collections
   private Set<AccessRequestItem> items = new LinkedHashSet<>();

//    @OneToMany(
//            mappedBy = "accessRequest",
//            cascade = CascadeType.ALL,
//            orphanRemoval = true
//    )
//    @Fetch(FetchMode.SUBSELECT) // To avoid Cartesian product issues, it works by fetching in a separate query for collections
//    private Set<RequestApprovalStep> approvalSteps = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "accessRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT) // To avoid Cartesian product issues, it works by fetching in a separate query for collections
    private Set<RequestComment> comments = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "accessRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT) // To avoid Cartesian product issues, it works by fetching in a separate query for collections
    private Set<RequestAuditLog> auditLogs = new LinkedHashSet<>();
}

