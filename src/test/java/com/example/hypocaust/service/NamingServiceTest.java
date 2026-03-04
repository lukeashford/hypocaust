package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamingServiceTest {

  private ChatService chatService;
  private NamingService service;

  @BeforeEach
  void setUp() {
    chatService = mock(ChatService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    service = new NamingService(chatService, objectMapper);
  }

  @Test
  void generateArtifactNaming_callsLlmAndParsesJson() {
    stubLlmResponse(
        "```json\n{\"title\": \"Cat Art\", \"name\": \"cat_art\", \"description\": \"A cute cat\"}\n```");
    NamingService.ArtifactNaming naming = service.generateArtifactNaming(
        "Make a cat", "the image", ArtifactKind.IMAGE, Set.of(), Set.of());

    assertThat(naming.title()).isEqualTo("Cat Art");
    assertThat(naming.name()).isEqualTo("cat_art");
    assertThat(naming.description()).isEqualTo("A cute cat");
  }

  @Test
  void generateArtifactNaming_handlesTakenNamesAndTitles() {
    stubLlmResponse(
        "{\"title\": \"Cat Art\", \"name\": \"cat_art\", \"description\": \"A cute cat\"}");
    NamingService.ArtifactNaming naming = service.generateArtifactNaming(
        "Make a cat", "the image", ArtifactKind.IMAGE, Set.of("cat_art"), Set.of("Cat Art"));

    assertThat(naming.title()).isEqualTo("Cat Art_2");
    assertThat(naming.name()).isEqualTo("cat_art_2");
  }

  @Test
  void generateArtifactNaming_fallsBackOnLlmFailure() {
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString()))
        .thenThrow(new RuntimeException("LLM error"));

    NamingService.ArtifactNaming naming = service.generateArtifactNaming(
        "Make a cat", "the image", ArtifactKind.IMAGE, Set.of(), Set.of());

    assertThat(naming.title()).isEqualTo("Make a cat");
    assertThat(naming.name()).isEqualTo("make_a_cat");
    assertThat(naming.description()).isEqualTo("Generated image");
  }

  private void stubLlmResponse(String responseText) {
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString()))
        .thenReturn(responseText);
  }
}
