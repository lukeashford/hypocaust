package com.example.hypocaust.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;

/**
 * Represents a single task item in the progress tree with nested children
 */
@Builder(toBuilder = true)
public record Todo(
    UUID id,
    @NonNull String description,
    @NonNull TodoStatus status,
    @NonNull List<Todo> children
) {

  public Todo {
    if (id == null) {
      id = UuidCreator.getTimeOrderedEpoch();
    }
  }

  public Todo(String description, TodoStatus status) {
    this(null, description, status, List.of());
  }

  public List<Todo> children() {
    return children;
  }
}
