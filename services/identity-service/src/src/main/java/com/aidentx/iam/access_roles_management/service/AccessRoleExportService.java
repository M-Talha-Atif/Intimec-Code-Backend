package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFilters;
import com.aidentx.iam.access_roles_management.service.export.AccessRoleExportFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Streams an access-role export (CSV or JSON) for a tenant to an output stream.
 *
 * <p>Implementations must page over role rows internally so memory use stays
 * bounded regardless of tenant size, and must enforce a soft row cap so a
 * single client cannot exhaust server resources.</p>
 */
public interface AccessRoleExportService {

    /**
     * Result of a streaming export, returned so the controller can attach
     * truncation metadata as response headers.
     */
    record ExportResult(long rowCount, boolean truncated) {
    }

    /**
     * Streams the export to {@code outputStream}.
     *
     * @param tenantId        tenant identifier (required)
     * @param filters         list-filter parity with the regular list endpoint
     * @param format          desired output format
     * @param requestedColumns optional ordered subset of column keys; null/empty = all
     * @param outputStream    destination stream (the service does not close it)
     * @return summary including row count and whether the row cap was hit
     * @throws IOException on writer/stream failures
     */
    ExportResult streamExport(String tenantId,
                              AccessRoleExportFilters filters,
                              AccessRoleExportFormat format,
                              List<String> requestedColumns,
                              OutputStream outputStream) throws IOException;
}

