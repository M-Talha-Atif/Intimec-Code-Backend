package com.aidentx.access_portal_service.factory;

import com.aidentx.access_portal_service.entity.access_request.*;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class AccessRequestEntityFactory {

    public RequestApprovalStep createApprovalStep(
            AccessRequestItem requestItem,
             AccessRequest accessRequest,
            int stepOrder,
            String approverRole,
            String approverName,
            String assignedApproverId,   // ← the approver/manager doing the approval
            String targetUserId,         // ← the beneficiary being approved for
            String targetUserName ) {

        RequestApprovalStep step = RequestApprovalStep.builder()
            .requestItem(requestItem)
            .accessRequest(accessRequest) // can't use requestItem.getAccessRequest() due to lazy loading
            .stepOrder(stepOrder)
            .decision( ApprovalDecision.PENDING)
            .approverRole(approverRole)
            .assignedApproverId(assignedApproverId)  // manager's ID
            .targetUserId(targetUserId)
            .assignedApproverName(approverName)
            // beneficiary's ID
            .targetUserName(targetUserName)
            .assignedApproverId(assignedApproverId)
            .status(ApprovalStepStatus.PENDING_ASSIGNMENT)
            .createdAt(OffsetDateTime.now())
            .build();

        // Set tenant ID from parent
        step.setTenantId(requestItem.getTenantId());

        return step;
    }

    public RequestProvisioning createProvisioning(
        AccessRequest request,
        AccessRequestItem item,
        String roleId,
        String targetUserId) {
    RequestProvisioning provisioning = RequestProvisioning.builder()
        .accessRequest(request)
        .requestItem(item)
        .targetUserId(targetUserId)
        .status(ProvisioningStatus.IN_PROGRESS)
        .startedAt(OffsetDateTime.now())
        .build();
    provisioning.setTenantId(request.getTenantId());
    return provisioning;
    }

    public RequestAuditLog createAuditLog(
            AccessRequest request,
            AuditAction action,
            String performedBy,
            String details) {

        RequestAuditLog log = RequestAuditLog.builder()
            .accessRequest(request)
            .action(action)
            .performedBy(performedBy)
            .details(details)
            .timestamp(OffsetDateTime.now())
            .build();

        log.setTenantId(request.getTenantId());

        return log;
    }
}