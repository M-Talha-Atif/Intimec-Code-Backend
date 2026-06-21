package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a task dry-run/preview execution.
 * Contains a complete simulation of what WOULD happen if the task were executed for real,
 * without any data being persisted.
 */
@Data
@Builder
public class TaskPreviewResponse {

    /** The execution ID of the persisted preview run (for history navigation) */
    private String executionId;

    /** Summary statistics */
    private int totalAccountsScanned;
    private int totalInDataSource;
    private boolean wasTruncated;
    private int wouldCreate;
    private int wouldUpdate;
    private int wouldLink;
    private int wouldDelete;
    private int unchanged;
    private int disputed;
    private int errors;

    /** Per-account preview details */
    private List<AccountPreview> accounts;

    /** Deletion previews (reconciliation only — absent accounts) */
    private List<DeletionPreviewEntry> deletions;

    /** Entitlement deletion previews (entitlements that would be removed during cleanup) */
    private List<EntitlementDeletionPreviewEntry> entitlementDeletions;

    /** Timestamp when the preview was executed */
    private Instant previewedAt;

    /** Duration of the preview in milliseconds */
    private long durationMs;

    /** Human-readable summary message */
    private String summary;

    @Data
    @Builder
    public static class AccountPreview {
        /** The account UID from the connector */
        private String resourceUid;

        /** The shadow ID (existing) or "new" for unmatched */
        private String shadowId;

        /** Classified situation: LINKED, UNMATCHED, UNLINKED, DISPUTED */
        private String situation;

        /** Whether the shadow already existed */
        private boolean shadowExists;

        /** The resolved reaction action (e.g., "add-focus", "link", "sync") */
        private String resolvedReaction;

        /** The proto action that would be taken */
        private String action;

        /** Identity that would be linked/created/updated (if any) */
        private String linkedIdentityId;

        /** Number of attributes that would change */
        private int attributeChanges;

        /** Detailed attribute changes (old → new) */
        private List<AttributeChangeEntry> changes;

        /** Human-readable message */
        private String message;
    }

    @Data
    @Builder
    public static class AttributeChangeEntry {
        private String fieldName;
        private String oldValue;
        private String newValue;
    }

    @Data
    @Builder
    public static class DeletionPreviewEntry {
        private String shadowId;
        private String resourceUid;
        private String linkedIdentityId;
        private String resolvedReaction;
    }

    @Data
    @Builder
    public static class EntitlementDeletionPreviewEntry {
        private String entitlementId;
        private String sourceUid;
        private String displayName;
        private String objectClass;
        private int assignmentCount;
    }

    @Data
    @Builder
    public static class EntitlementPreview {
        private String resourceUid;
        private String objectClass;
        private String action;
        private String message;
    }
}
