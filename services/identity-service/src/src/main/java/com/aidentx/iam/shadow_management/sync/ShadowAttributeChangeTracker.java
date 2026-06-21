package com.aidentx.iam.shadow_management.sync;

import com.aidentx.iam.shadow_management.model.entity.ShadowAttributeChangelog;
import com.aidentx.iam.shadow_management.model.entity.ShadowAttributeChangelog.ChangeType;
import com.aidentx.iam.shadow_management.repository.ShadowAttributeChangelogRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Computes and persists attribute-level diffs between successive sync runs.
 * <p>
 * Called by the sync pipeline every time a shadow's attributes are updated
 * from fresh connector data. Produces an immutable audit trail of what changed,
 * when, and in which sync run — essential for IGA compliance and forensics.
 */
@Component
public class ShadowAttributeChangeTracker {

    private final ShadowAttributeChangelogRepository changelogRepository;
    private final IUnifiedLogger logger;

    public ShadowAttributeChangeTracker(
            ShadowAttributeChangelogRepository changelogRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.changelogRepository = changelogRepository;
        this.logger = loggerFactory.getLogger(ShadowAttributeChangeTracker.class);
    }

    /**
     * Diffs old and new shadow attributes and persists changelog entries.
     *
     * @param shadowId      The shadow ID
     * @param tenantId      Tenant context
     * @param resourceId    Resource (application) ID
     * @param syncRunId     Current sync run identifier
     * @param oldAttributes Previous attributes (may be null for new shadows)
     * @param newAttributes Fresh attributes from the connector
     * @return Number of attribute changes detected and recorded
     */
    public int trackChanges(UUID shadowId, String tenantId, String resourceId,
                            String syncRunId,
                            Map<String, Object> oldAttributes,
                            Map<String, Object> newAttributes) {

        if (newAttributes == null || newAttributes.isEmpty()) {
            return 0;
        }

        Map<String, Object> oldAttrs = oldAttributes != null ? oldAttributes : Collections.emptyMap();
        List<ShadowAttributeChangelog> entries = new ArrayList<>();

        // Collect all unique keys
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(oldAttrs.keySet());
        allKeys.addAll(newAttributes.keySet());

        for (String key : allKeys) {
            Object oldVal = oldAttrs.get(key);
            Object newVal = newAttributes.get(key);

            String oldStr = normalizeValue(oldVal);
            String newStr = normalizeValue(newVal);

            if (Objects.equals(oldStr, newStr)) {
                continue;
            }

            ShadowAttributeChangelog entry = new ShadowAttributeChangelog();
            entry.setShadowId(shadowId);
            entry.setTenantId(tenantId);
            entry.setResourceId(resourceId);
            entry.setSyncRunId(syncRunId);
            entry.setFieldName(key);
            entry.setOldValue(oldStr);
            entry.setNewValue(newStr);

            if (oldVal == null) {
                entry.setChangeType(ChangeType.ADDED);
            } else if (newVal == null) {
                entry.setChangeType(ChangeType.REMOVED);
            } else {
                entry.setChangeType(ChangeType.MODIFIED);
            }

            entries.add(entry);
        }

        if (!entries.isEmpty()) {
            changelogRepository.saveAll(entries);

            logger.debug().message("Shadow attribute changes recorded")
                    .detail("shadowId", shadowId.toString())
                    .detail("syncRunId", syncRunId)
                    .detail("changesCount", String.valueOf(entries.size()))
                    .log();
        }

        return entries.size();
    }

    /**
     * Normalizes an attribute value to a string for comparison and storage.
     * Handles lists, maps, and primitives consistently.
     */
    private String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return null;
            // Collapse single-char string lists (char-array artifact from connectors)
            if (list.size() > 1 && list.stream().allMatch(
                    item -> item instanceof String s && s.length() == 1)) {
                StringBuilder sb = new StringBuilder(list.size());
                for (Object item : list) sb.append((String) item);
                return sb.toString();
            }
            if (list.size() == 1) {
                return normalizeValue(list.get(0));
            }
            return list.toString();
        }
        if (value instanceof Map) {
            return value.toString();
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }
}
