package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.MessageProcessingEvent.MessageProcessingPayload;
import java.util.UUID;

public final class MessageProcessingEvent extends MessageEvent<MessageProcessingPayload> {

  public MessageProcessingEvent(UUID threadId, UUID messageId) {
    super(threadId, new MessageProcessingPayload(messageId));
  }

  @Override
  public EventType type() {
    return EventType.MESSAGE_PROCESSING;
  }

  public record MessageProcessingPayload(UUID messageId) implements MessageEventPayload {

  }
}