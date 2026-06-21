package com.aidentx.iam.access_roles_management.service.export;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aggregates all {@link AccessRoleExportColumn} beans into an ordered registry
 * and resolves the column set for a given request.
 *
 * <p>Spring injects the column list ordered by {@link org.springframework.core.annotation.Order}.
 * This allows future dynamic-attribute providers to insert their own columns
 * without changes here.</p>
 */
@Component
public class AccessRoleExportColumnRegistry {

    private final List<AccessRoleExportColumn> allColumns;
    private final Map<String, AccessRoleExportColumn> byKey;

    public AccessRoleExportColumnRegistry(List<AccessRoleExportColumn> columns) {
        this.allColumns = List.copyOf(columns);
        Map<String, AccessRoleExportColumn> index = new LinkedHashMap<>();
        for (AccessRoleExportColumn column : this.allColumns) {
            index.put(column.key().toLowerCase(Locale.ROOT), column);
        }
        this.byKey = Map.copyOf(index);
    }

    /**
     * Resolves the columns to emit. When {@code requestedKeys} is null/empty,
     * returns all registered columns in their natural (Spring-ordered) order;
     * otherwise returns the subset matching the requested keys in the order
     * given. Unknown keys are silently ignored to keep the export forgiving.
     *
     * @param requestedKeys optional list of column keys (case-insensitive)
     * @return ordered list of columns to emit; never null, may be empty if all
     *         requested keys were unknown
     */
    public List<AccessRoleExportColumn> resolve(List<String> requestedKeys) {
        if (requestedKeys == null || requestedKeys.isEmpty()) {
            return allColumns;
        }
        List<AccessRoleExportColumn> result = new ArrayList<>(requestedKeys.size());
        for (String key : requestedKeys) {
            if (key == null) {
                continue;
            }
            AccessRoleExportColumn column = byKey.get(key.trim().toLowerCase(Locale.ROOT));
            if (column != null) {
                result.add(column);
            }
        }
        return List.copyOf(result);
    }

    /** Returns all registered columns in registry order. */
    public List<AccessRoleExportColumn> all() {
        return allColumns;
    }
}

