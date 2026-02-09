package com.example.hypocaust.domain;

import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Hierarchical task progress tree for a TaskExecution.
 */
public class TodoList {

  @Getter
  private List<Todo> topLevel = List.of();

  /**
   * Add tasks under a parent, overriding existing children. If parentId is null, it updates the
   * top-level tasks (the roots of the forest).
   */
  public synchronized void setTodos(UUID parentId, List<Todo> children) {
    if (parentId == null) {
      this.topLevel = children;
    } else {
      this.topLevel = topLevel.stream()
          .map(root -> recursiveSetTasks(root, parentId, children))
          .toList();
    }
  }

  /**
   * Update the status of a specific todo by ID.
   */
  public synchronized void updateStatus(UUID id, TodoStatus status) {
    this.topLevel = topLevel.stream()
        .map(root -> recursiveUpdateStatus(root, id, status))
        .toList();
  }

  private Todo recursiveSetTasks(Todo node, UUID targetId, List<Todo> newChildren) {
    if (node.id().equals(targetId)) {
      return node.toBuilder().children(newChildren).build();
    }

    List<Todo> updated = node.children().stream()
        .map(child -> recursiveSetTasks(child, targetId, newChildren))
        .toList();

    // Only return a new instance if children actually changed
    return updated.equals(node.children())
        ? node
        : node.toBuilder().children(updated).build();
  }

  private Todo recursiveUpdateStatus(Todo node, UUID id, TodoStatus status) {
    if (node.id().equals(id)) {
      return node.toBuilder().status(status).build();
    }

    List<Todo> updated = node.children().stream()
        .map(child -> recursiveUpdateStatus(child, id, status))
        .toList();

    return updated.equals(node.children())
        ? node
        : node.toBuilder().children(updated).build();
  }

  public List<Todo> toList() {
    return topLevel;
  }
}