package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class NamingServiceTest {

  private AnthropicChatModel chatModel;
  private NamingService service;

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = mock(ModelRegistry.class);
    chatModel = mock(AnthropicChatModel.class);
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(chatModel);
    service = new NamingService(modelRegistry);
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
    when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM error"));
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
    ChatResponse chatResponse = ChatResponse.builder()
        .generations(List.of(new Generation(new AssistantMessage(responseText))))
        .build();
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
  }
}
