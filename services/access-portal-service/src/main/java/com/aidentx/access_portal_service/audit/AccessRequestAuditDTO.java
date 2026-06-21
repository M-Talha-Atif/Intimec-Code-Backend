package com.aidentx.access_portal_service.audit;

import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simplified audit representation of an AccessRequest to prevent circular references.
 * Only includes essential fields needed for audit logging.
 */
@Data
@Builder
public class AccessRequestAuditDTO {
    private UUID id;
    private UUID requestedByUserId;
    private String requestedByUserName;
    private String overallJustification;
    private RequestStatus status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime updatedAt;
    private String tenantId;
    private int itemCount;
    private List<AccessRequestItemSummary> items;

    /**
     * Simplified item summary for audit
     */
    @Data
    @Builder
    public static class AccessRequestItemSummary {
        private UUID itemId;
        private String itemName;
        private String itemType;
        private String requestTarget;
        private int beneficiaryCount;
        private String urgency;
        private int validityDays;
    }

    /**
     * Converts an AccessRequest entity to an audit-safe DTO.
     */
    public static AccessRequestAuditDTO from(AccessRequest request) {
        List<AccessRequestItemSummary> itemSummaries = request.getItems() != null
                ? request.getItems().stream()
                .map(item -> AccessRequestItemSummary.builder()
                        .itemId(item.getItemId())
                        .itemName(item.getItemName())
                        .itemType(item.getItemType())
                        .requestTarget(item.getRequestTarget() != null ? item.getRequestTarget().name() : "SELF")
                        .beneficiaryCount(item.getBeneficiaryUserIds() != null && !item.getBeneficiaryUserIds().isEmpty()
                                ? item.getBeneficiaryUserIds().split(",").length
                                : 0)
                        .urgency(item.getUrgency() != null ? item.getUrgency().name() : "MEDIUM")
                        .validityDays(item.getValidityDays())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return AccessRequestAuditDTO.builder()
                .id(request.getId())
                .requestedByUserId(request.getRequestedByUserId())
                .requestedByUserName(request.getRequestedByUserName())
                .overallJustification(request.getOverallJustification())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .updatedAt(request.getUpdatedAt())
                .tenantId(request.getTenantId())
                .itemCount(request.getItems() != null ? request.getItems().size() : 0)
                .items(itemSummaries)
                .build();
    }
}