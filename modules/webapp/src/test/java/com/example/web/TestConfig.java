package com.example.web;

import com.example.dto.CompanyAnalysisDto;
import com.example.web.service.BrandIntelService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.List;
import lombok.val;
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
   * Provides a mock implementation of the BrandIntelService to avoid making real API calls during
   * tests.
   */
  @Bean
  @Primary
  public BrandIntelService brandIntelService() {
    val mockService = Mockito.mock(BrandIntelService.class);
    val mockAnalysis = new CompanyAnalysisDto(
        "Mock brand analysis for testing purposes.",
        List.of("Test key point 1", "Test key point 2"),
        "Mock brand personality",
        "Mock target audience",
        "Mock visual style",
        List.of("Mock key message 1", "Mock key message 2"),
        List.of("Mock competitive advantage 1", "Mock competitive advantage 2")
    );
    Mockito.when(mockService.analyzeBrand(Mockito.anyString()))
        .thenReturn(mockAnalysis);
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