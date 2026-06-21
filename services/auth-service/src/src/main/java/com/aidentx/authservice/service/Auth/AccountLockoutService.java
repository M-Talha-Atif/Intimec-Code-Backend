package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.model.entity.AccountLockPolicy;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.AccountLockPolicyRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing account lockout functionality based on tenant-specific policies.
 * <p>
 * This service handles:
 * - Checking if an account is currently locked
 * - Processing failed login attempts and applying lockout policies
 * - Resetting failed attempts on successful login
 * - Progressive lockout delays for repeated lockouts
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Service
public class AccountLockoutService {

    private final IUnifiedLogger logger;
    private final UserRepository userRepository;
    private final AccountLockPolicyRepository accountLockPolicyRepository;

    @Autowired
    public AccountLockoutService(UnifiedLoggerFactory unifiedLoggerFactory,
                                UserRepository userRepository,
                                AccountLockPolicyRepository accountLockPolicyRepository) {
        this.logger = unifiedLoggerFactory.getLogger("AccountLockoutService", AppConstants.LOG_CATEGORY_AUTH);
        this.userRepository = userRepository;
        this.accountLockPolicyRepository = accountLockPolicyRepository;
    }

    /**
     * Checks if an account is currently locked based on the lockedUntil timestamp.
     * 
     * @param user the user to check
     * @return true if the account is locked, false otherwise
     */
    public boolean isAccountLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }

        Instant now = Instant.now();
        boolean isLocked = now.isBefore(user.getLockedUntil());
        
        logger.info().message(AppConstants.MSG_ACCOUNT_LOCK_CHECK)
                .detail("userId", user.getId().toString())
                .detail("tenantId", user.getTenantId())
                .detail("lockedUntil", user.getLockedUntil().toString())
                .detail("currentTime", now.toString())
                .detail("isCurrentlyLocked", String.valueOf(isLocked))
                .log();

        return isLocked;
    }

    /**
     * Processes a failed login attempt and applies lockout policy if necessary.
     * This method runs in a separate transaction to ensure the failed attempt count
     * is saved even if the main login transaction is rolled back due to authentication failure.
     *
     * @param user the user who failed to login
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processFailedLoginAttempt(User user) {
        try {
            // Get the account lock policy for this tenant
            Optional<AccountLockPolicy> policyOpt = accountLockPolicyRepository.findByTenantId(user.getTenantId());
            if (policyOpt.isEmpty()) {
                logger.warn().message(AppConstants.LOG_NO_ACCOUNT_LOCK_POLICY_FOUND)
                        .detail("tenantId", user.getTenantId())
                        .detail("userId", user.getId().toString())
                        .log();
                return;
            }

            AccountLockPolicy policy = policyOpt.get();
            int currentAttempts = (user.getFailedLoginAttempts() != null) ? user.getFailedLoginAttempts() : 0;
            
            // Check if this is a progressive delay scenario (user was previously locked)
            boolean wasRecentlyLocked = false;
            if (user.getLockedUntil() != null) {
                Instant now = Instant.now();
                if (now.isAfter(user.getLockedUntil())) {
                    // Previous lockout has expired - clear it and mark for progressive delay
                    wasRecentlyLocked = true;
                    user.setLockedUntil(null);
                    logger.info().message(AppConstants.MSG_LOCKOUT_EXPIRED)
                            .detail("userId", user.getId().toString())
                            .detail("tenantId", user.getTenantId())
                            .detail("currentFailedAttempts", String.valueOf(currentAttempts))
                            .log();
                }
            }

            // Always increment failed attempts - never reset here, only on successful login
            int newAttempts = currentAttempts + 1;
            user.setFailedLoginAttempts(newAttempts);

            logger.info().message(AppConstants.LOG_FAILED_LOGIN_ATTEMPT_PROCESSED)
                    .detail("userId", user.getId().toString())
                    .detail("tenantId", user.getTenantId())
                    .detail("newFailedAttempts", String.valueOf(newAttempts))
                    .detail("maxAllowed", String.valueOf(policy.getMaxFailedLoginAttempts()))
                    .detail("wasRecentlyLocked", String.valueOf(wasRecentlyLocked))
                    .log();

            // Check if we should lock the account (basic lockout logic)
            if (newAttempts >= policy.getMaxFailedLoginAttempts()) {
                lockAccount(user, policy, wasRecentlyLocked, newAttempts);
            }

            userRepository.save(user);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_LOGIN_PROCESS_ERROR)
                    .detail("userId", user.getId().toString())
                    .detail("tenantId", user.getTenantId())
                    .log();
            throw e;
        }
    }

    /**
     * Resets failed login attempts on successful login.
     * This method runs in a separate transaction to ensure the reset
     * is saved even if there are issues with the main transaction.
     *
     * @param user the user who successfully logged in
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedLoginAttempts(User user) {
        try {
            if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
                logger.info().message(AppConstants.LOG_RESET_FAILED_ATTEMPTS)
                        .detail("userId", user.getId().toString())
                        .detail("tenantId", user.getTenantId())
                        .detail("previousFailedAttempts", String.valueOf(user.getFailedLoginAttempts()))
                        .log();

                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null); // Clear any existing lock
                // Note: lastLoginAt is now managed by the calling service (AuthServiceImpl)
                userRepository.save(user);
            }
            // If no failed attempts, no need to save anything

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_RESET_FAILED_ATTEMPTS_ERROR)
                    .detail("userId", user.getId().toString())
                    .detail("tenantId", user.getTenantId())
                    .log();
            throw e;
        }
    }

    /**
     * Locks the user account based on the tenant's lockout policy.
     * Implements progressive lockout delays for repeated lockouts.
     *
     * @param user the user to lock
     * @param policy the lockout policy to apply
     * @param wasRecentlyLocked whether this user was recently locked (for progressive delay)
     * @param currentAttempts the current failed login attempts count
     */
    private void lockAccount(User user, AccountLockPolicy policy, boolean wasRecentlyLocked, int currentAttempts) {
        Instant now = Instant.now();
        long lockoutDurationSeconds = policy.getLockoutDurationSeconds();

        // Apply progressive delay if this is a repeated lockout after expiration
        if (wasRecentlyLocked && policy.getIncreaseDelayAfterFailureSeconds() > 0) {
            long additionalDelay = policy.getIncreaseDelayAfterFailureSeconds();
            lockoutDurationSeconds += additionalDelay;

            logger.info().message(AppConstants.LOG_PROGRESSIVE_LOCKOUT_APPLIED)
                    .detail("userId", user.getId().toString())
                    .detail("tenantId", user.getTenantId())
                    .detail("baseLockoutSeconds", String.valueOf(policy.getLockoutDurationSeconds()))
                    .detail("additionalDelaySeconds", String.valueOf(additionalDelay))
                    .detail("totalLockoutSeconds", String.valueOf(lockoutDurationSeconds))
                    .detail("wasRecentlyLocked", String.valueOf(wasRecentlyLocked))
                    .log();
        }

        Instant lockUntil = now.plusSeconds(lockoutDurationSeconds);
        user.setLockedUntil(lockUntil);

        logger.warn().message(AppConstants.LOG_ACCOUNT_LOCKED)
                .detail("userId", user.getId().toString())
                .detail("tenantId", user.getTenantId())
                .detail("lockoutDurationSeconds", String.valueOf(lockoutDurationSeconds))
                .detail("lockedUntil", lockUntil.toString())
                .detail("failedAttempts", String.valueOf(currentAttempts))
                .detail("maxAllowedAttempts", String.valueOf(policy.getMaxFailedLoginAttempts()))
                .detail("wasRecentlyLocked", String.valueOf(wasRecentlyLocked))
                .log();
    }
}
