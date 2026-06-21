package com.aidentx.access_portal_service.repository.tile;

import com.aidentx.access_portal_service.entity.tile.TileAllowedRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing allowed roles for a tile.
 */
@Repository
public interface TileAllowedRoleRepository extends JpaRepository<TileAllowedRole, UUID> {

    /**
     * Get all allowed roles for a specific tile.
     *
     * @param tileId UUID of the tile
     * @return List of allowed roles
     */
    List<TileAllowedRole> findByTileId(UUID tileId);

    /**
     * Delete all allowed roles for a specific tile.
     *
     * @param tileId UUID of the tile
     */
    void deleteByTileId(UUID tileId);
}
