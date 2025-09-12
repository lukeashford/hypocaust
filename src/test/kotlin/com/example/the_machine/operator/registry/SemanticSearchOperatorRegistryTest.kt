package com.example.the_machine.operator.registry

import com.example.the_machine.domain.OperatorEmbedding
import com.example.the_machine.operator.Operator
import com.example.the_machine.operator.ToolSpec
import com.example.the_machine.repo.OperatorEmbeddingRepository
import com.example.the_machine.service.EmbeddingService
import com.example.the_machine.service.HashCalculationService
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * Unit tests for SemanticSearchOperatorRegistry functionality including operator discovery,
 * embedding generation, and semantic search capabilities.
 */
@ExtendWith(MockKExtension::class)
class SemanticSearchOperatorRegistryTest {

  private val embeddingRepository: OperatorEmbeddingRepository = mockk()
  private val embeddingService: EmbeddingService = mockk()
  private val hashCalculationService: HashCalculationService = mockk()
  private val mockOperator: Operator = mockk(relaxed = true)
  private val mockToolSpec: ToolSpec = mockk(relaxed = true)

  private lateinit var registry: SemanticSearchOperatorRegistry

  private val testEmbedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
  private val operatorName = "test-operator"
  private val operatorDescription = "Test operator for unit testing"

  @BeforeEach
  fun setUp() {
    registry = SemanticSearchOperatorRegistry(
      embeddingRepository,
      embeddingService,
      hashCalculationService
    )

    // Setup common mock behavior using MockK's every blocks
    every { embeddingService.generateEmbedding(any<String>()) } returns testEmbedding
    every { hashCalculationService.calculateSha256Hash(any<String>()) } returns "mock-hash-12345"

    // Setup operator and tool spec mocks - using relaxed mocks so these are optional
    every { mockOperator.spec() } returns mockToolSpec
    every { mockToolSpec.name } returns operatorName
    every { mockToolSpec.description } returns operatorDescription
    every { mockToolSpec.getInputKeys() } returns setOf("input1", "input2")
    every { mockToolSpec.getOutputKeys() } returns setOf("output1")
  }

  @Test
  fun testGetOperatorByName() {
    // Given
    (registry.getOperatorsByName() as MutableMap)[operatorName] = mockOperator

    // When
    val result = registry.get(operatorName)

    // Then
    assertThat(result).isNotNull
    assertThat(result).isEqualTo(mockOperator)
  }

  @Test
  fun testGetOperatorByNameNotFound() {
    // When
    val result = registry.get("non-existent-operator")

    // Then
    assertThat(result).isNull()
  }

  @Test
  fun testSearchByTaskDescription() {
    // Given
    val taskDescription = "process some text"
    val operatorEmbedding = OperatorEmbedding(
      operatorName = operatorName,
      embedding = testEmbedding,
      hash = "test-hash",
    )

    (registry.getOperatorsByName() as MutableMap)[operatorName] = mockOperator

    every {
      embeddingRepository.findTopByEmbeddingSimilarity(
        any<FloatArray>(),
        any<Pageable>()
      )
    } returns listOf(operatorEmbedding)

    // When
    val results = registry.searchByTask(taskDescription)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results.first()).isEqualTo(mockToolSpec)
    verify { embeddingService.generateEmbedding(taskDescription) }
    verify { embeddingRepository.findTopByEmbeddingSimilarity(testEmbedding, PageRequest.of(0, 3)) }
  }

  @Test
  fun testSearchByTaskDescriptionWithMaxResults() {
    // Given
    val taskDescription = "process some text"
    val operatorEmbedding = OperatorEmbedding(
      operatorName = operatorName,
      embedding = testEmbedding,
      hash = "test-hash",
    )

    (registry.getOperatorsByName() as MutableMap)[operatorName] = mockOperator

    every {
      embeddingRepository.findTopByEmbeddingSimilarity(
        any<FloatArray>(),
        any<Pageable>()
      )
    } returns listOf(operatorEmbedding)

    // When
    val results = registry.searchByTask(taskDescription)

    // Then
    assertThat(results).hasSize(1)
    assertThat(results.first()).isEqualTo(mockToolSpec)
    verify { embeddingRepository.findTopByEmbeddingSimilarity(testEmbedding, PageRequest.of(0, 3)) }
  }

  @Test
  fun testSearchByTaskHandlesException() {
    // Given
    val taskDescription = "process some text"
    every { embeddingService.generateEmbedding(taskDescription) } throws RuntimeException("Embedding service unavailable")

    // When
    val results = registry.searchByTask(taskDescription)

    // Then
    assertThat(results).isEmpty()
  }

  @Test
  fun testCreateEmbeddingText() {
    // Given - using package-private method for testing
    val embeddingText = registry.createEmbeddingTextForTesting(mockToolSpec)

    // Then - check individual components since Set order is not guaranteed
    assertThat(embeddingText)
      .contains("Tool: $operatorName")
      .contains(operatorDescription)
      .contains("Inputs:")
      .contains("input1")
      .contains("input2")
      .contains("Outputs: output1")
  }

  @Test
  fun testInitializeWithExistingEmbedding() {
    // Given
    every { embeddingRepository.findByOperatorName(operatorName) } returns Optional.of(
      OperatorEmbedding(
        operatorName = operatorName,
        embedding = testEmbedding,
        hash = "test-hash",
      )
    )
  }
}