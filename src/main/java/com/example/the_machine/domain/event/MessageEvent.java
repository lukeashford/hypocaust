package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.MessageEvent.MessageEventPayload;
import java.util.UUID;

public abstract sealed class MessageEvent<T extends MessageEventPayload>
    extends Event<MessageEventPayload>
    permits MessageProcessingEvent, MessageDeltaEvent, MessageCompletedEvent {

  protected MessageEvent(UUID threadId, T payload) {
    super(threadId, payload);
  }

  public interface MessageEventPayload extends Payload {

    UUID messageId();
  }
}
