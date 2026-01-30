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
import com.example.hypocaust.dto.ArtifactDto;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(config = GlobalMapperConfig.class)
public interface EventMapper {

  EventEntity toEntity(Event<?> event);

  default ArtifactAddedEvent toArtifactAddedEvent(EventEntity entity) {
    return new ArtifactAddedEvent(
        entity.getTaskExecutionId(),
        (ArtifactDto) entity.getPayload()
    );
  }

  default ArtifactUpdatedEvent toArtifactUpdatedEvent(EventEntity entity) {
    return new ArtifactUpdatedEvent(
        entity.getTaskExecutionId(),
        (ArtifactDto) entity.getPayload()
    );
  }

  default ArtifactRemovedEvent toArtifactRemovedEvent(EventEntity entity) {
    var payload = (ArtifactRemovedEvent.Payload) entity.getPayload();
    return new ArtifactRemovedEvent(entity.getTaskExecutionId(), payload.fileName());
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

  default TaskProgressUpdatedEvent toTaskProgressUpdatedEvent(EventEntity entity) {
    var payload = (TaskProgressUpdatedEvent.Payload) entity.getPayload();
    return new TaskProgressUpdatedEvent(entity.getTaskExecutionId(), payload.taskTree());
  }

  default ToolCallingEvent toToolCallingEvent(EventEntity entity) {
    var payload = (ToolCallingEvent.ToolCallingEventPayload) entity.getPayload();
    return new ToolCallingEvent(entity.getTaskExecutionId(), payload.content());
  }

  default ErrorEvent toErrorEvent(EventEntity entity) {
    var payload = (ErrorEvent.ErrorEventPayload) entity.getPayload();
    return new ErrorEvent(entity.getTaskExecutionId(), payload.message());
  }

  default OperatorStartedEvent toOperatorStartedEvent(EventEntity entity) {
    var payload = (OperatorStartedEvent.Payload) entity.getPayload();
    return new OperatorStartedEvent(entity.getTaskExecutionId(), payload.operatorName(),
        payload.inputs(), payload.taskPath());
  }

  default OperatorFinishedEvent toOperatorFinishedEvent(EventEntity entity) {
    var payload = (OperatorFinishedEvent.Payload) entity.getPayload();
    return new OperatorFinishedEvent(entity.getTaskExecutionId(), payload.operatorName(),
        payload.inputs(), payload.outputs(), payload.taskPath());
  }

  default OperatorFailedEvent toOperatorFailedEvent(EventEntity entity) {
    var payload = (OperatorFailedEvent.Payload) entity.getPayload();
    return new OperatorFailedEvent(entity.getTaskExecutionId(), payload.operatorName(),
        payload.inputs(), payload.taskPath(), payload.reason());
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
