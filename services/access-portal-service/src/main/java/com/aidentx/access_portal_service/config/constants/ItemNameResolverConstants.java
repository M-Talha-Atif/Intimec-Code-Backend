package com.aidentx.access_portal_service.config.constants;

/**
 * Constants for the ItemNameResolver enrichment component.
 */
public final class ItemNameResolverConstants {

    private ItemNameResolverConstants() {
        // Utility class
    }

    // Item types
    public static final String ITEM_TYPE_INTERNAL_ROLE = "INTERNAL_ROLE";
    public static final String ITEM_TYPE_ENTITLEMENT = "ENTITLEMENT";
    public static final String ITEM_TYPE_BUSINESS_ROLE = "BUSINESS_ROLE";

    // Thread pool
    public static final int RESOLVER_THREAD_POOL_SIZE = 10;
    public static final String RESOLVER_THREAD_NAME = "item-name-resolver";

    // Timeout
    public static final long RESOLUTION_TIMEOUT_SECONDS = 5;

    // Role data keys
    public static final String ROLE_NAME_KEY = "roleName";

    // Business role data keys (from identity-service AccessRoleDetailResponse)
    public static final String BUSINESS_ROLE_DISPLAY_NAME_KEY = "displayName";
    public static final String BUSINESS_ROLE_NAME_KEY = "name";

    // Log object types
    public static final String LOG_RESOLUTION_START = "item-name-resolution-start";
    public static final String LOG_RESOLUTION_COMPLETE = "item-name-resolution-complete";
    public static final String LOG_RESOLUTION_TIMEOUT = "item-name-resolution-timeout";
    public static final String LOG_RESOLUTION_ITEM_FALLBACK = "item-name-resolution-item-fallback";
    public static final String LOG_RESOLUTION_UNSUPPORTED_TYPE = "item-name-resolution-unsupported-type";
}