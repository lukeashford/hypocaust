package com.example.hypocaust.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TodoListUpsertTest {

  @Test
  void shouldAddTodoIfNotExist() {
    TodoList list = new TodoList();
    Todo todo = new Todo("Task 1", TodoStatus.PENDING);

    Todo effective = list.addOrUpdateTodo(null, todo);

    assertEquals(1, list.getTopLevel().size());
    assertEquals(todo.id(), effective.id());
    assertEquals("Task 1", effective.description());
  }

  @Test
  void shouldReturnExistingTodoIfMatchesDescription() {
    TodoList list = new TodoList();
    Todo todo1 = new Todo("Task 1", TodoStatus.PENDING);
    list.addOrUpdateTodo(null, todo1);

    Todo todo2 = new Todo("Task 1", TodoStatus.PENDING);
    Todo effective = list.addOrUpdateTodo(null, todo2);

    assertEquals(1, list.getTopLevel().size());
    assertEquals(todo1.id(), effective.id());
    assertNotEquals(todo2.id(), effective.id());
  }

  @Test
  void shouldAddUnderParentIfNotExist() {
    TodoList list = new TodoList();
    Todo parent = new Todo("Parent", TodoStatus.PENDING);
    list.addOrUpdateTodo(null, parent);

    Todo child = new Todo("Child", TodoStatus.PENDING);
    Todo effective = list.addOrUpdateTodo(parent.id(), child);

    Todo updatedParent = list.getTopLevel().getFirst();
    assertEquals(1, updatedParent.children().size());
    assertEquals(child.id(), effective.id());
    assertEquals(child.id(), updatedParent.children().getFirst().id());
  }

  @Test
  void shouldReturnExistingChildUnderParent() {
    TodoList list = new TodoList();
    Todo parent = new Todo("Parent", TodoStatus.PENDING);
    list.addOrUpdateTodo(null, parent);

    Todo child1 = new Todo("Child", TodoStatus.PENDING);
    list.addOrUpdateTodo(parent.id(), child1);

    Todo child2 = new Todo("Child", TodoStatus.PENDING);
    Todo effective = list.addOrUpdateTodo(parent.id(), child2);

    Todo updatedParent = list.getTopLevel().getFirst();
    assertEquals(1, updatedParent.children().size());
    assertEquals(child1.id(), effective.id());
  }

  @Test
  void shouldPreserveStatusOfExistingTodo() {
    TodoList list = new TodoList();
    Todo todo = new Todo("Task 1", TodoStatus.COMPLETED);
    list.addOrUpdateTodo(null, todo);

    Todo update = new Todo("Task 1", TodoStatus.PENDING);
    Todo effective = list.addOrUpdateTodo(null, update);

    assertEquals(TodoStatus.COMPLETED, effective.status());
  }
}
