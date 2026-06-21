package com.aidentx.access_portal_service.repository.access_request;

import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.access_portal_service.repository.access_request.projection.StatusCountProjection;

import java.time.OffsetDateTime;

/**
 * Repository for AccessRequest entity operations.
 */
@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, UUID> {

    /**
     * Finds requests by tenant and user with optional status filter.
     *
     * @param tenantId tenant identifier
     * @param userId   user identifier
     * @param status   request status
     * @param pageable pagination information
     * @return paginated requests
     */
    Page<AccessRequest> findByTenantIdAndRequestedByUserIdAndStatus(
            String tenantId,
            UUID userId,
            RequestStatus status,
            Pageable pageable
    );

    /**
     * Finds all requests by tenant and user.
     *
     * @param tenantId tenant identifier
     * @param userId   user identifier
     * @param pageable pagination information
     * @return paginated requests
     */
    Page<AccessRequest> findByTenantIdAndRequestedByUserId(
            String tenantId,
            UUID userId,
            Pageable pageable
    );

    /**
     * Finds a request by ID and tenant for basic details.
     *
     * @param requestId request identifier
     * @param tenantId  tenant identifier
     * @return optional request
     */
    Optional<AccessRequest> findByIdAndTenantId(UUID requestId, String tenantId);

    /**
     * Finds a request with items eagerly loaded.
     * Use this for fetching items first, then load other collections separately.
     *
     * @param requestId request identifier
     * @param tenantId  tenant identifier
     * @return optional request with items
     */
    @Query("""
            SELECT DISTINCT r FROM AccessRequest r
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.tile
            LEFT JOIN FETCH i.provisioning
            WHERE r.id = :requestId
            AND r.tenantId = :tenantId
            """)
    Optional<AccessRequest> findByIdWithItems(
            @Param("requestId") UUID requestId,
            @Param("tenantId") String tenantId
    );


    /**
     * Finds a request with comments eagerly loaded.
     *
     * @param requestId request identifier
     * @param tenantId  tenant identifier
     * @return optional request with comments
     */
    @Query("""
            SELECT DISTINCT r FROM AccessRequest r
            LEFT JOIN FETCH r.comments
            WHERE r.id = :requestId
            AND r.tenantId = :tenantId
            """)
    Optional<AccessRequest> findByIdWithComments(
            @Param("requestId") UUID requestId,
            @Param("tenantId") String tenantId
    );

    /**
     * Finds a request with audit logs eagerly loaded.
     *
     * @param requestId request identifier
     * @param tenantId  tenant identifier
     * @return optional request with audit logs
     */
    @Query("""
            SELECT DISTINCT r FROM AccessRequest r
            LEFT JOIN FETCH r.auditLogs
            WHERE r.id = :requestId
            AND r.tenantId = :tenantId
            """)
    Optional<AccessRequest> findByIdWithAuditLogs(
            @Param("requestId") UUID requestId,
            @Param("tenantId") String tenantId
    );

    /**
     * Returns count per status for requests within the given time window for a specific user.
     * Single GROUP BY query — avoids N separate count queries.
     *
     * @param tenantId tenant identifier
     * @param userId   user identifier
     * @param since    start of the analytics window (inclusive)
     * @return list of (status, count) projections
     */
    @Query("""
            SELECT r.status AS status, COUNT(r) AS count
            FROM AccessRequest r
            WHERE r.tenantId = :tenantId
            AND r.requestedByUserId = :userId
            AND r.requestedAt >= :since
            GROUP BY r.status
            """)
    List<StatusCountProjection> countByStatusGrouped(
            @Param("tenantId") String tenantId,
            @Param("userId") UUID userId,
            @Param("since") OffsetDateTime since
    );

    /**
     * Checks if a request exists for a specific user and tenant.
     *
     * @param requestId request identifier
     * @param userId    user identifier
     * @param tenantId  tenant identifier
     * @return true if request exists and belongs to user
     */
    boolean existsByIdAndRequestedByUserIdAndTenantId(
            UUID requestId,
            UUID userId,
            String tenantId
    );


    @Query("SELECT DISTINCT ar FROM AccessRequest ar " +
            "JOIN ar.items item " +
            "WHERE item.tile.id = :tileId")
    List<AccessRequest> findAllByTileId(@Param("tileId") UUID tileId);

    @Query("SELECT COUNT(DISTINCT ar) FROM AccessRequest ar " +
            "JOIN ar.items item " +
            "WHERE item.tile.id = :tileId " +
            "AND ar.status IN ('SUBMITTED', 'PENDING_APPROVAL')")
    long countBlockingRequestsByTileId(@Param("tileId") UUID tileId);
}