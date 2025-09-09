package com.example.the_machine.service;

import com.example.the_machine.models.ModelRegistry;
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Service responsible for generating text embeddings using configured AI models. Provides
 * centralized embedding functionality with proper model management.
 */
@Component
@RequiredArgsConstructor
public class EmbeddingService {

  private final ModelRegistry modelRegistry;

  /**
   * Gets the configured embedding model from the registry.
   *
   * @return the OpenAI embedding model instance
   */
  private OpenAiEmbeddingModel getEmbeddingModel() {
    return modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL);
  }

  /**
   * Generates embedding for the given text using the configured embedding model.
   *
   * @param text the input text to generate embedding for
   * @return float array representing the text embedding
   */
  public float[] generateEmbedding(String text) {
    val embeddingModel = getEmbeddingModel();
    val response = embeddingModel.embedForResponse(List.of(text));
    return response.getResult().getOutput();
  }
}