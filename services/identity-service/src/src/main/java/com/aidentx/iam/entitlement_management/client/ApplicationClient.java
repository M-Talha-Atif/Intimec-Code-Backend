package com.aidentx.iam.entitlement_management.client;


import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for interacting with Application (core) service.
 *
 * <p>Provides bulk resolution of resourceId → application name.
 *
 * @author aidentx
 * @since 1.0
 */
public interface ApplicationClient {

    /**
     * Fetches application names for given resource IDs.
     *
     * @param tenantId tenant identifier
     * @param resourceIds list of resource IDs
     * @return map of resourceId → application name
     */
    Map<UUID, String> getApplicationNames(String tenantId, List<UUID> resourceIds);
}