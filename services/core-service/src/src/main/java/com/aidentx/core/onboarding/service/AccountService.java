package com.aidentx.core.onboarding.service;

import com.aidentx.dal.protos.shadow.*;
import com.aidentx.dal.protos.identity.IdentityQueryServiceGrpc;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service responsible for aggregating shadow data from identity-service
 * and transforming it into the Account format expected by the frontend.
 *
 * <p>In the IGA model, "accounts" displayed on the frontend are actually shadow
 * objects — persistent representations of resource objects discovered during
 * connector import tasks. This service bridges the backend shadow domain model
 * with the frontend account display model.</p>
 */
@Service
public class AccountService {

    private final ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncStub;
    private final IdentityQueryServiceGrpc.IdentityQueryServiceBlockingStub identityQueryStub;
    private final IUnifiedLogger logger;

    public AccountService(
            ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncStub,
            IdentityQueryServiceGrpc.IdentityQueryServiceBlockingStub identityQueryStub,
            UnifiedLoggerFactory loggerFactory) {
        this.shadowSyncStub = shadowSyncStub;
        this.identityQueryStub = identityQueryStub;
        this.logger = loggerFactory.getLogger(AccountService.class);
    }

    /**
     * Retrieves accounts (shadows) for a specific application, transformed
     * into the AccountWithUserAndEntitlements format the frontend expects.
     *
     * @param tenantId  The tenant ID
     * @param resourceId The application/resource ID
     * @param page      Page number (0-based)
     * @param size      Page size
     * @param situation Optional situation filter
     * @param search    Optional search term
     * @return Map containing "data" (List of accounts) and "pagination" metadata
     */
    public Map<String, Object> getAccountsForApplication(
            String tenantId, String resourceId, int page, int size,
            String situation, String search) {

        // Build gRPC request
        GetShadowsByResourceRequest.Builder requestBuilder = GetShadowsByResourceRequest.newBuilder()
                .setTenantId(tenantId)
                .setResourceId(resourceId)
                .setObjectClass("__ACCOUNT__")
                .setPageSize(size)
                .setPageOffset(page * size);

        if (situation != null && !situation.isEmpty()) {
            requestBuilder.setSituationFilter(situation);
        }
        if (search != null && !search.isEmpty()) {
            requestBuilder.setSearchFilter(search);
        }

        // Call identity-service via gRPC
        GetShadowsByResourceResponse grpcResponse = shadowSyncStub.getShadowsByResource(requestBuilder.build());

        // Transform shadows to account format
        List<Map<String, Object>> accounts = new ArrayList<>();
        for (ShadowDTO shadow : grpcResponse.getShadowsList()) {
            accounts.add(toAccountDTO(shadow, resourceId));
        }

        // Build pagination
        long totalElements = grpcResponse.getTotalCount();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", totalPages);

        // Build stats by making lightweight gRPC calls for counts
        Map<String, Object> stats = buildAccountStats(tenantId, resourceId, totalElements);

        // Build response in the format useAccounts.ts expects: { data: [...], pagination: {...}, stats: {...} }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", accounts);
        response.put("pagination", pagination);
        response.put("stats", stats);

        logger.info().message("Fetched accounts for application")
                .detail("resourceId", resourceId)
                .detail("accountCount", accounts.size())
                .detail("totalElements", totalElements)
                .log();

        return response;
    }

    /**
     * Builds summary stats for the accounts summary bar by making filtered gRPC calls.
     * Uses page_size=1 to minimize data transfer while getting accurate total_count.
     */
    private Map<String, Object> buildAccountStats(String tenantId, String resourceId, long totalElements) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", totalElements);

        try {
            // Count disabled (DELETED situation)
            GetShadowsByResourceResponse deletedResponse = shadowSyncStub.getShadowsByResource(
                    GetShadowsByResourceRequest.newBuilder()
                            .setTenantId(tenantId)
                            .setResourceId(resourceId)
                            .setObjectClass("__ACCOUNT__")
                            .setSituationFilter("DELETED")
                            .setPageSize(1)
                            .setPageOffset(0)
                            .build());
            long disabledCount = deletedResponse.getTotalCount();

            // Count linked (non-orphan)
            GetShadowsByResourceResponse linkedResponse = shadowSyncStub.getShadowsByResource(
                    GetShadowsByResourceRequest.newBuilder()
                            .setTenantId(tenantId)
                            .setResourceId(resourceId)
                            .setObjectClass("__ACCOUNT__")
                            .setLinkedOnly(true)
                            .setPageSize(1)
                            .setPageOffset(0)
                            .build());
            long linkedCount = linkedResponse.getTotalCount();

            stats.put("activeCount", totalElements - disabledCount);
            stats.put("disabledCount", disabledCount);
            stats.put("orphanCount", totalElements - linkedCount);
            stats.put("privilegedCount", 0L);
        } catch (Exception e) {
            logger.warn().message("Failed to compute account stats, using defaults")
                    .detail("error", e.getMessage()).log();
            stats.put("activeCount", 0L);
            stats.put("disabledCount", 0L);
            stats.put("orphanCount", 0L);
            stats.put("privilegedCount", 0L);
        }

        return stats;
    }

    /**
     * Returns available filter options for the accounts page.
     */
    public Map<String, Object> getAccountFilters(String tenantId, String resourceId) {
        // Return the synchronization situations as filter options
        List<Map<String, String>> situationFilters = List.of(
                Map.of("value", "LINKED", "label", "Linked"),
                Map.of("value", "UNMATCHED", "label", "Unmatched"),
                Map.of("value", "UNLINKED", "label", "Unlinked"),
                Map.of("value", "DISPUTED", "label", "Disputed"),
                Map.of("value", "DELETED", "label", "Deleted")
        );

        List<Map<String, String>> statusFilters = List.of(
                Map.of("value", "Active", "label", "Active"),
                Map.of("value", "Disabled", "label", "Disabled")
        );

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("situations", situationFilters);
        filters.put("statuses", statusFilters);
        return filters;
    }

    /**
     * Transforms a ShadowDTO from gRPC into the Account format the frontend expects.
     *
     * <p>Frontend Account type:
     * <pre>
     * {
     *   id: string,
     *   name: string,
     *   applicationId: string,
     *   createdAt: number,
     *   updatedAt: number,
     *   status: "Active" | "Disabled",
     *   lastSync: number,
     *   userId: string | null,
     *   applicationAttributes: { [key: string]: any },
     *   user: UserMeta | null,
     *   entitlements: EntitlementMeta[]
     * }
     * </pre>
     */
    private Map<String, Object> toAccountDTO(ShadowDTO shadow, String resourceId) {
        Map<String, Object> account = new LinkedHashMap<>();

        account.put("id", shadow.getId());
        account.put("applicationId", resourceId);

        // The "name" is derived from the resource UID or a display attribute
        Map<String, Object> attributes = structToMap(shadow.getAttributes());
        String name = deriveAccountName(shadow.getResourceUid(), attributes);
        account.put("name", name);

        // Timestamps (shadow stores as epoch millis)
        account.put("createdAt", shadow.getCreatedAt());
        account.put("updatedAt", shadow.getUpdatedAt());
        account.put("lastSync", shadow.getLastSynchronizedAt());

        // Status: derive from synchronization situation
        String status = shadow.getSituation() == SynchronizationSituation.DELETED ? "Disabled" : "Active";
        account.put("status", status);

        // Linked identity ID
        String identityId = shadow.getLinkedIdentityId().isEmpty() ? null : shadow.getLinkedIdentityId();
        account.put("userId", identityId);

        // Application-specific attributes from the connector
        account.put("applicationAttributes", attributes);

        // Sync situation metadata (extra field for IGA-specific display)
        account.put("synchronizationSituation", shadow.getSituation().name());

        // User metadata (will be populated when identity is linked)
        account.put("user", identityId != null ? buildUserMeta(identityId, attributes) : null);

        // Entitlements (empty for now - to be populated when entitlement model is implemented)
        account.put("entitlements", Collections.emptyList());

        return account;
    }

    /**
     * Derives a human-readable account name from shadow data.
     * Tries common account name attributes before falling back to resourceUid.
     */
    private String deriveAccountName(String resourceUid, Map<String, Object> attributes) {
        // Try common name attributes in order of preference
        String[] nameFields = {"name", "displayName", "display_name", "cn",
                "sAMAccountName", "uid", "username", "accountName", "account_name",
                "firstName", "first_name", "email"};

        for (String field : nameFields) {
            Object val = attributes.get(field);
            if (val != null && !val.toString().isEmpty()) {
                return val.toString();
            }
        }

        // Fall back to resourceUid
        return resourceUid;
    }

    /**
     * Builds a minimal UserMeta object from available data.
     * In future, this will fetch full identity details from identity-service.
     */
    private Map<String, Object> buildUserMeta(String identityId, Map<String, Object> attributes) {
        Map<String, Object> userMeta = new LinkedHashMap<>();
        userMeta.put("id", identityId);

        // Try to derive a display name from connector attributes
        String displayName = null;
        if (attributes.containsKey("firstName") && attributes.containsKey("lastName")) {
            displayName = attributes.get("firstName") + " " + attributes.get("lastName");
        } else if (attributes.containsKey("first_name") && attributes.containsKey("last_name")) {
            displayName = attributes.get("first_name") + " " + attributes.get("last_name");
        } else if (attributes.containsKey("displayName")) {
            displayName = attributes.get("displayName").toString();
        } else if (attributes.containsKey("name")) {
            displayName = attributes.get("name").toString();
        }

        userMeta.put("name", displayName != null ? displayName : "Identity " + identityId.substring(0, 8));
        userMeta.put("riskLevel", "Low");
        userMeta.put("department", attributes.getOrDefault("department", null));
        userMeta.put("manager", attributes.getOrDefault("manager", null));
        userMeta.put("status", "Active");
        userMeta.put("type", "employee");
        userMeta.put("isPrivileged", false);

        return userMeta;
    }

    /**
     * Converts a Protobuf Struct to a Map.
     */
    private Map<String, Object> structToMap(Struct struct) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (struct == null) {
            return map;
        }
        for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
            map.put(entry.getKey(), valueToObject(entry.getValue()));
        }
        return map;
    }

    private Object valueToObject(Value value) {
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case NUMBER_VALUE -> value.getNumberValue();
            case BOOL_VALUE -> value.getBoolValue();
            case NULL_VALUE -> null;
            case LIST_VALUE -> value.getListValue().getValuesList().stream()
                    .map(this::valueToObject).toList();
            case STRUCT_VALUE -> structToMap(value.getStructValue());
            default -> null;
        };
    }
}
