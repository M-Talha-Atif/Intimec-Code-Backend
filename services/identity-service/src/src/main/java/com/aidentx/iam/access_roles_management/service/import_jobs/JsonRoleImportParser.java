package com.aidentx.iam.access_roles_management.service.import_jobs;

import com.aidentx.iam.access_roles_management.model.dto.request.RoleImportRowDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Parses JSON role import files. Accepts either {"roles": [...]} or a raw array. */
@Component
public class JsonRoleImportParser implements RoleImportParser {

    private final ObjectMapper objectMapper;

    public JsonRoleImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses the JSON document and converts its row array into canonical role
     * import DTOs. An invalid top-level shape fails fast with an
     * {@link IllegalArgumentException} so the upload request can return 400.
     */
    @Override
    public List<RoleImportRowDto> parse(InputStream inputStream) throws IOException {
        JsonNode root = objectMapper.readTree(inputStream);
        JsonNode rows = root != null && root.has("roles") ? root.get("roles") : root;
        if (rows == null || !rows.isArray()) {
            throw new IllegalArgumentException("JSON import must be an array or an object with a roles array");
        }
        return objectMapper.convertValue(rows, new TypeReference<List<RoleImportRowDto>>() {});
    }
}

