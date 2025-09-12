package com.example.the_machine.service

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.domain.AssistantEntity
import com.example.the_machine.domain.EventType
import com.example.the_machine.domain.RunEntity
import com.example.the_machine.dto.CreateRunRequestDto
import com.example.the_machine.dto.EventEnvelopeDto
import com.example.the_machine.dto.RunDto
import com.example.the_machine.repo.RunRepository
import com.example.the_machine.repo.ThreadRepository
import com.example.the_machine.service.events.EventPublisher
import com.example.the_machine.service.events.RunCreatedEvent
import com.example.the_machine.service.mapping.RunMapper
import com.example.the_machine.service.mapping.ThreadMapper
import kotlinx.serialization.json.encodeToJsonElement
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutorService

private val log = KotlinLogging.logger {}

/**
 * Service for managing run creation and execution.
 */
@Service
class RunService(
  private val runRepository: RunRepository,
  private val threadRepository: ThreadRepository,
  private val runMapper: RunMapper,
  private val threadMapper: ThreadMapper,
  private val eventPublisher: EventPublisher,
  private val runExecutorService: ExecutorService,
  private val assistantEngine: AssistantEngine,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val artifactService: ArtifactService
) {

  /**
   * Creates a new run based on the request.
   *
   * @param request the create run request
   * @return the created run DTO
   */
  @Transactional
  fun createRun(request: CreateRunRequestDto): RunDto {
    log.info { "Creating run for thread: ${request.threadId}" }

    // Load thread
    val thread = threadRepository.findById(request.threadId)
      .orElseThrow { IllegalArgumentException("Thread not found: ${request.threadId}") }

    // Note: MessageService would handle user message creation if available
    // For now, we just log that input was provided
    if (request.input != null) {
      log.debug { "User input provided for run" }
    }

    // Create run entity directly
    val run = RunEntity(
      threadId = request.threadId,
      assistantId = request.assistantId ?: AssistantEntity.DEFAULT_ASSISTANT_ID,
      status = RunEntity.Status.QUEUED,
      kind = RunEntity.Kind.FULL,
    )

    val savedRun = runRepository.save<RunEntity>(run)
    val runDto = runMapper.toDto(savedRun)

    // Log run creation event
    val envelope = EventEnvelopeDto(
      EventType.RUN_CREATED,
      thread.id,
      savedRun.id,
      null,
      KotlinSerializationConfig.staticJson.encodeToJsonElement(runDto)
    )
    eventPublisher.publishAndStore(thread.id, envelope, null)

    // Publish event for async execution after transaction commit
    applicationEventPublisher.publishEvent(RunCreatedEvent(savedRun.id, thread.id))

    log.info { "Run created and queued: ${savedRun.id}" }
    return runDto
  }

  /**
   * Handles run created events after transaction commit to ensure async execution starts only when
   * the run is persisted.
   *
   * @param event the run created event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleRunCreated(event: RunCreatedEvent) {
    log.debug { "Handling run created event for run: ${event.runId}" }
    runExecutorService.submit { executeRun(event.runId, event.threadId) }
  }

  /**
   * Executes a run in the background.
   *
   * @param runId the ID of the run to execute
   * @param threadId the ID of the thread associated with the run
   */
  private fun executeRun(runId: UUID, threadId: UUID) {
    log.info { "Starting execution of run: $runId" }

    try {
      // Always fetch fresh managed entity by ID
      val currentRun = runRepository.findById(runId)
        .orElseThrow { IllegalStateException("Run not found: $runId") }

      // Transition to RUNNING
      val updatedRun = currentRun.copy(
        status = RunEntity.Status.RUNNING,
        startedAt = Instant.now()
      )
      val run = runRepository.save(updatedRun)

      val envelope = EventEnvelopeDto(
        EventType.RUN_UPDATED,
        threadId,
        runId,
        null,
        KotlinSerializationConfig.staticJson.encodeToJsonElement(runMapper.toDto(run))
      )
      eventPublisher.publishAndStore(threadId, envelope, null)

      // Create run context with IDs
      val context = RunContext(
        threadId,
        runId,
        runRepository,
        threadRepository,
        runMapper,
        threadMapper,
        eventPublisher,
        artifactService,
        RunPolicy.defaultPolicy()
      )

      // Simple planner logic
      val executionType = determineExecutionType(run)

      when (executionType) {
        ExecutionType.PLAN_CLARIFY -> assistantEngine.executePlanAskClarify(context)
        ExecutionType.FULL_PIPELINE -> assistantEngine.executeFullPipeline(context)
        ExecutionType.PARTIAL_REVISION -> assistantEngine.executePartialRevision(
          context,
          run.reason ?: ""
        )
      }

      log.info { "Run execution completed: $runId" }

    } catch (e: Exception) {
      log.error(e) { "Run execution failed: $runId" }

      // Always fetch fresh managed entity by ID for error handling
      val currentRun = runRepository.findById(runId)
        .orElseThrow { IllegalStateException("Run not found: $runId") }

      val failedRun = currentRun.copy(
        status = RunEntity.Status.FAILED,
        error = e.message,
        completedAt = Instant.now()
      )
      val run = runRepository.save(failedRun)

      val errorEnvelope = EventEnvelopeDto(
        EventType.RUN_UPDATED,
        threadId,
        runId,
        null,
        KotlinSerializationConfig.staticJson.encodeToJsonElement(runMapper.toDto(run))
      )
      eventPublisher.publishAndStore(threadId, errorEnvelope, null)
    }
  }

  /**
   * Determines the execution type based on run and thread state. Simplified logic for MVP without
   * complex message checking.
   *
   * @param run the current run
   * @return the execution type
   */
  private fun determineExecutionType(run: RunEntity): ExecutionType {
    // Check if reason indicates partial revision
    val reason = run.reason
    if (reason != null && reason.startsWith("user_revision:")) {
      return ExecutionType.PARTIAL_REVISION
    }

    // Simple logic for MVP: assume FULL kind means full pipeline
    // In a real implementation, this would check message history and context
    if (run.kind == RunEntity.Kind.FULL) {
      // For now, always run full pipeline for FULL kind
      // This could be enhanced later with proper message context checking
      return ExecutionType.FULL_PIPELINE
    }

    // For new conversations, start with plan+maybe clarify
    return ExecutionType.PLAN_CLARIFY
  }

  private enum class ExecutionType {
    PLAN_CLARIFY,
    FULL_PIPELINE,
    PARTIAL_REVISION
  }
}