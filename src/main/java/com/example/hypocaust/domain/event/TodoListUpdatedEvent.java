package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.TodoList;
import java.util.UUID;

public final class TodoListUpdatedEvent extends
    TaskProgressEvent<TodoListUpdatedEvent.Payload> {

  public TodoListUpdatedEvent(UUID taskExecutionId, TodoList todoList) {
    super(taskExecutionId, new Payload(todoList));
  }

  @Override
  public EventType type() {
    return EventType.TODO_LIST_UPDATED;
  }

  public record Payload(TodoList todoList) implements TaskProgressEventPayload {

  }
}
