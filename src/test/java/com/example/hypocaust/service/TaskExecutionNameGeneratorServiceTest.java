package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ai.chat.client.ChatClient;

class TaskExecutionNameGeneratorServiceTest {

  private ModelRegistry modelRegistry;
  private TaskExecutionNameGeneratorService service;
  private ChatClient chatClient;
  private ChatClient.Builder chatClientBuilder;

  @BeforeEach
  void setUp() {
    modelRegistry = mock(ModelRegistry.class);
    chatClient = mock(ChatClient.class);
    chatClientBuilder = mock(ChatClient.Builder.class);

    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(null);
    service = new TaskExecutionNameGeneratorService(modelRegistry);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldGenerateNameFromTask() {
    try (MockedStatic<ChatClient> mockedChatClient = mockStatic(ChatClient.class)) {
      mockedChatClient.when(() -> ChatClient.builder(any())).thenReturn(chatClientBuilder);
      when(chatClientBuilder.build()).thenReturn(chatClient);

      ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
      ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

      when(chatClient.prompt()).thenReturn(promptSpec);
      when(promptSpec.system(anyString())).thenReturn(promptSpec);
      when(promptSpec.user(anyString())).thenReturn(promptSpec);
      when(promptSpec.call()).thenReturn(responseSpec);
      when(responseSpec.content()).thenReturn("hero_portrait_created");

      String result = service.generateUniqueName("Create the hero portrait", Set.of());

      assertThat(result).isEqualTo("hero_portrait_created");
      verify(promptSpec, times(1)).call();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRetryWhenLlmReturnsExistingName() {
    Set<String> existingNames = Set.of("taken_name");

    try (MockedStatic<ChatClient> mockedChatClient = mockStatic(ChatClient.class)) {
      mockedChatClient.when(() -> ChatClient.builder(any())).thenReturn(chatClientBuilder);
      when(chatClientBuilder.build()).thenReturn(chatClient);

      ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
      ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

      when(chatClient.prompt()).thenReturn(promptSpec);
      when(promptSpec.system(anyString())).thenReturn(promptSpec);
      when(promptSpec.user(anyString())).thenReturn(promptSpec);
      when(promptSpec.call()).thenReturn(responseSpec);
      when(responseSpec.content()).thenReturn("taken_name", "unique_name");

      String result = service.generateUniqueName("Some task", existingNames);

      assertThat(result).isEqualTo("unique_name");
      verify(promptSpec, times(2)).call();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFallbackWithCounterIfAllRetriesFail() {
    Set<String> existingNames = Set.of("some_task");

    try (MockedStatic<ChatClient> mockedChatClient = mockStatic(ChatClient.class)) {
      mockedChatClient.when(() -> ChatClient.builder(any())).thenReturn(chatClientBuilder);
      when(chatClientBuilder.build()).thenReturn(chatClient);

      ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
      ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

      when(chatClient.prompt()).thenReturn(promptSpec);
      when(promptSpec.system(anyString())).thenReturn(promptSpec);
      when(promptSpec.user(anyString())).thenReturn(promptSpec);
      when(promptSpec.call()).thenReturn(responseSpec);
      when(responseSpec.content()).thenReturn("some_task");

      String result = service.generateUniqueName("some task", existingNames);

      assertThat(result).isEqualTo("some_task_2");
      verify(promptSpec, times(3)).call();
    }
  }

  @Test
  void shouldFallbackGracefullyWhenLlmFails() {
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("LLM Down"));

    String result = service.generateUniqueName("Create character designs", Set.of());

    assertThat(result).isEqualTo("create_character_designs");
  }

  @Test
  void shouldHandleNullTask() {
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("LLM Down"));

    String result = service.generateUniqueName(null, Set.of());

    assertThat(result).isEqualTo("task");
  }

  @Test
  void shouldTruncateLongFallbackNames() {
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("LLM Down"));

    String longTask = "a".repeat(100);
    String result = service.generateUniqueName(longTask, Set.of());

    assertThat(result.length()).isLessThanOrEqualTo(50);
  }
}
