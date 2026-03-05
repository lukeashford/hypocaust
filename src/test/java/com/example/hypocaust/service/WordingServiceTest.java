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

class WordingServiceTest {

  private ChatService chatService;
  private WordingService wordingService;

  @BeforeEach
  void setUp() {
    chatService = mock(ChatService.class);
    wordingService = new WordingService(chatService, new ObjectMapper());
  }

  @Test
  void generateModelRequirement_success() {
    // GIVEN
    String jsonResponse = """
        {
          "inputs": ["IMAGE"],
          "tier": "powerful",
          "searchString": "cinematic animation"
        }
        """;
    mockChatResponse(jsonResponse);

    // WHEN
    var result = wordingService.generateModelRequirement("task", Set.of(ArtifactKind.VIDEO));

    // THEN
    assertThat(result.inputs()).containsExactly(ArtifactKind.IMAGE);
    assertThat(result.outputs()).containsExactly(ArtifactKind.VIDEO);
    assertThat(result.tier()).isEqualTo("powerful");
    assertThat(result.searchString()).isEqualTo("cinematic animation");
  }

  private void mockChatResponse(String content) {
    when(chatService.call(any(AnthropicChatModelSpec.class), anyString(), anyString()))
        .thenReturn(content);
  }
}
