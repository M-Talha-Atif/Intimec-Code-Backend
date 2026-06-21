package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing the outcome of a bulk add-child-roles operation.
 * <p>
 * Supports partial success: some children may be added successfully while
 * others fail validation (circular dependency, self-reference, etc.).
 * </p>
 */
public class AddChildRolesResultDto {

    /** IDs of child roles that were successfully linked. */
    private List<UUID> added;

    /** IDs of child roles that were skipped because they already exist. */
    private List<UUID> skipped;

    /** Details of child roles that failed validation. */
    private List<FailedChild> failed;

    /**
     * Represents a single child role that could not be added.
     */
    public static class FailedChild {

        /** The child role ID that failed. */
        private UUID childRoleId;

        /** Human-readable reason for the failure. */
        private String reason;

        public FailedChild() {}

        public FailedChild(UUID childRoleId, String reason) {
            this.childRoleId = childRoleId;
            this.reason = reason;
        }

        public UUID getChildRoleId() { return childRoleId; }
        public void setChildRoleId(UUID childRoleId) { this.childRoleId = childRoleId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public AddChildRolesResultDto() {}

    public AddChildRolesResultDto(List<UUID> added, List<UUID> skipped, List<FailedChild> failed) {
        this.added = added;
        this.skipped = skipped;
        this.failed = failed;
    }

    public List<UUID> getAdded() { return added; }
    public void setAdded(List<UUID> added) { this.added = added; }

    public List<UUID> getSkipped() { return skipped; }
    public void setSkipped(List<UUID> skipped) { this.skipped = skipped; }

    public List<FailedChild> getFailed() { return failed; }
    public void setFailed(List<FailedChild> failed) { this.failed = failed; }

    /**
     * Returns true if any child role failed validation.
     */
    public boolean hasFailures() {
        return failed != null && !failed.isEmpty();
    }
}