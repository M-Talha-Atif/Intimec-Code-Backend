package com.aidentx.configuration.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Static utility class for handling the merging of JSON objects.
 */
public final class ConfigurationMergeUtil {

    private ConfigurationMergeUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Merges two JSON nodes, with the patch data taking precedence.
     * If both nodes are objects, it merges them recursively. Otherwise, the patch node replaces the existing node.
     *
     * @param existing The existing JsonNode.
     * @param patch    The patch JsonNode.
     * @return The merged JsonNode.
     */
    public static JsonNode merge(JsonNode existing, JsonNode patch) {
        if (patch.isObject() && existing.isObject()) {
            ObjectNode result = existing.deepCopy();
            var patchFields = patch.fieldNames();
            while (patchFields.hasNext()) {
                String fieldName = patchFields.next();
                JsonNode patchValue = patch.get(fieldName);
                JsonNode existingValue = result.get(fieldName);

                if (existingValue != null && existingValue.isObject() && patchValue.isObject()) {
                    result.set(fieldName, merge(existingValue, patchValue));
                } else {
                    result.set(fieldName, patchValue);
                }
            }
            return result;
        }
        // If not merging objects, the patch replaces the existing value entirely
        return patch;
    }
}