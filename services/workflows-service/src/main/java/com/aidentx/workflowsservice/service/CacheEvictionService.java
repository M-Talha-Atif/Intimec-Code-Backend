package com.aidentx.workflowsservice.service;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.aidentx.workflowsservice.config.ApprovalCacheConfig.*;
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * CacheEvictionService - Manages cache invalidation for approval workflows
 * 
 * Phase 5 implementation for the multi-approval enhancement.
 * 
 * This service provides methods to evict cache entries when:
 * - User role assignments change
 * - Org hierarchy changes (manager updates)
 * - Delegations are created/updated/deleted
 * - Workflow templates are modified
 * 
 * Cache eviction strategies:
 * - Targeted eviction: Evict specific keys when known
 * - Full cache eviction: Evict entire cache when bulk changes occur
 */
@Service
public class CacheEvictionService {

    private final IUnifiedLogger logger;
    private final CacheManager cacheManager;

    // Log category
    private static final String LOG_CATEGORY = "workflow-cache";

    public CacheEvictionService(CacheManager cacheManager, UnifiedLoggerFactory loggerFactory) {
        this.cacheManager = cacheManager;
        this.logger = loggerFactory.getLogger(CacheEvictionService.class, LOG_CATEGORY);
    }

    // ============================================
    // Role Cache Eviction
    // ============================================

    /**
     * Evict cached approvers for specific role names.
     * Call this when role membership changes.
     */
    public void evictApproversByRole(String... roleNames) {
        Cache cache = cacheManager.getCache(CACHE_APPROVERS_BY_ROLE);
        if (cache != null) {
            for (String roleName : roleNames) {
                cache.evict(roleName);
                logger.debug()
                    .message("Evicted role cache")
                    .category(LOG_CATEGORY)
                    .detail("roleName", roleName)
                    .log();
            }
        }
    }

    /**
     * Evict all role-based approver cache entries.
     * Call this when bulk role changes occur.
     */
    public void evictAllRoleCache() {
        Cache cache = cacheManager.getCache(CACHE_APPROVERS_BY_ROLE);
        if (cache != null) {
            cache.clear();
            logger.info()
                .message("Cleared all role approver cache")
                .category(LOG_CATEGORY)
                .log();
        }
    }

    // ============================================
    // Manager/Hierarchy Cache Eviction
    // ============================================

    /**
     * Evict manager cache for a specific user.
     * Call this when a user's manager changes.
     */
    public void evictUserManager(UUID userId) {
        evictUserManager(userId.toString());
    }

    /**
     * Evict manager cache for a specific user.
     */
    public void evictUserManager(String userId) {
        Cache managerCache = cacheManager.getCache(CACHE_USER_MANAGERS);
        Cache hierarchyCache = cacheManager.getCache(CACHE_USER_HIERARCHY);
        
        if (managerCache != null) {
            managerCache.evict(userId);
        }
        if (hierarchyCache != null) {
            hierarchyCache.evict(userId);
        }
        
        logger.debug()
            .message("Evicted manager cache for user")
            .category(LOG_CATEGORY)
            .detail("userId", userId)
            .log();
    }

    /**
     * Evict all manager/hierarchy cache entries.
     * Call this when org structure is reorganized.
     */
    public void evictAllManagerCache() {
        Cache managerCache = cacheManager.getCache(CACHE_USER_MANAGERS);
        Cache hierarchyCache = cacheManager.getCache(CACHE_USER_HIERARCHY);
        
        if (managerCache != null) {
            managerCache.clear();
        }
        if (hierarchyCache != null) {
            hierarchyCache.clear();
        }
        
        logger.info()
            .message("Cleared all manager/hierarchy cache")
            .category(LOG_CATEGORY)
            .log();
    }

    // ============================================
    // User Details Cache Eviction
    // ============================================

    /**
     * Evict user details cache for a specific user.
     * Call this when user profile is updated.
     */
    public void evictUserDetails(UUID userId) {
        evictUserDetails(userId.toString());
    }

    /**
     * Evict user details cache for a specific user.
     */
    public void evictUserDetails(String userId) {
        Cache cache = cacheManager.getCache(CACHE_USER_DETAILS);
        if (cache != null) {
            cache.evict(userId);
            logger.debug()
                .message("Evicted user details cache")
                .category(LOG_CATEGORY)
                .detail("userId", userId)
                .log();
        }
    }

    /**
     * Evict all user details cache.
     * Call this for bulk user updates.
     */
    public void evictAllUserDetailsCache() {
        Cache cache = cacheManager.getCache(CACHE_USER_DETAILS);
        if (cache != null) {
            cache.clear();
            logger.info()
                .message("Cleared all user details cache")
                .category(LOG_CATEGORY)
                .log();
        }
    }

    // ============================================
    // Delegation Cache Eviction
    // ============================================

    /**
     * Evict delegation cache for a specific user (as delegator).
     * Call this when delegation is created/updated/deleted.
     */
    public void evictDelegationCache(UUID fromUserId) {
        evictDelegationCache(fromUserId.toString());
    }

    /**
     * Evict delegation cache for a specific user.
     */
    public void evictDelegationCache(String fromUserId) {
        Cache cache = cacheManager.getCache(CACHE_ACTIVE_DELEGATIONS);
        if (cache != null) {
            cache.evict(fromUserId);
            logger.debug()
                .message("Evicted delegation cache for user")
                .category(LOG_CATEGORY)
                .detail("fromUserId", fromUserId)
                .log();
        }
    }

    /**
     * Evict all delegation cache entries.
     * Call this for bulk delegation cleanup.
     */
    public void evictAllDelegationCache() {
        Cache cache = cacheManager.getCache(CACHE_ACTIVE_DELEGATIONS);
        if (cache != null) {
            cache.clear();
            logger.info()
                .message("Cleared all delegation cache")
                .category(LOG_CATEGORY)
                .log();
        }
    }

    // ============================================
    // Workflow Template Cache Eviction
    // ============================================

    /**
     * Evict workflow template cache for a specific template.
     * Call this when template is updated.
     */
    public void evictWorkflowTemplate(UUID templateId) {
        evictWorkflowTemplate(templateId.toString());
    }

    /**
     * Evict workflow template cache for a specific template.
     */
    public void evictWorkflowTemplate(String templateId) {
        Cache cache = cacheManager.getCache(CACHE_WORKFLOW_TEMPLATES);
        if (cache != null) {
            cache.evict(templateId);
            logger.debug()
                .message("Evicted workflow template cache")
                .category(LOG_CATEGORY)
                .detail("templateId", templateId)
                .log();
        }
    }

    /**
     * Evict all workflow template cache.
     * Call this for bulk template changes.
     */
    public void evictAllWorkflowTemplateCache() {
        Cache cache = cacheManager.getCache(CACHE_WORKFLOW_TEMPLATES);
        if (cache != null) {
            cache.clear();
            logger.info()
                .message("Cleared all workflow template cache")
                .category(LOG_CATEGORY)
                .log();
        }
    }

    // ============================================
    // Global Cache Operations
    // ============================================

    /**
     * Evict all caches.
     * Use with caution - only for maintenance or emergency.
     */
    public void evictAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        
        logger.warn()
            .message("Cleared ALL caches")
            .category(LOG_CATEGORY)
            .log();
    }

    /**
     * Get cache statistics for monitoring.
     * Returns cache names and their approximate sizes.
     */
    public java.util.Map<String, Object> getCacheStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Object nativeCache = cache.getNativeCache();
                if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                    com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats = caffeineCache.stats();
                    stats.put(cacheName, java.util.Map.of(
                        "size", caffeineCache.estimatedSize(),
                        "hitCount", cacheStats.hitCount(),
                        "missCount", cacheStats.missCount(),
                        "hitRate", cacheStats.hitRate(),
                        "evictionCount", cacheStats.evictionCount()
                    ));
                }
            }
        });
        
        return stats;
    }
}
