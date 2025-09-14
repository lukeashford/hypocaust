package com.example.the_machine.mapper;

import com.example.the_machine.domain.event.Event;
import com.example.the_machine.domain.event.EventEntity;
import com.example.the_machine.domain.event.MessageCompletedEvent;
import com.example.the_machine.domain.event.MessageDeltaEvent;
import com.example.the_machine.domain.event.MessageProcessingEvent;
import com.example.the_machine.domain.event.RunCreatedEvent;
import com.example.the_machine.domain.event.RunUpdatedEvent;
import com.example.the_machine.domain.event.ToolCallingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(config = GlobalMapperConfig.class)
public interface EventMapper {

  EventEntity toEntity(Event<?> event);

  MessageProcessingEvent toMessageProcessingEvent(EventEntity entity);

  MessageDeltaEvent toMessageDeltaEvent(EventEntity entity);

  MessageCompletedEvent toMessageCompletedEvent(EventEntity entity);

  RunCreatedEvent toRunCreatedEvent(EventEntity entity);

  RunUpdatedEvent toRunUpdatedEvent(EventEntity entity);

  ToolCallingEvent toToolCallingEvent(EventEntity entity);

  @ObjectFactory
  default Event<?> createEvent(EventEntity entity) {
    return switch (entity.getType()) {
      case MESSAGE_PROCESSING -> toMessageProcessingEvent(entity);
      case MESSAGE_DELTA -> toMessageDeltaEvent(entity);
      case MESSAGE_COMPLETED -> toMessageCompletedEvent(entity);
      case RUN_CREATED -> toRunCreatedEvent(entity);
      case RUN_UPDATED -> toRunUpdatedEvent(entity);
      case TOOL_CALLING -> toToolCallingEvent(entity);
      default -> throw new IllegalArgumentException("Unsupported event type: " + entity.getType());
    };
  }

  Event<?> toDomain(EventEntity entity);

}
