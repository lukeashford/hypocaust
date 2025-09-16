package com.example.the_machine.token;

/**
 * Factory for creating tokenizers based on model names.
 */
public interface TokenizerFactory {

  /**
   * Creates a tokenizer for the specified model.
   *
   * @param modelName the name of the model
   * @return the appropriate tokenizer for the model
   */
  Tokenizer forModel(String modelName);
}