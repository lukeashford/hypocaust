package com.example.the_machine.models;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Abstract base class for model providers to eliminate code duplication. Provides common
 * functionality for creating models from provider configuration.
 */
@RequiredArgsConstructor
public abstract class AbstractModelProvider<M extends ChatModel, A> implements ModelProvider {

  protected final LlmConfiguration.ProviderConfig providerConfig;

  @Override
  public final Map<String, ChatModel> createModels() {
    val api = createApi(providerConfig.getApiKey());

    return providerConfig.getModels().entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> createChatModel(api, entry.getValue())
        ));
  }

  /**
   * Creates the provider-specific API client.
   *
   * @param apiKey the API key for the provider
   * @return the configured API client
   */
  protected abstract A createApi(String apiKey);

  /**
   * Creates a provider-specific ChatModel instance.
   *
   * @param api the API client
   * @param modelProps the model configuration properties
   * @return the configured ChatModel
   */
  protected abstract M createChatModel(A api, ModelProps modelProps);
}