package com.example.the_machine.operator.registry

import com.example.the_machine.domain.OperatorEmbedding
import com.example.the_machine.operator.Operator
import com.example.the_machine.operator.ToolSpec
import com.example.the_machine.repo.OperatorEmbeddingRepository
import com.example.the_machine.service.EmbeddingService
import com.example.the_machine.service.HashCalculationService
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Semantic search-enabled operator registry that uses vector embeddings for intelligent operator
 * discovery and matching. Discovers operators via ServiceLoader, generates embeddings for their
 * descriptions, and provides semantic search capabilities.
 */
@Component
class SemanticSearchOperatorRegistry(
  private val embeddingRepository: OperatorEmbeddingRepository,
  private val embeddingService: EmbeddingService,
  private val hashCalculationService: HashCalculationService
) : OperatorRegistry {

  companion object {

    private val log = KotlinLogging.logger {}
  }

  private val operatorsByName = ConcurrentHashMap<String, Operator>()

  @PostConstruct
  fun initialize() {
    discoverAndIndexOperators()
  }

  /**
   * Discovers operators using ServiceLoader and generates embeddings for new operators.
   */
  private fun discoverAndIndexOperators() {
    log.info("Discovering operators and generating embeddings...")

    val serviceLoader = ServiceLoader.load(Operator::class.java)
    val newEmbeddings = mutableListOf<OperatorEmbedding>()

    serviceLoader.forEach { operator ->
      try {
        val spec = operator.spec()
        val operatorName = spec.name

        // Single source of truth - cache only operators
        operatorsByName[operatorName] = operator

        // Use ToolSpec's existing description method + context
        val descriptionText = createEmbeddingText(spec)
        val existingEmbedding = embeddingRepository.findByOperatorName(operatorName)
        val textHash = hashCalculationService.calculateSha256Hash(descriptionText)

        if (existingEmbedding.isEmpty || existingEmbedding.get().hash != textHash) {
          val embedding = embeddingService.generateEmbedding(descriptionText)

          newEmbeddings.add(
            OperatorEmbedding(
              operatorName = operatorName,
              embedding = embedding,
              hash = textHash
            )
          )
        }
      } catch (e: Exception) {
        log.error("Failed to process operator {}: {}", operator.javaClass.simpleName, e.message, e)
      }
    }

    if (newEmbeddings.isNotEmpty()) {
      embeddingRepository.saveAll(newEmbeddings)
      log.info("Generated embeddings for {} operators", newEmbeddings.size)
    }

    log.info("Registry initialization complete. Indexed {} operators", operatorsByName.size)
  }

  /**
   * Creates embedding text from ToolSpec using existing methods.
   */
  private fun createEmbeddingText(spec: ToolSpec): String {
    val text = StringBuilder().apply {
      append("Tool: ").append(spec.name)

      // ToolSpec already has getDescription()!
      spec.description?.takeIf { it.isNotBlank() }?.let { description ->
        append(" - ").append(description)
      }

      // Add I/O context for better matching
      if (spec.getInputKeys().isNotEmpty()) {
        append(" | Inputs: ").append(spec.getInputKeys().joinToString(", "))
      }

      if (spec.getOutputKeys().isNotEmpty()) {
        append(" | Outputs: ").append(spec.getOutputKeys().joinToString(", "))
      }
    }

    return text.toString()
  }

  /**
   * Performs semantic search for operators based on task description.
   *
   * @param taskDescription the task description to search for
   * @return list of tool specs ordered by similarity
   */
  override fun searchByTask(taskDescription: String): List<ToolSpec> {
    val maxResults = 3

    return try {
      val queryEmbedding = embeddingService.generateEmbedding(taskDescription)
      val pageable = PageRequest.of(0, maxResults)

      val results = embeddingRepository.findTopByEmbeddingSimilarity(queryEmbedding, pageable)

      results.mapNotNull { embedding ->
        operatorsByName[embedding.operatorName]?.spec() // Single source of truth!
      }
    } catch (e: Exception) {
      log.error("Semantic search failed for query: {}", taskDescription, e)
      emptyList()
    }
  }

  override fun get(name: String): Operator? {
    return operatorsByName[name]
  }

  override fun size(): Int {
    return operatorsByName.size
  }

  // Package-private methods for testing
  internal fun getOperatorsByName(): Map<String, Operator> {
    return operatorsByName
  }

  internal fun createEmbeddingTextForTesting(spec: ToolSpec): String {
    return createEmbeddingText(spec)
  }
}