package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing user sessions.
 * 
 * <p>Provides methods to create, find, and delete user sessions.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    /**
     * Find a session by its session ID.
     * 
     * @param sessionId the session UUID
     * @return Optional containing the session if found
     */
    Optional<UserSession> findBySessionId(UUID sessionId);
    
    /**
     * Find a session by its refresh token.
     * 
     * @param refreshToken the refresh token
     * @return Optional containing the session if found
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    /**
     * Find all sessions for a specific user.
     * 
     * @param userId the user ID
     * @return list of sessions for the user
     */
    List<UserSession> findByUserId(UUID userId);
    
    /**
     * Find all sessions for a user in a specific tenant.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return list of sessions
     */
    List<UserSession> findByUserIdAndTenantId(UUID userId, String tenantId);
    
    /**
     * Delete a session by refresh token.
     * 
     * @param refreshToken the refresh token
     * @return number of deleted records
     */
    int deleteByRefreshToken(String refreshToken);
    
    /**
     * Delete all sessions for a specific user.
     * 
     * @param userId the user ID
     * @return number of deleted records
     */
    int deleteByUserId(UUID userId);
    
    /**
     * Check if a session exists with the given refresh token.
     * 
     * @param refreshToken the refresh token
     * @return true if exists, false otherwise
     */
    boolean existsByRefreshToken(String refreshToken);
    
    /**
     * Find all sessions for a specific tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of sessions for the tenant
     */
    List<UserSession> findByTenantId(String tenantId);
}
