package com.example.the_machine.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;

/**
 * ObjectMapper factory with configured settings for JSON serialization/deserialization.
 */
public final class Json {

  private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  private Json() {
    // Utility class - prevent instantiation
  }

  /**
   * Returns the singleton ObjectMapper instance configured with: - Snake case property naming -
   * JavaTimeModule for proper date/time handling - WRITE_DATES_AS_TIMESTAMPS=false -
   * FAIL_ON_UNKNOWN_PROPERTIES=false - JsonTypeInfo support
   *
   * @return configured ObjectMapper instance
   */
  public static ObjectMapper getObjectMapper() {
    return OBJECT_MAPPER;
  }

  private static ObjectMapper createObjectMapper() {
    val mapper = new ObjectMapper();

    // Configure property naming strategy to snake_case
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    // Register JavaTimeModule for proper date/time handling
    mapper.registerModule(new JavaTimeModule());

    // Disable writing dates as timestamps
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Don't fail on unknown properties during deserialization
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    return mapper;
  }
}