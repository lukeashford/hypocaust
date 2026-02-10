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

class ArtifactNameGeneratorServiceTest {

  private ModelRegistry modelRegistry;
  private ArtifactNameGeneratorService service;
  private ChatClient chatClient;
  private ChatClient.Builder chatClientBuilder;

  @BeforeEach
  void setUp() {
    modelRegistry = mock(ModelRegistry.class);
    chatClient = mock(ChatClient.class);
    chatClientBuilder = mock(ChatClient.Builder.class);

    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(null);
    service = new ArtifactNameGeneratorService(modelRegistry);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRetryWhenLlmReturnsExistingName() {
    Set<String> existingNames = Set.of("taken_name");
    String description = "A new artifact";

    try (MockedStatic<ChatClient> mockedChatClient = mockStatic(ChatClient.class)) {
      mockedChatClient.when(() -> ChatClient.builder(any())).thenReturn(chatClientBuilder);
      when(chatClientBuilder.build()).thenReturn(chatClient);

      ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
      ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

      when(chatClient.prompt()).thenReturn(promptSpec);
      when(promptSpec.system(anyString())).thenReturn(promptSpec);
      when(promptSpec.user(anyString())).thenReturn(promptSpec);
      when(promptSpec.call()).thenReturn(responseSpec);

      // First call returns "taken_name", second returns "new_name"
      when(responseSpec.content()).thenReturn("taken_name", "new_name");

      String result = service.generateUniqueName(description, existingNames);

      assertThat(result).isEqualTo("new_name");
      verify(promptSpec, times(2)).call();
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFallbackToNumberAppendingIfAllRetriesFail() {
    Set<String> existingNames = Set.of("taken_name");
    String description = "taken_name";

    try (MockedStatic<ChatClient> mockedChatClient = mockStatic(ChatClient.class)) {
      mockedChatClient.when(() -> ChatClient.builder(any())).thenReturn(chatClientBuilder);
      when(chatClientBuilder.build()).thenReturn(chatClient);

      ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
      ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

      when(chatClient.prompt()).thenReturn(promptSpec);
      when(promptSpec.system(anyString())).thenReturn(promptSpec);
      when(promptSpec.user(anyString())).thenReturn(promptSpec);
      when(promptSpec.call()).thenReturn(responseSpec);

      // All calls return "taken_name"
      when(responseSpec.content()).thenReturn("taken_name");

      String result = service.generateUniqueName(description, existingNames);

      assertThat(result).isEqualTo("taken_name_2");
      verify(promptSpec, times(3)).call();
    }
  }

  @Test
  void shouldSanitizeNames() {
    String result = service.generateUniqueName("My Artifact Name!", Set.of());
    assertThat(result).isEqualTo("my_artifact_name");
  }

  @Test
  void shouldHandleLlmFailureGracefully() {
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenThrow(
        new RuntimeException("LLM Down"));

    String result = service.generateUniqueName("fallback name", Set.of());
    assertThat(result).isEqualTo("fallback_name");
  }
}
