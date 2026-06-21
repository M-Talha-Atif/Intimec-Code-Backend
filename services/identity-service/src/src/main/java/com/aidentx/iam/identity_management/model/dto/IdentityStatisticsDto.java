package com.aidentx.iam.identity_management.model.dto;

import com.aidentx.iam.identity_management.model.enums.IdentityType;
import java.util.Map;

/**
 * Data Transfer Object for identity statistics.
 *
 * <p>
 * This DTO contains various statistics and metrics about identities within a
 * tenant,
 * calculated on-the-fly without relying on pre-computed database tables.
 * </p>
 *
 * @since 1.0
 */
public class IdentityStatisticsDto {

    private long totalIdentities;
    private Map<IdentityType, Long> identitiesByType;
    private Map<IdentityType, IdentityTypeStatistics> statisticsByType;
    private long totalExtendedAttributes;
    private Map<String, Long> extendedAttributeUsage;
    private long activeIdentities;
    private long inactiveIdentities;

    public IdentityStatisticsDto() {
    }

    public IdentityStatisticsDto(long totalIdentities, Map<IdentityType, Long> identitiesByType,
            Map<IdentityType, IdentityTypeStatistics> statisticsByType,
            long totalExtendedAttributes, Map<String, Long> extendedAttributeUsage,
            long activeIdentities, long inactiveIdentities) {
        this.totalIdentities = totalIdentities;
        this.identitiesByType = identitiesByType;
        this.statisticsByType = statisticsByType;
        this.totalExtendedAttributes = totalExtendedAttributes;
        this.extendedAttributeUsage = extendedAttributeUsage;
        this.activeIdentities = activeIdentities;
        this.inactiveIdentities = inactiveIdentities;
    }

    // Getters and setters
    public long getTotalIdentities() {
        return totalIdentities;
    }

    public void setTotalIdentities(long totalIdentities) {
        this.totalIdentities = totalIdentities;
    }

    public Map<IdentityType, Long> getIdentitiesByType() {
        return identitiesByType;
    }

    public void setIdentitiesByType(Map<IdentityType, Long> identitiesByType) {
        this.identitiesByType = identitiesByType;
    }

    public Map<IdentityType, IdentityTypeStatistics> getStatisticsByType() {
        return statisticsByType;
    }

    public void setStatisticsByType(Map<IdentityType, IdentityTypeStatistics> statisticsByType) {
        this.statisticsByType = statisticsByType;
    }

    public long getTotalExtendedAttributes() {
        return totalExtendedAttributes;
    }

    public void setTotalExtendedAttributes(long totalExtendedAttributes) {
        this.totalExtendedAttributes = totalExtendedAttributes;
    }

    public Map<String, Long> getExtendedAttributeUsage() {
        return extendedAttributeUsage;
    }

    public void setExtendedAttributeUsage(Map<String, Long> extendedAttributeUsage) {
        this.extendedAttributeUsage = extendedAttributeUsage;
    }

    public long getActiveIdentities() {
        return activeIdentities;
    }

    public void setActiveIdentities(long activeIdentities) {
        this.activeIdentities = activeIdentities;
    }

    public long getInactiveIdentities() {
        return inactiveIdentities;
    }

    public void setInactiveIdentities(long inactiveIdentities) {
        this.inactiveIdentities = inactiveIdentities;
    }

    /**
     * Inner class representing statistics specific to an identity type.
     */
    public static class IdentityTypeStatistics {
        private long totalCount;
        private long activeCount;
        private long inactiveCount;
        private long suspendedCount;
        private long terminatedCount;
        private long pendingCount;
        private long expiredCount;
        private long archivedCount;
        private long extendedAttributesCount;
        private Map<String, Long> extendedAttributeUsage;

        public IdentityTypeStatistics() {
        }

        public IdentityTypeStatistics(long totalCount, long activeCount, long inactiveCount,
                long suspendedCount, long terminatedCount, long pendingCount,
                long expiredCount, long archivedCount, long extendedAttributesCount,
                Map<String, Long> extendedAttributeUsage) {
            this.totalCount = totalCount;
            this.activeCount = activeCount;
            this.inactiveCount = inactiveCount;
            this.suspendedCount = suspendedCount;
            this.terminatedCount = terminatedCount;
            this.pendingCount = pendingCount;
            this.expiredCount = expiredCount;
            this.archivedCount = archivedCount;
            this.extendedAttributesCount = extendedAttributesCount;
            this.extendedAttributeUsage = extendedAttributeUsage;
        }

        // Getters and setters
        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getActiveCount() {
            return activeCount;
        }

        public void setActiveCount(long activeCount) {
            this.activeCount = activeCount;
        }

        public long getInactiveCount() {
            return inactiveCount;
        }

        public void setInactiveCount(long inactiveCount) {
            this.inactiveCount = inactiveCount;
        }

        public long getSuspendedCount() {
            return suspendedCount;
        }

        public void setSuspendedCount(long suspendedCount) {
            this.suspendedCount = suspendedCount;
        }

        public long getTerminatedCount() {
            return terminatedCount;
        }

        public void setTerminatedCount(long terminatedCount) {
            this.terminatedCount = terminatedCount;
        }

        public long getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(long pendingCount) {
            this.pendingCount = pendingCount;
        }

        public long getExpiredCount() {
            return expiredCount;
        }

        public void setExpiredCount(long expiredCount) {
            this.expiredCount = expiredCount;
        }

        public long getArchivedCount() {
            return archivedCount;
        }

        public void setArchivedCount(long archivedCount) {
            this.archivedCount = archivedCount;
        }

        public long getExtendedAttributesCount() {
            return extendedAttributesCount;
        }

        public void setExtendedAttributesCount(long extendedAttributesCount) {
            this.extendedAttributesCount = extendedAttributesCount;
        }

        public Map<String, Long> getExtendedAttributeUsage() {
            return extendedAttributeUsage;
        }

        public void setExtendedAttributeUsage(Map<String, Long> extendedAttributeUsage) {
            this.extendedAttributeUsage = extendedAttributeUsage;
        }
    }
}
