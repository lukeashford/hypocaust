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
    return appendCounterIfExists(name, taken, Integer.MAX_VALUE);
  }

  /**
   * Appends a counter to a name if it already exists in the provided set, ensuring the final result
   * does not exceed maxLen.
   */
  public static String appendCounterIfExists(String name, Set<String> taken, int maxLen) {
    String result = name;
    int counter = 2;
    while (taken.contains(result)) {
      String suffix = "_" + counter;
      int baseLen = Math.min(name.length(), maxLen - suffix.length());
      result = name.substring(0, baseLen) + suffix;
      counter++;
    }
    return result;
  }
}
