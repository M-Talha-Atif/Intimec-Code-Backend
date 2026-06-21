package com.aidentx.iam.access_roles_management.service.import_jobs;

import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleAssignmentRuleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.RoleImportRowDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parses CSV role import files with a required header row.
 *
 * <p>The CSV format is designed to mirror {@link RoleImportRowDto}. Simple scalar
 * fields are stored directly in cells. Multi-value fields use a pipe delimiter
 * ({@code |}) inside one cell, for example {@code SOX|PII} or
 * {@code uuid-1|uuid-2}. The {@code rules} cell contains a JSON array of
 * {@link CreateAccessRoleAssignmentRuleRequest} objects and must be CSV-escaped
 * when it contains commas or quotes.</p>
 *
 * <p>Supported headers are:</p>
 * <pre>
  * externalRef,name,displayName,description,type,status,businessUnit,organization,businessFunction,
 * classification,complianceTags,riskLevel,riskReason,ownerId,requestable,requireApproval,
 * certificationSchedule,entitlementIds,childRoleIds,childRoleRefs,identityIds,rules
 * </pre>
 *
 * <p>This parser performs only syntactic conversion. It does not validate whether
 * role names are unique, referenced entitlements exist, or child-role references
 * can be linked. Those checks happen later in the import service/worker.</p>
 */
@Component
public class CsvRoleImportParser implements RoleImportParser {

    private final ObjectMapper objectMapper;

    public CsvRoleImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reads a UTF-8 CSV file, maps each non-blank data line to a role import row,
     * and preserves file order for row-number based error reporting.
     *
     * @param inputStream uploaded CSV file content
     * @return parsed role rows
     * @throws IOException when the file or embedded {@code rules} JSON cannot be read
     */
    @Override
    public List<RoleImportRowDto> parse(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || header.isBlank()) {
                return List.of();
            }

            List<String> headers = parseCsvLine(header);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerIndex.put(headers.get(i).trim(), i);
            }

            List<RoleImportRowDto> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(toRow(headerIndex, parseCsvLine(line)));
            }
            return rows;
        }
    }

    /**
     * Converts one CSV record into the canonical JSON-equivalent import DTO.
     * Missing optional columns are treated as {@code null}; required field checks
     * are performed by {@code AccessRoleImportServiceImpl} after parsing.
     */
    private RoleImportRowDto toRow(Map<String, Integer> headerIndex, List<String> values) throws IOException {
        RoleImportRowDto row = new RoleImportRowDto();
        row.setExternalRef(value(headerIndex, values, "externalRef"));
        row.setName(value(headerIndex, values, "name"));
        row.setDisplayName(value(headerIndex, values, "displayName"));
        row.setDescription(value(headerIndex, values, "description"));
        row.setType(value(headerIndex, values, "type"));
        row.setStatus(value(headerIndex, values, "status"));
        row.setBusinessUnit(value(headerIndex, values, "businessUnit"));
        row.setOrganization(uuid(value(headerIndex, values, "organization")));
        row.setBusinessFunction(value(headerIndex, values, "businessFunction"));
        row.setClassification(split(value(headerIndex, values, "classification")));
        row.setComplianceTags(split(value(headerIndex, values, "complianceTags")));
        row.setRiskLevel(value(headerIndex, values, "riskLevel"));
        row.setRiskReason(value(headerIndex, values, "riskReason"));
        row.setOwnerId(uuid(value(headerIndex, values, "ownerId")));
        row.setRequestable(bool(value(headerIndex, values, "requestable")));
        row.setRequireApproval(bool(value(headerIndex, values, "requireApproval")));
        row.setCertificationSchedule(value(headerIndex, values, "certificationSchedule"));
        row.setEntitlementIds(uuidList(value(headerIndex, values, "entitlementIds")));
        row.setChildRoleIds(uuidList(value(headerIndex, values, "childRoleIds")));
        row.setChildRoleRefs(split(value(headerIndex, values, "childRoleRefs")));
        row.setIdentityIds(uuidList(value(headerIndex, values, "identityIds")));

        String rulesJson = value(headerIndex, values, "rules");
        if (rulesJson != null && !rulesJson.isBlank()) {
            row.setRules(objectMapper.readValue(
                    rulesJson,
                    new TypeReference<List<CreateAccessRoleAssignmentRuleRequest>>() {}));
        }
        return row;
    }

    /** Returns a trimmed cell value or {@code null} when the column/cell is absent or blank. */
    private static String value(Map<String, Integer> headerIndex, List<String> values, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx >= values.size()) {
            return null;
        }
        String raw = values.get(idx);
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    /** Converts optional boolean text into a Boolean object. */
    private static Boolean bool(String value) {
        return value == null ? null : Boolean.parseBoolean(value);
    }

    /** Converts optional UUID text into a UUID object. */
    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    /** Converts pipe-delimited UUID text into a UUID list. */
    private static List<UUID> uuidList(String value) {
        List<String> parts = split(value);
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        return parts.stream().map(UUID::fromString).toList();
    }

    /** Splits pipe-delimited list cells and drops blank elements. */
    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (String part : value.split("\\|")) {
            if (part != null && !part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * Parses one RFC-4180-style CSV line with comma separators and double-quote
     * escaping. The import format is line-oriented, so embedded newlines inside
     * quoted cells are intentionally not supported.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }
}
