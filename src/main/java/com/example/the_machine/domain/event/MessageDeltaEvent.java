package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.MessageDeltaEvent.MessageDeltaPayload;
import java.util.UUID;

public final class MessageDeltaEvent extends MessageEvent<MessageDeltaPayload> {

  public MessageDeltaEvent(UUID threadId, UUID messageId, String deltaContent) {
    super(threadId, new MessageDeltaPayload(messageId, deltaContent));
  }

  @Override
  public EventType type() {
    return EventType.MESSAGE_DELTA;
  }

  public record MessageDeltaPayload(UUID messageId, String deltaContent)
      implements MessageEventPayload {

  }
}