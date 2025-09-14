package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.Event.Payload;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract sealed class Event<T extends Payload>
    permits MessageEvent, RunEvent, ToolEvent, ErrorEvent {

  private final UUID threadId;
  private final UUID threadSeq;
  private final T payload;
  private final Instant occurredAt;

  protected Event(UUID threadId, T payload) {
    this.threadId = threadId;
    this.threadSeq = UuidCreator.getTimeOrderedEpoch();
    this.payload = payload;
    this.occurredAt = Instant.now();
  }

  /**
   * Returns the type identifier for this event.
   */
  @JsonGetter("type")
  public abstract EventType type();

  public interface Payload {

  }
}
