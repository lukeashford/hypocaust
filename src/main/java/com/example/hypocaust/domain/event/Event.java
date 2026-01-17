package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.Event.EventPayload;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.github.f4b6a3.uuid.UuidCreator;
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
    permits ArtifactEvent, RunEvent, ToolEvent, ErrorEvent {

  private final UUID projectId;
  private final UUID projectSeq;
  private final T payload;
  private final Instant occurredAt;
  private final EventType type;

  protected Event(UUID projectId, T payload) {
    this.projectId = projectId;
    this.projectSeq = UuidCreator.getTimeOrderedEpoch();
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
