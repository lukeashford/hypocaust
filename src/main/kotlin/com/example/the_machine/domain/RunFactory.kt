package com.example.the_machine.domain

import com.example.the_machine.dto.CreateRunRequestDto
import com.example.the_machine.repo.RunRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * Factory for creating RunEntity instances with controlled access and validation. This factory
 * ensures that RunEntity instances are properly created and persisted, preventing detached entity
 * issues by managing the entity lifecycle.
 */
@Component
class RunFactory(
  private val runRepository: RunRepository
) {

  /**
   * Creates and saves a new RunEntity based on the request. Returns the UUID of the created run to
   * avoid passing detached entities.
   *
   * @param request the create run request
   * @param threadId the thread ID for the run
   * @return the UUID of the created and saved run
   */
  fun createAndSaveRun(request: CreateRunRequestDto, threadId: UUID): UUID {
    val run = RunEntity(
      id = UUID.randomUUID(),
      threadId = threadId,
      assistantId = request.assistantId ?: AssistantEntity.DEFAULT_ASSISTANT_ID,
      status = RunEntity.Status.QUEUED,
      kind = RunEntity.Kind.FULL
    )

    val savedRun = runRepository.save(run)
    return savedRun.id ?: throw IllegalStateException("Saved run has null ID")
  }

  /**
   * Finds a managed RunEntity by ID, ensuring it's attached to the current session.
   *
   * @param runId the run ID to find
   * @return the managed RunEntity
   * @throws IllegalStateException if the run is not found
   */
  fun findManagedRun(runId: UUID): RunEntity =
    runRepository.findById(runId)
      .orElseThrow { IllegalStateException("Run not found: $runId") }
}