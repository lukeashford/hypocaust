# Persisted Todo Implementation

This document describes the implementation plan for persisting the todo list, following a similar
pattern to artifacts but simpler (no version history or alteration needed).

---

## Overview

**Goal:** Persist the todo list so it's available for completed TaskExecutions.

**Pattern:**

- During execution (RUNNING): return from in-memory `TodoList` via context
- After completion (COMPLETED): return from persisted `TodoEntity` records

---

## Naming Convention

| Old Name                   | New Name               |
|----------------------------|------------------------|
| `TaskProgressEntity`       | `TodoEntity`           |
| `TaskProgressRepository`   | `TodoRepository`       |
| `TaskProgressController`   | `TodoController`       |
| `TaskProgressService`      | `TodoService`          |
| `TaskProgressUpdatedEvent` | `TodoListUpdatedEvent` |
| `TaskItem`                 | `Todo` (domain record) |
| `TaskTree`                 | `TodoList`             |
| `task_id` / `taskId`       | `path`                 |
| `TaskItem.id`              | `Todo.path`            |

---

## Implementation Tasks

### 1. Database Migration

**File:** `V8__todo_table.sql`

```sql
CREATE TABLE todo
(
    id                uuid PRIMARY KEY,
    created_at        timestamptz NOT NULL DEFAULT now(),
    task_execution_id uuid        NOT NULL REFERENCES task_execution (id) ON DELETE CASCADE,
    path              text        NOT NULL,
    description       text        NOT NULL,
    status            text        NOT NULL CHECK (status IN
                                                  ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED',
                                                   'CANCELLED')),
    UNIQUE (task_execution_id, path)
);
CREATE INDEX idx_todo_execution ON todo (task_execution_id);
```

---

### 2. Extend TaskExecutionContextHolder

**File:** `TaskExecutionContextHolder.java` (modify)

Add a concurrent map for cross-thread lookups by taskExecutionId:

```java
private static final ConcurrentHashMap<UUID, TaskExecutionContext> contextsByExecution = new ConcurrentHashMap<>();

public static void setContext(TaskExecutionContext ctx) {
  contextHolder.set(ctx);
  operatorDepth.set(0);
  // Register for cross-thread lookup
  contextsByExecution.put(ctx.getTaskExecutionId(), ctx);
}

public static void clear() {
  TaskExecutionContext ctx = contextHolder.get();
  if (ctx != null) {
    contextsByExecution.remove(ctx.getTaskExecutionId());
  }
  contextHolder.remove();
  operatorDepth.remove();
}

// New method for cross-thread access
public static Optional<TaskExecutionContext> getContextByTaskExecutionId(UUID taskExecutionId) {
  return Optional.ofNullable(contextsByExecution.get(taskExecutionId));
}
```

---

### 3. Rename Domain Record

**File:** `Todo.java` (rename from `TaskItem.java`)

```java
public record Todo(
    String path,        // was "id" - hierarchical path e.g. "0", "0.1", "0.1.2"
    String description,
    TaskStatus status
) {

  public Todo withStatus(TaskStatus newStatus) {
    return new Todo(path, description, newStatus);
  }
}
```

---

### 4. Rename In-Memory Structure

**File:** `TodoList.java` (rename from `TaskTree.java`)

Update all references from `TaskItem` -> `Todo` and `id` -> `path`:

- `Map<String, Todo> todos` (was `tasks`)
- `getParentPath(String path)` (was `getParentPath(String taskId)`)

---

### 5. Rename Entity

**File:** `TodoEntity.java` (rename from `TaskProgressEntity.java`)

```java

@Entity
@Table(name = "todo")
public class TodoEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID taskExecutionId;

  @Column(nullable = false)
  private String path;  // was taskId

  @Column(nullable = false, length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;
}
```

---

### 6. Rename Repository

**File:** `TodoRepository.java` (rename from `TaskProgressRepository.java`)

```java
public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

  List<TodoEntity> findByTaskExecutionIdOrderByPathAsc(UUID taskExecutionId);

  Optional<TodoEntity> findByTaskExecutionIdAndPath(UUID taskExecutionId, String path);

  void deleteByTaskExecutionId(UUID taskExecutionId);
}
```

---

### 7. Create TodoService

**File:** `TodoService.java` (new)

```java

@Service
@RequiredArgsConstructor
public class TodoService {

  private final TodoRepository todoRepository;

  public List<Todo> getTodosForTaskExecution(UUID taskExecutionId) {
    // 1. Check if execution is still running
    Optional<TaskExecutionContext> activeContext =
        TaskExecutionContextHolder.getContextByTaskExecutionId(taskExecutionId);

    if (activeContext.isPresent()) {
      // Return from in-memory TodoList
      return activeContext.get().getTodoList().getAllTodos();
    }

    // 2. Return from database
    return todoRepository.findByTaskExecutionIdOrderByPathAsc(taskExecutionId)
        .stream()
        .map(e -> new Todo(e.getPath(), e.getDescription(), e.getStatus()))
        .toList();
  }

  @Transactional
  public void materialize(TodoList todoList, UUID taskExecutionId) {
    for (Todo todo : todoList.getAllTodos()) {
      TodoEntity entity = TodoEntity.builder()
          .taskExecutionId(taskExecutionId)
          .path(todo.path())
          .description(todo.description())
          .status(todo.status())
          .build();
      todoRepository.save(entity);
    }
  }
}
```

---

### 8. Rename Event

**File:** `TodoListUpdatedEvent.java` (rename from `TaskProgressUpdatedEvent.java`)

```java
public record TodoListUpdatedEvent(UUID projectId, TodoList todoList) implements DomainEvent {

}
```

---

### 9. Update TaskExecutionContext

**File:** `TaskExecutionContext.java` (modify)

- Rename `taskProgress` -> `todoList` (type `TodoList`)
- Rename `getTaskTree()` -> `getTodoList()`
- Rename `publishSubtasks()` -> `publishTodos()`
- Rename `updateTaskStatus()` -> `updateTodoStatus()`
- Update callback type: `Consumer<TodoList> onTodoListUpdated`

---

### 10. Wire Into TaskService

**File:** `TaskService.java` (modify)

```java
// Inject TodoService
private final TodoService todoService;

// In commitExecution():
todoService.

materialize(context.getTodoList(),taskExecutionId);
```

---

### 11. Update Controller

**File:** `TodoController.java` (rename from `TaskProgressController.java`)

```java

@RestController
@RequiredArgsConstructor
public class TodoController {

  private final TodoService todoService;

  @GetMapping(Routes.TASK_EXECUTION_TODOS)  // update route constant
  public ResponseEntity<List<Todo>> getTodos(@PathVariable UUID taskExecutionId) {
    return ResponseEntity.ok(todoService.getTodosForTaskExecution(taskExecutionId));
  }
}
```

---

## File Summary

| File                              | Action                                                    |
|-----------------------------------|-----------------------------------------------------------|
| `V8__todo_table.sql`              | Create new migration                                      |
| `TaskExecutionContextHolder.java` | Add concurrent map for cross-thread lookup                |
| `Todo.java`                       | Rename from `TaskItem.java`, `id` -> `path`               |
| `TodoList.java`                   | Rename from `TaskTree.java`                               |
| `TodoEntity.java`                 | Rename from `TaskProgressEntity.java`, `taskId` -> `path` |
| `TodoRepository.java`             | Rename from `TaskProgressRepository.java`                 |
| `TodoService.java`                | Create new service                                        |
| `TodoListUpdatedEvent.java`       | Rename from `TaskProgressUpdatedEvent.java`               |
| `TaskExecutionContext.java`       | Rename fields/methods to use Todo naming                  |
| `TaskService.java`                | Wire TodoService, call materialize                        |
| `TodoController.java`             | Rename from `TaskProgressController.java`, use service    |
| `EventMapper.java`                | Update event mapping                                      |
| All operator files                | Update to use new naming                                  |

---

## Summary Checklist

- [ ] Create database migration `V8__todo_table.sql`
- [ ] Extend `TaskExecutionContextHolder` with cross-thread lookup
- [ ] Rename `TaskItem` -> `Todo` with `id` -> `path`
- [ ] Rename `TaskTree` -> `TodoList`
- [ ] Rename `TaskProgressEntity` -> `TodoEntity` with `taskId` -> `path`
- [ ] Rename `TaskProgressRepository` -> `TodoRepository`
- [ ] Create `TodoService` with `getTodosForTaskExecution()` and `materialize()`
- [ ] Rename `TaskProgressUpdatedEvent` -> `TodoListUpdatedEvent`
- [ ] Update `TaskExecutionContext` to use Todo naming
- [ ] Wire `TodoService` into `TaskService.commitExecution()`
- [ ] Rename `TaskProgressController` -> `TodoController`, use service
- [ ] Update `EventMapper` for renamed event
- [ ] Update all operator files for new naming
