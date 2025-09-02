package com.example.the_machine.models;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Unified configuration for all LLM providers. Replaces individual provider properties classes to
 * eliminate duplication.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmConfiguration {

  private Map<String, ProviderConfig> providers = Map.of();

  @Data
  public static class ProviderConfig {

    private String apiKey;
    private Map<String, ModelProps> models = Map.of();
  }
}