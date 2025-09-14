package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.MessageCompletedEvent.MessageCompletedPayload;
import java.util.UUID;

public final class MessageCompletedEvent extends MessageEvent<MessageCompletedPayload> {

  public MessageCompletedEvent(UUID threadId, UUID messageId, String finalContent) {
    super(threadId, new MessageCompletedPayload(messageId, finalContent));
  }

  @Override
  public EventType type() {
    return EventType.MESSAGE_COMPLETED;
  }

  public record MessageCompletedPayload(UUID messageId, String finalContent)
      implements MessageEventPayload {

  }
}