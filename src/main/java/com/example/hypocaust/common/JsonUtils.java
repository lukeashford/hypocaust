package com.example.hypocaust.common;

import lombok.experimental.UtilityClass;

/**
 * Utility class for JSON operations, specifically for extracting JSON from LLM responses which
 * might be wrapped in markdown or contain preamble/postamble.
 */
@UtilityClass
public class JsonUtils {

  /**
   * Extracts a JSON string from a larger text response. Handles markdown code blocks (```json ...
   * ``` or ``` ... ```) and bare objects { ... }.
   *
   * @param response The raw string response from an LLM.
   * @return The extracted JSON string, or the original response if no JSON patterns are found.
   */
  public static String extractJson(String response) {
    if (response == null || response.isBlank()) {
      return "{}";
    }

    // 1. Try to find JSON in markdown code block with language hint
    var jsonBlockStart = response.indexOf("```json");
    if (jsonBlockStart >= 0) {
      var contentStart = response.indexOf('\n', jsonBlockStart) + 1;
      var contentEnd = response.indexOf("```", contentStart);
      if (contentEnd > contentStart) {
        return response.substring(contentStart, contentEnd).trim();
      }
    }

    // 2. Try to find JSON in generic markdown code block
    var codeBlockStart = response.indexOf("```");
    if (codeBlockStart >= 0) {
      var contentStart = response.indexOf('\n', codeBlockStart) + 1;
      var contentEnd = response.indexOf("```", contentStart);
      if (contentEnd > contentStart) {
        return response.substring(contentStart, contentEnd).trim();
      }
    }

    // 3. Try to find bare JSON — pick whichever structural character appears first
    var arrayStart = response.indexOf('[');
    var braceStart = response.indexOf('{');

    if (arrayStart >= 0 && (braceStart < 0 || arrayStart < braceStart)) {
      var arrayEnd = response.lastIndexOf(']');
      if (arrayEnd > arrayStart) {
        return response.substring(arrayStart, arrayEnd + 1);
      }
    }

    if (braceStart >= 0) {
      var braceEnd = response.lastIndexOf('}');
      if (braceEnd > braceStart) {
        return response.substring(braceStart, braceEnd + 1);
      }
    }

    return response.trim();
  }
}
