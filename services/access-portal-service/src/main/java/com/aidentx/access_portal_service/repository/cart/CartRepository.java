package com.aidentx.access_portal_service.repository.cart;

import com.aidentx.access_portal_service.entity.cart.CartItem;
import com.aidentx.access_portal_service.entity.cart.CartStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<CartItem, UUID> {

    boolean existsByUserIdAndTileIdAndItemIdAndStatus(
            UUID userId, UUID tileId, UUID itemId, CartStatus status);

    boolean existsByUserIdAndItemIdAndStatusInAndRequestType(
            UUID userId, UUID itemId, List<CartStatus> status, String requestType);

    Page<CartItem> findByUserIdAndTenantIdAndStatus(
            UUID userId, String tenantId, CartStatus status, Pageable pageable);

    List<CartItem> findAllByUserIdAndTenantIdAndStatus(
            UUID userId,
            String tenantId,
            CartStatus status
    );


    @Query("SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.tile WHERE ci.id = :id AND ci.userId = :userId AND ci.tenantId = :tenantId")
    Optional<CartItem> findByIdAndUserIdAndTenantIdWithTile(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("tenantId") String tenantId);

    @Query(value = "SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.tile WHERE ci.userId = :userId AND ci.tenantId = :tenantId AND ci.status = :status",
            countQuery = "SELECT COUNT(ci) FROM CartItem ci WHERE ci.userId = :userId AND ci.tenantId = :tenantId AND ci.status = :status")
    Page<CartItem> findByUserIdAndTenantIdAndStatusWithTile(
            @Param("userId") UUID userId,
            @Param("tenantId") String tenantId,
            @Param("status") CartStatus status,
            Pageable pageable);

    long countByUserIdAndTenantIdAndStatus(UUID userId, String tenantId, CartStatus status);

    Optional<CartItem> findByIdAndUserIdAndTenantId(UUID id, UUID userId, String tenantId);

    /**
     * Finds all cart items by tenant and status.
     *
     * @param tenantId tenant identifier
     * @param status   cart item status
     * @return list of cart items
     */
    List<CartItem> findAllByTenantIdAndStatus(String tenantId, CartStatus status);


    /**
     * Check if a role assignment exists for a specific beneficiary
     * REGARDLESS of who requested it (effective duplicate check)
     *
     * @param beneficiaryUserId the beneficiary user ID (partial match in comma-separated list)
     * @param itemId            the role/item ID
     * @param tenantId          the tenant ID
     * @param statuses          list of statuses to check
     * @return true if assignment exists
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CartItem c " +
            "WHERE c.tenantId = :tenantId " +
            "AND c.itemId = :itemId " +
            "AND c.beneficiaryUserIds LIKE %:beneficiaryUserId% " +
            "AND c.status IN :statuses")
    boolean existsByBeneficiaryUserIdContainingAndItemIdAndTenantIdAndStatusIn(
            @Param("beneficiaryUserId") String beneficiaryUserId,
            @Param("itemId") UUID itemId,
            @Param("tenantId") String tenantId,
            @Param("statuses") List<CartStatus> statuses
    );

    /**
     * Find cart item by user, tenant, item ID, and request type
     */
    Optional<CartItem> findByUserIdAndTenantIdAndItemIdAndRequestTypeAndStatus(
            UUID userId,
            String tenantId,
            UUID itemId,
            String requestType,
            CartStatus status
    );


    @Query("SELECT c FROM CartItem c WHERE c.tile.id = :tileId")
    List<CartItem> findAllByTileId(@Param("tileId") UUID tileId);

    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.tile.id = :tileId AND c.status = 'ACTIVE'")
    long countActiveCartItemsByTileId(@Param("tileId") UUID tileId);
}