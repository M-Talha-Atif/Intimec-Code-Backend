package com.aidentx.core.audit_management.service.impl;

import com.aidentx.core.audit_management.model.dto.AuditLogResponse;
import com.aidentx.core.audit_management.model.entity.AuditLog;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AuditLogService;
import com.aidentx.core.audit_management.specification.AuditLogSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of the AuditLogService interface.
 * Provides methods to retrieve audit logs from the repository.
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Retrieves all audit log entries from the repository.
     *
     * @return a list of all audit logs
     */
    @Override
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }
    
    /**
     * Retrieves audit logs with optional filtering.
     * Empty or null filter parameters are ignored.
     *
     * @param globalSearch - searches across multiple text fields
     * @param event - exact match filter for event field
     * @param objectType - exact match filter for object type field
     * @param risk - exact match filter for risk field
     * @param status - exact match filter for status field
     * @param dateFrom - filter logs from this timestamp (inclusive)
     * @param dateTo - filter logs until this timestamp (inclusive)
     * @return filtered list of audit logs
     */
    @Override
    public List<AuditLog> getFilteredAuditLogs(String globalSearch, String event, String objectType, String risk, 
                                              String status, Instant dateFrom, Instant dateTo) {
        // Build combined specification
        Specification<AuditLog> spec = AuditLogSpecification.hasGlobalSearch(globalSearch)
                .and(AuditLogSpecification.hasEvent(event))
                .and(AuditLogSpecification.hasObjectType(objectType))
                .and(AuditLogSpecification.hasRisk(risk))
                .and(AuditLogSpecification.hasStatus(status))
                .and(AuditLogSpecification.hasTimestampFrom(dateFrom))
                .and(AuditLogSpecification.hasTimestampTo(dateTo));
        
        return auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp"));
    }
    
    /**
     * Retrieves audit logs with optional filtering, pagination and sorting (newest first).
     * Empty or null filter parameters are ignored.
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
     * @return paginated and filtered audit logs sorted by timestamp DESC
     */
    @Override
    public Page<AuditLog> getFilteredAuditLogsPaginated(String globalSearch, String event, String objectType, String risk, 
                                                       String status, Instant dateFrom, Instant dateTo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        // Build combined specification to handle all filters together
        Specification<AuditLog> spec = AuditLogSpecification.hasGlobalSearch(globalSearch)
                .and(AuditLogSpecification.hasEvent(event))
                .and(AuditLogSpecification.hasObjectType(objectType))
                .and(AuditLogSpecification.hasRisk(risk))
                .and(AuditLogSpecification.hasStatus(status))
                .and(AuditLogSpecification.hasTimestampFrom(dateFrom))
                .and(AuditLogSpecification.hasTimestampTo(dateTo));
        
        return auditLogRepository.findAll(spec, pageable);
    }
    
    /**
     * Retrieves audit logs with optional filtering, pagination, sorting and metadata for dropdowns.
     * Empty or null filter parameters are ignored.
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
    @Override
    public AuditLogResponse getAuditLogsWithMetadata(String globalSearch, String event, String objectType, String risk, 
                                                    String status, Instant dateFrom, Instant dateTo, int page, int size) {
        // Get paginated and filtered results
        Page<AuditLog> logs = getFilteredAuditLogsPaginated(globalSearch, event, objectType, risk, status, dateFrom, dateTo, page, size);
        
        // Get unique values for dropdowns
        List<String> availableEvents = auditLogRepository.findDistinctEvents();
        List<String> availableObjectTypes = auditLogRepository.findDistinctObjectTypes();
        List<String> availableRisks = auditLogRepository.findDistinctRisks();
        List<String> availableStatuses = auditLogRepository.findDistinctStatuses();
        
        return new AuditLogResponse(logs, availableEvents, availableObjectTypes, availableRisks, availableStatuses);
    }

    /**
     * Retrieves audit logs with optional filtering, pagination, sorting and metadata for dropdowns.
     * Filters results by tenant ID for multi-tenant support.
     * Empty or null filter parameters are ignored.
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
     * @param tenantId - tenant ID to filter logs by
     * @return response containing paginated logs and unique values for dropdowns filtered by tenant
     */
    @Override
    public AuditLogResponse getAuditLogsWithMetadata(String globalSearch, String event, String objectType, String risk, 
                                                    String status, Instant dateFrom, Instant dateTo, int page, int size, String tenantId) {
        return getAuditLogsWithMetadata(globalSearch, event, objectType, null, risk, status, dateFrom, dateTo, page, size, tenantId);
    }

    @Override
    public AuditLogResponse getAuditLogsWithMetadata(String globalSearch, String event, String objectType,
                                                    String excludeObjectType, String risk,
                                                    String status, Instant dateFrom, Instant dateTo, int page, int size, String tenantId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Specification<AuditLog> spec = AuditLogSpecification.hasGlobalSearch(globalSearch)
                .and(AuditLogSpecification.hasEvent(event))
                .and(AuditLogSpecification.hasObjectType(objectType))
                .and(AuditLogSpecification.excludeObjectTypes(excludeObjectType))
                .and(AuditLogSpecification.hasRisk(risk))
                .and(AuditLogSpecification.hasStatus(status))
                .and(AuditLogSpecification.hasTimestampFrom(dateFrom))
                .and(AuditLogSpecification.hasTimestampTo(dateTo))
                .and(AuditLogSpecification.hasTenantId(tenantId));
        
        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        
        List<String> availableEvents = auditLogRepository.findDistinctEventsByTenantId(tenantId);
        List<String> availableObjectTypes = auditLogRepository.findDistinctObjectTypesByTenantId(tenantId);
        List<String> availableRisks = auditLogRepository.findDistinctRisksByTenantId(tenantId);
        List<String> availableStatuses = auditLogRepository.findDistinctStatusesByTenantId(tenantId);
        
        return new AuditLogResponse(logs, availableEvents, availableObjectTypes, availableRisks, availableStatuses);
    }
}
