package com.aidentx.access_portal_service.client.grpc;

import com.aidentx.iam.grpc.EntitlementLookupServiceGrpc;
import com.aidentx.iam.grpc.GetEntitlementByIdRequest;
import com.aidentx.iam.grpc.GetEntitlementByIdResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC client for fetching entitlement details from the identity/entitlement management service.
 *
 * SERVICE PURPOSE:
 * This client resolves entitlement metadata (display name, description, risk level, kind,
 * owner, lifecycle status, etc.) by UUID. The access portal service stores only entitlement
 * UUIDs in tile_access_scope_items. When rendering the tile catalog or building access
 * request details, the full entitlement record is needed from the source-of-truth service.
 *
 * HOW IT WORKS:
 * 1. Access portal stores entitlement UUID as TileAccessScopeItem.itemCode
 * 2. This client calls EntitlementLookupService.GetEntitlementById over gRPC
 * 3. The identity service returns the full entitlement record (name, risk, owner, etc.)
 * 4. The caller maps the response fields into the access portal response DTOs
 *
 * WHY gRPC INSTEAD OF REST:
 * - Service-to-service call, no browser involved, binary protobuf is smaller and faster
 * - The .proto contract prevents drift between producer and consumer
 * - HTTP/2 multiplexing handles concurrent lookups efficiently when resolving
 *   multiple entitlements in a tile's access scope page
 *
 * USAGE SCENARIOS:
 * - TileAccessScopeServiceImpl resolving ENTITLEMENT scope items for tile catalog display
 * - AccessRequestServiceImpl fetching entitlement owner for approval workflow routing
 * - Any service-to-service call that needs entitlement metadata by ID
 *
 * ERROR HANDLING:
 * - Returns gRPC NOT_FOUND (status code 5) if entitlement does not exist for the tenant
 * - Callers should catch StatusRuntimeException and handle NOT_FOUND gracefully
 *   (e.g., log warning, show "Unknown Entitlement" in UI)
 *
 * @author aidentx Development Team
 * @version 1.0
 */
@Component
public class EntitlementGrpcClient {

    /**
     * Blocking stub for the EntitlementLookupService hosted on the identity service.
     *
     * The target address is configured in application.yml:
     *   grpc.client.identity-service.address: dns:///identity-service:9090
     *
     * BlockingStub is synchronous (waits for the response like RestTemplate).
     * Suitable here because the caller (TileAccessScopeServiceImpl) already
     * processes items sequentially within a paginated result set.
     */
    @GrpcClient("identity-service")
    private EntitlementLookupServiceGrpc.EntitlementLookupServiceBlockingStub entitlementStub;

    /**
     * Fetches a single entitlement by its UUID and tenant ID.
     *
     * REST equivalent:
     *   GET /api/v1/entitlements/{entitlementId}
     *   Headers: X-Tenant-Id: {tenantId}
     *
     * @param tenantId       tenant isolation key, must match the entitlement's tenant
     * @param entitlementId  UUID of the entitlement to fetch
     * @return full entitlement details including display name, risk level, kind,
     *         owner, lifecycle, classification, and assignment count
     * @throws io.grpc.StatusRuntimeException with NOT_FOUND if entitlement does not exist,
     *         UNAVAILABLE if the identity service is unreachable
     */
    public GetEntitlementByIdResponse getEntitlement(String tenantId, String entitlementId) {
        GetEntitlementByIdRequest request = GetEntitlementByIdRequest.newBuilder()
                .setTenantId(tenantId)
                .setEntitlementId(entitlementId)
                .build();

        return entitlementStub.getEntitlementById(request);
    }
}