package com.example.hypocaust.mapper;

import com.example.hypocaust.db.EventEntity;
import com.example.hypocaust.domain.event.ArtifactCancelledEvent;
import com.example.hypocaust.domain.event.ArtifactCreatedEvent;
import com.example.hypocaust.domain.event.ArtifactScheduledEvent;
import com.example.hypocaust.domain.event.ErrorEvent;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.domain.event.OperatorFailedEvent;
import com.example.hypocaust.domain.event.OperatorFinishedEvent;
import com.example.hypocaust.domain.event.OperatorStartedEvent;
import com.example.hypocaust.domain.event.RunCompletedEvent;
import com.example.hypocaust.domain.event.RunScheduledEvent;
import com.example.hypocaust.domain.event.RunStartedEvent;
import com.example.hypocaust.domain.event.ToolCallingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(config = GlobalMapperConfig.class)
public interface EventMapper {

  EventEntity toEntity(Event<?> event);

  default RunScheduledEvent toRunScheduledEvent(EventEntity entity) {
    var payload = (RunScheduledEvent.RunScheduledEventPayload) entity.getPayload();
    return new RunScheduledEvent(entity.getProjectId(), payload.runId());
  }

  default RunStartedEvent toRunStartedEvent(EventEntity entity) {
    var payload = (RunStartedEvent.RunStartedEventPayload) entity.getPayload();
    return new RunStartedEvent(entity.getProjectId(), payload.runId());
  }

  default RunCompletedEvent toRunCompletedEvent(EventEntity entity) {
    var payload = (RunCompletedEvent.RunCompletedEventPayload) entity.getPayload();
    return new RunCompletedEvent(entity.getProjectId(), payload.runId());
  }

  default ArtifactScheduledEvent toArtifactScheduledEvent(EventEntity entity) {
    var payload = (ArtifactScheduledEvent.ArtifactScheduledEventPayload) entity.getPayload();
    return new ArtifactScheduledEvent(entity.getProjectId(), payload.artifactId());
  }

  default ArtifactCreatedEvent toArtifactCreatedEvent(EventEntity entity) {
    var payload = (ArtifactCreatedEvent.ArtifactCreatedEventPayload) entity.getPayload();
    return new ArtifactCreatedEvent(entity.getProjectId(), payload.artifactId());
  }

  default ArtifactCancelledEvent toArtifactCancelledEvent(EventEntity entity) {
    var payload = (ArtifactCancelledEvent.ArtifactCancelledEventPayload) entity.getPayload();
    return new ArtifactCancelledEvent(entity.getProjectId(), payload.artifactId(), payload.kind());
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
        payload.inputs());
  }

  default OperatorFinishedEvent toOperatorFinishedEvent(EventEntity entity) {
    var payload = (OperatorFinishedEvent.Payload) entity.getPayload();
    return new OperatorFinishedEvent(entity.getProjectId(), payload.operatorName(),
        payload.inputs(),
        payload.outputs());
  }

  default OperatorFailedEvent toOperatorFailedEvent(EventEntity entity) {
    var payload = (OperatorFailedEvent.Payload) entity.getPayload();
    return new OperatorFailedEvent(entity.getProjectId(), payload.operatorName(), payload.inputs(),
        payload.reason());
  }

  @ObjectFactory
  default Event<?> createEvent(EventEntity entity) {
    return switch (entity.getType()) {
      case RUN_SCHEDULED -> toRunScheduledEvent(entity);
      case RUN_STARTED -> toRunStartedEvent(entity);
      case RUN_COMPLETED -> toRunCompletedEvent(entity);

      case ARTIFACT_SCHEDULED -> toArtifactScheduledEvent(entity);
      case ARTIFACT_CREATED -> toArtifactCreatedEvent(entity);
      case ARTIFACT_CANCELLED -> toArtifactCancelledEvent(entity);

      case TOOL_CALLING -> toToolCallingEvent(entity);

      case ERROR -> toErrorEvent(entity);

      case OPERATOR_STARTED -> toOperatorStartedEvent(entity);
      case OPERATOR_FINISHED -> toOperatorFinishedEvent(entity);
      case OPERATOR_FAILED -> toOperatorFailedEvent(entity);
    };
  }

  Event<?> toDomain(EventEntity entity);

}
