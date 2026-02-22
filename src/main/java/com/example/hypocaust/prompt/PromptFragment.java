package com.example.hypocaust.prompt;

/**
 * Represents a composable prompt fragment that can be combined with others to build complete system
 * prompts for operators.
 */
public record PromptFragment(String id, String text, int priority) {

  public PromptFragment(String id, String text) {
    this(id, text, 100);
  }
}
