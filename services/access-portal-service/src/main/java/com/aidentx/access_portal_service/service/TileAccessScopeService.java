package com.aidentx.access_portal_service.service;

import com.aidentx.access_portal_service.dto.response.tile.TileAccessScopeResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TileAccessScopeService {

    TileAccessScopeResponseDTO getAccessScopeItemsForTile(UUID tileId,  UUID userId,
                                                          String tenantId, Pageable pageable);


}
