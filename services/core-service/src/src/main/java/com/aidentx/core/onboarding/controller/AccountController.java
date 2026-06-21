package com.aidentx.core.onboarding.controller;

import com.aidentx.core.onboarding.service.AccountService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for account (shadow) data associated with applications.
 *
 * <p>This controller exposes shadow data as "accounts" for the frontend,
 * following the IGA pattern where shadows are the persistent representation
 * of resource objects (accounts) discovered during connector import tasks.</p>
 *
 * <p>The data flow is:
 * Connector → Shadow Sync Service → Shadow Repository → This Controller → Frontend</p>
 */
@RestController
@RequestMapping("/api/v1/applications")
public class AccountController {

    private final AccountService accountService;
    private final IUnifiedLogger logger;

    public AccountController(AccountService accountService, UnifiedLoggerFactory loggerFactory) {
        this.accountService = accountService;
        this.logger = loggerFactory.getLogger(AccountController.class);
    }

    /**
     * GET /api/v1/applications/{appId}/accounts
     *
     * <p>Retrieves accounts (shadows) for a specific application, with optional
     * filtering and pagination. Returns data in the format expected by the frontend
     * AccountWithUserAndEntitlements type.</p>
     *
     * @param tenantId The tenant ID from the request header
     * @param appId    The application (resource) ID
     * @param page     Page number (0-based, default 0)
     * @param size     Page size (default 10)
     * @param sort     Sort specification
     * @return Paginated list of accounts with user and entitlement metadata
     */
    @GetMapping("/{appId}/accounts")
    public ResponseEntity<Map<String, Object>> getAccounts(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String situation,
            @RequestParam(required = false) String search) {
        try {
            logger.info().message("Fetching accounts for application")
                    .detail("appId", appId)
                    .detail("tenantId", tenantId)
                    .detail("page", page)
                    .detail("size", size)
                    .log();

            Map<String, Object> response = accountService.getAccountsForApplication(
                    tenantId, appId.toString(), page, size, situation, search);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(e).message("Failed to fetch accounts for application")
                    .detail("appId", appId)
                    .log();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch accounts: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/applications/{appId}/accounts/filters
     *
     * <p>Returns available filter options for accounts of a specific application.</p>
     */
    @GetMapping("/{appId}/accounts/filters")
    public ResponseEntity<Map<String, Object>> getAccountFilters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID appId) {
        try {
            Map<String, Object> filters = accountService.getAccountFilters(tenantId, appId.toString());
            return ResponseEntity.ok(filters);
        } catch (Exception e) {
            logger.error(e).message("Failed to fetch account filters")
                    .detail("appId", appId)
                    .log();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch filters: " + e.getMessage()));
        }
    }
}
