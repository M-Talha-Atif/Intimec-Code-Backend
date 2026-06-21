package com.aidentx.access_portal_service.service.impl;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.response.tile.TileDeletionImpactDTO;
import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import com.aidentx.access_portal_service.entity.cart.CartItem;
import com.aidentx.access_portal_service.entity.cart.CartStatus;
import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestRepository;
import com.aidentx.access_portal_service.repository.cart.CartRepository;
import com.aidentx.access_portal_service.repository.tile.TileRepository;
import com.aidentx.access_portal_service.service.TileDeletionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TileDeletionServiceImpl implements TileDeletionService {

     private final TileRepository tileRepository;
    private final CartRepository cartRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final IUnifiedLogger logger;

    private static final List<RequestStatus> BLOCKING_STATUSES = Arrays.asList(
            RequestStatus.SUBMITTED,
            RequestStatus.PENDING_APPROVAL
    );

    @Autowired
    public TileDeletionServiceImpl(
            TileRepository tileRepository,
            CartRepository cartRepository,
            AccessRequestRepository accessRequestRepository,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.tileRepository = tileRepository;
        this.cartRepository = cartRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_TILE_DELETION_SERVICE
        );
    }

    private static final List<RequestStatus> PROCESSED_STATUSES = Arrays.asList(
            RequestStatus.PROVISIONED,
            RequestStatus.PARTIALLY_PROVISIONED,
            RequestStatus.REJECTED,
            RequestStatus.WITHDRAWN
    );

    @Override
    @Transactional(readOnly = true)
    public TileDeletionImpactDTO analyzeDeletionImpact(UUID tileId) {
        logger.info()
                .message("Analyzing deletion impact for tile")
                .detail("tileId", tileId.toString())
                .objectType("tile-deletion-impact-analysis")
                .log();

        Tile tile = tileRepository.findById(tileId)
                .orElseThrow(() -> new IllegalArgumentException("Tile not found: " + tileId));

        // Fetch affected cart items
        List<CartItem> cartItems = cartRepository.findAllByTileId(tileId);
        long activeCartItems = cartItems.stream()
                .filter(c -> c.getStatus() == CartStatus.ACTIVE)
                .count();

        // Fetch affected requests
        List<AccessRequest> requests = accessRequestRepository.findAllByTileId(tileId);
        long blockingRequests = requests.stream()
                .filter(r -> BLOCKING_STATUSES.contains(r.getStatus()))
                .count();
        long processedRequests = requests.stream()
                .filter(r -> PROCESSED_STATUSES.contains(r.getStatus()))
                .count();

        // Determine if tile can be deleted
        boolean canDelete = activeCartItems == 0 && blockingRequests == 0;
        String deleteBlockReason = null;

        if (!canDelete) {
            if (activeCartItems > 0 && blockingRequests > 0) {
                deleteBlockReason = String.format(
                        "Cannot delete tile: %d active cart item(s) and %d pending/submitted request(s) exist",
                        activeCartItems, blockingRequests
                );
            } else if (activeCartItems > 0) {
                deleteBlockReason = String.format(
                        "Cannot delete tile: %d active cart item(s) exist",
                        activeCartItems
                );
            } else {
                deleteBlockReason = String.format(
                        "Cannot delete tile: %d pending/submitted request(s) exist",
                        blockingRequests
                );
            }
        }

        logger.info()
                .message("Deletion impact analysis complete")
                .detail("tileId", tileId.toString())
                .detail("canDelete", String.valueOf(canDelete))
                .detail("activeCartItems", String.valueOf(activeCartItems))
                .detail("blockingRequests", String.valueOf(blockingRequests))
                .objectType("tile-deletion-impact-result")
                .log();

        // Build DTO
        return TileDeletionImpactDTO.builder()
                .tileId(tileId)
                .tileTitle(tile.getTitle())
                .canDelete(canDelete)
                .deleteBlockReason(deleteBlockReason)
                .impactSummary(buildImpactSummary(cartItems, requests, activeCartItems, blockingRequests, processedRequests))
                .affectedCartItems(buildAffectedCartItems(cartItems))
                .affectedRequests(buildAffectedRequests(requests))
                .build();
    }

    private TileDeletionImpactDTO.ImpactSummary buildImpactSummary(
            List<CartItem> cartItems,
            List<AccessRequest> requests,
            long activeCartItems,
            long blockingRequests,
            long processedRequests
    ) {
        return TileDeletionImpactDTO.ImpactSummary.builder()
                .totalCartItems(cartItems.size())
                .activeCartItems((int) activeCartItems)
                .totalRequests(requests.size())
                .blockingRequests((int) blockingRequests)
                .processedRequests((int) processedRequests)
                .build();
    }

    private List<TileDeletionImpactDTO.AffectedCartItem> buildAffectedCartItems(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(cart -> TileDeletionImpactDTO.AffectedCartItem.builder()
                        .cartItemId(cart.getId())
                        .itemName(cart.getItemName())
                        .itemType(cart.getItemType())
                        .status(cart.getStatus().name())
                        .userId(cart.getUserId())
                        .createdAt(cart.getCreatedAt() != null
                                ? cart.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                : null)
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<TileDeletionImpactDTO.AffectedRequest> buildAffectedRequests(List<AccessRequest> requests) {
        return requests.stream()
                .map(request -> TileDeletionImpactDTO.AffectedRequest.builder()
                        .requestId(request.getId())
                        .status(request.getStatus().name())
                        .requestedByUserName(request.getRequestedByUserName())
                        .requestedByUserId(request.getRequestedByUserId())
                        .requestedAt(request.getRequestedAt() != null
                                ? request.getRequestedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                : null)
                        .itemCount(request.getItems() != null ? request.getItems().size() : 0)
                        .items(buildRequestItemSummaries(request))
                        .build()
                )
                .collect(Collectors.toList());
    }

    private List<TileDeletionImpactDTO.RequestItemSummary> buildRequestItemSummaries(AccessRequest request) {
        if (request.getItems() == null) {
            return List.of();
        }

        return request.getItems().stream()
                .map(item -> TileDeletionImpactDTO.RequestItemSummary.builder()
                        .itemId(item.getId())
                        .itemName(item.getItemName())
                        .itemType(item.getItemType())
                        .requestTarget(item.getRequestTarget().name())
                        .beneficiaryCount(item.getBeneficiaryUserIds() != null
                                ? item.getBeneficiaryUserIds().split(",").length
                                : 0)
                        .build()
                )
                .collect(Collectors.toList());
    }
}