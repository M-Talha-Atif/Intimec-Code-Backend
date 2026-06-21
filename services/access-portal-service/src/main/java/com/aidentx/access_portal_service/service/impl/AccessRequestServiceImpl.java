package com.aidentx.access_portal_service.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.access_portal_service.client.UserProfileRestClient;
import com.aidentx.access_portal_service.client.UserRoleRestClient;
import com.aidentx.access_portal_service.client.grpc.UserIdentitiesGrpcClient;
import com.aidentx.access_portal_service.client.grpc.UserManagerLookupGrpcClient;
import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.request.access_request.SubmitAccessRequestDTO;
import com.aidentx.access_portal_service.dto.request.access_request.WithdrawRequestDTO;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestHistoryResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.AccessRequestResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.RequestDetailsResponseDTO;
import com.aidentx.access_portal_service.dto.response.access_request.RequestLifecycleDTO;
import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.entity.access_request.AccessRequestItem;
import com.aidentx.access_portal_service.entity.access_request.AuditAction;
import com.aidentx.access_portal_service.entity.access_request.CommentType;
import com.aidentx.access_portal_service.entity.access_request.RequestApprovalStep;
import com.aidentx.access_portal_service.entity.access_request.RequestAuditLog;
import com.aidentx.access_portal_service.entity.access_request.RequestComment;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import com.aidentx.access_portal_service.entity.access_request.RequestTarget;
import com.aidentx.access_portal_service.entity.access_request.UrgencyLevel;
import com.aidentx.access_portal_service.entity.cart.CartItem;
import com.aidentx.access_portal_service.entity.cart.CartStatus;
import com.aidentx.access_portal_service.event.AccessRequestEventPublisher;
import com.aidentx.access_portal_service.mapper.AccessRequestMapper;
import com.aidentx.access_portal_service.model.RoleAssignmentDetail;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestRepository;
import com.aidentx.access_portal_service.repository.access_request.RequestApprovalStepRepository;
import com.aidentx.access_portal_service.repository.cart.CartRepository;
import com.aidentx.access_portal_service.service.AccessRequestService;
import com.aidentx.access_portal_service.service.enrichment.ItemNameResolver;
import com.aidentx.access_portal_service.validation.access_request.AccessRequestValidator;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class AccessRequestServiceImpl implements AccessRequestService {

    private final CartRepository cartRepository;
    private final AccessRequestRepository requestRepository;
    private final AccessRequestValidator validator;
    private final AccessRequestMapper mapper;
    private final UserProfileRestClient userProfileRestClient;
    private final RequestApprovalStepRepository approvalStepRepository;
    private final IUnifiedLogger logger;
    private final ItemNameResolver itemNameResolver;
    private final UserRoleRestClient userRoleRestClient;
    private final AccessRequestEventPublisher eventPublisher;
    private final UserIdentitiesGrpcClient userIdentitiesGrpcClient;
    private final UserManagerLookupGrpcClient userManagerLookupGrpcClient;

    @Autowired
    public AccessRequestServiceImpl(
            CartRepository cartRepository,
            AccessRequestRepository requestRepository,
            RequestApprovalStepRepository approvalStepRepository,
            AccessRequestValidator validator,
            AccessRequestMapper mapper,
            UserProfileRestClient userProfileRestClient,
            UnifiedLoggerFactory loggerFactory,
            UserRoleRestClient userRoleRestClient,
            AccessRequestEventPublisher eventPublisher,
            UserIdentitiesGrpcClient userIdentitiesGrpcClient,
            UserManagerLookupGrpcClient userManagerLookupGrpcClient,
            ItemNameResolver itemNameResolver
    ) {
        this.cartRepository = cartRepository;
        this.requestRepository = requestRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.validator = validator;
        this.mapper = mapper;
        this.userProfileRestClient = userProfileRestClient;
        this.userRoleRestClient = userRoleRestClient;
        this.eventPublisher = eventPublisher;
        this.itemNameResolver = itemNameResolver;
        this.userIdentitiesGrpcClient = userIdentitiesGrpcClient;
        this.userManagerLookupGrpcClient = userManagerLookupGrpcClient;
        this.logger = loggerFactory.getLogger(
                getClass(),
                AccessPortalConstants.LOG_CATEGORY_ACCESS_REQUEST_SERVICE
        );
    }


    @Override
    public AccessRequestResponseDTO submitRequest(
            String tenantId,
            UUID userId,
            SubmitAccessRequestDTO dto
    ) {
        logger.info()
                .message("Submitting access request")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("itemCount", String.valueOf(dto.getItems().size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_SUBMIT_START)
                .log();

        // Step 1: Fetch and validate cart items
        List<CartItem> cartItems = cartRepository.findAllByUserIdAndTenantIdAndStatus(
                userId,
                tenantId,
                CartStatus.ACTIVE
        );

        logger.debug()
                .message("Fetched active cart items for access request")
                .detail("userId", userId.toString())
                .detail("cartItemCount", String.valueOf(cartItems.size()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_CART_ITEMS_FETCHED)
                .log();

        Map<UUID, SubmitAccessRequestDTO.RequestItemDTO> itemDTOMap =
                validateAndBuildItemMap(dto, cartItems);

        // Step 2: Fetch user display name
        String requestedByUserName = fetchUserDisplayName();

        logger.debug()
                .message("Resolved requester display name")
                .detail("displayName", requestedByUserName)
                .log();

        // Step 3: Create access request entity
        AccessRequest request = createAccessRequest(userId, dto, requestedByUserName, tenantId);

        // Step 4: Build access request items (without workflow logic)
        UUID workflowId = null;
        UUID tileId = null;
        String tileTitle = null;

        for (CartItem cartItem : cartItems) {
            SubmitAccessRequestDTO.RequestItemDTO itemDTO = itemDTOMap.get(cartItem.getId());
            if (itemDTO == null) {
                throw new IllegalStateException("Item DTO not found for cart item: " + cartItem.getId());
            }

            // Build access request item
            AccessRequestItem item = buildAccessRequestItem(
                    cartItem, itemDTO, request, tenantId, userId
            );
            request.getItems().add(item);

            // Capture workflow info (same for all items in this request)
            if (workflowId == null && cartItem.getTile().isRequireManagerApproval() &&
                    cartItem.getTile().getWorkflowId() != null &&
                    "INTERNAL_ROLE".equals(cartItem.getItemType())) {
                workflowId = cartItem.getTile().getWorkflowId();
                tileId = cartItem.getTile().getId();
                tileTitle = cartItem.getTile().getTitle();
            }
        }

        // Step 5: Save request (generates IDs for request AND items)
        requestRepository.save(request);

        // Step 6: Create audit log entry
        createAuditLog(request, userId, requestedByUserName, tenantId);

        // Step 7: Build workflow map AFTER save (now we have item IDs)
        Map<UUID, ItemWorkflowDetail> itemWorkflowMap = new HashMap<>();

        for (AccessRequestItem item : request.getItems()) {
            // Find corresponding cart item
            CartItem cartItem = cartItems.stream()
                    .filter(ci -> ci.getItemId().equals(item.getItemId()) &&
                            ci.getItemName().equals(item.getItemName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Cart item not found for request item: " + item.getId()));

            // Only include items that require approval workflow
            if (cartItem.getTile().isRequireManagerApproval() &&
                    cartItem.getTile().getWorkflowId() != null &&
                    "INTERNAL_ROLE".equals(cartItem.getItemType())) {


                // Build list of target users for this item
                List<UUID> targetUsersForItem = new ArrayList<>();
                List<String> targetUserNamesForItem = new ArrayList<>();

                if (item.getRequestTarget() == RequestTarget.SELF) {
                    // For SELF requests, target user is the requester
                    targetUsersForItem.add(userId);
                    targetUserNamesForItem.add(requestedByUserName);
                } else {

                    // For OTHER requests, target user is the cart item owner
                    if (item.getBeneficiaryUserIds() != null &&
                            !item.getBeneficiaryUserIds().isEmpty()) {
                        List<UUID> beneficiaryIds = parseBeneficiaryUserIds(
                                item.getBeneficiaryUserIds()
                        );
                        targetUsersForItem.addAll(beneficiaryIds);
                    }

                    // Get beneficiary names
                    if (item.getBeneficiaryNames() != null &&
                            !item.getBeneficiaryNames().isEmpty()) {
                        String[] names = item.getBeneficiaryNames().split(",");
                        for (String name : names) {
                            String trimmed = name.trim();
                            if (!trimmed.isEmpty()) {
                                targetUserNamesForItem.add(trimmed);
                            }
                        }
                    }
                }


                // Create workflow detail for this ITEM (not role)
                ItemWorkflowDetail workflowDetail = new ItemWorkflowDetail(
                        item.getId(),           // Use item DB ID
                        item.getItemId(),       // roleId
                        item.getItemName(),     // roleName
                        targetUsersForItem,
                        targetUserNamesForItem,
                        item.getJustification(),
                        item.getValidityDays()
                );

                itemWorkflowMap.put(item.getId(), workflowDetail); // Key by item DB ID

                logger.trace()
                        .message("Added item to workflow map")
                        .detail("itemId", item.getId().toString())
                        .detail("roleId", item.getItemId().toString())
                        .detail("roleName", item.getItemName())
                        .detail("targetUserCount", String.valueOf(targetUsersForItem.size()))
                        .log();
            }
        }

        // Step 8: Publish workflow events (one per ITEM, not per role)
        if (!itemWorkflowMap.isEmpty() && workflowId != null) {
            publishWorkflowEventsPerItem(
                    request,
                    itemWorkflowMap,
                    tenantId,
                    workflowId,
                    tileId,
                    tileTitle,
                    requestedByUserName,
                    userId,
                    dto.getOverallJustification()
            );
        }

        logger.info()
                .message("Access request submitted successfully")
                .detail("requestId", request.getId().toString())
                .detail("userId", userId.toString())
                .detail("requestedByUserName", requestedByUserName)
                .detail("itemCount", String.valueOf(request.getItems().size()))
                .detail("workflowEventsPublished", !itemWorkflowMap.isEmpty())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_SUBMIT_SUCCESS)
                .log();

        return mapper.toResponse(request);
    }

    /**
     * Creates AccessRequest entity.
     */
    private AccessRequest createAccessRequest(
            UUID userId,
            SubmitAccessRequestDTO dto,
            String requestedByUserName,
            String tenantId
    ) {
        AccessRequest request = new AccessRequest();
        request.setRequestedByUserId(userId);
        request.setOverallJustification(dto.getOverallJustification());
        request.setStatus(RequestStatus.SUBMITTED);
        request.setRequestedAt(OffsetDateTime.now());
        request.setRequestedByUserName(requestedByUserName);
        request.setTenantId(tenantId);
        return request;
    }

    /**
     * Creates audit log entry for request submission.
     */
    private void createAuditLog(
            AccessRequest request,
            UUID userId,
            String requestedByUserName,
            String tenantId
    ) {
        RequestAuditLog auditLog = new RequestAuditLog();
        auditLog.setAccessRequest(request);
        auditLog.setAction(AuditAction.REQUEST_SUBMITTED);
        auditLog.setPerformedBy(requestedByUserName);
        auditLog.setTimestamp(OffsetDateTime.now());
        auditLog.setPreviousStatus(null);
        auditLog.setNewStatus(RequestStatus.SUBMITTED.name());
        auditLog.setDetails("Access request submitted with " + request.getItems().size() + " items");
        auditLog.setTenantId(tenantId);
        request.getAuditLogs().add(auditLog);

        // Save again to persist audit log
        requestRepository.save(request);
    }


    /**
     * Returns true when the given item type should trigger the manager approval
     * workflow (currently INTERNAL_ROLE and BUSINESS_ROLE). Centralized here so
     * the workflow gates in {@code submitRequest} stay consistent.
     */
    private boolean isApprovableRoleType(String itemType) {
        return AccessPortalConstants.ITEM_TYPE_INTERNAL_ROLE.equals(itemType)
                || AccessPortalConstants.ITEM_TYPE_BUSINESS_ROLE.equals(itemType);
    }

    /**
     * Fetches role owners from user-role service for approval workflow.
     */
    private List<UUID> fetchRoleOwners(UUID roleId, String tenantId) {
        try {
            Map<String, Object> roleData = userRoleRestClient.getRoleById(roleId);
            if (roleData != null && roleData.containsKey("ownersData")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ownersData =
                        (List<Map<String, Object>>) roleData.get("ownersData");

                return ownersData.stream()
                        .map(owner -> UUID.fromString((String) owner.get("id")))
                        .collect(Collectors.toList());
            }

            logger.warn()
                    .message("No role owners found for role")
                    .detail("roleId", roleId.toString())
                    .log();

            return Collections.emptyList();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to fetch role owners")
                    .detail("roleId", roleId.toString())
                    .log();
            return Collections.emptyList();
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<AccessRequestHistoryResponseDTO> getRequestHistory(
            String tenantId,
            UUID userId,
            RequestStatus status,
            int page,
            int size
    ) {
        logger.info()
                .message("Fetching access request history")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("status", status != null ? status.name() : "ALL")
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_HISTORY_FETCH_START)
                .log();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "requestedAt")
        );

        Page<AccessRequest> requests;

        if (status != null) {
            requests = requestRepository
                    .findByTenantIdAndRequestedByUserIdAndStatus(
                            tenantId, userId, status, pageable
                    );
        } else {
            requests = requestRepository
                    .findByTenantIdAndRequestedByUserId(
                            tenantId, userId, pageable
                    );
        }

        logger.info()
                .message("Access request history fetched")
                .detail("userId", userId.toString())
                .detail("totalRequests", String.valueOf(requests.getTotalElements()))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_HISTORY_FETCH_SUCCESS)
                .log();

        return requests.map(mapper::toHistoryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDetailsResponseDTO getRequestDetails(
            String tenantId,
            UUID userId,
            UUID requestId
    ) {
        logger.info()
                .message("Fetching access request details")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("requestId", requestId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_DETAILS_FETCH_START)
                .log();

        // Step 1: Load base request with items (most important data)
        AccessRequest request = requestRepository.findByIdWithItems(requestId, tenantId)
                .orElseThrow(() -> {
                    logger.error()
                            .message("Access request not found")
                            .detail("requestId", requestId.toString())
                            .detail("tenantId", tenantId)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_NOT_FOUND)
                            .log();

                    return new IllegalArgumentException("Access request not found: " + requestId);
                });

        // Authorization check
        boolean canView = request.getRequestedByUserId().equals(userId);

        if (!canView) {
            logger.warn()
                    .message("Unauthorized access request details access attempt")
                    .detail("requestId", requestId.toString())
                    .detail("userId", userId.toString())
                    .detail("requestOwnerId", request.getRequestedByUserId().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_UNAUTHORIZED_ACCESS)
                    .log();

            throw new SecurityException("User not authorized to view this request");
        }

//        // Step 2: Load approval steps separately (avoids MultipleBagFetchException)
//        requestRepository.findByIdWithApprovalSteps(requestId, tenantId)
//                .ifPresent(r -> request.setApprovalSteps(r.getApprovalSteps()));

        // Step 2: Load approval steps for EACH ITEM separately
        request.getItems().forEach(item -> {
            List<RequestApprovalStep> itemApprovalSteps =
                    approvalStepRepository.findByRequestItemOrderByStepOrder(item);
            item.setApprovalSteps(new LinkedHashSet<>(itemApprovalSteps));
        });

        // Step 3: Load comments separately
        requestRepository.findByIdWithComments(requestId, tenantId)
                .ifPresent(r -> request.setComments(r.getComments()));


        // Step 4: Load audit logs separately
        requestRepository.findByIdWithAuditLogs(requestId, tenantId)
                .ifPresent(r -> request.setAuditLogs(r.getAuditLogs()));

        logger.info()
                .message("Access request details fetched successfully")
                .detail("requestId", requestId.toString())
                .detail("status", request.getStatus().name())
                .detail("itemCount", String.valueOf(request.getItems().size()))
                .detail("approvalStepsCount", String.valueOf(
                        request.getItems().stream()
                                .mapToInt(item -> item.getApprovalSteps() != null ? item.getApprovalSteps().size() : 0)
                                .sum()))
                .detail("commentsCount", String.valueOf(request.getComments() != null ? request.getComments().size() : 0))
                .detail("auditLogsCount", String.valueOf(request.getAuditLogs() != null ? request.getAuditLogs().size() : 0))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_DETAILS_FETCH_SUCCESS)
                .log();

        RequestDetailsResponseDTO response = mapper.toDetailsResponse(request);

        if (response.getItems() != null && !response.getItems().isEmpty()) {
            itemNameResolver.enrichNames(response.getItems(), tenantId);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public RequestLifecycleDTO getRequestLifecycle(
            String tenantId,
            UUID userId,
            UUID requestId
    ) {
        logger.info()
                .message("Fetching access request lifecycle")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("requestId", requestId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_LIFECYCLE_FETCH_START)
                .log();

        // Step 1: Load base request
        AccessRequest request = requestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> {
                    logger.error()
                            .message("Access request not found for lifecycle")
                            .detail("requestId", requestId.toString())
                            .detail("tenantId", tenantId)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_NOT_FOUND)
                            .log();

                    return new IllegalArgumentException("Access request not found: " + requestId);
                });

        // Authorization check
        boolean canView = request.getRequestedByUserId().equals(userId);

        if (!canView) {
            logger.warn()
                    .message("Unauthorized access request lifecycle access attempt")
                    .detail("requestId", requestId.toString())
                    .detail("userId", userId.toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_UNAUTHORIZED_ACCESS)
                    .log();

            throw new SecurityException("User not authorized to view this request lifecycle");
        }

        // Step 2: Load audit logs separately (primary data for lifecycle)
        requestRepository.findByIdWithAuditLogs(requestId, tenantId)
                .ifPresent(r -> request.setAuditLogs(r.getAuditLogs()));

        logger.info()
                .message("Access request lifecycle fetched successfully")
                .detail("requestId", requestId.toString())
                .detail("auditLogEntries", String.valueOf(request.getAuditLogs() != null ? request.getAuditLogs().size() : 0))
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_LIFECYCLE_FETCH_SUCCESS)
                .log();

        return mapper.toLifecycleDTO(request);
    }

    @Override
    @Transactional
    public AccessRequestResponseDTO withdrawRequest(
            String tenantId,
            UUID userId,
            UUID requestId,
            WithdrawRequestDTO withdrawDTO
    ) {
        logger.info()
                .message("Withdrawing access request")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("requestId", requestId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_WITHDRAW_START)
                .log();

        // Fetch the request with items
        AccessRequest request = requestRepository.findByIdWithItems(requestId, tenantId)
                .orElseThrow(() -> {
                    logger.error()
                            .message("Access request not found for withdrawal")
                            .detail("requestId", requestId.toString())
                            .detail("tenantId", tenantId)
                            .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_NOT_FOUND)
                            .log();

                    return new IllegalArgumentException("Access request not found: " + requestId);
                });

        // Authorization check - only requester can withdraw
        if (!request.getRequestedByUserId().equals(userId)) {
            logger.warn()
                    .message("Unauthorized withdrawal attempt")
                    .detail("requestId", requestId.toString())
                    .detail("userId", userId.toString())
                    .detail("requestOwnerId", request.getRequestedByUserId().toString())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_UNAUTHORIZED_ACCESS)
                    .log();

            throw new SecurityException("Only the requester can withdraw this request");
        }

        // Status validation - only SUBMITTED or PENDING_APPROVAL can be withdrawn
        if (request.getStatus() != RequestStatus.SUBMITTED &&
                request.getStatus() != RequestStatus.PENDING_APPROVAL) {

            logger.warn()
                    .message("Request cannot be withdrawn - invalid status")
                    .detail("requestId", requestId.toString())
                    .detail("currentStatus", request.getStatus().name())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_INVALID_STATUS_TRANSITION)
                    .log();

            throw new IllegalStateException(
                    "Request cannot be withdrawn. Current status: " + request.getStatus() +
                            ". Only SUBMITTED or PENDING_APPROVAL requests can be withdrawn."
            );
        }

        // Get the previous status for audit log
        String previousStatus = request.getStatus().name();

        // Update request status to WITHDRAWN
        request.setStatus(RequestStatus.WITHDRAWN);
        request.setUpdatedAt(OffsetDateTime.now());

        logger.debug()
                .message("Request status updated to WITHDRAWN")
                .detail("requestId", requestId.toString())
                .detail("previousStatus", previousStatus)
                .log();


        // Create audit log entry
        RequestAuditLog auditLog = new RequestAuditLog();
        auditLog.setAccessRequest(request);
        auditLog.setAction(AuditAction.REQUEST_WITHDRAWN);
        auditLog.setPerformedBy(fetchUserDisplayName());
        auditLog.setTimestamp(OffsetDateTime.now());
        auditLog.setPreviousStatus(previousStatus);
        auditLog.setNewStatus(RequestStatus.WITHDRAWN.name());
        auditLog.setDetails("Request withdrawn by requester. Reason: " + withdrawDTO.getWithdrawalReason());
        auditLog.setTenantId(tenantId);

        request.getAuditLogs().add(auditLog);

        logger.debug()
                .message("Audit log entry created for withdrawal")
                .detail("reason", withdrawDTO.getWithdrawalReason())
                .log();

        // Create comment for withdrawal
        RequestComment comment = new RequestComment();
        comment.setAccessRequest(request);
        comment.setCommentedByUserId(userId);
        comment.setCommentedByUserName(fetchUserDisplayName());
        comment.setCommentText(
                "Request withdrawn by requester. Reason: " + withdrawDTO.getWithdrawalReason()
        );
        comment.setCommentType(CommentType.WITHDRAWAL);
        comment.setCommentedAt(OffsetDateTime.now());
        comment.setIsInternal(false);
        comment.setTenantId(tenantId);

        request.getComments().add(comment);

        logger.debug()
                .message("Withdrawal comment added")
                .log();

        // Save all changes
        requestRepository.save(request);

        logger.info()
                .message("Access request withdrawn successfully")
                .detail("requestId", requestId.toString())
                .detail("userId", userId.toString())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_REQUEST_WITHDRAW_SUCCESS)
                .log();

        return mapper.toResponse(request);
    }

    /**
     * Fetches the display name for the current user from the auth service.
     * Falls back to "Unknown User" if the display name cannot be retrieved.
     *
     * @return user display name or fallback value
     */
    private String fetchUserDisplayName() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                logger.warn()
                        .message("Cannot fetch user display name - request context not available")
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                        .log();
                return "Unknown User";
            }

            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn()
                        .message("Cannot fetch user display name - no authorization header")
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                        .log();
                return "Unknown User";
            }

            String jwtToken = authHeader.substring(7);
            String displayName = userProfileRestClient.getCurrentUserDisplayName(jwtToken);

            if (displayName == null || displayName.isBlank()) {
                logger.warn()
                        .message("Display name not found - using fallback")
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                        .log();
                return "Unknown User";
            }

            return displayName;

        } catch (Exception e) {
            logger.error()
                    .message("Error fetching user display name - using fallback")
                    .detail("error", e.getMessage())
                    .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_USER_PROFILE_FETCH_ERROR)
                    .log();

            return "Unknown User";
        }
    }

    /**
     * Parses comma-separated beneficiary user IDs into a list.
     */
    private List<UUID> parseBeneficiaryUserIds(String beneficiaryUserIds) {
        if (beneficiaryUserIds == null || beneficiaryUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(beneficiaryUserIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    /**
     * Validates cart items and builds DTO lookup map.
     */
    private Map<UUID, SubmitAccessRequestDTO.RequestItemDTO> validateAndBuildItemMap(
            SubmitAccessRequestDTO dto,
            List<CartItem> cartItems
    ) {
        validator.validateSubmitRequest(dto, cartItems);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Map<UUID, SubmitAccessRequestDTO.RequestItemDTO> itemDTOMap = dto.getItems()
                .stream()
                .collect(Collectors.toMap(
                        SubmitAccessRequestDTO.RequestItemDTO::getCartItemId,
                        item -> item
                ));

        logger.debug()
                .message("Created item DTO lookup map")
                .detail("mapSize", String.valueOf(itemDTOMap.size()))
                .log();

        for (CartItem cartItem : cartItems) {
            if (!itemDTOMap.containsKey(cartItem.getId())) {
                logger.error()
                        .message("Cart item missing in request")
                        .detail("cartItemId", cartItem.getId().toString())
                        .detail("itemId", cartItem.getItemId().toString())
                        .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_ERROR)
                        .log();
                throw new IllegalArgumentException(
                        "Cart item " + cartItem.getItemId() + " is missing in request"
                );
            }
        }

        return itemDTOMap;
    }

    /**
     * Creates AccessRequestItem from CartItem and DTO.
     */
    private AccessRequestItem buildAccessRequestItem(
            CartItem cartItem,
            SubmitAccessRequestDTO.RequestItemDTO itemDTO,
            AccessRequest request,
            String tenantId,
            UUID userId
    ) {
        AccessRequestItem item = new AccessRequestItem();
        item.setAccessRequest(request);
        item.setTile(cartItem.getTile());
        item.setItemId(cartItem.getItemId());
        item.setItemName(cartItem.getItemName());
        item.setTenantId(tenantId);
        item.setItemType(cartItem.getItemType());
        item.setRequestTarget(
                cartItem.getBeneficiaryUserIds() == null || cartItem.getBeneficiaryUserIds().isEmpty()
                        ? RequestTarget.SELF
                        : RequestTarget.OTHERS
        );
        item.setBeneficiaryUserIds(cartItem.getBeneficiaryUserIds());
        item.setBeneficiaryNames(cartItem.getBeneficiaryNames());

        String justification = itemDTO.getJustification();

        if (justification == null || justification.trim().isEmpty()) {
            justification = ""; // or use overallJustification as fallback
        }
        item.setJustification(justification);

        item.setValidityDays(itemDTO.getValidityDays());
        item.setUrgency(UrgencyLevel.valueOf(itemDTO.getUrgency().toUpperCase()));

        cartItem.setStatus(CartStatus.INACTIVE); // status set to inactive after processing

        logger.trace()
                .message("Processed cart item")
                .detail("itemId", cartItem.getItemId().toString())
                .detail("requestTarget", item.getRequestTarget().name())
                .log();

        return item;
    }

    /**
     * Fetches manager IDs for a list of user IDs.
     */
    private Map<String, String> fetchManagersForUsers(
            String tenantId,
            List<UUID> userIds
    ) {
        try {
            List<String> userIdStrings = userIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());

            // Step 1: Get identity IDs for users
            Map<String, String> identityMap = userIdentitiesGrpcClient
                    .getIdentityIdsByUserIds(tenantId, userIdStrings);

            logger.debug()
                    .message("Fetched identity mappings for manager lookup")
                    .detail("userCount", String.valueOf(userIds.size()))
                    .detail("identitiesFound", identityMap.values().stream()
                            .filter(id -> !id.isEmpty()).count())
                    .log();

            // Step 2: Transform identity mappings to manager mappings
            Map<String, String> managerMap = userManagerLookupGrpcClient
                    .transformToManagerMappings(tenantId, identityMap);

            logger.debug()
                    .message("Fetched manager mappings")
                    .detail("userCount", String.valueOf(userIds.size()))
                    .detail("managersFound", managerMap.values().stream()
                            .filter(id -> !id.isEmpty()).count())
                    .log();

            return managerMap;

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to fetch managers for users")
                    .detail("userCount", String.valueOf(userIds.size()))
                    .log();
            return new HashMap<>();
        }
    }

    /**
     * Publishes workflow events - one event per ACCESS REQUEST ITEM.
     * Even if multiple items share the same role, each gets its own workflow.
     */
    private void publishWorkflowEventsPerItem(
            AccessRequest request,
            Map<UUID, ItemWorkflowDetail> itemWorkflowMap,
            String tenantId,
            UUID workflowId,
            UUID tileId,
            String tileTitle,
            String requestedByUserName,
            UUID userId,
            String overallJustification
    ) {
        if (itemWorkflowMap.isEmpty()) {
            return;
        }

        logger.info()
                .message("Publishing workflow events per item")
                .detail("itemCount", String.valueOf(itemWorkflowMap.size()))
                .log();

        int successCount = 0;
        int failureCount = 0;

        // Collect all unique user IDs across all items for manager lookup
        Set<UUID> allUserIds = new LinkedHashSet<>();
        allUserIds.add(userId); // Add requester

        for (ItemWorkflowDetail detail : itemWorkflowMap.values()) {
            allUserIds.addAll(detail.userIds);
        }

        // Fetch managers for all users at once
        Map<String, String> userManagerMap = fetchManagersForUsers(
                tenantId,
                new ArrayList<>(allUserIds)
        );

        // Publish one event per ITEM
        for (Map.Entry<UUID, ItemWorkflowDetail> entry : itemWorkflowMap.entrySet()) {
            UUID itemId = entry.getKey();  // AccessRequestItem DB ID
            ItemWorkflowDetail detail = entry.getValue();

            try {
                // Fetch role owners for this role
                List<UUID> roleOwners = fetchRoleOwners(detail.roleId, tenantId);

                // Build managers array for this item's users (with userNames)
                List<Map<String, String>> managers = new ArrayList<>();
                for (int i = 0; i < detail.userIds.size(); i++) {
                    UUID userForItem = detail.userIds.get(i);
                    String managerId = userManagerMap.getOrDefault(userForItem.toString(), "");

                    if (!managerId.isEmpty()) {
                        Map<String, String> managerEntry = new HashMap<>();
                        managerEntry.put("userId", userForItem.toString());
                        managerEntry.put("managerId", managerId);


                        if (i < detail.userNames.size()) {
                            managerEntry.put("userName", detail.userNames.get(i));
                        }
                        managers.add(managerEntry);
                    }
                }

                // Publish event for this ITEM
                eventPublisher.publishSingleRoleAssignmentRequestEvent(
                        request.getId(),
                        detail.roleId,          // The actual role UUID
                        detail.userIds,
                        managers,
                        roleOwners,
                        detail.roleName,
                        detail.justification,
                        detail.validityDays,
                        overallJustification,
                        tenantId,
                        workflowId.toString(),
                        requestedByUserName,
                        userId,
                        tileId,
                        tileTitle,
                        true // shouldSendNotification
                );

                successCount++;

                logger.debug()
                        .message("Published workflow event for item")
                        .detail("itemId", itemId.toString())
                        .detail("roleId", detail.roleId.toString())
                        .detail("roleName", detail.roleName)
                        .detail("userCount", String.valueOf(detail.userIds.size()))
                        .detail("managerCount", String.valueOf(managers.size()))
                        .detail("ownerCount", String.valueOf(roleOwners.size()))
                        .log();

            } catch (Exception e) {
                failureCount++;
                logger.error(e)
                        .message("Failed to publish workflow event for item")
                        .detail("itemId", itemId.toString())
                        .detail("roleId", detail.roleId.toString())
                        .detail("requestId", request.getId().toString())
                        .log();
            }
        }

        logger.info()
                .message("Workflow events published")
                .detail("totalItems", String.valueOf(itemWorkflowMap.size()))
                .detail("successCount", String.valueOf(successCount))
                .detail("failureCount", String.valueOf(failureCount))
                .log();

        if (failureCount > 0) {
            logger.warn()
                    .message("Some workflow events failed to publish")
                    .detail("failureCount", String.valueOf(failureCount))
                    .log();
        }
    }

    /**
     * Publishes workflow events for roles that require approval.
     * Publishes one event per role.
     */
    private void publishWorkflowEvents(
            AccessRequest request,
            Map<UUID, RoleAssignmentDetail> roleUserMap,
            String tenantId,
            UUID workflowId,
            UUID tileId,
            String tileTitle,
            String requestedByUserName,
            UUID userId,
            String overallJustification
    ) {
        if (roleUserMap.isEmpty()) {
            return;
        }

        logger.info()
                .message("Publishing workflow events per role")
                .detail("roleCount", String.valueOf(roleUserMap.size()))
                .log();

        int successCount = 0;
        int failureCount = 0;

        // Collect all unique user IDs across all roles for manager lookup
        Set<UUID> allUserIds = new LinkedHashSet<>();
        allUserIds.add(userId); // Add requester

        for (RoleAssignmentDetail detail : roleUserMap.values()) {
            allUserIds.addAll(detail.userIds);
        }

        // Fetch managers for all users at once
        Map<String, String> userManagerMap = fetchManagersForUsers(
                tenantId,
                new ArrayList<>(allUserIds)
        );

        // Publish one event per role
        for (Map.Entry<UUID, RoleAssignmentDetail> entry : roleUserMap.entrySet()) {
            UUID roleId = entry.getKey();
            RoleAssignmentDetail roleDetail = entry.getValue();

            try {
                // Fetch role owners for this role
                List<UUID> roleOwners = fetchRoleOwners(roleId, tenantId);

                // Build managers array for this role's users (with userNames)
                List<Map<String, String>> managers = new ArrayList<>();
                for (int i = 0; i < roleDetail.userIds.size(); i++) {
                    UUID userForRole = roleDetail.userIds.get(i);
                    String managerId = userManagerMap.getOrDefault(userForRole.toString(), "");
                    if (!managerId.isEmpty()) {
                        Map<String, String> managerEntry = new HashMap<>();
                        managerEntry.put("userId", userForRole.toString());
                        managerEntry.put("managerId", managerId);
                        // Add userName if available (same index as userId)
                        if (i < roleDetail.userNames.size()) {
                            managerEntry.put("userName", roleDetail.userNames.get(i));
                        }
                        managers.add(managerEntry);
                    }
                }

                // Publish event for this role
                eventPublisher.publishSingleRoleAssignmentRequestEvent(
                        request.getId(),
                        roleId,
                        roleDetail.userIds,
                        managers,
                        roleOwners,
                        roleDetail.roleName,  // Add role name
                        roleDetail.justification,
                        roleDetail.validityDays,
                        overallJustification,
                        tenantId,
                        workflowId.toString(),
                        requestedByUserName,
                        userId,
                        tileId,
                        tileTitle,
                        true // shouldSendNotification
                );

                successCount++;

                logger.debug()
                        .message("Published workflow event for role")
                        .detail("roleId", roleId.toString())
                        .detail("userCount", String.valueOf(roleDetail.userIds.size()))
                        .detail("managerCount", String.valueOf(managers.size()))
                        .detail("ownerCount", String.valueOf(roleOwners.size()))
                        .log();

            } catch (Exception e) {
                failureCount++;
                logger.error(e)
                        .message("Failed to publish workflow event for role")
                        .detail("roleId", roleId.toString())
                        .detail("requestId", request.getId().toString())
                        .log();
            }
        }

        logger.info()
                .message("Workflow events published")
                .detail("totalRoles", String.valueOf(roleUserMap.size()))
                .detail("successCount", String.valueOf(successCount))
                .detail("failureCount", String.valueOf(failureCount))
                .log();

        if (failureCount > 0) {
            logger.warn()
                    .message("Some workflow events failed to publish")
                    .detail("failureCount", String.valueOf(failureCount))
                    .log();
        }
    }
}