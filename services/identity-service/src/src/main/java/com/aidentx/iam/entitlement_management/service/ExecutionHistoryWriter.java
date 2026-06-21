package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionDetail;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.RuleExecutionDetailRepository;
import com.aidentx.iam.entitlement_management.repository.RuleExecutionHistoryRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writes collected execution history to the database in bulk.
 *
 * <p>Called once after rule evaluation completes (success or failure).
 * Uses REQUIRES_NEW propagation so history is persisted even if the
 * calling transaction has rolled back.
 *
 * <p>The write sequence:
 * <ol>
 *   <li>Build and persist the run-level history record (ID is auto-generated)</li>
 *   <li>Set the generated ID on the collector</li>
 *   <li>Build detail records (they reference the generated history ID as FK)</li>
 *   <li>Bulk persist detail records</li>
 * </ol>
 *
 * <p>If history write itself fails, the error is logged but not propagated.
 * Rule evaluation should never fail because of a history write failure.
 */
@Service
public class ExecutionHistoryWriter {

    private final RuleExecutionHistoryRepository historyRepository;
    private final RuleExecutionDetailRepository detailRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public ExecutionHistoryWriter(RuleExecutionHistoryRepository historyRepository,
                                   RuleExecutionDetailRepository detailRepository,
                                   UnifiedLoggerFactory loggerFactory) {
        this.historyRepository = historyRepository;
        this.detailRepository = detailRepository;
        this.logger = loggerFactory.getLogger(getClass(), RuleEvaluationConstants.LOGGER_MODULE_HISTORY);
    }

    /**
     * Flushes the collected execution history and details to the database.
     *
     * <p>Sequence:
     * <ol>
     *   <li>Build history entity from collector (no ID set)</li>
     *   <li>Persist history — Hibernate generates the ID</li>
     *   <li>Set the generated ID on the collector</li>
     *   <li>Build detail entities (they now have the correct executionId FK)</li>
     *   <li>Bulk persist details</li>
     * </ol>
     *
     * <p>Uses REQUIRES_NEW so this runs in its own transaction,
     * independent of the evaluation transaction's success or failure.
     *
     * @param collector    the in-memory collector populated during evaluation
     * @param success      true if evaluation completed successfully
     * @param errorMessage top-level error message if failed, null if success
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void flush(ExecutionHistoryCollector collector, boolean success, String errorMessage) {
        try {
            // Step 1: Build history entity (no ID — Hibernate will generate it)
            RuleExecutionHistory history = success
                    ? collector.buildSuccessHistory()
                    : collector.buildFailureHistory(errorMessage);

            // Step 2: Persist — Hibernate generates the ID
            RuleExecutionHistory saved = historyRepository.saveAndFlush(history);

            // Step 3: Set the generated ID on the collector
            collector.setExecutionId(saved.getId());

            // Step 4: Build detail entities with the correct executionId FK
            List<RuleExecutionDetail> details = collector.buildDetails();

            // Step 5: Bulk persist details
            if (!details.isEmpty()) {
                detailRepository.saveAll(details);
            }

            logger.info()
                    .message(RuleEvaluationConstants.LOG_EXECUTION_HISTORY_PERSISTED)
                    .detail(RuleEvaluationConstants.DETAIL_EXECUTION_ID, saved.getId().toString())
                    .detail("status", history.getStatus().name())
                    .detail("detailRecords", String.valueOf(details.size()))
                    .detail(RuleEvaluationConstants.DETAIL_CREATED_COUNT,
                            String.valueOf(history.getAssignmentsCreated()))
                    .detail(RuleEvaluationConstants.DETAIL_REVOKED_COUNT,
                            String.valueOf(history.getAssignmentsRevoked()))
                    .detail(RuleEvaluationConstants.DETAIL_SKIPPED_NO_SHADOW,
                            String.valueOf(history.getSkippedNoShadow()))
                    .detail(RuleEvaluationConstants.DETAIL_DURATION_MS,
                            String.valueOf(history.getDurationMs()))
                    .log();

        } catch (Exception e) {
            // Log but DO NOT propagate — evaluation result must not be affected
            logger.error()
                    .message(RuleEvaluationConstants.LOG_EXECUTION_HISTORY_FAILED)
                    .detail(RuleEvaluationConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        }
    }
}