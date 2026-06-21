package com.aidentx.access_portal_service.repository.access_request;

import com.aidentx.access_portal_service.entity.access_request.AccessRequestItem;
import com.aidentx.access_portal_service.repository.access_request.projection.ItemApprovalMetrics;
import com.aidentx.access_portal_service.repository.access_request.projection.TileApprovalMetrics;
import com.aidentx.access_portal_service.repository.access_request.projection.TopRequestedItemProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AccessRequestItemRepository extends JpaRepository<AccessRequestItem, UUID> {

    /**
     * Returns the top N most requested items for a specific user within the time window,
     * ordered by request count descending.
     *
     * @param tenantId tenant identifier
     * @param userId   user identifier
     * @param since    start of the analytics window (inclusive)
     * @param limit    maximum number of items to return
     * @return list of (itemId, itemName, requestCount) projections
     */
    @Query("""
            SELECT i.itemId AS itemId, i.itemName AS itemName, COUNT(i) AS requestCount
            FROM AccessRequestItem i
            JOIN i.accessRequest r
            WHERE r.tenantId = :tenantId
            AND r.requestedByUserId = :userId
            AND i.createdAt >= :since
            GROUP BY i.itemId, i.itemName
            ORDER BY COUNT(i) DESC
            LIMIT :limit
            """)
    List<TopRequestedItemProjection> findTopRequestedItemsForUser(
            @Param("tenantId") String tenantId,
            @Param("userId") UUID userId,
            @Param("since") OffsetDateTime since,
            @Param("limit") int limit
    );


    /**
     * Get approval metrics for a specific item
     *
     * @param itemId   the item ID (role ID)
     * @param tenantId tenant identifier
     * @return object with approvalRate and totalRequests
     */
    @Query("""
            SELECT 
                COUNT(DISTINCT ari.accessRequest.id) as totalRequests,
                SUM(CASE WHEN ras.decision = 'APPROVED' THEN 1 ELSE 0 END) as approvedCount,
                SUM(CASE WHEN ras.decision = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount
            FROM AccessRequestItem ari
            LEFT JOIN ari.approvalSteps ras
            WHERE ari.itemId = :itemId
            AND ari.tenantId = :tenantId
            """)
    ItemApprovalMetrics getApprovalMetricsForItem(
            @Param("itemId") UUID itemId,
            @Param("tenantId") String tenantId
    );


    /**
     * Get aggregate approval metrics for all items in a tile
     *
     * @param tileId   the tile ID
     * @param tenantId tenant identifier
     * @return aggregate metrics for the tile
     */
    @Query("""
            SELECT 
                SUM(CASE WHEN ras.decision = 'APPROVED' THEN 1 ELSE 0 END) as approvedCount,
                SUM(CASE WHEN ras.decision = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount
            FROM AccessRequestItem ari
            LEFT JOIN ari.approvalSteps ras
            WHERE ari.tile.id = :tileId
            AND ari.tenantId = :tenantId
            """)
    TileApprovalMetrics getApprovalMetricsForTile(
            @Param("tileId") UUID tileId,
            @Param("tenantId") String tenantId
    );


}
