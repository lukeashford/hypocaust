package com.example.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for chat models.
 */
@Configuration
public class ModelConfiguration {

  /**
   * OpenAI Chat Model bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.chat-model", havingValue = "openai")
  public ChatModel openAiChatModel(
      @Value("${openai.api-key}") String openAiApiKey) {
    return new OpenAiChatModel(openAiApiKey);
  }

  /**
   * OpenAI Image Model bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.chat-model", havingValue = "openai")
  public ImageModel openAiImageModel(
      @Value("${openai.api-key}") String openAiApiKey) {
    return dev.langchain4j.model.openai.OpenAiImageModel.builder()
        .apiKey(openAiApiKey)
        .modelName("dall-e-3")
        .quality("hd")
        .size("1024x1024")
        .build();
  }

  /**
   * Claude Chat Model bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.chat-model", havingValue = "claude")
  public ChatModel claudeChatModel() {
    throw new UnsupportedOperationException(
        "Claude chat model is not implemented yet. " +
            "Add the appropriate langchain4j dependency and implement this method.");
  }

  /**
   * Local Embedding Model bean.
   */
  @Bean
  @ConditionalOnProperty(name = "app.embedding", havingValue = "local")
  public EmbeddingModel localEmbeddingModel() {
    return new LocalEmbeddingModel();
  }
}