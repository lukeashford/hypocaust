package com.example.the_machine.domain;

import static com.example.the_machine.domain.AssistantEntity.DEFAULT_ASSISTANT_ID;

import com.example.the_machine.dto.CreateRunRequest;
import com.example.the_machine.repo.RunRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * Factory for creating RunEntity instances with controlled access and validation. This factory
 * ensures that RunEntity instances are properly created and persisted, preventing detached entity
 * issues by managing the entity lifecycle.
 */
@Component
@RequiredArgsConstructor
public class RunFactory {

  private final RunRepository runRepository;

  /**
   * Creates and saves a new RunEntity based on the request. Returns the UUID of the created run to
   * avoid passing detached entities.
   *
   * @param request the create run request
   * @param threadId the thread ID for the run
   * @return the UUID of the created and saved run
   */
  public UUID createAndSaveRun(CreateRunRequest request, UUID threadId) {
    val run = RunEntity.builder()
        .id(UUID.randomUUID())
        .threadId(threadId)
        .assistantId(
            request.assistantId() != null ? request.assistantId() : DEFAULT_ASSISTANT_ID)
        .status(RunEntity.Status.QUEUED)
        .kind(RunEntity.Kind.FULL)
        .build();

    val savedRun = runRepository.save(run);
    return savedRun.getId();
  }

  /**
   * Finds a managed RunEntity by ID, ensuring it's attached to the current session.
   *
   * @param runId the run ID to find
   * @return the managed RunEntity
   * @throws IllegalStateException if the run is not found
   */
  public RunEntity findManagedRun(UUID runId) {
    return runRepository.findById(runId)
        .orElseThrow(() -> new IllegalStateException("Run not found: " + runId));
  }
}