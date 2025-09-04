package com.example.the_machine.models;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;

@RequiredArgsConstructor
public abstract class AnthropicModelBuilder implements ModelBuilder {

  protected final AnthropicApi api;

  protected AnthropicChatOptions.Builder createBaseOptions(Map<String, String> props) {
    return AnthropicChatOptions.builder()
        .model(props.get("model"))
        .temperature(Double.valueOf(props.get("temperature")))
        .maxTokens(Integer.valueOf(props.get("maxTokens")));
  }

  @Override
  public ChatModel from(Map<String, String> props) {
    return AnthropicChatModel.builder()
        .anthropicApi(api)
        .defaultOptions(buildOptions(props))
        .build();
  }

  protected abstract AnthropicChatOptions buildOptions(Map<String, String> props);
}