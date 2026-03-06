package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.Changelist;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.domain.TaskExecutionStatus;
import com.example.hypocaust.domain.event.TaskExecutionCompletedEvent;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.dto.TaskInitializationResult;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.utils.NamingUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the transactional lifecycle transitions of a TaskExecution. Separated from TaskService to
 * avoid transactional self-invocation issues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutionLifecycleService {

  private final TaskExecutionRepository taskExecutionRepository;
  private final VersionManagementService versionService;
  private final TodoService todoService;
  private final EventService eventService;

  /**
   * Initialize a task execution. Transitions to RUNNING synchronously.
   */
  @Transactional
  public TaskInitializationResult startExecution(UUID projectId, String task,
      UUID predecessorIdInput) {
    UUID predecessorId = Optional.ofNullable(predecessorIdInput)
        .orElseGet(() -> versionService.getMostRecentTaskExecutionId(projectId)
            .orElse(null));

    Set<String> existingNames = taskExecutionRepository.findAllNamesByProjectId(projectId);
    String name = NamingUtils.sanitize(task, 50);
    name = NamingUtils.appendCounterIfExists(name, existingNames, 50);

    final var taskExecution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task(task)
        .predecessorId(predecessorId)
        .name(name)
        .build();
    taskExecution.start();
    taskExecutionRepository.save(taskExecution);
    final var taskExecutionId = taskExecution.getId();

    final UUID firstEventId = eventService.publish(
        new TaskExecutionStartedEvent(taskExecutionId));

    log.info("Created TaskExecution {} for project {} with predecessor {}",
        taskExecutionId, projectId, predecessorId);

    return new TaskInitializationResult(projectId, taskExecutionId, predecessorId, firstEventId,
        name);
  }

  /**
   * Commit a successful task execution. Persists finalized artifacts, generates commit message, and
   * publishes completion event.
   */
  @Transactional
  public void commitExecution(UUID taskExecutionId, UUID projectId, String task,
      TaskExecutionContext context) {
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(
            () -> new IllegalStateException("TaskExecution not found: " + taskExecutionId));

    try {
      ArtifactsContext artifacts = context.getArtifacts();
      Changelist changelist = artifacts.getChangelist();

      // 1. Persist artifacts (already finalized — no downloading)
      TaskExecutionDelta delta = versionService.persist(changelist, taskExecutionId, projectId);

      // 2. Persist todos
      try {
        todoService.materialize(context.getTodos().getList(), taskExecutionId);
      } catch (Exception e) {
        log.warn("Failed to materialize todos for execution {}: {}", taskExecutionId,
            e.getMessage());
      }

      // 3. Use task as commit message directly
      String commitMessage = task;

      // 4. Determine status from artifact states
      List<Artifact> allArtifacts = new ArrayList<>(changelist.getAdded());
      allArtifacts.addAll(changelist.getEdited());

      long failedCount = allArtifacts.stream()
          .filter(a -> a.status() == ArtifactStatus.FAILED)
          .count();
      boolean allFailed = !allArtifacts.isEmpty() && failedCount == allArtifacts.size();
      boolean anyFailed = failedCount > 0;

      if (allFailed) {
        log.warn("All artifacts failed for execution {}", taskExecutionId);
        taskExecution.fail("All artifacts failed: " + commitMessage);
      } else if (anyFailed) {
        log.info("Execution {} partially successful", taskExecutionId);
        taskExecution.partiallySuccessful(commitMessage, delta);
      } else {
        taskExecution.complete(commitMessage, delta);
      }

      taskExecutionRepository.save(taskExecution);

      // 5. Publish event
      if (taskExecution.getStatus() == TaskExecutionStatus.FAILED) {
        eventService.publish(new TaskExecutionFailedEvent(taskExecutionId,
            taskExecution.getCommitMessage()));
      } else {
        eventService.publish(new TaskExecutionCompletedEvent(
            taskExecutionId,
            delta != null && delta.hasChanges(),
            taskExecution.getName(),
            commitMessage));
      }

    } catch (Exception e) {
      log.error("Critical failure during commit of execution {}: {}", taskExecutionId,
          e.getMessage(), e);
      taskExecution.fail("Critical error: " + e.getMessage());
      taskExecutionRepository.save(taskExecution);
      eventService.publish(new TaskExecutionFailedEvent(taskExecutionId,
          taskExecution.getCommitMessage()));
    }
  }

  /**
   * Fail a task execution. Publishes failure event. Pending artifacts are simply not persisted.
   */
  @Transactional
  public void failExecution(UUID taskExecutionId, String errorMessage) {
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(
            () -> new IllegalStateException("TaskExecution not found: " + taskExecutionId));

    taskExecution.fail(errorMessage);
    taskExecutionRepository.save(taskExecution);

    eventService.publish(new TaskExecutionFailedEvent(taskExecutionId, errorMessage));
  }
}
