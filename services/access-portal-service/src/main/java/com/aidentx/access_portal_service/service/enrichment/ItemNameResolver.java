package com.aidentx.access_portal_service.service.enrichment;

import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.client.BusinessRoleRestClient;
import com.aidentx.access_portal_service.client.grpc.EntitlementGrpcClient;
import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.config.constants.ItemNameResolverConstants;
import com.aidentx.iam.grpc.GetEntitlementByIdResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Resolves fresh item names from source-of-truth services at read time.
 *
 * SERVICE PURPOSE:
 * Entities like cart items and access request items store itemName at
 * write time, but names can change (role renamed, entitlement display
 * name updated). This component resolves the current name from the
 * owning service on every read, using the stored name as a fallback
 * if the source service is unreachable.
 *
 * HOW IT WORKS:
 * 1. Receives a list of NameResolvable DTOs after DB fetch and mapping
 * 2. Resolves names in parallel using CompletableFuture
 *    - INTERNAL_ROLE: calls UserRoleRestClient.getRoleById()
 *    - ENTITLEMENT: calls EntitlementGrpcClient.getEntitlement()
 * 3. Overwrites itemName in the DTO with the fresh value
 * 4. If any individual call fails, the stored name is preserved (no data loss)
 *
 * PERFORMANCE:
 * - All lookups run in parallel, so total latency = max(single call latency)
 * - Uses a dedicated daemon thread pool to avoid blocking the caller
 *
 * CONSUMERS:
 * - CartServiceImpl.getCart()
 * - AccessRequestServiceImpl.getRequestDetails()
 * - AccessRequestServiceImpl.getRequestHistory()
 *
 * @author aidentx Development Team
 * @version 1.1
 */
@Component
public class ItemNameResolver {

    private final UserRoleRestClient userRoleRestClient;
    private final BusinessRoleRestClient businessRoleRestClient;
    private final EntitlementGrpcClient entitlementGrpcClient;
    private final IUnifiedLogger logger;
    private final ExecutorService resolverExecutor;

    @Autowired
    public ItemNameResolver(
            UserRoleRestClient userRoleRestClient,
            BusinessRoleRestClient businessRoleRestClient,
            EntitlementGrpcClient entitlementGrpcClient,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.userRoleRestClient = userRoleRestClient;
        this.businessRoleRestClient = businessRoleRestClient;
        this.entitlementGrpcClient = entitlementGrpcClient;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_CART_SERVICE
        );
        this.resolverExecutor = Executors.newFixedThreadPool(
                ItemNameResolverConstants.RESOLVER_THREAD_POOL_SIZE,
                r -> {
                    Thread t = new Thread(r, ItemNameResolverConstants.RESOLVER_THREAD_NAME);
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    /**
     * Enriches any list of NameResolvable DTOs with fresh names from source services.
     * Modifies the DTOs in place. If a source service call fails for any item,
     * that item's name remains unchanged (the stored/cached name is preserved).
     *
     * @param items    list of NameResolvable DTOs to enrich
     * @param tenantId tenant ID for entitlement gRPC calls
     * @param <T>      any DTO implementing NameResolvable
     */
    public <T extends NameResolvable> void enrichNames(List<T> items, String tenantId) {
        if (items == null || items.isEmpty()) {
            return;
        }

        logger.debug()
                .message("Starting item name resolution")
                .detail("itemCount", String.valueOf(items.size()))
                .detail("tenantId", tenantId)
                .objectType(ItemNameResolverConstants.LOG_RESOLUTION_START)
                .log();

        Map<String, String> resolvedNames = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = items.stream()
                .map(item -> resolveNameAsync(item, tenantId, resolvedNames))
                .collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(ItemNameResolverConstants.RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn()
                    .message("Item name resolution did not complete within timeout, some items may use stored names")
                    .detail("timeoutSeconds", String.valueOf(ItemNameResolverConstants.RESOLUTION_TIMEOUT_SECONDS))
                    .detail("error", e.getMessage())
                    .objectType(ItemNameResolverConstants.LOG_RESOLUTION_TIMEOUT)
                    .log();
        }

        int enrichedCount = 0;
        for (NameResolvable item : items) {
            String key = buildKey(item.getItemType(), item.getItemId());
            String freshName = resolvedNames.get(key);
            if (freshName != null && !freshName.isEmpty()) {
                item.setItemName(freshName);
                enrichedCount++;
            }
        }

        logger.debug()
                .message("Item name resolution completed")
                .detail("totalItems", String.valueOf(items.size()))
                .detail("enrichedCount", String.valueOf(enrichedCount))
                .detail("fallbackCount", String.valueOf(items.size() - enrichedCount))
                .objectType(ItemNameResolverConstants.LOG_RESOLUTION_COMPLETE)
                .log();
    }

    private CompletableFuture<Void> resolveNameAsync(
            NameResolvable item,
            String tenantId,
            Map<String, String> resolvedNames
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                String freshName = resolveItemName(item.getItemType(), item.getItemId(), tenantId);
                if (freshName != null && !freshName.isEmpty()) {
                    resolvedNames.put(buildKey(item.getItemType(), item.getItemId()), freshName);
                }
            } catch (Exception e) {
                logger.warn()
                        .message("Failed to resolve fresh name for item, using stored name")
                        .detail("itemType", item.getItemType())
                        .detail("itemId", item.getItemId() != null ? item.getItemId().toString() : "null")
                        .detail("storedName", item.getItemName())
                        .detail("error", e.getMessage())
                        .objectType(ItemNameResolverConstants.LOG_RESOLUTION_ITEM_FALLBACK)
                        .log();
            }
        }, resolverExecutor);
    }

    private String resolveItemName(String itemType, UUID itemId, String tenantId) {
        if (itemType == null || itemId == null) {
            return null;
        }

        switch (itemType) {
            case ItemNameResolverConstants.ITEM_TYPE_INTERNAL_ROLE:
                return resolveRoleName(itemId);

            case ItemNameResolverConstants.ITEM_TYPE_ENTITLEMENT:
                return resolveEntitlementName(itemId, tenantId);

            case ItemNameResolverConstants.ITEM_TYPE_BUSINESS_ROLE:
                return resolveBusinessRoleName(itemId, tenantId);

            default:
                logger.debug()
                        .message("No name resolver configured for item type, using stored name")
                        .detail("itemType", itemType)
                        .detail("itemId", itemId.toString())
                        .objectType(ItemNameResolverConstants.LOG_RESOLUTION_UNSUPPORTED_TYPE)
                        .log();
                return null;
        }
    }

    private String resolveRoleName(UUID roleId) {
        Map<String, Object> roleData = userRoleRestClient.getRoleById(roleId);

        if (roleData != null && !roleData.isEmpty()) {
            Object roleName = roleData.get(ItemNameResolverConstants.ROLE_NAME_KEY);
            if (roleName != null) {
                return roleName.toString();
            }
        }

        return null;
    }

    private String resolveEntitlementName(UUID entitlementId, String tenantId) {
        GetEntitlementByIdResponse response = entitlementGrpcClient.getEntitlement(
                tenantId,
                entitlementId.toString()
        );

        if (response != null) {
            String displayName = response.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        }

        return null;
    }

    private String resolveBusinessRoleName(UUID businessRoleId, String tenantId) {
        Map<String, Object> roleData = businessRoleRestClient.getBusinessRoleById(businessRoleId, tenantId);

        if (roleData != null && !roleData.isEmpty()) {
            Object displayName = roleData.get(ItemNameResolverConstants.BUSINESS_ROLE_DISPLAY_NAME_KEY);
            if (displayName != null && !displayName.toString().isEmpty()) {
                return displayName.toString();
            }
            Object name = roleData.get(ItemNameResolverConstants.BUSINESS_ROLE_NAME_KEY);
            if (name != null && !name.toString().isEmpty()) {
                return name.toString();
            }
        }

        return null;
    }

    private String buildKey(String itemType, UUID itemId) {
        return itemType + ":" + (itemId != null ? itemId.toString() : "null");
    }
}
