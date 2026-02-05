package com.example.hypocaust.domain;

import com.example.hypocaust.domain.event.TodoListUpdatedEvent;
import com.example.hypocaust.service.events.EventService;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sub-context for managing todos during a TaskExecution.
 */
@Getter
@RequiredArgsConstructor
public class TodosContext {

  private final UUID taskExecutionId;
  private final EventService eventService;

  private final TodoList list = new TodoList();

  /**
   * Add a root-level todo.
   */
  public synchronized void addRoot(Todo todo) {
    list.addRoot(todo);
    eventService.publish(new TodoListUpdatedEvent(taskExecutionId, list));
  }

  /**
   * Add subtasks under a parent todo.
   */
  public synchronized void addSubtasks(UUID parentId, List<Todo> subtasks) {
    list.addSubtasks(parentId, subtasks);
    eventService.publish(new TodoListUpdatedEvent(taskExecutionId, list));
  }

  /**
   * Update a todo's status by ID.
   */
  public synchronized void updateStatus(UUID todoId, TodoStatus status) {
    list.updateStatus(todoId, status);
    eventService.publish(new TodoListUpdatedEvent(taskExecutionId, list));
  }
}
