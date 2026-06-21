package com.aidentx.iam.access_roles_management.service.export;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Streaming JSON writer for the access-role export.
 *
 * <p>Emits a top-level array of role objects. Values are written with their
 * native JSON type when possible (number, boolean) and as ISO-8601 strings for
 * temporals, with UUIDs and other complex types stringified. Collections become
 * JSON arrays.</p>
 */
public final class JsonAccessRoleExportWriter implements AccessRoleExportWriter {

    private final JsonGenerator generator;
    private List<AccessRoleExportColumn> columns;

    public JsonAccessRoleExportWriter(OutputStream outputStream, ObjectMapper objectMapper) throws IOException {
        Objects.requireNonNull(outputStream, "outputStream");
        Objects.requireNonNull(objectMapper, "objectMapper");
        this.generator = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8);
    }

    @Override
    public void begin(List<AccessRoleExportColumn> columns) throws IOException {
        this.columns = List.copyOf(columns);
        generator.writeStartArray();
    }

    @Override
    public void writeRow(AccessRoleResponse role) throws IOException {
        generator.writeStartObject();
        for (AccessRoleExportColumn column : columns) {
            writeField(column.key(), column.extract(role));
        }
        generator.writeEndObject();
    }

    @Override
    public void end() throws IOException {
        generator.writeEndArray();
        generator.flush();
    }

    @Override
    public AccessRoleExportFormat format() {
        return AccessRoleExportFormat.JSON;
    }

    @Override
    public void close() throws IOException {
        // Flush only; caller owns the underlying stream.
        generator.flush();
    }

    private void writeField(String key, Object value) throws IOException {
        generator.writeFieldName(key);
        writeValue(value);
    }

    private void writeValue(Object value) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        if (value instanceof String s) {
            generator.writeString(s);
        } else if (value instanceof Boolean b) {
            generator.writeBoolean(b);
        } else if (value instanceof Integer i) {
            generator.writeNumber(i);
        } else if (value instanceof Long l) {
            generator.writeNumber(l);
        } else if (value instanceof Double d) {
            generator.writeNumber(d);
        } else if (value instanceof Float f) {
            generator.writeNumber(f);
        } else if (value instanceof Number n) {
            generator.writeNumber(n.toString());
        } else if (value instanceof UUID u) {
            generator.writeString(u.toString());
        } else if (value instanceof TemporalAccessor t) {
            generator.writeString(t.toString());
        } else if (value instanceof Collection<?> collection) {
            generator.writeStartArray();
            for (Object element : collection) {
                writeValue(element);
            }
            generator.writeEndArray();
        } else {
            generator.writeString(value.toString());
        }
    }
}

