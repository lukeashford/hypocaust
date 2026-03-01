package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamingServiceTest {

  private ChatService chatService;
  private NamingService service;

  @BeforeEach
  void setUp() {
    chatService = mock(ChatService.class);
    service = new NamingService(chatService);
  }

  @Test
  void generateArtifactName_withPreferredAvailable_usesPreferred() {
    String name = service.generateArtifactName("source", Set.of("other"), "preferred");
    assertThat(name).isEqualTo("preferred");
  }

  @Test
  void generateArtifactName_withPreferredTaken_callsLlm() {
    stubLlmResponse("llm_generated_name");
    String name = service.generateArtifactName("source", Set.of("preferred"), "preferred");
    assertThat(name).isEqualTo("llm_generated_name");
  }

  @Test
  void generateArtifactName_sanitizesLlmResponse() {
    stubLlmResponse("  My Artifact Name!  ");
    String name = service.generateArtifactName("source", Set.of());
    assertThat(name).isEqualTo("my_artifact_name");
  }

  @Test
  void generateArtifactName_truncatesLongNames() {
    stubLlmResponse("this_is_a_very_long_name_that_exceeds_thirty_characters");
    String name = service.generateArtifactName("source", Set.of());
    assertThat(name).hasSizeLessThanOrEqualTo(30);
    assertThat(name).isEqualTo("this_is_a_very_long_name_that");
  }

  @Test
  void generateArtifactName_fallsBackToCounterOnLlmFailure() {
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString()))
        .thenThrow(new RuntimeException("LLM error"));
    String name = service.generateArtifactName("source", Set.of("source"));
    assertThat(name).isEqualTo("source_2");
  }

  @Test
  void generateExecutionName_usesHigherLimit() {
    stubLlmResponse("this_is_a_long_execution_name_that_is_around_fifty_chars");
    String name = service.generateExecutionName("source", Set.of());
    assertThat(name).hasSizeLessThanOrEqualTo(50);
    assertThat(name).isEqualTo("this_is_a_long_execution_name_that_is_around_fifty");
  }

  private void stubLlmResponse(String responseText) {
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString()))
        .thenReturn(responseText);
  }
}
