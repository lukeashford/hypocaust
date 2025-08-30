package com.example.the_machine.service;

import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.domain.EventType;
import com.example.the_machine.domain.RunEntity.Status;
import com.example.the_machine.dto.EventEnvelopeDto;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.events.EventPublisher;
import com.example.the_machine.service.mapping.RunMapper;
import com.example.the_machine.service.mapping.ThreadMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Context class containing all necessary components and helper methods for run execution. Stores
 * only IDs to prevent detached entity issues, always fetching managed entities when needed.
 */
@Slf4j
public record RunContext(UUID threadId, UUID runId, RunRepository runRepository,
                         ThreadRepository threadRepository, RunMapper runMapper,
                         ThreadMapper threadMapper, EventPublisher eventPublisher,
                         ObjectMapper objectMapper, ArtifactService artifactService,
                         RunPolicy policy) {

  // Helper methods

  /**
   * Checks if the current operation is within budget limits.
   *
   * @throws IllegalStateException if budget limits are exceeded
   */
  public void checkBudgets() {
    // In a real implementation, this would check actual resource usage
    // For now, we'll implement a simple check
    log.debug("Checking budgets - maxCostUsd: {}, maxDepth: {}, maxNodes: {}",
        policy.maxCostUsd(), policy.maxDepth(), policy.maxNodes());
    // Budget checking logic would go here - for demo purposes, always passes
  }

  /**
   * Converts an object to JsonNode.
   *
   * @param data the object to convert
   * @return JsonNode representation
   */
  private JsonNode toJsonNode(Object data) {
    try {
      return objectMapper.valueToTree(data);
    } catch (Exception e) {
      log.error("Failed to convert object to JsonNode: {}", data.getClass().getSimpleName(), e);
      throw new RuntimeException("JSON serialization failed", e);
    }
  }

  /**
   * Emits a message event (delta or completed).
   *
   * @param messageId the message ID
   * @param messageData the message data
   * @param completed whether the message is completed
   */
  public void emitMessage(UUID messageId, Object messageData, boolean completed) {
    val eventType = completed ? EventType.MESSAGE_COMPLETED : EventType.MESSAGE_DELTA;
    val envelope = new EventEnvelopeDto(
        eventType,
        threadId,
        runId,
        messageId,
        toJsonNode(messageData)
    );
    eventPublisher.publishAndStore(threadId, envelope, null);
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
  public ArtifactEntity createArtifact(
      ArtifactEntity.Kind kind,
      ArtifactEntity.Stage stage,
      String title,
      String mime
  ) {
    return artifactService.createArtifact(threadId, runId, kind, stage, title, mime);
  }

  /**
   * Updates artifact with generated content.
   *
   * @param artifactId the artifact ID
   * @param content the content to set
   */
  public void setArtifactContent(UUID artifactId, JsonNode content) {
    artifactService.setContent(artifactId, content);
  }

  /**
   * Updates artifact with file storage.
   *
   * @param artifactId the artifact ID
   * @param storageKey the storage location
   * @param mime the MIME type
   */
  public void setArtifactFile(UUID artifactId, String storageKey, String mime) {
    artifactService.setStorageKey(artifactId, storageKey, mime);
  }

  /**
   * Updates artifact metadata.
   *
   * @param artifactId the artifact ID
   * @param metadata the metadata to set
   */
  public void setArtifactMetadata(UUID artifactId, JsonNode metadata) {
    artifactService.setMetadata(artifactId, metadata);
  }

  /**
   * Updates the run status and emits corresponding events.
   *
   * @param status the new status
   */
  public void updateRunStatus(Status status) {
    // Fetch the current managed entity to ensure it's managed
    val managedRun = runRepository.findById(runId)
        .orElseThrow(() -> new IllegalStateException("Run not found: " + runId));

    // Update the managed entity
    managedRun.setStatus(status);

    if (status == Status.RUNNING && managedRun.getStartedAt() == null) {
      managedRun.setStartedAt(Instant.now());
    } else if ((status == Status.COMPLETED || status == Status.FAILED)
        && managedRun.getCompletedAt() == null) {
      managedRun.setCompletedAt(Instant.now());
    }

    // Save the managed entity (no merge issues)
    runRepository.save(managedRun);

    val envelope = new EventEnvelopeDto(
        EventType.RUN_UPDATED,
        threadId,
        managedRun.getId(),
        null,
        toJsonNode(runMapper.toDto(managedRun))
    );
    eventPublisher.publishAndStore(threadId, envelope, null);
  }
}