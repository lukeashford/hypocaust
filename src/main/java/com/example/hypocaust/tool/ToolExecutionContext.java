package com.example.hypocaust.tool;

import java.util.HashMap;
import java.util.Map;

/**
 * Carries tool-specific context through the orchestration pipeline without requiring mutable state
 * on singleton beans or ThreadLocal usage. For example, GenerateCreativeTool stores its
 * ModelSearchResult here.
 */
public record ToolExecutionContext(Map<String, Object> attributes) {

  public static ToolExecutionContext empty() {
    return new ToolExecutionContext(Map.of());
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type) {
    Object value = attributes.get(key);
    if (value == null) {
      return null;
    }
    return type.cast(value);
  }

  public ToolExecutionContext with(String key, Object value) {
    var newAttributes = new HashMap<>(attributes);
    newAttributes.put(key, value);
    return new ToolExecutionContext(Map.copyOf(newAttributes));
  }
}
