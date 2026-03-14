package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.ModelSpecEnum;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ModelRegistry modelRegistry;

  @Retryable(
      retryFor = Exception.class,
      exceptionExpression = "@retryMatcher.isTransient(#root)",
      backoff = @Backoff(delay = 1000, multiplier = 2, random = true)
  )
  @SuppressWarnings("unchecked")
  public <T> T call(ModelSpecEnum spec, String system, String user, Class<T> responseType) {
    CallResponseSpec response = ChatClient.builder(modelRegistry.get(spec.getModelName())).build()
        .prompt()
        .system(system)
        .user(user)
        .call();
    return responseType == String.class
        ? (T) response.content()
        : response.entity(responseType);
  }

  @Retryable(
      retryFor = Exception.class,
      exceptionExpression = "@retryMatcher.isTransient(#root)",
      backoff = @Backoff(delay = 1000, multiplier = 2, random = true)
  )
  @SuppressWarnings("unchecked")
  public <T> T callWithImage(ModelSpecEnum spec, String system, byte[] image, String mimeType,
      Class<T> responseType) {
    var media = new Media(MimeType.valueOf(mimeType), image);
    var userMessage = new UserMessage("Analyze this image.", List.of(media));
    var systemMessage = new SystemMessage(system);
    var prompt = new Prompt(List.of(systemMessage, userMessage));
    CallResponseSpec response = ChatClient.builder(modelRegistry.get(spec.getModelName())).build()
        .prompt(prompt)
        .call();
    return responseType == String.class
        ? (T) response.content()
        : response.entity(responseType);
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
