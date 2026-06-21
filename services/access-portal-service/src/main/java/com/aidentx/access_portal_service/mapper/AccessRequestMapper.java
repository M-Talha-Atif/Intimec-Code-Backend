package com.aidentx.access_portal_service.mapper;

import com.aidentx.access_portal_service.dto.response.access_request.*;
import com.aidentx.access_portal_service.entity.access_request.*;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static com.aidentx.access_portal_service.util.StringParsingUtils.parseBeneficiaryNames;
import static com.aidentx.access_portal_service.util.StringParsingUtils.parseBeneficiaryUserIds;

/**
 * Mapper for converting AccessRequest entities to DTOs.
 */
@Component
public class AccessRequestMapper {

    /**
     * Maps AccessRequest to basic response DTO.
     */
    public AccessRequestResponseDTO toResponse(AccessRequest request) {
        AccessRequestResponseDTO dto = new AccessRequestResponseDTO();
        dto.setRequestId(request.getId());
        dto.setTenantId(request.getTenantId());
        dto.setStatus(request.getStatus().name());
        dto.setOverallJustification(request.getOverallJustification());
        return dto;
    }

    /**
     * Maps AccessRequest to history response DTO.
     */
    public AccessRequestHistoryResponseDTO toHistoryResponse(AccessRequest request) {
        AccessRequestHistoryResponseDTO dto = new AccessRequestHistoryResponseDTO();
        dto.setRequestId(request.getId());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setStatus(request.getStatus());
        dto.setOverallJustification(request.getOverallJustification());
        dto.setItems(
                request.getItems().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
        return dto;
    }

    /**
     * Maps AccessRequest to detailed response DTO with full information.
     */
    public RequestDetailsResponseDTO toDetailsResponse(AccessRequest request) {
        RequestDetailsResponseDTO dto = new RequestDetailsResponseDTO();
        dto.setRequestId(request.getId());
        dto.setRequestedByUserId(request.getRequestedByUserId());
        dto.setRequestedByUserName(request.getRequestedByUserName());
        dto.setOverallJustification(request.getOverallJustification());
        dto.setStatus(request.getStatus().name());
        dto.setRequestedAt(request.getRequestedAt());
        dto.setLastUpdatedAt(request.getUpdatedAt());

        // Map items with full details also having approval steps
        dto.setItems(
                request.getItems().stream()
                        .map(this::toItemDetailsResponse)
                        .collect(Collectors.toList())
        );

        // Map comments if available
        if (request.getComments() != null) {
            dto.setComments(
                    request.getComments().stream()
                            .map(this::toCommentDTO)
                            .collect(Collectors.toList())
            );
        }

        // Map audit log if available
        if (request.getAuditLogs() != null) {
            dto.setAuditLog(
                    request.getAuditLogs().stream()
                            .map(this::toAuditLogDTO)
                            .collect(Collectors.toList())
            );
        }

        // Build metadata
        dto.setMetadata(buildMetadata(request));

        return dto;
    }

    /**
     * Maps AccessRequestItem to basic item response DTO.
     */
    private AccessRequestItemResponseDTO toItemResponse(AccessRequestItem item) {
        AccessRequestItemResponseDTO dto = new AccessRequestItemResponseDTO();
        dto.setItemId(item.getItemId());
        dto.setItemType(item.getItemType());
        dto.setItemName(item.getItemName());
        dto.setValidityDays(item.getValidityDays());
        dto.setUrgency(item.getUrgency().name());
        dto.setJustification(item.getJustification());
        dto.setRequestTarget(item.getRequestTarget().name());
        return dto;
    }

    /**
     * Maps AccessRequestItem to detailed item response DTO.
     */
    private RequestItemDetailsDTO toItemDetailsResponse(AccessRequestItem item) {
        RequestItemDetailsDTO dto = new RequestItemDetailsDTO();
        dto.setItemId(item.getItemId());
        dto.setItemType(item.getItemType());
        dto.setItemName(item.getItemName());
        dto.setRequestTarget(item.getRequestTarget().name());

        // Parse comma-separated beneficiary data
        dto.setBeneficiaryUserIds(parseBeneficiaryUserIds(item.getBeneficiaryUserIds()));
        dto.setBeneficiaryUserNames(parseBeneficiaryNames(item.getBeneficiaryNames()));

        dto.setValidityDays(item.getValidityDays());
        dto.setUrgency(item.getUrgency().name());
        dto.setJustification(item.getJustification());

        if (item.getTile() != null) {
            dto.setTileId(item.getTile().getId().toString());
            dto.setTileName(item.getTile().getTitle());
        }

        // Map provisioning status if available
        if (item.getProvisioning() != null && !item.getProvisioning().isEmpty())  {
            dto.setProvisioningStatuses(
                item.getProvisioning().stream()
                    .map(this::toProvisioningStatusDTO)
                    .collect(Collectors.toList())
            );
        }

        if (item.getApprovalSteps() != null) {
        dto.setApprovalSteps(
            item.getApprovalSteps().stream()
                .map(this::toApprovalStepDTO)
                .collect(Collectors.toList())
        );
    }

        return dto;
    }

    /**
     * Maps RequestProvisioning to ProvisioningStatusDTO.
     */
    private ProvisioningStatusDTO toProvisioningStatusDTO(RequestProvisioning provisioning) {
        ProvisioningStatusDTO dto = new ProvisioningStatusDTO();
        dto.setProvisioningId(provisioning.getId());
        dto.setTargetUserId(provisioning.getTargetUserId());
        dto.setStatus(provisioning.getStatus().name());
        dto.setProvisioningStartedAt(provisioning.getStartedAt());
        dto.setProvisioningCompletedAt(provisioning.getCompletedAt());
        dto.setTargetSystem(provisioning.getTargetSystem());
        dto.setExternalTicketId(provisioning.getExternalTicketId());
        dto.setErrorMessage(provisioning.getErrorMessage());
        dto.setAccessExpiresAt(provisioning.getAccessExpiresAt());
        dto.setRetryCount(provisioning.getRetryCount());
        return dto;
    }

    /**
     * Maps RequestApprovalStep to ApprovalStepDTO.
     */
    private ApprovalStepDTO toApprovalStepDTO(RequestApprovalStep step) {
        ApprovalStepDTO dto = new ApprovalStepDTO();
        dto.setStepId(step.getId());
        dto.setStepOrder(step.getStepOrder());
        dto.setItemId( String.valueOf(step.getRequestItem().getItemId()));
        dto.setTargetUserId(step.getTargetUserId());
        dto.setTargetUserName(step.getTargetUserName());
        dto.setApproverRole(step.getApproverRole());
        dto.setAssignedApproverId(step.getAssignedApproverId());
        dto.setAssignedApproverName(step.getAssignedApproverName());
        dto.setAssignedApproverEmail(step.getAssignedApproverEmail());
        dto.setStatus(step.getStatus().name());
        dto.setCreatedAt(step.getCreatedAt());
        dto.setRespondedAt(step.getRespondedAt());
        dto.setDecision(step.getDecision() != null ? step.getDecision().name() : null);
        dto.setComments(step.getComments());
        dto.setDelegatedToUserId(step.getDelegatedToUserId());
        dto.setDelegatedToUserName(step.getDelegatedToUserName());
        dto.setDelegatedAt(step.getDelegatedAt());
        return dto;
    }

    /**
     * Maps RequestComment to RequestCommentDTO.
     */
    private RequestCommentDTO toCommentDTO(RequestComment comment) {
        RequestCommentDTO dto = new RequestCommentDTO();
        dto.setCommentId(comment.getId());
        dto.setCommentedByUserId(comment.getCommentedByUserId());
        dto.setCommentedByUserName(comment.getCommentedByUserName());
        dto.setCommentText(comment.getCommentText());
        dto.setCommentType(comment.getCommentType().name());
        dto.setParentCommentId(comment.getParentCommentId());
        dto.setCommentedAt(comment.getCommentedAt());
        dto.setIsInternal(comment.getIsInternal());
        dto.setMentionedUserId(comment.getMentionedUserId());
        return dto;
    }

    /**
     * Maps RequestAuditLog to AuditLogEntryDTO.
     */
    private AuditLogEntryDTO toAuditLogDTO(RequestAuditLog log) {
        AuditLogEntryDTO dto = new AuditLogEntryDTO();
        dto.setLogId(log.getId());
        dto.setAction(log.getAction().name());
        dto.setPerformedBy(log.getPerformedBy());
        dto.setPerformedAt(log.getTimestamp());
        dto.setPreviousStatus(log.getPreviousStatus());
        dto.setNewStatus(log.getNewStatus());
        dto.setDetails(log.getDetails());
        return dto;
    }

      private RequestMetadataDTO buildMetadata(AccessRequest request) {
          RequestMetadataDTO metadata = new RequestMetadataDTO();
          metadata.setTotalItems(request.getItems().size());

          // Calculate item statuses (would need item-level status tracking)
          metadata.setApprovedItems(0);
          metadata.setRejectedItems(0);
          metadata.setPendingItems(request.getItems().size());

          // Calculate risk score based on items
          int riskScore = calculateRiskScore(request);
          metadata.setRiskScore(riskScore);

          // Check for SOD conflicts and compliance flags
          metadata.setHasSODConflicts(false); // TODO: Implement SOD detection
          metadata.setHasComplianceFlags(false); // TODO: Implement compliance checking
          metadata.setIsEmergencyRequest(false); // TODO: Add emergency flag to request

          // Approval workflow metadata - NOW AGGREGATED FROM ITEMS
          List<RequestApprovalStep> allApprovalSteps = request.getItems().stream()
              .filter(item -> item.getApprovalSteps() != null)
              .flatMap(item -> item.getApprovalSteps().stream())
              .collect(Collectors.toList());

          if (!allApprovalSteps.isEmpty()) {
              metadata.setTotalApprovalSteps(allApprovalSteps.size());

              long completed = allApprovalSteps.stream()
                      .filter(step -> step.getStatus() == ApprovalStepStatus.APPROVED ||
                                     step.getStatus() == ApprovalStepStatus.REJECTED)
                      .count();
              metadata.setCompletedApprovalSteps((int) completed);

              // Find current approval step (first pending step across all items)
              allApprovalSteps.stream()
                      .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING_ASSIGNMENT ||
                                      step.getStatus() == ApprovalStepStatus.ASSIGNED ||
                                      step.getStatus() == ApprovalStepStatus.IN_REVIEW)
                      .findFirst()
                      .ifPresent(step -> metadata.setCurrentApprovalStep(step.getApproverRole()));
          } else {
              metadata.setTotalApprovalSteps(0);
              metadata.setCompletedApprovalSteps(0);
          }

          return metadata;
      }
     /**
      * Calculates risk score based on request items and urgency.
      */
    private int calculateRiskScore(AccessRequest request) {
        int score = request.getItems().size() * 15;

        boolean hasHighUrgency = request.getItems().stream()
                .anyMatch(item -> item.getUrgency() == UrgencyLevel.HIGH);

        if (hasHighUrgency) {
            score += 20;
        }

        return Math.min(100, score);
    }

    /**
     * Maps AccessRequest to lifecycle timeline DTO.
     */
    public RequestLifecycleDTO toLifecycleDTO(AccessRequest request) {
        RequestLifecycleDTO dto = new RequestLifecycleDTO();
        dto.setSubmittedAt(request.getRequestedAt());

        // Calculate durations if applicable
        if (request.getAuditLogs() != null && !request.getAuditLogs().isEmpty()) {
            // Find approval and provisioning timestamps from audit log
            request.getAuditLogs().stream()
                    .filter(log -> log.getAction() == AuditAction.REQUEST_FULLY_APPROVED)
                    .findFirst()
                    .ifPresent(log -> {
                        dto.setApprovedAt(log.getTimestamp());
                        long hours = Duration.between(request.getRequestedAt(), log.getTimestamp()).toHours();
                        dto.setApprovalDurationHours(hours);
                    });

            request.getAuditLogs().stream()
                    .filter(log -> log.getAction() == AuditAction.PROVISIONING_COMPLETED)
                    .findFirst()
                    .ifPresent(log -> {
                        dto.setProvisionedAt(log.getTimestamp());
                        if (dto.getApprovedAt() != null) {
                            long hours = Duration.between(dto.getApprovedAt(), log.getTimestamp()).toHours();
                            dto.setProvisioningDurationHours(hours);
                        }
                    });

            request.getAuditLogs().stream()
                    .filter(log -> log.getAction() == AuditAction.REQUEST_PARTIALLY_APPROVED)
                    .findFirst()
                    .ifPresent(log -> dto.setRejectedAt(log.getTimestamp()));
        }

        // Build lifecycle events from audit log
        List<LifecycleEventDTO> events = new ArrayList<>();
        if (request.getAuditLogs() != null) {
            events = request.getAuditLogs().stream()
                    .map(this::toLifecycleEventDTO)
                    .collect(Collectors.toList());
        }
        dto.setEvents(events);

        return dto;
    }

    /**
     * Maps audit log to lifecycle event DTO.
     */
    private LifecycleEventDTO toLifecycleEventDTO(RequestAuditLog log) {
        LifecycleEventDTO event = new LifecycleEventDTO();
        event.setEventType(log.getAction().name());
        event.setEventDescription(log.getDetails());
        event.setPerformedBy(log.getPerformedBy());
        event.setOccurredAt(log.getTimestamp());

        // Set icon and color based on action type
        switch (log.getAction()) {
            case REQUEST_SUBMITTED:
                event.setIcon("file-plus");
                event.setColor("blue");
                break;
            case REQUEST_FULLY_APPROVED:
            case APPROVAL_APPROVED:
                event.setIcon("check-circle");
                event.setColor("green");
                break;
            case REQUEST_PARTIALLY_APPROVED:
            case APPROVAL_REJECTED:
                event.setIcon("x-circle");
                event.setColor("red");
                break;
            case PROVISIONING_COMPLETED:
                event.setIcon("check");
                event.setColor("green");
                break;
            case PROVISIONING_FAILED:
                event.setIcon("alert-circle");
                event.setColor("red");
                break;
            default:
                event.setIcon("circle");
                event.setColor("gray");
        }

        return event;
    }
}