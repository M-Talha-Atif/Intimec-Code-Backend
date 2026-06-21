package com.aidentx.access_portal_service.service;

import com.aidentx.access_portal_service.dto.request.access_request.SubmitAccessRequestDTO;
import com.aidentx.access_portal_service.dto.request.access_request.WithdrawRequestDTO;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestHistoryResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.RequestDetailsResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.RequestLifecycleDTO;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Service interface for access request operations.
 * Handles request submission, retrieval, and lifecycle management.
 */
public interface AccessRequestService {

    /**
     * Submits a new access request from cart items.
     *
     * @param tenantId tenant identifier
     * @param userId user submitting the request
     * @param requestDTO request submission data
     * @return submitted request response
     */
    AccessRequestResponseDTO submitRequest(
            String tenantId,
            UUID userId,
            SubmitAccessRequestDTO requestDTO
    );

    /**
     * Retrieves paginated access request history for a user.
     *
     * @param tenantId tenant identifier
     * @param userId user whose requests to retrieve
     * @param status optional status filter
     * @param page page number (0-based)
     * @param size page size
     * @return paginated request history
     */
    Page<AccessRequestHistoryResponseDTO> getRequestHistory(
            String tenantId,
            UUID userId,
            RequestStatus status,
            int page,
            int size
    );

    /**
     * Retrieves detailed information for a specific access request.
     * Includes items, approval workflow, comments, and audit trail.
     *
     * @param tenantId tenant identifier
     * @param userId user requesting details (for authorization)
     * @param requestId request identifier
     * @return detailed request information
     */
    RequestDetailsResponseDTO getRequestDetails(
            String tenantId,
            UUID userId,
            UUID requestId
    );

    /**
     * Retrieves the complete lifecycle timeline for a request.
     * Shows all events from submission to completion/rejection.
     *
     * @param tenantId tenant identifier
     * @param userId user requesting lifecycle (for authorization)
     * @param requestId request identifier
     * @return request lifecycle information
     */
    RequestLifecycleDTO getRequestLifecycle(
            String tenantId,
            UUID userId,
            UUID requestId
    );


    /**
     * Withdraws an access request.
     * Only requests in SUBMITTED or PENDING_APPROVAL status can be withdrawn.
     * Cart items associated with the request are set back to ACTIVE status.
     *
     * @param tenantId tenant identifier
     * @param userId user withdrawing the request (must be the requester)
     * @param requestId request identifier
     * @param withdrawDTO withdrawal details including reason
     * @return updated request response
     * @throws IllegalStateException if request cannot be withdrawn (wrong status)
     * @throws SecurityException if user is not authorized to withdraw
     */
    AccessRequestResponseDTO withdrawRequest(
            String tenantId,
            UUID userId,
            UUID requestId,
            WithdrawRequestDTO withdrawDTO
    );
}