package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.PasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for PasswordHistory entity.
 * <p>
 * Provides data access operations for password history management including
 * retrieving recent passwords for history validation and cleanup operations.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    /**
     * Find the most recent password history entries for a user, ordered by creation time (newest first).
     * This is used to enforce password history policies.
     *
     * @param userId   the user ID
     * @param tenantId the tenant ID
     * @param pageable pagination settings (use to limit to N most recent entries)
     * @return list of password history entries, ordered by creation time descending
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.userId = :userId AND ph.tenantId = :tenantId ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findRecentPasswordHistory(@Param("userId") UUID userId, 
                                                   @Param("tenantId") String tenantId, 
                                                   Pageable pageable);

    /**
     * Check if a specific password hash exists in the user's password history.
     * This is used to quickly validate if a password has been used before.
     *
     * @param userId       the user ID
     * @param tenantId     the tenant ID
     * @param passwordHash the hashed password to check
     * @return true if the password hash exists in history, false otherwise
     */
    @Query("SELECT COUNT(ph) > 0 FROM PasswordHistory ph WHERE ph.userId = :userId AND ph.tenantId = :tenantId AND ph.passwordHash = :passwordHash")
    boolean existsByUserIdAndTenantIdAndPasswordHash(@Param("userId") UUID userId, 
                                                   @Param("tenantId") String tenantId, 
                                                   @Param("passwordHash") String passwordHash);

    /**
     * Delete old password history entries for a user, keeping only the N most recent ones.
     * This is used to maintain the password history count limit.
     *
     * @param userId                the user ID
     * @param tenantId              the tenant ID
     * @param passwordHistoryCount  the number of recent entries to keep
     */
    @Modifying
    @Query(value = "DELETE FROM password_history WHERE user_id = :userId AND tenant_id = :tenantId AND id NOT IN " +
                   "(SELECT id FROM (SELECT id FROM password_history WHERE user_id = :userId AND tenant_id = :tenantId " +
                   "ORDER BY created_at DESC LIMIT :passwordHistoryCount) AS recent_passwords)", 
           nativeQuery = true)
    void deleteOldPasswordHistory(@Param("userId") UUID userId, 
                                 @Param("tenantId") String tenantId, 
                                 @Param("passwordHistoryCount") int passwordHistoryCount);

    /**
     * Count the total number of password history entries for a user.
     *
     * @param userId   the user ID
     * @param tenantId the tenant ID
     * @return the count of password history entries
     */
    long countByUserIdAndTenantId(UUID userId, String tenantId);

    /**
     * Delete all password history entries for a user.
     * This might be used when a user account is deleted.
     *
     * @param userId   the user ID
     * @param tenantId the tenant ID
     */
    @Modifying
    void deleteByUserIdAndTenantId(UUID userId, String tenantId);
}
