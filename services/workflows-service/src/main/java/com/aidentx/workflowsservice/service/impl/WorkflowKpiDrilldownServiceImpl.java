package com.aidentx.workflowsservice.service.impl;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.config.analytics.WorkflowAnalyticsConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowKpiDrilldownConstants;
import com.aidentx.workflowsservice.dto.request.KpiDrilldownRequestDTO;
import com.aidentx.workflowsservice.dto.response.KpiDrilldownResponseDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowKpiEntryDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.service.WorkflowKpiDrilldownService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of WorkflowKpiDrilldownService
 *
 * Retrieves and filters workflows based on KPI type,
 * applies pagination and sorting, and maps to DTOs.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Service
public class WorkflowKpiDrilldownServiceImpl implements WorkflowKpiDrilldownService {

    private final IUnifiedLogger logger;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;

    public WorkflowKpiDrilldownServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.logger = loggerFactory.getLogger(
                WorkflowKpiDrilldownServiceImpl.class,
                WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KpiDrilldownResponseDTO getKpiDrilldown(KpiDrilldownRequestDTO requestDTO) {
        logger.info()
                .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, requestDTO.getKpiType())
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_PAGE, requestDTO.getPage().toString())
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_PAGE_SIZE, requestDTO.getPageSize().toString())
                .log();

        try {
            // Validate KPI type
            validateKpiType(requestDTO.getKpiType());

            logger.debug()
                    .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_CALCULATION_START)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, requestDTO.getKpiType())
                    .log();

            // Create pageable with sorting
            Pageable pageable = createPageable(requestDTO);

            // Fetch workflows based on KPI type
            Page<Workflow> workflowPage = fetchWorkflowsByKpiType(requestDTO.getKpiType(), pageable);

            // Map to DTOs
            List<WorkflowKpiEntryDTO> workflowEntries = workflowPage.getContent().stream()
                    .map(workflow -> mapToWorkflowKpiEntryDTO(workflow, requestDTO.getKpiType()))
                    .collect(Collectors.toList());

            logger.info()
                    .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_CALCULATION_COMPLETE)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, requestDTO.getKpiType())
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_TOTAL_WORKFLOWS, String.valueOf(workflowPage.getTotalElements()))
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_CURRENT_PAGE, String.valueOf(workflowPage.getNumber()))
                    .log();

            return KpiDrilldownResponseDTO.builder()
                    .kpiType(requestDTO.getKpiType())
                    .totalWorkflows(workflowPage.getTotalElements())
                    .currentPage(workflowPage.getNumber())
                    .pageSize(workflowPage.getSize())
                    .totalPages(workflowPage.getTotalPages())
                    .hasNext(workflowPage.hasNext())
                    .hasPrevious(workflowPage.hasPrevious())
                    .workflows(workflowEntries)
                    .build();

        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();
            throw e;
        } catch (Exception e) {
            logger.error()
                    .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            throw new RuntimeException(
                    WorkflowKpiDrilldownConstants.ERR_KPI_DRILLDOWN_CALCULATION_FAILED,
                    e
            );
        }
    }

    /**
     * Validate KPI type
     *
     * @param kpiType KPI type to validate
     * @throws IllegalArgumentException if KPI type is invalid
     */
    private void validateKpiType(String kpiType) {
        if (kpiType == null || kpiType.trim().isEmpty()) {
            throw new IllegalArgumentException(WorkflowKpiDrilldownConstants.ERR_INVALID_KPI_TYPE);
        }

        boolean isValid = kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_TOTAL_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_ACTIVE_DRAFT_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_AUTOMATED_MANUAL_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_SUCCESS_RATE_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_AVERAGE_EXECUTION_TIME_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_FAILED_EXECUTION_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_HIGH_RISK_WORKFLOWS) ||
                kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_AI_DRIVEN_WORKFLOWS);

        if (!isValid) {
            logger.error()
                    .message(WorkflowKpiDrilldownConstants.LOG_INVALID_KPI_TYPE)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, kpiType)
                    .log();
            throw new IllegalArgumentException(WorkflowKpiDrilldownConstants.ERR_INVALID_KPI_TYPE);
        }
    }

    /**
     * Create pageable with sorting
     *
     * @param requestDTO Request containing pagination and sorting parameters
     * @return Pageable object
     */
    private Pageable createPageable(KpiDrilldownRequestDTO requestDTO) {
        // Validate and cap page size
        int pageSize = Math.min(
                requestDTO.getPageSize(),
                WorkflowKpiDrilldownConstants.MAX_PAGE_SIZE
        );

        // For AVERAGE_EXECUTION_TIME_WORKFLOWS, we need to fetch all and sort in memory
        // So we return an unpaged request for that case
        if (requestDTO.getKpiType().equals(WorkflowKpiDrilldownConstants.KPI_TYPE_AVERAGE_EXECUTION_TIME_WORKFLOWS)) {
            return Pageable.unpaged();
        }

        // Create sort
        Sort.Direction direction = requestDTO.getSortDirection().equalsIgnoreCase(WorkflowKpiDrilldownConstants.SORT_DIRECTION_DESC)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, requestDTO.getSortBy());

        return PageRequest.of(requestDTO.getPage(), pageSize, sort);
    }

    /**
     * Fetch workflows based on KPI type
     *
     * @param kpiType KPI type
     * @param pageable Pagination and sorting parameters
     * @return Page of workflows
     */
    private Page<Workflow> fetchWorkflowsByKpiType(String kpiType, Pageable pageable) {
        return switch (kpiType) {
            case WorkflowKpiDrilldownConstants.KPI_TYPE_TOTAL_WORKFLOWS ->
                    workflowRepository.findAll(pageable);

            case WorkflowKpiDrilldownConstants.KPI_TYPE_ACTIVE_DRAFT_WORKFLOWS ->
                    workflowRepository.findByStatusIn(
                            List.of(
                                    Workflow.WorkflowStatus.ACTIVE.name(),
                                    Workflow.WorkflowStatus.DRAFT.name()
                            ),
                            pageable
                    );

            case WorkflowKpiDrilldownConstants.KPI_TYPE_AUTOMATED_MANUAL_WORKFLOWS ->
                    workflowRepository.findAll(pageable);

            case WorkflowKpiDrilldownConstants.KPI_TYPE_SUCCESS_RATE_WORKFLOWS ->
                    workflowRepository.findAllWithExecutions(pageable);

            case WorkflowKpiDrilldownConstants.KPI_TYPE_AVERAGE_EXECUTION_TIME_WORKFLOWS ->
                    workflowRepository.findAllWithExecutions(pageable);

            case WorkflowKpiDrilldownConstants.KPI_TYPE_FAILED_EXECUTION_WORKFLOWS ->
                    workflowRepository.findAllWithFailedExecutions(pageable);

            case WorkflowKpiDrilldownConstants.KPI_TYPE_HIGH_RISK_WORKFLOWS ->
                    workflowRepository.findByRiskScoreGreaterThanEqual(
                            WorkflowKpiDrilldownConstants.HIGH_RISK_THRESHOLD,
                            pageable
                    );

            case WorkflowKpiDrilldownConstants.KPI_TYPE_AI_DRIVEN_WORKFLOWS ->
                    workflowRepository.findByCategory(Workflow.WorkflowCategory.AI_RISK, pageable);

            default -> workflowRepository.findAll(pageable);
        };
    }

    /**
     * Map Workflow entity to WorkflowKpiEntryDTO
     *
     * @param workflow Workflow entity
     * @param kpiType KPI type (needed to determine if avgExecutionTime should be calculated)
     * @return WorkflowKpiEntryDTO
     */
    private WorkflowKpiEntryDTO mapToWorkflowKpiEntryDTO(Workflow workflow, String kpiType) {
        // Get execution count for this workflow
        Long executionCount = executionRepository.countByWorkflowId(workflow.getId());

        // Calculate average execution time if needed
        Double avgExecutionTimeHours = null;
        if (kpiType.equals(WorkflowKpiDrilldownConstants.KPI_TYPE_AVERAGE_EXECUTION_TIME_WORKFLOWS)) {
            Double avgExecutionTimeMs = executionRepository.getAverageExecutionTimeByWorkflowId(workflow.getId());
            avgExecutionTimeHours = convertMsToHours(avgExecutionTimeMs);
        }

        return WorkflowKpiEntryDTO.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .category(formatCategoryName(workflow.getCategory()))
                .status(workflow.getStatus().name())
                .triggerType(formatTriggerType(workflow.getTriggerType()))
                .riskScore(workflow.getRiskScore())
                .executionCount(executionCount != null ? executionCount : WorkflowKpiDrilldownConstants.DEFAULT_EXECUTION_COUNT)
                .avgExecutionTimeHours(avgExecutionTimeHours)
                .description(workflow.getDescription())
                .version(workflow.getVersion())
                .build();
    }

    /**
     * Convert milliseconds to hours
     *
     * @param milliseconds Execution time in milliseconds
     * @return Execution time in hours, or null if milliseconds is null
     */
    private Double convertMsToHours(Double milliseconds) {
        if (milliseconds == null) {
            return null;
        }

        double hours = milliseconds / WorkflowAnalyticsConstants.MS_PER_HOUR;
        return Math.round(hours * WorkflowAnalyticsConstants.HOUR_SCALE) /
               WorkflowAnalyticsConstants.HOUR_SCALE;
    }

    /**
     * Format category name for display
     *
     * @param category Workflow category enum
     * @return Formatted category name with spaces
     */
    private String formatCategoryName(Workflow.WorkflowCategory category) {
        if (category == null) {
            return WorkflowKpiDrilldownConstants.EMPTY_STRING;
        }

        return switch (category) {
            case JOINER_MOVER_LEAVER -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_JML;
            case ACCESS_REQUEST -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_ACCESS_REQUEST;
            case CERTIFICATION -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_CERTIFICATION;
            case REVOCATION -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_REVOCATION;
            case EXCEPTION -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_EXCEPTION;
            case AI_RISK -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_AI_RISK;
        };
    }

    /**
     * Format trigger type for display
     *
     * @param triggerType Trigger type enum
     * @return Formatted trigger type with spaces
     */
    private String formatTriggerType(Workflow.TriggerType triggerType) {
        if (triggerType == null) {
            return WorkflowKpiDrilldownConstants.EMPTY_STRING;
        }

        return switch (triggerType) {
            case EVENT_BASED -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_EVENT_BASED;
            case TIME_BASED -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_TIME_BASED;
            case MANUAL -> WorkflowKpiDrilldownConstants.DISPLAY_NAME_MANUAL;
        };
    }
}