package com.example.web.service.util;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Base class for AI services that provides common ChatModel interaction patterns
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseAiService {

  protected final ChatModel chatModel;
  protected final JsonResponseParser jsonParser;

  /**
   * Common pattern for AI service generation with system message, user prompt, and fallback
   *
   * @param systemMessage The system message defining the AI's role
   * @param userPrompt The user prompt with specific instructions
   * @param targetClass The class to parse the response into
   * @param fallbackSupplier Supplier for fallback object if parsing fails
   * @param operationName Name of the operation for logging
   * @param brandName Brand name for error context
   * @param <T> The target type
   * @return Parsed response or fallback
   */
  protected <T> T generateWithAi(
      String systemMessage,
      String userPrompt,
      Class<T> targetClass,
      Supplier<T> fallbackSupplier,
      String operationName,
      String brandName) {

    try {
      val systemMsg = SystemMessage.from(systemMessage);
      val userMsg = UserMessage.from(userPrompt);
      val messages = List.of(systemMsg, userMsg);

      val response = chatModel.chat(messages).aiMessage().text();
      log.debug("AI response for {}: {}", operationName, response);

      return jsonParser.parseWithFallback(response, targetClass, fallbackSupplier);

    } catch (Exception e) {
      log.error("Error during {} for brand: {}", operationName, brandName, e);
      throw new RuntimeException("Failed to " + operationName + ": " + e.getMessage(), e);
    }
  }
}