package com.aidentx.applicationpermissions.roles_management.service.impl;

import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRule;
import com.aidentx.applicationpermissions.roles_management.model.entity.DynamicRuleExecutionHistory;
import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleExecutionHistoryRepository;
import com.aidentx.applicationpermissions.roles_management.repository.DynamicRuleRepository;
import com.aidentx.applicationpermissions.roles_management.repository.RoleRepository;
import com.aidentx.applicationpermissions.roles_management.service.DynamicRuleService;
import com.aidentx.applicationpermissions.roles_management.service.ScheduledDynamicRuleExecutionService;
import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Production-ready multi-threaded scheduled execution service for dynamic rules.
 * 
 * <p>This service implements a sophisticated scheduling system that:</p>
 * <ul>
 *   <li>Executes scheduled dynamic rules (DAILY, WEEKLY, MONTHLY) every minute</li>
 *   <li>Provides tenant-level parallelization for optimal performance</li>
 *   <li>Prevents duplicate executions using execution history</li>
 *   <li>Implements circuit breaker patterns for resilience</li>
 *   <li>Provides comprehensive monitoring and metrics</li>
 *   <li>Handles graceful degradation under load</li>
 * </ul>
 * 
 * <p>Architecture Features:</p>
 * <ul>
 *   <li>Thread Pool: Configurable size with work-stealing queues</li>
 *   <li>Tenant Isolation: Each tenant executes in separate thread context</li>
 *   <li>Circuit Breaker: Automatic failure detection and recovery</li>
 *   <li>Execution History: Prevents duplicate processing within schedule periods</li>
 *   <li>Metrics: Comprehensive monitoring via Micrometer</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class ScheduledDynamicRuleExecutionServiceImpl implements ScheduledDynamicRuleExecutionService {

    private final IUnifiedLogger logger;
    
    private final DynamicRuleRepository dynamicRuleRepository;
    private final DynamicRuleExecutionHistoryRepository executionHistoryRepository;
    private final DynamicRuleService dynamicRuleService;
    private final RoleRepository roleRepository;
    
    // Thread Pool Configuration
    private final ExecutorService scheduledExecutionPool;
    private final ScheduledExecutorService mainScheduler;
    
    // CRITICAL: Lock map to prevent concurrent execution of the same rule
    private final ConcurrentHashMap<UUID, Object> ruleExecutionLocks = new ConcurrentHashMap<>();
    
    // Circuit Breaker State
    private volatile boolean circuitBreakerOpen = false;
    private volatile int consecutiveFailures = 0;
    private volatile LocalDateTime lastFailureTime;
    
    // Metrics
    private final Counter scheduledExecutionsTotal;
    private final Counter scheduledExecutionsFailed;
    private final Timer executionDuration;
    private final Counter circuitBreakerTrips;
    
    // Configuration
    @Value("${dynamic-rules.scheduler.thread-pool-size:10}")
    private int threadPoolSize;
    
    @Value("${dynamic-rules.scheduler.max-consecutive-failures:5}")
    private int maxConsecutiveFailures;
    
    @Value("${dynamic-rules.scheduler.circuit-breaker-timeout-minutes:5}")
    private int circuitBreakerTimeoutMinutes;
    
    @Value("${dynamic-rules.scheduler.execution-timeout-minutes:10}")
    private int executionTimeoutMinutes;

    @Autowired
    public ScheduledDynamicRuleExecutionServiceImpl(
            DynamicRuleRepository dynamicRuleRepository,
            DynamicRuleExecutionHistoryRepository executionHistoryRepository,
            DynamicRuleService dynamicRuleService,
            RoleRepository roleRepository,
            MeterRegistry meterRegistry,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        
        this.logger = unifiedLoggerFactory.getLogger(ScheduledDynamicRuleExecutionServiceImpl.class);
        this.dynamicRuleRepository = dynamicRuleRepository;
        this.executionHistoryRepository = executionHistoryRepository;
        this.dynamicRuleService = dynamicRuleService;
        this.roleRepository = roleRepository;
        
        // Initialize thread pools
        this.scheduledExecutionPool = ForkJoinPool.commonPool();
        this.mainScheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize metrics
        this.scheduledExecutionsTotal = Counter.builder("dynamic_rules_scheduled_executions_total")
                .description("Total number of scheduled dynamic rule executions")
                .register(meterRegistry);
        
        this.scheduledExecutionsFailed = Counter.builder("dynamic_rules_scheduled_executions_failed")
                .description("Number of failed scheduled dynamic rule executions")
                .register(meterRegistry);
        
        this.executionDuration = Timer.builder("dynamic_rules_scheduled_execution_duration")
                .description("Duration of scheduled dynamic rule executions")
                .register(meterRegistry);
        
        this.circuitBreakerTrips = Counter.builder("dynamic_rules_circuit_breaker_trips")
                .description("Number of circuit breaker trips")
                .register(meterRegistry);
    }

    /**
     * Main scheduled method that runs every minute to check for rules due for execution.
     * Uses cron expression for precise timing: every minute, every hour, every day.
     * NOTE: Temporarily disabled - IMMEDIATE rules now handled by CDC listener
     */
    // @Scheduled(cron = "0 * * * * *") // Every minute at second 0
    @Transactional(readOnly = true)
    public void executeScheduledRules() {
        LocalDateTime now = LocalDateTime.now();
        logger.info()
                .message(AppConstants.SCHEDULER_MESSAGE_CHECKING_RULES)
                .detail(AppConstants.DETAIL_KEY_EXECUTION_TIME, now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .objectType(AppConstants.LOG_OBJECT_TYPE_SCHEDULER_EXECUTION)
                .log();
        
        if (circuitBreakerOpen && !isCircuitBreakerRecoveryTime()) {
            logger.warn()
                    .message(AppConstants.SCHEDULER_MESSAGE_CIRCUIT_BREAKER_OPEN)
                    .detail(AppConstants.DETAIL_KEY_CIRCUIT_BREAKER_STATE, "OPEN")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CIRCUIT_BREAKER)
                    .log();
            return;
        }
        logger.info()
                .message(AppConstants.SCHEDULER_MESSAGE_STARTING_EXECUTION_CHECK)
                .detail(AppConstants.DETAIL_KEY_EXECUTION_TIME, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .objectType(AppConstants.LOG_OBJECT_TYPE_SCHEDULER_EXECUTION)
                .log();

        Timer.Sample sample = Timer.start();
        
        try {
            // Get all active scheduled rules (DAILY, WEEKLY, MONTHLY)
            // IMPORTANT: IMMEDIATE rules are now handled by CDC (Change Data Capture) via PostgreSQL LISTEN/NOTIFY
            // This ensures true real-time evaluation when user data changes, instead of polling every minute
            List<DynamicRule> scheduledRules = dynamicRuleRepository.findAllActiveScheduledRules();
            
            if (scheduledRules.isEmpty()) {
                logger.debug()
                        .message(AppConstants.SCHEDULER_MESSAGE_NO_ACTIVE_RULES)
                        .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EVALUATION)
                        .log();
                return;
            }
            
            logger.trace()
                    .message(AppConstants.SCHEDULER_MESSAGE_FOUND_ACTIVE_RULES)
                    .detail(AppConstants.DETAIL_KEY_SCHEDULED_RULES_COUNT, String.valueOf(scheduledRules.size()))
                    .detail(AppConstants.DETAIL_KEY_TOTAL_RULES_COUNT, String.valueOf(scheduledRules.size()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EVALUATION)
                    .log();

            // Filter rules that are due for execution (scheduled rules at their configured time)
            List<DynamicRule> rulesDueForExecution = scheduledRules.parallelStream()
                    .filter(rule -> isRuleDueForExecution(rule, now))
                    .collect(Collectors.toList());

            if (rulesDueForExecution.isEmpty()) {
                logger.debug()
                        .message(AppConstants.SCHEDULER_MESSAGE_NO_RULES_DUE)
                        .detail(AppConstants.DETAIL_KEY_EXECUTION_TIME, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EVALUATION)
                        .log();
                return;
            }

            logger.info()
                    .message(AppConstants.SCHEDULER_MESSAGE_FOUND_RULES_DUE)
                    .detail(AppConstants.DETAIL_KEY_RULES_COUNT, String.valueOf(rulesDueForExecution.size()))
                    .detail(AppConstants.DETAIL_KEY_EXECUTION_TIME, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EVALUATION)
                    .log();

            // Group rules by tenant for parallel execution
            Map<String, List<DynamicRule>> rulesByTenant = rulesDueForExecution.stream()
                    .collect(Collectors.groupingBy(rule -> {
                        // Get tenant from the role entity using the repository
                        Optional<Role> roleOpt = roleRepository.findById(rule.getRoleId());
                        if (roleOpt.isPresent()) {
                            return roleOpt.get().getTenantId();
                        } else {
                            logger.warn()
                                    .message(AppConstants.SCHEDULER_MESSAGE_ROLE_NOT_FOUND)
                                    .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, "default-tenant")
                                    .objectType(AppConstants.LOG_OBJECT_TYPE_TENANT_CONTEXT)
                                    .log();
                            return "default-tenant";
                        }
                    }));

            // Execute rules in parallel by tenant
            executeRulesInParallel(rulesByTenant, now);
            
            // Reset circuit breaker on successful execution
            resetCircuitBreaker();
            
        } catch (Exception e) {
            handleExecutionFailure(e);
        } finally {
            sample.stop(executionDuration);
        }
    }

    /**
     * Execute rules in parallel, grouped by tenant for isolation and performance.
     */
    private void executeRulesInParallel(Map<String, List<DynamicRule>> rulesByTenant, LocalDateTime executionTime) {
        List<CompletableFuture<Void>> tenantExecutions = rulesByTenant.entrySet().stream()
                .map(entry -> executeRulesForTenant(entry.getKey(), entry.getValue(), executionTime))
                .collect(Collectors.toList());

        // Wait for all tenant executions to complete with timeout
        CompletableFuture<Void> allExecutions = CompletableFuture.allOf(
                tenantExecutions.toArray(new CompletableFuture[0]));

        try {
            allExecutions.get(executionTimeoutMinutes, TimeUnit.MINUTES);
            logger.info()
                    .message(AppConstants.SCHEDULER_MESSAGE_ALL_EXECUTIONS_COMPLETED)
                    .detail(AppConstants.DETAIL_KEY_TENANT_COUNT, String.valueOf(rulesByTenant.size()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_TENANT_EXECUTION)
                    .log();
        } catch (TimeoutException e) {
            logger.error()
                    .message(AppConstants.SCHEDULER_MESSAGE_EXECUTION_TIMEOUT)
                    .detail(AppConstants.DETAIL_KEY_EXECUTION_TIMEOUT_MINUTES, String.valueOf(executionTimeoutMinutes))
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_ERROR)
                    .log();
            scheduledExecutionsFailed.increment();
        } catch (Exception e) {
            logger.error()
                    .message(AppConstants.SCHEDULER_MESSAGE_PARALLEL_EXECUTION_ERROR)
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                    .detail(AppConstants.DETAIL_KEY_ERROR_CLASS, e.getClass().getSimpleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_ERROR)
                    .log();
            scheduledExecutionsFailed.increment();
        }
    }

    /**
     * Execute all rules for a specific tenant in a separate thread.
     */
    private CompletableFuture<Void> executeRulesForTenant(String tenantId, List<DynamicRule> rules, LocalDateTime executionTime) {
        return CompletableFuture.runAsync(() -> {
            logger.info()
                    .message(AppConstants.SCHEDULER_MESSAGE_EXECUTING_RULES_FOR_TENANT)
                    .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_KEY_RULES_COUNT, String.valueOf(rules.size()))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_TENANT_EXECUTION)
                    .log();
            
            for (DynamicRule rule : rules) {
                try {
                    executeIndividualRule(rule, executionTime, tenantId);
                } catch (Exception e) {
                    logger.error()
                            .message(AppConstants.SCHEDULER_MESSAGE_FAILED_RULE_EXECUTION)
                            .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                            .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                            .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                            .detail(AppConstants.DETAIL_KEY_ERROR_CLASS, e.getClass().getSimpleName())
                            .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_ERROR)
                            .log();
                    recordExecutionHistory(rule, executionTime, 0L, 0, 0, 0, 0, 0, "FAILED", e.getMessage(), tenantId);
                }
            }
        }, scheduledExecutionPool);
    }

    /**
     * Execute an individual dynamic rule with comprehensive error handling and metrics.
     * CRITICAL: Uses per-rule locking to prevent concurrent execution of the same rule.
     */
    private void executeIndividualRule(DynamicRule rule, LocalDateTime executionTime, String tenantId) {
        // Get or create a lock object for this specific rule
        Object lock = ruleExecutionLocks.computeIfAbsent(rule.getId(), k -> new Object());
        
        // Synchronize on the rule-specific lock
        synchronized (lock) {
            long startTime = System.currentTimeMillis();
            
            // CRITICAL FIX: Check execution history BEFORE starting execution to prevent duplicates
            // For IMMEDIATE rules, always execute (continuous reconciliation)
            boolean shouldExecute = switch (rule.getScheduleType()) {
                case DAILY -> isDailyExecutionDue(rule, executionTime);
                case WEEKLY -> isWeeklyExecutionDue(rule, executionTime);
                case MONTHLY -> isMonthlyExecutionDue(rule, executionTime);
                case IMMEDIATE -> true; // Always execute IMMEDIATE rules
                default -> false;
            };
            
            if (!shouldExecute) {
                logger.info()
                        .message(AppConstants.LOG_SKIPPING_RULE_ALREADY_EXECUTED)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                        .detail(AppConstants.DETAIL_KEY_SCHEDULE_TYPE, rule.getScheduleType().toString())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EXECUTION)
                        .log();
                return;
            }
            
            scheduledExecutionsTotal.increment();
            
            logger.info()
                    .message(AppConstants.SCHEDULER_MESSAGE_EXECUTING_SCHEDULED_RULE)
                    .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                    .detail(AppConstants.DETAIL_KEY_SCHEDULE_TYPE, rule.getScheduleType().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EXECUTION)
                    .log();

            // CRITICAL FIX: Create execution history record BEFORE execution (optimistic locking)
            // This prevents duplicate executions by creating the record first
            DynamicRuleExecutionHistory pendingHistory = null;
            try {
                pendingHistory = new DynamicRuleExecutionHistory(
                        rule.getId(),
                        rule.getRoleId(),
                        tenantId,
                        rule.getScheduleType(),
                        rule.getTime() != null ? rule.getTime().toString() : null,
                        0L, // duration set to 0 initially (NOT NULL constraint)
                        0, // totalUsers not known yet
                        0, // matchingUsers not known yet
                        0, // successful not known yet
                        0, // failed not known yet
                        0, // skipped not known yet
                        "PENDING", // Mark as pending during execution
                        null // no error message yet
                );
                
                pendingHistory = executionHistoryRepository.save(pendingHistory);
                
                logger.info()
                        .message(AppConstants.LOG_CREATED_PENDING_EXECUTION_HISTORY)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                        .detail(AppConstants.DETAIL_KEY_HISTORY_ID, String.valueOf(pendingHistory.getId()))
                        .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EXECUTION)
                        .log();
                        
            } catch (Exception e) {
                logger.error(e)
                        .message(AppConstants.LOG_FAILED_CREATE_PENDING_HISTORY)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_ERROR)
                        .log();
                // If we can't create the pending record, it means another execution is in progress or completed
                // Skip this execution to prevent duplicates
                return;
            }

            try {
                // Double-check that the rule is still active before execution
                if (!rule.getRuleIsActive()) {
                    logger.warn()
                            .message(AppConstants.SCHEDULER_MESSAGE_SKIPPING_INACTIVE_RULE)
                            .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                            .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_VALIDATION)
                            .log();
                    
                    // Update pending history to SKIPPED
                    updateExecutionHistory(pendingHistory, 0L, 0, 0, 0, 0, 0, "SKIPPED", "Rule is inactive");
                    return;
                }
                
                logger.info()
                        .message(AppConstants.SCHEDULER_MESSAGE_EXECUTING_RULE_FOR_TENANT)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                        .detail(AppConstants.DETAIL_KEY_TENANT_ID, tenantId)
                        .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EXECUTION)
                        .log();
                
                // Execute the rule with tenant context
                var executionResult = dynamicRuleService.executeDynamicRuleWithTenant(rule.getId(), tenantId);
                
                long executionDuration = System.currentTimeMillis() - startTime;
                
                // Update pending execution history with actual results
                updateExecutionHistory(
                    pendingHistory,
                    executionDuration,
                    executionResult.totalUsersEvaluated(),
                    executionResult.usersMatchingCondition(), 
                    executionResult.successfulAssignments(),
                    executionResult.failedAssignments(),
                    executionResult.skippedAssignments(),
                    "SUCCESS",
                    null
                );
                
                logger.info()
                        .message(AppConstants.SCHEDULER_MESSAGE_SUCCESSFULLY_EXECUTED_RULE)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                        .detail(AppConstants.DETAIL_KEY_USERS_EVALUATED, String.valueOf(executionResult.totalUsersEvaluated()))
                        .detail(AppConstants.DETAIL_KEY_SUCCESSFUL_ASSIGNMENTS, String.valueOf(executionResult.successfulAssignments()))
                        .detail(AppConstants.DETAIL_KEY_EXECUTION_DURATION, String.valueOf(executionDuration))
                        .objectType(AppConstants.LOG_OBJECT_TYPE_RULE_EXECUTION)
                        .log();
                       
            } catch (Exception e) {
                long executionDuration = System.currentTimeMillis() - startTime;
                scheduledExecutionsFailed.increment();
                
                logger.error()
                        .message(AppConstants.SCHEDULER_MESSAGE_FAILED_SCHEDULED_RULE)
                        .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                        .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                        .detail(AppConstants.DETAIL_KEY_ERROR_CLASS, e.getClass().getSimpleName())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_ERROR)
                        .log();
                
                // Update pending history with failure details
                if (pendingHistory != null) {
                    updateExecutionHistory(pendingHistory, executionDuration, 0, 0, 0, 0, 0, "FAILED", e.getMessage());
                }
                throw e; // Re-throw to be handled by caller
            }
        } // End synchronized block
    }

    /**
     * Check if a rule is due for execution based on its schedule type and last execution.
     */
    private boolean isRuleDueForExecution(DynamicRule rule, LocalDateTime now) {
        // IMMEDIATE rules: Execute every minute (continuous reconciliation)
        // Check if not executed in the last minute to prevent duplicates
        if (rule.getScheduleType() == ScheduleType.IMMEDIATE) {
            return isImmediateExecutionDue(rule, now);
        }
        
        // Scheduled rules (DAILY/WEEKLY/MONTHLY): Check if rule has a scheduled time and if current time matches
        if (rule.getTime() != null && !isTimeMatching(rule.getTime(), now.toLocalTime())) {
            return false;
        }

        // Check execution history to prevent duplicates
        return switch (rule.getScheduleType()) {
            case DAILY -> isDailyExecutionDue(rule, now);
            case WEEKLY -> isWeeklyExecutionDue(rule, now);
            case MONTHLY -> isMonthlyExecutionDue(rule, now);
            default -> false; // Fallback for safety
        };
    }

    /**
     * Check if current time matches the scheduled time (within 1-minute window).
     */
    private boolean isTimeMatching(LocalTime scheduledTime, LocalTime currentTime) {
        // Allow execution within the same minute
        return scheduledTime.getHour() == currentTime.getHour() && 
               scheduledTime.getMinute() == currentTime.getMinute();
    }

    /**
     * Check if daily rule is due for execution (not executed today).
     */
    private boolean isDailyExecutionDue(DynamicRule rule, LocalDateTime now) {
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        return !executionHistoryRepository.existsByRuleIdAndExecutionDateBetween(
                rule.getId(), startOfToday, now);
    }

    /**
     * Check if weekly rule is due for execution (not executed this week).
     */
    private boolean isWeeklyExecutionDue(DynamicRule rule, LocalDateTime now) {
        LocalDateTime startOfWeek = now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        return !executionHistoryRepository.existsByRuleIdAndExecutionDateBetween(
                rule.getId(), startOfWeek, now);
    }

    /**
     * Check if monthly rule is due for execution (not executed this month).
     */
    private boolean isMonthlyExecutionDue(DynamicRule rule, LocalDateTime now) {
        LocalDateTime startOfMonth = now.toLocalDate()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        return !executionHistoryRepository.existsByRuleIdAndExecutionDateBetween(
                rule.getId(), startOfMonth, now);
    }
    
    /**
     * Check if IMMEDIATE rule is due for execution.
     * 
     * <p>IMMEDIATE rules are ALWAYS executed every minute for continuous near real-time reconciliation.
     * Unlike scheduled rules (DAILY/WEEKLY/MONTHLY), IMMEDIATE rules don't check execution history
     * to prevent duplicates - they run every minute by design to catch any user attribute changes.</p>
     * 
     * @param rule the dynamic rule
     * @param now the current time
     * @return true (always execute IMMEDIATE rules every minute)
     */
    private boolean isImmediateExecutionDue(DynamicRule rule, LocalDateTime now) {
        // IMMEDIATE rules ALWAYS execute every minute - no history check needed
        // This ensures users who get updated attributes are immediately reconciled
        return true;
    }

    /**
     * Record execution history for monitoring and duplicate prevention.
     * NOTE: This method is now deprecated in favor of optimistic locking approach.
     * Use the pattern: create pending history -> execute -> update history.
     */
    private void recordExecutionHistory(DynamicRule rule, LocalDateTime executionTime, Long duration,
                                      int totalUsers, int matchingUsers, int successful, 
                                      int failed, int skipped, String status, String errorMessage, String tenantId) {
        try {
            DynamicRuleExecutionHistory history = new DynamicRuleExecutionHistory(
                    rule.getId(),
                    rule.getRoleId(),
                    tenantId, // Use actual tenant ID
                    rule.getScheduleType(),
                    rule.getTime() != null ? rule.getTime().toString() : null,
                    duration,
                    totalUsers,
                    matchingUsers,
                    successful,
                    failed,
                    skipped,
                    status,
                    errorMessage
            );
            
            executionHistoryRepository.save(history);
            
        } catch (Exception e) {
            logger.error()
                    .message(AppConstants.SCHEDULER_MESSAGE_FAILED_EXECUTION_HISTORY)
                    .detail(AppConstants.DETAIL_KEY_DYNAMIC_RULE_ID, String.valueOf(rule.getId()))
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                    .detail(AppConstants.DETAIL_KEY_ERROR_CLASS, e.getClass().getSimpleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_HISTORY)
                    .log();
        }
    }
    
    /**
     * Updates an existing execution history record with final execution results.
     * Used for optimistic locking pattern where pending history is created before execution.
     */
    private void updateExecutionHistory(DynamicRuleExecutionHistory history, Long duration,
                                      int totalUsers, int matchingUsers, int successful, 
                                      int failed, int skipped, String status, String errorMessage) {
        try {
            history.setExecutionDurationMs(duration);
            history.setTotalUsersEvaluated(totalUsers);
            history.setUsersMatchingCondition(matchingUsers);
            history.setSuccessfulAssignments(successful);
            history.setFailedAssignments(failed);
            history.setSkippedAssignments(skipped);
            history.setExecutionStatus(status);
            history.setErrorMessage(errorMessage);
            
            executionHistoryRepository.save(history);
            
            logger.debug()
                    .message(AppConstants.LOG_UPDATED_EXECUTION_HISTORY)
                    .detail(AppConstants.DETAIL_KEY_HISTORY_ID, String.valueOf(history.getId()))
                    .detail(AppConstants.DETAIL_KEY_STATUS, status)
                    .detail(AppConstants.DETAIL_KEY_SUCCESSFUL, String.valueOf(successful))
                    .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_HISTORY)
                    .log();
            
        } catch (Exception e) {
            logger.error()
                    .message(AppConstants.LOG_FAILED_UPDATE_EXECUTION_HISTORY)
                    .detail(AppConstants.DETAIL_KEY_HISTORY_ID, String.valueOf(history.getId()))
                    .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                    .detail(AppConstants.DETAIL_KEY_ERROR_CLASS, e.getClass().getSimpleName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_HISTORY)
                    .log();
        }
    }

    /**
     * Handle execution failures and circuit breaker logic.
     */
    private void handleExecutionFailure(Exception e) {
        consecutiveFailures++;
        lastFailureTime = LocalDateTime.now();
        scheduledExecutionsFailed.increment();
        
        logger.error()
                .message(AppConstants.SCHEDULER_MESSAGE_EXECUTION_FAILED)
                .detail(AppConstants.DETAIL_KEY_CONSECUTIVE_FAILURES, String.valueOf(consecutiveFailures))
                .detail(AppConstants.DETAIL_KEY_ERROR_MESSAGE, e.getMessage())
                .detail(AppConstants.DETAIL_KEY_ERROR_CLASS, e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_EXECUTION_ERROR)
                .log();

        if (consecutiveFailures >= maxConsecutiveFailures) {
            circuitBreakerOpen = true;
            circuitBreakerTrips.increment();
            
            logger.error()
                    .message(AppConstants.SCHEDULER_MESSAGE_CIRCUIT_BREAKER_OPENED)
                    .detail(AppConstants.DETAIL_KEY_CONSECUTIVE_FAILURES, String.valueOf(consecutiveFailures))
                    .detail(AppConstants.DETAIL_KEY_CIRCUIT_BREAKER_STATE, "OPEN")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CIRCUIT_BREAKER)
                    .log();
        }
    }

    /**
     * Reset circuit breaker after successful execution.
     */
    private void resetCircuitBreaker() {
        if (circuitBreakerOpen || consecutiveFailures > 0) {
            circuitBreakerOpen = false;
            consecutiveFailures = 0;
            lastFailureTime = null;
            
            logger.info()
                    .message(AppConstants.SCHEDULER_MESSAGE_CIRCUIT_BREAKER_RESET)
                    .detail(AppConstants.DETAIL_KEY_CIRCUIT_BREAKER_STATE, "CLOSED")
                    .detail(AppConstants.DETAIL_KEY_CONSECUTIVE_FAILURES, "0")
                    .objectType(AppConstants.LOG_OBJECT_TYPE_CIRCUIT_BREAKER)
                    .log();
        }
    }

    /**
     * Check if circuit breaker should attempt recovery.
     */
    private boolean isCircuitBreakerRecoveryTime() {
        if (!circuitBreakerOpen || lastFailureTime == null) {
            return false;
        }
        
        return lastFailureTime.plusMinutes(circuitBreakerTimeoutMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * Get current circuit breaker status for monitoring.
     */
    @Override
    public boolean isCircuitBreakerOpen() {
        return circuitBreakerOpen;
    }

    /**
     * Get consecutive failure count for monitoring.
     */
    @Override
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    /**
     * Manual trigger for scheduled execution (for testing/debugging).
     */
    @Override
    public void triggerScheduledExecution() {
        logger.info()
                .message(AppConstants.SCHEDULER_MESSAGE_MANUAL_TRIGGER)
                .objectType(AppConstants.LOG_OBJECT_TYPE_MANUAL_TRIGGER)
                .log();
        executeScheduledRules();
    }

    /**
     * Graceful shutdown of thread pools.
     */
    public void shutdown() {
        logger.info()
                .message(AppConstants.SCHEDULER_MESSAGE_SHUTTING_DOWN)
                .objectType(AppConstants.LOG_OBJECT_TYPE_SERVICE_LIFECYCLE)
                .log();
        
        mainScheduler.shutdown();
        scheduledExecutionPool.shutdown();
        
        try {
            if (!mainScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                mainScheduler.shutdownNow();
            }
            if (!scheduledExecutionPool.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutionPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            mainScheduler.shutdownNow();
            scheduledExecutionPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}