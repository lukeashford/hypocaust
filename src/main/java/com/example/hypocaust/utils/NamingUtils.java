package com.example.hypocaust.utils;

import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Utility methods for naming and sanitizing strings.
 */
@UtilityClass
public class NamingUtils {

  /**
   * Sanitizes a string by converting it to lower case, replacing non-alphanumeric characters with
   * underscores, removing duplicate underscores, and trimming underscores from the beginning and
   * end. It also truncates the result to a maximum length.
   */
  public static String sanitize(String input, int maxLen) {
    String sanitized = input.toLowerCase()
        .replaceAll("[^a-z0-9_]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");

    if (sanitized.length() > maxLen) {
      sanitized = sanitized.substring(0, maxLen);
      sanitized = sanitized.replaceAll("_+$", "");
    }
    return sanitized;
  }

  /**
   * Appends a counter to a name if it already exists in the provided set.
   */
  public static String appendCounterIfExists(String name, Set<String> taken) {
    String result = name;
    int counter = 2;
    while (taken.contains(result)) {
      result = name + "_" + counter;
      counter++;
    }
    return result;
  }
}
