package com.aidentx.access_portal_service.dto.request.tile;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for visual filter in TileRequestDTO
 */
@Getter
@Setter
public class TileVisualFilterDTO {
    private String filterName;
    private List<TileVisualFilterRuleDTO> rules;
}
