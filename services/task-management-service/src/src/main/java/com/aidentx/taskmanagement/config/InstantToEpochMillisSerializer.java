package com.aidentx.taskmanagement.config;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom Jackson serializer to convert Java's Instant objects into a long representing epoch
 * milliseconds.
 */
public class InstantToEpochMillisSerializer extends JsonSerializer<Instant> {

  @Override
  public void serialize(
      Instant instant, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    if (instant == null) {
      jsonGenerator.writeNull();
    } else {
      jsonGenerator.writeNumber(instant.toEpochMilli());
    }
  }
}
