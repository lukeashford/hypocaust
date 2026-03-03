# Artifact Intent & Orchestration Refactor Plan

## Context

This document captures the full architectural decisions from a design discussion about the artifact intent system, the orchestration lifecycle in `AbstractArtifactTool`, statelessness, error messaging for the decomposer, and related concerns. It is written so that an implementer in a fresh session (without the original chat) has all the detail needed.

---

## 1. Decouple Intent Derivation from OutputSpec Mapping

### Problem
`ArtifactIntentService.deriveMappings()` currently receives `List<OutputSpec>` and returns `List<IntentMapping>` where each mapping couples an `ArtifactIntent` to an `OutputSpec` via an `outputIndex` the LLM produces. This means the intent LLM must understand model-specific output slots — a concern that belongs to the executor, not the intent analyzer.

### Decision
Split into two phases:
1. **Intent derivation** (in `ArtifactIntentService`): Receives only the `task` string. Returns `List<ArtifactIntent>` — pure user-intent analysis ("ADD image", "EDIT @hero_image", "DELETE @old_logo"). No `OutputSpec`, no `outputIndex`.
2. **Output-to-intent mapping** (in the executor/tool): The tool or executor receives the gestating artifacts (already created from intents) and maps its own outputs to them.

### Changes

**`ArtifactIntent`** (`domain/ArtifactIntent.java`): No change needed. Already has `action`, `kind`, `targetName`, `description`.

**`IntentMapping`** (`domain/IntentMapping.java`): Remove the `outputSpec` field. The record becomes just `IntentMapping(ArtifactIntent intent)`. Alternatively, consider whether `IntentMapping` is still needed as a wrapper or whether `List<ArtifactIntent>` suffices. Decision: keep `IntentMapping` as a wrapper for extensibility, but drop `outputSpec`.

**`ArtifactIntentService.deriveMappings()`** (`service/ArtifactIntentService.java`):
- Change signature to `deriveMappings(String task)` — remove the `List<OutputSpec> outputs` parameter.
- Update the prompt fragment `PromptFragments.artifactIntentsAndMapping()` to no longer include output specs or ask for `outputIndex`. Rename it to something like `artifactIntents()`. The prompt should just ask: "What artifact actions does this task require? Return a JSON array of intents."
- Parsing: No longer read `outputIndex` from the LLM response. Just parse the `intent` objects.

**`AbstractArtifactTool`** (`tool/AbstractArtifactTool.java`):
- `deriveMappings()` signature changes to `deriveMappings(String task)` (no `outputs`).
- `orchestrate()` signature changes accordingly — see Section 3 for the new orchestrate flow.
- `prepareArtifacts()` no longer needs `OutputSpec` to decide whether to create gestating artifacts. Instead, every ADD and EDIT intent produces a gestating artifact. The `kind` for ADD comes from `intent.kind()`. The `description` for the gestating artifact comes from `intent.description()`.

**`GenerateCreativeTool`** (`tool/creative/GenerateCreativeTool.java`):
- Currently calls `orchestrate(task, model.outputs())`. Will call `orchestrate(task)` instead (no outputs).
- The executor now receives the gestating list and must match its own outputs to them — see Section 5.

---

## 2. Intent First, Then Naming (Ordering)

### Decision
Intent derivation happens first. Naming happens second, with the intent available as context.

### Current Flow
1. `deriveMappings()` — intents (LLM call)
2. `prepareArtifacts()` → `ArtifactsContext.add()` → `NamingService.generateArtifactNaming()` (LLM call per ADD)

These are already separate LLM calls. The ordering is already intent-first. The only change: pass the intent's `description` into the naming call so the naming LLM has richer context about what this artifact is for.

### Change
In `prepareArtifacts()`, when calling `TaskExecutionContextHolder.addArtifact()` for ADD intents, pass `intent.description()` as the `outputDescription` parameter (this already happens via `spec.getDescription()`, but after removing OutputSpec, we use `intent.description()` directly).

---

## 3. Refactor Orchestration Lifecycle — Context Updates in Parent

### Problem
Currently, `doExecute()` in `GenerateCreativeTool` (line 125) calls `TaskExecutionContextHolder.updateArtifact(updated)`. This mixes side effects into what should be a pure "produce artifacts" step. The parent prepared the gestating artifacts, so the parent should finalize them.

### Decision
Move context updates from `doExecute()` to `orchestrate()` in `AbstractArtifactTool`. `doExecute()` becomes a pure function: receives gestating artifacts, returns finalized artifacts (with `storageKey`/`inlineContent` set, status `MANIFESTED` or `FAILED`).

### New `orchestrate()` Flow

```java
protected final R orchestrate(String task) {
    // 1. Derive intents (overridable for deterministic tools)
    List<IntentMapping> mappings = deriveMappings(task);

    // 2. Prepare gestating artifacts
    List<Artifact> gestating = prepareArtifacts(mappings, task);

    try {
        // 3. Tool-specific execution (pure — no side effects)
        List<Artifact> results = doExecute(task, gestating, mappings);

        // 4. Validate and commit to context
        for (int i = 0; i < results.size(); i++) {
            Artifact result = results.get(i);
            validateFinalized(result);
            TaskExecutionContextHolder.updateArtifact(result);
        }

        // 5. Finalize result
        return finalizeResult(results, mappings);
    } catch (Exception e) {
        rollbackArtifacts(gestating);
        throw e;
    }
}

private void validateFinalized(Artifact artifact) {
    if (artifact.status() == ArtifactStatus.MANIFESTED) {
        if (artifact.storageKey() == null && artifact.inlineContent() == null) {
            throw new IllegalStateException(
                "MANIFESTED artifact without content: " + artifact.name());
        }
    }
    // FAILED artifacts are allowed — they carry errorMessage
}
```

### Contract for `doExecute()`
- Receives gestating artifacts (GESTATING status, no storageKey).
- Returns artifacts in **the same order** as the gestating list.
- Each returned artifact must be either:
  - `MANIFESTED` with `storageKey` (for media) or `inlineContent` (for TEXT) set.
  - `FAILED` with `errorMessage` set.
- Must NOT call `TaskExecutionContextHolder.updateArtifact()` — the parent does this.
- Must NOT publish events — the parent handles lifecycle events.

### Changes
- `GenerateCreativeTool.doExecute()`: Remove the `TaskExecutionContextHolder.updateArtifact(updated)` call at line 125. Just return the finalized artifacts.
- `AbstractArtifactTool.orchestrate()`: Add the validation + update loop after `doExecute()` returns.

---

## 4. Remove Instance Variables from Singleton Beans (Statelessness)

### Problem
`DeleteArtifactTool` stores `private String target` as an instance variable (line 26). Spring components are singletons by default. Two concurrent calls to `delete()` would overwrite `target`, causing a race condition.

`GenerateCreativeTool` uses `private static final ThreadLocal<ModelSearchResult> currentModel`. While `ThreadLocal` avoids the race, it's an implicit side-channel that obscures the data flow.

### Decision
Remove all instance variables and `ThreadLocal` usage from tool beans. Pass everything through method arguments.

### Changes

**`DeleteArtifactTool`**:
- Remove `private String target`.
- Pass `artifactName` through the orchestration. Two options:
  - **Option A**: Override `deriveMappings(String task)` and parse the target from the task string (which is `"Delete " + target`).
  - **Option B (preferred)**: Add a protected overload `orchestrate(String task, List<IntentMapping> explicitMappings)` in `AbstractArtifactTool` that skips `deriveMappings()`. DeleteArtifactTool calls this directly with its programmatic intent.

  With Option B, `delete()` becomes:
  ```java
  public DeleteResult delete(String artifactName) {
      if (artifactName == null || artifactName.isBlank()) {
          return DeleteResult.error("Artifact name is required");
      }
      String name = artifactName.trim();
      var mapping = new IntentMapping(
          ArtifactIntent.builder()
              .action(ArtifactAction.DELETE)
              .targetName(name)
              .description("Delete artifact " + name)
              .build());
      return orchestrate("Delete " + name, List.of(mapping));
  }
  ```
  No instance variable needed. The `deriveMappings()` override is also no longer needed.

**`GenerateCreativeTool`**:
- Remove `private static final ThreadLocal<ModelSearchResult> currentModel`.
- The `model` is selected in `generate()` and needs to flow to `doExecute()` and `finalizeResult()`. Since `doExecute` and `finalizeResult` are called from `orchestrate()` (in the parent), and the parent doesn't know about models, we need a way to pass tool-specific context.
- **Solution**: Add a generic "tool context" mechanism. `AbstractArtifactTool` gets a `ThreadLocal<Object> toolContext` (private), with `setToolContext()` / `getToolContext()` ... No, this reintroduces ThreadLocal.
- **Better solution**: Change the `orchestrate` / `doExecute` / `finalizeResult` signatures to pass a generic context. Since only `GenerateCreativeTool` needs model context, we use the type parameter `R` already on the class. Add a second type parameter for tool context, or just accept that the tool stashes its model in a local variable and passes it through a narrower scope.
- **Simplest solution**: Since `generate()` calls `orchestrate()` which calls `doExecute()` and `finalizeResult()` — and this all happens on the same thread within the same `generate()` call — we can extract the try-loop body into a method that takes `model` as a parameter. But `orchestrate()` is in the parent and calls the abstract methods...
- **Practical solution**: Add a protected `Map<String, Object> toolContext` as a method-scoped parameter threaded through orchestrate → doExecute → finalizeResult. Or simply change `doExecute` signature to include a context map.

  **Final decision**: Introduce a `ToolExecutionContext` record that `orchestrate` accepts and passes through:
  ```java
  public record ToolExecutionContext(Map<String, Object> attributes) {
      public <T> T get(String key, Class<T> type) { ... }
      public ToolExecutionContext with(String key, Object value) { ... }
  }
  ```
  `orchestrate(String task, ToolExecutionContext ctx)` passes `ctx` to `doExecute(task, gestating, mappings, ctx)` and `finalizeResult(results, mappings, ctx)`. GenerateCreativeTool stashes its `ModelSearchResult` in the ctx. DeleteArtifactTool ignores it. This is explicit, stateless, and extensible.

  Alternatively, if this feels too generic, just add `ModelSearchResult` as an optional field or use a more constrained approach. The key principle: **no mutable state on the bean, no ThreadLocal**.

---

## 5. Make Executor Intent-Aware

### Problem
`ModelExecutor.run()` currently receives only `List<Artifact> artifacts, String task, ModelSearchResult model, UnaryOperator<JsonNode> inputTransformer`. The executor's `generatePlan()` receives only `(String task, ModelSearchResult model)`. The planner has to re-derive what the user wants for each output slot from the raw task string.

### Decision
Pass `List<IntentMapping>` into the executor so the planner can generate precise provider-specific instructions. For example, if the intent is "EDIT @hero_image", the planner knows to set up an image-to-image call rather than text-to-image.

### Changes

**`ModelExecutor` interface** (`models/ModelExecutor.java`):
```java
ExecutionResult run(List<Artifact> artifacts, String task, ModelSearchResult model,
    List<IntentMapping> intents, UnaryOperator<JsonNode> inputTransformer);
```

**`AbstractModelExecutor`** (`models/AbstractModelExecutor.java`):
- `generatePlan` signature: `generatePlan(String task, ModelSearchResult model, List<IntentMapping> intents)`
- The planner prompt can now include: "This execution should produce: [1] An EDIT of an existing image (hero_image), [2] A new audio track." This gives the LLM planning step much more precise context.
- **Important**: Pass intent `description` and `action` only. Never pass storageKey, UUID, or URL to the planner LLM. Artifact references (`@name`) stay as placeholders in the plan output; the `inputTransformer` resolves them later.

**All 8 executor implementations** must update their `generatePlan` signatures:
- `AnthropicModelExecutor`
- `AssemblyAiModelExecutor`
- `ElevenLabsModelExecutor`
- `FalModelExecutor`
- `OpenAiModelExecutor`
- `OpenRouterModelExecutor`
- `ReplicateModelExecutor`
- `RunwayModelExecutor`

Initially, most can ignore the new `intents` parameter and continue working as before. Enhance individual executors incrementally as needed (e.g., Fal executor could use EDIT intent to switch from text-to-image to image-to-image endpoint).

---

## 6. Move ArtifactResolver to Util Package

### Problem
`ArtifactResolver` lives in `tool.creative` but the `@name` / `@name.url` / `@name.metadata.field` substitution logic is general-purpose. Future tools (ffmpeg, deterministic processors) will need it too.

### Decision
Move to `com.example.hypocaust.util.ArtifactResolver`.

### Additional Change — Remove UnaryOperator Hook
Currently, `GenerateCreativeTool` creates an `inputTransformer` lambda:
```java
input -> artifactResolver.resolve(input, availableArtifacts)
```
and passes it to `executor.run()` via the `UnaryOperator<JsonNode> inputTransformer` parameter.

With `ArtifactResolver` as a shared util:
- Inject `ArtifactResolver` directly into `AbstractArtifactTool`.
- Call `artifactResolver.resolve()` in `orchestrate()`, between `generatePlan()` and `doExecute()`, or let the executor call it internally.
- **Decision**: The parent (`AbstractArtifactTool`) resolves artifact placeholders in the context it controls. But for executor pipeline (where the plan output contains `@name` placeholders that need resolving before the API call), the executor still needs access.
- **Final approach**: Inject `ArtifactResolver` into `AbstractModelExecutor`. The `inputTransformer` parameter is removed from `ModelExecutor.run()`. The executor calls `artifactResolver.resolve(planOutput, availableArtifacts)` internally. The `availableArtifacts` list is passed as a new parameter to `run()`.

Updated `ModelExecutor.run()` signature:
```java
ExecutionResult run(List<Artifact> artifacts, String task, ModelSearchResult model,
    List<IntentMapping> intents, List<Artifact> availableArtifacts);
```

The executor calls `artifactResolver.resolve(plan.providerInput(), availableArtifacts)` internally in its pipeline.

---

## 7. Improve Error Messages for Decomposer — Capacity vs. Infrastructure

### Problem
The current error flow has only two categories:
1. Service/infrastructure failure → "All models failed ... DO NOT retry" → decomposer gives up.
2. Bad parameters → adjust and retry.

Missing third category: **capacity/structural mismatch** — e.g., "model supports 1 audio output, but 3 were requested." This should signal the decomposer to restructure its approach (e.g., generate one at a time), not give up entirely.

### Changes

**`AbstractModelExecutor.run()`** (lines 111-115): Change the error message:
```java
// Before:
throw new IllegalStateException(
    "Model returned " + extractedOutputs.size() + " outputs, but " + artifacts.size()
        + " were expected.");

// After:
throw new IllegalStateException(
    "This model produces " + extractedOutputs.size()
        + " output(s) per call, but " + artifacts.size()
        + " were expected. Consider generating them individually in separate calls.");
```

**`GenerateCreativeTool.generate()`**: Distinguish between capacity errors and infrastructure errors in the fallback message:
```java
// When building the final error after all models fail:
boolean allCapacityErrors = errors.stream()
    .allMatch(e -> e.contains("per call") || e.contains("individually"));

if (allCapacityErrors) {
    // Don't say "DO NOT retry" — the decomposer should restructure
    return GenerateCreativeResult.error(
        "All attempted models produce fewer outputs than requested. "
            + "Details: " + String.join("; ", errors) + ". "
            + "Try requesting fewer artifacts per call.");
} else {
    // Infrastructure failure — do say "DO NOT retry"
    return GenerateCreativeResult.error(
        "All models failed. Providers attempted: " + String.join(", ", failedPlatforms)
            + ". Details: " + String.join("; ", errors) + ". "
            + "DO NOT retry generation with similar parameters — "
            + "the underlying service appears unavailable.");
}
```

---

## 8. Consolidate selfHealing Prompt — Category-Based Reasoning

### Problem
The current `selfHealing()` prompt fragment pattern-matches on specific strings ("All models failed", "DO NOT retry", etc.). This is brittle and requires updating the prompt every time error messages change.

### Decision
Replace string-pattern matching with category-based reasoning. The decomposer learns to classify errors by their nature, not by keyword.

### New `selfHealing()` Text
```
After any action, evaluate the result. If the tool or child decomposer returned an error,
classify it and respond accordingly:

1. INFRASTRUCTURE / PROVIDER FAILURE — The error describes a technical issue
   with the underlying service (timeouts, network errors, API failures, service
   unavailable). The tool has already retried internally.
   → Do NOT retry this capability. Report the issue and move on.

2. INPUT / PARAMETER ISSUE — The error describes a problem with YOUR request
   (missing parameters, invalid format, wrong artifact reference).
   → Adjust your parameters and retry (max {{maxRetries}} attempts per approach).

3. CAPACITY / SCOPE MISMATCH — The error indicates your request exceeds what
   the tool can handle in a single call (too many outputs, unsupported
   combination), but the capability itself works.
   → Restructure your approach. Break the task into smaller units and retry
     each individually.

If a child decomposer failed, trust its diagnosis. Do not re-attempt the same
generation that the child already exhausted. When giving up, include error
details in your response.
```

This is shorter, clearer, and doesn't reference specific error message strings. It teaches reasoning rather than lookup.

---

## 9. Variations Are Separate Artifacts

### Decision
When a model produces multiple variations (e.g., 4 versions of an image), each is a separate artifact with its own unique name. The original stays untouched. This keeps the versioning model linear (the current `TaskExecutionDelta` → `predecessorId` chain).

### No Code Change Required
This is already how the system works. `Changelist` uses `Map<String, Artifact>` — one entry per name. Multiple edits to the same name overwrite. The decision is to not change this and keep it simple.

If a user wants to "pick a favorite" from variations and promote it, that's an explicit edit or delete+add — handled by existing tools.

---

## 10. LLM Must Never Handle Long IDs or URLs

### Current State
Already correct. `generatePlan()` produces `@artifact_name` placeholders. `inputTransformer` (soon: `ArtifactResolver` internally) resolves them to presigned URLs after planning. The planner LLM never sees URLs or UUIDs.

### Guardrail
When adding intent-awareness to `generatePlan()` (Section 5), pass only:
- `intent.action()` (ADD/EDIT/DELETE)
- `intent.kind()` (IMAGE/AUDIO/etc.)
- `intent.targetName()` (semantic name like "hero_image")
- `intent.description()` (natural language)

Never pass `storageKey`, `id`, `mimeType`, or presigned URLs into the planning prompt.

---

## Summary of All File Changes

### Domain Layer
| File | Change |
|------|--------|
| `IntentMapping.java` | Remove `outputSpec` field |
| `OutputSpec.java` | No change (still used by ModelSearchResult.outputs()) |

### Service Layer
| File | Change |
|------|--------|
| `ArtifactIntentService.java` | Remove `List<OutputSpec>` param from `deriveMappings()`. Simplify LLM prompt and parsing (no `outputIndex`). |

### Prompt Layer
| File | Change |
|------|--------|
| `PromptFragments.java` | (1) Rename `artifactIntentsAndMapping()` → `artifactIntents()`, remove OutputSpec/outputIndex from prompt text. (2) Rewrite `selfHealing()` with category-based reasoning (Section 8). |

### Tool Layer
| File | Change |
|------|--------|
| `AbstractArtifactTool.java` | (1) `orchestrate()` takes just `String task` (or `String task, List<IntentMapping> explicitMappings` for deterministic tools). (2) `deriveMappings()` takes just `String task`. (3) `prepareArtifacts()` uses `intent.description()` instead of `spec.getDescription()`. (4) Add validation + context update loop after `doExecute()`. (5) Inject `ArtifactResolver`. |
| `DeleteArtifactTool.java` | Remove `private String target`. Call `orchestrate(task, explicitMappings)` directly. Remove `deriveMappings()` override. |
| `GenerateCreativeTool.java` | (1) Remove `ThreadLocal<ModelSearchResult>`. Pass model through tool context. (2) Remove `TaskExecutionContextHolder.updateArtifact()` from `doExecute()` — parent does this. (3) Remove `inputTransformer` lambda — executor resolves internally. (4) Call `orchestrate(task)` without `model.outputs()`. |
| `ArtifactResolver.java` | Move from `tool.creative` to `util` package. |

### Executor Layer
| File | Change |
|------|--------|
| `ModelExecutor.java` | `run()` signature: add `List<IntentMapping> intents, List<Artifact> availableArtifacts`. Remove `UnaryOperator<JsonNode> inputTransformer`. |
| `AbstractModelExecutor.java` | (1) `generatePlan()` signature: add `List<IntentMapping> intents`. (2) Inject `ArtifactResolver`, call it internally instead of receiving `inputTransformer`. (3) Improve capacity-mismatch error message (Section 7). |
| All 8 concrete executors | Update `generatePlan()` signature to accept `List<IntentMapping> intents`. Initially, most can ignore the new param. |

### Notes
- `RestoreArtifactTool` does not extend `AbstractArtifactTool` — it calls `TaskExecutionContextHolder.restoreArtifact()` directly. No changes needed for this refactor.
- The `ToolExecutionContext` (or equivalent mechanism for passing model-specific context without ThreadLocal) needs to be introduced. See Section 4 for options. The simplest approach: add a `Map<String, Object>` parameter to `doExecute` and `finalizeResult`. Or use a dedicated record.

---

## Order of Implementation

1. **Move `ArtifactResolver` to `util`** — No behavioral change, just a package move. Update imports everywhere.
2. **Remove `outputSpec` from `IntentMapping`** and update `ArtifactIntentService` — Decouple intent from output mapping. Update prompt fragment.
3. **Rewrite `selfHealing()` prompt** — Category-based reasoning.
4. **Refactor `AbstractArtifactTool.orchestrate()`** — Add overload for explicit mappings, move context updates to parent, add validation.
5. **Remove `target` instance var from `DeleteArtifactTool`** — Use explicit mappings overload.
6. **Remove `ThreadLocal` from `GenerateCreativeTool`** — Introduce tool context passing. Remove `updateArtifact` call from `doExecute`.
7. **Update `ModelExecutor` interface** — Add `intents` and `availableArtifacts`, remove `inputTransformer`. Update `AbstractModelExecutor` and all 8 concrete executors.
8. **Improve capacity-mismatch error messages** — In `AbstractModelExecutor` and `GenerateCreativeTool` error builder.
