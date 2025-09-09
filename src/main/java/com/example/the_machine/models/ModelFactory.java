package com.example.the_machine.models;

import com.example.the_machine.exception.ModelException;
import com.example.the_machine.models.enums.AnthropicChatModelSpec;
import com.example.the_machine.models.enums.OpenAiChatModelSpec;
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelFactory {

  private final ModelProperties modelProperties;
  private final OpenAiApi openAiApi;
  private final AnthropicApi anthropicApi;

  public OpenAiChatModel createOpenAiChatModel(
      OpenAiChatModelSpec model) throws ModelException {
    if (modelProperties.getOpenAi() == null || modelProperties.getOpenAi().getChat() == null) {
      throw new ModelException("No OpenAI chat configuration found");
    }

    val options = modelProperties.getOpenAi().getChat().get(model);
    if (options == null) {
      throw new ModelException("No chat model configuration found for " + model);
    }
    options.setModel(model.getModelName());

    return OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .defaultOptions(options)
        .build();
  }

  public OpenAiEmbeddingModel createOpenAiEmbeddingModel(
      OpenAiEmbeddingModelSpec model) throws ModelException {
    if (modelProperties.getOpenAi() == null || modelProperties.getOpenAi().getEmbedding() == null) {
      throw new ModelException("No OpenAI embedding configuration found");
    }

    val options = modelProperties.getOpenAi().getEmbedding().get(model);
    if (options == null) {
      throw new ModelException("No embedding model configuration found for " + model);
    }
    options.setModel(model.getModelName());

    return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options);
  }

  public AnthropicChatModel createAnthropicChatModel(
      AnthropicChatModelSpec model) throws ModelException {
    if (modelProperties.getAnthropic() == null
        || modelProperties.getAnthropic().getChat() == null) {
      throw new ModelException("No Anthropic chat configuration found");
    }

    val options = modelProperties.getAnthropic().getChat().get(model);
    if (options == null) {
      throw new ModelException("No chat model configuration found for " + model);
    }
    options.setModel(model.getModelName());

    return AnthropicChatModel.builder()
        .anthropicApi(anthropicApi)
        .defaultOptions(options)
        .build();
  }
}