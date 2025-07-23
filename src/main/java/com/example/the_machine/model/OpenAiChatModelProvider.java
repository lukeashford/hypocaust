package com.example.the_machine.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * OpenAI Chat Model Provider implementation.
 */
@Component
@Profile("openai")
@Slf4j
public class OpenAiChatModelProvider implements ChatModelProvider {

  private final ChatModel chatModel;

  public OpenAiChatModelProvider(
      @Value("${spring.ai.openai.api-key}") String openAiApiKey) {

    val debugListener = new ChatModelListener() {
      @Override
      public void onRequest(ChatModelRequestContext ctx) {
        System.out.println("⏳ PROMPT:\n" + ctx.chatRequest().messages());
      }

      @Override
      public void onResponse(ChatModelResponseContext ctx) {
        System.out.println("✅ RAW REPLY:\n" + ctx.chatResponse().aiMessage());
      }

      @Override
      public void onError(ChatModelErrorContext ctx) {
        log.error(ctx.error().getMessage());
      }
    };

    this.chatModel = OpenAiChatModel.builder()
        .apiKey(openAiApiKey)
        .modelName("gpt-4o-mini")      // cheap but smart enough
        .temperature(0.2)              // keep it factual
        .logRequests(true)             // log full request JSON
        .logResponses(true)            // log full response JSON
        .listeners(List.of(debugListener))
        .build();
  }

  @Override
  public ChatModel getChatModel() {
    return chatModel;
  }
}