# Artifact Graph with Semantic Anchors

## Overview

This document describes the conceptual extension of the DecomposingOperator system to support:

1. **Corrections to previous runs** - modify specific artifacts without regenerating everything
2. **Unified execution model** - no distinction between "first run" and "correction"
3. **Selective regeneration** - only regenerate what's explicitly requested
4. **Natural language references** - "make the dog blonde" resolves to the correct artifact
5. **Follow-up tasks** - "now give me a mood board" extends without modifying
6. **Versioning with branching** - like git, explore alternatives without losing work

---

## Core Philosophy

### Current Model
```
Task → DecomposingOperator → Ledger (ephemeral) → Artifacts
```

### New Model
```
Task + Artifact Graph → DecomposingOperator → Ledger → Graph Mutation → Commit
```

The key shift: **the artifact graph is persistent context** that informs decomposition. Each run reads the graph, interprets the task against it, decomposes accordingly, and commits changes atomically.

**First run = task with empty graph. Correction = task with populated graph. Same code path.**

---

## The Recursive Decomposition Model (Unchanged Core)

The DecomposingOperator's power comes from recursive delegation:

```
DecomposingOperator receives task
    │
    ▼
Can ONE operator fully solve this?
    │
    ├─ YES → Invoke directly (leaf case)
    │
    └─ NO → Decompose into subtasks
            Each subtask → child DecomposingOperator
            Children execute sequentially
            Outputs wire to inputs via ledger
```

This avoids context explosion: complex tasks get broken into manageable pieces, each handled by a fresh operator instance with focused context.

**This model stays exactly the same.** What changes is the context available during decomposition.

---

## New Concepts

### 1. Semantic Anchors

Every artifact carries a **semantic anchor** - a rich description that serves as its natural language identity:

```java
record SemanticAnchor(
    String description,    // "A golden retriever wearing a top hat, sitting on a park bench"
    String role,           // Optional: "hero-image", "background-music", "opening-scene"
    Set<String> tags       // Searchable facets: ["dog", "park", "whimsical"]
)
```

**Why this matters**: When the user says "make the dog blonde", the system can:
1. Search anchors for "dog" → find the golden retriever artifact
2. Retrieve its provenance (what prompt/operator created it)
3. Compose a modification task with full context

The anchor is the **stable identity** across versions. Content changes, anchor persists.

### 2. Artifact Graph

The graph is the persistent state of all artifacts within a project:

```java
record ArtifactGraph(
    Map<UUID, ArtifactNode> nodes,           // All artifacts by ID
    Map<String, UUID> anchorIndex,           // Hash(anchor) → current version ID
    List<ArtifactRelation> relations         // Derivation edges
)

record ArtifactNode(
    UUID id,
    SemanticAnchor anchor,

    // Content
    ArtifactKind kind,                       // IMAGE, VIDEO, AUDIO, TEXT, STRUCTURED
    Object content,                          // Actual content or storage key

    // Provenance
    Provenance provenance,

    // Versioning
    int version,                             // Version number within this anchor
    UUID supersedes                          // Previous version (same anchor)
)

record Provenance(
    String operatorName,                     // What operator created this
    Map<String, Object> inputs,              // Exact inputs used
    UUID runId,                              // Which run
    List<UUID> derivedFrom                   // Parent artifacts that informed this
)

record ArtifactRelation(
    UUID sourceId,
    UUID targetId,
    RelationType type                        // DERIVED_FROM, SUPERSEDES, REFERENCES
)
```

### 3. Branches and Commits

Each project has branches (like git). Each run produces a commit.

```java
record Branch(
    UUID id,
    String name,                             // "main", "blonde-variant", etc.
    UUID headCommitId,
    UUID parentBranchId                      // For branch lineage
)

record Commit(
    UUID id,
    UUID parentCommitId,                     // Previous commit on this branch
    UUID runId,                              // The run that produced this
    String task,                             // Verbatim task (for visualization)
    Instant timestamp,
    CommitDelta delta
)

record CommitDelta(
    List<UUID> added,                        // New artifacts
    List<ArtifactUpdate> updated,            // Modified artifacts (new versions)
    List<UUID> removed                       // Removed artifacts (rare)
)

record ArtifactUpdate(
    String anchorHash,
    UUID oldVersionId,
    UUID newVersionId
)
```

### 4. Execution Context

This is what operators see during execution. It replaces the implicit thread-local context with rich graph access:

```java
record ExecutionContext(
    UUID projectId,
    UUID runId,
    Branch currentBranch,
    Commit parentCommit,

    // The visible artifact graph (snapshot at run start)
    ArtifactGraph graph,

    // Accumulator for this run's changes
    PendingChanges pending
)

record PendingChanges(
    Set<UUID> regenerating,                  // Artifacts being regenerated
    Set<UUID> keeping,                       // Explicitly preserved
    List<ArtifactNode> created               // New artifacts from this run
)
```

**Key methods on ExecutionContext:**

```java
// Find artifacts by natural language query (semantic search on anchors)
List<ArtifactNode> findByDescription(String query);

// Get artifact by exact anchor
Optional<ArtifactNode> findByAnchor(SemanticAnchor anchor);

// Get artifact by role
Optional<ArtifactNode> findByRole(String role);

// Mark for regeneration (will be superseded)
void markForRegeneration(UUID artifactId);

// Explicitly keep (won't be touched)
void keep(UUID artifactId);

// Add new artifact
void addArtifact(ArtifactNode artifact);
```

---

## How Decomposition Changes

### The Ledger's New Role

The ledger remains the **intra-run execution plan**. It still defines:
- `values`: shared context for this execution
- `children`: sequence of operator invocations
- `inputsToKeys` / `outputsToKeys`: wiring

**What's new**: values can reference existing artifacts by anchor:

```json
{
  "values": {
    "existingImage": "@anchor:woman in red dress at cafe",
    "modificationTask": "Regenerate {{existingImage}} but make her blonde",
    "videoTask": "Create a 5-second video from {{blondeImage}}"
  },
  "children": [
    {
      "operatorName": "DecomposingOperator",
      "inputsToKeys": { "task": "modificationTask" },
      "outputsToKeys": { "result": "blondeImage" }
    },
    {
      "operatorName": "DecomposingOperator",
      "inputsToKeys": { "task": "videoTask" },
      "outputsToKeys": { "result": "finalVideo" }
    }
  ],
  "finalOutputKey": "finalVideo"
}
```

The `@anchor:` prefix tells the system to resolve from the artifact graph.

### The DecomposingOperator Flow (Enhanced)

```
┌─────────────────────────────────────────────────────────────────────┐
│ DecomposingOperator.doExecute(task)                                 │
│                                                                     │
│ 1. GET CONTEXT                                                      │
│    - Retrieve ExecutionContext (includes artifact graph)            │
│    - Build list of existing artifacts with their anchors            │
│                                                                     │
│ 2. BUILD ENHANCED SYSTEM MESSAGE                                    │
│    - Existing decision algorithm (leaf vs decompose)                │
│    - PLUS: "Here are the existing artifacts in this project:"       │
│      [                                                              │
│        { anchor: "woman in red dress at cafe", kind: IMAGE, ... },  │
│        { anchor: "cafe background music", kind: AUDIO, ... }        │
│      ]                                                              │
│    - "You may reference these by anchor in your decomposition"      │
│    - "If the task modifies existing artifacts, reference them"      │
│    - "If the task creates new artifacts, describe their anchors"    │
│                                                                     │
│ 3. CALL LLM WITH TOOLS                                              │
│    - Same tools: invoke, workflowSearch, modelSearch                │
│    - LLM sees both: available operators AND existing artifacts      │
│                                                                     │
│ 4. LLM PRODUCES LEDGER                                              │
│    - For modifications: references existing artifacts               │
│    - For new work: describes new anchors                            │
│    - Complex tasks: decomposes into child DecomposingOperators      │
│                                                                     │
│ 5. EXECUTE LEDGER (via InvokeTool)                                  │
│    - Resolve @anchor: references from graph                         │
│    - Execute children sequentially                                  │
│    - Each child DecomposingOperator recurses with same flow         │
│                                                                     │
│ 6. RETURN RESULT                                                    │
│    - Success: artifacts created/modified in pending changes         │
│    - Failure: rollback pending changes                              │
└─────────────────────────────────────────────────────────────────────┘
```

### Example: "Make her blonde and then generate a short video"

**Artifact graph before:**
```
- Anchor: "woman in red dress at cafe" → Image v1
- Anchor: "cafe ambient sounds" → Audio v1
```

**DecomposingOperator receives task:**
```
"Make her blonde and then generate a short video from that"
```

**LLM sees:**
- Available operators: ImageGenerationOperator, VideoGenerationOperator, etc.
- Existing artifacts: woman image, cafe audio

**LLM produces ledger:**
```json
{
  "values": {
    "originalImage": "@anchor:woman in red dress at cafe",
    "regenerateTask": "Regenerate {{originalImage}} with blonde hair instead of original hair color. Keep everything else identical.",
    "videoTask": "Generate a 5-second video based on {{blondeImage}}"
  },
  "children": [
    {
      "operatorName": "DecomposingOperator",
      "inputsToKeys": { "task": "regenerateTask" },
      "outputsToKeys": { "result": "blondeImage" }
    },
    {
      "operatorName": "DecomposingOperator",
      "inputsToKeys": { "task": "videoTask" },
      "outputsToKeys": { "result": "video" }
    }
  ],
  "finalOutputKey": "video"
}
```

**Child 1 executes:**
- DecomposingOperator analyzes "Regenerate {{originalImage}} with blonde hair..."
- Resolves {{originalImage}} → full artifact with provenance
- Sees this is a direct image task → invokes ImageGenerationOperator
- ImageGenerationOperator:
  - Reads original provenance (knows exact prompt/style used)
  - Modifies prompt: same scene, blonde hair
  - Generates new image
  - Creates artifact with **same anchor** "woman in red dress at cafe" → v2
  - Marks v1 as superseded

**Child 2 executes:**
- DecomposingOperator analyzes "Generate 5-second video based on {{blondeImage}}"
- Resolves {{blondeImage}} → the newly created v2 image
- May decompose further or invoke VideoGenerationOperator directly
- Creates new artifact with anchor "5-second video of woman at cafe"

**Result:**
```
- Anchor: "woman in red dress at cafe" → Image v2 (blonde) [supersedes v1]
- Anchor: "cafe ambient sounds" → Audio v1 [unchanged]
- Anchor: "5-second video of woman at cafe" → Video v1 [new]
```

---

## Handling Specific Scenarios

### Scenario A: Pure Modification
**Task:** "Make the dog blonde"

```
Graph: [dog with hat → Image v1, park background → Image v1]

Decomposition:
- LLM identifies: modification of "dog with hat"
- Creates ledger referencing existing artifact
- Child invokes ImageGenerationOperator with modification context
- Same anchor, new version

Result: dog with hat → Image v2 (blonde), park unchanged
```

### Scenario B: Pure Continuation
**Task:** "Great! Now give me a mood board"

```
Graph: [script for commercial → Text v1]

Decomposition:
- LLM identifies: new artifact, derived from script
- Creates ledger with new task, references script for context
- Child invokes appropriate operator
- New anchor created

Result: script unchanged, mood board → new artifact derived from script
```

### Scenario C: Mixed (Modify + Continue)
**Task:** "Make her blonde and generate a video"

```
Graph: [woman image → Image v1]

Decomposition:
- LLM identifies: modify image, then create derived video
- Creates ledger with two children (sequential, second depends on first)
- Child 1: regenerate image → v2
- Child 2: create video from v2

Result: woman → Image v2, video → new artifact derived from v2
```

### Scenario D: Multi-Modify
**Task:** "Change the dog to a cat and make the background a beach"

```
Graph: [dog with hat → Image v1, park background → Image v1, music → Audio v1]

Decomposition:
- LLM identifies: modify two artifacts, keep one
- Creates ledger with two parallel-ish modifications
- Child 1: regenerate dog → cat
- Child 2: regenerate park → beach
- Music untouched

Result: cat with hat → v2, beach background → v2, music unchanged
```

### Scenario E: Branching
**User action:** "Go back to before the blonde change and try brown instead"

```
Branch: main at Commit C (blonde)

System:
1. Find Commit B (parent of C)
2. Create new branch "brown-variant" from B
3. Execute new task on that branch

Result:
  main:   A → B → C (blonde)
  brown:  A → B → D (brown)
```

User can switch between branches, each with its own artifact graph state.

---

## Anchor Resolution

### The `@anchor:` Syntax

When the ledger contains `@anchor:description`, the InvokeTool resolves it:

```java
private Object resolveValue(ExecutionContext ctx, Object value) {
    if (value instanceof String s && s.startsWith("@anchor:")) {
        String anchorQuery = s.substring("@anchor:".length());
        return ctx.getGraph().findByDescription(anchorQuery)
            .orElseThrow(() -> new AnchorNotFoundException(anchorQuery));
    }
    // ... existing placeholder resolution
}
```

The resolved value includes:
- Full artifact content (or storage key)
- Provenance (original inputs, operator used)
- Anchor details

This gives leaf operators everything they need to modify intelligently.

### Semantic Search on Anchors

Anchors are embedded (like operator descriptions) for semantic search:

```java
interface AnchorRegistry {
    // Find by semantic similarity
    List<ArtifactNode> search(String query, int topK);

    // Find by exact anchor hash
    Optional<ArtifactNode> findExact(SemanticAnchor anchor);

    // Check for conflicts (would this anchor be confused with existing?)
    boolean wouldConflict(SemanticAnchor proposed);
}
```

This enables fuzzy matching: "the dog" matches "golden retriever with top hat".

---

## Operator Changes

### Leaf Operators Receive Richer Context

When an operator receives a task that references an existing artifact, it gets:

```java
Map<String, Object> inputs = {
    "task": "Regenerate with blonde hair",
    "sourceArtifact": {
        "id": "uuid",
        "anchor": { "description": "woman in red dress at cafe", ... },
        "kind": "IMAGE",
        "storageKey": "s3://...",
        "provenance": {
            "operatorName": "ImageGenerationOperator",
            "inputs": {
                "prompt": "A woman in a red dress sitting at a Parisian cafe...",
                "style": "photorealistic",
                "model": "flux-pro"
            }
        }
    }
}
```

The operator can then:
1. Use the original prompt as a base
2. Apply the modification ("blonde hair")
3. Maintain consistency (same style, model, etc.)

### Operators Declare Anchors for Outputs

Operators should describe what they're creating:

```java
@Override
protected OperatorResult doExecute(Map<String, Object> inputs) {
    // ... generate image ...

    var anchor = SemanticAnchor.builder()
        .description(generateDescription(result))  // AI-generated or input-based
        .role(inputs.get("role"))
        .tags(extractTags(result))
        .build();

    artifactService.createWithAnchor(artifactId, anchor, content, provenance);

    return OperatorResult.success(...);
}
```

---

## Run Lifecycle (Enhanced)

```
1. TASK SUBMITTED
   └─ Create Run entity (QUEUED)
   └─ Snapshot current branch head as parent commit
   └─ Create ExecutionContext with graph snapshot

2. EXECUTION
   └─ Set RunContextHolder (includes ExecutionContext)
   └─ DecomposingOperator.execute(task)
   └─ Recursive decomposition with graph awareness
   └─ Leaf operators create/modify artifacts
   └─ Changes accumulate in ExecutionContext.pending

3. COMMIT (on success)
   └─ Create Commit from pending changes
   └─ Update branch head
   └─ Persist all new artifact versions
   └─ Mark superseded artifacts

4. ROLLBACK (on failure)
   └─ Discard pending changes
   └─ Graph unchanged
   └─ Run marked as FAILED
```

---

## Database Schema Additions

### New Tables

```sql
-- Branches within a project
CREATE TABLE branch (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES project(id),
    name VARCHAR(255) NOT NULL,
    head_commit_id UUID REFERENCES commit(id),
    parent_branch_id UUID REFERENCES branch(id),
    created_at TIMESTAMP NOT NULL,
    UNIQUE(project_id, name)
);

-- Commits (immutable snapshots)
CREATE TABLE commit (
    id UUID PRIMARY KEY,
    branch_id UUID NOT NULL REFERENCES branch(id),
    parent_commit_id UUID REFERENCES commit(id),
    run_id UUID NOT NULL REFERENCES run(id),
    task TEXT NOT NULL,  -- Verbatim task for visualization
    timestamp TIMESTAMP NOT NULL,
    delta JSONB NOT NULL  -- CommitDelta as JSON
);

-- Artifact relations (derivation graph)
CREATE TABLE artifact_relation (
    id UUID PRIMARY KEY,
    source_artifact_id UUID NOT NULL REFERENCES artifact(id),
    target_artifact_id UUID NOT NULL REFERENCES artifact(id),
    relation_type VARCHAR(50) NOT NULL,  -- DERIVED_FROM, SUPERSEDES, REFERENCES
    created_at TIMESTAMP NOT NULL
);

-- Anchor embeddings for semantic search
CREATE TABLE anchor_embedding (
    id UUID PRIMARY KEY,
    artifact_id UUID NOT NULL REFERENCES artifact(id),
    embedding vector(1536) NOT NULL,
    anchor_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_anchor_embedding_vector ON anchor_embedding
    USING ivfflat (embedding vector_cosine_ops);
```

### Artifact Table Additions

```sql
ALTER TABLE artifact ADD COLUMN anchor_description TEXT;
ALTER TABLE artifact ADD COLUMN anchor_role VARCHAR(100);
ALTER TABLE artifact ADD COLUMN anchor_tags JSONB;  -- Array of strings
ALTER TABLE artifact ADD COLUMN version INTEGER DEFAULT 1;
ALTER TABLE artifact ADD COLUMN derived_from JSONB;  -- Array of UUIDs
```

---

## Summary: What Changes vs. What Stays

### Unchanged
- DecomposingOperator's recursive decomposition algorithm
- Ledger structure for intra-run operator wiring
- Operator registry and semantic search for operators
- Individual operator implementations (mostly)
- Event publishing and SSE
- Run entity and status tracking

### Enhanced
- Ledger values can reference artifacts via `@anchor:`
- Operators receive richer context including provenance
- Operators should declare semantic anchors for outputs
- ExecutionContext replaces simple RunContextHolder

### New
- Artifact graph as persistent, queryable state
- Semantic anchors on artifacts
- Branch and Commit entities for versioning
- Anchor registry with semantic search
- `@anchor:` resolution in InvokeTool

---

## Key Principles

1. **Graph = nouns, Ledger = verbs**: The artifact graph is what exists. The ledger is how operators chain within a run.

2. **Same path, different context**: First run and corrections use identical code. The graph being empty or populated is the only difference.

3. **Decomposition is the plan**: Complex tasks (modify + continue) naturally decompose into child operators. No separate "task plan" structure needed.

4. **Anchors are identity**: Artifacts are referenced by semantic description, not opaque IDs. This enables natural language interaction.

5. **Provenance enables smart modification**: When modifying, operators see exactly how the original was created and can apply targeted changes.

6. **Branches enable exploration**: Users can try alternatives without losing work. Switch between branches like git.

7. **Commits are tasks**: The commit message is the verbatim task, making history human-readable and visualizable.
