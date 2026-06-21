package com.aidentx.access_portal_service.repository.tile;

import com.aidentx.access_portal_service.entity.tile.TileVisualFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing visual filter groups for a tile.
 */
@Repository
public interface TileVisualFilterRepository extends JpaRepository<TileVisualFilter, UUID> {

    List<TileVisualFilter> findByTileId(UUID tileId);

    void deleteByTileId(UUID tileId);
}
