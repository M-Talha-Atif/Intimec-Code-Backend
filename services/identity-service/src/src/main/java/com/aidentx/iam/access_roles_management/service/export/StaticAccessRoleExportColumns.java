package com.aidentx.iam.access_roles_management.service.export;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleExportConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.function.Function;

/**
 * Registers the built-in access-role export columns as Spring beans so they are
 * auto-discovered by {@link AccessRoleExportColumnRegistry}.
 *
 * <p>Columns are ordered using {@link Order} so the default CSV/JSON output is
 * stable. Future dynamic-attribute providers (e.g. one bean per tenant-defined
 * attribute) can be added without touching this file by simply registering more
 * {@link AccessRoleExportColumn} beans with higher {@code @Order} values.</p>
 */
@Configuration
public class StaticAccessRoleExportColumns {

    private static AccessRoleExportColumn column(String key, Function<AccessRoleResponse, Object> extractor) {
        return new SimpleColumn(key, extractor);
    }

    @Bean
    @Order(10)
    public AccessRoleExportColumn idColumn() {
        return column(AccessRoleExportConstants.COLUMN_ID, AccessRoleResponse::getId);
    }

    @Bean
    @Order(20)
    public AccessRoleExportColumn tenantIdColumn() {
        return column(AccessRoleExportConstants.COLUMN_TENANT_ID, AccessRoleResponse::getTenantId);
    }

    @Bean
    @Order(30)
    public AccessRoleExportColumn nameColumn() {
        return column(AccessRoleExportConstants.COLUMN_NAME, AccessRoleResponse::getName);
    }

    @Bean
    @Order(40)
    public AccessRoleExportColumn displayNameColumn() {
        return column(AccessRoleExportConstants.COLUMN_DISPLAY_NAME, AccessRoleResponse::getDisplayName);
    }

    @Bean
    @Order(50)
    public AccessRoleExportColumn descriptionColumn() {
        return column(AccessRoleExportConstants.COLUMN_DESCRIPTION, AccessRoleResponse::getDescription);
    }

    @Bean
    @Order(60)
    public AccessRoleExportColumn typeColumn() {
        return column(AccessRoleExportConstants.COLUMN_TYPE, AccessRoleResponse::getType);
    }

    @Bean
    @Order(70)
    public AccessRoleExportColumn statusColumn() {
        return column(AccessRoleExportConstants.COLUMN_STATUS, AccessRoleResponse::getStatus);
    }

    @Bean
    @Order(80)
    public AccessRoleExportColumn businessUnitColumn() {
        return column(AccessRoleExportConstants.COLUMN_BUSINESS_UNIT, AccessRoleResponse::getBusinessUnit);
    }

    @Bean
    @Order(90)
    public AccessRoleExportColumn organizationColumn() {
        return column(AccessRoleExportConstants.COLUMN_ORGANIZATION, AccessRoleResponse::getOrganization);
    }

    @Bean
    @Order(100)
    public AccessRoleExportColumn businessFunctionColumn() {
        return column(AccessRoleExportConstants.COLUMN_BUSINESS_FUNCTION, AccessRoleResponse::getBusinessFunction);
    }

    @Bean
    @Order(110)
    public AccessRoleExportColumn classificationColumn() {
        return column(AccessRoleExportConstants.COLUMN_CLASSIFICATION, AccessRoleResponse::getClassification);
    }

    @Bean
    @Order(120)
    public AccessRoleExportColumn complianceTagsColumn() {
        return column(AccessRoleExportConstants.COLUMN_COMPLIANCE_TAGS, AccessRoleResponse::getComplianceTags);
    }

    @Bean
    @Order(130)
    public AccessRoleExportColumn riskLevelColumn() {
        return column(AccessRoleExportConstants.COLUMN_RISK_LEVEL, AccessRoleResponse::getRiskLevel);
    }

    @Bean
    @Order(140)
    public AccessRoleExportColumn riskReasonColumn() {
        return column(AccessRoleExportConstants.COLUMN_RISK_REASON, AccessRoleResponse::getRiskReason);
    }

    @Bean
    @Order(150)
    public AccessRoleExportColumn ownerIdColumn() {
        return column(AccessRoleExportConstants.COLUMN_OWNER_ID, AccessRoleResponse::getOwnerId);
    }

    @Bean
    @Order(160)
    public AccessRoleExportColumn requestableColumn() {
        return column(AccessRoleExportConstants.COLUMN_REQUESTABLE, AccessRoleResponse::getRequestable);
    }

    @Bean
    @Order(170)
    public AccessRoleExportColumn requireApprovalColumn() {
        return column(AccessRoleExportConstants.COLUMN_REQUIRE_APPROVAL, AccessRoleResponse::getRequireApproval);
    }

    @Bean
    @Order(180)
    public AccessRoleExportColumn certificationScheduleColumn() {
        return column(AccessRoleExportConstants.COLUMN_CERTIFICATION_SCHEDULE, AccessRoleResponse::getCertificationSchedule);
    }

    @Bean
    @Order(190)
    public AccessRoleExportColumn entitlementCountColumn() {
        return column(AccessRoleExportConstants.COLUMN_ENTITLEMENT_COUNT, AccessRoleResponse::getEntitlementCount);
    }

    @Bean
    @Order(200)
    public AccessRoleExportColumn createdAtColumn() {
        return column(AccessRoleExportConstants.COLUMN_CREATED_AT, AccessRoleResponse::getCreatedAt);
    }

    @Bean
    @Order(210)
    public AccessRoleExportColumn updatedAtColumn() {
        return column(AccessRoleExportConstants.COLUMN_UPDATED_AT, AccessRoleResponse::getUpdatedAt);
    }

    /** Internal value-type backing the static column beans. */
    private record SimpleColumn(String key, Function<AccessRoleResponse, Object> extractor)
            implements AccessRoleExportColumn {
        @Override
        public Object extract(AccessRoleResponse role) {
            return role == null ? null : extractor.apply(role);
        }
    }
}

