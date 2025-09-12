package com.example.the_machine.service

import com.example.the_machine.models.ModelRegistry
import com.example.the_machine.models.enums.OpenAiEmbeddingModelSpec
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.openai.OpenAiEmbeddingModel

/**
 * Unit tests for EmbeddingService functionality. Tests embedding generation with proper model
 * interaction and error handling.
 */
@ExtendWith(MockKExtension::class)
class EmbeddingServiceTest {

  private val modelRegistry: ModelRegistry = mockk()
  private val embeddingModel: OpenAiEmbeddingModel = mockk()
  private val embeddingResponse: EmbeddingResponse = mockk()
  private val embedding: Embedding = mockk()
  private lateinit var embeddingService: EmbeddingService

  private val testEmbedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)

  @BeforeEach
  fun setUp() {
    embeddingService = EmbeddingService(modelRegistry)
  }

  @Test
  fun testGenerateEmbedding_Success() {
    // Given
    val inputText = "Test text for embedding"
    every { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) } returns embeddingModel
    every { embeddingModel.embedForResponse(any<List<String>>()) } returns embeddingResponse
    every { embeddingResponse.result } returns embedding
    every { embedding.output } returns testEmbedding

    // When
    val result = embeddingService.generateEmbedding(inputText)

    // Then
    assertThat(result)
      .isNotNull()
      .isEqualTo(testEmbedding)
      .hasSize(5)

    verify { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) }
    verify { embeddingModel.embedForResponse(listOf(inputText)) }
    verify { embeddingResponse.result }
    verify { embedding.output }
  }

  @Test
  fun testGenerateEmbedding_WithEmptyString() {
    // Given
    val inputText = ""
    every { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) } returns embeddingModel
    every { embeddingModel.embedForResponse(any<List<String>>()) } returns embeddingResponse
    every { embeddingResponse.result } returns embedding
    every { embedding.output } returns testEmbedding

    // When
    val result = embeddingService.generateEmbedding(inputText)

    // Then
    assertThat(result).isEqualTo(testEmbedding)
    verify { embeddingModel.embedForResponse(listOf("")) }
  }

  @Test
  fun testGenerateEmbedding_WithLongText() {
    // Given
    val inputText =
      "Tool: complex-operator - This is a very long description that includes multiple sentences and various details about the operator functionality. It processes input data and transforms it using advanced algorithms. | Inputs: data, config, parameters | Outputs: result, metadata, status"
    every { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) } returns embeddingModel
    every { embeddingModel.embedForResponse(any<List<String>>()) } returns embeddingResponse
    every { embeddingResponse.result } returns embedding
    every { embedding.output } returns testEmbedding

    // When
    val result = embeddingService.generateEmbedding(inputText)

    // Then
    assertThat(result).isEqualTo(testEmbedding)
    verify { embeddingModel.embedForResponse(listOf(inputText)) }
  }

  @Test
  fun testGenerateEmbedding_ModelRegistryThrowsException() {
    // Given
    val inputText = "Test text"
    every { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) } throws RuntimeException(
      "Model not available"
    )

    // When & Then
    assertThatThrownBy { embeddingService.generateEmbedding(inputText) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("Model not available")
  }

  @Test
  fun testGenerateEmbedding_EmbeddingModelThrowsException() {
    // Given
    val inputText = "Test text"
    every { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) } returns embeddingModel
    every { embeddingModel.embedForResponse(any<List<String>>()) } throws RuntimeException("Embedding generation failed")

    // When & Then
    assertThatThrownBy { embeddingService.generateEmbedding(inputText) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("Embedding generation failed")
  }

  @Test
  fun testGenerateEmbedding_UsesCorrectModel() {
    // Given
    val inputText = "Test text"
    every { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) } returns embeddingModel
    every { embeddingModel.embedForResponse(any<List<String>>()) } returns embeddingResponse
    every { embeddingResponse.result } returns embedding
    every { embedding.output } returns testEmbedding

    // When
    embeddingService.generateEmbedding(inputText)

    // Then
    verify { modelRegistry.get(OpenAiEmbeddingModelSpec.TEXT_EMBEDDING_3_SMALL) }
  }
}