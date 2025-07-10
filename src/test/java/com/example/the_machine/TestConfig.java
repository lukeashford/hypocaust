package com.example.the_machine;

import com.example.the_machine.langchain.HelloWorldAgentService;
import dev.langchain4j.model.chat.ChatModel;
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
   * Provides a mock implementation of the HelloWorldAgentService to avoid making real API calls
   * during tests.
   */
  @Bean
  @Primary
  public HelloWorldAgentService helloWorldAgentService() {
    HelloWorldAgentService mockService = Mockito.mock(HelloWorldAgentService.class);
    Mockito.when(mockService.getGreeting(Mockito.anyString()))
        .thenReturn("Hello from mock agent!");
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
}