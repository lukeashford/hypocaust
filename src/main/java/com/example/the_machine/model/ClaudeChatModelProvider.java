package com.example.the_machine.model;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Anthropic Claude Chat Model Provider implementation. Note: This is a stub implementation. To use
 * Claude, you would need to add the appropriate langchain4j dependency and implement the actual
 * Claude model logic.
 */
@Component
@Profile("claude")
public class ClaudeChatModelProvider implements ChatModelProvider {

  /**
   * This is a stub implementation. In a real implementation, you would: 1. Add the appropriate
   * langchain4j dependency for Claude 2. Configure the Claude model with your API key 3. Return the
   * configured Claude model
   */
  @Override
  public ChatModel getChatModel() {
    throw new UnsupportedOperationException(
        "Claude chat model is not implemented yet. " +
            "Add the appropriate langchain4j dependency and implement this method.");
  }
}