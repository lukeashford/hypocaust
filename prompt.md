# Artifact Version Control - Implementation Refinements

This document describes refinements needed for the artifact version control system implemented on
this branch. Read `docs/artifact-version-control-concept.md` for the full specification. If there
are conflicts in the descriptions below, this file takes precedence over the concept document.

---

## 1. Create TaskProgressController

The route `Routes.TASK_EXECUTION_TASKS` exists but no controller handles it.

Create `TaskProgressController.java`:

- `GET /task-executions/{id}/tasks` → returns the `TaskTree` for that execution
- The `TaskTree` is available via `TaskExecutionContextHolder` during execution
- Since this needs to be available for completed TaskExecutions, also make this an entity with
  everything that implies (repository and all). Make the best possible decision as the experienced
  developer you are, how to get from the ephemeral system to a persisted one. That could be only
  persisting on execution completion or with every update to the todolist

---

2. Reorganize Ledger Documentation + Fix Todo Field

2a. Move ledger technical documentation to InvokeTool

Currently, DecompositionFragments contains both:

- Conceptual guidance: when to decompose, how to think about subtasks, decomposition strategy
- Technical specification: the OperatorLedger JSON structure, field names, formatting rules

Move the technical specification to InvokeTool's class javadoc or @Tool description. This includes:

- The OperatorLedger record structure (summary, next)
- The ChildConfig structure (operator, inputs, todo)
- JSON formatting requirements
- The @artifact:name reference syntax

Keep the conceptual guidance in DecompositionFragments:

- When to decompose vs. execute directly
- How to analyze a task and identify subtasks
- References to "use the InvokeTool" when describing how to express decomposition

This way, when someone edits InvokeTool, they'll see and update the ledger contract. The DO prompts
focus on strategy, not syntax.
This isn't a task to rewrite the instructions. Actually, what we have works very well. This is just
a refactor. Don't change the facts, just where it's described.

2b. DecomposingOperator must populate todo
Update DecompositionFragments to instruct the LLM that each child task needs a todo field - a short,
human-readable description for the progress UI (e.g., "Generate hero portrait", "Edit background to
add sunset"). The prompt should explain why this matters (user visibility), while the technical
schema in InvokeTool shows where it goes.

---

## 3. Name Generation in PendingChanges.add()

Move name generation from operators into the `add()` method itself.

Current flow:

1. Operator calls `TaskExecutionContext.addArtifact(pendingArtifact)` with a name
2. Name must be unique

New flow:

1. Operator calls `addArtifact(pendingArtifact)` with `name = null` and a `description`
2. `PendingChanges.add()` generates the name:

- Call small LLM (Haiku) with: "Generate snake_case name for: {description}. Exclude:
  {attemptedNames}"
- If name collides with existing or pending artifacts, add to `attemptedNames` and retry
- Return the generated name to the caller

This centralizes naming logic and handles conflicts automatically.

---

## 4. InvokeTool Artifact Reference Resolution

When an operator ledger contains `{"inputName": "@artifact:myArtifact"}`, `InvokeTool` must resolve
this to the actual artifact structure before passing to the child operator.

Current behavior (`InvokeTool.java:177-182`): just extracts the name string.

Required behavior:

```java
private Object resolveArtifact(String artifactRef) {
  String name = artifactRef.substring(ARTIFACT_PREFIX.length()).trim();
  // Get artifact from current state (pending or committed)
  ArtifactEntity artifact = TaskExecutionContextHolder.getContext()
      .getArtifactByName(name);  // Need to add this method

  // Return full structure including metadata
  return Map.of(
      "name", artifact.getName(),
      "kind", artifact.getKind(),
      "description", artifact.getDescription(),
      "storageKey", artifact.getStorageKey(),
      "content", artifact.getContent(),
      "metadata", artifact.getMetadata()
      // ... all relevant fields
  );
}
```

Add `getArtifactByName(String name)` to `TaskExecutionContext` that checks pending artifacts first,
then queries committed artifacts via the version service.

---

## 5. Emit artifact.removed on Materialize

In `ArtifactVersionManagementService`, when materializing pending changes, emit `artifact.removed`
events for any cancelled pending artifacts.

Current `complete()` method skips cancelled artifacts silently. Add:

```
for (String cancelledName : pending.getCancelled()) {
    // Emit event via callback
    onArtifactRemoved.accept(new ArtifactRemovedEventData(cancelledName));
}
```

This requires passing an event callback to `materialize()` or having the service publish events
directly.

---

## 6. Refactor Callback Injection

`TaskExecutionContext` currently uses 6+ setter methods for callbacks, configured verbosely in
`TaskService.executeTask()`.

Refactor to use a config object:

```java
public record TaskExecutionContextConfig(
    Consumer<ArtifactAddedEventData> onArtifactAdded,
    Consumer<ArtifactUpdatedEventData> onArtifactUpdated,
    Consumer<ArtifactRemovedEventData> onArtifactRemoved,
    Consumer<TaskTree> onTaskProgressUpdated,
    Function<String, Boolean> artifactExistsChecker,
    Function<String, Optional<Kind>> artifactKindGetter,
    Function<Void, Set<String>> artifactNamesGetter,
    Function<NameGenerationRequest, String> nameGenerator,
    Function<Void, List<ArtifactEntity>> currentArtifactsGetter
) {

}

// Usage:
var config = new TaskExecutionContextConfig(
    data -> eventService.publish(new ArtifactAddedEvent(...)),
data ->eventService.

publish(new ArtifactUpdatedEvent(...)),
    // ...
    );
var context = new TaskExecutionContext(projectId, taskExecutionId, predecessorId, config);
```

---

## 7. Split materialize() from completeExecution()

### Current structure (problematic):

- `ArtifactVersionManagementService.complete()` does everything: materialize artifacts, generate
  commit message, update TaskExecution status
- `TaskService` calls `complete()` then calls `generateMessage()` again (duplicate!)

### New structure:

**ArtifactVersionManagementService:**

```java
/**
 * Materialize pending changes to database. Does NOT complete the execution.
 * @return TaskExecutionDelta describing what changed
 */
public TaskExecutionDelta materialize(UUID taskExecutionId, PendingChanges pending) {
  // Create artifact records for added/edited
  // Mark deleted artifacts
  // Emit artifact.removed for cancelled
  // Return delta (don't save to TaskExecution yet)
}
```

**TaskService:**

```java

@Transactional
public void completeExecution(UUID taskExecutionId, String task, PendingChanges pending) {
  TaskExecutionDelta delta = versionService.materialize(taskExecutionId, pending);

  String message = null;
  if (delta.hasChanges()) {
    message = versionService.generateMessage(task);
  }

  TaskExecutionEntity execution = taskExecutionRepository.findById(taskExecutionId).orElseThrow();
  execution.complete(message, delta);
  taskExecutionRepository.save(execution);

  eventService.publish(new TaskExecutionCompletedEvent(projectId, delta.hasChanges(), message));
}
```

This fixes the duplicate message generation and gives `TaskService` proper transactional control.

---

## 8. ArtifactEntity Field Cleanup

**Keep `title`**: Operators should set this when creating artifacts (same time as `description`).
The `title` is user-facing display text, while `name` is the programmatic identifier.

**Remove these legacy fields:**

- `subtitle` - unused
- `alt` - unused
- `mime` - can be derived from `kind` or `storageKey` extension

Update `PendingArtifact` to include `title` if not already present. Operators like
`ImageGenerationOperator` should set both `title` and `description`.

---

## 9. Fix Check-Then-Act Race Condition

`TaskExecutionContext` has patterns like:

```
if (pending.isPending(name)) {
    pending.updatePendingArtifact(name, newVersion);
}
```

While `PendingChanges` methods are individually synchronized, this compound operation is not atomic.

**Fix**: Add atomic compound methods to `PendingChanges`:

```java
public synchronized boolean updateIfPending(String name, PendingArtifact newVersion) {
  if (isPending(name)) {
    updatePendingArtifact(name, newVersion);
    return true;
  }
  return false;
}

```

Update `OperatorLedger` to include a `status` field. Update `DecompositionFragments` prompts to
require this field. Then check `ledger.status()` instead of parsing the summary.

---

## 10. ProjectContextTool Validation

Add input validation to `ProjectContextTool.ask()`:

```java
public String ask(String question) {
  if (question == null || question.isBlank()) {
    throw new IllegalArgumentException("Question cannot be empty");
  }
  // ... rest of method
}
```

Also consider adding a max length check to prevent extremely long questions from being sent to the
LLM.

---

## 11. Use ModelRegistry Consistently

**ImageGenerationOperator** and **ImageEditOperator** have hardcoded model strings:

```java
private static final String IMAGE_MODEL = "dall-e-3";
```

Refactor to use `ModelRegistry`:

```java

@RequiredArgsConstructor
public class ImageGenerationOperator extends BaseOperator {

  private final ModelRegistry modelRegistry;

  // Use modelRegistry.get(ImageModelSpec.DALL_E_3) or similar
}
```

Since image models aren't in the registry yet, add them following the pattern of
`AnthropicChatModelSpec`.

---

## 12. Refactor InvokeTool Loop Repetition

The `invoke()` method has repetitive conditional blocks for `if (!singleChild)` scattered throughout
the loop.

**Current pattern:**

```
for (int i = 0; i < children.size(); i++) {
    String childPath = singleChild ? todoPath : todoPath + "." + i;
    // ... setup ...
    if (!singleChild) {
        TaskExecutionContextHolder.getContext().updateTaskStatus(childPath, IN_PROGRESS);
    }
    // ... execute ...
    if (!singleChild) {
        TaskExecutionContextHolder.getContext().updateTaskStatus(childPath, COMPLETED);
    }
}
```

**Refactor**: Extract a helper method that captures `singleChild` and `childPath`:

```java
private void invokeChildren(List<ChildConfig> children, String todoPath) {
  boolean singleChild = children.size() == 1;

  for (int i = 0; i < children.size(); i++) {
    String childPath = singleChild ? todoPath : todoPath + "." + i;
    invokeChild(children.get(i), childPath, singleChild);
  }
}

private void invokeChild(ChildConfig child, String childPath, boolean singleChild) {
  updateStatus(childPath, singleChild, IN_PROGRESS);
  try {
    // ... execute child ...
    updateStatus(childPath, singleChild, COMPLETED);
  } catch (Exception e) {
    updateStatus(childPath, singleChild, FAILED);
    throw e;
  }
}

private void updateStatus(String path, boolean singleChild, TaskStatus status) {
  if (!singleChild) {
    TaskExecutionContextHolder.getContext().updateTaskStatus(path, status);
  }
}
```

This eliminates the repeated conditionals and makes the status update logic consistent.

---

## Summary Checklist

- [ ] Create `TaskProgressController`
- [ ] Move ledger docs to `InvokeTool`, update DO prompts to require `todo`
- [ ] Move name generation into `PendingChanges.add()` with retry loop
- [ ] Implement full artifact resolution in `InvokeTool` for `@artifact:` refs
- [ ] Emit `artifact.removed` for cancelled artifacts during materialize
- [ ] Refactor callback injection to use config object
- [ ] Split `materialize()` from `completeExecution()`, fix duplicate message gen
- [ ] Keep `title` in ArtifactEntity, remove `subtitle`/`alt`/`mime`
- [ ] Fix check-then-act with atomic compound methods
- [ ] Add validation to `ProjectContextTool.ask()`
- [ ] Use `ModelRegistry` for image model references
- [ ] Refactor `InvokeTool` loop with helper method for status updates
