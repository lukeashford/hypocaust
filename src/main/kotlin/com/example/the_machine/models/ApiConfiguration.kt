package com.example.the_machine.models

import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiConfiguration(
  private val modelProperties: ModelProperties
) {

  @Bean
  fun anthropicApi(): AnthropicApi {
    return AnthropicApi.builder()
      .apiKey(modelProperties.anthropic?.apiKey)
      .build()
  }

  @Bean
  fun openAiApi(): OpenAiApi {
    return OpenAiApi.builder()
      .apiKey(modelProperties.openAi?.apiKey)
      .build()
  }
}