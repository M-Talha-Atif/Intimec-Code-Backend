package com.aidentx.iam.access_roles_management.service.export;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;

import java.io.IOException;
import java.util.List;

/**
 * Streaming writer SPI that emits access-role rows in a specific output format.
 *
 * <p>Lifecycle: {@link #begin(List)} → repeated {@link #writeRow(AccessRoleResponse)}
 * → {@link #end()}. Implementations must be stateful per-export and not shared
 * across threads.</p>
 */
public interface AccessRoleExportWriter extends AutoCloseable {

    /** Writes the document preamble (CSV header / JSON array open). */
    void begin(List<AccessRoleExportColumn> columns) throws IOException;

    /** Emits a single role row using the columns provided to {@link #begin(List)}. */
    void writeRow(AccessRoleResponse role) throws IOException;

    /** Writes the document trailer (JSON array close, flush). */
    void end() throws IOException;

    /** Format produced by this writer. */
    AccessRoleExportFormat format();

    @Override
    void close() throws IOException;
}

