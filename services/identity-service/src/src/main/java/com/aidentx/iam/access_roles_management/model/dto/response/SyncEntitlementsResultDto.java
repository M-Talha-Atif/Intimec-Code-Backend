package com.aidentx.iam.access_roles_management.model.dto.response;

/**
 * Response DTO for the entitlement sync (desired-state PUT) operation.
 *
 * <p>Provides a summary of what changed during the sync so the frontend
 * can display appropriate success/info messages.</p>
 *
 * <h3>Why a Separate DTO?</h3>
 * <p>The sync operation is conceptually different from individual add/remove.
 * The caller sends a desired state and wants to know <em>what changed</em>,
 * not just "success/fail". This DTO tells the caller exactly how many
 * entitlements were added, how many were removed, and what the final
 * total is.</p>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "addedCount": 3,
 *   "removedCount": 1,
 *   "totalCount": 7
 * }
 * }</pre>
 */
public class SyncEntitlementsResultDto {

    /** Number of entitlements that were newly linked to the role. */
    private final int addedCount;

    /** Number of entitlements that were unlinked from the role. */
    private final int removedCount;

    /** Total number of entitlements linked to the role after the sync. */
    private final int totalCount;

    /**
     * Constructs the result DTO.
     *
     * @param addedCount   number of entitlements added
     * @param removedCount number of entitlements removed
     * @param totalCount   total entitlements after sync
     */
    public SyncEntitlementsResultDto(int addedCount, int removedCount, int totalCount) {
        this.addedCount = addedCount;
        this.removedCount = removedCount;
        this.totalCount = totalCount;
    }

    /**
     * Returns the number of entitlements that were newly linked.
     *
     * @return added count
     */
    public int getAddedCount() {
        return addedCount;
    }

    /**
     * Returns the number of entitlements that were unlinked.
     *
     * @return removed count
     */
    public int getRemovedCount() {
        return removedCount;
    }

    /**
     * Returns the total entitlement count after the sync operation.
     * This is read from the link table after all adds/removes complete,
     * so it reflects the true final state.
     *
     * @return total count
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Returns true if the sync actually changed something
     * (at least one entitlement was added or removed).
     *
     * @return true if any changes were made
     */
    public boolean hasChanges() {
        return addedCount > 0 || removedCount > 0;
    }
}