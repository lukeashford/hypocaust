package com.example.the_machine.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.the_machine.models.enums.AnthropicChatModelSpec;
import com.example.the_machine.models.enums.OpenAiChatModelSpec;
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class ModelRegistryTest {

  @Mock
  private ApplicationContext applicationContext;

  @InjectMocks
  private ModelRegistry modelRegistry;

  @Test
  void testTypeSafeEnumBasedAccess() {
    // Mock the application context to return specific model instances
    val mockOpenAiChatModel = org.mockito.Mockito.mock(
        org.springframework.ai.openai.OpenAiChatModel.class);
    val mockOpenAiEmbeddingModel = org.mockito.Mockito.mock(
        org.springframework.ai.openai.OpenAiEmbeddingModel.class);
    val mockAnthropicChatModel = org.mockito.Mockito.mock(
        org.springframework.ai.anthropic.AnthropicChatModel.class);

    when(applicationContext.getBean("gpt-4o", org.springframework.ai.openai.OpenAiChatModel.class))
        .thenReturn(mockOpenAiChatModel);
    when(applicationContext.getBean("text-embedding-3-small",
        org.springframework.ai.openai.OpenAiEmbeddingModel.class))
        .thenReturn(mockOpenAiEmbeddingModel);
    when(applicationContext.getBean("claude-sonnet-4-latest",
        org.springframework.ai.anthropic.AnthropicChatModel.class))
        .thenReturn(mockAnthropicChatModel);

    // Test OpenAI chat model access
    val retrievedOpenAiChat = modelRegistry.get(OpenAiChatModelSpec.GPT_4O);
    assertThat(retrievedOpenAiChat).isEqualTo(mockOpenAiChatModel);

    // Test OpenAI embedding model access
    val retrievedOpenAiEmbedding = modelRegistry.get(
        OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL);
    assertThat(retrievedOpenAiEmbedding).isEqualTo(mockOpenAiEmbeddingModel);

    // Test Anthropic chat model access
    val retrievedAnthropicChat = modelRegistry.get(AnthropicChatModelSpec.CLAUDE_SONNET_4_LATEST);
    assertThat(retrievedAnthropicChat).isEqualTo(mockAnthropicChatModel);
  }

  @Test
  void testStringBasedAccess() {
    // Mock the application context to return a generic ChatModel
    val mockChatModel = org.mockito.Mockito.mock(ChatModel.class);
    when(applicationContext.getBean("gpt-4o", ChatModel.class)).thenReturn(mockChatModel);

    // Test string-based access
    val retrievedModel = modelRegistry.get("gpt-4o");
    assertThat(retrievedModel).isEqualTo(mockChatModel);
  }
}