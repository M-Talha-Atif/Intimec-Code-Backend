package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRuleExecutionHistory;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleExecutionHistoryRepository;
import com.aidentx.applicationpermissions.roles_management.service.ScheduledDynamicRuleExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health check and monitoring service for the dynamic rule scheduler.
 * 
 * <p>This service provides comprehensive health monitoring and cleanup operations
 * for the scheduled dynamic rule execution system. It includes:</p>
 * <ul>
 *   <li>Health status reporting for external monitoring systems</li>
 *   <li>Execution history cleanup for data retention compliance</li>
 *   <li>Performance metrics and alerting thresholds</li>
 *   <li>System resource monitoring and optimization recommendations</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class DynamicRuleSchedulerHealthService {

    private final ScheduledDynamicRuleExecutionService scheduledExecutionService;
    private final DynamicRuleExecutionHistoryRepository executionHistoryRepository;
    
    @Value("${dynamic-rules.scheduler.history.retention-days:90}")
    private int retentionDays;
    
    @Value("${dynamic-rules.scheduler.monitoring.failure-rate-threshold-percent:25}")
    private int failureRateThreshold;
    
    @Value("${dynamic-rules.scheduler.monitoring.monitoring-window-minutes:60}")
    private int monitoringWindowMinutes;

    @Autowired
    public DynamicRuleSchedulerHealthService(
            ScheduledDynamicRuleExecutionService scheduledExecutionService,
            DynamicRuleExecutionHistoryRepository executionHistoryRepository) {
        this.scheduledExecutionService = scheduledExecutionService;
        this.executionHistoryRepository = executionHistoryRepository;
    }

    /**
     * Health check method that returns system health status.
     * Provides detailed health status for monitoring systems.
     */
    public Map<String, Object> getHealthStatus() {
        try {
            Map<String, Object> healthDetails = new HashMap<>();
            
            // Check circuit breaker status
            boolean circuitBreakerOpen = scheduledExecutionService.isCircuitBreakerOpen();
            int consecutiveFailures = scheduledExecutionService.getConsecutiveFailures();
            
            // Get recent execution statistics
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(monitoringWindowMinutes);
            
            // Circuit breaker status
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN, circuitBreakerOpen);
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_CONSECUTIVE_FAILURES, consecutiveFailures);
            
            // Recent execution metrics
            List<DynamicRuleExecutionHistory> recentExecutions = 
                executionHistoryRepository.findByExecutionDateBetween(windowStart, LocalDateTime.now());
            
            long totalExecutions = recentExecutions.size();
            long failedExecutions = recentExecutions.stream()
                .mapToLong(exec -> AppConstants.EXECUTION_STATUS_FAILED.equals(exec.getExecutionStatus()) ? 1 : 0)
                .sum();
            
            double failureRate = totalExecutions > 0 ? (double) failedExecutions / totalExecutions * 100 : 0;
            
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_TOTAL_EXECUTIONS, totalExecutions);
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_FAILED_EXECUTIONS, failedExecutions);
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_FAILURE_RATE_PERCENT, Math.round(failureRate * 100.0) / 100.0);
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_MONITORING_WINDOW_MINUTES, monitoringWindowMinutes);
            
            // Determine overall health status
            if (circuitBreakerOpen) {
                healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_DOWN);
                healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_REASON, AppConstants.HEALTH_REASON_CIRCUIT_BREAKER_OPEN);
                return healthDetails;
            }
            
            if (failureRate > failureRateThreshold) {
                healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_DEGRADED);
                healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_REASON, AppConstants.HEALTH_REASON_HIGH_FAILURE_RATE + failureRate + AppConstants.HEALTH_PERCENT_SUFFIX);
                return healthDetails;
            }
            
            if (consecutiveFailures > 2) {
                healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_DEGRADED);
                healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_REASON, AppConstants.HEALTH_REASON_MULTIPLE_CONSECUTIVE_FAILURES);
                return healthDetails;
            }
            
            healthDetails.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_UP);
            return healthDetails;
                
        } catch (Exception e) {
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_DOWN);
            errorStatus.put(AppConstants.HEALTH_RESPONSE_KEY_ERROR, AppConstants.HEALTH_ERROR_CHECK_FAILED + e.getMessage());
            return errorStatus;
        }
    }

    /**
     * Cleanup old execution history records for data retention compliance.
     * Runs daily at 2 AM to maintain optimal database performance.
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupOldExecutionHistory() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            
            // Get count before deletion for logging
            long recordsToDelete = executionHistoryRepository.countExecutionsOlderThan(cutoffDate);
            
            if (recordsToDelete > 0) {
                executionHistoryRepository.deleteExecutionsOlderThan(cutoffDate);
                
                // Log cleanup activity
                System.out.println(AppConstants.HEALTH_LOG_CLEANUP_COMPLETED + recordsToDelete + 
                                 AppConstants.HEALTH_LOG_CLEANUP_RECORDS_OLDER_THAN + retentionDays + AppConstants.HEALTH_LOG_CLEANUP_DAYS);
            }
            
        } catch (Exception e) {
            System.err.println(AppConstants.HEALTH_LOG_CLEANUP_FAILED + e.getMessage());
        }
    }

    /**
     * Generate performance report for monitoring dashboards.
     * Runs every hour to provide up-to-date system metrics.
     * DISABLED: Not needed with CDC approach for IMMEDIATE rules
     */
    // @Scheduled(fixedRate = 3600000) // Every hour
    public void generatePerformanceReport() {
        try {
            LocalDateTime reportStart = LocalDateTime.now().minusHours(24);
            LocalDateTime reportEnd = LocalDateTime.now();
            
            List<DynamicRuleExecutionHistory> executions = 
                executionHistoryRepository.findByExecutionDateBetween(reportStart, reportEnd);
            
            if (executions.isEmpty()) {
                return;
            }
            
            // Calculate performance metrics
            long totalExecutions = executions.size();
            long successfulExecutions = executions.stream()
                .mapToLong(exec -> AppConstants.EXECUTION_STATUS_SUCCESS.equals(exec.getExecutionStatus()) ? 1 : 0)
                .sum();
            
            double avgExecutionTime = executions.stream()
                .mapToLong(DynamicRuleExecutionHistory::getExecutionDurationMs)
                .average()
                .orElse(0.0);
            
            long totalUsersProcessed = executions.stream()
                .mapToLong(DynamicRuleExecutionHistory::getTotalUsersEvaluated)
                .sum();
            
            long totalRoleAssignments = executions.stream()
                .mapToLong(DynamicRuleExecutionHistory::getSuccessfulAssignments)
                .sum();
            
            // Log performance metrics (these could be sent to monitoring systems)
            System.out.println(AppConstants.HEALTH_LOG_PERFORMANCE_REPORT_TITLE);
            System.out.println(AppConstants.HEALTH_LOG_TOTAL_EXECUTIONS + totalExecutions);
            System.out.println(AppConstants.HEALTH_LOG_SUCCESS_RATE + (totalExecutions > 0 ? 
                Math.round((double) successfulExecutions / totalExecutions * 100 * 100.0) / 100.0 : 0) + AppConstants.HEALTH_PERCENT_SUFFIX);
            System.out.println(AppConstants.HEALTH_LOG_AVG_EXECUTION_TIME + Math.round(avgExecutionTime) + AppConstants.HEALTH_MS_SUFFIX);
            System.out.println(AppConstants.HEALTH_LOG_TOTAL_USERS_PROCESSED + totalUsersProcessed);
            System.out.println(AppConstants.HEALTH_LOG_TOTAL_ROLE_ASSIGNMENTS + totalRoleAssignments);
            
        } catch (Exception e) {
            System.err.println(AppConstants.HEALTH_LOG_PERFORMANCE_REPORT_FAILED + e.getMessage());
        }
    }

    /**
     * Get current system status for API endpoints.
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Circuit breaker status
            status.put(AppConstants.HEALTH_RESPONSE_KEY_CIRCUIT_BREAKER_OPEN, scheduledExecutionService.isCircuitBreakerOpen());
            status.put(AppConstants.HEALTH_RESPONSE_KEY_CONSECUTIVE_FAILURES, scheduledExecutionService.getConsecutiveFailures());
            
            // Recent execution count
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentExecutions = executionHistoryRepository.countExecutionsSince(oneHourAgo);
            status.put(AppConstants.HEALTH_RESPONSE_KEY_EXECUTIONS_LAST_HOUR, recentExecutions);
            
            // Database health
            long totalHistoryRecords = executionHistoryRepository.count();
            status.put(AppConstants.HEALTH_RESPONSE_KEY_TOTAL_HISTORY_RECORDS, totalHistoryRecords);
            
            status.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_HEALTHY);
            status.put(AppConstants.HEALTH_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
            
        } catch (Exception e) {
            status.put(AppConstants.HEALTH_RESPONSE_KEY_STATUS, AppConstants.HEALTH_STATUS_ERROR);
            status.put(AppConstants.HEALTH_RESPONSE_KEY_ERROR, e.getMessage());
            status.put(AppConstants.HEALTH_RESPONSE_KEY_TIMESTAMP, LocalDateTime.now().toString());
        }
        
        return status;
    }
}