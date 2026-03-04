package com.example.hypocaust.domain;

import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.service.VersionManagementService;
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
  private final VersionManagementService versionService;
  private final ArtifactMapper artifactMapper;

  /**
   * Create a new TaskExecutionContext for a task execution.
   */
  public TaskExecutionContext create(UUID projectId, UUID taskExecutionId, UUID predecessorId,
      String name) {
    return new TaskExecutionContext(
        projectId,
        taskExecutionId,
        predecessorId,
        name,
        eventService,
        versionService,
        artifactMapper::toPresignedUrl
    );
  }
}
