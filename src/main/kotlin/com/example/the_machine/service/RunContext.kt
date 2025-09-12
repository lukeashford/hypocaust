package com.example.the_machine.service

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.domain.EventType
import com.example.the_machine.domain.RunEntity.Status
import com.example.the_machine.dto.EventEnvelopeDto
import com.example.the_machine.repo.RunRepository
import com.example.the_machine.repo.ThreadRepository
import com.example.the_machine.service.events.EventPublisher
import com.example.the_machine.service.mapping.RunMapper
import com.example.the_machine.service.mapping.ThreadMapper
import kotlinx.serialization.json.JsonElement
import mu.KotlinLogging
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Context class containing all necessary components and helper methods for run execution. Stores
 * only IDs to prevent detached entity issues, always fetching managed entities when needed.
 */
data class RunContext(
  val threadId: UUID,
  val runId: UUID,
  val runRepository: RunRepository,
  val threadRepository: ThreadRepository,
  val runMapper: RunMapper,
  val threadMapper: ThreadMapper,
  val eventPublisher: EventPublisher,
  val artifactService: ArtifactService,
  val policy: RunPolicy
) {

  // Helper methods

  /**
   * Checks if the current operation is within budget limits.
   *
   * @throws IllegalStateException if budget limits are exceeded
   */
  fun checkBudgets() {
    // In a real implementation, this would check actual resource usage
    // For now, we'll implement a simple check
    logger.debug(
      "Checking budgets - maxCostUsd: {}, maxDepth: {}, maxNodes: {}",
      policy.maxCostUsd, policy.maxDepth, policy.maxNodes
    )
    // Budget checking logic would go here - for demo purposes, always passes
  }

  /**
   * Converts an object to JsonElement.
   *
   * @param data the object to convert
   * @return JsonElement representation
   */
  private fun toJsonElement(data: Any): JsonElement {
    return try {
      // Convert to JSON string first, then parse to JsonElement
      val jsonString = when (data) {
        is String -> "\"$data\""
        is Number -> data.toString()
        is Boolean -> data.toString()
        else -> "\"$data\""
      }
      KotlinSerializationConfig.staticJson.parseToJsonElement(jsonString)
    } catch (e: Exception) {
      logger.error("Failed to convert object to JsonElement: {}", data.javaClass.simpleName, e)
      throw RuntimeException("JSON serialization failed", e)
    }
  }

  /**
   * Emits a message event (delta or completed).
   *
   * @param messageId the message ID
   * @param messageData the message data
   * @param completed whether the message is completed
   */
  fun emitMessage(messageId: UUID, messageData: Any, completed: Boolean) {
    val eventType = if (completed) EventType.MESSAGE_COMPLETED else EventType.MESSAGE_DELTA
    val envelope = EventEnvelopeDto(
      eventType,
      threadId,
      runId,
      messageId,
      toJsonElement(messageData)
    )
    eventPublisher.publishAndStore(threadId, envelope, null)
  }

  /**
   * Creates an artifact directly - no events needed.
   *
   * @param kind the artifact kind
   * @param stage the workflow stage
   * @param title the artifact title
   * @param mime the MIME type (optional)
   * @return the created artifact entity
   */
  fun createArtifact(
    kind: ArtifactEntity.Kind,
    stage: ArtifactEntity.Stage,
    title: String,
    mime: String?
  ): ArtifactEntity {
    return artifactService.createArtifact(threadId, runId, kind, stage, title, mime)
  }

  /**
   * Updates artifact with generated content.
   *
   * @param artifactId the artifact ID
   * @param content the content to set
   */
  fun setArtifactContent(artifactId: UUID, content: JsonElement) {
    artifactService.setContent(artifactId, content)
  }

  /**
   * Updates artifact with file storage.
   *
   * @param artifactId the artifact ID
   * @param storageKey the storage location
   * @param mime the MIME type
   */
  fun setArtifactFile(artifactId: UUID, storageKey: String, mime: String) {
    artifactService.setStorageKey(artifactId, storageKey, mime)
  }

  /**
   * Updates artifact metadata.
   *
   * @param artifactId the artifact ID
   * @param metadata the metadata to set
   */
  fun setArtifactMetadata(artifactId: UUID, metadata: JsonElement) {
    artifactService.setMetadata(artifactId, metadata)
  }

  /**
   * Updates the run status and emits corresponding events.
   *
   * @param status the new status
   */
  fun updateRunStatus(status: Status) {
    // Fetch the current managed entity to ensure it's managed
    val currentRun = runRepository.findById(runId)
      .orElseThrow { IllegalStateException("Run not found: $runId") }

    // Create updated entity with new values
    val now = Instant.now()
    val updatedRun = currentRun.copy(
      status = status,
      startedAt = if (status == Status.RUNNING && currentRun.startedAt == null) now else currentRun.startedAt,
      completedAt = if ((status == Status.COMPLETED || status == Status.FAILED) && currentRun.completedAt == null) now else currentRun.completedAt
    )

    // Save the updated entity
    val managedRun = runRepository.save(updatedRun)

    val envelope = EventEnvelopeDto(
      EventType.RUN_UPDATED,
      threadId,
      managedRun.id,
      null,
      toJsonElement(runMapper.toDto(managedRun))
    )
    eventPublisher.publishAndStore(threadId, envelope, null)
  }
}