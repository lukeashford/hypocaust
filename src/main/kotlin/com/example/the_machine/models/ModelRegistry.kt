package com.example.the_machine.models

import com.example.the_machine.exception.ModelException
import com.example.the_machine.models.enums.AnthropicChatModelSpec
import com.example.the_machine.models.enums.OpenAiChatModelSpec
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ModelRegistry(
  private val modelProperties: ModelProperties,
  private val modelFactory: ModelFactory,
  private val applicationContext: ApplicationContext
) {

  @PostConstruct
  private fun init() {
    registerModelsFromConfiguration()
  }

  private fun registerModelsFromConfiguration() {
    val beanFactory = (applicationContext as ConfigurableApplicationContext).beanFactory

    // Register OpenAI chat models
    modelProperties.openAi?.chat?.keys?.forEach { modelSpec ->
      registerOpenAiChatModel(beanFactory, modelSpec)
    }

    // Register OpenAI embedding models
    modelProperties.openAi?.embedding?.keys?.forEach { modelSpec ->
      registerOpenAiEmbeddingModel(beanFactory, modelSpec)
    }

    // Register Anthropic chat models
    modelProperties.anthropic?.chat?.keys?.forEach { modelSpec ->
      registerAnthropicChatModel(beanFactory, modelSpec)
    }
  }

  private fun registerOpenAiChatModel(
    beanFactory: ConfigurableListableBeanFactory,
    modelSpec: OpenAiChatModelSpec
  ) {
    try {
      val model = modelFactory.createOpenAiChatModel(modelSpec)
      val modelName = modelSpec.modelName
      beanFactory.registerSingleton(modelName, model)
      logger.info("Registered OpenAI chat model bean: {}", modelName)
    } catch (e: ModelException) {
      logger.error("Failed to register OpenAI chat model: {}", modelSpec, e)
    }
  }

  private fun registerOpenAiEmbeddingModel(
    beanFactory: ConfigurableListableBeanFactory,
    modelSpec: OpenAiEmbeddingModelSpec
  ) {
    try {
      val model = modelFactory.createOpenAiEmbeddingModel(modelSpec)
      val modelName = modelSpec.modelName
      beanFactory.registerSingleton(modelName, model)
      logger.info("Registered OpenAI embedding model bean: {}", modelName)
    } catch (e: ModelException) {
      logger.error("Failed to register OpenAI embedding model: {}", modelSpec, e)
    }
  }

  private fun registerAnthropicChatModel(
    beanFactory: ConfigurableListableBeanFactory,
    modelSpec: AnthropicChatModelSpec
  ) {
    try {
      val model = modelFactory.createAnthropicChatModel(modelSpec)
      val modelName = modelSpec.modelName
      beanFactory.registerSingleton(modelName, model)
      logger.info("Registered Anthropic chat model bean: {}", modelName)
    } catch (e: ModelException) {
      logger.error("Failed to register Anthropic chat model: {}", modelSpec, e)
    }
  }

  fun get(modelName: String): ChatModel {
    return applicationContext.getBean(modelName, ChatModel::class.java)
  }

  fun listAvailableModels(): Set<String> {
    val beanFactory = (applicationContext as ConfigurableApplicationContext).beanFactory
    return beanFactory.getBeanNamesForType(ChatModel::class.java)
      .toSet()
  }

  // Type-safe enum-based access methods
  fun get(model: OpenAiChatModelSpec): OpenAiChatModel {
    return applicationContext.getBean(model.modelName, OpenAiChatModel::class.java)
  }

  fun get(model: OpenAiEmbeddingModelSpec): OpenAiEmbeddingModel {
    return applicationContext.getBean(model.modelName, OpenAiEmbeddingModel::class.java)
  }

  fun get(model: AnthropicChatModelSpec): AnthropicChatModel {
    return applicationContext.getBean(model.modelName, AnthropicChatModel::class.java)
  }
}