package com.example.hypocaust.tool.registry;

import java.util.List;
import java.util.Optional;
import org.springframework.ai.tool.ToolCallback;

/**
 * Registry for discoverable tools. Provides semantic search and lookup by name.
 */
public interface ToolRegistry {

  /**
   * Semantic search for tools matching a task description.
   *
   * @param taskDescription natural language description of the task
   * @return top matching tool descriptors ordered by similarity
   */
  List<ToolDescriptor> searchByTask(String taskDescription);

  /**
   * Get a tool callback by exact name.
   *
   * @param name the tool name
   * @return the tool callback if found
   */
  Optional<ToolCallback> getCallback(String name);

  int size();
}
