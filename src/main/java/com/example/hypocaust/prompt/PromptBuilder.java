package com.example.hypocaust.prompt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fluent builder for composing system prompts from reusable fragments.
 *
 * <p>Fragments are deduplicated by ID and ordered by priority, then combined
 * with configurable separators. Placeholders in the form {{key}} are resolved
 * from the provided parameters.
 *
 * <p>Example usage:
 * <pre>{@code
 * var prompt = PromptBuilder.create()
 *     .with(DecompositionFragments.core())
 *     .with(DecompositionFragments.artifactAwareness())
 *     .with(DecompositionFragments.selfCorrection())
 *     .param("maxChildren", 3)
 *     .build();
 * }</pre>
 */
public final class PromptBuilder {

  private final Set<PromptFragment> fragments = new LinkedHashSet<>();
  private final Map<String, Object> params = new HashMap<>();
  private String separator = "\n\n";

  private PromptBuilder() {
  }

  /**
   * Creates a new PromptBuilder instance.
   *
   * @return a new builder
   */
  public static PromptBuilder create() {
    return new PromptBuilder();
  }

  /**
   * Adds a fragment to the prompt. Duplicate fragments (by ID) are ignored.
   *
   * @param fragment the fragment to add
   * @return this builder for chaining
   */
  public PromptBuilder with(PromptFragment fragment) {
    if (fragment != null) {
      fragments.add(fragment);
    }
    return this;
  }

  /**
   * Adds multiple fragments to the prompt.
   *
   * @param fragments the fragments to add
   * @return this builder for chaining
   */
  public PromptBuilder withAll(List<PromptFragment> fragments) {
    if (fragments != null) {
      fragments.forEach(this::with);
    }
    return this;
  }

  /**
   * Sets a parameter value for placeholder resolution.
   * Placeholders in the form {{key}} will be replaced with the value.
   *
   * @param key   the parameter name
   * @param value the parameter value
   * @return this builder for chaining
   */
  public PromptBuilder param(String key, Object value) {
    params.put(key, value);
    return this;
  }

  /**
   * Sets multiple parameters for placeholder resolution.
   *
   * @param params the parameters to set
   * @return this builder for chaining
   */
  public PromptBuilder params(Map<String, Object> params) {
    if (params != null) {
      this.params.putAll(params);
    }
    return this;
  }

  /**
   * Sets the separator used between fragments. Default is double newline.
   *
   * @param separator the separator string
   * @return this builder for chaining
   */
  public PromptBuilder separator(String separator) {
    this.separator = separator;
    return this;
  }

  /**
   * Builds the final prompt string by combining all fragments.
   * Fragments are sorted by priority (ascending), deduplicated by ID,
   * and concatenated with the separator. Placeholders are resolved.
   *
   * @return the complete prompt string
   */
  public String build() {
    // Sort by priority, then combine
    List<PromptFragment> sorted = new ArrayList<>(fragments);
    sorted.sort(Comparator.comparingInt(PromptFragment::priority));

    String combined = sorted.stream()
        .map(PromptFragment::text)
        .collect(Collectors.joining(separator));

    return resolvePlaceholders(combined);
  }

  /**
   * Returns the list of fragment IDs in the current builder state, sorted by priority.
   * Useful for debugging and testing.
   *
   * @return list of fragment IDs
   */
  public List<String> fragmentIds() {
    return fragments.stream()
        .sorted(Comparator.comparingInt(PromptFragment::priority))
        .map(PromptFragment::id)
        .toList();
  }

  private String resolvePlaceholders(String text) {
    String result = text;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      String value = String.valueOf(entry.getValue());
      result = result.replace(placeholder, value);
    }
    return result;
  }
}
