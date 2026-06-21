package com.aidentx.taskmanagement.config;

import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Configuration class for customizing the Jackson ObjectMapper used by Spring Boot. */
@Configuration
public class JacksonConfig {

  /**
   * Defines the primary ObjectMapper bean for the entire application.
   *
   * <p>By marking this bean as @Primary, we ensure it overrides any default ObjectMapper that
   * Spring Boot might auto-configure. This gives us full control over JSON serialization.
   *
   * <p>We configure it to: 1. Support standard Java 8+ time types (but we will override Instant).
   * 2. Register our custom serializer for the Instant class to format it as epoch milliseconds.
   *
   * @return A fully configured ObjectMapper instance.
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    // Register the standard JavaTimeModule to handle other time types like
    // LocalDateTime, LocalDate, etc., correctly.
    objectMapper.registerModule(new JavaTimeModule());

    // Create a simple module to house our custom serializer and deserializer
    SimpleModule instantModule = new SimpleModule("InstantEpochMillisModule");
    instantModule.addSerializer(Instant.class, new InstantToEpochMillisSerializer());
    instantModule.addDeserializer(Instant.class, new InstantDeserializer());

    // Register our custom module. Because we register it after the JavaTimeModule,
    // our custom serializer for Instant will take precedence.
    objectMapper.registerModule(instantModule);

    return objectMapper;
  }
}
