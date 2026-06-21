package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.BatchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BatchJob entity operations.
 * Provides methods to track and query async batch role assignments.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {

    /**
     * Finds a batch job by ID and tenant ID (for security).
     *
     * @param id batch job ID
     * @param tenantId tenant identifier
     * @return Optional containing the batch job if found
     */
    Optional<BatchJob> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Finds all batch jobs for a tenant with specific status.
     *
     * @param tenantId tenant identifier
     * @param status batch status (PENDING, PROCESSING, etc.)
     * @return list of batch jobs
     */
    List<BatchJob> findByTenantIdAndStatus(String tenantId, String status);

    /**
     * Finds all PENDING batch jobs (for worker to process).
     *
     * @param status batch status
     * @return list of pending batch jobs
     */
    List<BatchJob> findByStatus(String status);
}