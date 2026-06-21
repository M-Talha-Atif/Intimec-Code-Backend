package com.aidentx.taskmanagement.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom Jackson deserializer for Instant that handles multiple input formats: - Epoch milliseconds
 * (numbers) - ISO strings with timezone (e.g., "2025-10-14T02:00:00Z") - ISO strings without
 * timezone (e.g., "2025-10-14T02:00:00") - assumes UTC
 */
public class InstantDeserializer extends JsonDeserializer<Instant> {

  @Override
  public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {

    // Check if the value is a numeric type (epoch milliseconds)
    if (jsonParser.isExpectedNumberIntToken()) {
      long epochMillis = jsonParser.getLongValue();
      return Instant.ofEpochMilli(epochMillis);
    }

    String text = jsonParser.getText();

    if (text == null || text.trim().isEmpty()) {
      return null;
    }

    // Try parsing as epoch milliseconds from string
    try {
      long epochMillis = Long.parseLong(text);
      return Instant.ofEpochMilli(epochMillis);
    } catch (NumberFormatException e) {
      // Not a number, continue to string parsing
    }

    // Try parsing as ISO instant with timezone
    try {
      return Instant.parse(text);
    } catch (DateTimeParseException e) {
      // Not a standard ISO instant, try local date-time assuming UTC
    }

    // Try parsing as local date-time and assume UTC
    try {
      LocalDateTime localDateTime =
          LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      return localDateTime.toInstant(ZoneOffset.UTC);
    } catch (DateTimeParseException e) {
      throw new IOException(
          "Unable to parse Instant from: "
              + text
              + ". Expected format: epoch milliseconds, ISO instant (with timezone), or ISO local date-time (assumed UTC).",
          e);
    }
  }
}
