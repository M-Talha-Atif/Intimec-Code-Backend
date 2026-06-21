package com.aidentx.access_portal_service.entity.access_request;

import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing an approval step in the access request workflow.
 * Tracks the approval chain and decision-making process for each (role + user + step).
 */
@Entity
@Table(name = "access_request_approval_steps", indexes = {
    @Index(name = "idx_approval_step_request_item",
           columnList = "access_request_id, access_request_item_id, step_order")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestApprovalStep extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "access_request_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_approval_step_request")
    )
    private AccessRequest accessRequest;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
    name = "access_request_item_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_approval_step_item")
    )
    private AccessRequestItem requestItem;

    /**
     * Target user receiving this role - supports multi-user requests
     */
    @Column(nullable = false, length = 100)
    private String targetUserId;

    @Column(nullable = false, length = 200)
    private String targetUserName;

    /**
     * Step order: 1 = Manager approval, 2 = Role owner approval
     */
    @Column(nullable = false)
    private Integer stepOrder;

    /**
     * Approver role type: MANAGER, ROLE_OWNER, etc.
     */
    @Column(nullable = false, length = 100)
    private String approverRole;

    /**
     * ID of the assigned approver
     */
    @Column
    private String assignedApproverId;

    @Column(length = 200)
    private String assignedApproverName;

    @Column(length = 200)
    private String assignedApproverEmail;

    /**
     * Current status of this approval step
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING_ASSIGNMENT;

    /**
     * When this approval was assigned
     */
    @Column
    private OffsetDateTime createdAt;

    /**
     * When the approver responded
     */
    @Column
    private OffsetDateTime respondedAt;

    /**
     * Approval decision
     */
    @Column
    @Enumerated(EnumType.STRING)
    private ApprovalDecision decision;

    /**
     * Comments from approver
     */
    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column
    private UUID delegatedToUserId;

    @Column(length = 200)
    private String delegatedToUserName;

    @Column
    private OffsetDateTime delegatedAt;

}