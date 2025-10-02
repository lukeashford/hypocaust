package com.example.the_machine.mapper;

import com.example.the_machine.db.EventEntity;
import com.example.the_machine.domain.event.ArtifactCancelledEvent;
import com.example.the_machine.domain.event.ArtifactCreatedEvent;
import com.example.the_machine.domain.event.ArtifactScheduledEvent;
import com.example.the_machine.domain.event.ErrorEvent;
import com.example.the_machine.domain.event.Event;
import com.example.the_machine.domain.event.RunCompletedEvent;
import com.example.the_machine.domain.event.RunScheduledEvent;
import com.example.the_machine.domain.event.RunStartedEvent;
import com.example.the_machine.domain.event.ToolCallingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(config = GlobalMapperConfig.class)
public interface EventMapper {

  EventEntity toEntity(Event<?> event);

  RunScheduledEvent toRunScheduledEvent(EventEntity entity);

  RunStartedEvent toRunStartedEvent(EventEntity entity);

  RunCompletedEvent toRunCompletedEvent(EventEntity entity);

  ArtifactScheduledEvent toArtifactScheduledEvent(EventEntity entity);

  ArtifactCreatedEvent toArtifactCreatedEvent(EventEntity entity);

  ArtifactCancelledEvent toArtifactCancelledEvent(EventEntity entity);

  ToolCallingEvent toToolCallingEvent(EventEntity entity);

  ErrorEvent toErrorEvent(EventEntity entity);

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
    };
  }

  Event<?> toDomain(EventEntity entity);

}
