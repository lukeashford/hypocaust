package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.token.ContextWindowRegistry;
import com.example.hypocaust.token.TokenizerFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;

/**
 * Service for packing messages into context windows based on token limits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public final class ContextPackingService {

  private final TokenizerFactory tokenizerFactory;
  private final ContextWindowRegistry contextWindowRegistry;

  public static int SAFETY_MARGIN_PERCENT = 30;
  private final ModelRegistry modelRegistry;

  /**
   * Builds a context that fits within the model's token limits.
   *
   * @param modelName the model fileName
   * @param systemMessage the system message (always included)
   * @param chronologicalHistory the message history in chronological order
   * @return the packed messages that fit within the context window
   */
  public List<Message> buildContext(
      final String modelName,
      final SystemMessage systemMessage,
      final List<Message> chronologicalHistory) {

    final var tokenizer = tokenizerFactory.forModel(modelName);
    final int contextWindow = contextWindowRegistry.getContextWindow(modelName);
    final int maxOutputTokens = Optional.ofNullable(
        modelRegistry.get(modelName).getDefaultOptions().getMaxTokens()
    ).orElseThrow();

    final int safetyMargin = (int) Math.ceil(contextWindow * (SAFETY_MARGIN_PERCENT / 100.0));

    final int inputBudget = Math.max(0, contextWindow - maxOutputTokens - safetyMargin);
    log.debug("Context packing for model {}: window={}, maxOutput={}, safety={}, inputBudget={}",
        modelName, contextWindow, maxOutputTokens, safetyMargin, inputBudget);

    // Always include system message first
    final int systemTokens = tokenizer.countMessage(systemMessage);
    if (systemTokens > inputBudget) {
      throw new IllegalStateException(
          String.format("System prompt (%d tokens) exceeds input token budget (%d tokens)",
              systemTokens, inputBudget));
    }

    int usedTokens = systemTokens;

    // Pack messages from newest to oldest until budget is exhausted
    final var packedHistory = new ArrayDeque<Message>();
    for (int i = 0; i < chronologicalHistory.size(); i++) {
      final var message = chronologicalHistory.get(i);
      final int messageTokens = tokenizer.countMessage(message);

      if (usedTokens + messageTokens > inputBudget) {
        // Would exceed budget - stop packing
        log.debug("Stopping message packing at index {} - would exceed budget ({} + {} > {})",
            i, usedTokens, messageTokens, inputBudget);
        break;
      }

      packedHistory.addFirst(message);
      usedTokens += messageTokens;
    }

    // Build final result: system message + packed history
    final var result = new ArrayList<Message>(1 + packedHistory.size());
    result.add(systemMessage);
    result.addAll(packedHistory);

    log.debug("Context packed: {} total messages, {} total tokens",
        result.size(), usedTokens);

    return Collections.unmodifiableList(result);
  }
}