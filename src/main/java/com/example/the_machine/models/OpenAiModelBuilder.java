package com.example.the_machine.models;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

@RequiredArgsConstructor
public abstract class OpenAiModelBuilder implements ModelBuilder {

  protected final OpenAiApi api;

  protected OpenAiChatOptions.Builder createBaseOptions(Map<String, String> props) {
    return OpenAiChatOptions.builder()
        .model(props.get("model"))
        .temperature(Double.valueOf(props.get("temperature")))
        .maxTokens(Integer.valueOf(props.get("maxTokens")));
  }

  @Override
  public ChatModel from(Map<String, String> props) {
    return OpenAiChatModel.builder()
        .openAiApi(api)
        .defaultOptions(buildOptions(props))
        .build();
  }

  protected abstract OpenAiChatOptions buildOptions(Map<String, String> props);
}
