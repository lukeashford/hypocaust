package com.example.hypocaust.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hierarchical task progress tree for a TaskExecution.
 * Thread-safe for concurrent modifications.
 */
public class TaskTree {

  private final Map<String, TaskItem> tasks = new ConcurrentHashMap<>();

  /**
   * Add subtasks under a path prefix.
   */
  public void addSubtasks(String pathPrefix, List<TaskItem> subtasks) {
    for (TaskItem subtask : subtasks) {
      tasks.put(subtask.id(), subtask);
    }
  }

  /**
   * Update the status of a specific task.
   */
  public void updateStatus(String taskId, TaskStatus status) {
    TaskItem existing = tasks.get(taskId);
    if (existing != null) {
      tasks.put(taskId, existing.withStatus(status));
    }
  }

  /**
   * Get a task by its ID.
   */
  public TaskItem getTask(String taskId) {
    return tasks.get(taskId);
  }

  /**
   * Get all tasks as a flat list.
   */
  public List<TaskItem> getAllTasks() {
    return new ArrayList<>(tasks.values());
  }

  /**
   * Get tasks organized by their hierarchy.
   * Returns a map where keys are parent paths and values are direct children.
   */
  public Map<String, List<TaskItem>> getHierarchy() {
    Map<String, List<TaskItem>> hierarchy = new HashMap<>();

    for (TaskItem task : tasks.values()) {
      String parentPath = getParentPath(task.id());
      hierarchy.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(task);
    }

    return hierarchy;
  }

  private String getParentPath(String taskId) {
    int lastDot = taskId.lastIndexOf('.');
    return lastDot > 0 ? taskId.substring(0, lastDot) : "";
  }

  /**
   * Check if the tree is empty.
   */
  public boolean isEmpty() {
    return tasks.isEmpty();
  }

  /**
   * Get the count of tasks.
   */
  public int size() {
    return tasks.size();
  }
}
