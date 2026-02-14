package com.example.hypocaust.mapper;

import com.example.hypocaust.db.EventEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.domain.event.ArtifactRemovedEvent;
import com.example.hypocaust.domain.event.ArtifactUpdatedEvent;
import com.example.hypocaust.domain.event.DecomposerFailedEvent;
import com.example.hypocaust.domain.event.DecomposerFinishedEvent;
import com.example.hypocaust.domain.event.DecomposerStartedEvent;
import com.example.hypocaust.domain.event.ErrorEvent;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.domain.event.TaskExecutionCompletedEvent;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.domain.event.TodoListUpdatedEvent;
import com.example.hypocaust.domain.event.ToolCallingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(config = GlobalMapperConfig.class)
public interface EventMapper {

  EventEntity toEntity(Event<?> event);

  default ArtifactAddedEvent toArtifactAddedEvent(EventEntity entity) {
    return new ArtifactAddedEvent(
        entity.getTaskExecutionId(),
        (Artifact) entity.getPayload()
    );
  }

  default ArtifactUpdatedEvent toArtifactUpdatedEvent(EventEntity entity) {
    return new ArtifactUpdatedEvent(
        entity.getTaskExecutionId(),
        (Artifact) entity.getPayload()
    );
  }

  default ArtifactRemovedEvent toArtifactRemovedEvent(EventEntity entity) {
    var payload = (ArtifactRemovedEvent.Payload) entity.getPayload();
    return new ArtifactRemovedEvent(entity.getTaskExecutionId(), payload.name());
  }

  default TaskExecutionStartedEvent toTaskExecutionStartedEvent(EventEntity entity) {
    return new TaskExecutionStartedEvent(entity.getTaskExecutionId());
  }

  default TaskExecutionCompletedEvent toTaskExecutionCompletedEvent(EventEntity entity) {
    var payload = (TaskExecutionCompletedEvent.Payload) entity.getPayload();
    return new TaskExecutionCompletedEvent(entity.getTaskExecutionId(), payload.hasChanges(),
        payload.message());
  }

  default TaskExecutionFailedEvent toTaskExecutionFailedEvent(EventEntity entity) {
    var payload = (TaskExecutionFailedEvent.Payload) entity.getPayload();
    return new TaskExecutionFailedEvent(entity.getTaskExecutionId(), payload.reason());
  }

  default TodoListUpdatedEvent toTodoListUpdatedEvent(EventEntity entity) {
    var payload = (TodoListUpdatedEvent.Payload) entity.getPayload();
    return new TodoListUpdatedEvent(entity.getTaskExecutionId(), payload.todoList());
  }

  default ToolCallingEvent toToolCallingEvent(EventEntity entity) {
    var payload = (ToolCallingEvent.ToolCallingEventPayload) entity.getPayload();
    return new ToolCallingEvent(entity.getTaskExecutionId(), payload.content());
  }

  default ErrorEvent toErrorEvent(EventEntity entity) {
    var payload = (ErrorEvent.ErrorEventPayload) entity.getPayload();
    return new ErrorEvent(entity.getTaskExecutionId(), payload.message());
  }

  default DecomposerStartedEvent toDecomposerStartedEvent(EventEntity entity) {
    var payload = (DecomposerStartedEvent.Payload) entity.getPayload();
    return new DecomposerStartedEvent(entity.getTaskExecutionId(), payload.task());
  }

  default DecomposerFinishedEvent toDecomposerFinishedEvent(EventEntity entity) {
    var payload = (DecomposerFinishedEvent.Payload) entity.getPayload();
    return new DecomposerFinishedEvent(entity.getTaskExecutionId(), payload.task(),
        payload.summary(), payload.artifactNames());
  }

  default DecomposerFailedEvent toDecomposerFailedEvent(EventEntity entity) {
    var payload = (DecomposerFailedEvent.Payload) entity.getPayload();
    return new DecomposerFailedEvent(entity.getTaskExecutionId(), payload.task(),
        payload.reason());
  }

  @ObjectFactory
  default Event<?> createEvent(EventEntity entity) {
    return switch (entity.getType()) {
      case ARTIFACT_ADDED -> toArtifactAddedEvent(entity);
      case ARTIFACT_UPDATED -> toArtifactUpdatedEvent(entity);
      case ARTIFACT_REMOVED -> toArtifactRemovedEvent(entity);

      case TASKEXECUTION_STARTED -> toTaskExecutionStartedEvent(entity);
      case TASKEXECUTION_COMPLETED -> toTaskExecutionCompletedEvent(entity);
      case TASKEXECUTION_FAILED -> toTaskExecutionFailedEvent(entity);

      case TASK_PROGRESS_UPDATED -> toTodoListUpdatedEvent(entity);

      case TOOL_CALLING -> toToolCallingEvent(entity);

      case ERROR -> toErrorEvent(entity);

      case DECOMPOSER_STARTED -> toDecomposerStartedEvent(entity);
      case DECOMPOSER_FINISHED -> toDecomposerFinishedEvent(entity);
      case DECOMPOSER_FAILED -> toDecomposerFailedEvent(entity);
    };
  }

  Event<?> toDomain(EventEntity entity);

}
