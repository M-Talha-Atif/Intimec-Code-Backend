package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleExportConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.service.AccessRoleExportService;
import com.aidentx.iam.access_roles_management.service.AccessRoleService;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportColumn;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportColumnRegistry;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFilters;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFormat;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportWriter;
import com.aidentx.iam.access_roles_management.service.export.CsvAccessRoleExportWriter;
import com.aidentx.iam.access_roles_management.service.export.JsonAccessRoleExportWriter;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Default streaming implementation of {@link AccessRoleExportService}.
 *
 * <p>Iterates the existing {@link AccessRoleService#listRoles} contract one
 * page at a time so memory remains O({@link AccessRoleExportConstants#EXPORT_PAGE_SIZE}).
 * Aborts emission once {@link AccessRoleExportConstants#EXPORT_MAX_ROWS} is
 * reached and reports the truncation in the result.</p>
 */
@Service
public class AccessRoleExportServiceImpl implements AccessRoleExportService {

    private static final Sort EXPORT_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final AccessRoleService roleService;
    private final AccessRoleExportColumnRegistry columnRegistry;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public AccessRoleExportServiceImpl(AccessRoleService roleService,
                                       AccessRoleExportColumnRegistry columnRegistry,
                                       ObjectMapper objectMapper,
                                       UnifiedLoggerFactory loggerFactory) {
        this.roleService = roleService;
        this.columnRegistry = columnRegistry;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleExportConstants.LOG_CATEGORY);
    }

    @Override
    public ExportResult streamExport(String tenantId,
                                     AccessRoleExportFilters filters,
                                     AccessRoleExportFormat format,
                                     List<String> requestedColumns,
                                     OutputStream outputStream) throws IOException {
        validateInputs(tenantId, format, outputStream);
        AccessRoleExportFilters effectiveFilters = filters != null ? filters : AccessRoleExportFilters.empty();
        List<AccessRoleExportColumn> columns = columnRegistry.resolve(requestedColumns);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(AccessRoleExportConstants.ERROR_NO_COLUMNS_RESOLVED);
        }

        logger.info()
                .category(AccessRoleExportConstants.LOG_CATEGORY)
                .message(AccessRoleExportConstants.LOG_EXPORT_START)
                .details(Map.of(
                        AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                        AccessRoleExportConstants.DETAIL_FORMAT, format.token(),
                        AccessRoleExportConstants.DETAIL_COLUMNS, String.valueOf(columns.size())
                ))
                .log();

        long rowCount = 0L;
        boolean truncated = false;
        try (AccessRoleExportWriter writer = createWriter(format, outputStream)) {
            writer.begin(columns);

            int pageNumber = 0;
            while (true) {
                Pageable pageable = PageRequest.of(
                        pageNumber, AccessRoleExportConstants.EXPORT_PAGE_SIZE, EXPORT_SORT);
                Page<AccessRoleResponse> page = roleService.listRoles(
                        tenantId,
                        effectiveFilters.types(),
                        effectiveFilters.statuses(),
                        effectiveFilters.businessUnits(),
                        effectiveFilters.riskLevels(),
                        effectiveFilters.search(),
                        pageable);

                for (AccessRoleResponse role : page.getContent()) {
                    if (rowCount >= AccessRoleExportConstants.EXPORT_MAX_ROWS) {
                        truncated = true;
                        break;
                    }
                    writer.writeRow(role);
                    rowCount++;
                }

                logger.debug()
                        .category(AccessRoleExportConstants.LOG_CATEGORY)
                        .message(AccessRoleExportConstants.LOG_EXPORT_PAGE)
                        .details(Map.of(
                                AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                                AccessRoleExportConstants.DETAIL_PAGE_NUMBER, String.valueOf(pageNumber),
                                AccessRoleExportConstants.DETAIL_PAGE_SIZE,
                                String.valueOf(page.getNumberOfElements()),
                                AccessRoleExportConstants.DETAIL_ROW_COUNT, String.valueOf(rowCount)
                        ))
                        .log();

                if (truncated || !page.hasNext()) {
                    break;
                }
                pageNumber++;
            }

            writer.end();
        } catch (IOException ex) {
            logger.error()
                    .category(AccessRoleExportConstants.LOG_CATEGORY)
                    .message(AccessRoleExportConstants.LOG_EXPORT_FAILED)
                    .details(Map.of(
                            AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                            AccessRoleExportConstants.DETAIL_FORMAT, format.token(),
                            AccessRoleExportConstants.DETAIL_ERROR, ex.getClass().getSimpleName()
                    ))
                    .log();
            throw ex;
        }

        if (truncated) {
            logger.warn()
                    .category(AccessRoleExportConstants.LOG_CATEGORY)
                    .message(AccessRoleExportConstants.LOG_EXPORT_TRUNCATED)
                    .details(Map.of(
                            AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                            AccessRoleExportConstants.DETAIL_ROW_COUNT, String.valueOf(rowCount)
                    ))
                    .log();
        }

        logger.info()
                .category(AccessRoleExportConstants.LOG_CATEGORY)
                .message(AccessRoleExportConstants.LOG_EXPORT_COMPLETE)
                .details(Map.of(
                        AccessRoleExportConstants.DETAIL_TENANT_ID, tenantId,
                        AccessRoleExportConstants.DETAIL_FORMAT, format.token(),
                        AccessRoleExportConstants.DETAIL_ROW_COUNT, String.valueOf(rowCount)
                ))
                .log();

        return new ExportResult(rowCount, truncated);
    }

    private AccessRoleExportWriter createWriter(AccessRoleExportFormat format, OutputStream out) throws IOException {
        return switch (format) {
            case CSV -> new CsvAccessRoleExportWriter(out);
            case JSON -> new JsonAccessRoleExportWriter(out, objectMapper);
        };
    }

    private static void validateInputs(String tenantId, AccessRoleExportFormat format, OutputStream out) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (format == null) {
            throw new IllegalArgumentException("Export format is required");
        }
        if (out == null) {
            throw new IllegalArgumentException("Output stream is required");
        }
    }
}

