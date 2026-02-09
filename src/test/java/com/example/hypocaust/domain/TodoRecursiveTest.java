package com.example.hypocaust.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TodoRecursiveTest {

  @Test
  void shouldRecursivelySetChildren() {
    // Given
    Todo parent = new Todo("Parent", TodoStatus.IN_PROGRESS);
    TodoList list = new TodoList();
    list.setTodos(null, List.of(parent));

    Todo newChild1 = new Todo("New Child 1", TodoStatus.IN_PROGRESS);
    Todo newChild2 = new Todo("New Child 2", TodoStatus.IN_PROGRESS);

    // When adding children to parent
    list.setTodos(parent.id(), List.of(newChild1, newChild2));

    // Then
    Todo updatedParent = list.getTopLevel().get(0);
    assertEquals(2, updatedParent.children().size());
    assertEquals("New Child 1", updatedParent.children().get(0).description());
    assertEquals("New Child 2", updatedParent.children().get(1).description());

    // Status check
    assertEquals(TodoStatus.IN_PROGRESS, updatedParent.status());
  }
}
