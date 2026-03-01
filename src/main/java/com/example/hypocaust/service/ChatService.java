package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.ModelSpecEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ModelRegistry modelRegistry;

  @Retryable(
      retryFor = Exception.class,
      exceptionExpression = "@retryMatcher.isTransient(#root)",
      backoff = @Backoff(delay = 1000, multiplier = 2, random = true)
  )
  public String call(ModelSpecEnum spec, String system, String user) {
    return ChatClient.builder(modelRegistry.get(spec.getModelName())).build()
        .prompt()
        .system(system)
        .user(user)
        .call()
        .content();
  }

  @Retryable(
      retryFor = Exception.class,
      exceptionExpression = "@retryMatcher.isTransient(#root)",
      backoff = @Backoff(delay = 1000, multiplier = 2, random = true)
  )
  public String call(String modelName, String system, String user, Object... tools) {
    return ChatClient.builder(modelRegistry.get(modelName))
        .defaultTools(tools)
        .build()
        .prompt()
        .system(system)
        .user(user)
        .call()
        .content();
  }
}
