package com.example.hypocaust.tool.creative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured report of what happened during tool execution. Designed to bridge context between
 * execution layers without requiring LLM summarization — the decomposer receives concrete,
 * machine-readable metadata about each attempt, enabling informed self-healing decisions.
 *
 * <p>The {@code attempts} list records each discrete step or retry in order, with flexible
 * key-value metadata per attempt (e.g., which model was used, what error occurred). Values are
 * truncated to avoid bloating the context window.
 *
 * @param success whether the overall operation succeeded
 * @param summary human-readable one-liner (not LLM-generated — constructed by the tool)
 * @param attempts ordered list of attempt metadata; each map contains keys like "model", "platform",
 *     "status", "error" — flexible per tool type
 * @param artifactNames names of artifacts added or edited by this tool invocation
 */
public record ExecutionReport(
    boolean success,
    String summary,
    List<Map<String, String>> attempts,
    List<String> artifactNames
) {

  private static final int MAX_VALUE_LENGTH = 200;

  /**
   * Creates a builder for constructing an ExecutionReport incrementally as attempts are made.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean success;
    private String summary;
    private final List<Map<String, String>> attempts = new ArrayList<>();
    private final List<String> artifactNames = new ArrayList<>();

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public Builder summary(String summary) {
      this.summary = summary;
      return this;
    }

    /**
     * Records a single attempt with flexible metadata. Values are automatically truncated to
     * prevent context bloat.
     */
    public Builder addAttempt(Map<String, String> attemptData) {
      attempts.add(truncateValues(attemptData));
      return this;
    }

    public Builder addArtifactName(String name) {
      if (name != null) {
        artifactNames.add(name);
      }
      return this;
    }

    public ExecutionReport build() {
      return new ExecutionReport(
          success,
          summary,
          Collections.unmodifiableList(attempts),
          Collections.unmodifiableList(artifactNames)
      );
    }

    private static Map<String, String> truncateValues(Map<String, String> map) {
      var truncated = new java.util.LinkedHashMap<String, String>();
      for (var entry : map.entrySet()) {
        truncated.put(entry.getKey(), truncate(entry.getValue()));
      }
      return Collections.unmodifiableMap(truncated);
    }

    private static String truncate(String value) {
      if (value == null || value.length() <= MAX_VALUE_LENGTH) {
        return value;
      }
      // Take first two lines or MAX_VALUE_LENGTH chars, whichever is shorter
      var lines = value.split("\n", 3);
      String twoLines;
      if (lines.length >= 2) {
        twoLines = lines[0] + "\n" + lines[1];
      } else {
        twoLines = lines[0];
      }
      if (twoLines.length() > MAX_VALUE_LENGTH) {
        return twoLines.substring(0, MAX_VALUE_LENGTH) + "…";
      }
      return twoLines + (lines.length > 2 ? "…" : "");
    }
  }
}
