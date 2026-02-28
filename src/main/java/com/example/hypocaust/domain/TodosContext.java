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
   * Add tasks under a parent (overrides existing children). Pass null for parentId to target the
   * "ROOT" node.
   */
  public void registerSubtodos(UUID parentId, List<Todo> children) {
    list.setTodos(parentId, children);
    eventService.publish(new TodoListUpdatedEvent(taskExecutionId, list));
  }

  /**
   * Add a todo under a parent, returning the existing one if it matches by description. Publishes
   * an update event if the list changed.
   */
  public Todo addOrUpdateTodo(UUID parentId, Todo todo) {
    Todo effective = list.addOrUpdateTodo(parentId, todo);
    // Note: currently addOrUpdateTodo returns the existing instance if found, 
    // or the new one if added. If it was added, the list changed.
    // If it was found, the list didn't change (stateless comparison in list is not perfect here,
    // but for now we always publish to be safe, or we could check if effective == todo).
    eventService.publish(new TodoListUpdatedEvent(taskExecutionId, list));
    return effective;
  }

  /**
   * Update a todo's status to IN_PROGRESS.
   */
  public void markRunning(UUID todoId) {
    if (todoId != null) {
      updateStatus(todoId, TodoStatus.IN_PROGRESS);
    }
  }

  /**
   * Update a todo's status to COMPLETED.
   */
  public void markCompleted(UUID todoId) {
    if (todoId != null) {
      updateStatus(todoId, TodoStatus.COMPLETED);
    }
  }

  /**
   * Update a todo's status to FAILED.
   */
  public void markFailed(UUID todoId) {
    if (todoId != null) {
      updateStatus(todoId, TodoStatus.FAILED);
    }
  }

  /**
   * Update a todo's status by ID.
   */
  public void updateStatus(UUID todoId, TodoStatus status) {
    list.updateStatus(todoId, status);
    eventService.publish(new TodoListUpdatedEvent(taskExecutionId, list));
  }

}
