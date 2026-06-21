package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the child role sync (desired-state PUT) operation.
 *
 * <p>Unlike entitlement sync, child role additions can <strong>partially fail</strong>
 * due to validation rules (circular dependency, self-reference, role not found).
 * This DTO provides three lists: successfully added, successfully removed,
 * and failed with reasons.</p>
 *
 * <p>The frontend uses this to:</p>
 * <ul>
 *   <li>Show success toast for added/removed children</li>
 *   <li>Show per-child error toasts for failures with the specific reason</li>
 *   <li>Revert failed children from the UI selection</li>
 * </ul>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "added": ["uuid-1", "uuid-2"],
 *   "removed": ["uuid-3"],
 *   "failed": [
 *     {"childRoleId": "uuid-4", "reason": "Circular dependency detected"}
 *   ],
 *   "totalCount": 5
 * }
 * }</pre>
 */
public class SyncChildRolesResultDto {

    /** Child role IDs that were successfully added to the composition. */
    private final List<UUID> added;

    /** Child role IDs that were successfully removed from the composition. */
    private final List<UUID> removed;

    /**
     * Child roles that failed validation during addition.
     * Reuses {@link AddChildRolesResultDto.FailedChild} which carries
     * the child role ID and the human-readable failure reason.
     */
    private final List<AddChildRolesResultDto.FailedChild> failed;

    /** Total number of child roles after the sync operation. */
    private final int totalCount;

    /**
     * Constructs the result DTO.
     *
     * @param added      list of successfully added child role IDs
     * @param removed    list of successfully removed child role IDs
     * @param failed     list of children that failed validation (with reasons)
     * @param totalCount total child count after sync
     */
    public SyncChildRolesResultDto(
            List<UUID> added,
            List<UUID> removed,
            List<AddChildRolesResultDto.FailedChild> failed,
            int totalCount) {
        this.added = added != null ? added : Collections.emptyList();
        this.removed = removed != null ? removed : Collections.emptyList();
        this.failed = failed != null ? failed : Collections.emptyList();
        this.totalCount = totalCount;
    }

    /**
     * Returns the list of child role IDs that were successfully added.
     *
     * @return added child role IDs
     */
    public List<UUID> getAdded() {
        return added;
    }

    /**
     * Returns the list of child role IDs that were successfully removed.
     *
     * @return removed child role IDs
     */
    public List<UUID> getRemoved() {
        return removed;
    }

    /**
     * Returns the list of child roles that failed validation during addition.
     * Each entry contains the child role ID and the failure reason.
     *
     * @return failed children with reasons
     */
    public List<AddChildRolesResultDto.FailedChild> getFailed() {
        return failed;
    }

    /**
     * Returns the total child count after the sync operation.
     *
     * @return total count
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Returns true if any children failed validation.
     *
     * @return true if there are failures
     */
    public boolean hasFailures() {
        return !failed.isEmpty();
    }

    /**
     * Returns true if the sync actually changed something.
     *
     * @return true if any children were added, removed, or failed
     */
    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !failed.isEmpty();
    }
}