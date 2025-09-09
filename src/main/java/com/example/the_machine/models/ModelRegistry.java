package com.example.the_machine.models;

import com.example.the_machine.exception.ModelException;
import com.example.the_machine.models.enums.AnthropicChatModelSpec;
import com.example.the_machine.models.enums.OpenAiChatModelSpec;
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
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
    val beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

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
  }

  private void registerOpenAiChatModel(ConfigurableListableBeanFactory beanFactory,
      OpenAiChatModelSpec modelSpec) {
    try {
      val model = modelFactory.createOpenAiChatModel(modelSpec);
      val modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered OpenAI chat model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register OpenAI chat model: {}", modelSpec, e);
    }
  }

  private void registerOpenAiEmbeddingModel(ConfigurableListableBeanFactory beanFactory,
      OpenAiEmbeddingModelSpec modelSpec) {
    try {
      val model = modelFactory.createOpenAiEmbeddingModel(modelSpec);
      val modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered OpenAI embedding model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register OpenAI embedding model: {}", modelSpec, e);
    }
  }

  private void registerAnthropicChatModel(ConfigurableListableBeanFactory beanFactory,
      AnthropicChatModelSpec modelSpec) {
    try {
      val model = modelFactory.createAnthropicChatModel(modelSpec);
      val modelName = modelSpec.getModelName();
      beanFactory.registerSingleton(modelName, model);
      log.info("Registered Anthropic chat model bean: {}", modelName);
    } catch (ModelException e) {
      log.error("Failed to register Anthropic chat model: {}", modelSpec, e);
    }
  }

  public ChatModel get(String modelName) {
    return applicationContext.getBean(modelName, ChatModel.class);
  }

  public Set<String> listAvailableModels() {
    val beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
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
}
