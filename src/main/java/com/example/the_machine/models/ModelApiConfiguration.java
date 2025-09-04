package com.example.the_machine.models;

import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelApiConfiguration {

  @Bean
  public AnthropicApi anthropicApi(
      @Value("${app.llm.platforms.anthropic.api-key}") String apiKey
  ) {
    return AnthropicApi.builder().apiKey(apiKey).build();
  }

  @Bean
  public OpenAiApi openAiApi(
      @Value("${app.llm.platforms.openai.api-key}") String apiKey
  ) {
    return OpenAiApi.builder().apiKey(apiKey).build();
  }
}