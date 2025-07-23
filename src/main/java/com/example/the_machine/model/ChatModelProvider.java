package com.example.the_machine.model;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Interface for providing chat models that can be used for generating responses.
 */
public interface ChatModelProvider {

  /**
   * Returns the configured chat model implementation.
   *
   * @return The chat model implementation
   */
  ChatModel getChatModel();
}