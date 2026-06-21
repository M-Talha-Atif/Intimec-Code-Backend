package com.aidentx.access_portal_service.repository.access_request;

import com.aidentx.access_portal_service.entity.access_request.AccessRequest;
import com.aidentx.access_portal_service.entity.access_request.AccessRequestItem;
import com.aidentx.access_portal_service.entity.access_request.RequestProvisioning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequestProvisioningRepository extends JpaRepository<RequestProvisioning, UUID> {

    List<RequestProvisioning> findByRequestItemIdAndTenantId(UUID requestItemId, String tenantId);

    List<RequestProvisioning> findByTargetSystemAndTenantId(String targetSystem, String tenantId);

    /**
     * Find all provisioning records for a given access request
     * Joins through requestItem to get all provisioning for the entire request
     */
    @Query("""
        SELECT p FROM RequestProvisioning p
        JOIN p.requestItem ri
        WHERE ri.accessRequest = :accessRequest
        """)
    List<RequestProvisioning> findByAccessRequest(@Param("accessRequest") AccessRequest accessRequest);

    /**
     * Find a specific provisioning record by request, role, and target user
     * Used during event processing to update provisioning status
     */
    @Query("""
        SELECT p FROM RequestProvisioning p
        WHERE p.requestItem = :requestItem
        AND p.targetUserId = :targetUserId
        """)
    Optional<RequestProvisioning> findByRequestItemAndTargetUserId(
        @Param("requestItem") AccessRequestItem requestItem,
        @Param("targetUserId") String targetUserId
    );
}