package com.example.hypocaust.service;

import com.example.hypocaust.db.TodoEntity;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoList;
import com.example.hypocaust.mapper.TodoMapper;
import com.example.hypocaust.repo.TodoRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TodoService {

  private final TodoRepository todoRepository;
  private final TodoMapper todoMapper;

  /**
   * Materializes the current state of the TodoList into the database. Leveraging cascade saves, we
   * only need to save the root entities.
   */
  @Transactional
  public void materialize(TodoList todoList, UUID taskExecutionId) {
    log.info("Materializing {} root todos for task execution {}",
        todoList.getTopLevel().size(), taskExecutionId);

    // Clean up existing todos for this execution to avoid duplicates if re-materializing
    todoRepository.deleteByTaskExecutionId(taskExecutionId);

    List<TodoEntity> entities = todoList.getTopLevel().stream()
        .map(todo -> todoMapper.toEntity(todo, taskExecutionId))
        .toList();

    todoRepository.saveAll(entities);
  }

  /**
   * Retrieves the domain representation of todos for a task execution.
   */
  public List<Todo> getTodosForTaskExecution(UUID taskExecutionId) {
    return todoRepository.findByTaskExecutionIdAndParentIsNull(taskExecutionId).stream()
        .map(todoMapper::toDomain)
        .toList();
  }
}
