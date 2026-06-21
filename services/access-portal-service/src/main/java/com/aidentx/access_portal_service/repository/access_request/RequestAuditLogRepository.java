package com.aidentx.access_portal_service.repository.access_request;

import com.aidentx.access_portal_service.entity.access_request.RequestAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, UUID> {

    List<RequestAuditLog> findByAccessRequestIdOrderByTimestampDesc(UUID accessRequestId);

    List<RequestAuditLog> findByAccessRequestIdAndTenantId(UUID accessRequestId, String tenantId);


    List<RequestAuditLog> findByPerformedByAndTenantId(String performedBy, String tenantId);

    List<RequestAuditLog> findByAccessRequestIdAndTimestampBetween(
        UUID accessRequestId,
        OffsetDateTime startTime,
        OffsetDateTime endTime
    );
}