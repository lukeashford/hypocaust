package com.example.the_machine.models

import com.example.the_machine.exception.ModelException
import com.example.the_machine.models.enums.AnthropicChatModelSpec
import com.example.the_machine.models.enums.OpenAiChatModelSpec
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.anthropic.api.AnthropicApi
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Component

@Component
class ModelFactory(
  private val modelProperties: ModelProperties,
  private val openAiApi: OpenAiApi,
  private val anthropicApi: AnthropicApi
) {

  fun createOpenAiChatModel(model: OpenAiChatModelSpec): OpenAiChatModel {
    val openAiConfig = modelProperties.openAi
      ?: throw ModelException("No OpenAI chat configuration found")

    val chatConfig = openAiConfig.chat
      ?: throw ModelException("No OpenAI chat configuration found")

    val options = chatConfig[model]
      ?: throw ModelException("No chat model configuration found for $model")

    options.model = model.modelName

    return OpenAiChatModel.builder()
      .openAiApi(openAiApi)
      .defaultOptions(options)
      .build()
  }

  fun createOpenAiEmbeddingModel(model: OpenAiEmbeddingModelSpec): OpenAiEmbeddingModel {
    val openAiConfig = modelProperties.openAi
      ?: throw ModelException("No OpenAI embedding configuration found")

    val embeddingConfig = openAiConfig.embedding
      ?: throw ModelException("No OpenAI embedding configuration found")

    val options = embeddingConfig[model]
      ?: throw ModelException("No embedding model configuration found for $model")

    options.model = model.modelName

    return OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options)
  }

  fun createAnthropicChatModel(model: AnthropicChatModelSpec): AnthropicChatModel {
    val anthropicConfig = modelProperties.anthropic
      ?: throw ModelException("No Anthropic chat configuration found")

    val chatConfig = anthropicConfig.chat
      ?: throw ModelException("No Anthropic chat configuration found")

    val options = chatConfig[model]
      ?: throw ModelException("No chat model configuration found for $model")

    options.model = model.modelName

    return AnthropicChatModel.builder()
      .anthropicApi(anthropicApi)
      .defaultOptions(options)
      .build()
  }
}