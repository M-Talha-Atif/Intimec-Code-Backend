package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.model.entity.PasswordHistory;
import com.aidentx.authservice.repository.PasswordHistoryRepository;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing password history operations.
 * <p>
 * This service handles saving password history entries and validating
 * password reuse based on password policy settings.
 * </p>
 * 
 * @author Aidentx
 * @since 1.0
 */
@Service
public class PasswordHistoryService {

    private final IUnifiedLogger logger;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordHistoryService(PasswordHistoryRepository passwordHistoryRepository,
                                 PasswordEncoder passwordEncoder,
                                 UnifiedLoggerFactory unifiedLoggerFactory) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.logger = unifiedLoggerFactory.getLogger("PasswordHistoryService", AppConstants.LOG_CATEGORY_USER_MGMT);
    }

    /**
     * Saves a password hash to the user's password history.
     * <p>
     * This method should be called whenever a user changes their password
     * (either through password update or reset operations).
     * </p>
     *
     * @param userId       the user ID
     * @param passwordHash the hashed password to save
     * @param tenantId     the tenant ID
     */
    @Transactional
    public void savePasswordToHistory(UUID userId, String passwordHash, String tenantId) {
        try {
            logger.info().message(AppConstants.LOG_PASSWORD_HISTORY_SAVE_ATTEMPT)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("passwordHashPrefix", passwordHash.substring(0, Math.min(10, passwordHash.length())))
                    .log();

            PasswordHistory passwordHistory = new PasswordHistory(userId, passwordHash, tenantId);
            PasswordHistory savedHistory = passwordHistoryRepository.save(passwordHistory);

            logger.info().message(AppConstants.LOG_PASSWORD_HISTORY_SAVE_SUCCESS)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("historyId", savedHistory.getId().toString())
                    .detail("createdAt", savedHistory.getCreatedAt())
                    .log();

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_PASSWORD_HISTORY_SAVE_FAILED)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("errorMessage", e.getMessage())
                    .log();
            // Don't throw exception to avoid breaking password change operations
            // Password history is important but not critical for the main flow
        }
    }

    /**
     * Validates if a password has been used before by checking password history.
     * <p>
     * This method compares the new plain password against the user's recent
     * password history based on the password policy settings.
     * </p>
     *
     * @param userId                the user ID
     * @param plainPassword         the plain password to validate
     * @param tenantId              the tenant ID
     * @param passwordHistoryCount  the number of recent passwords to check (from password policy)
     * @return true if the password has been used before, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isPasswordReused(UUID userId, String plainPassword, String tenantId, int passwordHistoryCount) {
        try {
            if (passwordHistoryCount <= 0) {
                // Password history is disabled
                return false;
            }

            logger.trace().message(AppConstants.LOG_PASSWORD_REUSE_CHECK_START)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("historyCount", passwordHistoryCount)
                    .log();

            // Get the recent password hashes to compare against
            List<PasswordHistory> recentPasswords = passwordHistoryRepository
                    .findRecentPasswordHistory(userId, tenantId, PageRequest.of(0, passwordHistoryCount));

            // Check if the plain password matches any of the stored hashed passwords
            boolean isReused = recentPasswords.stream()
                    .anyMatch(passwordHistory -> 
                        passwordEncoder.matches(plainPassword, passwordHistory.getPasswordHash())
                    );

            logger.debug().message(AppConstants.LOG_PASSWORD_REUSE_CHECK_COMPLETED)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("isReused", isReused)
                    .detail("checkedHistoryCount", recentPasswords.size())
                    .log();

            return isReused;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_PASSWORD_REUSE_CHECK_ERROR)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .log();
            // Return false to allow password change in case of error
            // Better to allow a potentially reused password than block legitimate changes
            return false;
        }
    }

    /**
     * Cleans up old password history entries, keeping only the specified number of recent entries.
     * <p>
     * This method should be called after saving a new password to maintain the
     * password history limit defined in the password policy.
     * </p>
     *
     * @param userId                the user ID
     * @param tenantId              the tenant ID
     * @param passwordHistoryCount  the number of recent entries to keep
     */
    @Transactional
    public void cleanupOldPasswordHistory(UUID userId, String tenantId, int passwordHistoryCount) {
        try {
            if (passwordHistoryCount <= 0) {
                // If password history is disabled, we might want to clean all entries
                passwordHistoryRepository.deleteByUserIdAndTenantId(userId, tenantId);
                logger.info().message(AppConstants.LOG_PASSWORD_HISTORY_ALL_CLEANED)
                        .detail("userId", userId.toString())
                        .detail("tenantId", tenantId)
                        .log();
                return;
            }

            logger.trace().message(AppConstants.LOG_PASSWORD_HISTORY_CLEANUP_START)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("keepCount", passwordHistoryCount)
                    .log();

            passwordHistoryRepository.deleteOldPasswordHistory(userId, tenantId, passwordHistoryCount);

            logger.debug().message(AppConstants.LOG_PASSWORD_HISTORY_CLEANUP_SUCCESS)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .detail("keepCount", passwordHistoryCount)
                    .log();

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_PASSWORD_HISTORY_CLEANUP_FAILED)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .log();
            // Don't throw exception - cleanup is not critical
        }
    }

    /**
     * Gets the count of password history entries for a user.
     *
     * @param userId   the user ID
     * @param tenantId the tenant ID
     * @return the count of password history entries
     */
    @Transactional(readOnly = true)
    public long getPasswordHistoryCount(UUID userId, String tenantId) {
        try {
            return passwordHistoryRepository.countByUserIdAndTenantId(userId, tenantId);
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_PASSWORD_HISTORY_COUNT_FAILED)
                    .detail("userId", userId.toString())
                    .detail("tenantId", tenantId)
                    .log();
            return 0;
        }
    }
}
