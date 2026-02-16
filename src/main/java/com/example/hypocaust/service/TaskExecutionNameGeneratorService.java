package com.example.hypocaust.service;

import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.models.ModelRegistry;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Generates unique task execution names from task description, commit message, and delta. These
 * names make the version timeline LLM-addressable: instead of UUIDs, executions are referenced by
 * readable names like {@code initial_character_designs} or {@code hair_color_change}.
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
        Generate a short snake_case name for a completed task execution (max 50 chars).
        The name should describe the outcome, not the process.
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
   * Generate a unique execution name from context about what happened.
   *
   * @param task the original task description
   * @param commitMessage the LLM-generated commit message summarizing the outcome
   * @param delta what artifacts were added/edited/deleted
   * @param existingNames execution names already taken in this project
   * @return a unique snake_case name
   */
  public String generateUniqueName(String task, String commitMessage, TaskExecutionDelta delta,
      Set<String> existingNames) {
    return generateUniqueName(
        buildUserPrompt(task, commitMessage, delta, existingNames),
        task != null ? task : "task",
        existingNames);
  }

  private String buildUserPrompt(String task, String commitMessage, TaskExecutionDelta delta,
      Set<String> existingNames) {
    var prompt = new StringBuilder();
    if (task != null) {
      prompt.append("Task: ").append(task);
    }
    if (commitMessage != null) {
      prompt.append("\nOutcome: ").append(commitMessage);
    }
    if (delta != null && delta.hasChanges()) {
      prompt.append("\nArtifacts changed:");
      if (!delta.added().isEmpty()) {
        prompt.append(" added: ").append(
            delta.added().stream().map(ArtifactChange::name).collect(Collectors.joining(", ")));
      }
      if (!delta.edited().isEmpty()) {
        prompt.append(" edited: ").append(
            delta.edited().stream().map(ArtifactChange::name).collect(Collectors.joining(", ")));
      }
      if (!delta.deleted().isEmpty()) {
        prompt.append(" deleted: ").append(String.join(", ", delta.deleted()));
      }
    }
    if (!existingNames.isEmpty()) {
      prompt.append("\n\nThe following names are already taken, choose a different one: ");
      prompt.append(String.join(", ", existingNames));
    }
    return prompt.toString();
  }
}
