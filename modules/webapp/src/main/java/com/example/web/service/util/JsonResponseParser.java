package com.example.web.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * Utility service for parsing JSON responses from AI models with common cleanup logic
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JsonResponseParser {

  private final ObjectMapper objectMapper;

  /**
   * Parses JSON response with automatic cleanup and fallback handling
   *
   * @param jsonResponse The raw JSON response from AI model
   * @param targetClass The target class to parse into
   * @param fallbackSupplier Supplier for fallback object if parsing fails
   * @param <T> The target type
   * @return Parsed object or fallback
   */
  public <T> T parseWithFallback(String jsonResponse, Class<T> targetClass,
      Supplier<T> fallbackSupplier) {
    try {
      return objectMapper.readValue(jsonResponse, targetClass);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse JSON response for {}, attempting to clean: {}",
          targetClass.getSimpleName(), e.getMessage());

      val cleanedJson = cleanJsonResponse(jsonResponse);

      try {
        return objectMapper.readValue(cleanedJson.trim(), targetClass);
      } catch (JsonProcessingException ex) {
        log.error("Failed to parse cleaned JSON response for {}", targetClass.getSimpleName(), ex);
        return fallbackSupplier.get();
      }
    }
  }

  /**
   * Cleans JSON response by extracting from markdown code blocks
   *
   * @param jsonResponse The raw JSON response
   * @return Cleaned JSON string
   */
  private String cleanJsonResponse(String jsonResponse) {
    var cleanedJson = jsonResponse;

    if (jsonResponse.contains("```json")) {
      int startIndex = jsonResponse.indexOf("```json") + 7;
      // Skip any whitespace/newlines after ```json
      while (startIndex < jsonResponse.length() && Character.isWhitespace(
          jsonResponse.charAt(startIndex))) {
        startIndex++;
      }
      cleanedJson = jsonResponse.substring(startIndex);
      if (cleanedJson.contains("```")) {
        cleanedJson = cleanedJson.substring(0, cleanedJson.lastIndexOf("```"));
      }
    } else if (jsonResponse.contains("```")) {
      int startIndex = jsonResponse.indexOf("```") + 3;
      // Skip any whitespace/newlines after ```
      while (startIndex < jsonResponse.length() && Character.isWhitespace(
          jsonResponse.charAt(startIndex))) {
        startIndex++;
      }
      cleanedJson = jsonResponse.substring(startIndex);
      if (cleanedJson.contains("```")) {
        cleanedJson = cleanedJson.substring(0, cleanedJson.lastIndexOf("```"));
      }
    }

    return cleanedJson;
  }
}