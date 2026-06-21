package com.aidentx.authservice.service.Policy;

import com.aidentx.authservice.config.PasswordPolicyExpirationConfig;
import com.aidentx.authservice.model.entity.PasswordPolicy;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.PasswordPolicyRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for automatically extending expired password policy expiration dates.
 * <p>
 * This service runs as a scheduled task to check for expired password policies and 
 * automatically extends their expiration based on the maxPasswordValidityDays configuration.
 * The schedule runs daily at 2:00 AM by default but can be customized via configuration.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Service
public class PasswordPolicyExpirationService {

    private final IUnifiedLogger logger;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final UserRepository userRepository;
    private final PasswordPolicyExpirationConfig config;

    public PasswordPolicyExpirationService(UnifiedLoggerFactory unifiedLoggerFactory,
                                         PasswordPolicyRepository passwordPolicyRepository,
                                         UserRepository userRepository,
                                         PasswordPolicyExpirationConfig config) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordPolicyExpirationService", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.userRepository = userRepository;
        this.config = config;
    }

    /**
     * Scheduled task that runs automatically to check and extend expired password policies.
     * The schedule is configurable through application properties.
     * Default: Daily at 2:00 AM (cron = "0 0 2 * * *")
     * For testing: Every minute (cron = "0 * * * * *")
     */
    @Scheduled(cron = "${aidentx.auth.password-policy.expiration.cron-expression:0 0 2 * * *}")
    @Transactional
    public void extendExpiredPasswordPolicies() {
        // Check if the automatic extension is enabled
        if (!config.isEnabled()) {
            logger.debug().message(AppConstants.DEBUG_CRON_POLICY_EXTENSION_DISABLED).log();
            return;
        }

        logger.info().message(AppConstants.INFO_CRON_POLICY_EXTENSION_START).log();
        
        try {
            long currentTimeMillis = System.currentTimeMillis();
            
            // Log current time for debugging
            logger.info()
                .message(AppConstants.PASSWORD_POLICY_EXTENSION_CRON)
                .detail("currentTimeMillis", currentTimeMillis)
                .detail("currentTimeReadable", new java.util.Date(currentTimeMillis).toString())
                .log();
            
            // Find all password policies that have expired (policyExpiresAt < current time)
            List<PasswordPolicy> expiredPolicies = passwordPolicyRepository.findExpiredPolicies(currentTimeMillis);
            
            // Log details of found policies for debugging
            for (PasswordPolicy policy : expiredPolicies) {
                logger.info()
                    .message(AppConstants.EXPIRED_POLICY_FOUND)
                    .detail("policyId", policy.getId().toString())
                    .detail("tenantId", policy.getTenantId())
                    .detail("policyExpiresAt", policy.getPolicyExpiresAt())
                    .detail("policyExpiresAtReadable", policy.getPolicyExpiresAt() != null ? new java.util.Date(policy.getPolicyExpiresAt()).toString() : "null")
                    .detail("isExpired", policy.getPolicyExpiresAt() != null && policy.getPolicyExpiresAt() < currentTimeMillis)
                    .log();
            }
            
            if (expiredPolicies.isEmpty()) {
                logger.info().message(AppConstants.INFO_CRON_NO_EXPIRED_POLICIES).log();
                return;
            }
            
            // Limit the number of policies processed per run to prevent memory issues
            int policiesToProcess = Math.min(expiredPolicies.size(), config.getMaxPoliciesPerRun());
            List<PasswordPolicy> policiesToExtend = expiredPolicies.subList(0, policiesToProcess);
            
            logger.info().message(String.format(AppConstants.INFO_CRON_FOUND_EXPIRED_POLICIES, policiesToExtend.size())).log();
            
            if (policiesToExtend.size() < expiredPolicies.size()) {
                logger.warn().message(String.format(AppConstants.WARN_CRON_POLICY_LIMIT_REACHED, 
                        expiredPolicies.size(), policiesToProcess)).log();
            }
            
            int extendedCount = 0;
            int skippedCount = 0;
            
            for (PasswordPolicy policy : policiesToExtend) {
                try {
                    if (extendPolicyExpiration(policy, currentTimeMillis)) {
                        extendedCount++;
                        logger.debug().message(String.format(AppConstants.DEBUG_CRON_POLICY_EXTENDED, 
                                policy.getId(), policy.getTenantId())).log();
                    } else {
                        skippedCount++;
                        logger.debug().message(String.format(AppConstants.DEBUG_CRON_POLICY_SKIPPED, 
                                policy.getId(), policy.getTenantId())).log();
                    }
                } catch (Exception ex) {
                    logger.error(ex).message(String.format(AppConstants.ERROR_CRON_POLICY_EXTENSION_FAILED, 
                            policy.getId(), policy.getTenantId())).log();
                    skippedCount++;
                }
            }
            
            logger.info().message(String.format(AppConstants.INFO_CRON_POLICY_EXTENSION_COMPLETE, 
                    extendedCount, skippedCount)).log();
                    
        } catch (Exception ex) {
            logger.error(ex).message(AppConstants.ERROR_CRON_POLICY_EXTENSION_GENERAL).log();
        }
    }

    /**
     * Extends the expiration of a single password policy based on its maxPasswordValidityDays.
     * The new expiration is calculated from the current time, not from the old expiration.
     * 
     * @param policy The password policy to extend
     * @param currentTimeMillis Current time in milliseconds
     * @return true if the policy was extended, false if it was skipped
     */
    private boolean extendPolicyExpiration(PasswordPolicy policy, long currentTimeMillis) {
        // Skip if policy doesn't have maxPasswordValidityDays configured
        if (policy.getMaxPasswordValidityDays() == null || policy.getMaxPasswordValidityDays() <= 0) {
            logger.warn().message(String.format(AppConstants.WARN_CRON_POLICY_NO_VALIDITY_DAYS, 
                    policy.getId(), policy.getTenantId())).log();
            return false;
        }
        
        // Calculate new expiration time: current time + maxPasswordValidityDays
        long daysInMillis = policy.getMaxPasswordValidityDays() * 24L * 60L * 60L * 1000L;
        long newExpirationTime = currentTimeMillis + daysInMillis;
        
        // Store old expiration for logging
        Long oldExpirationTime = policy.getPolicyExpiresAt();
        
        // Update the policy expiration and reset email sent flag for new period
        policy.setPolicyExpiresAt(newExpirationTime);
        policy.setIsEmailSent(false); // Reset email sent flag for the new password expiration period
        passwordPolicyRepository.save(policy);
        
        // Set isPasswordExpired=true for all users in this tenant when policy is extended
        List<User> tenantUsers = userRepository.findByTenantId(policy.getTenantId());
        int usersUpdated = 0;
        for (User user : tenantUsers) {
            user.setIsPasswordExpired(true);
            userRepository.save(user);
            usersUpdated++;
        }
        
        logger.info().message(String.format(AppConstants.INFO_CRON_POLICY_EXPIRATION_EXTENDED, 
                policy.getId(), policy.getTenantId(), oldExpirationTime, newExpirationTime, 
                policy.getMaxPasswordValidityDays())).log();
                
        logger.info()
                .message("Password expiration flags set for tenant users during policy extension")
                .detail("policyId", policy.getId().toString())
                .detail("tenantId", policy.getTenantId())
                .detail("usersUpdated", usersUpdated)
                .log();
        
        logger.debug()
                .message(AppConstants.DEBUG_CRON_EMAIL_FLAG_RESET)
                .detail("policyId", policy.getId().toString())
                .detail("tenantId", policy.getTenantId())
                .log();
        
        return true;
    }

    /**
     * Manual method to trigger policy extension check (for testing or emergency use).
     * This method can be called programmatically if needed.
     */
    public void manualExtendExpiredPolicies() {
        logger.info().message(AppConstants.INFO_CRON_MANUAL_TRIGGER).log();
        extendExpiredPasswordPolicies();
    }

    /**
     * Get count of currently expired policies (for monitoring purposes).
     * 
     * @return Number of currently expired password policies
     */
    public long getExpiredPolicyCount() {
        long currentTimeMillis = System.currentTimeMillis();
        return passwordPolicyRepository.countExpiredPolicies(currentTimeMillis);
    }
}
