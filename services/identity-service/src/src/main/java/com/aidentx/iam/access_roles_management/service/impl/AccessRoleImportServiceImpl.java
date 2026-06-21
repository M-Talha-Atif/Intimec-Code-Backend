package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleImportConstants;
import com.aidentx.iam.access_roles_management.event.RoleImportJobEventPublisher;
import com.aidentx.iam.access_roles_management.event.RoleImportJobStartedEvent;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.RoleImportRowDto;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobItemResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleImportJobResponse;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJob;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJobItem;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemPhase;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobItemRepository;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleImportService;
import com.aidentx.iam.access_roles_management.service.import_jobs.CsvRoleImportParser;
import com.aidentx.iam.access_roles_management.service.import_jobs.JsonRoleImportParser;
import com.aidentx.iam.access_roles_management.service.import_jobs.RoleImportParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Default implementation for asynchronous access-role import jobs. */
@Service
public class AccessRoleImportServiceImpl implements AccessRoleImportService {

	private final RoleImportJobRepository jobRepository;
	private final RoleImportJobItemRepository itemRepository;
	private final JsonRoleImportParser jsonParser;
	private final CsvRoleImportParser csvParser;
	private final RoleImportJobEventPublisher eventPublisher;
	private final ObjectMapper objectMapper;
	private final int maxRows;

	public AccessRoleImportServiceImpl(RoleImportJobRepository jobRepository,
									   RoleImportJobItemRepository itemRepository,
									   JsonRoleImportParser jsonParser,
									   CsvRoleImportParser csvParser,
									   RoleImportJobEventPublisher eventPublisher,
									   ObjectMapper objectMapper,
									   @Value("${" + AccessRoleImportConstants.CONFIG_MAX_ROWS + ":"
											   + AccessRoleImportConstants.MAX_ROWS_DEFAULT + "}") int maxRows) {
		this.jobRepository = jobRepository;
		this.itemRepository = itemRepository;
		this.jsonParser = jsonParser;
		this.csvParser = csvParser;
		this.eventPublisher = eventPublisher;
		this.objectMapper = objectMapper;
		this.maxRows = Math.max(1, maxRows);
	}

	@Override
	@Transactional
	public RoleImportJobResponse submit(String tenantId, String actorId, String actorUsername, String format, MultipartFile file) throws IOException {
		String normalizedFormat = normalizeFormat(format);
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException(AccessRoleImportConstants.ERROR_EMPTY_FILE);
		}

		RoleImportParser parser = parserFor(normalizedFormat);
		List<RoleImportRowDto> rows = parser.parse(file.getInputStream());
		List<String> validationErrors = validateRows(rows);

		RoleImportJob job = new RoleImportJob();
		job.setTenantId(tenantId);
		job.setActorId(actorId);
		job.setActorUsername(trimToNull(actorUsername));
		job.setFormat(normalizedFormat);
		job.setTotalCount(rows.size());
		RoleImportJob savedJob = jobRepository.save(job);
		long initialFailedCount = 0;

		for (int i = 0; i < rows.size(); i++) {
			RoleImportRowDto row = rows.get(i);
			String validationError = validationErrors.get(i);
			RoleImportJobItem item = new RoleImportJobItem();
			item.setJobId(savedJob.getId());
			item.setTenantId(tenantId);
			item.setRowNumber(i + 1);
			item.setExternalRef(row != null ? trimToNull(row.getExternalRef()) : null);
			item.setPhase(RoleImportItemPhase.CREATE);
			if (validationError == null) {
				item.setStatus(RoleImportItemStatus.QUEUED);
			} else {
				item.setStatus(RoleImportItemStatus.FAILED);
				item.setErrorCode(AccessRoleImportConstants.ERROR_VALIDATION);
				item.setErrorMessage(validationError);
				item.setProcessedAt(Instant.now());
				initialFailedCount++;
			}
			item.setPayload(objectMapper.valueToTree(row));
			itemRepository.save(item);
		}
		savedJob.setFailedCount(initialFailedCount);
		jobRepository.save(savedJob);

		publishAfterCommit(new RoleImportJobStartedEvent(savedJob.getId(), tenantId, actorId));
		return toJobResponse(savedJob);
	}

	private void publishAfterCommit(RoleImportJobStartedEvent event) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			eventPublisher.publish(event);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				eventPublisher.publish(event);
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public RoleImportJobResponse getJob(String tenantId, UUID jobId) {
		return toJobResponse(findJob(tenantId, jobId));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<RoleImportJobItemResponse> listItems(String tenantId, UUID jobId, RoleImportItemStatus status, Pageable pageable) {
		findJob(tenantId, jobId);
		Page<RoleImportJobItem> items = status == null
				? itemRepository.findByJobId(jobId, pageable)
				: itemRepository.findByJobIdAndStatus(jobId, status, pageable);
		return items.map(this::toItemResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public void streamErrorsCsv(String tenantId, UUID jobId, OutputStream outputStream) throws IOException {
		findJob(tenantId, jobId);
		try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
			writer.write("rowNumber,externalRef,roleName,phase,status,displayStatus,roleId,errorCode,errorMessage,resultMessage\n");
			for (RoleImportJobItem item : itemRepository.findByJobIdAndStatusOrderByRowNumberAsc(jobId, RoleImportItemStatus.FAILED)) {
				RoleImportJobItemResponse response = toItemResponse(item);
				writer.write(item.getRowNumber() + ",");
				writer.write(csv(item.getExternalRef()) + ",");
				writer.write(csv(response.getRoleName()) + ",");
				writer.write(item.getPhase() != null ? item.getPhase().name() : "");
				writer.write(",");
				writer.write(item.getStatus() != null ? item.getStatus().name() : "");
				writer.write(",");
				writer.write(csv(response.getDisplayStatus()) + ",");
				writer.write(item.getRoleId() != null ? item.getRoleId().toString() : "");
				writer.write(",");
				writer.write(csv(item.getErrorCode()) + ",");
				writer.write(csv(item.getErrorMessage()) + ",");
				writer.write(csv(response.getResultMessage()));
				writer.write("\n");
			}
		}
	}

	private String normalizeFormat(String format) {
		String normalized = format == null || format.isBlank()
				? AccessRoleImportConstants.FORMAT_JSON
				: format.trim().toLowerCase(Locale.ROOT);
		if (!AccessRoleImportConstants.FORMAT_JSON.equals(normalized)
				&& !AccessRoleImportConstants.FORMAT_CSV.equals(normalized)) {
			throw new IllegalArgumentException(String.format(AccessRoleImportConstants.ERROR_UNSUPPORTED_FORMAT, format));
		}
		return normalized;
	}

	private RoleImportParser parserFor(String format) {
		return AccessRoleImportConstants.FORMAT_CSV.equals(format) ? csvParser : jsonParser;
	}

	private List<String> validateRows(List<RoleImportRowDto> rows) {
		if (rows == null || rows.isEmpty()) {
			throw new IllegalArgumentException(AccessRoleImportConstants.ERROR_EMPTY_FILE);
		}
		if (rows.size() > maxRows) {
			throw new IllegalArgumentException(String.format(
					AccessRoleImportConstants.ERROR_TOO_MANY_ROWS, rows.size(), maxRows));
		}

		Set<String> externalRefs = new HashSet<>();
		List<String> validationErrors = new ArrayList<>(rows.size());
		for (int i = 0; i < rows.size(); i++) {
			validationErrors.add(validateRow(rows.get(i), externalRefs));
		}
		return validationErrors;
	}

	private String validateRow(RoleImportRowDto row, Set<String> externalRefs) {
		if (row == null) {
			return "This row is empty. Add the role details or remove the row from the import file.";
		}
		if (isBlank(row.getName())) {
			return requiredFieldMessage(row, "Role name", "name");
		}
		String externalRefError = validateExternalRef(row.getExternalRef(), externalRefs);
		if (externalRefError != null) {
			return rolePrefix(row) + " uses duplicate externalRef '" + externalRefError
					+ "'. Each imported row must have a unique externalRef.";
		}
		String childRoleRefError = validateChildRoleRefs(row.getChildRoleRefs());
		if (childRoleRefError != null) {
			return rolePrefix(row) + " has invalid childRoleRefs. " + childRoleRefError;
		}
		String identityIdsError = validateIdentityIds(row.getIdentityIds());
		if (identityIdsError != null) {
			return rolePrefix(row) + " has invalid identityIds. " + identityIdsError;
		}
		String rulesError = validateRules(row.getRules());
		if (rulesError != null) {
			return rolePrefix(row) + " has invalid assignment rules. " + rulesError;
		}
		return null;
	}

	private String validateExternalRef(String rawExternalRef, Set<String> externalRefs) {
		String externalRef = trimToNull(rawExternalRef);
		if (externalRef != null && !externalRefs.add(externalRef)) {
			return externalRef;
		}
		return null;
	}

	private String validateChildRoleRefs(List<String> childRoleRefs) {
		if (childRoleRefs == null) {
			return null;
		}
		for (String ref : childRoleRefs) {
			if (isBlank(ref)) {
				return "Remove blank values from childRoleRefs or provide a valid externalRef.";
			}
		}
		return null;
	}

	private String validateIdentityIds(List<UUID> identityIds) {
		if (identityIds == null) {
			return null;
		}
		Set<UUID> uniqueIdentityIds = new HashSet<>();
		for (int i = 0; i < identityIds.size(); i++) {
			UUID identityId = identityIds.get(i);
			if (identityId == null) {
				return "identityIds[" + i + "] is required.";
			}
			if (!uniqueIdentityIds.add(identityId)) {
				return "duplicate identityId: " + identityId + ". Each identity can appear only once per row.";
			}
		}
		return null;
	}

	private String validateRules(List<CreateAccessRoleAssignmentRuleRequest> rules) {
		if (rules == null) {
			return null;
		}
		for (int i = 0; i < rules.size(); i++) {
			if (rules.get(i) == null) {
				return "rules[" + i + "] is empty. Remove it or provide a valid assignment rule.";
			}
		}
		return null;
	}

	private RoleImportJob findJob(String tenantId, UUID jobId) {
		return jobRepository.findByIdAndTenantId(jobId, tenantId)
				.orElseThrow(() -> new IllegalArgumentException(
						String.format(AccessRoleImportConstants.ERROR_JOB_NOT_FOUND, jobId)));
	}

	private RoleImportJobResponse toJobResponse(RoleImportJob job) {
		RoleImportJobResponse response = new RoleImportJobResponse();
		response.setJobId(job.getId());
		response.setTenantId(job.getTenantId());
		response.setStatus(job.getStatus());
		response.setTotalCount(job.getTotalCount());
		response.setSucceededCount(job.getSucceededCount());
		response.setFailedCount(job.getFailedCount());
		response.setSkippedCount(job.getSkippedCount());
		long fullySucceededCount = itemRepository.countByJobIdAndStatus(job.getId(), RoleImportItemStatus.SUCCEEDED);
		long createdWithErrorsCount = itemRepository.countByJobIdAndStatusAndRoleIdIsNotNull(job.getId(), RoleImportItemStatus.FAILED);
		response.setCreatedCount(job.getSucceededCount());
		response.setFullySucceededCount(fullySucceededCount);
		response.setCreatedWithErrorsCount(createdWithErrorsCount);
		response.setRowsNeedingAttentionCount(job.getFailedCount());
		response.setProgressPercent(progress(job));
		response.setSummaryMessage(summaryMessage(job, fullySucceededCount, createdWithErrorsCount));
		response.setCreatedAt(job.getCreatedAt());
		response.setStartedAt(job.getStartedAt());
		response.setCompletedAt(job.getCompletedAt());
		response.setErrorMessage(job.getErrorMessage());
		response.setStatusUrl(String.format(AccessRoleImportConstants.STATUS_URL_TEMPLATE, job.getId()));
		response.setItemsUrl(String.format(AccessRoleImportConstants.ITEMS_URL_TEMPLATE, job.getId()));
		response.setErrorsUrl(String.format(AccessRoleImportConstants.ERRORS_URL_TEMPLATE, job.getId()));
		return response;
	}

	private RoleImportJobItemResponse toItemResponse(RoleImportJobItem item) {
		RoleImportJobItemResponse response = new RoleImportJobItemResponse();
		response.setItemId(item.getId());
		response.setRowNumber(item.getRowNumber());
		response.setExternalRef(item.getExternalRef());
		response.setPhase(item.getPhase());
		response.setStatus(item.getStatus());
		response.setDisplayStatus(displayStatus(item));
		response.setRoleId(item.getRoleId());
		RoleImportRowDto row = readPayloadOrNull(item.getPayload());
		if (row != null) {
			response.setRoleName(row.getName());
			response.setRoleType(row.getType());
		}
		response.setRoleCreated(item.getRoleId() != null);
		response.setErrorCode(item.getErrorCode());
		response.setErrorMessage(item.getErrorMessage());
		response.setWarningMessage(item.getWarningMessage());
		response.setResultMessage(resultMessage(item, row));
		response.setProcessedAt(item.getProcessedAt());
		return response;
	}

	private static int progress(RoleImportJob job) {
		if (job.getTotalCount() <= 0) {
			return 0;
		}
		long processed = job.getSucceededCount() + job.getFailedCount() + job.getSkippedCount();
		return (int) Math.min(100, Math.round((double) processed / job.getTotalCount() * 100));
	}

	private String requiredFieldMessage(RoleImportRowDto row, String displayName, String fieldName) {
		return rolePrefix(row) + " could not be imported because "
				+ String.format(AccessRoleImportConstants.USER_ERROR_REQUIRED_FIELD_TEMPLATE, displayName, fieldName);
	}

	private String rolePrefix(RoleImportRowDto row) {
		String name = row != null ? trimToNull(row.getName()) : null;
		return name != null ? "Role '" + name + "'" : "This row";
	}

	private RoleImportRowDto readPayloadOrNull(JsonNode payload) {
		try {
			return payload == null ? null : objectMapper.treeToValue(payload, RoleImportRowDto.class);
		} catch (Exception ignored) {
			return null;
		}
	}

	private static String displayStatus(RoleImportJobItem item) {
		if (item.getStatus() == RoleImportItemStatus.SUCCEEDED) {
			return AccessRoleImportConstants.DISPLAY_STATUS_CREATED;
		}
		if (item.getStatus() == RoleImportItemStatus.FAILED) {
			return AccessRoleImportConstants.DISPLAY_STATUS_FAILED;
		}
		if (item.getStatus() == RoleImportItemStatus.SKIPPED) {
			return AccessRoleImportConstants.DISPLAY_STATUS_SKIPPED;
		}
		return AccessRoleImportConstants.DISPLAY_STATUS_QUEUED;
	}

	private static String resultMessage(RoleImportJobItem item, RoleImportRowDto row) {
		String roleName = row != null && trimToNull(row.getName()) != null ? "Role '" + trimToNull(row.getName()) + "'" : "This row";
		if (item.getRoleId() != null && item.getStatus() == RoleImportItemStatus.FAILED) {
			return roleName + " was created, but a follow-up step failed. " + item.getErrorMessage();
		}
		if (item.getRoleId() != null) {
			if (trimToNull(item.getWarningMessage()) != null) {
				return roleName + " was created successfully with warnings. " + item.getWarningMessage();
			}
			return roleName + " was created successfully.";
		}
		if (item.getStatus() == RoleImportItemStatus.FAILED) {
			return roleName + " was not created. " + item.getErrorMessage();
		}
		return roleName + " is waiting to be processed.";
	}

	private static String summaryMessage(RoleImportJob job, long fullySucceededCount, long createdWithErrorsCount) {
		if (job.getCompletedAt() == null) {
			return "Import is still processing. Refresh the job status to see the latest row results.";
		}
		if (job.getFailedCount() == 0) {
			return job.getSucceededCount() + " role(s) were created successfully.";
		}
		return fullySucceededCount + " row(s) fully succeeded and " + job.getFailedCount()
				+ " row(s) need attention. Fix the row errors and upload again.";
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static String csv(String value) {
		if (value == null) {
			return "";
		}
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}

