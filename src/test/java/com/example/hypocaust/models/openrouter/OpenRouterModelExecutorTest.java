package com.example.hypocaust.models.openrouter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenRouterModelExecutorTest {

  private OpenRouterClient openRouterClient;
  private OpenRouterModelExecutor executor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    ModelRegistry modelRegistry = mock(ModelRegistry.class);
    objectMapper = new ObjectMapper();
    openRouterClient = mock(OpenRouterClient.class);
    executor = new OpenRouterModelExecutor(modelRegistry, objectMapper, openRouterClient);
  }

  @Test
  void platform_returnsOpenRouter() {
    assertThat(executor.platform()).isEqualTo(Platform.OPENROUTER);
  }

  @Test
  void execute_combinesOwnerAndModelId() {
    var input = objectMapper.createObjectNode().put("prompt", "Write a poem");
    var expectedOutput = objectMapper.createObjectNode();
    when(openRouterClient.chatCompletion(eq("meta-llama/llama-4-maverick"), any()))
        .thenReturn(expectedOutput);

    var result = executor.execute("meta-llama", "llama-4-maverick", input);

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
      assertThat(executor.extractOutputUrl(node)).isEqualTo("Once upon a time...");
    }

    @Test
    void unknownShape_fallsBackToToString() throws Exception {
      var node = objectMapper.readTree("{\"data\": 123}");
      assertThat(executor.extractOutputUrl(node)).isEqualTo("{\"data\":123}");
    }
  }
}
