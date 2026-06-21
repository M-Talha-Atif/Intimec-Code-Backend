package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Constants for the access-role export feature.
 *
 * <p>Centralizes paths, MIME types, header names, format values, column keys,
 * filename patterns, page/cap sizing, and structured log messages used by the
 * export controller, service and writers.</p>
 */
public final class AccessRoleExportConstants {

    private AccessRoleExportConstants() {
    }

    // ===================================================================
    //  HTTP
    // ===================================================================

    /** Export endpoint suffix appended to {@link AccessRoleConstants#BASE_PATH}. */
    public static final String PATH_EXPORT = "/export";

    /** Query-param name carrying desired export format. */
    public static final String PARAM_FORMAT = "format";

    /** Query-param name carrying optional comma-separated column key subset. */
    public static final String PARAM_COLUMNS = "columns";

    /** Response header indicating result was truncated due to {@link #EXPORT_MAX_ROWS}. */
    public static final String HEADER_EXPORT_TRUNCATED = "X-Export-Truncated";

    /** Standard HTTP Content-Disposition header name. */
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

    /** Content-Disposition template for downloads: {@code attachment; filename="..."}. */
    public static final String CONTENT_DISPOSITION_TEMPLATE = "attachment; filename=\"%s\"";

    /** MIME type for CSV downloads (UTF-8). */
    public static final String MIME_CSV = "text/csv; charset=UTF-8";

    /** MIME type for JSON downloads. */
    public static final String MIME_JSON = "application/json";

    /** Filename template: {@code access-roles-<tenant>-<UTC-yyyyMMdd-HHmmss>.<ext>}. */
    public static final String FILENAME_TEMPLATE = "access-roles-%s-%s.%s";

    /** File extension for CSV exports. */
    public static final String EXTENSION_CSV = "csv";

    /** File extension for JSON exports. */
    public static final String EXTENSION_JSON = "json";

    /** UTC timestamp pattern used in export filenames. */
    public static final String FILENAME_TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss";

    // ===================================================================
    //  Format values (query param)
    // ===================================================================

    public static final String FORMAT_CSV = "csv";
    public static final String FORMAT_JSON = "json";

    // ===================================================================
    //  Sizing
    // ===================================================================

    /** Page size used when iterating the role list during export streaming. */
    public static final int EXPORT_PAGE_SIZE = 500;

    /** Soft cap on total exported rows; excess rows are dropped and a header is added. */
    public static final int EXPORT_MAX_ROWS = 100_000;

    // ===================================================================
    //  CSV formatting
    // ===================================================================

    /** Field separator used for list-typed values inside a single CSV cell. */
    public static final String CSV_LIST_DELIMITER = "|";

    /** Field separator used between CSV columns. */
    public static final String CSV_FIELD_DELIMITER = ",";

    /** Line terminator used between CSV rows (RFC-4180). */
    public static final String CSV_LINE_TERMINATOR = "\r\n";

    /** Quote character used by RFC-4180 CSV escaping. */
    public static final char CSV_QUOTE = '"';

    // ===================================================================
    //  Column keys (also CSV header names / JSON field names)
    // ===================================================================

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TENANT_ID = "tenantId";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DISPLAY_NAME = "displayName";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_BUSINESS_UNIT = "businessUnit";
    public static final String COLUMN_ORGANIZATION = "organization";
    public static final String COLUMN_BUSINESS_FUNCTION = "businessFunction";
    public static final String COLUMN_CLASSIFICATION = "classification";
    public static final String COLUMN_COMPLIANCE_TAGS = "complianceTags";
    public static final String COLUMN_RISK_LEVEL = "riskLevel";
    public static final String COLUMN_RISK_REASON = "riskReason";
    public static final String COLUMN_OWNER_ID = "ownerId";
    public static final String COLUMN_REQUESTABLE = "requestable";
    public static final String COLUMN_REQUIRE_APPROVAL = "requireApproval";
    public static final String COLUMN_CERTIFICATION_SCHEDULE = "certificationSchedule";
    public static final String COLUMN_ENTITLEMENT_COUNT = "entitlementCount";
    public static final String COLUMN_CREATED_AT = "createdAt";
    public static final String COLUMN_UPDATED_AT = "updatedAt";

    // ===================================================================
    //  Logging
    // ===================================================================

    public static final String LOG_CATEGORY = "access-role-export";

    public static final String LOG_EXPORT_START = "Access role export started";
    public static final String LOG_EXPORT_PAGE = "Access role export page processed";
    public static final String LOG_EXPORT_COMPLETE = "Access role export completed";
    public static final String LOG_EXPORT_TRUNCATED = "Access role export truncated at row cap";
    public static final String LOG_EXPORT_FAILED = "Access role export failed";

    public static final String LOG_CONTROLLER_EXPORT = "AccessRoleController.exportRoles invoked";

    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_FORMAT = "format";
    public static final String DETAIL_COLUMNS = "columns";
    public static final String DETAIL_ROW_COUNT = "rowCount";
    public static final String DETAIL_PAGE_NUMBER = "page";
    public static final String DETAIL_PAGE_SIZE = "pageSize";
    public static final String DETAIL_ERROR = "error";

    // ===================================================================
    //  Errors
    // ===================================================================

    public static final String ERROR_UNSUPPORTED_FORMAT = "Unsupported export format: %s";
    public static final String ERROR_NO_COLUMNS_RESOLVED =
            "No export columns resolved for the requested column selection";
}

