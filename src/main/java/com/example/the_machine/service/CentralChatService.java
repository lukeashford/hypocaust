package com.example.the_machine.service;

import com.example.the_machine.models.ModelProperties;
import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.tool.TaskSchedulingTool;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralChatService {

  private final ModelRegistry modelRegistry;
  private final ModelProperties modelProperties;
  private final TaskSchedulingTool taskSchedulingTool;

  private ChatClient chatClient;

  private static final SystemMessage SYSTEM_MESSAGE = new SystemMessage("""
      You are the front-desk chat for a general “everything-agent.” You operate in three modes: (1) conversation, (2) light clarification, (3) dispatch to backend.
      Your prime rule: only tell the backend what the user already knows and said. Do not invent details or defaults.
      The backend tool accepts one string. When you detect action intent, compose a Task String by restating the user’s request verbatim/condensed, including only explicitly stated elements: intent, object, deliverable, constraints/tone/timeframe, references (links/files), and delegation tokens like “surprise me.” Exclude anything unspecified.
      Dispatch rule: If you can write a sensible Task String from what was said, call the tool immediately with that string.
      Blocking question rule: Ask at most one short, necessary question only when the task would be undefined without a single missing choice or resource. Otherwise, do not ask; dispatch.
      After dispatch, display a brief confirmation and the exact Task String; offer Edit/Rerun/Cancel.
      Mirror the user’s tone. Be concise. No timelines or promises. Stay out of the way.
      """);

  @PostConstruct
  private void initializeChatClient() {
    final var modelName = modelProperties.getOrchestrationModelName();
    if (modelName == null) {
      throw new IllegalStateException(
          "Orchestration model not configured. Please set app.llm.orchestration.model-name");
    }

    try {
      chatClient = ChatClient.builder(modelRegistry.get(modelName))
          .build();
      log.info("Initialized orchestration model: {}", modelName);
    } catch (Exception e) {
      throw new IllegalStateException(
          String.format("Orchestration model '%s' not found. Available models: %s",
              modelName, modelRegistry.listAvailableModels()), e);
    }
  }

  public Flux<ChatResponse> streamChatCompletion(
      ChatCompletionRequest request,
      String librechatConversationId
  ) {
    return chatClient.prompt(convertOpenAiToSpringAiMessages(request))
        .tools(taskSchedulingTool)
        .toolContext(Map.of("librechatConversationId", librechatConversationId))
        .stream()
        .chatResponse();
  }

  public ChatResponse chatCompletion(
      ChatCompletionRequest request,
      String librechatConversationId
  ) {
    return chatClient.prompt(convertOpenAiToSpringAiMessages(request))
        .tools(taskSchedulingTool)
        .toolContext(Map.of("librechatConversationId", librechatConversationId))
        .call()
        .chatResponse();
  }

  private Prompt convertOpenAiToSpringAiMessages(ChatCompletionRequest request) {
    final List<Message> messages = Stream.concat(
            Stream.of(SYSTEM_MESSAGE),
            request.messages().stream()
                .map(msg -> switch (msg.role()) {
                  case SYSTEM -> new SystemMessage(msg.content());
                  case ASSISTANT -> new AssistantMessage(msg.content());
                  default -> new UserMessage(msg.content());
                }))
        .collect(Collectors.toUnmodifiableList());

    return new Prompt(messages);
  }
}
