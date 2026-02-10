package com.example.hypocaust.models;

import com.example.hypocaust.exception.ModelException;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import com.example.hypocaust.models.enums.OpenAiEmbeddingModelSpec;
import com.example.hypocaust.models.enums.OpenAiImageModelSpec;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRegistry {

  private final ModelProperties modelProperties;
  private final ModelFactory modelFactory;
  private final ApplicationContext applicationContext;

  @PostConstruct
  private void init() {
    registerModelsFromConfiguration();
  }

  private void registerModelsFromConfiguration() {
    final var beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

    // Register OpenAI chat models
    if (modelProperties.getOpenAi() != null && modelProperties.getOpenAi().getChat() != null) {
      modelProperties.getOpenAi().getChat().keySet().forEach(modelSpec ->
          registerOpenAiChatModel(beanFactory, modelSpec));
    }

    // Register OpenAI embedding models
    if (modelProperties.getOpenAi() != null && modelProperties.getOpenAi().getEmbedding() != null) {
      modelProperties.getOpenAi().getEmbedding().keySet().forEach(modelSpec ->
          registerOpenAiEmbeddingModel(beanFactory, modelSpec));
    }

    // Register Anthropic chat models
    if (modelProperties.getAnthropic() != null
        && modelProperties.getAnthropic().getChat() != null) {
      modelProperties.getAnthropic().getChat().keySet().forEach(modelSpec ->
          registerAnthropicChatModel(beanFactory, modelSpec));
    }

    // Register OpenAI image models
    if (modelProperties.getOpenAi() != null && modelProperties.getOpenAi().getImage() != null) {
      modelProperties.getOpenAi().getImage().keySet().forEach(modelSpec ->
          registerOpenAiImageModel(beanFactory, modelSpec));
    }
  }

  private void registerOpenAiChatModel(ConfigurableListableBeanFactory beanFactory,
      OpenAiChatModelSpec modelSpec) {
    try {
      final var model = modelFactory.createOpenAiChatModel(modelSpec);
      final var modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered OpenAI chat model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register OpenAI chat model: {}", modelSpec, e);
    }
  }

  private void registerOpenAiEmbeddingModel(ConfigurableListableBeanFactory beanFactory,
      OpenAiEmbeddingModelSpec modelSpec) {
    try {
      final var model = modelFactory.createOpenAiEmbeddingModel(modelSpec);
      final var modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered OpenAI embedding model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register OpenAI embedding model: {}", modelSpec, e);
    }
  }

  private void registerAnthropicChatModel(ConfigurableListableBeanFactory beanFactory,
      AnthropicChatModelSpec modelSpec) {
    try {
      final var model = modelFactory.createAnthropicChatModel(modelSpec);
      final var modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered Anthropic chat model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register Anthropic chat model: {}", modelSpec, e);
    }
  }

  private void registerOpenAiImageModel(ConfigurableListableBeanFactory beanFactory,
      OpenAiImageModelSpec modelSpec) {
    try {
      final var model = modelFactory.createOpenAiImageModel(modelSpec);
      final var modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered OpenAI image model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register OpenAI image model: {}", modelSpec, e);
    }
  }

  public ChatModel get(String modelName) {
    return applicationContext.getBean(modelName, ChatModel.class);
  }

  public Set<String> listAvailableModels() {
    final var beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    return Arrays.stream(beanFactory.getBeanNamesForType(ChatModel.class))
        .collect(java.util.stream.Collectors.toSet());
  }

  // Type-safe enum-based access methods
  public OpenAiChatModel get(OpenAiChatModelSpec model) {
    return applicationContext.getBean(model.getModelName(), OpenAiChatModel.class);
  }

  public OpenAiEmbeddingModel get(OpenAiEmbeddingModelSpec model) {
    return applicationContext.getBean(model.getModelName(), OpenAiEmbeddingModel.class);
  }

  public AnthropicChatModel get(AnthropicChatModelSpec model) {
    return applicationContext.getBean(model.getModelName(), AnthropicChatModel.class);
  }

  public OpenAiImageModel get(OpenAiImageModelSpec model) {
    return applicationContext.getBean(model.getModelName(), OpenAiImageModel.class);
  }
}
