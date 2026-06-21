package com.aidentx.core.audit_management.service;

import com.aidentx.core.audit_management.model.dto.AuditLogResponse;
import com.aidentx.core.audit_management.model.entity.AuditLog;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for audit log operations.
 * Provides methods to retrieve audit logs.
 *
 * @author aidentx
 * @since 1.0
 */
public interface AuditLogService {
    /**
     * Retrieves all audit log entries.
     *
     * @return a list of all audit logs
     */
    List<AuditLog> getAllAuditLogs();
    
    /**
     * Retrieves audit logs with optional filtering.
     *
     * @param globalSearch - searches across multiple text fields (actor name, object type, event, status, risk, error details)
     * @param event - exact match filter for event field
     * @param objectType - exact match filter for object type field
     * @param risk - exact match filter for risk field
     * @param status - exact match filter for status field
     * @param dateFrom - filter logs from this timestamp (inclusive)
     * @param dateTo - filter logs until this timestamp (inclusive)
     * @return filtered list of audit logs
     */
    List<AuditLog> getFilteredAuditLogs(String globalSearch, String event, String objectType, String risk, 
                                       String status, Instant dateFrom, Instant dateTo);
    
    /**
     * Retrieves audit logs with optional filtering, pagination and sorting (newest first).
     *
     * @param globalSearch - searches across multiple text fields
     * @param event - exact match filter for event field
     * @param objectType - exact match filter for object type field
     * @param risk - exact match filter for risk field
     * @param status - exact match filter for status field
     * @param dateFrom - filter logs from this timestamp (inclusive)
     * @param dateTo - filter logs until this timestamp (inclusive)
     * @param page - page number (0-based)
     * @param size - number of items per page
     * @return paginated and filtered audit logs
     */
    Page<AuditLog> getFilteredAuditLogsPaginated(String globalSearch, String event, String objectType, String risk, 
                                               String status, Instant dateFrom, Instant dateTo, int page, int size);
    
    /**
     * Retrieves audit logs with optional filtering, pagination, sorting and metadata for dropdowns.
     *
     * @param globalSearch - searches across multiple text fields
     * @param event - exact match filter for event field
     * @param objectType - exact match filter for object type field
     * @param risk - exact match filter for risk field
     * @param status - exact match filter for status field
     * @param dateFrom - filter logs from this timestamp (inclusive)
     * @param dateTo - filter logs until this timestamp (inclusive)
     * @param page - page number (0-based)
     * @param size - number of items per page
     * @return response containing paginated logs and unique values for dropdowns
     */
    AuditLogResponse getAuditLogsWithMetadata(String globalSearch, String event, String objectType, String risk, 
                                            String status, Instant dateFrom, Instant dateTo, int page, int size);

    /** @deprecated Use the overload with {@code excludeObjectType} instead. */
    AuditLogResponse getAuditLogsWithMetadata(String globalSearch, String event, String objectType, String risk,
                                            String status, Instant dateFrom, Instant dateTo, int page, int size, String tenantId);

    /**
     * Retrieves audit logs with optional filtering, pagination, sorting and metadata for dropdowns.
     * Filters results by tenant ID for multi-tenant support.
     * Supports excluding specific object types from results.
     */
    AuditLogResponse getAuditLogsWithMetadata(String globalSearch, String event, String objectType,
                                            String excludeObjectType, String risk,
                                            String status, Instant dateFrom, Instant dateTo, int page, int size, String tenantId);
}
