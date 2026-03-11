package com.example.hypocaust.models;

import com.example.hypocaust.models.anthropic.AnthropicPromptCachingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class ApiConfiguration {

  /**
   * Beta features sent on every request via the {@code anthropic-beta} header.
   * Includes the prompt-caching beta so that {@link AnthropicPromptCachingInterceptor}
   * cache_control markers are honoured by the API.
   */
  private static final String ANTHROPIC_BETA_FEATURES =
      "tools-2024-04-04,pdfs-2024-09-25,prompt-caching-2024-07-31";

  private final ModelProperties modelProperties;
  private final AnthropicPromptCachingInterceptor promptCachingInterceptor;

  @Bean
  public AnthropicApi anthropicApi() {
    return AnthropicApi.builder()
        .apiKey(modelProperties.getAnthropic().getApiKey())
        .anthropicBetaFeatures(ANTHROPIC_BETA_FEATURES)
        .restClientBuilder(RestClient.builder().requestInterceptor(promptCachingInterceptor))
        .build();
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
}