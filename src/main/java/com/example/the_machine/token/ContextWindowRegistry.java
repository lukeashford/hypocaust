package com.example.the_machine.token;

/**
 * Registry for model context window sizes.
 */
public interface ContextWindowRegistry {

  /**
   * Gets the context window size for the specified model.
   *
   * @param modelName the name of the model
   * @return the context window size in tokens
   */
  int getContextWindow(String modelName);
}