package com.aidentx.authservice.controllers;

import com.aidentx.authservice.service.Policy.PasswordPolicyExpirationService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for managing password policy expiration operations.
 * Provides endpoints for monitoring and manually triggering policy extension jobs.
 *
 * @author Aidentx
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/admin/password-policy-expiration")
public class PasswordPolicyExpirationController {

    private final IUnifiedLogger logger;
    private final PasswordPolicyExpirationService passwordPolicyExpirationService;

    public PasswordPolicyExpirationController(UnifiedLoggerFactory unifiedLoggerFactory,
                                            PasswordPolicyExpirationService passwordPolicyExpirationService) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordPolicyExpirationController", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.passwordPolicyExpirationService = passwordPolicyExpirationService;
    }

    /**
     * Get the count of currently expired password policies.
     * 
     * @return ResponseEntity with expired policy count
     */
    @GetMapping("/expired-count")
    public ResponseEntity<Map<String, Object>> getExpiredPolicyCount() {
        logger.debug().message(AppConstants.DEBUG_CONTROLLER_GET_EXPIRED_POLICY_COUNT).log();
        
        try {
            long expiredCount = passwordPolicyExpirationService.getExpiredPolicyCount();
            
            return ResponseEntity.ok(Map.of(
                "expiredPolicyCount", expiredCount,
                "status", "success"
            ));
            
        } catch (Exception ex) {
            logger.error(ex).message(AppConstants.ERROR_CONTROLLER_GET_EXPIRED_POLICY_COUNT).log();
            return ResponseEntity.internalServerError().body(Map.of(
                "error", AppConstants.ERROR_CONTROLLER_EXPIRED_POLICY_COUNT_FAILED,
                "status", "error"
            ));
        }
    }

    /**
     * Manually trigger the password policy extension job.
     * This endpoint should be protected and only accessible by administrators.
     * 
     * @return ResponseEntity with operation result
     */
    @PostMapping("/extend-expired")
    public ResponseEntity<Map<String, Object>> triggerPolicyExtension() {
        logger.info().message(AppConstants.INFO_CONTROLLER_POLICY_EXTENSION_TRIGGERED).log();
        
        try {
            passwordPolicyExpirationService.manualExtendExpiredPolicies();
            
            return ResponseEntity.ok(Map.of(
                "message", AppConstants.SUCCESS_CONTROLLER_POLICY_EXTENSION_TRIGGERED,
                "status", "success"
            ));
            
        } catch (Exception ex) {
            logger.error(ex).message(AppConstants.ERROR_CONTROLLER_TRIGGER_POLICY_EXTENSION).log();
            return ResponseEntity.internalServerError().body(Map.of(
                "error", AppConstants.ERROR_CONTROLLER_POLICY_EXTENSION_FAILED,
                "status", "error"
            ));
        }
    }

    /**
     * Health check endpoint for the password policy expiration service.
     * 
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", AppConstants.SERVICE_NAME_PASSWORD_POLICY_EXPIRATION,
            "status", "healthy",
            "message", AppConstants.SUCCESS_CONTROLLER_POLICY_SERVICE_RUNNING
        ));
    }
}
