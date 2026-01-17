package com.example.hypocaust.models;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ApiConfiguration {

  private final ModelProperties modelProperties;

  @Bean
  public AnthropicApi anthropicApi() {
    return AnthropicApi.builder().apiKey(modelProperties.getAnthropic().getApiKey()).build();
  }

  @Bean
  public OpenAiApi openAiApi() {
    return OpenAiApi.builder().apiKey(modelProperties.getOpenAi().getApiKey()).build();
  }

  @Bean
  public OpenAiImageApi openAiImageApi() {
    return OpenAiImageApi.builder()
        .apiKey(modelProperties.getOpenAi().getApiKey())
        .build();
  }

  @Bean
  public OpenAiImageModel openAiImageModel(OpenAiImageApi openAiImageApi) {
    return new OpenAiImageModel(openAiImageApi);
  }
}