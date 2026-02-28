package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WordingServiceTest {

  private ChatService chatService;
  private WordingService wordingService;

  @BeforeEach
  void setUp() {
    chatService = mock(ChatService.class);
    wordingService = new WordingService(chatService, new ObjectMapper());
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

  @Test
  void generateModelRequirement_success() {
    // GIVEN
    String jsonResponse = """
        {
          "inputs": ["IMAGE"],
          "output": "VIDEO",
          "tier": "powerful",
          "searchString": "cinematic animation"
        }
        """;
    mockChatResponse(jsonResponse);

    // WHEN
    var result = wordingService.generateModelRequirement("task",
        com.example.hypocaust.domain.ArtifactKind.VIDEO);

    // THEN
    assertThat(result.inputs()).containsExactly(com.example.hypocaust.domain.ArtifactKind.IMAGE);
    assertThat(result.output()).isEqualTo(com.example.hypocaust.domain.ArtifactKind.VIDEO);
    assertThat(result.tier()).isEqualTo("powerful");
    assertThat(result.searchString()).isEqualTo("cinematic animation");
  }

  private void mockChatResponse(String content) {
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString()))
        .thenReturn(content);
  }
}
