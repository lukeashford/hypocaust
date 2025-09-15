package com.example.the_machine.service;

import com.example.the_machine.domain.event.MessageCompletedEvent;
import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.repo.MessageRepository;
import com.example.the_machine.service.events.EventService;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralChatService {

  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;
  private final MessageRepository messageRepository;
  private final EventService eventService;

  private ChatClient chatClient;

  @PostConstruct
  private void initializeOrchestrationModel() {
    final var orchestrationConfig = modelProperties.getOrchestration();
    if (orchestrationConfig == null || orchestrationConfig.getModelName() == null) {
      throw new IllegalStateException(
          "Orchestration model not configured. Please set app.llm.orchestration.model-name");
    }

    final var modelName = orchestrationConfig.getModelName();
    try {
      chatClient = ChatClient.create(modelRegistry.get(modelName));
      log.info("Initialized orchestration model: {}", modelName);
    } catch (Exception e) {
      throw new IllegalStateException(
          String.format("Orchestration model '%s' not found. Available models: %s",
              modelName, modelRegistry.listAvailableModels()), e);
    }
  }

  @Async
  public void processChatMessage(UUID threadId, UUID messageId) {
    final var message = messageRepository.findById(messageId).orElseThrow();
    log.info("Processing chat message for thread: {}, messageId: {}", threadId, messageId);

    final var response = chatClient.prompt(message.getContent()).call().content();
    log.info("LLM response for thread: {}, messageId: {}: {}", threadId, messageId, response);
    eventService.publish(new MessageCompletedEvent(threadId, messageId, response));
  }

}
