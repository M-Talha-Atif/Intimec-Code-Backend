package com.aidentx.access_portal_service.service.impl;

import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.client.BusinessRoleRestClient;
import com.aidentx.access_portal_service.client.grpc.EntitlementGrpcClient;
import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.response.tile.TileAccessScopeItemResponseDTO;
import com.aidentx.access_portal_service.dto.response.tile.TileAccessScopeResponseDTO;
import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.entity.tile.TileAccessScopeItem;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestItemRepository;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestRepository;
import com.aidentx.access_portal_service.repository.access_request.projection.ItemApprovalMetrics;
import com.aidentx.access_portal_service.repository.tile.TileAccessScopeItemRepository;
import com.aidentx.access_portal_service.repository.tile.TileRepository;
import com.aidentx.access_portal_service.service.TileAccessScopeService;
import com.aidentx.iam.grpc.GetEntitlementByIdResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TileAccessScopeServiceImpl implements TileAccessScopeService {

    private final TileAccessScopeItemRepository accessScopeItemRepository;
    private final TileRepository tileRepository;
    private final UserRoleRestClient userRoleRestClient;
    private final BusinessRoleRestClient businessRoleRestClient;
    private final EntitlementGrpcClient entitlementGrpcClient;
    private final IUnifiedLogger logger;
    private final AccessRequestItemRepository accessRequestItemRepository;

    @Autowired
    public TileAccessScopeServiceImpl(
            TileAccessScopeItemRepository accessScopeItemRepository,
            TileRepository tileRepository,
            AccessRequestItemRepository accessRequestItemRepository,
            UserRoleRestClient userRoleRestClient,
            BusinessRoleRestClient businessRoleRestClient,
            EntitlementGrpcClient entitlementGrpcClient,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.accessScopeItemRepository = accessScopeItemRepository;
        this.tileRepository = tileRepository;
        this.accessRequestItemRepository = accessRequestItemRepository;
        this.userRoleRestClient = userRoleRestClient;
        this.businessRoleRestClient = businessRoleRestClient;
        this.entitlementGrpcClient = entitlementGrpcClient;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_TILE_SERVICE
        );
    }

    @Override
    public TileAccessScopeResponseDTO getAccessScopeItemsForTile(UUID tileId, UUID userId,
                                                                 String tenantId, Pageable pageable) {
        logger.info()
                .message("Fetching access scope items for tile")
                .detail("tileId", tileId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_ACCESS_SCOPE_FETCH_START)
                .log();

        // Fetch tile entity
        Tile tile = tileRepository.findById(tileId)
                .orElseThrow(() -> new RuntimeException("Tile not found: " + tileId));

        // Fetch user's active roles (already assigned)
        Set<UUID> activeRoleIds =
                userRoleRestClient.getActiveRoleIds(userId, tenantId);

        logger.info()
                .message("Active roles fetched for user")
                .detail("userId", userId.toString())
                .detail("tenantId", tenantId)
                .detail("activeRoleIds", activeRoleIds.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_FETCH_SUCCESS)
                .log();

        // Fetch access scope items
        Page<TileAccessScopeItem> itemPage = accessScopeItemRepository.findByTileId(tileId, pageable);

        logger.info()
                .message("Fetched access scope items for tile")
                .detail("tileId", tileId.toString())
                .detail("itemCount", String.valueOf(itemPage.getTotalElements()))
                .detail("pageCount", String.valueOf(itemPage.getTotalPages()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_TILE_ACCESS_SCOPE_FETCH_SUCCESS)
                .log();

        // Get allowRequestForOthers from tile (applies to all items)
        Boolean allowRequestForOthers = tile.isAllowRequestForOthers();

        // Fetch full details for each item
        List<TileAccessScopeItemResponseDTO> responseItems = itemPage.getContent().stream()
                .map(item -> fetchItemDetails(item, allowRequestForOthers, tenantId))
                .collect(Collectors.toList());

        return TileAccessScopeResponseDTO.builder()
                .tileId(tileId)
                .tileTitle(tile.getTitle())
                .tileCategory(tile.getCategory())
                .allowRequestForOthers(allowRequestForOthers)
                .items(responseItems)
                .page(itemPage.getNumber())
                .size(itemPage.getSize())
                .totalElements(itemPage.getTotalElements())
                .totalPages(itemPage.getTotalPages())
                .last(itemPage.isLast())
                .build();
    }

    /**
     * Fetch full details for an access scope item.
     * Routes to the correct resolver based on itemType:
     *   INTERNAL_ROLE -> UserRoleRestClient (REST, application-permissions-service)
     *   BUSINESS_ROLE -> BusinessRoleRestClient (REST, identity-service access_roles_management)
     *   ENTITLEMENT   -> EntitlementGrpcClient (gRPC, identity-service)
     */
    private TileAccessScopeItemResponseDTO fetchItemDetails(
            TileAccessScopeItem item,
            Boolean allowRequestForOthers,
            String tenantId
    ) {
        String itemName = "Unknown";
        String itemDescription = "";
        String itemIcon = "Shield";
        String riskLevel = "low";
        String itemOwner = "System";
        String department = "IT";
        Double approvalRate = 0.0;
        Integer totalRequests = 0;

        try {
            if (AccessPortalConstants.ITEM_TYPE_INTERNAL_ROLE.equals(item.getItemType())) {
                logger.debug()
                        .message("Fetching role details")
                        .detail("roleId", item.getItemCode().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_FETCH)
                        .log();

                Map<String, Object> roleData = userRoleRestClient.getRoleById(item.getItemCode());

                if (roleData != null && !roleData.isEmpty()) {
                    itemName = (String) roleData.getOrDefault("roleName", "Unknown");
                    itemDescription = (String) roleData.getOrDefault("description", "");
                    itemIcon = "Shield";
                    riskLevel = ((String) roleData.getOrDefault("riskLevel", "Low")).toLowerCase();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ownersData =
                            (List<Map<String, Object>>) roleData.get("ownersData");

                    if (ownersData != null && !ownersData.isEmpty()) {
                        Map<String, Object> firstOwner = ownersData.get(0);
                        itemOwner = (String) firstOwner.getOrDefault("displayName", "System");
                    }

                    logger.debug()
                            .message("Successfully fetched role details")
                            .detail("roleId", item.getItemCode().toString())
                            .detail("roleName", itemName)
                            .detail("riskLevel", riskLevel)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ROLE_FETCH_SUCCESS)
                            .log();
                }

                // Fetch approval metrics for this role
                approvalRate = fetchApprovalRate(item, approvalRate);
                totalRequests = fetchTotalRequests(item, totalRequests);

            } else if (AccessPortalConstants.ITEM_TYPE_ENTITLEMENT.equals(item.getItemType())) {
                logger.debug()
                        .message("Fetching entitlement details via gRPC")
                        .detail("entitlementId", item.getItemCode().toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ENTITLEMENT_FETCH)
                        .log();

                GetEntitlementByIdResponse entitlement = entitlementGrpcClient.getEntitlement(
                        tenantId,
                        item.getItemCode().toString()
                );

                itemName = entitlement.getDisplayName();
                itemDescription = entitlement.getDescription();
                itemIcon = "Key";
                riskLevel = entitlement.getRiskLevel().toLowerCase();
                department = "";

                // Resolve owner: use ownerId if present, otherwise "Unowned"
                String ownerId = entitlement.getOwnerId();
                itemOwner = (ownerId != null && !ownerId.isEmpty()) ? ownerId : "Unowned";

                logger.debug()
                        .message("Successfully fetched entitlement details")
                        .detail("entitlementId", item.getItemCode().toString())
                        .detail("displayName", itemName)
                        .detail("riskLevel", riskLevel)
                        .detail("kind", entitlement.getKind())
                        .detail("lifecycle", entitlement.getLifecycle())
                        .detail("assignedUsersCount", String.valueOf(entitlement.getAssignedUsersCount()))
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ENTITLEMENT_FETCH_SUCCESS)
                        .log();

                // Fetch approval metrics for this entitlement
                approvalRate = fetchApprovalRate(item, approvalRate);
                totalRequests = fetchTotalRequests(item, totalRequests);

            } else if (AccessPortalConstants.ITEM_TYPE_BUSINESS_ROLE.equals(item.getItemType())) {
                logger.debug()
                        .message("Fetching business role details via REST")
                        .detail("businessRoleId", item.getItemCode().toString())
                        .detail("tenantId", tenantId)
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH)
                        .log();

                Map<String, Object> roleData =
                        businessRoleRestClient.getBusinessRoleById(item.getItemCode(), tenantId);

                if (roleData != null && !roleData.isEmpty()) {
                    Object displayName = roleData.get("displayName");
                    Object name = roleData.get("name");
                    if (displayName != null && !displayName.toString().isEmpty()) {
                        itemName = displayName.toString();
                    } else if (name != null) {
                        itemName = name.toString();
                    }

                    Object desc = roleData.get("description");
                    itemDescription = desc != null ? desc.toString() : "";

                    itemIcon = "Briefcase";

                    Object risk = roleData.get("riskLevel");
                    if (risk != null && !risk.toString().isEmpty()) {
                        riskLevel = risk.toString().toLowerCase();
                    }

                    Object ownerId = roleData.get("ownerId");
                    itemOwner = (ownerId != null && !ownerId.toString().isEmpty())
                            ? ownerId.toString() : "Unowned";

                    Object businessUnit = roleData.get("businessUnit");
                    department = businessUnit != null ? businessUnit.toString() : "";

                    logger.debug()
                            .message("Successfully fetched business role details")
                            .detail("businessRoleId", item.getItemCode().toString())
                            .detail("displayName", itemName)
                            .detail("riskLevel", riskLevel)
                            .detail("businessUnit", department)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_BUSINESS_ROLE_FETCH_SUCCESS)
                            .log();
                }

                // Fetch approval metrics for this business role
                approvalRate = fetchApprovalRate(item, approvalRate);
                totalRequests = fetchTotalRequests(item, totalRequests);

            } else {
                logger.warn()
                        .message("Unsupported item type")
                        .detail("itemType", item.getItemType())
                        .detail("itemCode", item.getItemCode().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_UNSUPPORTED_ITEM_TYPE)
                        .log();
            }

        } catch (Exception e) {
            logger.error()
                    .message("Failed to fetch item details")
                    .detail("itemType", item.getItemType())
                    .detail("itemCode", item.getItemCode().toString())
                    .detail("error", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ITEM_FETCH_ERROR)
                    .log();
        }

        return TileAccessScopeItemResponseDTO.builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .itemCode(item.getItemCode())
                .itemName(itemName)
                .itemDescription(itemDescription)
                .itemIcon(itemIcon)
                .riskLevel(riskLevel)
                .itemOwner(itemOwner)
                .department(department)
                .allowRequestForOthers(allowRequestForOthers)
                .approvalRate(approvalRate)
                .totalRequests(totalRequests)
                .build();
    }

    /**
     * Fetches approval rate for a given access scope item from historical request data.
     * Calculates as: (approved count / (approved + rejected)) * 100.
     * Returns the fallback value if no metrics are available or if the query fails.
     */
    private Double fetchApprovalRate(TileAccessScopeItem item, Double fallback) {
        try {
            ItemApprovalMetrics metrics = accessRequestItemRepository.getApprovalMetricsForItem(
                    item.getItemCode(),
                    item.getTenantId()
            );

            if (metrics == null) {
                return fallback;
            }

            long approvedCount = metrics.getApprovedCount() != null ? metrics.getApprovedCount() : 0L;
            long rejectedCount = metrics.getRejectedCount() != null ? metrics.getRejectedCount() : 0L;
            long totalDecisions = approvedCount + rejectedCount;

            if (totalDecisions > 0) {
                double rate = (double) approvedCount / totalDecisions * 100.0;
                return Math.round(rate * 100.0) / 100.0;
            }

            return fallback;

        } catch (Exception e) {
            logger.warn()
                    .message("Failed to fetch approval rate for item")
                    .detail("itemId", item.getItemCode().toString())
                    .detail("error", e.getMessage())
                    .log();
            return fallback;
        }
    }

    /**
     * Fetches total request count for a given access scope item from historical request data.
     * Returns the fallback value if no metrics are available or if the query fails.
     */
    private Integer fetchTotalRequests(TileAccessScopeItem item, Integer fallback) {
        try {
            ItemApprovalMetrics metrics = accessRequestItemRepository.getApprovalMetricsForItem(
                    item.getItemCode(),
                    item.getTenantId()
            );

            if (metrics != null && metrics.getTotalRequests() != null) {
                return metrics.getTotalRequests().intValue();
            }

            return fallback;

        } catch (Exception e) {
            logger.warn()
                    .message("Failed to fetch total requests for item")
                    .detail("itemId", item.getItemCode().toString())
                    .detail("error", e.getMessage())
                    .log();
            return fallback;
        }
    }
}