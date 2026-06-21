package com.aidentx.applicationpermissions.roles_management.model.dto;

/**
 * Data Transfer Object for role statistics.
 * Contains aggregated statistics about roles within a tenant.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class RoleStatsDTO {

    /**
     * Count of total roles in the tenant.
     */
    private Long totalRoles;

    /**
     * Count of inactive roles in the tenant (where is_active = false).
     */
    private Long inactiveRoles;

    /**
     * Count of roles with ADMINISTRATOR type.
     */
    private Long activeAdmins;

    /**
     * The tenant ID for these statistics.
     */
    private String tenantId;

    /**
     * Default constructor.
     */
    public RoleStatsDTO() {
    }

    /**
     * Constructor with all fields.
     * 
     * @param totalRoles count of total roles
     * @param inactiveRoles count of inactive roles
     * @param activeAdmins count of administrator roles
     * @param tenantId the tenant ID
     */
    public RoleStatsDTO(Long totalRoles, Long inactiveRoles, Long activeAdmins, String tenantId) {
        this.totalRoles = totalRoles;
        this.inactiveRoles = inactiveRoles;
        this.activeAdmins = activeAdmins;
        this.tenantId = tenantId;
    }

    /**
     * Gets the count of total roles.
     * 
     * @return total roles count
     */
    public Long getTotalRoles() {
        return totalRoles;
    }

    /**
     * Sets the count of total roles.
     * 
     * @param totalRoles total roles count
     */
    public void setTotalRoles(Long totalRoles) {
        this.totalRoles = totalRoles;
    }

    /**
     * Gets the count of inactive roles.
     * 
     * @return inactive roles count
     */
    public Long getInactiveRoles() {
        return inactiveRoles;
    }

    /**
     * Sets the count of inactive roles.
     * 
     * @param inactiveRoles inactive roles count
     */
    public void setInactiveRoles(Long inactiveRoles) {
        this.inactiveRoles = inactiveRoles;
    }

    /**
     * Gets the count of administrator roles.
     * 
     * @return active admins count
     */
    public Long getActiveAdmins() {
        return activeAdmins;
    }

    /**
     * Sets the count of administrator roles.
     * 
     * @param activeAdmins active admins count
     */
    public void setActiveAdmins(Long activeAdmins) {
        this.activeAdmins = activeAdmins;
    }

    /**
     * Gets the tenant ID.
     * 
     * @return tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant ID.
     * 
     * @param tenantId tenant ID
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Builder for RoleStatsDTO.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for RoleStatsDTO.
     */
    public static class Builder {
        private Long totalRoles;
        private Long inactiveRoles;
        private Long activeAdmins;
        private String tenantId;

        /**
         * Sets total roles.
         * 
         * @param totalRoles total roles count
         * @return builder instance
         */
        public Builder totalRoles(Long totalRoles) {
            this.totalRoles = totalRoles;
            return this;
        }

        /**
         * Sets inactive roles.
         * 
         * @param inactiveRoles inactive roles count
         * @return builder instance
         */
        public Builder inactiveRoles(Long inactiveRoles) {
            this.inactiveRoles = inactiveRoles;
            return this;
        }

        /**
         * Sets active admins.
         * 
         * @param activeAdmins active admins count
         * @return builder instance
         */
        public Builder activeAdmins(Long activeAdmins) {
            this.activeAdmins = activeAdmins;
            return this;
        }

        /**
         * Sets tenant ID.
         * 
         * @param tenantId tenant ID
         * @return builder instance
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Builds the RoleStatsDTO instance.
         * 
         * @return new RoleStatsDTO instance
         */
        public RoleStatsDTO build() {
            return new RoleStatsDTO(totalRoles, inactiveRoles, activeAdmins, tenantId);
        }
    }
}
