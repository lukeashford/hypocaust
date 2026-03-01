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

  /**
   * Add a todo to the children of a parent (or top-level if parentId is null). If a todo with the
   * same description already exists under that parent, it is returned. Otherwise, the new todo is
   * added and returned.
   */
  public synchronized Todo addOrUpdateTodo(UUID parentId, Todo todo) {
    if (parentId == null) {
      var existing = topLevel.stream()
          .filter(t -> t.description().equals(todo.description()))
          .findFirst();
      if (existing.isPresent()) {
        return existing.get();
      }
      var newTopLevel = new java.util.ArrayList<>(topLevel);
      newTopLevel.add(todo);
      this.topLevel = List.copyOf(newTopLevel);
      return todo;
    } else {
      var searchResult = recursiveAddOrUpdate(topLevel, parentId, todo);
      this.topLevel = searchResult.updatedList();
      return searchResult.effectiveTodo();
    }
  }

  private record AddOrUpdateResult(List<Todo> updatedList, Todo effectiveTodo) {

  }

  private AddOrUpdateResult recursiveAddOrUpdate(List<Todo> nodes, UUID parentId, Todo todo) {
    for (int i = 0; i < nodes.size(); i++) {
      Todo node = nodes.get(i);
      if (node.id().equals(parentId)) {
        // Found parent! Check its children.
        var existing = node.children().stream()
            .filter(t -> t.description().equals(todo.description()))
            .findFirst();
        if (existing.isPresent()) {
          return new AddOrUpdateResult(nodes, existing.get());
        }
        // Add new child
        var newChildren = new java.util.ArrayList<>(node.children());
        newChildren.add(todo);
        Todo updatedNode = node.toBuilder().children(List.copyOf(newChildren)).build();
        var newNodes = new java.util.ArrayList<>(nodes);
        newNodes.set(i, updatedNode);
        return new AddOrUpdateResult(List.copyOf(newNodes), todo);
      }

      // Try children
      var childResult = recursiveAddOrUpdate(node.children(), parentId, todo);
      if (childResult.effectiveTodo() != null && childResult.updatedList() != node.children()) {
        // Found and modified
        Todo updatedNode = node.toBuilder().children(childResult.updatedList()).build();
        var newNodes = new java.util.ArrayList<>(nodes);
        newNodes.set(i, updatedNode);
        return new AddOrUpdateResult(List.copyOf(newNodes), childResult.effectiveTodo());
      } else if (childResult.effectiveTodo() != null) {
        // Found but not modified (already existed)
        return new AddOrUpdateResult(nodes, childResult.effectiveTodo());
      }
    }
    return new AddOrUpdateResult(nodes, null);
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

  /**
   * Return the number of direct children of the given parent. If parentId is null, returns the
   * count of top-level todos. Returns -1 if the parent is not found.
   */
  public synchronized int getChildCount(UUID parentId) {
    if (parentId == null) {
      return topLevel.size();
    }
    return recursiveChildCount(topLevel, parentId);
  }

  private int recursiveChildCount(List<Todo> nodes, UUID parentId) {
    for (Todo node : nodes) {
      if (node.id().equals(parentId)) {
        return node.children().size();
      }
      int found = recursiveChildCount(node.children(), parentId);
      if (found >= 0) {
        return found;
      }
    }
    return -1;
  }

  public List<Todo> toList() {
    return topLevel;
  }
}