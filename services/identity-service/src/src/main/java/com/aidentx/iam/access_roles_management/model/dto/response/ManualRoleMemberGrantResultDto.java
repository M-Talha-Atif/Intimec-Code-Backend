package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing the outcome of a bulk manual role-grant operation.
 */
public class ManualRoleMemberGrantResultDto {

    private List<GrantedIdentity> added;
    private List<GrantedIdentity> skipped;
    private List<FailedIdentity> failed;

    public ManualRoleMemberGrantResultDto() {
    }

    public ManualRoleMemberGrantResultDto(List<GrantedIdentity> added,
                                          List<GrantedIdentity> skipped,
                                          List<FailedIdentity> failed) {
        this.added = added;
        this.skipped = skipped;
        this.failed = failed;
    }

    public List<GrantedIdentity> getAdded() {
        return added;
    }

    public void setAdded(List<GrantedIdentity> added) {
        this.added = added;
    }

    public List<GrantedIdentity> getSkipped() {
        return skipped;
    }

    public void setSkipped(List<GrantedIdentity> skipped) {
        this.skipped = skipped;
    }

    public List<FailedIdentity> getFailed() {
        return failed;
    }

    public void setFailed(List<FailedIdentity> failed) {
        this.failed = failed;
    }

    public int getRequestedCount() {
        return getAddedCount() + getSkippedCount() + getFailedCount();
    }

    public int getAddedCount() {
        return added != null ? added.size() : 0;
    }

    public int getSkippedCount() {
        return skipped != null ? skipped.size() : 0;
    }

    public int getFailedCount() {
        return failed != null ? failed.size() : 0;
    }

    public boolean hasFailures() {
        return failed != null && !failed.isEmpty();
    }

    /**
     * Lightweight identity summary used for added and skipped results.
     */
    public static class GrantedIdentity {

        private UUID identityId;
        private String identityName;
        private String identityType;

        public GrantedIdentity() {
        }

        public GrantedIdentity(UUID identityId, String identityName, String identityType) {
            this.identityId = identityId;
            this.identityName = identityName;
            this.identityType = identityType;
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
    }

    /**
     * Represents one identity that could not be granted the role.
     */
    public static class FailedIdentity {

        private UUID identityId;
        private String identityName;
        private String identityType;
        private String reason;

        public FailedIdentity() {
        }

        public FailedIdentity(UUID identityId, String identityName, String identityType, String reason) {
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

