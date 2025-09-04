package com.example.the_machine.models;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRegistry {

  private final ModelConfig config;
  private final List<ModelPlatform> platforms;

  private Map<String, ChatModel> models;

  @PostConstruct
  public void init() {
    models = config.getPlatforms().entrySet().stream()
        .flatMap(platformEntry -> {
          val platformName = platformEntry.getKey();
          val platformConfig = platformEntry.getValue();
          val platform = findPlatform(platformName);
          val builder = platform.getGenericBuilder();

          return platformConfig.getModels().entrySet().stream()
              .map(modelEntry -> {
                val modelName = modelEntry.getKey();
                val modelProps = modelEntry.getValue();
//                val builder = findBuilderOrDefault(platform, modelName);
                ChatModel chatModel;
                try {
                  chatModel = builder.from(modelProps.getParams());
                } catch (NoSuchMethodException | InvocationTargetException |
                         IllegalAccessException e) {
                  throw new RuntimeException(e);
                }

                return Map.entry(platformName + ":" + modelName, chatModel);
              });
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Log initial model registry state
    log.info("Model registry initialized with {} models: {}", models.size(), models.keySet());
//    if (!models.containsKey(config.getDefaultModel())) {
//      throw new IllegalStateException("Default model not found: " + config.getDefaultModel());
//    }
  }

  public ChatModel get(String name) {
    return models.get(name);
  }

  public ChatModel getDefault() {
    return get(config.getDefaultModel());
  }

  public Set<String> listAvailableModels() {
    return models.keySet();
  }

  private ModelPlatform findPlatform(String name) {
    return platforms.stream()
        .filter(p -> p.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Platform not found: " + name));
  }

  private ModelBuilder findBuilderOrDefault(ModelPlatform platform, String modelName) {
    return platform.getBuilderOrDefault(modelName);
  }
}
