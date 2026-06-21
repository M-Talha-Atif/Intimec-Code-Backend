package com.aidentx.applicationpermissions.roles_management.controller;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRuleExecutionHistory;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleExecutionHistoryRepository;
import com.aidentx.applicationpermissions.roles_management.service.ScheduledDynamicRuleExecutionService;
import com.aidentx.applicationpermissions.roles_management.service.impl.DynamicRuleSchedulerHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for dynamic rule scheduler monitoring and management.
 * 
 * <p>This controller provides comprehensive monitoring and management endpoints
 * for the scheduled dynamic rule execution system. It includes:</p>
 * <ul>
 *   <li>Health status and performance metrics</li>
 *   <li>Execution history queries and analytics</li>
 *   <li>Manual scheduler control and circuit breaker management</li>
 *   <li>System diagnostics and troubleshooting endpoints</li>
 * </ul>
 * 
 * <p>All endpoints require appropriate authorization and are designed for
 * operations teams, system administrators, and monitoring systems.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/dynamic-rules/scheduler")
@Tag(name = "Dynamic Rule Scheduler", description = "Monitoring and management endpoints for scheduled dynamic rule execution")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
public class DynamicRuleSchedulerController {

    private final ScheduledDynamicRuleExecutionService scheduledExecutionService;
    private final DynamicRuleSchedulerHealthService healthService;
    private final DynamicRuleExecutionHistoryRepository executionHistoryRepository;

    @Autowired
    public DynamicRuleSchedulerController(
            ScheduledDynamicRuleExecutionService scheduledExecutionService,
            DynamicRuleSchedulerHealthService healthService,
            DynamicRuleExecutionHistoryRepository executionHistoryRepository) {
        this.scheduledExecutionService = scheduledExecutionService;
        this.healthService = healthService;
        this.executionHistoryRepository = executionHistoryRepository;
    }

    /**
     * Get comprehensive health status of the dynamic rule scheduler.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Get scheduler health status",
        description = "Returns comprehensive health information including circuit breaker status, recent execution metrics, and system performance indicators"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Health status retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = healthService.getHealthStatus();
        Map<String, Object> response = new HashMap<>();
        
        response.putAll(health);
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current system status with real-time metrics.
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get real-time system status",
        description = "Returns current system status including circuit breaker state, recent execution counts, and database health"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System status retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = healthService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Manually trigger scheduled rule execution.
     */
    @PostMapping("/trigger")
    @Operation(
        summary = "Manually trigger scheduled execution",
        description = "Manually triggers the scheduled rule execution process. Useful for testing, debugging, or immediate execution needs"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scheduled execution triggered successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "503", description = "Service unavailable (circuit breaker open)")
    })
    public ResponseEntity<Map<String, Object>> triggerScheduledExecution() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (scheduledExecutionService.isCircuitBreakerOpen()) {
                response.put(AppConstants.SCHEDULER_RESPONSE_KEY_STATUS, AppConstants.SCHEDULER_STATUS_ERROR);
                response.put(AppConstants.SCHEDULER_RESPONSE_KEY_MESSAGE, AppConstants.SCHEDULER_MSG_CANNOT_TRIGGER_CIRCUIT_BREAKER_OPEN);
                response.put(AppConstants.SCHEDULER_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN, true);
                return ResponseEntity.status(503).body(response);
            }
            
            scheduledExecutionService.triggerScheduledExecution();
            
            response.put(AppConstants.SCHEDULER_RESPONSE_KEY_STATUS, AppConstants.SCHEDULER_STATUS_SUCCESS);
            response.put(AppConstants.SCHEDULER_RESPONSE_KEY_MESSAGE, AppConstants.SCHEDULER_MSG_EXECUTION_TRIGGERED_SUCCESSFULLY);
            response.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put(AppConstants.SCHEDULER_RESPONSE_KEY_STATUS, AppConstants.SCHEDULER_STATUS_ERROR);
            response.put(AppConstants.SCHEDULER_RESPONSE_KEY_MESSAGE, String.format(AppConstants.SCHEDULER_MSG_FAILED_TO_TRIGGER_EXECUTION, e.getMessage()));
            response.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get execution history with optional filtering.
     */
    @GetMapping("/history")
    @Operation(
        summary = "Get execution history",
        description = "Retrieve execution history with optional date range and tenant filtering for monitoring and analytics"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execution history retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getExecutionHistory(
            @Parameter(description = "Start date for history query (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startDate,
            
            @Parameter(description = "End date for history query (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endDate,
            
            @Parameter(description = "Tenant ID filter")
            @RequestParam(required = false) 
            String tenantId,
            
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(defaultValue = "100") 
            int limit) {
        
        // Set default date range if not provided
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(AppConstants.SCHEDULER_DEFAULT_HISTORY_DAYS); // Default to last 7 days
        }
        
        // Validate date range
        if (startDate.isAfter(endDate)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(AppConstants.SCHEDULER_RESPONSE_KEY_STATUS, AppConstants.SCHEDULER_STATUS_ERROR);
            errorResponse.put(AppConstants.SCHEDULER_RESPONSE_KEY_MESSAGE, AppConstants.SCHEDULER_MSG_START_DATE_BEFORE_END_DATE);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        List<DynamicRuleExecutionHistory> history;
        
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            history = executionHistoryRepository.findByTenantIdAndExecutionDateBetween(
                tenantId, startDate, endDate);
        } else {
            history = executionHistoryRepository.findByExecutionDateBetween(startDate, endDate);
        }
        
        // Limit results to prevent large responses
        if (history.size() > limit) {
            history = history.subList(0, limit);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_HISTORY, history);
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_TOTAL_RECORDS, history.size());
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_START_DATE, startDate.toString());
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_END_DATE, endDate.toString());
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_TENANT_ID, tenantId);
        response.put(AppConstants.SCHEDULER_RESPONSE_KEY_LIMITED, history.size() == limit);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get execution statistics for monitoring dashboards.
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get execution statistics",
        description = "Retrieve aggregated execution statistics for monitoring dashboards and performance analysis"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getExecutionStatistics(
            @Parameter(description = "Hours to look back for statistics")
            @RequestParam(defaultValue = "24") 
            int hoursBack,
            
            @Parameter(description = "Tenant ID filter")
            @RequestParam(required = false) 
            String tenantId) {
        
        LocalDateTime startTime = LocalDateTime.now().minusHours(hoursBack);
        LocalDateTime endTime = LocalDateTime.now();
        
        List<DynamicRuleExecutionHistory> executions;
        
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            executions = executionHistoryRepository.findByTenantIdAndExecutionDateBetween(
                tenantId, startTime, endTime);
        } else {
            executions = executionHistoryRepository.findByExecutionDateBetween(startTime, endTime);
        }
        
        // Calculate statistics
        long totalExecutions = executions.size();
        long successfulExecutions = executions.stream()
            .mapToLong(exec -> AppConstants.SCHEDULER_EXECUTION_STATUS_SUCCESS.equals(exec.getExecutionStatus()) ? 1 : 0)
            .sum();
        long failedExecutions = executions.stream()
            .mapToLong(exec -> AppConstants.SCHEDULER_EXECUTION_STATUS_FAILED.equals(exec.getExecutionStatus()) ? 1 : 0)
            .sum();
        
        double avgExecutionTime = executions.stream()
            .mapToLong(DynamicRuleExecutionHistory::getExecutionDurationMs)
            .average()
            .orElse(0.0);
        
        long totalUsersProcessed = executions.stream()
            .mapToLong(DynamicRuleExecutionHistory::getTotalUsersEvaluated)
            .sum();
        
        long totalAssignments = executions.stream()
            .mapToLong(DynamicRuleExecutionHistory::getSuccessfulAssignments)
            .sum();
        
        double successRate = totalExecutions > 0 ? 
            (double) successfulExecutions / totalExecutions * 100 : 0;
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIME_WINDOW, hoursBack + AppConstants.SCHEDULER_TIME_WINDOW_HOURS_SUFFIX);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TOTAL_EXECUTIONS, totalExecutions);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_SUCCESSFUL_EXECUTIONS, successfulExecutions);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_FAILED_EXECUTIONS, failedExecutions);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_SUCCESS_RATE, Math.round(successRate * 100.0) / 100.0);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_AVG_EXECUTION_TIME_MS, Math.round(avgExecutionTime));
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TOTAL_USERS_PROCESSED, totalUsersProcessed);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TOTAL_ROLE_ASSIGNMENTS, totalAssignments);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TENANT_ID, tenantId);
        statistics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get circuit breaker status and metrics.
     */
    @GetMapping("/circuit-breaker")
    @Operation(
        summary = "Get circuit breaker status",
        description = "Returns current circuit breaker state and related metrics for system monitoring"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Circuit breaker status retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put(AppConstants.SCHEDULER_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN, scheduledExecutionService.isCircuitBreakerOpen());
        status.put(AppConstants.SCHEDULER_RESPONSE_KEY_CONSECUTIVE_FAILURES, scheduledExecutionService.getConsecutiveFailures());
        status.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        
        return ResponseEntity.ok(status);
    }

    /**
     * Get system performance metrics.
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get performance metrics",
        description = "Returns detailed performance metrics for system optimization and capacity planning"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Recent failed executions
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(AppConstants.SCHEDULER_METRICS_LOOKBACK_HOURS);
        List<DynamicRuleExecutionHistory> recentFailures = 
            executionHistoryRepository.findFailedExecutionsSince(oneHourAgo);
        
        // Long-running executions (>30 seconds)
        List<DynamicRuleExecutionHistory> longRunningExecutions = 
            executionHistoryRepository.findLongRunningExecutions(AppConstants.SCHEDULER_LONG_RUNNING_THRESHOLD_MS, oneHourAgo);
        
        metrics.put(AppConstants.SCHEDULER_RESPONSE_KEY_RECENT_FAILURES, recentFailures.size());
        metrics.put(AppConstants.SCHEDULER_RESPONSE_KEY_LONG_RUNNING_EXECUTIONS, longRunningExecutions.size());
        metrics.put(AppConstants.SCHEDULER_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN, scheduledExecutionService.isCircuitBreakerOpen());
        metrics.put(AppConstants.SCHEDULER_RESPONSE_KEY_CONSECUTIVE_FAILURES, scheduledExecutionService.getConsecutiveFailures());
        metrics.put(AppConstants.SCHEDULER_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        
        return ResponseEntity.ok(metrics);
    }
}