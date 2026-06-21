package com.aidentx.access_portal_service.controller;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.access_request.SubmitAccessRequestDTO;
import com.aidentx.access_portal_service.dto.request.access_request.WithdrawRequestDTO;
import com.aidentx.access_portal_service.dto.response.ApiResponse;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestHistoryResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.RequestDetailsResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.RequestLifecycleDTO;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import com.aidentx.access_portal_service.service.AccessRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_TENANT_ID;
import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_USER_ID;
import static com.aidentx.access_portal_service.util.RequestContextUtil.resolveUserId;

/**
 * REST controller for access request operations.
 * Handles request submission, history retrieval, and detailed request information.
 */
@RestController
@RequestMapping(AccessPortalConstants.API_BASE_ACCESS_REQUEST)
@RequiredArgsConstructor
@Tag(
        name = AccessPortalConstants.TAG_ACCESS_REQUEST_API,
        description = AccessPortalConstants.TAG_ACCESS_REQUEST_API_DESC
)
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    /**
     * Submits a new access request from cart items.
     *
     * @param tenantIdHeader tenant identifier from header
     * @param userIdHeader user identifier from header
     * @param requestDTO request submission data
     * @return created request response
     */
    @PostMapping
    @Operation(
            summary = AccessPortalConstants.OP_SUBMIT_ACCESS_REQUEST,
            description = AccessPortalConstants.OP_SUBMIT_ACCESS_REQUEST_DESC
    )
    public ResponseEntity<ApiResponse<AccessRequestResponseDTO>> submitAccessRequest(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantIdHeader,
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdHeader,
            @Valid @RequestBody SubmitAccessRequestDTO requestDTO
    ) {
        UUID userId = resolveUserId(userIdHeader);
        AccessRequestResponseDTO response =
                accessRequestService.submitRequest(tenantIdHeader, userId, requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                AccessPortalConstants.SUCCESS_ACCESS_REQUEST_SUBMITTED,
                                response
                        )
                );
    }

    /**
     * Retrieves paginated access request history for the current user.
     *
     * @param tenantId tenant identifier
     * @param userIdHeader user identifier from header
     * @param page page number (0-based)
     * @param size page size
     * @param status optional status filter
     * @return paginated request history
     */
    @GetMapping("/history")
    @Operation(
            summary = "Fetch access request history",
            description = "Retrieves paginated list of access requests for the current user with optional status filtering"
    )
    public ResponseEntity<ApiResponse<Page<AccessRequestHistoryResponseDTO>>> getAccessRequestHistory(
            @RequestHeader(HEADER_TENANT_ID) String tenantId,
            @RequestHeader(HEADER_USER_ID) String userIdHeader,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Optional status filter")
            @RequestParam(required = false) RequestStatus status
    ) {
        UUID userId = resolveUserId(userIdHeader);
        Page<AccessRequestHistoryResponseDTO> response = accessRequestService.getRequestHistory(
                tenantId, userId, status, page, size
        );

        return ResponseEntity.ok(
                ApiResponse.success("Access request history fetched", response)
        );
    }

    /**
     * Retrieves detailed information for a specific access request.
     * Includes items, approval workflow, comments, and audit trail.
     *
     * @param tenantId tenant identifier
     * @param userIdHeader user identifier from header
     * @param requestId request identifier
     * @return detailed request information
     */
    @GetMapping("/{requestId}/details")
    @Operation(
            summary = "Get access request details",
            description = "Retrieves comprehensive details for a specific access request including items, approval steps, comments, and audit log"
    )
    public ResponseEntity<ApiResponse<RequestDetailsResponseDTO>> getRequestDetails(
            @RequestHeader(HEADER_TENANT_ID) String tenantId,
            @RequestHeader(HEADER_USER_ID) String userIdHeader,
            @Parameter(description = "Request identifier")
            @PathVariable UUID requestId
    ) {
        UUID userId = resolveUserId(userIdHeader);
        RequestDetailsResponseDTO response = accessRequestService.getRequestDetails(
                tenantId, userId, requestId
        );

        return ResponseEntity.ok(
                ApiResponse.success("Access request details fetched", response)
        );
    }

    /**
     * Retrieves the complete lifecycle timeline for a request.
     * Shows all events from submission to completion or rejection.
     *
     * @param tenantId tenant identifier
     * @param userIdHeader user identifier from header
     * @param requestId request identifier
     * @return request lifecycle information
     */
    @GetMapping("/{requestId}/lifecycle")
    @Operation(
            summary = "Get access request lifecycle",
            description = "Retrieves complete timeline of events for a specific access request showing the full approval and provisioning workflow"
    )
    public ResponseEntity<ApiResponse<RequestLifecycleDTO>> getRequestLifecycle(
            @RequestHeader(HEADER_TENANT_ID) String tenantId,
            @RequestHeader(HEADER_USER_ID) String userIdHeader,
            @Parameter(description = "Request identifier")
            @PathVariable UUID requestId
    ) {
        UUID userId = resolveUserId(userIdHeader);
        RequestLifecycleDTO response = accessRequestService.getRequestLifecycle(
                tenantId, userId, requestId
        );

        return ResponseEntity.ok(
                ApiResponse.success("Access request lifecycle fetched", response)
        );
    }

    /**
     * Withdraws an access request.
     * Only requests in SUBMITTED or PENDING_APPROVAL status can be withdrawn.
     *
     * @param tenantId tenant identifier
     * @param userIdHeader user identifier from header
     * @param requestId request identifier
     * @param withdrawDTO withdrawal details including reason
     * @return updated request response
     */
    @PostMapping("/{requestId}/withdraw")
    @Operation(
            summary = "Withdraw access request",
            description = "Withdraws a pending access request and reactivates cart items. Only SUBMITTED or PENDING_APPROVAL requests can be withdrawn."
    )
    public ResponseEntity<ApiResponse<AccessRequestResponseDTO>> withdrawRequest(
            @RequestHeader(HEADER_TENANT_ID) String tenantId,
            @RequestHeader(HEADER_USER_ID) String userIdHeader,
            @Parameter(description = "Request identifier")
            @PathVariable UUID requestId,
            @Valid @RequestBody WithdrawRequestDTO withdrawDTO
    ) {
        UUID userId = resolveUserId(userIdHeader);

        AccessRequestResponseDTO response = accessRequestService.withdrawRequest(
                tenantId, userId, requestId, withdrawDTO
        );

        return ResponseEntity.ok(
                ApiResponse.success("Access request withdrawn successfully", response)
        );
    }
}