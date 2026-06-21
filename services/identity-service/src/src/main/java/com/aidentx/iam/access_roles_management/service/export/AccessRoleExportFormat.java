package com.aidentx.iam.access_roles_management.service.export;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleExportConstants;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Supported access-role export formats.
 *
 * <p>Each value carries its query-param token, MIME type, and download filename
 * extension so the controller and writers stay free of hardcoded literals.</p>
 */
public enum AccessRoleExportFormat {

    CSV(AccessRoleExportConstants.FORMAT_CSV,
            AccessRoleExportConstants.MIME_CSV,
            AccessRoleExportConstants.EXTENSION_CSV),

    JSON(AccessRoleExportConstants.FORMAT_JSON,
            AccessRoleExportConstants.MIME_JSON,
            AccessRoleExportConstants.EXTENSION_JSON);

    private final String token;
    private final String mimeType;
    private final String extension;

    AccessRoleExportFormat(String token, String mimeType, String extension) {
        this.token = token;
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String token() {
        return token;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    /**
     * Parses the query-param token (case-insensitive) into a format value.
     *
     * @param token the requested format token (may be {@code null} or blank)
     * @return matching format, or empty if the token is null/blank/unknown
     */
    public static Optional<AccessRoleExportFormat> fromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(f -> f.token.equals(normalized))
                .findFirst();
    }
}

