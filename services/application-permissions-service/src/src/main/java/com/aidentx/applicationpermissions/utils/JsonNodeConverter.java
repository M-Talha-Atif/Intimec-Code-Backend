package com.aidentx.applicationpermissions.utils;

import com.aidentx.applicationpermissions.shared.SharedAppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter to handle the conversion between {@link JsonNode}
 * and its JSON representation for persistence in PostgreSQL JSONB columns.
 * <p>
 * This converter handles PostgreSQL JSONB columns by returning String values
 * that Hibernate can properly convert to JSONB using @JdbcTypeCode annotation.
 */
@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a {@link JsonNode} object to its JSON String representation for storage in the database.
     * Returns a String that will be automatically handled by PostgreSQL for JSONB columns.
     *
     * @param jsonNode the {@link JsonNode} to convert
     * @return the JSON string representation, or {@code null} if the input is {@code null}
     * @throws IllegalArgumentException if conversion fails
     */
    @Override
    public String convertToDatabaseColumn(JsonNode jsonNode) {
        try {
            return jsonNode == null ? null : objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new IllegalArgumentException(SharedAppConstants.JSONNODE_TO_STRING_ERROR, e);
        }
    }

    /**
     * Converts JSON String from the database to a {@link JsonNode} object.
     * Handles both regular String and complex object extraction.
     *
     * @param jsonString the JSON string stored in the database
     * @return the parsed {@link JsonNode}, or {@code null} if the input is {@code null}
     * @throws IllegalArgumentException if conversion fails
     */
    @Override
    public JsonNode convertToEntityAttribute(String jsonString) {
        if (jsonString == null) {
            return null;
        }

        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new IllegalArgumentException(SharedAppConstants.STRING_TO_JSONNODE_ERROR, e);
        }
    }
}