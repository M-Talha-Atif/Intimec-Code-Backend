package com.aidentx.iam.entitlement_management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JPA {@link AttributeConverter} that serializes {@code List<Map<String, Object>>}
 * to a PostgreSQL {@code jsonb} column and deserializes it back.
 *
 * <p>This converter handles the rule conditions column which stores either:
 * <ul>
 *   <li><b>Grouped format (canonical):</b> each element is a condition group with
 *       {@code groupOperator}, {@code negated}, and nested {@code conditions} list</li>
 *   <li><b>Legacy flat format:</b> each element is a single condition with
 *       {@code field}, {@code operator}, {@code value}, {@code logicalOperator}</li>
 * </ul>
 *
 * <p>Both formats are valid JSON arrays of objects, so the converter doesn't
 * need to distinguish between them — it simply serializes/deserializes
 * {@code List<Map<String, Object>>} generically.
 *
 * <p>Thread safety: this converter is stateless and uses a shared
 * {@link ObjectMapper} instance which is thread-safe for reading/writing.
 *
 * <p>Error handling:
 * <ul>
 *   <li>{@code null} database values → empty list</li>
 *   <li>{@code null} entity values → {@code null} stored in DB</li>
 *   <li>Malformed JSON → {@link IllegalStateException} (fail-fast, no silent corruption)</li>
 * </ul>
 */
@Converter
public class JsonbConditionsConverter implements AttributeConverter<List<Map<String, Object>>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> TYPE_REF =
            new TypeReference<>() {};

    /**
     * Converts the entity attribute value ({@code List<Map<String, Object>>})
     * to a JSON string for database storage.
     *
     * @param attribute the conditions list from the entity
     * @return JSON string, or {@code null} if the attribute is null
     * @throws IllegalStateException if serialization fails
     */
    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize conditions to JSON for database storage", e);
        }
    }

    /**
     * Converts the database JSON string back to {@code List<Map<String, Object>>}.
     *
     * @param dbData the JSON string from the database
     * @return the deserialized conditions list, or empty list if null/blank
     * @throws IllegalStateException if deserialization fails (malformed JSON)
     */
    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE_REF);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to deserialize conditions JSON from database: " + dbData, e);
        }
    }
}