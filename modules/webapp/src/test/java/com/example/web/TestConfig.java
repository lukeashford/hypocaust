package com.example.web;

import com.example.web.service.BrandIntelService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides mock implementations of beans that require external resources.
 */
@TestConfiguration
public class TestConfig {

  /**
   * Provides a mock implementation of the BrandIntelService to avoid making real API calls
   * during tests.
   */
  @Bean
  @Primary
  public BrandIntelService brandIntelService() {
    BrandIntelService mockService = Mockito.mock(BrandIntelService.class);
    Mockito.when(mockService.analyzeBrand(Mockito.anyString()))
        .thenReturn("Mock brand analysis for testing purposes.");
    return mockService;
  }

  /**
   * Provides a mock implementation of the ChatLanguageModel to avoid making real API calls during
   * tests.
   */
  @Bean
  @Primary
  public ChatModel chatLanguageModel() {
    return Mockito.mock(ChatModel.class);
  }

  /**
   * Provides a mock implementation of the EmbeddingModel to avoid loading the actual model during
   * tests.
   */
  @Bean
  @Primary
  public EmbeddingModel embeddingModel() {
    return Mockito.mock(EmbeddingModel.class);
  }
}