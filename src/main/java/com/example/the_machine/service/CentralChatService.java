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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralChatService {

  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;
  private final MessageRepository messageRepository;
  private final EventService eventService;
  private final ContextPackingService contextPackingService;

  private ChatClient chatClient;

  @PostConstruct
  private void initializeOrchestrationModel() {
    final var modelName = modelProperties.getOrchestrationModelName();
    if (modelName == null) {
      throw new IllegalStateException(
          "Orchestration model not configured. Please set app.llm.orchestration.model-name");
    }

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

    final var allMessages = messageRepository.findByThreadIdOrderByCreatedAtDesc(threadId);

    // Convert repository messages to Spring AI messages
    final List<Message> chronological = allMessages.stream().map(m -> switch (m.getAuthor()) {
      case ASSISTANT -> new AssistantMessage(m.getContent());
      case SYSTEM -> new SystemMessage(m.getContent());
      default -> new UserMessage(m.getContent());
    }).collect(Collectors.toUnmodifiableList());

    final var modelName = modelProperties.getOrchestrationModelName();

    // Extract system message if present, or create empty one
    final var systemMessage = new SystemMessage("You are a helpful assistant.");

    // Use token-aware context packing instead of fixed maxTurns
    final var packedMessages = contextPackingService.buildContext(
        modelName, systemMessage, chronological);

    final var response = chatClient.prompt(new Prompt(packedMessages)).call().content();

    log.info("LLM response for thread: {}, messageId: {}: {}", threadId, messageId, response);
    eventService.publish(new MessageCompletedEvent(threadId, messageId, response));
  }

  public Flux<ChatResponse> streamChatCompletion(ChatCompletionRequest request) {
    return chatClient.prompt(convertOpenAiToSpringAiMessages(request))
        .stream()
        .chatResponse();
  }

  public ChatResponse chatCompletion(ChatCompletionRequest request) {
    return chatClient.prompt(convertOpenAiToSpringAiMessages(request)).call().chatResponse();
  }

  private Prompt convertOpenAiToSpringAiMessages(ChatCompletionRequest request) {
    final List<Message> messages = request.messages().stream()
        .map(msg -> switch (msg.role()) {
          case SYSTEM -> new SystemMessage(msg.content());
          case ASSISTANT -> new AssistantMessage(msg.content());
          default -> new UserMessage(msg.content());
        })
        .collect(Collectors.toUnmodifiableList());

    return new Prompt(messages);
  }
}
