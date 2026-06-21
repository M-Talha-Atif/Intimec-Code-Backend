package com.aidentx.applicationpermissions.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the application-permissions-service.
 * 
 * <p>This configuration uses Caffeine, a high-performance, near-optimal caching library
 * for Java 8+. It provides automatic cache eviction based on time and size constraints,
 * with built-in statistics tracking for monitoring cache performance.
 * 
 * <h3>Cache Strategy:</h3>
 * <ul>
 *   <li><strong>Permission Checks Cache:</strong> Stores user permission results (5 min TTL, 10K entries max)</li>
 *   <li><strong>User Roles Cache:</strong> Stores user role assignments (5 min TTL, 5K entries max)</li>
 *   <li><strong>Page Access Cache:</strong> Stores page-level access results (5 min TTL, 5K entries max)</li>
 * </ul>
 * 
 * <h3>Cache Eviction:</h3>
 * <ul>
 *   <li><strong>Time-based:</strong> Entries expire 5 minutes after write</li>
 *   <li><strong>Size-based:</strong> LRU eviction when max size reached</li>
 *   <li><strong>Manual:</strong> Via @CacheEvict on permission assignment/revocation methods</li>
 * </ul>
 * 
 * <h3>Performance Impact:</h3>
 * <ul>
 *   <li><strong>Without cache:</strong> 5 DB queries × 50ms = 250ms per permission check</li>
 *   <li><strong>With cache:</strong> Cache hit in ~0.1ms (2500x faster)</li>
 *   <li><strong>Cache hit rate:</strong> Expected 95-99% after warmup</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Method-level caching
 * {@literal @}Cacheable(value = "permissionChecks", key = "#userId + ':' + #method + ':' + #endpoint")
 * public boolean hasPermissionForEndpoint(String userId, String endpoint, String method) {
 *     // Database query only on cache miss
 * }
 * 
 * // Cache eviction on changes
 * {@literal @}CacheEvict(value = "permissionChecks", allEntries = true)
 * public void assignPermissionToRole(UUID roleId, UUID permissionId) {
 *     // Clear cache after permission changes
 * }
 * </pre>
 * 
 * <h3>Monitoring:</h3>
 * <p>Cache statistics are automatically recorded and can be accessed via:
 * <ul>
 *   <li>Spring Boot Actuator metrics endpoint</li>
 *   <li>JMX beans</li>
 *   <li>Application logs (via CacheStats API)</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CacheEvict
 * @see com.github.benmanes.caffeine.cache.Caffeine
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Cache for permission check results.
     * Key format: "userId:httpMethod:endpoint" (e.g., "123:DELETE:/api/v1/identity/*")
     * 
     * <p><strong>Configuration:</strong>
     * <ul>
     *   <li>TTL: 5 minutes after write</li>
     *   <li>Max entries: 10,000 (covers ~1000 users with 10 permissions each)</li>
     *   <li>Eviction: LRU (Least Recently Used)</li>
     * </ul>
     * 
     * <p><strong>Memory usage:</strong> ~2MB for 10K entries (assuming 200 bytes per entry)
     */
    private static final String PERMISSION_CHECKS_CACHE = "permissionChecks";
    
    /**
     * Cache for user role assignments.
     * Key format: "userId:tenantId" (e.g., "123:default")
     * 
     * <p><strong>Configuration:</strong>
     * <ul>
     *   <li>TTL: 5 minutes after write</li>
     *   <li>Max entries: 5,000 (covers 5000 unique user-tenant combinations)</li>
     *   <li>Eviction: LRU</li>
     * </ul>
     * 
     * <p><strong>Memory usage:</strong> ~1MB for 5K entries
     */
    private static final String USER_ROLES_CACHE = "userRoles";
    
    /**
     * Cache for page-level access checks.
     * Key format: "userId:tenantId:pagePath" (e.g., "123:default:/identities")
     * 
     * <p><strong>Configuration:</strong>
     * <ul>
     *   <li>TTL: 5 minutes after write</li>
     *   <li>Max entries: 5,000 (covers ~500 users accessing ~10 pages each)</li>
     *   <li>Eviction: LRU</li>
     * </ul>
     * 
     * <p><strong>Memory usage:</strong> ~1MB for 5K entries
     */
    private static final String PAGE_ACCESS_CACHE = "pageAccess";
    
    /**
     * Creates and configures the cache manager with multiple Caffeine caches.
     * 
     * <p>Each cache has its own configuration tuned for its specific use case:
     * <ul>
     *   <li><strong>permissionChecks:</strong> High volume, short-lived permission results</li>
     *   <li><strong>userRoles:</strong> Medium volume, user role assignments</li>
     *   <li><strong>pageAccess:</strong> Medium volume, page-level access results</li>
     * </ul>
     * 
     * @return configured CacheManager with all caches registered
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        cacheManager.setCaches(Arrays.asList(
            // Permission checks cache - highest volume, most critical for performance
            buildCache(PERMISSION_CHECKS_CACHE, 10_000, 5, TimeUnit.MINUTES),
            
            // User roles cache - medium volume, frequently accessed
            buildCache(USER_ROLES_CACHE, 5_000, 5, TimeUnit.MINUTES),
            
            // Page access cache - medium volume, UI navigation
            buildCache(PAGE_ACCESS_CACHE, 5_000, 5, TimeUnit.MINUTES)
        ));
        
        return cacheManager;
    }
    
    /**
     * Builds a Caffeine cache with specified configuration.
     * 
     * @param name cache name for identification
     * @param maxSize maximum number of entries before LRU eviction
     * @param expireAfterWrite time duration before entries expire
     * @param timeUnit time unit for expiration
     * @return configured CaffeineCache instance
     */
    private CaffeineCache buildCache(String name, long maxSize, long expireAfterWrite, TimeUnit timeUnit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            // Time-based eviction: Remove entries after X minutes
            .expireAfterWrite(expireAfterWrite, timeUnit)
            
            // Size-based eviction: LRU when max size reached
            .maximumSize(maxSize)
            
            // Enable statistics tracking for monitoring
            .recordStats()
            
            // Build the cache
            .build());
    }
}
