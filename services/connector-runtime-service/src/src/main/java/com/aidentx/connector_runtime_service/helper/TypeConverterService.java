package com.aidentx.connector_runtime_service.helper;

import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A critical adapter service that converts generic, protobuf-derived Java
 * objects
 * into the specific, strongly-typed classes required by ConnId framework
 * configuration properties.
 *
 * <p>
 * This service is NOT a redundant mapper. It is essential because Protobuf's
 * Struct deserializes all numbers as {@code Double} and all secure strings as
 * plain {@code String}. This service correctly converts those generic types
 * into
 * the specific types the connector bundles expect (e.g., {@code Integer},
 * {@code Long}, {@code GuardedString}, {@code File}, {@code String[]}),
 * preventing runtime class cast exceptions.
 */
@Service
public class TypeConverterService {

    private final IUnifiedLogger logger;

    public TypeConverterService(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(TypeConverterService.class);
    }

    /**
     * Converts an input value into the expected Java type for a connector property.
     *
     * <p>This method provides robust type conversion with the following features:</p>
     * <ul>
     *   <li><strong>Numeric conversions:</strong> Handles Number-to-primitive conversions with range validation</li>
     *   <li><strong>String-to-numeric:</strong> Parses string representations of numbers with proper error handling</li>
     *   <li><strong>Boolean flexibility:</strong> Accepts various string representations (true/false, 1/0, yes/no, etc.)</li>
     *   <li><strong>Array handling:</strong> Converts Lists to String arrays and comma-separated strings to arrays</li>
     *   <li><strong>Security types:</strong> Converts plain strings to GuardedString/GuardedByteArray</li>
     *   <li><strong>Range validation:</strong> Prevents overflow when converting between numeric types</li>
     *   <li><strong>Graceful degradation:</strong> Returns original value with warning if conversion fails</li>
     * </ul>
     *
     * @param value        the raw value from the protobuf Struct
     * @param expectedType the specific Java {@link Class} required by the ConnId property
     * @return the correctly typed object, or the original value if conversion fails
     */
    public Object convert(Object value, Class<?> expectedType) {
        if (value == null || expectedType.isInstance(value)) {
            return value;
        }

        if (expectedType.equals(GuardedString.class) && value instanceof String) {
            return new GuardedString(((String) value).toCharArray());
        }

        if (expectedType.equals(GuardedByteArray.class) && value instanceof String) {
            return new GuardedByteArray(((String) value).getBytes(StandardCharsets.UTF_8));
        }

        if (value instanceof Number numberValue) {
            try {
                if (expectedType.equals(Integer.class) || expectedType.equals(int.class)) {
                    long longValue = numberValue.longValue();
                    if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Value " + longValue + " is out of range for int");
                    }
                    return numberValue.intValue();
                }
                if (expectedType.equals(Long.class) || expectedType.equals(long.class)) {
                    return numberValue.longValue();
                }
                if (expectedType.equals(Double.class) || expectedType.equals(double.class)) {
                    return numberValue.doubleValue();
                }
                if (expectedType.equals(Float.class) || expectedType.equals(float.class)) {
                    double doubleValue = numberValue.doubleValue();
                    if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                        throw new IllegalArgumentException("Invalid float value: " + doubleValue);
                    }
                    return numberValue.floatValue();
                }
                if (expectedType.equals(Short.class) || expectedType.equals(short.class)) {
                    long longValue = numberValue.longValue();
                    if (longValue < Short.MIN_VALUE || longValue > Short.MAX_VALUE) {
                        throw new IllegalArgumentException("Value " + longValue + " is out of range for short");
                    }
                    return numberValue.shortValue();
                }
                if (expectedType.equals(Byte.class) || expectedType.equals(byte.class)) {
                    long longValue = numberValue.longValue();
                    if (longValue < Byte.MIN_VALUE || longValue > Byte.MAX_VALUE) {
                        throw new IllegalArgumentException("Value " + longValue + " is out of range for byte");
                    }
                    return numberValue.byteValue();
                }
                if (expectedType.equals(Character.class) || expectedType.equals(char.class)) {
                    int intValue = numberValue.intValue();
                    if (intValue < Character.MIN_VALUE || intValue > Character.MAX_VALUE) {
                        throw new IllegalArgumentException("Value " + intValue + " is out of range for char");
                    }
                    return (char) intValue;
                }
            } catch (Exception e) {
                logger.warn().message(String.format("Failed to convert numeric value %s to %s: %s", numberValue, expectedType.getSimpleName(), e.getMessage())).log();
                return value; // Return original value if conversion fails
            }
        }

        // Handle string to numeric type conversions
        if (value instanceof String stringValue && !stringValue.trim().isEmpty()) {
            String trimmedValue = stringValue.trim();
            try {
                if (expectedType.equals(Integer.class) || expectedType.equals(int.class)) {
                    int parsedValue = Integer.parseInt(trimmedValue);
                    return parsedValue;
                }
                if (expectedType.equals(Long.class) || expectedType.equals(long.class)) {
                    long parsedValue = Long.parseLong(trimmedValue);
                    return parsedValue;
                }
                if (expectedType.equals(Double.class) || expectedType.equals(double.class)) {
                    double parsedValue = Double.parseDouble(trimmedValue);
                    if (Double.isNaN(parsedValue) || Double.isInfinite(parsedValue)) {
                        throw new IllegalArgumentException("Invalid double value: " + parsedValue);
                    }
                    return parsedValue;
                }
                if (expectedType.equals(Float.class) || expectedType.equals(float.class)) {
                    float parsedValue = Float.parseFloat(trimmedValue);
                    if (Float.isNaN(parsedValue) || Float.isInfinite(parsedValue)) {
                        throw new IllegalArgumentException("Invalid float value: " + parsedValue);
                    }
                    return parsedValue;
                }
                if (expectedType.equals(Short.class) || expectedType.equals(short.class)) {
                    short parsedValue = Short.parseShort(trimmedValue);
                    return parsedValue;
                }
                if (expectedType.equals(Byte.class) || expectedType.equals(byte.class)) {
                    byte parsedValue = Byte.parseByte(trimmedValue);
                    return parsedValue;
                }
                if (expectedType.equals(Character.class) || expectedType.equals(char.class)) {
                    if (trimmedValue.length() != 1) {
                        throw new IllegalArgumentException("String '" + trimmedValue + "' cannot be converted to char");
                    }
                    return trimmedValue.charAt(0);
                }
            } catch (NumberFormatException e) {
                logger.warn().message(String.format("Failed to parse string '%s' as %s: %s", trimmedValue, expectedType.getSimpleName(), e.getMessage())).log();
                return value; // Return original value if parsing fails
            } catch (IllegalArgumentException e) {
                logger.warn().message("Invalid value for type conversion: " + e.getMessage()).log();
                return value; // Return original value if validation fails
            }
        }

        if ((expectedType.equals(Boolean.class) || expectedType.equals(boolean.class))) {
            if (value instanceof String stringValue) {
                String trimmed = stringValue.trim().toLowerCase();
                // Handle common boolean string representations
                return switch (trimmed) {
                    case "true", "1", "yes", "on", "enabled" -> true;
                    case "false", "0", "no", "off", "disabled" -> false;
                    default -> Boolean.parseBoolean(trimmed); // fallback to standard parsing
                };
            }
            if (value instanceof Number numberValue) {
                // Convert numbers: 0 = false, non-zero = true
                return numberValue.doubleValue() != 0.0;
            }
        }

        if (expectedType.equals(String[].class)) {
            if (value instanceof List<?> list) {
                if (list.isEmpty()) {
                    return new String[0];
                }
                return list.stream()
                          .filter(item -> item != null)
                          .map(Object::toString)
                          .toArray(String[]::new);
            }
            if (value instanceof String stringValue) {
                // Handle comma-separated string as array
                if (stringValue.trim().isEmpty()) {
                    return new String[0];
                }
                return stringValue.split("\\s*,\\s*");
            }
        }

        if (expectedType.equals(File.class) && value instanceof String stringValue) {
            if (stringValue.trim().isEmpty()) {
                logger.warn().message("Empty string provided for File type, returning null").log();
                return null;
            }
            try {
                return new File(stringValue);
            } catch (Exception e) {
                logger.warn().message(String.format("Failed to create File from string '%s': %s", stringValue, e.getMessage())).log();
                return null;
            }
        }

        if (expectedType.equals(String.class)) {
            return value.toString();
        }

        logger.warn().message(String.format("No specific converter found for expected type '%s' from actual type '%s'. Returning original value.", expectedType.getName(), value.getClass().getName())).log();
        return value;
    }
}