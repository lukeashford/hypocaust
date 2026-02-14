package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.Event.EventPayload;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonTypeIdResolver(EventTypeIdResolver.class)
public abstract sealed class Event<T extends EventPayload>
    permits ArtifactEvent, TaskExecutionEvent, ToolEvent, ErrorEvent, OperatorEvent,
    DecomposerEvent, TaskProgressEvent {

  private final UUID taskExecutionId;
  private final T payload;
  private final Instant occurredAt;
  private final EventType type;

  protected Event(UUID taskExecutionId, T payload) {
    this.taskExecutionId = taskExecutionId;
    this.payload = payload;
    this.occurredAt = Instant.now();
    this.type = type();
  }

  /**
   * Returns the type identifier for this event.
   */
  @JsonGetter("type")
  public abstract EventType type();

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.PROPERTY,
      property = "payloadType"
  )
  @JsonTypeIdResolver(PayloadTypeIdResolver.class)
  public interface EventPayload {

  }
}
