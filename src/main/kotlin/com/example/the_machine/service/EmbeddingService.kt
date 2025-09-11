package com.example.the_machine.service

import com.example.the_machine.models.ModelRegistry
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.stereotype.Component

/**
 * Service responsible for generating text embeddings using configured AI models. Provides
 * centralized embedding functionality with proper model management.
 */
@Component
class EmbeddingService(
  private val modelRegistry: ModelRegistry
) {

  /**
   * Gets the configured embedding model from the registry.
   *
   * @return the OpenAI embedding model instance
   */
  private fun getEmbeddingModel(): OpenAiEmbeddingModel {
    return modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL)
  }

  /**
   * Generates embedding for the given text using the configured embedding model.
   *
   * @param text the input text to generate embedding for
   * @return float array representing the text embedding
   */
  fun generateEmbedding(text: String): FloatArray {
    val embeddingModel = getEmbeddingModel()
    val response = embeddingModel.embedForResponse(listOf(text))
    return response.result.output
  }
}