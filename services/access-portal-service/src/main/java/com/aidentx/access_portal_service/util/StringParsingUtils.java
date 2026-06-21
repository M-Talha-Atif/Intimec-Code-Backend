package com.aidentx.access_portal_service.util;
import java.util.*;
import java.util.stream.Collectors;

public class StringParsingUtils {

    private StringParsingUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Parses comma-separated string into a list of UUIDs.
     * @param commaSeparatedString The comma-separated string to parse
     * @return List of UUIDs, empty list if input is null or empty
     * @throws IllegalArgumentException if any substring is not a valid UUID
     */
    public static List<UUID> parseCommaSeparatedUUIDs(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(commaSeparatedString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    /**
     * Parses comma-separated string into a list of strings.
     * @param commaSeparatedString The comma-separated string to parse
     * @return List of trimmed strings, empty list if input is null or empty
     */
    public static List<String> parseCommaSeparatedStrings(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(commaSeparatedString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Alternative method with specific name for backward compatibility
     */
    public static List<UUID> parseBeneficiaryUserIds(String beneficiaryUserIds) {
        return parseCommaSeparatedUUIDs(beneficiaryUserIds);
    }

    /**
     * Alternative method with specific name for backward compatibility
     */
    public static List<String> parseBeneficiaryNames(String beneficiaryNames) {
        return parseCommaSeparatedStrings(beneficiaryNames);
    }

    /**
     * More generic version that can handle any type with a converter function
     */
    public static <T> List<T> parseCommaSeparatedValues(
            String commaSeparatedString,
            java.util.function.Function<String, T> converter) {
        if (commaSeparatedString == null || commaSeparatedString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(commaSeparatedString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(converter)
                .collect(Collectors.toList());
    }
}