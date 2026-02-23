package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

class WordingServiceTest {

  private org.springframework.ai.anthropic.AnthropicChatModel chatModel;
  private WordingService wordingService;

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = mock(ModelRegistry.class);
    chatModel = mock(org.springframework.ai.anthropic.AnthropicChatModel.class);
    when(modelRegistry.get(any(com.example.hypocaust.models.enums.AnthropicChatModelSpec.class)))
        .thenReturn(chatModel);
    wordingService = new WordingService(modelRegistry);
  }

  @Test
  void generateTodoWording_truncatesAt50() {
    // GIVEN
    String longResponse = IntStream.range(0, 100).mapToObj(i -> "a").collect(Collectors.joining());
    mockChatResponse(longResponse);

    // WHEN
    String result = wordingService.generateTodoWording("task");

    // THEN
    assertThat(result).hasSize(50);
    assertThat(result).endsWith("...");
  }

  @Test
  void generateTodoWording_shortResponse_noTruncation() {
    // GIVEN
    String shortResponse = "Hello";
    mockChatResponse(shortResponse);

    // WHEN
    String result = wordingService.generateTodoWording("task");

    // THEN
    assertThat(result).isEqualTo("Hello");
  }

  @Test
  void generateArtifactDescription_truncatesAt100() {
    // GIVEN
    String longResponse = IntStream.range(0, 200).mapToObj(i -> "a").collect(Collectors.joining());
    mockChatResponse(longResponse);

    // WHEN
    String result = wordingService.generateArtifactDescription("source");

    // THEN
    assertThat(result).hasSize(100);
    assertThat(result).endsWith("...");
  }

  @Test
  void generateArtifactDescription_shortResponse_noTruncation() {
    // GIVEN
    String shortResponse = "Hello world description";
    mockChatResponse(shortResponse);

    // WHEN
    String result = wordingService.generateArtifactDescription("source");

    // THEN
    assertThat(result).isEqualTo("Hello world description");
  }

  private void mockChatResponse(String content) {
    ChatResponse response = mock(ChatResponse.class);
    Generation generation = mock(Generation.class);
    when(generation.getOutput()).thenReturn(
        new org.springframework.ai.chat.messages.AssistantMessage(content));
    when(response.getResult()).thenReturn(generation);
    when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(response);
  }
}
