package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.db.TodoEntity;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoList;
import com.example.hypocaust.domain.TodoStatus;
import com.example.hypocaust.mapper.TodoMapperImpl;
import com.example.hypocaust.repo.TodoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TodoService.class, TodoMapperImpl.class})
class TodoServiceIntegrationTest {

  @Autowired
  private TodoService todoService;

  @Autowired
  private TodoRepository todoRepository;

  @Test
  void shouldMaterializeRecursiveTodos() {
    // Given
    UUID taskExecutionId = UUID.randomUUID();
    Todo child = new Todo("Child task", TodoStatus.PENDING);
    Todo parent = Todo.builder()
        .description("Parent task")
        .status(TodoStatus.IN_PROGRESS)
        .children(List.of(child))
        .build();

    TodoList todoList = new TodoList();
    todoList.setTodos(null, List.of(parent));

    // When
    todoService.materialize(todoList, taskExecutionId);

    // Then
    List<TodoEntity> rootEntities = todoRepository.findByTaskExecutionIdAndParentIsNull(
        taskExecutionId);
    assertThat(rootEntities).hasSize(1);

    TodoEntity rootEntity = rootEntities.get(0);
    assertThat(rootEntity.getDescription()).isEqualTo("Parent task");
    assertThat(rootEntity.getTaskExecutionId()).isEqualTo(taskExecutionId);
    assertThat(rootEntity.getChildren()).hasSize(1);

    TodoEntity childEntity = rootEntity.getChildren().get(0);
    assertThat(childEntity.getDescription()).isEqualTo("Child task");
    assertThat(childEntity.getTaskExecutionId()).isEqualTo(taskExecutionId);
    assertThat(childEntity.getParent()).isEqualTo(rootEntity);
  }

  @Test
  void shouldGetTodosForTaskExecution() {
    // Given
    UUID taskExecutionId = UUID.randomUUID();
    Todo child = new Todo("Child task", TodoStatus.COMPLETED);
    Todo parent = Todo.builder()
        .description("Parent task")
        .status(TodoStatus.COMPLETED)
        .children(List.of(child))
        .build();

    TodoList todoList = new TodoList();
    todoList.setTodos(null, List.of(parent));
    todoService.materialize(todoList, taskExecutionId);

    // When
    List<Todo> result = todoService.getTodosForTaskExecution(taskExecutionId);

    // Then
    assertThat(result).hasSize(1);
    Todo rootTodo = result.get(0);
    assertThat(rootTodo.description()).isEqualTo("Parent task");
    assertThat(rootTodo.children()).hasSize(1);
    assertThat(rootTodo.children().get(0).description()).isEqualTo("Child task");
  }
}
