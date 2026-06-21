package com.aidentx.iam.access_roles_management.controller;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleImportConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.ApiResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobItemResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobResponse;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import com.aidentx.iam.access_roles_management.service.AccessRoleImportService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.UUID;

/**
 * REST controller for asynchronous access-role import jobs.
 *
 * <p>The controller exposes a multipart upload endpoint and read-only polling
 * endpoints. The upload endpoint returns {@code 202 Accepted} after the file is
 * parsed, structurally validated, persisted as job/item rows, and scheduled for
 * Kafka-backed background processing. It does not wait for role creation to
 * complete.</p>
 *
 * <p>Frontend flow:</p>
 * <ol>
 *     <li>{@code POST /api/v1/access-roles/import?format=json|csv} with a form-data file part named {@code file}.</li>
 *     <li>Poll {@code GET /api/v1/access-roles/import/{jobId}} for summary counters and status.</li>
 *     <li>Read {@code GET /api/v1/access-roles/import/{jobId}/items} for row-level results.</li>
 *     <li>Download {@code GET /api/v1/access-roles/import/{jobId}/errors.csv} when failures exist.</li>
 * </ol>
 */
@RestController
@RequestMapping(AccessRoleConstants.BASE_PATH)
public class AccessRoleImportController {

	private final AccessRoleImportService importService;
	private final IUnifiedLogger logger;

	public AccessRoleImportController(AccessRoleImportService importService,
									  UnifiedLoggerFactory loggerFactory) {
		this.importService = importService;
		this.logger = loggerFactory.getLogger(getClass(), AccessRoleImportConstants.LOG_CATEGORY);
	}

	/**
	 * Accepts a JSON/CSV import file and creates an asynchronous import job.
	 *
	 * <p>The multipart part name must be exactly {@code file}. The optional
	 * {@code format} query parameter selects the parser and defaults to JSON.
	 * The returned response contains URLs the frontend can use for polling and
	 * downloading failures.</p>
	 */
	@PostMapping(value = AccessRoleImportConstants.PATH_IMPORT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<RoleImportJobResponse>> submitImport(
			@RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
			@RequestHeader(value = AccessRoleImportConstants.HEADER_USER_ID, required = false) String actorId,
			@RequestHeader(value = AccessRoleImportConstants.HEADER_USERNAME, required = false) String actorUsername,
			@RequestHeader(value = AccessRoleImportConstants.HEADER_LEGACY_USER, required = false) String legacyActorUsername,
			@RequestParam(name = AccessRoleImportConstants.PARAM_FORMAT,
					defaultValue = AccessRoleImportConstants.FORMAT_JSON) String format,
			@RequestPart(AccessRoleImportConstants.PART_FILE) MultipartFile file) throws IOException {

		String effectiveActor = actorId == null || actorId.isBlank() ? "role-import" : actorId;
		String effectiveActorUsername = firstNonBlank(actorUsername, legacyActorUsername, effectiveActor);
		logger.info()
				.category(AccessRoleImportConstants.LOG_CATEGORY)
				.message(AccessRoleImportConstants.LOG_SUBMIT)
				.detail(AccessRoleImportConstants.DETAIL_TENANT_ID, tenantId)
				.detail(AccessRoleImportConstants.DETAIL_ACTOR_ID, effectiveActor)
				.detail(AccessRoleImportConstants.DETAIL_ACTOR_USERNAME, effectiveActorUsername)
				.detail(AccessRoleImportConstants.DETAIL_FORMAT, format)
				.detail(AccessRoleImportConstants.DETAIL_FILE_NAME, file.getOriginalFilename())
				.detail(AccessRoleImportConstants.DETAIL_FILE_SIZE, String.valueOf(file.getSize()))
				.log();

		RoleImportJobResponse response = importService.submit(
				tenantId,
				effectiveActor,
				effectiveActorUsername,
				format,
				file);

		logger.info()
				.category(AccessRoleImportConstants.LOG_CATEGORY)
				.message(AccessRoleImportConstants.LOG_SUBMIT_ACCEPTED)
				.detail(AccessRoleImportConstants.DETAIL_TENANT_ID, tenantId)
				.detail(AccessRoleImportConstants.DETAIL_JOB_ID, response.getJobId().toString())
				.detail(AccessRoleImportConstants.DETAIL_ROW_COUNT, String.valueOf(response.getTotalCount()))
				.detail(AccessRoleImportConstants.DETAIL_STATUS, response.getStatus().name())
				.log();

		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(ApiResponse.success(response, AccessRoleImportConstants.RESPONSE_JOB_ACCEPTED));
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Returns the latest summary for a tenant-scoped import job.
	 */
	@GetMapping(AccessRoleImportConstants.PATH_IMPORT_JOB)
	public ResponseEntity<ApiResponse<RoleImportJobResponse>> getImportJob(
			@RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
			@PathVariable UUID jobId) {

		logger.info()
				.category(AccessRoleImportConstants.LOG_CATEGORY)
				.message(AccessRoleImportConstants.LOG_JOB_FETCH)
				.detail(AccessRoleImportConstants.DETAIL_TENANT_ID, tenantId)
				.detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
				.log();

		RoleImportJobResponse response = importService.getJob(tenantId, jobId);
		return ResponseEntity.ok(ApiResponse.success(response, AccessRoleImportConstants.RESPONSE_JOB_FETCHED));
	}

	/**
	 * Returns paginated row-level results for an import job.
	 *
	 * <p>The optional {@code status} filter lets the frontend load only failed,
	 * queued, skipped, or succeeded rows.</p>
	 */
	@GetMapping(AccessRoleImportConstants.PATH_IMPORT_JOB_ITEMS)
	public ResponseEntity<ApiResponse<Page<RoleImportJobItemResponse>>> listImportJobItems(
			@RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
			@PathVariable UUID jobId,
			@RequestParam(name = AccessRoleImportConstants.PARAM_STATUS, required = false) RoleImportItemStatus status,
			Pageable pageable) {

		logger.info()
				.category(AccessRoleImportConstants.LOG_CATEGORY)
				.message(AccessRoleImportConstants.LOG_ITEMS_FETCH)
				.detail(AccessRoleImportConstants.DETAIL_TENANT_ID, tenantId)
				.detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
				.detail(AccessRoleImportConstants.DETAIL_STATUS, status == null ? "ALL" : status.name())
				.detail("page", String.valueOf(pageable.getPageNumber()))
				.detail("size", String.valueOf(pageable.getPageSize()))
				.log();

		Page<RoleImportJobItemResponse> response = importService.listItems(tenantId, jobId, status, pageable);
		return ResponseEntity.ok(ApiResponse.success(response, AccessRoleImportConstants.RESPONSE_JOB_ITEMS_FETCHED));
	}

	/**
	 * Streams a CSV report containing failed import rows for one job.
	 */
	@GetMapping(value = AccessRoleImportConstants.PATH_IMPORT_JOB_ERRORS_CSV, produces = "text/csv")
	public ResponseEntity<StreamingResponseBody> downloadImportErrors(
			@RequestHeader(AccessRoleConstants.HEADER_TENANT_ID) String tenantId,
			@PathVariable UUID jobId) {

		logger.info()
				.category(AccessRoleImportConstants.LOG_CATEGORY)
				.message(AccessRoleImportConstants.LOG_ERRORS_DOWNLOAD)
				.detail(AccessRoleImportConstants.DETAIL_TENANT_ID, tenantId)
				.detail(AccessRoleImportConstants.DETAIL_JOB_ID, jobId.toString())
				.log();

		StreamingResponseBody body = outputStream -> importService.streamErrorsCsv(tenantId, jobId, outputStream);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("text/csv"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"access-role-import-errors-" + jobId + ".csv\"")
				.body(body);
	}
}

