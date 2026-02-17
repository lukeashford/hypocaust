package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Generates unique task execution names from the task description. These names make the version
 * timeline LLM-addressable: instead of UUIDs, executions are referenced by readable names like
 * {@code initial_character_designs} or {@code hair_color_change}.
 */
@Service
public class TaskExecutionNameGeneratorService extends LlmNameGeneratorService {

  private static final int MAX_NAME_LENGTH = 50;

  public TaskExecutionNameGeneratorService(ModelRegistry modelRegistry) {
    super(modelRegistry);
  }

  @Override
  protected String systemPrompt() {
    return """
        Generate a short snake_case name for a task execution (max 50 chars).
        The name should describe the intent of the task.
        Use only lowercase letters, numbers, and underscores.
        Reply with ONLY the name, nothing else.
        Examples: initial_character_designs, hair_color_change, forest_background_added
        """;
  }

  @Override
  protected int maxLength() {
    return MAX_NAME_LENGTH;
  }

  /**
   * Generate a unique execution name from the task description.
   *
   * @param task the task description
   * @param existingNames execution names already taken in this project
   * @return a unique snake_case name
   */
  public String generateUniqueName(String task, Set<String> existingNames) {
    String userPrompt = task != null ? "Task: " + task : "task";
    if (!existingNames.isEmpty()) {
      userPrompt += "\n\nThe following names are already taken, choose a different one: "
          + String.join(", ", existingNames);
    }
    return generateUniqueName(userPrompt, task != null ? task : "task", existingNames);
  }
}
