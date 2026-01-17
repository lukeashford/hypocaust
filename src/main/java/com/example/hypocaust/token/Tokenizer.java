package com.example.hypocaust.token;

import org.springframework.ai.chat.messages.Message;

/**
 * Interface for counting tokens in text and messages.
 */
public interface Tokenizer {

  /**
   * Count tokens in plain text.
   *
   * @param text the text to count tokens for
   * @return the token count
   */
  int count(String text);

  /**
   * Count tokens in a Spring AI message, including role overhead.
   *
   * @param message the message to count tokens for
   * @return the token count
   */
  int countMessage(Message message);
}