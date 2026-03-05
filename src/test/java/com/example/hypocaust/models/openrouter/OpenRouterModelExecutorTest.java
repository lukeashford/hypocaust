package com.example.hypocaust.models.openrouter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

class OpenRouterModelExecutorTest {

  private OpenRouterClient openRouterClient;
  private OpenRouterModelExecutor executor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = mock(ModelRegistry.class);
    ChatService chatService = mock(ChatService.class);
    objectMapper = new ObjectMapper();
    openRouterClient = mock(OpenRouterClient.class);
    ArtifactResolver artifactResolver = mock(ArtifactResolver.class);
    executor = new OpenRouterModelExecutor(modelRegistry, objectMapper, chatService,
        new RetryTemplate(), null, artifactResolver, openRouterClient);
  }

  @Test
  void platform_returnsOpenRouter() {
    assertThat(executor.platform()).isEqualTo(Platform.OPENROUTER);
  }

  @Test
  void doExecute_combinesOwnerAndModelId() {
    var input = objectMapper.createObjectNode().put("prompt", "Write a poem");
    var expectedOutput = objectMapper.createObjectNode();
    when(openRouterClient.chatCompletion(eq("meta-llama/llama-4-maverick"), any()))
        .thenReturn(expectedOutput);

    var result = executor.doExecute("meta-llama", "llama-4-maverick", input);

    verify(openRouterClient).chatCompletion("meta-llama/llama-4-maverick", input);
    assertThat(result).isEqualTo(expectedOutput);
  }

  @Nested
  class ExtractOutputUrl {

    @Test
    void choicesArray_returnsMessageContent() throws Exception {
      var node = objectMapper.readTree("""
          {"choices": [{"message": {"role": "assistant", "content": "Once upon a time..."}}]}
          """);
      assertThat(executor.extractOutputs(node).values()).extracting(ExtractedOutput::content)
          .containsExactly("Once upon a time...");
    }

    @Test
    void unknownShape_fallsBackToToString() throws Exception {
      var node = objectMapper.readTree("{\"data\": 123}");
      assertThat(executor.extractOutputs(node).values()).extracting(ExtractedOutput::content)
          .containsExactly("{\"data\":123}");
    }
  }
}
