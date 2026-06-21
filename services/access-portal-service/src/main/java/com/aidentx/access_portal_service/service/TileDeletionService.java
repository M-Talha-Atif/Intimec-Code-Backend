package com.aidentx.access_portal_service.service;

import com.aidentx.access_portal_service.dto.response.tile.TileDeletionImpactDTO;

import java.util.UUID;

/**
 * Service for analyzing tile deletion impact
 */
public interface TileDeletionService {

    /**
     * Analyze the impact of deleting a tile
     *
     * @param tileId the tile ID to analyze
     * @return deletion impact analysis
     */
    TileDeletionImpactDTO analyzeDeletionImpact(UUID tileId);
}