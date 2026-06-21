package com.aidentx.access_portal_service.entity.access_request;

import com.aidentx.access_portal_service.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a comment on an access request.
 * Supports threaded discussions and approval workflow communication.
 */
@Entity
@Table(name = "access_request_comments")
@Getter
@Setter
public class RequestComment extends TenantAwareEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "access_request_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_request")
    )
    private AccessRequest accessRequest;

    @Column(nullable = false)
    private UUID commentedByUserId;

    @Column(nullable = false, length = 200)
    private String commentedByUserName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CommentType commentType;

    @Column
    private UUID parentCommentId;

    @Column(nullable = false)
    private OffsetDateTime commentedAt;

    @Column
    private Boolean isInternal;

    @Column
    private UUID mentionedUserId;
}