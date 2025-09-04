package com.example.the_machine.models;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class ModelConfig {

  private String defaultModel;

  private Map<String, ModelPlatformConfig> platforms = Map.of();

  @Data
  public static class ModelPlatformConfig {

    private String apiKey;
    private Map<String, ModelProps> models = Map.of();

    @Data
    public static class ModelProps {

      private Map<String, String> params;
    }
  }
}
