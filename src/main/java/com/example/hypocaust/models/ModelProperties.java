package com.example.hypocaust.models;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.models.enums.OpenAiChatModelSpec;
import com.example.hypocaust.models.enums.OpenAiEmbeddingModelSpec;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.llm")
public class ModelProperties {

  private OpenAi openAi;
  private Anthropic anthropic;
  private String orchestrationModelName;
  private String decomposingModelName;

  @Data
  public static class OpenAi {

    private String apiKey;
    private Map<OpenAiChatModelSpec, OpenAiChatOptions> chat;
    private Map<OpenAiEmbeddingModelSpec, OpenAiEmbeddingOptions> embedding;
  }

  @Data
  public static class Anthropic {

    private String apiKey;
    private Map<AnthropicChatModelSpec, AnthropicChatOptions> chat;
  }

}
