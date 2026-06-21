package com.aidentx.authservice.controllers;

import com.aidentx.authservice.exception.PolicyAlreadyExistsException;
import com.aidentx.authservice.exception.PolicyNotFoundException;
import com.aidentx.authservice.model.dto.AccountLockPolicyRequest;
import com.aidentx.authservice.model.dto.AccountLockPolicyResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.service.Policy.AccountLockPolicyService;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account lock policy management endpoints.
 * <p>
 * Handles CRUD operations for account lock policies with automatic tenant extraction
 * from JWT tokens. Each tenant can have only one account lock policy.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/account-lock-policy")
public class AccountLockPolicyController {

    private final IUnifiedLogger logger;
    private final AccountLockPolicyService accountLockPolicyService;
    private final JwtUtil jwtUtil;

    public AccountLockPolicyController(UnifiedLoggerFactory unifiedLoggerFactory,
                                      AccountLockPolicyService accountLockPolicyService,
                                      JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("AccountLockPolicyController", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.accountLockPolicyService = accountLockPolicyService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates a new account lock policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * Each tenant can have only one account lock policy.
     * </p>
     *
     * @param request the account lock policy creation request
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with status and message
     */
    @PostMapping
    public ResponseEntity<StatusMessageResponse> createAccountLockPolicy(
            @Valid @RequestBody AccountLockPolicyRequest request,
            HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        
        logger.trace().message(String.format(AppConstants.REST_CREATE_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            StatusMessageResponse response = accountLockPolicyService.createAccountLockPolicy(tenantId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PolicyAlreadyExistsException ex) {
            logger.warn().message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_ALREADY_EXISTS, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new StatusMessageResponse(409, ex.getMessage()));
        }
    }

    /**
     * Updates the existing account lock policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * The policy must exist for the tenant.
     * </p>
     *
     * @param request the account lock policy update request
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with status and message
     */
    @PutMapping
    public ResponseEntity<StatusMessageResponse> updateAccountLockPolicy(
            @Valid @RequestBody AccountLockPolicyRequest request,
            HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_UPDATE_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            StatusMessageResponse response = accountLockPolicyService.updateAccountLockPolicy(tenantId, request);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException ex) {
            logger.warn().message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StatusMessageResponse(404, ex.getMessage()));
        }
    }

    /**
     * Retrieves the account lock policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * Returns the tenant's account lock policy if it exists.
     * </p>
     *
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with the account lock policy
     */
    @GetMapping
    public ResponseEntity<AccountLockPolicyResponse> getAccountLockPolicy(HttpServletRequest httpRequest) {
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_GET_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            AccountLockPolicyResponse response = accountLockPolicyService.getAccountLockPolicy(tenantId);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException ex) {
            logger.warn().message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Deletes the account lock policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * Removes the tenant's account lock policy if it exists.
     * </p>
     *
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with status and message
     */
    @DeleteMapping
    public ResponseEntity<StatusMessageResponse> deleteAccountLockPolicy(HttpServletRequest httpRequest) {
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_DELETE_ACCOUNT_LOCK_POLICY, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            StatusMessageResponse response = accountLockPolicyService.deleteAccountLockPolicy(tenantId);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException ex) {
            logger.warn().message(String.format(AppConstants.ACCOUNT_LOCK_POLICY_NOT_FOUND, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StatusMessageResponse(404, ex.getMessage()));
        }
    }

    /**
     * Extracts tenant ID from JWT token in the Authorization header.
     * 
     * @param request the HTTP request
     * @return the tenant ID from the JWT token
     * @throws RuntimeException if token is invalid or missing
     */
    private String extractTenantIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error().message(AppConstants.MISSING_OR_INVALID_AUTH_HEADER )
                    .detail("authHeader", authHeader).log();
            throw new RuntimeException(AppConstants.EXPIRED_PASSWORD_INVALID_TOKEN_MSG);
        }

        String token = authHeader.substring(7);
        String tenantId = jwtUtil.extractTenantId(token);
        
        if (tenantId == null || "unknown".equals(tenantId)) {
            logger.error().message(AppConstants.FAILED_TO_EXTRACT_TENANT_ID_FROM_JWT)
                    .detail("tokenPrefix", token.substring(0, Math.min(10, token.length()))).log();
            throw new RuntimeException(AppConstants.EXPIRED_PASSWORD_TOKEN_EXTRACTION_ERROR_MSG);
        }

        return tenantId;
    }
}
