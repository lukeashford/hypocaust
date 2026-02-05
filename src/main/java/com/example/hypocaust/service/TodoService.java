package com.example.hypocaust.service;

import com.example.hypocaust.db.TodoEntity;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoList;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.repo.TodoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

  private final TodoRepository todoRepository;

  public List<Todo> getTodosForTaskExecution(UUID taskExecutionId) {
    // 1. Check if execution is still running
    Optional<TaskExecutionContext> activeContext =
        TaskExecutionContextHolder.getContextByTaskExecutionId(taskExecutionId);

    if (activeContext.isPresent()) {
      // Return from in-memory TodoList (root-level todos with nested children)
      return activeContext.get().getTodos().getList().getRoots();
    }

    // 2. Return from database - fetch root entities and map to domain
    List<TodoEntity> roots = todoRepository.findByTaskExecutionIdAndParentIsNull(taskExecutionId);
    return roots.stream()
        .map(this::mapToDomain)
        .toList();
  }

  private Todo mapToDomain(TodoEntity entity) {
    List<Todo> children = entity.getChildren().stream()
        .map(this::mapToDomain)
        .toList();
    return new Todo(entity.getId(), entity.getDescription(), entity.getStatus(), children);
  }

  @Transactional
  public void materialize(TodoList todoList, UUID taskExecutionId) {
    for (Todo todo : todoList.getRoots()) {
      materializeTodo(todo, null, taskExecutionId);
    }
  }

  private void materializeTodo(Todo todo, TodoEntity parent, UUID taskExecutionId) {
    TodoEntity entity = TodoEntity.builder()
        .taskExecutionId(taskExecutionId)
        .parent(parent)
        .description(todo.description())
        .status(todo.status())
        .build();
    todoRepository.save(entity);

    for (Todo child : todo.children()) {
      materializeTodo(child, entity, taskExecutionId);
    }
  }
}
