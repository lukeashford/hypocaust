package com.example.the_machine.service;

import com.example.the_machine.domain.EventType;
import com.example.the_machine.domain.RunEntity;
import com.example.the_machine.domain.RunFactory;
import com.example.the_machine.domain.ThreadEntity;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.EventEnvelopeDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.events.EventPublisher;
import com.example.the_machine.service.events.RunCreatedEvent;
import com.example.the_machine.service.mapping.RunMapper;
import com.example.the_machine.service.mapping.ThreadMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Service for managing run creation and execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunService {

  private final RunRepository runRepository;
  private final ThreadRepository threadRepository;
  private final RunMapper runMapper;
  private final ThreadMapper threadMapper;
  private final EventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  private final ExecutorService runExecutorService;
  private final AssistantEngine assistantEngine;
  private final RunFactory runFactory;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final ArtifactService artifactService;

  /**
   * Creates a new run based on the request.
   *
   * @param request the create run request
   * @return the created run DTO
   */
  @Transactional
  public RunDto createRun(CreateRunRequestDto request) {
    log.info("Creating run for thread: {}", request.threadId());

    // Load thread
    val thread = threadRepository.findById(request.threadId())
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + request.threadId()));

    // Note: MessageService would handle user message creation if available
    // For now, we just log that input was provided
    if (request.input() != null) {
      log.debug("User input provided for run");
    }

    // Create run entity using factory
    val runId = runFactory.createAndSaveRun(request, request.threadId());
    val runDto = runMapper.toDto(runFactory.findManagedRun(runId));

    // Log run creation event
    val envelope = new EventEnvelopeDto(
        EventType.RUN_CREATED,
        thread.getId(),
        runId,
        null,
        objectMapper.valueToTree(runDto)
    );
    eventPublisher.publishAndStore(thread.getId(), envelope, null);

    // Publish event for async execution after transaction commit
    applicationEventPublisher.publishEvent(new RunCreatedEvent(runId, thread.getId()));

    log.info("Run created and queued: {}", runId);
    return runDto;
  }

  /**
   * Handles run created events after transaction commit to ensure async execution starts only when
   * the run is persisted.
   *
   * @param event the run created event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRunCreated(RunCreatedEvent event) {
    log.debug("Handling run created event for run: {}", event.runId());
    runExecutorService.submit(() -> executeRun(event.runId(), event.threadId()));
  }

  /**
   * Executes a run in the background.
   *
   * @param runId the ID of the run to execute
   * @param threadId the ID of the thread associated with the run
   */
  private void executeRun(UUID runId, UUID threadId) {
    log.info("Starting execution of run: {}", runId);

    try {
      // Fetch managed entities
      val run = runFactory.findManagedRun(runId);
      val thread = threadRepository.findById(threadId)
          .orElseThrow(() -> new IllegalStateException("Thread not found: " + threadId));

      // Transition to RUNNING
      run.setStatus(RunEntity.Status.RUNNING);
      run.setStartedAt(Instant.now());
      runRepository.save(run);

      val envelope = new EventEnvelopeDto(
          EventType.RUN_UPDATED,
          threadId,
          runId,
          null,
          objectMapper.valueToTree(runMapper.toDto(run))
      );
      eventPublisher.publishAndStore(threadId, envelope, null);

      // Create run context with IDs
      val context = new RunContext(
          threadId,
          runId,
          runRepository,
          threadRepository,
          runMapper,
          threadMapper,
          eventPublisher,
          objectMapper,
          artifactService
      );

      // Simple planner logic
      val executionType = determineExecutionType(run, thread);

      switch (executionType) {
        case PLAN_CLARIFY -> assistantEngine.executePlanAskClarify(context);
        case FULL_PIPELINE -> assistantEngine.executeFullPipeline(context);
        case PARTIAL_REVISION -> assistantEngine.executePartialRevision(context, run.getReason());
      }

      log.info("Run execution completed: {}", runId);

    } catch (Exception e) {
      log.error("Run execution failed: {}", runId, e);

      // Fetch managed run for error update
      val run = runFactory.findManagedRun(runId);
      run.setStatus(RunEntity.Status.FAILED);
      run.setError(e.getMessage());
      run.setCompletedAt(Instant.now());
      runRepository.save(run);

      val errorEnvelope = new EventEnvelopeDto(
          EventType.RUN_UPDATED,
          threadId,
          runId,
          null,
          objectMapper.valueToTree(runMapper.toDto(run))
      );
      eventPublisher.publishAndStore(threadId, errorEnvelope, null);
    }
  }

  /**
   * Determines the execution type based on run and thread state. Simplified logic for MVP without
   * complex message checking.
   *
   * @param run the current run
   * @param thread the thread
   * @return the execution type
   */
  private ExecutionType determineExecutionType(RunEntity run, ThreadEntity thread) {
    // Check if reason indicates partial revision
    if (run.getReason() != null && run.getReason().startsWith("user_revision:")) {
      return ExecutionType.PARTIAL_REVISION;
    }

    // Simple logic for MVP: assume FULL kind means full pipeline
    // In a real implementation, this would check message history and context
    if (run.getKind() == RunEntity.Kind.FULL) {
      // For now, always run full pipeline for FULL kind
      // This could be enhanced later with proper message context checking
      return ExecutionType.FULL_PIPELINE;
    }

    // For new conversations, start with plan+maybe clarify
    return ExecutionType.PLAN_CLARIFY;
  }

  private UUID getDefaultAssistantId() {
    // For now, return a fixed UUID. In real implementation, this would come from configuration
    return UUID.fromString("00000000-0000-0000-0000-000000000001");
  }

  private enum ExecutionType {
    PLAN_CLARIFY,
    FULL_PIPELINE,
    PARTIAL_REVISION
  }
}