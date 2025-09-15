package com.example.the_machine.service;

import com.example.the_machine.domain.event.MessageCompletedEvent;
import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.repo.MessageRepository;
import com.example.the_machine.service.events.EventService;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
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
    log.info("Processing chat message for thread: {}, messageId: {}", threadId, messageId);

    final var allMessages = messageRepository.findByThreadIdOrderByCreatedAt(threadId);

    final int maxTurns = 10;
    final var history = allMessages.stream().limit(maxTurns).toList();

    List<Message> msgs = history.stream().map(m ->
        switch (m.getAuthor()) {
          case ASSISTANT -> new AssistantMessage(m.getContent());
          case SYSTEM -> new SystemMessage(m.getContent());
          default -> new UserMessage(m.getContent());
        }
    ).collect(Collectors.toUnmodifiableList());

    final var response = chatClient.prompt(new Prompt(msgs)).call().content();

    log.info("LLM response for thread: {}, messageId: {}: {}", threadId, messageId, response);
    eventService.publish(new MessageCompletedEvent(threadId, messageId, response));
  }

}
