package com.example.the_machine.models;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class OpenAiProvider extends AbstractModelProvider<OpenAiChatModel, OpenAiApi> {

  public OpenAiProvider(LlmConfiguration llmConfig) {
    super(llmConfig.getProviders().get("openai"));
  }

  @Override
  public String getProviderName() {
    return "openai";
  }

  @Override
  protected OpenAiApi createApi(String apiKey) {
    return OpenAiApi.builder()
        .apiKey(apiKey)
        .build();
  }

  @Override
  protected OpenAiChatModel createChatModel(OpenAiApi api, ModelProps modelProps) {
    return OpenAiChatModel.builder()
        .openAiApi(api)
        .defaultOptions(
            OpenAiChatOptions.builder()
                .model(modelProps.model())
                .temperature(modelProps.temperature())
                .maxTokens(modelProps.maxTokens())
                .build()
        )
        .build();
  }
}