package com.aidentx.authservice.service;

import com.aidentx.authservice.dto.request.UpdateExpiredPasswordRequest;
import com.aidentx.authservice.exception.PasswordValidationException;
import com.aidentx.authservice.model.entity.PasswordPolicy;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.PasswordPolicyRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.Auth.PasswordValidationService;
import com.aidentx.authservice.service.Auth.PasswordHistoryService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple service for handling password expiration based on policy expiration time.
 * 
 * This simplified approach uses a single field (policyExpiresAt) on the PasswordPolicy
 * to determine if passwords are expired for a tenant. When current time > policyExpiresAt,
 * all passwords for that tenant are considered expired.
 */
@Service
public class UpdateExpiredPasswordService {

    private final IUnifiedLogger logger;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final UserRepository userRepository;
    private final PasswordValidationService passwordValidationService;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordEncoder passwordEncoder;

    public UpdateExpiredPasswordService(UnifiedLoggerFactory unifiedLoggerFactory,
                                      PasswordPolicyRepository passwordPolicyRepository,
                                      UserRepository userRepository,
                                      PasswordValidationService passwordValidationService,
                                      PasswordHistoryService passwordHistoryService,
                                      PasswordEncoder passwordEncoder) {
        this.logger = unifiedLoggerFactory.getLogger("UpdateExpiredPasswordService", AppConstants.LOG_CATEGORY_PASSWORD_EXPIRATION);
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.userRepository = userRepository;
        this.passwordValidationService = passwordValidationService;
        this.passwordHistoryService = passwordHistoryService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Update expired password for a user.
     * This method only works if the password is currently expired.
     */
    public Map<String, Object> updateExpiredPassword(String userId, String tenantId, String currentPassword, String newPassword, String confirmPassword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get user first to check if their password is expired
            Optional<User> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_USER_NOT_FOUND_MSG);
                return result;
            }
            
            User user = userOpt.get();
            
            // Check if this specific user's password is expired
            if (user.getIsPasswordExpired() == null || !user.getIsPasswordExpired()) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_NOT_EXPIRED_ERROR_MSG);
                return result;
            }
            
            // Validate password confirmation
            if (!newPassword.equals(confirmPassword)) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_CONFIRMATION_MISMATCH_MSG);
                return result;
            }
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_CURRENT_INCORRECT_MSG);
                return result;
            }
            
            // Get password policy for dynamic validation
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_POLICY_NOT_FOUND_MSG);
                return result;
            }
            
            PasswordPolicy policy = policyOpt.get();
            
            // Validate new password against tenant policy
            try {
                passwordValidationService.validatePassword(newPassword, user);
            } catch (PasswordValidationException validationException) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_VALIDATION_FAILED_MSG);
                result.put("errors", validationException.getValidationErrors());
                return result;
            }
            
            // Update password
            String hashedNewPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHash(hashedNewPassword);
            
            // Set password as no longer expired since user successfully updated it
            user.setIsPasswordExpired(false);
            
            userRepository.save(user);
            
            // Save new password to history (important for passwordHistoryCount enforcement)
            passwordHistoryService.savePasswordToHistory(UUID.fromString(userId), hashedNewPassword, tenantId);
            
            logger.info()
                .message(AppConstants.INFO_EXPIRED_PASSWORD_UPDATE_SUCCESS)
                .detail("userId", userId)
                .detail("tenantId", tenantId)
                .log();
            
            result.put("success", true);
            result.put("message", AppConstants.EXPIRED_PASSWORD_UPDATE_SUCCESS_MSG);
            return result;
            
        } catch (Exception ex) {
            logger.error(ex)
                .message(AppConstants.ERROR_EXPIRED_PASSWORD_UPDATE)
                .detail("userId", userId)
                .detail("tenantId", tenantId)
                .log();
            
            result.put("success", false);
            result.put("message", AppConstants.EXPIRED_PASSWORD_INTERNAL_ERROR_MSG);
            return result;
        }
    }

    /**
     * Check if passwords are expired for a given tenant.
     * Simple logic: current time > policy.policyExpiresAt
     */
    public boolean isPasswordExpired(String tenantId) {
        try {
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            
            if (policyOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.WARN_EXPIRED_PASSWORD_NO_POLICY_EXPIRATION)
                    .detail("tenantId", tenantId)
                    .log();
                return false; // No policy = no expiration
            }

            PasswordPolicy policy = policyOpt.get();
            Long policyExpiresAt = policy.getPolicyExpiresAt();
            
            if (policyExpiresAt == null) {
                logger.debug()
                    .message(AppConstants.DEBUG_EXPIRED_PASSWORD_POLICY_NO_EXPIRATION)
                    .detail("tenantId", tenantId)
                    .log();
                return false; // No expiration time = never expires
            }

            long currentTime = System.currentTimeMillis();
            boolean isExpired = currentTime > policyExpiresAt;
            
            logger.debug()
                .message(AppConstants.DEBUG_EXPIRED_PASSWORD_EXPIRATION_CHECK)
                .detail("tenantId", tenantId)
                .detail("currentTime", currentTime)
                .detail("policyExpiresAt", policyExpiresAt)
                .detail("isExpired", isExpired)
                .log();
                
            return isExpired;
            
        } catch (Exception ex) {
            logger.error(ex)
                .message(AppConstants.ERROR_EXPIRED_PASSWORD_CHECK)
                .detail("tenantId", tenantId)
                .log();
            return false; // Default to not expired on error
        }
    }

    /**
     * Get detailed password expiration information for debugging.
     */
    public Map<String, Object> getPasswordExpirationInfo(String tenantId) {
        Map<String, Object> info = new HashMap<>();
        info.put("tenantId", tenantId);
        info.put("currentTimestamp", System.currentTimeMillis());
        
        try {
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            
            if (policyOpt.isEmpty()) {
                info.put("error", AppConstants.EXPIRED_PASSWORD_POLICY_NOT_FOUND_MSG);
                info.put("isExpired", false);
                return info;
            }

            PasswordPolicy policy = policyOpt.get();
            info.put("policyId", policy.getId());
            info.put("maxPasswordValidityDays", policy.getMaxPasswordValidityDays());
            info.put("policyExpiresAt", policy.getPolicyExpiresAt());
            
            if (policy.getPolicyExpiresAt() != null) {
                long currentTime = System.currentTimeMillis();
                boolean isExpired = currentTime > policy.getPolicyExpiresAt();
                info.put("isExpired", isExpired);
                info.put("timeUntilExpiration", policy.getPolicyExpiresAt() - currentTime);
            } else {
                info.put("isExpired", false);
                info.put("timeUntilExpiration", null);
                info.put("note", AppConstants.DEBUG_EXPIRED_PASSWORD_POLICY_NO_EXPIRATION);
            }
            
        } catch (Exception ex) {
            info.put("error", "Exception occurred: " + ex.getMessage());
            info.put("isExpired", false);
            logger.error(ex)
                .message(AppConstants.ERROR_EXPIRED_PASSWORD_DEBUG_INFO)
                .detail("tenantId", tenantId)
                .log();
        }
        
        return info;
    }

    /**
     * Update expired password for a user. Only works if passwords are expired for the tenant.
     * Applies dynamic password validation based on current tenant policy.
     */
    public Map<String, Object> updateExpiredPassword(String tenantId, String username, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Find the user by username (we'll need to search all users and filter by tenant)
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty() || !userOpt.get().getTenantId().toString().equals(tenantId)) {
                result.put("success", false);
                result.put("error", AppConstants.EXPIRED_PASSWORD_USER_NOT_FOUND_MSG);
                logger.warn()
                    .message(AppConstants.WARN_EXPIRED_PASSWORD_USER_NOT_FOUND)
                    .detail("tenantId", tenantId)
                    .detail("username", username)
                    .log();
                return result;
            }

            User user = userOpt.get();
            
            // Check if this specific user's password is expired
            if (user.getIsPasswordExpired() == null || !user.getIsPasswordExpired()) {
                result.put("success", false);
                result.put("error", AppConstants.EXPIRED_PASSWORD_NOT_EXPIRED_ERROR_MSG);
                logger.warn()
                    .message(AppConstants.WARN_EXPIRED_PASSWORD_UPDATE_WHEN_NOT_EXPIRED)
                    .detail("tenantId", tenantId)
                    .detail("username", username)
                    .log();
                return result;
            }

            // Get password policy for dynamic validation
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                result.put("success", false);
                result.put("error", AppConstants.EXPIRED_PASSWORD_POLICY_NOT_FOUND_MSG);
                logger.warn()
                    .message(AppConstants.WARN_EXPIRED_PASSWORD_NO_POLICY)
                    .detail("tenantId", tenantId)
                    .log();
                return result;
            }

            PasswordPolicy policy = policyOpt.get();

            // Validate new password against current policy (dynamic validation)
            try {
                passwordValidationService.validatePassword(newPassword, user);
            } catch (PasswordValidationException validationException) {
                result.put("success", false);
                result.put("message", AppConstants.EXPIRED_PASSWORD_VALIDATION_FAILED_MSG);
                result.put("errors", validationException.getValidationErrors());
                logger.warn()
                    .message(AppConstants.WARN_EXPIRED_PASSWORD_VALIDATION_FAILED)
                    .detail("tenantId", tenantId)
                    .detail("username", username)
                    .detail("validationErrors", validationException.getValidationErrors())
                    .log();
                return result;
            }

            // Hash and update the password
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHash(hashedPassword);
            
            // Set password as no longer expired since user successfully updated it
            user.setIsPasswordExpired(false);
            
            userRepository.save(user);

            // Save new password to history (important for passwordHistoryCount enforcement)
            passwordHistoryService.savePasswordToHistory(user.getId(), hashedPassword, tenantId);

            result.put("success", true);
            result.put("message", AppConstants.EXPIRED_PASSWORD_UPDATE_SUCCESS_MSG);
            
            logger.info()
                .message(AppConstants.INFO_EXPIRED_PASSWORD_UPDATE_SUCCESS)
                .detail("tenantId", tenantId)
                .detail("username", username)
                .detail("userId", user.getId().toString())
                .log();

            return result;

        } catch (Exception ex) {
            result.put("success", false);
            result.put("error", AppConstants.EXPIRED_PASSWORD_INTERNAL_ERROR_MSG);
            logger.error(ex)
                .message(AppConstants.ERROR_EXPIRED_PASSWORD_UPDATE)
                .detail("tenantId", tenantId)
                .detail("username", username)
                .log();
            return result;
        }
    }

    /**
     * Update expired password for a user using DTO and JWT-extracted user ID.
     * This method only works if the password is currently expired for the tenant.
     * Uses dynamic password validation against current tenant policy.
     */
    public Map<String, Object> updateExpiredPassword(String userId, String tenantId, 
                                                      UpdateExpiredPasswordRequest request) {
        try {
            // Find user first to check if their password is expired
            Optional<User> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", AppConstants.EXPIRED_PASSWORD_USER_NOT_FOUND_MSG);
                return response;
            }
            
            User user = userOpt.get();
            
            // Check if this specific user's password is expired
            if (user.getIsPasswordExpired() == null || !user.getIsPasswordExpired()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", AppConstants.EXPIRED_PASSWORD_NOT_EXPIRED_MSG);
                return response;
            }
            
            // Get password policy for validation
            Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", AppConstants.EXPIRED_PASSWORD_POLICY_NOT_FOUND_MSG);
                return response;
            }
            
            PasswordPolicy policy = policyOpt.get();
            
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", AppConstants.EXPIRED_PASSWORD_CONFIRMATION_MISMATCH_MSG);
                return response;
            }
            
            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", AppConstants.EXPIRED_PASSWORD_CURRENT_INCORRECT_MSG);
                return response;
            }
            
            // Validate new password against current tenant policy (dynamic validation)
            try {
                passwordValidationService.validatePassword(request.getNewPassword(), user);
            } catch (PasswordValidationException validationException) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", AppConstants.EXPIRED_PASSWORD_VALIDATION_FAILED_MSG);
                response.put("errors", validationException.getValidationErrors());
                return response;
            }
            
            // Update password and save to history
            String newHashedPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPasswordHash(newHashedPassword);
            
            // Set password as no longer expired since user successfully updated it
            user.setIsPasswordExpired(false);
            
            userRepository.save(user);
            
            // Save new password to history (important for passwordHistoryCount enforcement)
            passwordHistoryService.savePasswordToHistory(user.getId(), newHashedPassword, tenantId);
            
            logger.info()
                .message(AppConstants.INFO_EXPIRED_PASSWORD_UPDATE_SUCCESS_VIA_DTO)
                .detail("userId", userId)
                .detail("tenantId", tenantId)
                .log();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", AppConstants.EXPIRED_PASSWORD_UPDATE_SUCCESS_MSG);
            return response;
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ERROR_EXPIRED_PASSWORD_UPDATE_VIA_DTO)
                .detail("userId", userId)
                .detail("tenantId", tenantId)
                .log();
                
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
}
