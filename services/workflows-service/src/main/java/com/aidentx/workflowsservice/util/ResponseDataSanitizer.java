package com.aidentx.workflowsservice.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sanitizes workflow execution data for API responses.
 *
 * This is intentionally "response-only": persisted execution context must remain unchanged
 * so workflow execution behavior is not impacted.
 */
public final class ResponseDataSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private ResponseDataSanitizer() {
    }

    public static Map<String, Object> sanitizeMap(Map<String, Object> input) {
        if (input == null) {
            return null;
        }
        Object sanitized = sanitizeObject(input);
        @SuppressWarnings("unchecked")
        Map<String, Object> sanitizedMap = (Map<String, Object>) sanitized;
        return sanitizedMap;
    }

    @SuppressWarnings("unchecked")
    public static Object sanitizeObject(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
                Object entryValue = entry.getValue();

                if (key != null && isSensitiveKey(key)) {
                    result.put(key, REDACTED);
                    continue;
                }

                // Also redact common bearer header values if present under any key
                if (entryValue instanceof String s && looksLikeBearerToken(s)) {
                    result.put(key, REDACTED);
                    continue;
                }

                result.put(key, sanitizeObject(entryValue));
            }
            return result;
        }

        if (value instanceof List<?> rawList) {
            List<Object> result = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                result.add(sanitizeObject(item));
            }
            return result;
        }

        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String k = key.toLowerCase(Locale.ROOT);

        // Exact / canonical keys used in our context
        if (k.equals("jwttoken")) return true;
        if (k.equals("authorization")) return true;

        // Common secret-bearing keys
        if (k.contains("token")) return true;
        if (k.contains("secret")) return true;
        if (k.contains("password")) return true;
        if (k.contains("apikey")) return true;
        if (k.contains("api_key")) return true;
        if (k.contains("clientsecret")) return true;
        if (k.contains("privatekey")) return true;

        return false;
    }

    private static boolean looksLikeBearerToken(String value) {
        String v = value.trim();
        return v.regionMatches(true, 0, "bearer ", 0, "bearer ".length());
    }
}
