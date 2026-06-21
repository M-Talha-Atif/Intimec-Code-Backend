package com.aidentx.access_portal_service.repository.access_request;

import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.entity.access_request.AccessRequestItem;
import com.aidentx.access_portal_service.entity.access_request.RequestApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequestApprovalStepRepository extends JpaRepository<RequestApprovalStep, UUID> {

    List<RequestApprovalStep> findByAccessRequestIdOrderByStepOrder(UUID accessRequestId);

    List<RequestApprovalStep> findByAccessRequestIdAndTenantId(UUID accessRequestId, String tenantId);

    /**
     * Find all approval steps for a given access request
     */
    List<RequestApprovalStep> findByAccessRequest(AccessRequest accessRequest);

      /**
     * Find all approval steps for a specific access request item, ordered by step order
     */
    List<RequestApprovalStep> findByRequestItemOrderByStepOrder(AccessRequestItem item);

    Optional<RequestApprovalStep> findByRequestItemAndStepOrder(
       AccessRequestItem item,
       Integer stepOrder
   );

   Optional<RequestApprovalStep> findByRequestItemAndApproverRole(
       AccessRequestItem item,
       String approverRole
   );

   Optional<RequestApprovalStep> findByRequestItemAndApproverRoleAndTargetUserIdAndAssignedApproverId(
    AccessRequestItem item,
    String approverRole,
    String targetUserId,
    String assignedApproverId
);

   // Keep request-level queries if needed for reporting:
   List<RequestApprovalStep> findByAccessRequestId(UUID accessRequestId);
}