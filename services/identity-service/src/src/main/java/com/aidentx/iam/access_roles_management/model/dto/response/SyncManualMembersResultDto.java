package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the manual member sync (desired-state PUT) operation.
 *
 * <p>The caller sends the full list of identity IDs that should currently hold
 * manual assignments to the role. The backend computes the diff and:
 * <ul>
 *   <li>Grants identities not yet assigned (added)</li>
 *   <li>Revokes identities no longer in the desired state (revoked)</li>
 *   <li>Reports identities that failed validation (failed)</li>
 * </ul>
 *
 * <p>Each identity entry includes an entitlement impact summary showing which
 * entitlements were provisioned/deprovisioned in the {@code entitlement_assignments}
 * table as a result of the grant or revoke.</p>
 *
 * <pre>{@code
 * {
 *   "added": [ { "identityId": "...", "identityName": "John", "identityType": "EMPLOYEE", "entitlementImpact": [...] } ],
 *   "revoked": [ { "identityId": "...", "identityName": "Jane", "identityType": "EMPLOYEE", "entitlementImpact": [...] } ],
 *   "skipped": [ ... ],
 *   "failed": [ { "identityId": "...", "reason": "..." } ],
 *   "totalActiveCount": 12
 * }
 * }</pre>
 */
public class SyncManualMembersResultDto {

    private final List<MemberSyncEntry> added;
    private final List<MemberSyncEntry> revoked;
    private final List<MemberSyncEntry> skipped;
    private final List<FailedMember> failed;
    private final int totalActiveCount;

    public SyncManualMembersResultDto(List<MemberSyncEntry> added,
                                      List<MemberSyncEntry> revoked,
                                      List<MemberSyncEntry> skipped,
                                      List<FailedMember> failed,
                                      int totalActiveCount) {
        this.added = added != null ? added : Collections.emptyList();
        this.revoked = revoked != null ? revoked : Collections.emptyList();
        this.skipped = skipped != null ? skipped : Collections.emptyList();
        this.failed = failed != null ? failed : Collections.emptyList();
        this.totalActiveCount = totalActiveCount;
    }

    public List<MemberSyncEntry> getAdded() {
        return added;
    }

    public List<MemberSyncEntry> getRevoked() {
        return revoked;
    }

    public List<MemberSyncEntry> getSkipped() {
        return skipped;
    }

    public List<FailedMember> getFailed() {
        return failed;
    }

    public int getAddedCount() {
        return added.size();
    }

    public int getRevokedCount() {
        return revoked.size();
    }

    public int getSkippedCount() {
        return skipped.size();
    }

    public int getFailedCount() {
        return failed.size();
    }

    public int getTotalActiveCount() {
        return totalActiveCount;
    }

    public boolean hasFailures() {
        return !failed.isEmpty();
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !revoked.isEmpty() || !failed.isEmpty();
    }

    /**
     * Represents one identity that was successfully added, revoked, or skipped.
     */
    public static class MemberSyncEntry {

        private UUID identityId;
        private String identityName;
        private String identityType;
        private List<EntitlementImpact> entitlementImpact;

        public MemberSyncEntry() {
        }

        public MemberSyncEntry(UUID identityId, String identityName, String identityType,
                               List<EntitlementImpact> entitlementImpact) {
            this.identityId = identityId;
            this.identityName = identityName;
            this.identityType = identityType;
            this.entitlementImpact = entitlementImpact != null ? entitlementImpact : Collections.emptyList();
        }

        public UUID getIdentityId() {
            return identityId;
        }

        public void setIdentityId(UUID identityId) {
            this.identityId = identityId;
        }

        public String getIdentityName() {
            return identityName;
        }

        public void setIdentityName(String identityName) {
            this.identityName = identityName;
        }

        public String getIdentityType() {
            return identityType;
        }

        public void setIdentityType(String identityType) {
            this.identityType = identityType;
        }

        public List<EntitlementImpact> getEntitlementImpact() {
            return entitlementImpact;
        }

        public void setEntitlementImpact(List<EntitlementImpact> entitlementImpact) {
            this.entitlementImpact = entitlementImpact;
        }
    }

    /**
     * Summary of a single entitlement provisioned or deprovisioned as part of
     * a member grant/revoke, including whether it exists in the
     * {@code entitlement_assignments} table.
     */
    public static class EntitlementImpact {

        private UUID entitlementId;
        private String entitlementName;
        private UUID applicationId;
        private String applicationName;
        private String action; // PROVISIONED or DEPROVISIONED
        private boolean existedInAssignmentTable;

        public EntitlementImpact() {
        }

        public EntitlementImpact(UUID entitlementId, String entitlementName,
                                 UUID applicationId, String applicationName,
                                 String action, boolean existedInAssignmentTable) {
            this.entitlementId = entitlementId;
            this.entitlementName = entitlementName;
            this.applicationId = applicationId;
            this.applicationName = applicationName;
            this.action = action;
            this.existedInAssignmentTable = existedInAssignmentTable;
        }

        public UUID getEntitlementId() {
            return entitlementId;
        }

        public void setEntitlementId(UUID entitlementId) {
            this.entitlementId = entitlementId;
        }

        public String getEntitlementName() {
            return entitlementName;
        }

        public void setEntitlementName(String entitlementName) {
            this.entitlementName = entitlementName;
        }

        public UUID getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(UUID applicationId) {
            this.applicationId = applicationId;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public boolean isExistedInAssignmentTable() {
            return existedInAssignmentTable;
        }

        public void setExistedInAssignmentTable(boolean existedInAssignmentTable) {
            this.existedInAssignmentTable = existedInAssignmentTable;
        }
    }

    /**
     * Represents one identity for which the sync operation failed.
     */
    public static class FailedMember {

        private UUID identityId;
        private String identityName;
        private String identityType;
        private String reason;

        public FailedMember() {
        }

        public FailedMember(UUID identityId, String identityName, String identityType, String reason) {
            this.identityId = identityId;
            this.identityName = identityName;
            this.identityType = identityType;
            this.reason = reason;
        }

        public UUID getIdentityId() {
            return identityId;
        }

        public void setIdentityId(UUID identityId) {
            this.identityId = identityId;
        }

        public String getIdentityName() {
            return identityName;
        }

        public void setIdentityName(String identityName) {
            this.identityName = identityName;
        }

        public String getIdentityType() {
            return identityType;
        }

        public void setIdentityType(String identityType) {
            this.identityType = identityType;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}

