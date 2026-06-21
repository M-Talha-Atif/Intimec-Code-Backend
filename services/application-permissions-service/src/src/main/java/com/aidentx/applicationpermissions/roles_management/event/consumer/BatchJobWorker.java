package com.aidentx.applicationpermissions.roles_management.event.consumer;

import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.model.entity.BatchJob;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import com.aidentx.applicationpermissions.roles_management.repository.BatchJobRepository;
import com.aidentx.applicationpermissions.roles_management.repository.UserRoleRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Kafka consumer for processing batch role assignment jobs.
 *
 * <p>This worker listens to the "role-assignment-jobs" topic and processes
 * large batch assignments asynchronously to avoid blocking API requests.</p>
 *
 * <p><b>Processing Strategy:</b></p>
 * <ul>
 *   <li>Fetch batch job from database</li>
 *   <li>Pre-fetch existing assignments (duplicate detection)</li>
 *   <li>Generate M×N cartesian product</li>
 *   <li>Insert in chunks of 5000 records</li>
 *   <li>Update batch job progress</li>
 * </ul>
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class BatchJobWorker {

    private final BatchJobRepository batchJobRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    @Autowired
    public BatchJobWorker(
            BatchJobRepository batchJobRepository,
            UserRoleRepository userRoleRepository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.batchJobRepository = batchJobRepository;
        this.userRoleRepository = userRoleRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger("BatchJobWorker", "batch-processing");
    }

    /**
     * Kafka listener for batch job messages.
     *
     * <p><b>Kafka Configuration:</b></p>
     * <ul>
     *   <li>Topic: role-assignment-jobs</li>
     *   <li>Group ID: batch-job-workers</li>
     *   <li>Concurrency: 3 (processes 3 batches in parallel)</li>
     * </ul>
     *
     * @param message the Kafka message containing batch job details
     */
    @KafkaListener(
            topics = "role-assignment-jobs",
            groupId = "batch-job-workers",
            containerFactory = "batchKafkaListenerContainerFactory",
            concurrency = "3"
    )
    @Transactional
    public void processBatchJob(String message) {
        UUID batchId = null;

        try {
            logger.info()
                    .message("Received batch job message from Kafka")
                    .detail("message", message)
                    .log();

            // Step 1: Parse Kafka message
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);

            batchId = UUID.fromString((String) payload.get("batchId"));
            String tenantId = (String) payload.get("tenantId");

            final UUID finalBatchId = batchId; // effectively final for lambdas

            // Step 2: Fetch batch job from database
            BatchJob batchJob = batchJobRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + finalBatchId));

            logger.info()
                    .message("Processing batch job")
                    .detail("batchId", finalBatchId.toString())
                    .detail("tenantId", tenantId)
                    .detail("totalAssignments", String.valueOf(batchJob.getTotalAssignments()))
                    .log();

            // Step 3: Update status to PROCESSING
            batchJob.setStatus("PROCESSING");
            batchJob.setStartedAt(Instant.now());
            batchJobRepository.save(batchJob);

            // Step 4: Extract roleIds and userIds from payload
            List<String> roleIdStrings = (List<String>) payload.get("roleIds");
            List<String> userIdStrings = (List<String>) payload.get("userIds");
            Integer validityDays = (Integer) payload.get("validityDays");

            List<UUID> roleIds = roleIdStrings.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            List<UUID> userIds = userIdStrings.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            // Step 5: Pre-fetch existing assignments (duplicate detection)
            List<UserRole> existingAssignments = userRoleRepository.findByTenantIdAndUserIdInAndRoleIdIn(
                    tenantId,
                    userIds,
                    roleIds
            );

            // Build Set for O(1) lookup: "userId:roleId"
            Set<String> existingPairs = existingAssignments.stream()
                    .map(ur -> ur.getUserId() + ":" + ur.getRoleId())
                    .collect(Collectors.toSet());

            logger.info()
                    .message("Found existing assignments (will skip)")
                    .detail("existingCount", String.valueOf(existingAssignments.size()))
                    .log();

            // Step 6: Generate M×N assignments in chunks
            int successCount = 0;
            int skippedCount = 0;
            List<UserRole> assignmentsToCreate = new ArrayList<>();

            for (UUID roleId : roleIds) {
                for (UUID userId : userIds) {
                    // Check if assignment already exists
                    String pair = userId + ":" + roleId;
                    if (existingPairs.contains(pair)) {
                        skippedCount++;
                        continue; // Skip duplicate
                    }

                    // Create new assignment
                    UserRole assignment = new UserRole(
                            userId,
                            roleId,
                            tenantId,
                            AssignmentType.MANUAL,
                            batchJob.getId(), // Link to batch
                            validityDays
                    );

                    assignmentsToCreate.add(assignment);
                    successCount++;

                    // Batch insert every 5000 records
                    if (assignmentsToCreate.size() >= 5000) {
                        userRoleRepository.saveAll(assignmentsToCreate);

                        // Update progress in database
                        batchJob.setCompletedCount(successCount);
                        batchJob.setFailedCount(skippedCount);
                        batchJobRepository.save(batchJob);

                        assignmentsToCreate.clear();

                        logger.info()
                                .message("Saved chunk of assignments")
                                .detail("batchId", batchId.toString())
                                .detail("successCount", String.valueOf(successCount))
                                .detail("skippedCount", String.valueOf(skippedCount))
                                .log();
                    }
                }
            }

            // Step 7: Save remaining assignments
            if (!assignmentsToCreate.isEmpty()) {
                userRoleRepository.saveAll(assignmentsToCreate);
            }

            // Step 8: Update batch job status to COMPLETED
            batchJob.setStatus("COMPLETED");
            batchJob.setCompletedCount(successCount);
            batchJob.setFailedCount(skippedCount);
            batchJob.setCompletedAt(Instant.now());
            batchJobRepository.save(batchJob);

            logger.info()
                    .message("Batch job completed successfully")
                    .detail("batchId", batchId.toString())
                    .detail("successCount", String.valueOf(successCount))
                    .detail("skippedCount", String.valueOf(skippedCount))
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to process batch job")
                    .detail("batchId", batchId != null ? batchId.toString() : "unknown")
                    .detail("error", e.getMessage())
                    .log();

            // Update batch job status to FAILED
            if (batchId != null) {
                try {
                    BatchJob failedJob = batchJobRepository.findById(batchId).orElse(null);
                    if (failedJob != null) {
                        failedJob.setStatus("FAILED");
                        failedJob.setErrorMessage(e.getMessage());
                        failedJob.setCompletedAt(Instant.now());
                        batchJobRepository.save(failedJob);
                    }
                } catch (Exception saveError) {
                    logger.error(saveError)
                            .message("Failed to update batch job status to FAILED")
                            .detail("batchId", batchId.toString())
                            .log();
                }
            }
        }
    }
}