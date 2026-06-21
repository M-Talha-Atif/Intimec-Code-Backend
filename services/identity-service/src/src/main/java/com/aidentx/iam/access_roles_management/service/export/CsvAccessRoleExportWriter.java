package com.aidentx.iam.access_roles_management.service.export;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleExportConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RFC-4180 compliant CSV writer for the access-role export.
 *
 * <p>Lists are joined with {@link AccessRoleExportConstants#CSV_LIST_DELIMITER}.
 * Fields containing the delimiter, the quote character, or a line break are
 * wrapped in quotes with embedded quotes doubled.</p>
 */
public final class CsvAccessRoleExportWriter implements AccessRoleExportWriter {

    private final BufferedWriter out;
    private List<AccessRoleExportColumn> columns;

    public CsvAccessRoleExportWriter(OutputStream outputStream) {
        Objects.requireNonNull(outputStream, "outputStream");
        this.out = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    @Override
    public void begin(List<AccessRoleExportColumn> columns) throws IOException {
        this.columns = List.copyOf(columns);
        writeLine(this.columns.stream().map(AccessRoleExportColumn::header).toList());
    }

    @Override
    public void writeRow(AccessRoleResponse role) throws IOException {
        List<String> cells = columns.stream()
                .map(c -> stringify(c.extract(role)))
                .toList();
        writeLine(cells);
    }

    @Override
    public void end() throws IOException {
        out.flush();
    }

    @Override
    public AccessRoleExportFormat format() {
        return AccessRoleExportFormat.CSV;
    }

    @Override
    public void close() throws IOException {
        out.flush();
        // Caller owns the underlying stream; do not close it here.
    }

    private void writeLine(List<String> cells) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(AccessRoleExportConstants.CSV_FIELD_DELIMITER);
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append(AccessRoleExportConstants.CSV_LINE_TERMINATOR);
        out.write(sb.toString());
    }

    private static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(CsvAccessRoleExportWriter::stringify)
                    .collect(Collectors.joining(AccessRoleExportConstants.CSV_LIST_DELIMITER));
        }
        return value.toString();
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuote = value.indexOf(AccessRoleExportConstants.CSV_QUOTE) >= 0
                || value.indexOf(',') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuote) {
            return value;
        }
        String escaped = value.replace(
                String.valueOf(AccessRoleExportConstants.CSV_QUOTE),
                "" + AccessRoleExportConstants.CSV_QUOTE + AccessRoleExportConstants.CSV_QUOTE);
        return AccessRoleExportConstants.CSV_QUOTE + escaped + AccessRoleExportConstants.CSV_QUOTE;
    }
}

