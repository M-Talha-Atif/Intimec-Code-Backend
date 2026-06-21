package com.aidentx.workflowsservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * ApprovalCacheConfig - Caffeine-based caching configuration for approval workflows
 * 
 * Phase 5 implementation for the multi-approval enhancement.
 * 
 * This configuration provides high-performance caching for:
 * - Role-based approver resolution
 * - Hierarchy-based approver resolution (manager lookups)
 * - User details caching
 * - Delegation lookup caching
 * 
 * Cache eviction strategies:
 * - Time-based: Entries expire after a configured duration
 * - Size-based: Maximum entries to prevent memory issues
 * 
 * Performance benefits:
 * - Reduces database queries for repeated role lookups
 * - Reduces gRPC calls for manager hierarchy resolution
 * - Improves approval creation throughput
 */
@Configuration
@EnableCaching
public class ApprovalCacheConfig {

    // Cache names
    public static final String CACHE_APPROVERS_BY_ROLE = "approvers-by-role";
    public static final String CACHE_USER_MANAGERS = "user-managers";
    public static final String CACHE_USER_HIERARCHY = "user-hierarchy";
    public static final String CACHE_USER_DETAILS = "user-details";
    public static final String CACHE_ACTIVE_DELEGATIONS = "active-delegations";
    public static final String CACHE_WORKFLOW_TEMPLATES = "workflow-templates";

    // Default cache settings
    private static final int DEFAULT_EXPIRE_AFTER_WRITE_MINUTES = 5;
    private static final int DEFAULT_EXPIRE_AFTER_ACCESS_MINUTES = 3;
    private static final int DEFAULT_MAXIMUM_SIZE = 1000;

    // Specific cache settings
    private static final int ROLE_CACHE_EXPIRE_MINUTES = 5;
    private static final int ROLE_CACHE_MAX_SIZE = 500;
    
    private static final int MANAGER_CACHE_EXPIRE_MINUTES = 10;
    private static final int MANAGER_CACHE_MAX_SIZE = 2000;
    
    private static final int USER_DETAILS_CACHE_EXPIRE_MINUTES = 5;
    private static final int USER_DETAILS_CACHE_MAX_SIZE = 5000;
    
    private static final int DELEGATION_CACHE_EXPIRE_MINUTES = 2;
    private static final int DELEGATION_CACHE_MAX_SIZE = 1000;

    private static final int TEMPLATE_CACHE_EXPIRE_MINUTES = 15;
    private static final int TEMPLATE_CACHE_MAX_SIZE = 200;

    /**
     * Primary cache manager using Caffeine for high-performance caching.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Enable statistics recording for monitoring
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(DEFAULT_EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(DEFAULT_MAXIMUM_SIZE)
            .recordStats());
        
        // Register cache names
        cacheManager.setCacheNames(java.util.List.of(
            CACHE_APPROVERS_BY_ROLE,
            CACHE_USER_MANAGERS,
            CACHE_USER_HIERARCHY,
            CACHE_USER_DETAILS,
            CACHE_ACTIVE_DELEGATIONS,
            CACHE_WORKFLOW_TEMPLATES
        ));
        
        return cacheManager;
    }

    /**
     * Specialized cache manager for role-based approver resolution.
     * Uses shorter TTL since role memberships can change.
     */
    @Bean
    public Caffeine<Object, Object> roleCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(ROLE_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(ROLE_CACHE_MAX_SIZE)
            .recordStats();
    }

    /**
     * Specialized cache builder for manager hierarchy lookups.
     * Uses longer TTL since org structure changes infrequently.
     */
    @Bean
    public Caffeine<Object, Object> managerCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(MANAGER_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MANAGER_CACHE_MAX_SIZE)
            .recordStats();
    }

    /**
     * Cache builder for user details.
     * Frequently accessed during notification sending.
     */
    @Bean
    public Caffeine<Object, Object> userDetailsCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(USER_DETAILS_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .expireAfterAccess(DEFAULT_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
            .maximumSize(USER_DETAILS_CACHE_MAX_SIZE)
            .recordStats();
    }

    /**
     * Cache builder for delegation lookups.
     * Uses shorter TTL since delegations can be created/updated frequently.
     */
    @Bean
    public Caffeine<Object, Object> delegationCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(DELEGATION_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(DELEGATION_CACHE_MAX_SIZE)
            .recordStats();
    }

    /**
     * Cache builder for workflow templates.
     * Uses longer TTL since templates change infrequently.
     */
    @Bean
    public Caffeine<Object, Object> templateCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(TEMPLATE_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(TEMPLATE_CACHE_MAX_SIZE)
            .recordStats();
    }
}
