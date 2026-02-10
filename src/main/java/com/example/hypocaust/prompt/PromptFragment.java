package com.example.hypocaust.prompt;

/**
 * Represents a composable prompt fragment that can be combined with others
 * to build complete system prompts for operators.
 *
 * <p>Each fragment is a self-contained instruction block that can be mixed and matched
 * based on operator requirements. This enables consistent behavior across operators
 * while allowing customization.
 */
public interface PromptFragment {

  /**
   * Returns the prompt text for this fragment.
   * May contain placeholders in the form {{key}} that will be resolved by PromptBuilder.
   *
   * @return the prompt text
   */
  String text();

  /**
   * Returns a unique identifier for this fragment, used for ordering and deduplication.
   *
   * @return the fragment identifier
   */
  String id();

  /**
   * Returns the priority for ordering fragments in the final prompt.
   * Lower values appear first. Default is 100.
   *
   * @return the ordering priority
   */
  default int priority() {
    return 100;
  }
}
