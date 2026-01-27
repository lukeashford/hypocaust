# Artifact Version Control - Final Concept

This document describes the simplified artifact version control system for Hypocaust. It replaces
the overly complex branch-based system with a streamlined TaskExecution graph approach.

---

## Core Philosophy

1. **Stateless backend**: No concept of "checkout". The backend knows the full TaskExecution graph;
   the frontend tells it which TaskExecution to start from.
2. **Name-based artifacts**: Artifacts have semantic file names (e.g., `protagonists_dog`, `script`)
   instead of UUIDs for LLM-friendly references.
3. **TaskExecution-time persistence**: Operators don't save artifacts during execution. They record
   intentions in a diff; the TaskExecution completion process downloads and stores files.
4. **Everything is a task**: All operations (create, edit, delete) flow through the
   DecomposingOperator. No direct REST manipulation of artifacts.
5. **Lean implementation**: Projects usually have ~10 artifacts. No need for semantic embeddings.
6. **Unified model**: A TaskExecution that produces changes automatically creates a versioned
   snapshot. No separate "commit" concept - the TaskExecution itself is the unit of version control,
   stored with its predecessor and diff.

---

## Data Model

### Project

Unchanged. A project is a container for TaskExecutions and artifacts.

### TaskExecution

A TaskExecution belongs to a project and executes a task. If there are artifact changes, the
TaskExecution completion automatically creates a versioned snapshot with a diff.

```java
// TaskExecutionEntity.java
@Entity
class TaskExecutionEntity extends BaseEntity {

  UUID projectId;           // Existing
  String task;              // Existing
  Status status;            // Existing: QUEUED, RUNNING, COMPLETED, FAILED
  String reason;            // Existing
  Instant startedAt;        // Existing
  Instant completedAt;      // Existing

  // VERSION CONTROL FIELDS:
  UUID predecessorId;       // The TaskExecution this one started from (null for first)
  String commitMessage;           // Auto-generated summary of changes (null if no changes)
  TaskExecutionDelta delta; // What changed in this TaskExecution (null if no changes)
}
```

**Key points**:

- `predecessorId` tracks where the TaskExecution started from (which TaskExecution's state)
- `commitMessage` and `delta` are set atomically with status=COMPLETED when there are changes
- A TaskExecution with `delta != null` represents a versioned snapshot
- No separate Commit entity - the TaskExecution itself captures version control information

### TaskExecutionDelta

Records what changed in a TaskExecution. Only present if there were artifact changes.

```java
record TaskExecutionDelta(
    List<ArtifactChange> added,     // New artifacts
    List<ArtifactChange> edited,    // New versions of existing artifacts
    List<String> deleted            // Artifact names marked as deleted
)

record ArtifactChange(
    String name           // e.g., "protagonists_dog"
)
```

**Key points**:

- Uses artifact names, not UUIDs
- `edited` replaces `updated` with clearer semantics
- No version number stored per artifact - version history is derived from the TaskExecution graph

### Artifact

```java
record Artifact(
    UUID id,
    UUID projectId,
    UUID taskExecutionId, // The TaskExecution that created this version

    // Identity
    String name,          // Semantic file name: "protagonists_dog", "script"

    // Content
    Kind kind,            // IMAGE, STRUCTURED_JSON, PDF, AUDIO, VIDEO
    String storageKey,    // For file-based artifacts
    JsonNode content,     // For inline content

    // Metadata
    String description,   // Full description of what's in the artifact
    String prompt,        // The prompt used to generate it
    String model,         // The model used
    JsonNode metadata,    // Additional technical metadata

    // Status
    boolean deleted,      // Soft-delete flag
    Status status         // SCHEDULED, CREATED, CANCELLED
)
```

**Key changes**:

- `name` + `taskExecutionId` identifies the artifact
- Added `description`, `prompt`, `model` for full provenance
- Added `deleted` flag for soft deletes
- Removed: `version`, `anchorDescription`, `anchorRole`, `anchorTags`, `supersededById`, `branchId`,
  `derivedFrom`

---

## Services

### ArtifactVersionManagementService

Central service for all version control operations.

```java

@Service
class ArtifactVersionManagementService {

  // === TaskExecution Completion Operations ===

  /**
   * Complete a TaskExecution with its pending changes.
   * Called by TaskService at TaskExecution completion.
   *
   * @param taskExecutionId The TaskExecution to complete
   * @param task The original task (for message generation)
   * @param pending The accumulated changes
   * @return The completed TaskExecution, with delta if there were changes
   */
  @Transactional
  TaskExecution complete(UUID taskExecutionId, String task, PendingChanges pending);

  /**
   * Generate a summary message from a task using a small LLM.
   */
  String generateMessage(String task);

  // === Artifact Resolution ===

  /**
   * Get the current artifacts for a TaskExecution by traversing history.
   * Walks from the TaskExecution back through predecessors, progressively building state.
   */
  List<Artifact> getArtifactsAtTaskExecution(UUID taskExecutionId);

  /**
   * Get artifacts for a TaskExecution.
   * If TaskExecution is completed with changes: return artifacts at that snapshot.
   * If TaskExecution is in progress: return predecessor artifacts adjusted by pending changes.
   */
  List<Artifact> getArtifactsForTaskExecution(UUID taskExecutionId);

  /**
   * Get the most recent completed TaskExecution for a project (with or without changes).
   */
  Optional<TaskExecution> getMostRecentTaskExecution(UUID projectId);

  /**
   * Get full TaskExecution history for a project.
   */
  List<TaskExecution> getTaskExecutionHistory(UUID projectId);

  /**
   * Get all versions of an artifact by name (across TaskExecutions).
   */
  List<Artifact> getVersionHistory(UUID projectId, String artifactName);

  // === Used by completion process ===

  /**
   * Download and store a pending artifact.
   * Called during completion for artifacts that have external URLs.
   * Needs to land in MinIO before the TaskExecution is finalized.
   */
  void materializeArtifact(PendingArtifact pending, UUID taskExecutionId);
}
```

### TaskExecutionContext (Thread-Local)

Replaces both `RunContextHolder` and `ExecutionContext`. Thread-local context for the current
TaskExecution. Also incorporates the task progress tracking (previously in TaskProgressService).

```java
class TaskExecutionContext {

  UUID projectId;
  UUID taskExecutionId;
  UUID predecessorId;            // TaskExecution this one started from
  PendingChanges pending;        // Accumulated changes
  TaskTree taskProgress;         // Hierarchical task progress (thread-local)

  // === Artifact Hooks (called by operators) ===

  /**
   * Schedule a new artifact for creation.
   * Generates a unique name from the description using a small LLM.
   * If the generated name conflicts, retries with an exclusion list until a unique name is found.
   * Emits artifact.added event with name, kind, description, externalUrl, inlineContent, metadata.
   * @return the generated artifact name (for use in subsequent updatePendingArtifact calls)
   */
  String addArtifact(PendingArtifact artifact);

  /**
   * Schedule an edit to an existing artifact (creates new version).
   * Emits artifact.updated event with name, kind, description, externalUrl, inlineContent, metadata.
   * @throws ArtifactNotFoundException if name doesn't exist in predecessor's state
   * @throws ArtifactTypeMismatchException if pending artifact kind differs from existing
   */
  void editArtifact(String name, PendingArtifact newVersion);

  /**
   * Schedule an artifact for deletion (soft delete).
   * Emits artifact.removed event with name.
   * @throws ArtifactNotFoundException if name doesn't exist in predecessor's state
   */
  void deleteArtifact(String name);

  /**
   * Update a pending artifact that was previously scheduled via addArtifact or editArtifact.
   * Does NOT introduce a new change - just updates the pending entry.
   * Emits artifact.updated event with name, kind, description, externalUrl, inlineContent, metadata.
   * Used by operators to update progress (e.g., streaming text tokens, image generation completion).
   * @throws IllegalStateException if no pending artifact with this name exists
   */
  void updatePendingArtifact(String name, PendingArtifact newVersion);

  /**
   * Cancel a pending artifact that was previously scheduled.
   * Emits artifact.removed event with name.
   * @throws IllegalStateException if no pending artifact with this name exists
   */
  void cancelPendingArtifact(String name);

  /**
   * Check if an artifact name exists in current state.
   */
  boolean artifactExists(String name);

  /**
   * Get current artifact state (predecessor, adjusted in this method by pending).
   */
  List<ArtifactDto> getCurrentArtifacts();

  // === Task Progress (thread-local) ===

  /**
   * Publish subtasks for a path.
   * Called by operators to declare their planned work.
   */
  void publishSubtasks(String pathPrefix, List<TaskItem> subtasks);

  /**
   * Update a task's status.
   */
  void updateTaskStatus(String taskId, TaskStatus status);

  /**
   * Get the full task tree for this TaskExecution.
   */
  TaskTree getTaskTree();
}

record PendingArtifact(
    Kind kind,
    String description,      // Required: used to generate the artifact name
    String prompt,
    String model,
    String externalUrl,      // URL to download from (for images)
    JsonNode inlineContent,  // For structured content
    JsonNode metadata
)

record ArtifactDto(
    String name,
    Kind kind,
    String description,
    String url,              // Resolved URL for frontend display

boolean isPending,       // True if from pending changes, not yet persisted
Status status            // SCHEDULED, CREATED, CANCELLED (for pending artifacts)
)
```

### TaskExecutionContextHolder

Simplified thread-local holder.

```java
class TaskExecutionContextHolder {

  private static final ThreadLocal<TaskExecutionContext> context = new ThreadLocal<>();

  static void setContext(TaskExecutionContext ctx);

  static TaskExecutionContext getContext();

  static void clear();

  // Convenience methods
  static UUID getProjectId();

  static UUID getTaskExecutionId();

  static String addArtifact(PendingArtifact artifact);

  static void editArtifact(String name, PendingArtifact newVersion);

  static void deleteArtifact(String name);

  static void updatePendingArtifact(String name, PendingArtifact newVersion);

  static void cancelPendingArtifact(String name);
}
```

### PendingChanges

Accumulator for TaskExecution changes, used by TaskExecutionContext. This is materialized as a
TaskExecutionDelta.

```java
class PendingChanges {

  List<PendingArtifact> added;
  Map<String, PendingArtifact> edited;  // name -> new version
  Set<String> deleted;                   // names to delete
}
```

---

## Tools

### ProjectContextTool

A **tool** (not an operator) that provides project context to operators. Answers questions in
natural language.

```java

@Component
class ProjectContextTool implements Tool {

  private final ArtifactVersionManagementService versionService;
  private final ChatClient chatClient;  // Small model for interpretation

  /**
   * Answer a question about project artifacts or version history.
   *
   * Examples:
   * - "What is this project about?"
   * - "What is the artifact name for the picture of our protagonist wearing a suit?"
   * - "What prompt was used for the forest_background artifact?"
   * - "List all current artifacts"
   * - "Show me the version history of hero_image"
   *
   * We are explicitly limiting your access to version history to save you space in your
   * context. You can ask this anything, even to dump the whole history graph, but if you use this
   * wisely and ask a good question, you get a good answer without blowing up your context.
   */
  String ask(String question) {
    // 1. Get current TaskExecution context
    var ctx = TaskExecutionContextHolder.getContext();

    // 2. Gather relevant data based on question
    var artifacts = versionService.getArtifactsForTaskExecution(ctx.taskExecutionId());
    var history = versionService.getTaskExecutionHistory(ctx.projectId());

    // 3. Build context for LLM
    var prompt = buildPrompt(question, artifacts, history);

    // 4. Call small LLM to interpret and answer
    return chatClient.prompt()
        .system("You answer questions about a creative project's artifacts...")
        .user(prompt)
        .call()
        .content();
  }
}
```

**Usage**: Operators call `projectContextTool.ask("What is the artifact name for the dog picture?")`
to resolve natural language references to artifact names and understand their provenance.

---

## Operator Error Handling

### Failure Wiring Pattern

All operators extend `BaseOperator` which provides consistent error handling. When an operator
fails,
it returns a failure result with a message, allowing parent decomposing operators to compensate or
propagate the failure upward.

```java
abstract class BaseOperator implements Operator {

  /**
   * Template method that wraps execution with error handling.
   */
  public final OperatorResult execute(Map<String, Object> inputs) {
    try {
      return doExecute(inputs);
    } catch (ArtifactNotFoundException e) {
      return OperatorResult.failure("Artifact not found: " + e.getArtifactName());
    } catch (ArtifactExistsException e) {
      return OperatorResult.failure("Artifact already exists: " + e.getArtifactName());
    } catch (ArtifactTypeMismatchException e) {
      return OperatorResult.failure("Type mismatch: " + e.getMessage());
    } catch (ExternalServiceException e) {
      return OperatorResult.failure("External service error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error in operator", e);
      return OperatorResult.failure("Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Subclasses implement this. Can throw exceptions which are caught above.
   */
  protected abstract OperatorResult doExecute(Map<String, Object> inputs);
}
```

---

## Operators

### ImageGenerationOperator (Adjusted)

Creates new images. Calls `TaskExecutionContext.addArtifact()` when scheduling and
`updatePendingArtifact()` when generation completes.

The artifact naming is fully centralized in `TaskExecutionContext.addArtifact()`: the name is
generated from the description using a small LLM, with automatic retry on conflicts. Operators
don't need to worry about name generation or collision handling.

```java

@Override
protected OperatorResult doExecute(Map<String, Object> inputs) {
  var prompt = (String) inputs.get("prompt");
  var description = (String) inputs.get("description");    // Required

  var artifactName = TaskExecutionContextHolder.addArtifact(PendingArtifact.builder()
      .kind(Kind.IMAGE)
      .description(description)
      .prompt(prompt)
      .model("dall-e-3")
      .build());

  // Generate image (async call to image service)
  var imageUrl = generateImage(prompt);

  // Update the pending artifact with the generated URL - emits ARTIFACT_UPDATED event
  TaskExecutionContextHolder.updatePendingArtifact(artifactName, PendingArtifact.builder()
      .kind(Kind.IMAGE)
      .description(description)
      .prompt(prompt)
      .model("dall-e-3")
      .externalUrl(imageUrl)
      .build());

  return OperatorResult.success("Generated image: " + artifactName,
      Map.of("artifactName", artifactName));
}
```

**Key points**:

- `addArtifact()` generates a unique name from the description and returns it
- Name conflict retry loop is internal to `addArtifact()`, not operator's concern
- Calls `addArtifact()` to schedule (returns name), then `updatePendingArtifact()` when generation
  completes
- Both calls emit SSE events so the frontend can track progress

### ImageEditOperator (New)

Edits existing images using inpainting/img2img. Calls `TaskExecutionContext.editArtifact()`.

```java

@Component
class ImageEditOperator extends BaseOperator {

  private final ProjectContextTool projectContext;
  private final ReplicateClient replicateClient;  // For nano banana

  @Override
  protected OperatorResult doExecute(Map<String, Object> inputs) {
    var task = (String) inputs.get("task");  // e.g., "Make the dog black"

    // 1. Resolve which artifact to edit
    var artifactName = projectContext.ask(
        "What is the artifact name for: " + task + "? Reply with just the name.");

    // 2. Get the current artifact
    var artifact = projectContext.getArtifactByName(artifactName)
        .orElseThrow(() -> new ArtifactNotFoundException(artifactName));

    // 3. Schedule edit - emits ARTIFACT_EDIT_SCHEDULED event
    TaskExecutionContextHolder.editArtifact(artifactName, PendingArtifact.builder()
        .name(artifactName)
        .kind(Kind.IMAGE)
        .description(artifact.description())
        .prompt(task)
        .model("nano-banana")
        .build());

    // 4. Edit the image
    var newImageUrl = replicateClient.editImage(
        artifact.storageKey(),
        task,
        artifact.prompt()  // Use original prompt for context
    );

    // 5. Update pending artifact with result - emits ARTIFACT_UPDATED event
    TaskExecutionContextHolder.updatePendingArtifact(artifactName, PendingArtifact.builder()
        .name(artifactName)
        .kind(Kind.IMAGE)
        .description(artifact.description())
        .prompt(task)
        .model("nano-banana")
        .externalUrl(newImageUrl)
        .build());

    return OperatorResult.success("Edited image: " + artifactName,
        Map.of("artifactName", artifactName));
  }
}
```

### DeleteArtifactOperator (New)

Soft-deletes artifacts. Lightweight operator that just marks for deletion.

```java

@Component
class DeleteArtifactOperator extends BaseOperator {

  private final ProjectContextTool projectContext;

  @Override
  protected OperatorResult doExecute(Map<String, Object> inputs) {
    var task = (String) inputs.get("task");  // e.g., "Delete the forest background"

    // Resolve which artifact to delete
    var artifactName = projectContext.ask(
        "What artifact name should be deleted for: " + task + "? Reply with just the name.");

    // Mark for deletion - emits ARTIFACT_DELETE_SCHEDULED event
    // Throws ArtifactNotFoundException if doesn't exist (caught by BaseOperator)
    TaskExecutionContextHolder.deleteArtifact(artifactName);

    return OperatorResult.success("Marked " + artifactName + " for deletion",
        Map.of("artifactName", artifactName));
  }
}
```

---

## Task Progress System

Task progress is managed thread-locally within `TaskExecutionContext`. Subtasks are ledger-scoped:
the DecomposingOperator encodes a short "todo" wording for each child in the ledger alongside the
operator name. The InvokeTool publishes all subtasks at once at the beginning of `invoke()` and
updates their status as children execute.

### Ledger Structure with Todo Wording

The `OperatorLedger` contains children with both the operator name and a human-readable todo:

```java
record OperatorLedgerChild(
    String operatorName,    // e.g., "ImageGenerationOperator"
    String todo,            // e.g., "Generate hero portrait image"
    Map<String, String> inputsToKeys,
    Map<String, String> outputsToKeys
)
```

### BaseOperator Signature Change

To support task path propagation, `BaseOperator.execute` changes signature:

```java
public final OperatorResult execute(Map<String, Object> inputs, String todoPath)
```

The `todoPath` (e.g., "0.1.2") identifies this operator's position in the task tree.

### InvokeTool Integration

The InvokeTool publishes subtasks and manages path propagation:

```java

@Tool(name = "invoke", description = "Invoke a chain of operators, as specified in the ledger")
public OperatorResult invoke(OperatorLedger ledger, String todoPath) {

  // Path propagation logic:
  // - Multiple children: extend the path (e.g., "0.1" → "0.1.0", "0.1.1", ...)
  // - Single child: propagate the same path (no subtask publishing)
  var singleChild = ledger.children().size() == 1;

  if (!singleChild) {
    // Publish all subtasks at once at the beginning
    var subtasks = new ArrayList<TaskItem>();
    for (int i = 0; i < ledger.children().size(); i++) {
      var child = ledger.children().get(i);
      var childPath = todoPath + "." + i;
      subtasks.add(new TaskItem(childPath, child.todo(), PENDING));
    }
    TaskExecutionContextHolder.getContext().publishSubtasks(todoPath, subtasks);
  }

  // Execute children
  for (int i = 0; i < ledger.children().size(); i++) {
    var child = ledger.children().get(i);
    // Single child keeps the same path; multiple children extend it
    var childPath = singleChild ? todoPath : todoPath + "." + i;

    if (!singleChild) {
      TaskExecutionContextHolder.getContext().updateTaskStatus(childPath, IN_PROGRESS);
    }

    var operator = operatorRegistry.get(child.operatorName()).orElseThrow();
    var inputs = resolveInputs(ledger, child);

    // Pass the todoPath to the operator
    var result = operator.execute(inputs, childPath);

    if (!singleChild) {
      var status = result.ok() ? COMPLETED : FAILED;
      TaskExecutionContextHolder.getContext().updateTaskStatus(childPath, status);
    }

    if (!result.ok()) {
      return result;
    }

    // ... store outputs in ledger values ...
  }

  return OperatorResult.success("...", Map.of(), Map.of("result", finalValue));
}
```

**Key points**:

- Single child (DecomposingOperator found a matching operator): path is propagated unchanged,
  no subtasks published (avoids redundant nesting in the UI)
- Multiple children: path is extended (e.g., "0.1" → "0.1.0", "0.1.1"), subtasks published at start
- Each operator receives its `todoPath` so it can propagate further if needed

### TaskItem and TaskStatus

```java
record TaskItem(
    String id,           // Hierarchical: "0", "0.1", "0.1.2"
    String description,  // The "todo" wording from the ledger
    TaskStatus status    // PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
)

enum TaskStatus {
  PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}
```

---

## SSE Events

Events contain sufficient payload for the frontend to update its artifact display without
additional API calls. The philosophy: tell the frontend all info you have so it can add cards,
show skeletons while no external URL or inline content is set, and update artifacts as you go.

**Artifact event simplification**: The frontend only needs to know whether to add a card, change
one, or remove it. Therefore, artifact events are reduced to three types:

- `artifact.added` - a new artifact card should appear
- `artifact.updated` - an existing artifact card should be updated (covers both edits to existing
  artifacts and progress updates to pending artifacts)
- `artifact.removed` - an artifact card should be removed (covers deletion, cancellation, and
  discarding of incomplete artifacts on commit)

```
GET /task-executions/{taskExecutionId}/events

Streams SSE events for the specified TaskExecution. The frontend already knows the taskExecutionId
and projectId from the initial POST /tasks response, so event payloads don't repeat them.

Streams:
- taskexecution.started     { }
- taskexecution.completed   { hasChanges, message }
- taskexecution.failed      { reason }

- artifact.added            { name, kind, description, externalUrl, inlineContent, metadata }
- artifact.updated          { name, description, externalUrl, inlineContent, metadata }
- artifact.removed          { name }

- task.progress.updated     { taskTree }

- operator.started          { operatorName, taskPath }
- operator.finished         { operatorName, taskPath, success, message }
- operator.failed           { operatorName, taskPath, error }

- error                     { message }
```

### Artifact Events

The artifact events directly correspond to `TaskExecutionContext` methods:

| Context Method            | Event Emitted      | When                                         |
|---------------------------|--------------------|----------------------------------------------|
| `addArtifact()`           | `artifact.added`   | New artifact scheduled                       |
| `editArtifact()`          | `artifact.updated` | Edit to existing artifact scheduled          |
| `updatePendingArtifact()` | `artifact.updated` | Pending artifact updated (e.g., URL ready)   |
| `deleteArtifact()`        | `artifact.removed` | Artifact marked for deletion                 |
| `cancelPendingArtifact()` | `artifact.removed` | Pending artifact cancelled                   |
| (on commit)               | `artifact.removed` | Incomplete artifacts discarded during commit |

**Event payload philosophy**: Each event includes all available fields (name, kind, description,
externalUrl, inlineContent, metadata). Fields may be null initially (e.g., externalUrl before
generation completes). The frontend can:

- Show a skeleton card when `artifact.added` arrives with null externalUrl
- Update the card with the actual image when `artifact.updated` arrives with the URL
- Remove cards when `artifact.removed` arrives

---

## API Endpoints

### Projects

```
POST /projects
  Response: { projectId: UUID }

  Creates a new empty project.
```

### Tasks

```
POST /tasks
  Request: {
    projectId: UUID,              // Required
    predecessorId: UUID | null,   // Optional: start from this TaskExecution (default: most recent)
    task: String
  }
  Response: { taskExecutionId: UUID, status: "accepted" }

  Starts a new TaskExecution. Emits TASKEXECUTION_CREATED event with taskExecutionId.
```

### Artifacts

```
GET /task-executions/{taskExecutionId}/artifacts
  Response: List<ArtifactDto>

  Returns artifacts for the TaskExecution:
  - If TaskExecution is completed: artifacts at that snapshot
  - If TaskExecution is in progress: predecessor artifacts + pending changes

GET /artifacts/{artifactId}/content
  Response: Binary content (unchanged)
```

### Task Progress

```
GET /task-executions/{taskExecutionId}/tasks
  Response: TaskTree

  Returns the hierarchical task progress tree.
```

---

## Task Execution Flow

```
1. POST /tasks { projectId, predecessorId?, task }
   └─ Validate projectId exists
   └─ Resolve predecessorId (provided or most recent completed)
   └─ Create TaskExecution (status=QUEUED, predecessorId set)
   └─ Return { taskExecutionId } immediately (frontend subscribes to SSE)

2. EXECUTION (async TaskService.executeTask)
   └─ Set TaskExecutionContext (projectId, taskExecutionId, predecessorId, empty pending)
   └─ Emit TASKEXECUTION_STARTED
   └─ DecomposingOperator.execute(task, "0")  // Root todoPath
   │   └─ Operators call TaskExecutionContext hooks:
   │   │   - addArtifact() → emits artifact.added
   │   │   - editArtifact() → emits artifact.updated
   │   │   - deleteArtifact() → emits artifact.removed
   │   │   - updatePendingArtifact() → emits artifact.updated
   │   │   - cancelPendingArtifact() → emits artifact.removed
   │   └─ Changes accumulate in pending
   └─ On success: complete()
   └─ On failure: fail() and discard all pending

3. COMPLETION (transactional)
   └─ For each cancelled pending artifact:
   │   └─ Emit artifact.removed { name }
   │   └─ Remove from pending
   └─ If pending.hasChanges():
   │   └─ Generate message from task
   │   └─ For each pending artifact:
   │   │   └─ Download external URL (if any)
   │   │   └─ Store to MinIO
   │   │   └─ Create ArtifactEntity
   │   └─ Build TaskExecutionDelta
   │   └─ Update TaskExecution (delta set, message set, status=COMPLETED)
   └─ Else:
   │   └─ Update TaskExecution (status=COMPLETED, no delta/message)
   └─ Emit TASKEXECUTION_COMPLETED
```

---

## Artifact Resolution Algorithm

To get artifacts at a given TaskExecution:

```java
List<Artifact> getArtifactsAtTaskExecution(UUID taskExecutionId) {
  // 1. Build TaskExecution chain from root to target
  var chain = new ArrayList<TaskExecution>();
  var current = taskExecutionId;
  while (current != null) {
    var taskExecution = taskExecutionRepository.findById(current).orElseThrow();
    chain.add(0, taskExecution);  // Prepend to get oldest first
    current = taskExecution.predecessorId();
  }

  // 2. Replay deltas to build current state
  var state = new HashMap<String, Artifact>();  // name -> latest artifact

  for (var taskExecution : chain) {
    var delta = taskExecution.delta();
    if (delta == null)
      continue;  // No changes in this TaskExecution

    // Add new artifacts
    for (var added : delta.added()) {
      var artifact = artifactRepository.findByTaskExecutionIdAndName(
          taskExecution.id(), added.name());
      state.put(added.name(), artifact);
    }

    // Apply edits (new versions)
    for (var edited : delta.edited()) {
      var artifact = artifactRepository.findByTaskExecutionIdAndName(
          taskExecution.id(), edited.name());
      state.put(edited.name(), artifact);
    }

    // Remove deleted
    for (var deleted : delta.deleted()) {
      state.remove(deleted);
    }
  }

  return new ArrayList<>(state.values());
}
```

---

## Changes to Existing Implementation

### Files to DELETE

| File                                     | Reason                                       |
|------------------------------------------|----------------------------------------------|
| `db/BranchEntity.java`                   | No branches in new design                    |
| `db/AnchorEmbeddingEntity.java`          | No semantic embeddings                       |
| `db/ArtifactRelationEntity.java`         | Simplified model doesn't need relations      |
| `db/CommitEntity.java`                   | Merged into TaskExecutionEntity              |
| `domain/Branch.java`                     | No branches                                  |
| `domain/Commit.java`                     | Merged into TaskExecution                    |
| `domain/CommitDelta.java`                | Replaced by TaskExecutionDelta               |
| `domain/ArtifactGraph.java`              | Replaced by simpler list-based approach      |
| `domain/ArtifactNode.java`               | Replaced by simplified Artifact              |
| `domain/ArtifactRelation.java`           | Not needed                                   |
| `domain/ArtifactUpdate.java`             | Replaced by ArtifactChange with names        |
| `domain/ArtifactSnapshot.java`           | Replaced by ArtifactDto                      |
| `domain/RelationType.java`               | No artifact relations in new design          |
| `domain/SemanticAnchor.java`             | Replaced by name + description               |
| `domain/Provenance.java`                 | Merged into Artifact fields                  |
| `domain/ExecutionContext.java`           | Replaced by TaskExecutionContext             |
| `operator/ExecutionContextHolder.java`   | Replaced by TaskExecutionContextHolder       |
| `service/ArtifactGraphService.java`      | Replaced by ArtifactVersionManagementService |
| `service/BranchService.java`             | No branches                                  |
| `service/CommitService.java`             | Merged into ArtifactVersionManagementService |
| `service/TaskProgressService.java`       | Merged into TaskExecutionContext             |
| `repo/BranchRepository.java`             | No branches                                  |
| `repo/CommitRepository.java`             | Merged into TaskExecutionRepository          |
| `repo/AnchorEmbeddingRepository.java`    | No embeddings                                |
| `repo/ArtifactRelationRepository.java`   | No relations                                 |
| `exception/BranchNotFoundException.java` | No branches                                  |
| `exception/AnchorNotFoundException.java` | Replaced by ArtifactNotFoundException        |
| `ledger-prompt.md`                       | Superseded by this document                  |

### Files to MODIFY

| File                                    | Changes                                                                                                                                                                                                         |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `db/RunEntity.java`                     | Rename to `TaskExecutionEntity`, add `predecessorId`, `message`, `delta` fields                                                                                                                                 |
| `db/ArtifactEntity.java`                | Add `name`, `description`, `prompt`, `model`, `deleted`. Remove `version`, `anchorDescription`, `anchorRole`, `anchorTags`, `supersededById`, `branchId`, `derivedFrom`. Change `commitId` to `taskExecutionId` |
| `domain/Run.java`                       | Rename to `TaskExecution`, add `predecessorId`, `message`, `delta` fields                                                                                                                                       |
| `domain/PendingChanges.java`            | Rewrite to use name-based tracking with `added`, `edited`, `deleted`                                                                                                                                            |
| `domain/OperatorLedger.java`            | Add `todo` field to `OperatorLedgerChild` record for human-readable task description                                                                                                                            |
| `operator/RunContextHolder.java`        | Rename to `TaskExecutionContextHolder`, enhance with full TaskExecutionContext, add artifact hooks (add, edit, delete, update, cancel). addArtifact() generates names from description with conflict retry      |
| `operator/BaseOperator.java`            | Change signature to `execute(Map<String, Object> inputs, String todoPath)`. Add error handling wrapper with consistent failure result pattern                                                                   |
| `tool/InvokeTool.java`                  | Publish subtasks at start of invoke(); implement path propagation (single child: same path, multiple children: extend path); pass todoPath to operators; update task status during execution                    |
| `service/TaskService.java`              | Require projectId (throw if missing), handle predecessorId, call complete at end of TaskExecution transactionally                                                                                               |
| `service/ArtifactService.java`          | Remove `schedule()` and `updateArtifact()` methods - keep only read methods (`getArtifact`, `downloadArtifact`)                                                                                                 |
| `dto/CreateTaskRequestDto.java`         | Add `projectId` (required), `predecessorId` (optional)                                                                                                                                                          |
| `dto/TaskResponseDto.java`              | Return `taskExecutionId` instead of (or in addition to) `projectId`                                                                                                                                             |
| `repo/ArtifactRepository.java`          | Add queries by name, by taskExecutionId. Remove anchor-based and branch-based queries                                                                                                                           |
| `repo/RunRepository.java`               | Rename to `TaskExecutionRepository`, add queries by projectId                                                                                                                                                   |
| `operator/ImageGenerationOperator.java` | Use TaskExecutionContext hooks (add + update).                                                                                                                                                                  |
| `operator/DecomposingOperator.java`     | Build ledger with todo wordings for each child; do not publish subtasks directly (InvokeTool handles this)                                                                                                      |
| `domain/event/EventType.java`           | Add new event types: `artifact.added`, `artifact.updated`, `artifact.removed`, `taskexecution.started`, `taskexecution.completed`, `taskexecution.failed`, `task.progress.updated`                              |
| `web/ArtifactController.java`           | Change to accept taskExecutionId, delegate to ArtifactVersionManagementService                                                                                                                                  |
| `web/TaskController.java`               | Update to validate projectId, return taskExecutionId in response                                                                                                                                                |
| `web/EventController.java`              | Change SSE endpoint from `/projects/{projectId}/events` to `/task-executions/{taskExecutionId}/events`                                                                                                          |

### Files to CREATE

| File                                            | Purpose                                            |
|-------------------------------------------------|----------------------------------------------------|
| `service/ArtifactVersionManagementService.java` | Central version control service                    |
| `domain/TaskExecutionContext.java`              | Enhanced context with artifact hooks + progress    |
| `domain/TaskExecutionDelta.java`                | What changed in a TaskExecution                    |
| `domain/PendingArtifact.java`                   | Pending artifact record                            |
| `domain/ArtifactDto.java`                       | Frontend-ready artifact view with pending status   |
| `domain/TaskTree.java`                          | Task progress tree structure                       |
| `domain/TaskItem.java`                          | Single task item with status                       |
| `domain/ArtifactChange.java`                    | Change record for TaskExecution delta              |
| `tool/ProjectContextTool.java`                  | Project context queries for operators              |
| `operator/ImageEditOperator.java`               | Image editing via nano banana                      |
| `operator/DeleteArtifactOperator.java`          | Soft-delete artifacts                              |
| `exception/ArtifactTypeMismatchException.java`  | Thrown when edit type doesn't match                |
| `domain/event/ArtifactAddedEvent.java`          | Event: new artifact scheduled                      |
| `domain/event/ArtifactUpdatedEvent.java`        | Event: artifact edited or pending artifact updated |
| `domain/event/ArtifactRemovedEvent.java`        | Event: artifact deleted, cancelled, or discarded   |
| `domain/event/TaskProgressUpdatedEvent.java`    | Event: task progress tree updated                  |
| `web/ProjectController.java`                    | Project creation endpoint                          |
| `web/TaskProgressController.java`               | Task progress endpoint                             |

### Database Migration

Create `V6__simplify_version_control.sql`:

```sql
-- Remove branch table and related
DROP TABLE if EXISTS anchor_embedding;
DROP TABLE if EXISTS artifact_relation;
DROP TABLE if EXISTS branch CASCADE;
DROP TABLE if EXISTS commit CASCADE;

-- Rename run table to task_execution
ALTER TABLE run RENAME TO task_execution;

-- Modify task_execution table (formerly run)
ALTER TABLE task_execution
    ADD COLUMN if NOT EXISTS predecessor_id UUID REFERENCES task_execution(id);
ALTER TABLE task_execution
    ADD COLUMN if NOT EXISTS message TEXT;
ALTER TABLE task_execution
    ADD COLUMN if NOT EXISTS delta JSONB;

-- Modify artifact table
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS name VARCHAR (100);
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS description TEXT;
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS prompt TEXT;
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS model VARCHAR (100);
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS task_execution_id UUID REFERENCES task_execution(id);

ALTER TABLE artifact DROP COLUMN IF EXISTS version;
ALTER TABLE artifact DROP COLUMN IF EXISTS commit_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS anchor_description;
ALTER TABLE artifact DROP COLUMN IF EXISTS anchor_role;
ALTER TABLE artifact DROP COLUMN IF EXISTS anchor_tags;
ALTER TABLE artifact DROP COLUMN IF EXISTS superseded_by_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS branch_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS derived_from;

-- Add indexes
CREATE INDEX idx_artifact_name ON artifact (project_id, name);
CREATE INDEX idx_artifact_task_execution ON artifact (task_execution_id);
CREATE INDEX idx_task_execution_project ON task_execution (project_id, created_at DESC);
CREATE INDEX idx_task_execution_predecessor ON task_execution (predecessor_id);

-- Update event type constraint
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_type_check;
ALTER TABLE event
    ADD CONSTRAINT event_type_check CHECK (type IN (
                                                    'artifact.added',
                                                    'artifact.updated',
                                                    'artifact.removed',
                                                    'taskexecution.started',
                                                    'taskexecution.completed',
                                                    'taskexecution.failed',
                                                    'task.progress.updated',
                                                    'tool.calling',
                                                    'operator.started', 'operator.finished',
                                                    'operator.failed',
                                                    'error'
        ));
```

---

## Artifact Reference Syntax

The current implementation uses `@anchor:description` to resolve artifacts by semantic description.
This changes to name-based references.

### Current (to be removed)

```
@anchor:woman in red dress at cafe  →  semantic search  →  ArtifactNode
```

### New (to implement)

```
@artifact:hero_image  →  direct name lookup  →  Artifact
```

Or simply use the artifact name directly in inputs without special syntax, since names are now
explicit:

```json
{
  "values": {
    "targetArtifact": "hero_image",
    "editTask": "Make the hero's hair blonde"
  }
}
```

The `InvokeTool` should be simplified to just resolve artifact names from the current state
(predecessor TaskExecution + pending changes) via `TaskExecutionContext.getArtifactByName(name)`.

---

## Implementation Order

1. **Database migration** - Simplify schema, rename run to task_execution
2. **Delete unused files** - Clean up branch/embedding/commit code
3. **Core domain** - TaskExecutionContext, PendingChanges, PendingArtifact, TaskExecutionDelta
4. **ArtifactVersionManagementService** - Completion logic, artifact resolution
5. **TaskExecutionContextHolder enhancement** - Thread-local with hooks + progress
6. **BaseOperator error handling** - Consistent failure wiring
7. **TaskService changes** - projectId required, completion integration
8. **ProjectContextTool** - Context queries for operators
9. **Update ImageGenerationOperator** - Use TaskExecutionContext hooks, generate names
10. **InvokeTool changes** - Task progress updates, name-based artifact resolution
11. **New operators** - ImageEditOperator, DeleteArtifactOperator
12. **API changes** - ProjectController, updated task/artifact endpoints
13. **Events** - New event types with full payload and SSE integration

---

## Files That Stay Unchanged

These files are not affected by this refactoring:

| File                                           | Reason                                                                         |
|------------------------------------------------|--------------------------------------------------------------------------------|
| `db/ProjectEntity.java`                        | No changes needed                                                              |
| `db/EventEntity.java`                          | No changes needed                                                              |
| `domain/ArtifactKind.java`                     | Simple enum, still needed (consider unifying with `ArtifactEntity.Kind` later) |
| `operator/ImagePromptEngineerOperator.java`    | Pure LLM operator, no artifact handling                                        |
| `operator/CreativeTextGenerationOperator.java` | Pure LLM operator, no artifact handling                                        |
| `service/StorageService.java`                  | Storage abstraction unchanged                                                  |
| `service/EmbeddingService.java`                | Still needed for operator registry (not artifacts)                             |
| `service/events/EventService.java`             | Event publishing unchanged                                                     |
| `service/events/SseHub.java`                   | SSE infrastructure unchanged                                                   |
| `prompt/*`                                     | Prompt system unchanged                                                        |
| `models/*`                                     | Model registry unchanged                                                       |

