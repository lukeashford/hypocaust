package com.example.hypocaust.token;

import com.knuddels.jtokkit.api.Encoding;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;

/**
 * OpenAI-compatible tokenizer implementation using jtokkit.
 */
@RequiredArgsConstructor
public final class OpenAiLikeTokenizer implements Tokenizer {

  private final Encoding encoding;

  @Override
  public int count(final String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }

    return encoding.encode(text).size();
  }

  @Override
  public int countMessage(final Message message) {
    if (message == null) {
      return 0;
    }

    final int messageOverhead = 4;

    return count(message.getText()) + messageOverhead;
  }
}