package com.aidentx.iam.access_roles_management.service.export;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;

/**
 * Extensible export column definition.
 *
 * <p>Each implementation describes one logical column / JSON field that should
 * be emitted by the access-role export. Adding new columns (e.g. tenant-defined
 * dynamic attributes in the future) is a matter of registering an additional
 * Spring bean implementing this interface; the writers and service iterate over
 * whatever the {@link AccessRoleExportColumnRegistry} returns and need no
 * changes.</p>
 */
public interface AccessRoleExportColumn {

    /**
     * Machine-readable, stable column key.
     *
     * <p>Used as the CSV header, the JSON field name, and the value accepted by
     * the {@code columns} query-param subset selector.</p>
     */
    String key();

    /**
     * Human-readable header used in CSV downloads. Defaults to {@link #key()}.
     */
    default String header() {
        return key();
    }

    /**
     * Extracts the raw value for this column from a role row.
     *
     * <p>Implementations may return any of: {@link String}, {@link Number},
     * {@link Boolean}, {@link java.time.temporal.TemporalAccessor},
     * {@link java.util.Collection}, {@link java.util.UUID}, or {@code null}.
     * Writers are responsible for type-aware serialization.</p>
     *
     * @param role the role row source
     * @return raw column value, possibly {@code null}
     */
    Object extract(AccessRoleResponse role);
}

