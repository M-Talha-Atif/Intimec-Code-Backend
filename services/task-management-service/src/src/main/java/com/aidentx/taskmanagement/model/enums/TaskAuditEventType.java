package com.aidentx.taskmanagement.model.enums;

/**
 * Structural milestone event types written to the durable PostgreSQL audit trail.
 * <p>
 * These are the headline events that must survive indefinitely for IGA compliance
 * (SOX, ISO 27001, ISO 27701). Per-account detail logs remain in OpenSearch.
 * Only ~10–15 rows are written per task run, so the volume is negligible.
 */
public enum TaskAuditEventType {

    // ── Lifecycle ────────────────────────────────────────────────────────────
    /** Task execution thread acquired the advisory lock and started processing. */
    TASK_STARTED,

    /** Task execution completed successfully — all phases finished. */
    TASK_COMPLETED,

    /** Task execution failed with an unhandled exception. */
    TASK_FAILED,

    /** Task was explicitly stopped by an operator before natural completion. */
    TASK_CANCELLED,

    /**
     * Task was skipped because another execution for the same application already
     * held the PostgreSQL advisory lock. No accounts were processed.
     */
    TASK_SKIPPED,

    // ── Connection ───────────────────────────────────────────────────────────
    /** testConnection() RPC result — success or failure. */
    CONNECTION_TESTED,

    // ── Phase 1: Entitlement Import ──────────────────────────────────────────
    /** Phase 1 (entitlement import) started. */
    ENTITLEMENT_IMPORT_STARTED,

    /**
     * Phase 1 completed. metadata contains:
     * {@code {"total": N, "created": N, "updated": N, "errors": N}}
     */
    ENTITLEMENT_IMPORT_COMPLETED,

    /**
     * Phase 1 was skipped because it was disabled in the task configuration
     * or the connector does not support __GROUP__.
     */
    ENTITLEMENT_IMPORT_SKIPPED,

    /**
     * Phase 1 was aborted because the gRPC deadline expired on the __GROUP__
     * searchObjects stream. The task itself is then marked FAILED.
     */
    ENTITLEMENT_IMPORT_TIMEOUT,

    // ── Phase 2: Account Import ───────────────────────────────────────────────
    /** Phase 2 (account import) started. */
    ACCOUNT_IMPORT_STARTED,

    /**
     * A batch of accounts was sent to identity-service and processed.
     * metadata contains:
     * {@code {"batchSize": N, "totalSoFar": N, "created": N, "updated": N,
     *          "linked": N, "unmatched": N, "disputed": N, "errors": N}}
     */
    ACCOUNT_BATCH_PROCESSED,

    /**
     * Phase 2 was skipped because account import was disabled in the task
     * configuration.
     */
    ACCOUNT_IMPORT_SKIPPED,

    // ── Phase 3: Finalization ─────────────────────────────────────────────────
    /**
     * Finalization (deletion detection + cleanup) completed.
     * metadata contains:
     * {@code {"shadowsDeleted": N, "identitiesDisabled": N}}
     */
    SYNC_FINALIZED
}
