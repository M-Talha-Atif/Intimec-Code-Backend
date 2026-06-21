package com.aidentx.core.audit_management.controller;

import com.aidentx.core.audit_management.model.dto.AuditLogResponse;
import com.aidentx.core.audit_management.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidentx.core.shared.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * REST controller for retrieving audit logs.
 * Provides endpoints to fetch audit log entries with optional filtering, pagination, sorting and metadata.
 * Supports multi-tenant filtering based on JWT token.
 *
 * @author aidentx
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final JwtUtil jwtUtil;

    public AuditLogController(AuditLogService auditLogService, JwtUtil jwtUtil) {
        this.auditLogService = auditLogService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Retrieves audit log entries with optional filtering, pagination, sorting and metadata.
     * Results are sorted by timestamp in descending order (newest first).
     * Also includes unique lists of events, object types, risk levels, and statuses for frontend dropdowns.
     * Automatically filters by tenant based on JWT token.
     *
     * @param search - global search across multiple fields (optional)
     * @param event - filter for event field - supports single value or comma-separated list (e.g. "assign,update") (optional)  
     * @param objectType - filter for object type field - supports single value or comma-separated list (optional)
     * @param risk - filter for risk field - supports single value or comma-separated list (optional)
     * @param status - filter for status field - supports single value or comma-separated list (optional)
     * @param dateFrom - filter logs from this timestamp (inclusive, optional, ISO-8601 format)
     * @param dateTo - filter logs until this timestamp (inclusive, optional, ISO-8601 format)
     * @param page - page number, 0-based (optional, default: 0)
     * @param size - number of records per page (optional, default: 25, min: 1, max: 1000)
     * @param request - HTTP request to extract JWT token from
     * @return a response containing paginated and filtered audit logs plus metadata for dropdowns
     */
    @GetMapping
    public ResponseEntity<AuditLogResponse> getAuditLogs(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "event", required = false) String event,
            @RequestParam(value = "objectType", required = false) String objectType,
            @RequestParam(value = "excludeObjectType", required = false) String excludeObjectType,
            @RequestParam(value = "risk", required = false) String risk,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            HttpServletRequest request) {

        // Extract tenant ID from JWT token
        String tenantId = extractTenantFromRequest(request);
        
        // Validate page size to prevent performance issues
        if (size < 1) {
            size = 1;
        } else if (size > 1000) {
            size = 1000;
        }
        
        // Validate page number
        if (page < 0) {
            page = 0;
        }
        
        // Get logs with metadata for dropdown population, filtered by tenant
        AuditLogResponse response = auditLogService.getAuditLogsWithMetadata(
            search, event, objectType, excludeObjectType, risk, status, dateFrom, dateTo, page, size, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts tenant ID from JWT token in the Authorization header.
     *
     * @param request HTTP servlet request containing Authorization header
     * @return tenant ID extracted from JWT token
     * @throws RuntimeException if JWT token is missing or invalid
     */
    private String extractTenantFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header is missing or invalid");
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return jwtUtil.extractTenantId(token);
    }
}
