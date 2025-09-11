package com.example.the_machine.models

import com.example.the_machine.models.enums.AnthropicChatModelSpec
import com.example.the_machine.models.enums.OpenAiChatModelSpec
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec
import org.springframework.ai.anthropic.AnthropicChatOptions
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.llm")
data class ModelProperties(
  var openAi: OpenAi? = null,
  var anthropic: Anthropic? = null
) {

  data class OpenAi(
    var apiKey: String? = null,
    var chat: Map<OpenAiChatModelSpec, OpenAiChatOptions>? = null,
    var embedding: Map<OpenAiEmbeddingModelSpec, OpenAiEmbeddingOptions>? = null
  )

  data class Anthropic(
    var apiKey: String? = null,
    var chat: Map<AnthropicChatModelSpec, AnthropicChatOptions>? = null
  )
}