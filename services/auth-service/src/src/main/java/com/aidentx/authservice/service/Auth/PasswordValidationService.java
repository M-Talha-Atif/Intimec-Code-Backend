package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.exception.PasswordValidationException;
import com.aidentx.authservice.model.entity.PasswordPolicy;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.PasswordPolicyRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating passwords against tenant-specific password policies.
 * <p>
 * This service provides comprehensive password validation including:
 * - Length requirements
 * - Character complexity requirements  
 * - Username/email restrictions
 * - Banned substring checks
 * - Password history validation
 * </p>
 * 
 * @author Aidentx
 * @since 1.0
 */
@Service
public class PasswordValidationService {

    private final IUnifiedLogger logger;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordValidationService(UnifiedLoggerFactory unifiedLoggerFactory,
                                   PasswordPolicyRepository passwordPolicyRepository,
                                   PasswordHistoryService passwordHistoryService,
                                   PasswordEncoder passwordEncoder) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordValidationService", "user_management");
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.passwordHistoryService = passwordHistoryService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates a password against the tenant-specific password policy.
     * <p>
     * This method performs comprehensive password validation including:
     * - Minimum length requirements
     * - Character complexity requirements (uppercase, lowercase, digits, special characters)
     * - Username/email restrictions
     * - Banned substrings validation
     * - Password history validation (reuse prevention)
     * </p>
     *
     * @param password the password to validate
     * @param user     the user attempting to set the password
     * @throws PasswordValidationException if the password fails validation
     */
    public void validatePassword(String password, User user) throws PasswordValidationException {
        List<String> errors = new ArrayList<>();

        try {
            logger.info().message(AppConstants.LOG_PASSWORD_VALIDATION_START)
                    .detail("userId", user.getId().toString())
                    .detail("tenantId", user.getTenantId())
                    .log();

            // Get the password policy for this tenant
            PasswordPolicy policy = getPasswordPolicyForTenant(user.getTenantId());

            // 1. Validate minimum length
            validateMinimumLength(password, policy, errors);

            // 2. Validate character requirements
            validateCharacterRequirements(password, policy, errors);

            // 3. Validate username/email restrictions
            validateUsernameEmailRestrictions(password, user, policy, errors);

            // 4. Validate banned substrings
            validateBannedSubstrings(password, policy, errors);

            // 5. Validate password history
            validatePasswordHistory(password, user, policy, errors);

            if (errors.isEmpty()) {
                logger.info().message(AppConstants.LOG_PASSWORD_VALIDATION_SUCCESS)
                        .detail("userId", user.getId().toString())
                        .detail("tenantId", user.getTenantId())
                        .log();
            } else {
                logger.warn().message(AppConstants.LOG_PASSWORD_VALIDATION_FAILED)
                        .detail("userId", user.getId().toString())
                        .detail("tenantId", user.getTenantId())
                        .detail("errorCount", errors.size())
                        .log();
                
                throw new PasswordValidationException(errors);
            }

        } catch (PasswordValidationException e) {
            // Re-throw password validation exceptions
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_PASSWORD_VALIDATION_ERROR)
                    .detail("userId", user.getId().toString())
                    .detail("tenantId", user.getTenantId())
                    .log();
            errors.add(AppConstants.PASSWORD_VALIDATION_GENERAL_ERROR);
            throw new PasswordValidationException(errors);
        }
    }    /**
     * Gets the password policy for the specified tenant.
     */
    private PasswordPolicy getPasswordPolicyForTenant(String tenantId) {
        return passwordPolicyRepository.findByTenantId(tenantId).orElse(null);
    }

    /**
     * Validates minimum password length requirement.
     */
    private void validateMinimumLength(String password, PasswordPolicy policy, List<String> errors) {
        if (policy.getMinPasswordLength() != null && 
            password.length() < policy.getMinPasswordLength()) {
            errors.add(String.format(AppConstants.PASSWORD_VALIDATION_MIN_LENGTH, 
                      policy.getMinPasswordLength()));
        }
    }

    /**
     * Validates character complexity requirements.
     */
    private void validateCharacterRequirements(String password, PasswordPolicy policy, List<String> errors) {
        // Check uppercase requirement
        if (Boolean.TRUE.equals(policy.getRequireUppercase()) && 
            !Pattern.compile("[A-Z]").matcher(password).find()) {
            errors.add(AppConstants.PASSWORD_VALIDATION_REQUIRE_UPPERCASE);
        }

        // Check lowercase requirement
        if (Boolean.TRUE.equals(policy.getRequireLowercase()) && 
            !Pattern.compile("[a-z]").matcher(password).find()) {
            errors.add(AppConstants.PASSWORD_VALIDATION_REQUIRE_LOWERCASE);
        }

        // Check digits requirement
        if (Boolean.TRUE.equals(policy.getRequireDigits()) && 
            !Pattern.compile("[0-9]").matcher(password).find()) {
            errors.add(AppConstants.PASSWORD_VALIDATION_REQUIRE_DIGITS);
        }

        // Check special characters requirement
        if (Boolean.TRUE.equals(policy.getRequireSpecialChars()) && 
            !Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) {
            errors.add(AppConstants.PASSWORD_VALIDATION_REQUIRE_SPECIAL_CHARS);
        }
    }

    /**
     * Validates username/email restrictions.
     */
    private void validateUsernameEmailRestrictions(String password, User user, PasswordPolicy policy, List<String> errors) {
        if (Boolean.TRUE.equals(policy.getDisallowUsernameOrEmail())) {
            String lowerPassword = password.toLowerCase();
            
            // Check username
            if (user.getUsername() != null && 
                lowerPassword.contains(user.getUsername().toLowerCase())) {
                errors.add(AppConstants.PASSWORD_VALIDATION_USERNAME_NOT_ALLOWED);
            }

            // Check email
            if (user.getEmail() != null && 
                lowerPassword.contains(user.getEmail().toLowerCase())) {
                errors.add(AppConstants.PASSWORD_VALIDATION_EMAIL_NOT_ALLOWED);
            }
        }
    }

    /**
     * Validates banned substrings.
     */
    private void validateBannedSubstrings(String password, PasswordPolicy policy, List<String> errors) {
        if (policy.getBannedSubstrings() != null && !policy.getBannedSubstrings().isEmpty()) {
            String lowerPassword = password.toLowerCase();
            
            for (String bannedSubstring : policy.getBannedSubstrings()) {
                if (bannedSubstring != null && 
                    lowerPassword.contains(bannedSubstring.toLowerCase())) {
                    errors.add(String.format(AppConstants.PASSWORD_VALIDATION_BANNED_SUBSTRING, bannedSubstring));
                }
            }
        }
    }

    /**
     * Validates password history to prevent reuse.
     */
    private void validatePasswordHistory(String password, User user, PasswordPolicy policy, List<String> errors) {
        if (policy.getPasswordHistoryCount() != null && policy.getPasswordHistoryCount() > 0) {
            boolean isReused = passwordHistoryService.isPasswordReused(
                user.getId(), 
                password, // Pass plain password, not hashed
                user.getTenantId(), 
                policy.getPasswordHistoryCount()
            );

            if (isReused) {
                errors.add(String.format(AppConstants.PASSWORD_VALIDATION_PASSWORD_HISTORY, 
                          policy.getPasswordHistoryCount()));
            }
        }
    }
}
