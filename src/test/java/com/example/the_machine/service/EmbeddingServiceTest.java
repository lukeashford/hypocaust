package com.example.the_machine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

/**
 * Unit tests for EmbeddingService functionality. Tests embedding generation with proper model
 * interaction and error handling.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

  @Mock
  private ModelRegistry modelRegistry;

  @Mock
  private OpenAiEmbeddingModel embeddingModel;

  @Mock
  private EmbeddingResponse embeddingResponse;

  @Mock
  private Embedding embedding;

  private EmbeddingService embeddingService;

  private final float[] testEmbedding = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

  @BeforeEach
  void setUp() {
    embeddingService = new EmbeddingService(modelRegistry);
  }

  @Test
  void testGenerateEmbedding_Success() {
    // Given
    String inputText = "Test text for embedding";
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenReturn(embeddingModel);
    when(embeddingModel.embedForResponse(anyList()))
        .thenReturn(embeddingResponse);
    when(embeddingResponse.getResult())
        .thenReturn(embedding);
    when(embedding.getOutput())
        .thenReturn(testEmbedding);

    // When
    float[] result = embeddingService.generateEmbedding(inputText);

    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo(testEmbedding)
        .hasSize(5);

    verify(modelRegistry).get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL);
    verify(embeddingModel).embedForResponse(List.of(inputText));
    verify(embeddingResponse).getResult();
    verify(embedding).getOutput();
  }

  @Test
  void testGenerateEmbedding_WithEmptyString() {
    // Given
    String inputText = "";
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenReturn(embeddingModel);
    when(embeddingModel.embedForResponse(anyList()))
        .thenReturn(embeddingResponse);
    when(embeddingResponse.getResult())
        .thenReturn(embedding);
    when(embedding.getOutput())
        .thenReturn(testEmbedding);

    // When
    float[] result = embeddingService.generateEmbedding(inputText);

    // Then
    assertThat(result).isEqualTo(testEmbedding);
    verify(embeddingModel).embedForResponse(List.of(""));
  }

  @Test
  void testGenerateEmbedding_WithLongText() {
    // Given
    String inputText = "Tool: complex-operator - This is a very long description that includes multiple sentences and various details about the operator functionality. It processes input data and transforms it using advanced algorithms. | Inputs: data, config, parameters | Outputs: result, metadata, status";
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenReturn(embeddingModel);
    when(embeddingModel.embedForResponse(anyList()))
        .thenReturn(embeddingResponse);
    when(embeddingResponse.getResult())
        .thenReturn(embedding);
    when(embedding.getOutput())
        .thenReturn(testEmbedding);

    // When
    float[] result = embeddingService.generateEmbedding(inputText);

    // Then
    assertThat(result).isEqualTo(testEmbedding);
    verify(embeddingModel).embedForResponse(List.of(inputText));
  }

  @Test
  void testGenerateEmbedding_ModelRegistryThrowsException() {
    // Given
    String inputText = "Test text";
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenThrow(new RuntimeException("Model not available"));

    // When & Then
    assertThatThrownBy(() -> embeddingService.generateEmbedding(inputText))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Model not available");
  }

  @Test
  void testGenerateEmbedding_EmbeddingModelThrowsException() {
    // Given
    String inputText = "Test text";
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenReturn(embeddingModel);
    when(embeddingModel.embedForResponse(anyList()))
        .thenThrow(new RuntimeException("Embedding generation failed"));

    // When & Then
    assertThatThrownBy(() -> embeddingService.generateEmbedding(inputText))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Embedding generation failed");
  }

  @Test
  void testGenerateEmbedding_WithNullInput() {
    // Given
    String inputText = null;
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenReturn(embeddingModel);

    // When & Then
    assertThatThrownBy(() -> embeddingService.generateEmbedding(inputText))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testGenerateEmbedding_UsesCorrectModel() {
    // Given
    String inputText = "Test text";
    when(modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL))
        .thenReturn(embeddingModel);
    when(embeddingModel.embedForResponse(anyList()))
        .thenReturn(embeddingResponse);
    when(embeddingResponse.getResult())
        .thenReturn(embedding);
    when(embedding.getOutput())
        .thenReturn(testEmbedding);

    // When
    embeddingService.generateEmbedding(inputText);

    // Then
    verify(modelRegistry).get(eq(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL));
  }
}