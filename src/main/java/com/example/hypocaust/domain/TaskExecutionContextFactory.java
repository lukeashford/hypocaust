package com.example.hypocaust.domain;

import com.example.hypocaust.service.ArtifactNameGeneratorService;
import com.example.hypocaust.service.ArtifactVersionManagementService;
import com.example.hypocaust.service.events.EventService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating TaskExecutionContext instances with injected services.
 */
@Component
@RequiredArgsConstructor
public class TaskExecutionContextFactory {

  private final EventService eventService;
  private final ArtifactVersionManagementService versionService;
  private final ArtifactNameGeneratorService nameGeneratorService;

  /**
   * Create a new TaskExecutionContext for a task execution.
   */
  public TaskExecutionContext create(UUID projectId, UUID taskExecutionId, UUID predecessorId) {
    return new TaskExecutionContext(
        projectId,
        taskExecutionId,
        predecessorId,
        eventService,
        versionService,
        nameGeneratorService
    );
  }
}
