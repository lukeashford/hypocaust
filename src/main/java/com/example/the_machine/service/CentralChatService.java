package com.example.the_machine.service;

import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralChatService {

  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;

  private ChatModel orchestrationModel;

  @PostConstruct
  private void initializeOrchestrationModel() {
    final var orchestrationConfig = modelProperties.getOrchestration();
    if (orchestrationConfig == null || orchestrationConfig.getModelName() == null) {
      throw new IllegalStateException(
          "Orchestration model not configured. Please set app.llm.orchestration.model-name");
    }

    final var modelName = orchestrationConfig.getModelName();
    try {
      orchestrationModel = modelRegistry.get(modelName);
      log.info("Initialized orchestration model: {}", modelName);
    } catch (Exception e) {
      throw new IllegalStateException(
          String.format("Orchestration model '%s' not found. Available models: %s",
              modelName, modelRegistry.listAvailableModels()), e);
    }
  }

  public String processChatMessage(UUID threadId, UUID messageId) {
    return "placeholder";
  }


}
