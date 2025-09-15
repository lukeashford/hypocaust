package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.Event.Payload;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MessageProcessingEvent.class, name = "message.processing"),
    @JsonSubTypes.Type(value = MessageDeltaEvent.class, name = "message.delta"),
    @JsonSubTypes.Type(value = MessageCompletedEvent.class, name = "message.completed"),
    @JsonSubTypes.Type(value = RunCreatedEvent.class, name = "run.created"),
    @JsonSubTypes.Type(value = RunUpdatedEvent.class, name = "run.updated"),
    @JsonSubTypes.Type(value = ToolCallingEvent.class, name = "tool.calling"),
    @JsonSubTypes.Type(value = ErrorEvent.class, name = "error")
})
public abstract sealed class Event<T extends Payload>
    permits MessageEvent, RunEvent, ToolEvent, ErrorEvent {

  private final UUID threadId;
  private final UUID threadSeq;
  private final T payload;
  private final Instant occurredAt;
  private final EventType type;

  protected Event(UUID threadId, T payload) {
    this.threadId = threadId;
    this.threadSeq = UuidCreator.getTimeOrderedEpoch();
    this.payload = payload;
    this.occurredAt = Instant.now();
    this.type = type();
  }

  /**
   * Returns the type identifier for this event.
   */
  @JsonGetter("type")
  public abstract EventType type();

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "payloadType")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = MessageProcessingEvent.MessageProcessingPayload.class, name = "MESSAGE_PROCESSING"),
      @JsonSubTypes.Type(value = MessageDeltaEvent.MessageDeltaPayload.class, name = "MESSAGE_DELTA"),
      @JsonSubTypes.Type(value = MessageCompletedEvent.MessageCompletedPayload.class, name = "MESSAGE_COMPLETED"),
      @JsonSubTypes.Type(value = RunCreatedEvent.RunCreatedPayload.class, name = "RUN_CREATED"),
      @JsonSubTypes.Type(value = RunUpdatedEvent.RunUpdatedPayload.class, name = "RUN_UPDATED"),
      @JsonSubTypes.Type(value = ToolCallingEvent.ToolCallingPayload.class, name = "TOOL_CALLING"),
      @JsonSubTypes.Type(value = ErrorEvent.ErrorPayload.class, name = "ERROR")
  })
  public interface Payload {

  }
}
