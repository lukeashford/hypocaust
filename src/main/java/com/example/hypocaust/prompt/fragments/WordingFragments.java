package com.example.hypocaust.prompt.fragments;

import com.example.hypocaust.prompt.PromptFragment;

/**
 * Prompt fragments for generating human-friendly labels and summaries.
 */
public final class WordingFragments {

  private WordingFragments() {
  }

  /**
   * Generates a brief progress label (1-5 words) for a task.
   */
  public static PromptFragment todoLabel() {
    return new PromptFragment(
        "wording-todo-label",
        """
            Generate a brief progress label (1-5 words) for this task.
            Focus on the action. Start with a present participle like 'Adding', 'Creating', 'Updating'.
            Output ONLY the label."""
    );
  }

  /**
   * Generates a brief commit message (1 sentence, max 100 chars) summarizing what was done.
   */
  public static PromptFragment commitMessage() {
    return new PromptFragment(
        "wording-commit-message",
        """
            Generate a brief commit message (1 sentence, max 100 chars) summarizing what was done.
            Focus on the outcome, not the process. Start with a verb like 'Added', 'Created', 'Updated'.
            Output ONLY the message."""
    );
  }

  /**
   * Generates a short snake_case name for a task execution (max 50 chars).
   */
  public static PromptFragment taskExecutionName() {
    return new PromptFragment(
        "wording-task-execution-name",
        """
            Generate a short snake_case name for a task execution (max 50 chars).
            The name should describe the intent of the task.
            Use only lowercase letters, numbers, and underscores.
            Reply with ONLY the name, nothing else.
            Examples: initial_character_designs, hair_color_change, forest_background_added"""
    );
  }

  /**
   * Generates a short snake_case name for an artifact (max 30 chars).
   */
  public static PromptFragment artifactName() {
    return new PromptFragment(
        "wording-artifact-name",
        """
            Generate a short snake_case name for an artifact (max 30 chars).
            Use only lowercase letters, numbers, and underscores.
            Reply with ONLY the name, nothing else.
            Examples: hero_portrait, forest_background, main_script"""
    );
  }

  /**
   * Generates a catchy title (max 60 chars) for a creative artifact.
   */
  public static PromptFragment artifactTitle() {
    return new PromptFragment(
        "wording-artifact-title",
        """
            The user will provide a prompt describing a creative generation task.
            Your job is to generate a catchy, human-friendly title (max 60 characters) for the artifact that will be produced by this task.
            
            IMPORTANT: You are NOT performing the task. You are only naming the expected outcome.
            Output ONLY the title, without quotes or explanation."""
    );
  }

  /**
   * Generates a brief description (1-2 sentences) for a creative artifact.
   */
  public static PromptFragment artifactDescription() {
    return new PromptFragment(
        "wording-artifact-description",
        """
            The user will provide a prompt describing a creative generation task.
            Your job is to generate a brief, 1-2 sentence description for the artifact that will be produced by this task.
            Focus on the intended content, style, and subject matter.
            
            IMPORTANT: You are NOT performing the task. You are only describing the expected outcome.
            Output ONLY the description."""
    );
  }
}
