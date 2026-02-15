package com.example.hypocaust.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TodoListTest {

  private TodoList list;

  @BeforeEach
  void setUp() {
    list = new TodoList();
  }

  @Test
  void setTodos_nullParent_setsTopLevel() {
    var a = new Todo("Task A", TodoStatus.PENDING);
    var b = new Todo("Task B", TodoStatus.PENDING);

    list.setTodos(null, List.of(a, b));

    assertThat(list.getTopLevel()).hasSize(2);
    assertThat(list.getTopLevel().get(0).description()).isEqualTo("Task A");
    assertThat(list.getTopLevel().get(1).description()).isEqualTo("Task B");
  }

  @Test
  void setTodos_withParentId_setsChildren() {
    var parent = new Todo("Parent", TodoStatus.IN_PROGRESS);
    list.setTodos(null, List.of(parent));

    var child1 = new Todo("Child 1", TodoStatus.PENDING);
    var child2 = new Todo("Child 2", TodoStatus.PENDING);
    list.setTodos(parent.id(), List.of(child1, child2));

    var updatedParent = list.getTopLevel().getFirst();
    assertThat(updatedParent.children()).hasSize(2);
    assertThat(updatedParent.children().get(0).description()).isEqualTo("Child 1");
    assertThat(updatedParent.children().get(1).description()).isEqualTo("Child 2");
  }

  @Test
  void updateStatus_changesOnlyTargetTodo() {
    var a = new Todo("A", TodoStatus.PENDING);
    var b = new Todo("B", TodoStatus.PENDING);
    list.setTodos(null, List.of(a, b));

    list.updateStatus(a.id(), TodoStatus.COMPLETED);

    assertThat(list.getTopLevel().get(0).status()).isEqualTo(TodoStatus.COMPLETED);
    assertThat(list.getTopLevel().get(1).status()).isEqualTo(TodoStatus.PENDING);
  }

  @Test
  void updateStatus_deepNesting_updatesLeaf() {
    var root = new Todo("Root", TodoStatus.IN_PROGRESS);
    list.setTodos(null, List.of(root));

    var mid = new Todo("Mid", TodoStatus.IN_PROGRESS);
    list.setTodos(root.id(), List.of(mid));

    var leaf = new Todo("Leaf", TodoStatus.PENDING);
    list.setTodos(mid.id(), List.of(leaf));

    list.updateStatus(leaf.id(), TodoStatus.COMPLETED);

    var updatedLeaf = list.getTopLevel().getFirst()
        .children().getFirst()
        .children().getFirst();
    assertThat(updatedLeaf.status()).isEqualTo(TodoStatus.COMPLETED);
    // Ancestors unchanged
    assertThat(list.getTopLevel().getFirst().status()).isEqualTo(TodoStatus.IN_PROGRESS);
  }

  @Test
  void updateStatus_nonExistentId_treeUnchanged() {
    var a = new Todo("A", TodoStatus.PENDING);
    list.setTodos(null, List.of(a));

    list.updateStatus(UUID.randomUUID(), TodoStatus.COMPLETED);

    assertThat(list.getTopLevel().getFirst().status()).isEqualTo(TodoStatus.PENDING);
  }

  @Test
  void multipleTopLevelWithChildren_structurePreserved() {
    var p1 = new Todo("P1", TodoStatus.IN_PROGRESS);
    var p2 = new Todo("P2", TodoStatus.PENDING);
    list.setTodos(null, List.of(p1, p2));

    var c1 = new Todo("C1", TodoStatus.PENDING);
    list.setTodos(p1.id(), List.of(c1));

    var c2 = new Todo("C2", TodoStatus.PENDING);
    list.setTodos(p2.id(), List.of(c2));

    // Update child of P2
    list.updateStatus(c2.id(), TodoStatus.COMPLETED);

    // P1's children unaffected
    assertThat(list.getTopLevel().get(0).children().getFirst().status())
        .isEqualTo(TodoStatus.PENDING);
    // P2's child updated
    assertThat(list.getTopLevel().get(1).children().getFirst().status())
        .isEqualTo(TodoStatus.COMPLETED);
  }
}
