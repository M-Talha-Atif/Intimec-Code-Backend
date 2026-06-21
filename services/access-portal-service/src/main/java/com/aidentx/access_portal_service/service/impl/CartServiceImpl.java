package com.aidentx.access_portal_service.service.impl;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.cart.AddToCartRequestDTO;
import com.aidentx.access_portal_service.dto.response.cart.CartItemResponseDTO;
import com.aidentx.access_portal_service.dto.response.cart.CartPageResponseDTO;
import com.aidentx.access_portal_service.entity.cart.CartItem;
import com.aidentx.access_portal_service.entity.cart.CartStatus;
import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.mapper.CartMapper;
import com.aidentx.access_portal_service.repository.cart.CartRepository;
import com.aidentx.access_portal_service.repository.tile.TileRepository;
import com.aidentx.access_portal_service.service.CartService;
import com.aidentx.access_portal_service.service.enrichment.ItemNameResolver;
import com.aidentx.access_portal_service.validation.cart.CartItemValidator;
import com.aidentx.access_portal_service.validation.cart.CartPaginationValidator;
import com.aidentx.access_portal_service.validation.cart.CartRequestValidator;
import com.aidentx.access_portal_service.validation.cart.CartTileValidator;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final TileRepository tileRepository;
    private final CartRequestValidator cartRequestValidator;
    private final CartTileValidator cartTileValidator;
    private final CartItemValidator cartItemValidator;
    private final CartPaginationValidator cartPaginationValidator;
    private final IUnifiedLogger logger;
    private final ItemNameResolver itemNameResolver;

    @Autowired
    public CartServiceImpl(
            CartRepository cartRepository,
            CartMapper cartMapper,
            TileRepository tileRepository,
            CartRequestValidator cartRequestValidator,
            CartTileValidator cartTileValidator,
            CartItemValidator cartItemValidator,
            CartPaginationValidator cartPaginationValidator,
            ItemNameResolver itemNameResolver,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.cartRepository = cartRepository;
        this.cartMapper = cartMapper;
        this.tileRepository = tileRepository;
        this.cartRequestValidator = cartRequestValidator;
        this.cartTileValidator = cartTileValidator;
        this.cartItemValidator = cartItemValidator;
        this.cartPaginationValidator = cartPaginationValidator;
        this.itemNameResolver = itemNameResolver;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_CART_SERVICE
        );
    }

    @Override
    @Transactional
    public CartItemResponseDTO addToCart(String tenantId, UUID userId, AddToCartRequestDTO request) {

        logger.info()
                .message("Adding item to cart")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("tileId", request.getTileId().toString())
                .detail("itemId", request.getItemId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_ADD_START)
                .log();

        // Validate the request
        cartRequestValidator.validateAddToCartRequest(request, tenantId, userId);

        // Fetch tile with relationships
        Tile tile = tileRepository.findByIdAndTenantIdWithRelations(request.getTileId(), tenantId);
        if (tile == null) {
            logger.warn()
                    .message("Tile not found for cart add")
                    .detail("tileId", request.getTileId().toString())
                    .detail("tenantId", tenantId)
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_NOT_FOUND)
                    .log();

            throw new IllegalArgumentException("Tile does not exist for tenant");
        }

        // Validate tile for cart operations
        cartTileValidator.validateTileForCart(tile);
        // Validate item exists in tile
        cartTileValidator.validateItemAllowedInTile(tile, request.getItemId(), request.getItemType());
        cartTileValidator.validateRequestType(tile, request.getRequestType(),
                request.getBeneficiaryUserIds(), userId);

        //  Check if item already exists in cart and those are active (for OTHERS requests)
        if (request.getRequestType().equals("OTHERS") && request.getBeneficiaryUserIds() != null && !request.getBeneficiaryUserIds().isEmpty()) {
            Optional<CartItem> existingCartItem = cartRepository.findByUserIdAndTenantIdAndItemIdAndRequestTypeAndStatus(
                    userId,
                    tenantId,
                    request.getItemId(),
                    "OTHERS",
                    CartStatus.ACTIVE
            );

            if (existingCartItem.isPresent()) {
                // MERGE: Add new beneficiaries to existing cart item
                CartItem existingItem = existingCartItem.get();

                // Parse existing beneficiaries
                Set<String> existingBeneficiaryIds = new HashSet<>();
                Set<String> existingBeneficiaryNames = new HashSet<>();

                if (existingItem.getBeneficiaryUserIds() != null && !existingItem.getBeneficiaryUserIds().isEmpty()) {
                    existingBeneficiaryIds.addAll(Arrays.asList(existingItem.getBeneficiaryUserIds().split(",")));
                }

                if (existingItem.getBeneficiaryNames() != null && !existingItem.getBeneficiaryNames().isEmpty()) {
                    existingBeneficiaryNames.addAll(Arrays.asList(existingItem.getBeneficiaryNames().split(",")));
                }

                // Add new beneficiaries, by converting UUIDs to strings
                List<String> newBeneficiaryIds = request.getBeneficiaryUserIds().stream()
                        .map(UUID::toString)
                        .toList(); // toList() returns immutable list, introduced in Java 16

                existingBeneficiaryIds.addAll(newBeneficiaryIds);

                if (request.getBeneficiaryNames() != null && !request.getBeneficiaryNames().isEmpty()) {
                    existingBeneficiaryNames.addAll(request.getBeneficiaryNames());
                }

                // Update the existing cart item
                existingItem.setBeneficiaryUserIds(String.join(",", existingBeneficiaryIds));
                existingItem.setBeneficiaryNames(String.join(",", existingBeneficiaryNames));
                existingItem.setUpdatedAt(OffsetDateTime.now());

                CartItem updatedItem = cartRepository.save(existingItem);

                logger.info()
                        .message("Beneficiaries merged into existing cart item")
                        .detail("cartItemId", updatedItem.getId().toString())
                        .detail("totalBeneficiaries", String.valueOf(existingBeneficiaryIds.size()))
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_MERGE_SUCCESS)
                        .log();

                CartItemResponseDTO response = cartMapper.toCartItemResponse(updatedItem);
                response.setMerged(true); // Set merged flag, added the beneficiaries in same item (role, app etc)
                return response;

            }
        }

        // If no existing item, create new cart item
        CartItem cartItem = createCartItem(tile, userId, request, tenantId);
        cartItem = cartRepository.save(cartItem);

        logger.info()
                .message("Item added to cart successfully")
                .detail("cartItemId", cartItem.getId().toString())
                .detail("userId", userId.toString())
                .detail("tileId", tile.getId().toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_ADD_SUCCESS)
                .log();

        return cartMapper.toCartItemResponse(cartItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CartPageResponseDTO getCart(String tenantId, UUID userId, int page, int size,
                                       String sortBy, String sortDirection) {
        logger.info()
                .message("Fetching cart for user")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("page", String.valueOf(page))
                .detail("size", String.valueOf(size))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_FETCH_START)
                .log();


        cartPaginationValidator.validatePagination(page, size);
        cartPaginationValidator.validateSortField(sortBy);
        cartPaginationValidator.validateSortDirection(sortDirection);

        Sort.Direction direction = sortDirection.equalsIgnoreCase(AccessPortalConstants.DEFAULT_SORT_DIRECTION)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<CartItem> cartPage = cartRepository.findByUserIdAndTenantIdAndStatusWithTile(
                userId,
                tenantId,
                CartStatus.ACTIVE,
                pageable
        );

        logger.info()
                .message("Cart fetched successfully")
                .detail("userId", userId.toString())
                .detail("totalItems", String.valueOf(cartPage.getTotalElements()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_FETCH_SUCCESS)
                .log();

        CartPageResponseDTO response = cartMapper.toCartPageResponse(cartPage);


        // Enrich cart item names with fresh values from source services.
        // If a source service is unreachable, the stored name is preserved as fallback.
        if (response.getItems() != null && !response.getItems().isEmpty()) {
            itemNameResolver.enrichNames(response.getItems(), tenantId);
        }


        return response;
    }

    @Override
    @Transactional
    public void removeFromCart(String tenantId, UUID userId, UUID cartItemId) {

        logger.info()
                .message("Removing item from cart")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("cartItemId", cartItemId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_REMOVE_START)
                .log();

        CartItem cartItem = cartRepository.findByIdAndUserIdAndTenantIdWithTile(cartItemId, userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        // Validate cart item for removal
        cartItemValidator.validateCartItem(cartItem);
        cartItemValidator.validateCartItemOwnership(cartItem, userId);
        cartItemValidator.validateCartItemTenant(cartItem, tenantId);

        cartRepository.delete(cartItem);

        logger.info()
                .message("Item removed from cart successfully")
                .detail("cartItemId", cartItemId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_REMOVE_SUCCESS)
                .log();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCartItemCount(String tenantId, UUID userId) {

        long count = cartRepository.countByUserIdAndTenantIdAndStatus(
                userId,
                tenantId,
                CartStatus.ACTIVE
        );

        logger.debug()
                .message("Cart item count fetched")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("count", String.valueOf(count))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_COUNT)
                .log();

        return count;
    }

    /**
     * Creates a CartItem entity with proper relationships
     */
    private CartItem createCartItem(Tile tile, UUID userId, AddToCartRequestDTO request, String tenantId) {
        CartItem cartItem = new CartItem();

        cartItem.setTile(tile); // Set bidirectional relationship
        cartItem.setUserId(userId);
        cartItem.setItemId(request.getItemId());
        cartItem.setItemType(request.getItemType());
        cartItem.setItemName(request.getItemName());
        cartItem.setItemDescription(request.getItemDescription());
        cartItem.setRequestType(request.getRequestType());

        if (request.getBeneficiaryUserIds() != null && !request.getBeneficiaryUserIds().isEmpty()) {
            String beneficiaryUserIds = request.getBeneficiaryUserIds().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            cartItem.setBeneficiaryUserIds(beneficiaryUserIds);

            if (request.getBeneficiaryNames() != null && !request.getBeneficiaryNames().isEmpty()) {
                String beneficiaryNames = String.join(",", request.getBeneficiaryNames());
                cartItem.setBeneficiaryNames(beneficiaryNames);
            }
        }
        cartItem.setStatus(CartStatus.ACTIVE);
        cartItem.setTenantId(tenantId);
        cartItem.setCreatedAt(OffsetDateTime.now());
        cartItem.setUpdatedAt(OffsetDateTime.now());

        // Validate the created cart item
        cartItemValidator.validateBeneficiaryInfo(cartItem);

        return cartItem;
    }


}