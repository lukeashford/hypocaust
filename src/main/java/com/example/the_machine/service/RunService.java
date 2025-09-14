package com.example.the_machine.service;

import com.example.the_machine.db.RunEntity;
import com.example.the_machine.domain.event.RunCreatedEvent;
import com.example.the_machine.domain.event.RunUpdatedEvent;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.mapper.RunMapper;
import com.example.the_machine.repo.RunRepository;
import com.example.the_machine.repo.ThreadRepository;
import com.example.the_machine.service.events.EventService;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final EventService eventService;
  private final ExecutorService runExecutorService;

  /**
   * Creates a new run based on the request.
   *
   * @param request the create run request
   * @return the created run DTO
   */
  @Transactional
  public RunDto scheduleRun(CreateRunRequestDto request) {
    final var thread = threadRepository.findById(request.threadId())
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + request.threadId()));
    final var threadId = thread.getId();
    log.info("Creating run for thread: {}", threadId);

    final var run = runRepository.save(RunEntity.builder()
        .threadId(threadId)
        .assistantId(request.assistantId())
        .task(request.task())
        .status(RunEntity.Status.QUEUED)
        .build()
    );

    eventService.publish(new RunCreatedEvent(threadId, run.getId()));

    log.info("Run created and queued: {}", run.getId());
    return runMapper.toDto(run);
  }

  /**
   * Handles run created events after transaction commit to ensure async execution starts only when
   * the run is persisted.
   *
   * @param event the run created event
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleRunCreated(RunCreatedEvent event) {
    log.debug("Handling run created event for run: {}", event.getPayload().runId());
    runExecutorService.submit(() -> executeRun(event.getPayload().runId()));
  }

  /**
   * Executes a run in the background.
   *
   * @param runId the ID of the run to execute
   */
  private void executeRun(UUID runId) {
    log.info("Starting execution of run: {}", runId);

    // Fetch managed entities
    final var run = runRepository.findById(runId).orElseThrow();
    run.start();
    eventService.publish(
        new RunUpdatedEvent(
            run.getThreadId(), runId, run.getAssistantId(), run.getStatus(), "Started"
        )
    );

    log.info("Run execution completed: {}", runId);

  }
}