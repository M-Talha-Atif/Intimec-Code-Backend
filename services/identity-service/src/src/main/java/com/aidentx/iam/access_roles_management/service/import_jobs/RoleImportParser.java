package com.aidentx.iam.access_roles_management.service.import_jobs;

import com.aidentx.iam.access_roles_management.model.dto.request.RoleImportRowDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Parses uploaded access-role import files into canonical row DTOs.
 *
 * <p>Implementations are format-specific adapters only. They should translate
 * JSON/CSV input into {@link RoleImportRowDto} objects without creating roles or
 * performing database lookups. Structural and business validation are handled by
 * the import service and worker respectively.</p>
 */
public interface RoleImportParser {

    /**
     * Reads the uploaded file stream and returns import rows in the same order as
     * they appear in the file. Row order is important because row numbers are
     * stored on {@code RoleImportJobItem} for user-friendly error reporting.
     *
     * @param inputStream uploaded file content
     * @return parsed rows, preserving source order
     * @throws IOException when the file cannot be read or converted
     */
    List<RoleImportRowDto> parse(InputStream inputStream) throws IOException;
}

