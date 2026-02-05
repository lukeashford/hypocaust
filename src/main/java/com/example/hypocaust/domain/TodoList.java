package com.example.hypocaust.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hierarchical task progress tree for a TaskExecution. Thread-safe for concurrent modifications.
 * Uses adjacency list pattern with parent references.
 */
public class TodoList {

  private final Map<UUID, Todo> todosById = new ConcurrentHashMap<>();
  private final List<Todo> roots = new ArrayList<>();

  /**
   * Add a root-level todo (no parent).
   */
  public synchronized void addRoot(Todo todo) {
    todosById.put(todo.id(), todo);
    roots.add(todo);
  }

  /**
   * Add subtasks under a parent todo.
   */
  public synchronized void addSubtasks(UUID parentId, List<Todo> subtasks) {
    Todo parent = todosById.get(parentId);
    if (parent != null) {
      List<Todo> newChildren = new ArrayList<>(parent.children());
      for (Todo subtask : subtasks) {
        todosById.put(subtask.id(), subtask);
        newChildren.add(subtask);
      }
      Todo updatedParent = parent.withChildren(newChildren);
      todosById.put(parentId, updatedParent);
      updateInTree(updatedParent);
    } else {
      // If no parent, add as roots
      for (Todo subtask : subtasks) {
        addRoot(subtask);
      }
    }
  }

  /**
   * Update the status of a specific todo by ID.
   */
  public synchronized void updateStatus(UUID id, TodoStatus status) {
    Todo existing = todosById.get(id);
    if (existing != null) {
      Todo updated = existing.withStatus(status);
      todosById.put(id, updated);
      updateInTree(updated);
    }
  }

  /**
   * Get a todo by its ID.
   */
  public Todo getTodo(UUID id) {
    return todosById.get(id);
  }

  /**
   * Get all todos as a flat list.
   */
  public List<Todo> getAllTodos() {
    return new ArrayList<>(todosById.values());
  }

  /**
   * Get root-level todos (those without parents).
   */
  public synchronized List<Todo> getRoots() {
    return new ArrayList<>(roots);
  }

  /**
   * Check if the list is empty.
   */
  public boolean isEmpty() {
    return todosById.isEmpty();
  }

  /**
   * Get the count of tasks.
   */
  public int size() {
    return todosById.size();
  }

  /**
   * Update a todo in the tree structure (roots or as a child).
   */
  private void updateInTree(Todo updated) {
    // Update in roots if present
    for (int i = 0; i < roots.size(); i++) {
      if (roots.get(i).id().equals(updated.id())) {
        roots.set(i, updated);
        return;
      }
    }
    // Otherwise, need to update parent's children list
    for (Todo todo : todosById.values()) {
      for (int i = 0; i < todo.children().size(); i++) {
        if (todo.children().get(i).id().equals(updated.id())) {
          List<Todo> newChildren = new ArrayList<>(todo.children());
          newChildren.set(i, updated);
          Todo updatedParent = todo.withChildren(newChildren);
          todosById.put(updatedParent.id(), updatedParent);
          updateInTree(updatedParent);
          return;
        }
      }
    }
  }
}
