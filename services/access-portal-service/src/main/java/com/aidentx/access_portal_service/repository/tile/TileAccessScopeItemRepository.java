package com.aidentx.access_portal_service.repository.tile;

import com.aidentx.access_portal_service.entity.tile.TileAccessScopeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TileAccessScopeItemRepository
        extends JpaRepository<TileAccessScopeItem, UUID> {

    List<TileAccessScopeItem> findByTileId(UUID tileId);

    Page<TileAccessScopeItem> findByTileId(UUID tileId, Pageable pageable);
}
