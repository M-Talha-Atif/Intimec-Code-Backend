package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobItemResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobResponse;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Service contract for asynchronous access-role import jobs.
 *
 * <p>The import flow is intentionally split into a fast HTTP submission path and
 * a background processing path:</p>
 * <ol>
 *     <li>The controller uploads a JSON/CSV file as multipart form-data.</li>
 *     <li>This service parses and performs structural validation only.</li>
 *     <li>A {@code RoleImportJob} row and one {@code RoleImportJobItem} row per input row are persisted.</li>
 *     <li>A Kafka event is published after transaction commit so a worker can process queued rows.</li>
 *     <li>The frontend polls job summary/items endpoints and can download failed rows as CSV.</li>
 * </ol>
 */
public interface AccessRoleImportService {

    /**
     * Accepts an uploaded role import file and creates an asynchronous import job.
     *
     * <p>This method does not create roles synchronously. It only parses the file,
     * validates required row structure, stores job/item rows, and schedules the
     * background Kafka handoff. Business validation such as duplicate role names,
     * entitlement existence, child-role cycle checks, and rule creation failures
     * are captured later at item level by the worker.</p>
     *
     * @param tenantId tenant scope from {@code X-Tenant-Id}
     * @param actorId actor/user id from {@code X-User-ID}, or the controller fallback
     * @param actorUsername username from {@code X-Username}, or the controller fallback
     * @param format import format token; currently {@code json} or {@code csv}
     * @param file multipart file part named {@code file}
     * @return accepted job summary containing polling URLs and counters
     * @throws IOException if the uploaded file cannot be read
     */
    RoleImportJobResponse submit(String tenantId, String actorId, String actorUsername, String format, MultipartFile file) throws IOException;

    /**
     * Reads the current status/counters for one tenant-scoped import job.
     *
     * @param tenantId tenant scope from {@code X-Tenant-Id}
     * @param jobId import job id returned by {@link #submit(String, String, String, String, MultipartFile)}
     * @return job summary with status, counters, progress, and URLs
     */
    RoleImportJobResponse getJob(String tenantId, UUID jobId);

    /**
     * Lists row-level import results for a job.
     *
     * @param tenantId tenant scope from {@code X-Tenant-Id}
     * @param jobId import job id
     * @param status optional item status filter ({@code QUEUED}, {@code SUCCEEDED}, {@code FAILED}, {@code SKIPPED})
     * @param pageable Spring pageable supplied by query parameters
     * @return paginated row result DTOs
     */
    Page<RoleImportJobItemResponse> listItems(String tenantId, UUID jobId, RoleImportItemStatus status, Pageable pageable);

    /**
     * Streams a lightweight CSV report containing only failed rows.
     *
     * @param tenantId tenant scope from {@code X-Tenant-Id}
     * @param jobId import job id
     * @param outputStream response output stream owned by Spring MVC
     * @throws IOException if CSV writing fails
     */
    void streamErrorsCsv(String tenantId, UUID jobId, OutputStream outputStream) throws IOException;
}

