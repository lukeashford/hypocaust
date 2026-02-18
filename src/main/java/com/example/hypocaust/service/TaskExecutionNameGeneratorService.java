package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import org.springframework.stereotype.Service;

/**
 * Generates unique task execution names from the task description. These names make the version
 * timeline LLM-addressable: instead of UUIDs, executions are referenced by readable names like
 * {@code initial_character_designs} or {@code hair_color_change}.
 */
@Service
public class TaskExecutionNameGeneratorService extends LlmNameGeneratorService {

  public TaskExecutionNameGeneratorService(ModelRegistry modelRegistry) {
    super(modelRegistry, 50);
  }

  @Override
  protected String systemPrompt() {
    return buildSystemPrompt("a task execution", "The name should describe the intent of the task.",
        "initial_character_designs, hair_color_change, forest_background_added");
  }

  @Override
  protected String defaultName() {
    return "task";
  }
}
