package com.example.hypocaust.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.db.TodoEntity;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TodoMapperTest {

  private final TodoMapper todoMapper = new TodoMapperImpl();

  @Test
  void shouldMapRecursiveTodosToEntities() {
    // Given
    UUID taskExecutionId = UUID.randomUUID();
    Todo child = new Todo("Child task", TodoStatus.PENDING);
    Todo parent = Todo.builder()
        .description("Parent task")
        .status(TodoStatus.IN_PROGRESS)
        .children(List.of(child))
        .build();

    // When
    TodoEntity parentEntity = todoMapper.toEntity(parent, taskExecutionId);

    // Then
    assertThat(parentEntity.getDescription()).isEqualTo("Parent task");
    assertThat(parentEntity.getTaskExecutionId()).isEqualTo(taskExecutionId);
    assertThat(parentEntity.getChildren()).hasSize(1);

    TodoEntity childEntity = parentEntity.getChildren().get(0);
    assertThat(childEntity.getDescription()).isEqualTo("Child task");
    assertThat(childEntity.getTaskExecutionId()).isEqualTo(taskExecutionId);
    assertThat(childEntity.getParent()).isEqualTo(parentEntity);
  }

  @Test
  void shouldMapEntitiesToDomain() {
    // Given
    TodoEntity childEntity = TodoEntity.builder()
        .description("Child task")
        .status(TodoStatus.COMPLETED)
        .build();
    TodoEntity parentEntity = TodoEntity.builder()
        .description("Parent task")
        .status(TodoStatus.COMPLETED)
        .children(List.of(childEntity))
        .build();
    childEntity.setParent(parentEntity);

    // When
    Todo parent = todoMapper.toDomain(parentEntity);

    // Then
    assertThat(parent.description()).isEqualTo("Parent task");
    assertThat(parent.children()).hasSize(1);
    assertThat(parent.children().get(0).description()).isEqualTo("Child task");
  }
}
