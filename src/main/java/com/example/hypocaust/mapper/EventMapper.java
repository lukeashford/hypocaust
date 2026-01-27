package com.example.hypocaust.mapper;

import com.example.hypocaust.db.EventEntity;
import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.domain.event.ArtifactRemovedEvent;
import com.example.hypocaust.domain.event.ArtifactUpdatedEvent;
import com.example.hypocaust.domain.event.ErrorEvent;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.domain.event.OperatorFailedEvent;
import com.example.hypocaust.domain.event.OperatorFinishedEvent;
import com.example.hypocaust.domain.event.OperatorStartedEvent;
import com.example.hypocaust.domain.event.TaskExecutionCompletedEvent;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.domain.event.TaskProgressUpdatedEvent;
import com.example.hypocaust.domain.event.ToolCallingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(config = GlobalMapperConfig.class)
public interface EventMapper {

  EventEntity toEntity(Event<?> event);

  default ArtifactAddedEvent toArtifactAddedEvent(EventEntity entity) {
    var payload = (ArtifactAddedEvent.Payload) entity.getPayload();
    return new ArtifactAddedEvent(
        entity.getProjectId(),
        payload.name(),
        payload.kind(),
        payload.description(),
        payload.externalUrl(),
        payload.inlineContent(),
        payload.metadata()
    );
  }

  default ArtifactUpdatedEvent toArtifactUpdatedEvent(EventEntity entity) {
    var payload = (ArtifactUpdatedEvent.Payload) entity.getPayload();
    return new ArtifactUpdatedEvent(
        entity.getProjectId(),
        payload.name(),
        payload.description(),
        payload.externalUrl(),
        payload.inlineContent(),
        payload.metadata()
    );
  }

  default ArtifactRemovedEvent toArtifactRemovedEvent(EventEntity entity) {
    var payload = (ArtifactRemovedEvent.Payload) entity.getPayload();
    return new ArtifactRemovedEvent(entity.getProjectId(), payload.name());
  }

  default TaskExecutionStartedEvent toTaskExecutionStartedEvent(EventEntity entity) {
    return new TaskExecutionStartedEvent(entity.getProjectId());
  }

  default TaskExecutionCompletedEvent toTaskExecutionCompletedEvent(EventEntity entity) {
    var payload = (TaskExecutionCompletedEvent.Payload) entity.getPayload();
    return new TaskExecutionCompletedEvent(entity.getProjectId(), payload.hasChanges(), payload.message());
  }

  default TaskExecutionFailedEvent toTaskExecutionFailedEvent(EventEntity entity) {
    var payload = (TaskExecutionFailedEvent.Payload) entity.getPayload();
    return new TaskExecutionFailedEvent(entity.getProjectId(), payload.reason());
  }

  default TaskProgressUpdatedEvent toTaskProgressUpdatedEvent(EventEntity entity) {
    var payload = (TaskProgressUpdatedEvent.Payload) entity.getPayload();
    return new TaskProgressUpdatedEvent(entity.getProjectId(), payload.taskTree());
  }

  default ToolCallingEvent toToolCallingEvent(EventEntity entity) {
    var payload = (ToolCallingEvent.ToolCallingEventPayload) entity.getPayload();
    return new ToolCallingEvent(entity.getProjectId(), payload.content());
  }

  default ErrorEvent toErrorEvent(EventEntity entity) {
    var payload = (ErrorEvent.ErrorEventPayload) entity.getPayload();
    return new ErrorEvent(entity.getProjectId(), payload.message());
  }

  default OperatorStartedEvent toOperatorStartedEvent(EventEntity entity) {
    var payload = (OperatorStartedEvent.Payload) entity.getPayload();
    return new OperatorStartedEvent(entity.getProjectId(), payload.operatorName(),
        payload.taskPath(), payload.inputs());
  }

  default OperatorFinishedEvent toOperatorFinishedEvent(EventEntity entity) {
    var payload = (OperatorFinishedEvent.Payload) entity.getPayload();
    return new OperatorFinishedEvent(entity.getProjectId(), payload.operatorName(),
        payload.taskPath(), payload.inputs(), payload.outputs());
  }

  default OperatorFailedEvent toOperatorFailedEvent(EventEntity entity) {
    var payload = (OperatorFailedEvent.Payload) entity.getPayload();
    return new OperatorFailedEvent(entity.getProjectId(), payload.operatorName(),
        payload.taskPath(), payload.inputs(), payload.reason());
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

      case TASK_PROGRESS_UPDATED -> toTaskProgressUpdatedEvent(entity);

      case TOOL_CALLING -> toToolCallingEvent(entity);

      case ERROR -> toErrorEvent(entity);

      case OPERATOR_STARTED -> toOperatorStartedEvent(entity);
      case OPERATOR_FINISHED -> toOperatorFinishedEvent(entity);
      case OPERATOR_FAILED -> toOperatorFailedEvent(entity);
    };
  }

  Event<?> toDomain(EventEntity entity);

}
