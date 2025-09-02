package com.example.the_machine.models;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.stereotype.Component;

@Component
public class AnthropicProvider extends AbstractModelProvider<AnthropicChatModel, AnthropicApi> {

  public AnthropicProvider(LlmConfiguration llmConfig) {
    super(llmConfig.getProviders().get("anthropic"));
  }

  @Override
  public String getProviderName() {
    return "anthropic";
  }

  @Override
  protected AnthropicApi createApi(String apiKey) {
    return AnthropicApi.builder()
        .apiKey(apiKey)
        .build();
  }

  @Override
  protected AnthropicChatModel createChatModel(AnthropicApi api, ModelProps modelProps) {
    return AnthropicChatModel.builder()
        .anthropicApi(api)
        .defaultOptions(
            AnthropicChatOptions.builder()
                .model(modelProps.model())
                .temperature(modelProps.temperature())
                .maxTokens(modelProps.maxTokens())
                .build()
        )
        .build();
  }
}