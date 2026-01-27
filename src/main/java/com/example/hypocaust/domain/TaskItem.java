package com.example.hypocaust.domain;

/**
 * Represents a single task item in the progress tree.
 *
 * @param id          Hierarchical identifier (e.g., "0", "0.1", "0.1.2")
 * @param description Human-readable description (the "todo" wording from ledger)
 * @param status      Current status of the task
 */
public record TaskItem(
    String id,
    String description,
    TaskStatus status
) {

  public TaskItem withStatus(TaskStatus newStatus) {
    return new TaskItem(id, description, newStatus);
  }
}
