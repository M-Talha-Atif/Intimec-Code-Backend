package com.aidentx.access_portal_service.repository.tile;

import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.entity.tile.TileAllowedRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository interface for CRUD operations on Tile entities.
 */
@Repository
public interface TileRepository extends JpaRepository<Tile, UUID> {

    /**
     * Find all tiles for a given tenant.
     *
     * @param tenantId Stringof the tenant
     * @return List of tiles
     */
    Page<Tile> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find a tile by its ID and tenantId.
     *
     * @param id       UUID of the tile
     * @param tenantId String of the tenant
     * @return Tile if found, else null
     */
    Optional<Tile> findByIdAndTenantId(UUID id, String tenantId);


    @Query("SELECT ar FROM TileAllowedRole ar WHERE ar.tile.id = :tileId")
    Set<TileAllowedRole> findAllowedRolesByTileId(@Param("tileId") UUID tileId);


    @Query("SELECT DISTINCT t FROM Tile t " +
            "LEFT JOIN FETCH t.allowedRoles " +
            "LEFT JOIN FETCH t.accessScopeItems " +
            "LEFT JOIN FETCH t.visualFilters f " +
            "LEFT JOIN FETCH f.rules " +
            "WHERE t.id = :id")
    Optional<Tile> findByIdWithRelations(@Param("id") UUID id);

    // NEW: Single method that properly loads all relationships
    default Tile findByIdWithAllRelations(UUID id) {
        // Load base tile
        Tile tile = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tile not found: " + id));

        // Load each relationship separately to avoid Cartesian product
        findByIdWithAllowedRoles(id).ifPresent(t -> tile.setAllowedRoles(t.getAllowedRoles()));
        findByIdWithAccessScopeItems(id).ifPresent(t -> tile.setAccessScopeItems(t.getAccessScopeItems()));
        findByIdWithVisualFilters(id).ifPresent(t -> tile.setVisualFilters(t.getVisualFilters()));

        return tile;
    }


    @Query("SELECT t FROM Tile t LEFT JOIN FETCH t.accessScopeItems WHERE t.id = :id AND t.tenantId = :tenantId")
    Tile findByIdAndTenantIdWithRelations(@Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query("SELECT t FROM Tile t WHERE t.id = :id")
    Optional<Tile> findById(@Param("id") UUID id);

    @Query("SELECT t FROM Tile t " +
            "LEFT JOIN FETCH t.allowedRoles " +
            "WHERE t.id = :id")
    Optional<Tile> findByIdWithAllowedRoles(@Param("id") UUID id);

    @Query("SELECT t FROM Tile t " +
            "LEFT JOIN FETCH t.accessScopeItems " +
            "WHERE t.id = :id")
    Optional<Tile> findByIdWithAccessScopeItems(@Param("id") UUID id);

    @Query("SELECT t FROM Tile t " +
            "LEFT JOIN FETCH t.visualFilters f " +
            "LEFT JOIN FETCH f.rules " +
            "WHERE t.id = :id")
    Optional<Tile> findByIdWithVisualFilters(@Param("id") UUID id);


    /**
     * Find all active tiles for tenant (unpaged)
     */
    List<Tile> findAllByTenantIdAndTileActiveTrue(String tenantId);


    List<Tile> findAllByTenantId(String tenantId);


    // In TileRepository interface
    List<Tile> findByTenantIdAndTitle(String tenantId, String title);
}
