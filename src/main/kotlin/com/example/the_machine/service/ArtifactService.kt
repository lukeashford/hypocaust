package com.example.the_machine.service

import com.example.the_machine.common.IdGenerator
import com.example.the_machine.domain.ArtifactEntity
import com.example.the_machine.repo.ArtifactRepository
import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Service for handling artifact creation and management using direct type-safe methods.
 */
@Service
class ArtifactService(
  private val artifactRepository: ArtifactRepository,
  private val idGenerator: IdGenerator
) {

  // Direct artifact creation methods

  /**
   * Creates an artifact directly with metadata - no JSON parsing needed.
   *
   * @param threadId the thread ID
   * @param runId the run ID
   * @param kind the artifact kind
   * @param stage the workflow stage
   * @param title the artifact title
   * @param mime the MIME type (optional)
   * @return the created artifact entity
   */
  @Transactional
  fun createArtifact(
    threadId: UUID,
    runId: UUID,
    kind: ArtifactEntity.Kind,
    stage: ArtifactEntity.Stage,
    title: String,
    mime: String?
  ): ArtifactEntity {
    log.debug { "Creating artifact: kind=$kind, stage=$stage, title=$title" }

    val artifact = ArtifactEntity(
      id = idGenerator.newId(),
      threadId = threadId,
      runId = runId,
      kind = kind,
      stage = stage,
      status = ArtifactEntity.Status.PENDING,
      title = title,
      mime = mime,
      createdAt = Instant.now()
    )

    val saved = artifactRepository.save(artifact)
    log.info { "Artifact created: ${saved.id} for thread: $threadId" }
    return saved
  }

  /**
   * Updates artifact with structured content (JSON).
   *
   * @param artifactId the artifact ID
   * @param content the content to set
   */
  @Transactional
  fun setContent(artifactId: UUID, content: JsonNode) {
    val artifact = artifactRepository.findById(artifactId)
      .orElseThrow { IllegalArgumentException("Artifact not found: $artifactId") }

    val updatedArtifact = artifact.copy(
      content = content,
      status = ArtifactEntity.Status.DONE
    )
    artifactRepository.save(updatedArtifact)

    log.debug { "Content set for artifact: $artifactId" }
  }

  /**
   * Updates artifact with file storage information.
   *
   * @param artifactId the artifact ID
   * @param storageKey the storage location
   * @param mime the MIME type
   */
  @Transactional
  fun setStorageKey(artifactId: UUID, storageKey: String, mime: String) {
    val artifact = artifactRepository.findById(artifactId)
      .orElseThrow { IllegalArgumentException("Artifact not found: $artifactId") }

    val updatedArtifact = artifact.copy(
      storageKey = storageKey,
      mime = mime,
      status = ArtifactEntity.Status.DONE
    )
    artifactRepository.save(updatedArtifact)

    log.debug { "Storage key set for artifact: $artifactId -> $storageKey" }
  }

  /**
   * Updates artifact metadata.
   *
   * @param artifactId the artifact ID
   * @param metadata the metadata to set
   */
  @Transactional
  fun setMetadata(artifactId: UUID, metadata: JsonNode) {
    val artifact = artifactRepository.findById(artifactId)
      .orElseThrow { IllegalArgumentException("Artifact not found: $artifactId") }

    val updatedArtifact = artifact.copy(metadata = metadata)
    artifactRepository.save(updatedArtifact)

    log.debug { "Metadata set for artifact: $artifactId" }
  }
}