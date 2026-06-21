package com.aidentx.iam.access_roles_management.service.analytics;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleBusinessUnitAnalyticsItemDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleIntelligenceKpisDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleTypeDistributionDto;

import java.util.List;

/**
 * Service contract for Access Role Intelligence analytics.
 *
 * <p>Provides analytics data used by the frontend Intelligence tab,
 * including KPI cards and chart datasets.</p>
 */
public interface AccessRoleIntelligenceAnalyticsService {

    /**
     * Returns intelligence KPI values for access roles.
     *
     * @param tenantId the tenant identifier
     * @return KPI dataset for the Intelligence tab
     */
    AccessRoleIntelligenceKpisDto getIntelligenceKpis(String tenantId);

    /**
     * Returns role counts grouped by role type.
     *
     * @param tenantId the tenant identifier
     * @return chart data for role distribution by type
     */
    List<AccessRoleTypeDistributionDto> getRoleDistributionByType(String tenantId);

    /**
     * Returns role and user counts grouped by business unit.
     *
     * <p>User counts are currently mocked as zero until role-user assignment
     * analytics are implemented.</p>
     *
     * @param tenantId the tenant identifier
     * @return chart data for roles and users by business unit
     */
    List<AccessRoleBusinessUnitAnalyticsItemDto> getRolesUsersByBusinessUnit(String tenantId);
}