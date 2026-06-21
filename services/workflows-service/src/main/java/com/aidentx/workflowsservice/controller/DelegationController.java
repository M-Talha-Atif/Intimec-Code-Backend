package com.aidentx.workflowsservice.controller;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.dto.request.CreateDelegationRequest;
import com.aidentx.workflowsservice.dto.request.UpdateDelegationRequest;
import com.aidentx.workflowsservice.dto.response.ApprovalDelegationDTO;
import com.aidentx.workflowsservice.entity.ApprovalDelegation;
import com.aidentx.workflowsservice.util.JwtUtils;
import com.aidentx.workflowsservice.service.ApprovalDelegationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * REST Controller for Approval Delegations
 * 
 * Provides endpoints for managing time-based delegation rules that allow users
 * to automatically delegate their approval responsibilities to other users
 * for specified time periods.
 * 
 * Key Features:
 * - Create time-based delegations with start/end dates
 * - Scope delegations to all workflows, workflow types, or specific workflows
 * - Configure notification preferences for delegator and delegate
 * - Query active delegations for users and tenants
 * 
 * @author AidentX Development Team
 * @since 2.0 (Phase 3: Delegation Enhancement)
 */
@RestController
@RequestMapping("/api/v1/delegations")
@Tag(name = "Approval Delegations", description = "Manage time-based approval delegation rules")
public class DelegationController {

    private final IUnifiedLogger logger;
    private final ApprovalDelegationService delegationService;
    private final JwtUtils jwtUtils;

    public DelegationController(
            ApprovalDelegationService delegationService,
            JwtUtils jwtUtils,
            UnifiedLoggerFactory loggerFactory) {
        this.delegationService = delegationService;
        this.jwtUtils = jwtUtils;
        this.logger = loggerFactory.getLogger(DelegationController.class, LOG_CATEGORY_WORKFLOW_CONTROLLER);
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create a new delegation rule
     * 
     * The delegation will automatically route approval requests from the
     * authenticated user to the specified delegate during the active period.
     */
    @PostMapping
    @Operation(
        summary = "Create a new delegation",
        description = "Create a time-based delegation rule to automatically route your approvals to another user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Delegation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation error or overlapping delegation"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApprovalDelegationDTO> createDelegation(
            @Valid @RequestBody CreateDelegationRequest request,
            HttpServletRequest httpRequest) {
        
        String tenantId = jwtUtils.extractTenantId(httpRequest);
        UUID currentUserId = UUID.fromString(jwtUtils.extractUserId(httpRequest));

        logger.info()
            .message(LOG_CREATING_DELEGATION)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail(DETAIL_KEY_FROM_USER_ID, currentUserId.toString())
            .detail(DETAIL_KEY_TO_USER_ID, request.getToUserId().toString())
            .detail(DETAIL_KEY_START_DATE, request.getStartDate().toString())
            .detail(DETAIL_KEY_END_DATE, request.getEndDate().toString())
            .log();

        ApprovalDelegation delegation = delegationService.createDelegation(
            currentUserId,
            request.getToUserId(),
            request.getStartDate(),
            request.getEndDate(),
            request.getReason(),
            request.getScope(),
            request.getWorkflowIdsAsString(),
            request.getNotifyDelegate(),
            request.getNotifyDelegator(),
            currentUserId,
            tenantId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApprovalDelegationDTO.from(delegation));
    }

    /**
     * Get a specific delegation by ID
     */
    @GetMapping("/{delegationId}")
    @Operation(summary = "Get a delegation by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delegation found"),
        @ApiResponse(responseCode = "404", description = "Delegation not found")
    })
    public ResponseEntity<ApprovalDelegationDTO> getDelegation(
            @PathVariable @Parameter(description = "Delegation ID") UUID delegationId) {
        
        logger.info()
            .message(LOG_GETTING_DELEGATION)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_DELEGATION)
            .objectId(delegationId.toString())
            .log();

        Optional<ApprovalDelegation> delegation = delegationService.getDelegation(delegationId);
        
        return delegation
            .map(d -> ResponseEntity.ok(ApprovalDelegationDTO.from(d)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing delegation
     */
    @PutMapping("/{delegationId}")
    @Operation(summary = "Update a delegation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delegation updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation error"),
        @ApiResponse(responseCode = "404", description = "Delegation not found")
    })
    public ResponseEntity<ApprovalDelegationDTO> updateDelegation(
            @PathVariable @Parameter(description = "Delegation ID") UUID delegationId,
            @Valid @RequestBody UpdateDelegationRequest request,
            HttpServletRequest httpRequest) {
        
        UUID currentUserId = UUID.fromString(jwtUtils.extractUserId(httpRequest));

        logger.info()
            .message(LOG_DELEGATION_UPDATED)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_DELEGATION)
            .objectId(delegationId.toString())
            .log();

        try {
            ApprovalDelegation delegation = delegationService.updateDelegation(
                delegationId,
                request.getStartDate(),
                request.getEndDate(),
                request.getReason(),
                request.getScope(),
                request.getWorkflowIdsAsString(),
                request.getNotifyDelegate(),
                request.getNotifyDelegator(),
                currentUserId
            );

            return ResponseEntity.ok(ApprovalDelegationDTO.from(delegation));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Deactivate (soft delete) a delegation
     */
    @DeleteMapping("/{delegationId}")
    @Operation(summary = "Deactivate a delegation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delegation deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Delegation not found")
    })
    public ResponseEntity<ApprovalDelegationDTO> deactivateDelegation(
            @PathVariable @Parameter(description = "Delegation ID") UUID delegationId,
            HttpServletRequest httpRequest) {
        
        UUID currentUserId = UUID.fromString(jwtUtils.extractUserId(httpRequest));

        logger.info()
            .message(LOG_DELEGATION_DEACTIVATED)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .objectType(OBJECT_TYPE_DELEGATION)
            .objectId(delegationId.toString())
            .log();

        try {
            ApprovalDelegation delegation = delegationService.deactivateDelegation(delegationId, currentUserId);
            return ResponseEntity.ok(ApprovalDelegationDTO.from(delegation));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Get my delegations (as delegator)
     * Returns all delegations created by the current user
     */
    @GetMapping("/my-delegations")
    @Operation(
        summary = "Get my delegations",
        description = "Get all delegations where you are the delegator (your approvals being delegated to others)"
    )
    public ResponseEntity<List<ApprovalDelegationDTO>> getMyDelegations(HttpServletRequest httpRequest) {
        UUID currentUserId = UUID.fromString(jwtUtils.extractUserId(httpRequest));

        logger.info()
            .message(LOG_GETTING_USER_DELEGATIONS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail(DETAIL_KEY_FROM_USER_ID, currentUserId.toString())
            .log();

        List<ApprovalDelegation> delegations = delegationService.getDelegationsForUser(currentUserId);
        return ResponseEntity.ok(ApprovalDelegationDTO.fromList(delegations));
    }

    /**
     * Get delegations to me (as delegate)
     * Returns all delegations where the current user is the delegate
     */
    @GetMapping("/delegated-to-me")
    @Operation(
        summary = "Get delegations to me",
        description = "Get all delegations where you are the delegate (receiving others' approvals)"
    )
    public ResponseEntity<List<ApprovalDelegationDTO>> getDelegationsToMe(HttpServletRequest httpRequest) {
        UUID currentUserId = UUID.fromString(jwtUtils.extractUserId(httpRequest));

        logger.info()
            .message(LOG_GETTING_DELEGATIONS_TO_USER)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail(DETAIL_KEY_TO_USER_ID, currentUserId.toString())
            .log();

        List<ApprovalDelegation> delegations = delegationService.getDelegationsToUser(currentUserId);
        return ResponseEntity.ok(ApprovalDelegationDTO.fromList(delegations));
    }

    /**
     * Get all active delegations for the tenant (admin view)
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get all active delegations",
        description = "Get all currently active delegations in the tenant (admin only)"
    )
    public ResponseEntity<List<ApprovalDelegationDTO>> getAllActiveDelegations(HttpServletRequest httpRequest) {
        String tenantId = jwtUtils.extractTenantId(httpRequest);

        logger.info()
            .message(LOG_GETTING_ALL_ACTIVE_DELEGATIONS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail(DETAIL_KEY_TENANT_ID, tenantId)
            .log();

        List<ApprovalDelegation> delegations = delegationService.getAllActiveDelegations(tenantId);
        return ResponseEntity.ok(ApprovalDelegationDTO.fromList(delegations));
    }

    /**
     * Check if a user has an active delegation
     */
    @GetMapping("/check/{userId}")
    @Operation(
        summary = "Check active delegation",
        description = "Check if a specific user has any active delegation"
    )
    public ResponseEntity<DelegationCheckResponse> checkActiveDelegation(
            @PathVariable @Parameter(description = "User ID to check") UUID userId,
            HttpServletRequest httpRequest) {
        
        String tenantId = jwtUtils.extractTenantId(httpRequest);

        boolean hasActive = delegationService.hasActiveDelegation(userId, tenantId);
        
        Optional<UUID> delegate = hasActive 
            ? delegationService.findEffectiveDelegate(userId, tenantId)
            : Optional.empty();

        return ResponseEntity.ok(new DelegationCheckResponse(
            userId,
            hasActive,
            delegate.orElse(null)
        ));
    }

    // ==================== ADMIN OPERATIONS ====================

    /**
     * Get delegations for a specific user (admin)
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user's delegations",
        description = "Get all delegations for a specific user (admin only)"
    )
    public ResponseEntity<List<ApprovalDelegationDTO>> getDelegationsForUser(
            @PathVariable @Parameter(description = "User ID") UUID userId) {
        
        logger.info()
            .message(LOG_GETTING_USER_DELEGATIONS)
            .category(LOG_CATEGORY_WORKFLOW_CONTROLLER)
            .detail(DETAIL_KEY_FROM_USER_ID, userId.toString())
            .log();

        List<ApprovalDelegation> delegations = delegationService.getDelegationsForUser(userId);
        return ResponseEntity.ok(ApprovalDelegationDTO.fromList(delegations));
    }

    // ==================== RESPONSE CLASSES ====================

    /**
     * Response for delegation check endpoint
     */
    public record DelegationCheckResponse(
        UUID userId,
        boolean hasActiveDelegation,
        UUID delegateTo
    ) {}
}
