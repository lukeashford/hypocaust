package com.example.hypocaust.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;

/**
 * Represents a single task item in the progress tree with nested children.
 *
 * @param id Unique identifier
 * @param description Human-readable description (the "todo" wording from ledger)
 * @param status Current status of the task
 * @param children Child todo items
 */
public record Todo(
    UUID id,
    @NonNull String description,
    @NonNull TodoStatus status,
    List<Todo> children
) {

  public Todo(String description, TodoStatus status) {
    this(null, description, status, null);
  }

  public Todo {
    id = UuidCreator.getTimeOrderedEpoch();
    if (children == null) {
      children = List.of();
    } else {
      children = List.copyOf(children);
    }
  }

  public Todo withStatus(TodoStatus newStatus) {
    return new Todo(id, description, newStatus, children);
  }

  public Todo withChildren(List<Todo> newChildren) {
    return new Todo(id, description, status, newChildren);
  }
}
