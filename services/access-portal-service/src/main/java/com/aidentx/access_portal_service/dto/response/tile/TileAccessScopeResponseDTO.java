package com.aidentx.access_portal_service.dto.response.tile;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TileAccessScopeResponseDTO {

    private UUID tileId;
    private String tileTitle;
    private String tileCategory;
    private Boolean allowRequestForOthers;
    private List<TileAccessScopeItemResponseDTO> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

}
