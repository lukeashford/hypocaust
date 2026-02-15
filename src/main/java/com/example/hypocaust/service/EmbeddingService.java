package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.OpenAiEmbeddingModelSpec;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.Embedding;
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
    final var embeddingModel = getEmbeddingModel();
    final var response = embeddingModel.embedForResponse(List.of(text));
    return response.getResult().getOutput();
  }

  /**
   * Generates embeddings for the given list of texts using the configured embedding model.
   *
   * @param texts the list of input texts to generate embeddings for
   * @return list of float arrays representing the text embeddings
   */
  public List<float[]> generateEmbeddings(List<String> texts) {
    final var embeddingModel = getEmbeddingModel();
    final var response = embeddingModel.embedForResponse(texts);
    return response.getResults().stream().map(Embedding::getOutput).toList();
  }
}