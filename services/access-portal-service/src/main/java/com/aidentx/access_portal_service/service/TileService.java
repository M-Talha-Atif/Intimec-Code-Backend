package com.aidentx.access_portal_service.service;

import com.aidentx.access_portal_service.dto.request.tile.TileRequestDTO;
import com.aidentx.access_portal_service.dto.response.tile.TileResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing Tiles using DTOs.
 */
public interface TileService {

    /**
     * Create a new tile.
     *
     * @param tenantId tenant UUID
     * @param createdBy user UUID who creates the tile
     * @param requestDTO tile request DTO
     * @return created tile response DTO
     */
    TileResponseDTO createTile(String tenantId, UUID createdBy, TileRequestDTO requestDTO);


    /**
     * Update an existing tile.
     *
     * @param tileId tile UUID
     * @param modifiedBy user UUID who modifies the tile
     * @param requestDTO updated tile request DTO
     * @return updated tile response DTO
     */
    TileResponseDTO updateTile(String tenantId, UUID tileId, UUID modifiedBy, TileRequestDTO requestDTO);


    /**
     * Delete a tile by its UUID.
     *
     * @param tileId tile UUID
     */
    void deleteTile(UUID tileId);

    /**
     * Fetch a tile by its UUID.
     *
     * @param tileId tile UUID
     * @return tile response DTO
     */
    TileResponseDTO getTileById(UUID tileId, UUID userId, String tenantId);

    /**
     * Fetch all tiles for a tenant.
     *
     * @param tenantId tenant UUID
     * @return list of tile response DTOs
     */
    Page<TileResponseDTO> getAllTilesForTenant(String tenantId, UUID userId, Pageable pageable);

}
