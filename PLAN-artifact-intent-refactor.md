# Artifact Intent & Orchestration Refactor — Revised Plan (v2)

## Current State Assessment

The original plan was partially implemented. This section audits what landed correctly, what landed incorrectly, and what was missed entirely.

### Correctly Implemented

| Original Section | Status |
|---|---|
| §1 Decouple intent from OutputSpec | Done. `ArtifactIntentService.deriveMappings(String task)` takes only the task. `IntentMapping` wraps only `ArtifactIntent` — no `outputSpec` field. `PromptFragments.artifactIntents()` does not mention output specs. |
| §2 Intent-first ordering | Done. `deriveMappings()` is called before `prepareArtifacts()`. |
| §3 Context updates in parent | Done. `doExecute()` is pure — no `TaskExecutionContextHolder.updateArtifact()`. The parent's `orchestrate()` handles validation + commit + rollback. |
| §5 Executor signatures updated | Partially done. `ModelExecutor.run()` and `generatePlan()` accept `List<IntentMapping> intents`. But **no executor actually uses the intents** — they are dead parameters in every implementation (see "Missed" below). |
| §6 ArtifactResolver moved to `util` | Done. Lives at `com.example.hypocaust.util.ArtifactResolver`. Injected into `AbstractModelExecutor`. `inputTransformer` lambda removed — executor resolves internally. |
| §7 Capacity-mismatch error message | Done. `AbstractModelExecutor.run()` throws: `"This model produces N output(s) per call, but M were expected. Consider generating them individually."` |
| §8 selfHealing prompt rewrite | Done. Category-based reasoning (infrastructure / input / capacity) with no string-pattern matching. |
| §9 Variations are separate artifacts | No code change needed — already works via name-keyed Changelist. |
| §10 LLM never handles raw IDs | Verified. `@name` placeholders resolve to presigned URLs via `ArtifactResolver` after planning. Planning prompts never see UUIDs, storage keys, or URLs. |

### Implemented Incorrectly

1. **`ToolExecutionContext` was introduced** (§4) — despite the agreed constraint: _"Do not introduce a `ToolExecutionContext` or similar wrapper. State is passed through method arguments, not stored on beans."_ The current `ToolExecutionContext` is a generic `Map<String, Object>` bag that `GenerateCreativeTool` uses to pass its `ModelSearchResult` through the orchestration pipeline. It is not an instance variable or ThreadLocal (good), but it is exactly the kind of type-unsafe wrapper we said we wouldn't build.

2. **Three `orchestrate()` overloads** (§3/§4) — `AbstractArtifactTool` has three protected overloads (`(task)`, `(task, ctx)`, `(task, explicitMappings)`) plus a private implementation. The agreed design: _"a single, clean signature."_

3. **Model selection happens *outside* `doExecute()`** — currently in `GenerateCreativeTool.generate()`, before `orchestrate()` is called. The agreed architecture: _"In GenerateCreativeTool, model selection happens inside doExecute, which means the intent is available at model-selection time."_ This is the root cause of why `ToolExecutionContext` was needed.

### Not Yet Implemented

1. **No executor uses intents in planning** — all 8 executors accept `List<IntentMapping> intents` in `generatePlan()` but ignore the parameter. The intent information never reaches the planning LLM prompts.

2. **`AnthropicModelExecutor` has no text-only validation** — a non-TEXT intent would silently produce a broken result. The agreed design: _"validate that the intent list contains only text artifact intents and fail clearly if not."_

3. **`artifactIntents()` prompt is missing the variations guideline** — the prompt doesn't explain the distinction between EDIT (in-place replacement) and multiple ADDs referencing the same artifact as input. This matters for multi-variation scenarios.

4. **Tests reference stale API** — `GenerateCreativeToolTest` and `DeleteArtifactToolTest` reference `ToolExecutionContext` and outdated constructor signatures. The test for `GenerateCreativeToolTest` also doesn't mock `ArtifactIntentService` (field-injected in the parent), which means `deriveMappings()` would NPE. Tests need alignment with the new architecture.

---

## Answers to Open Questions

### Q1: Multi-variation edits — does the architecture handle 4 outputs referencing the same predecessor?

**Answer: Yes, but only via ADDs. Not via 4 EDITs of the same name.**

The `Changelist` uses `Map<String, Artifact>` — keyed by artifact name. If you tried 4 EDIT intents all targeting `hero_image`, they'd overwrite each other in the map. The last one wins; the first three are lost.

The correct pattern for "4 variations of an edit to hero_image" is:

```json
[
  {"action": "ADD", "kind": "IMAGE", "description": "Variation 1: hero_image with warm tones"},
  {"action": "ADD", "kind": "IMAGE", "description": "Variation 2: hero_image with cool tones"},
  {"action": "ADD", "kind": "IMAGE", "description": "Variation 3: hero_image more contrast"},
  {"action": "ADD", "kind": "IMAGE", "description": "Variation 4: hero_image softer lighting"}
]
```

Each gets a unique name from the naming service. All four reference `@hero_image` in the task description, which resolves to the original's presigned URL via `ArtifactResolver` during execution. The original stays untouched.

**What changes:** The `artifactIntents()` prompt must clarify this distinction explicitly so the intent LLM doesn't generate 4 EDITs with the same `targetName`. See §4 below.

A single in-place EDIT (one intent, one output, same name) works correctly today. Multiple edits to the same artifact (different names, same input reference) also works today via ADDs.

### Q2: Decomposer error interpretability — how do we avoid drift between error messages and the decomposer's system prompt?

**Answer: We already have the right architecture. The key property is that the selfHealing prompt teaches *reasoning patterns*, not keyword matching.**

The current `selfHealing()` prompt describes three error categories by their *nature* (infrastructure, input, capacity), not by specific strings. When an executor throws `"This model produces 1 output(s) per call, but 3 were expected. Consider generating them individually."`, the decomposer classifies it as capacity/scope based on semantic understanding, not by scanning for "per call."

This means:
- **New error messages don't require prompt updates** — as long as the message is written in natural language that a human could classify.
- **The contract is: write clear, self-classifying error messages.** If the message says what went wrong and what to try instead, the decomposer can route correctly.

**What changes:** Nothing in the prompt. Instead, add a **code-level guideline** as a Javadoc on `AbstractModelExecutor`:

```java
/**
 * Error message contract for decomposer interpretability:
 * Every exception message thrown from run() should be self-classifying:
 * 1. State what went wrong (the fact)
 * 2. State why it went wrong (infrastructure, bad input, or capacity limit)
 * 3. Suggest what to try instead (if applicable)
 *
 * Example: "This model produces 1 output(s) per call, but 3 were expected.
 *           Consider generating them individually in separate calls."
 *
 * The decomposer LLM classifies errors by understanding the message,
 * not by keyword matching. No error tags or category enums are needed.
 */
```

### Q3: Intent injection into planning — how should this be wired?

**Answer: A helper method in `AbstractModelExecutor` that formats intents into a human-readable string for the planning prompt. Each executor includes it in its prompt. AnthropicModelExecutor validates instead of planning.**

**`AbstractModelExecutor` gets a protected helper:**

```java
protected String formatIntentContext(List<IntentMapping> intents) {
    if (intents == null || intents.isEmpty()) return "";
    var sb = new StringBuilder("Artifact intents for this execution:\n");
    for (int i = 0; i < intents.size(); i++) {
        var intent = intents.get(i).intent();
        sb.append("  [").append(i + 1).append("] ")
          .append(intent.action()).append(" ").append(intent.kind());
        if (intent.targetName() != null) {
            sb.append(" (@").append(intent.targetName()).append(")");
        }
        sb.append(": ").append(intent.description()).append("\n");
    }
    return sb.toString();
}
```

**Each planning executor includes it in the user prompt:**

```java
// FalModelExecutor.generatePlan():
var userPrompt = String.format("""
    Task: %s
    %s
    Model Docs: %s
    Best Practices: %s
    """, task, formatIntentContext(intents), model.description(), model.bestPractices());
```

**AnthropicModelExecutor validates instead:**

```java
@Override
protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
    List<IntentMapping> intents) {
  for (var mapping : intents) {
    if (mapping.intent().kind() != ArtifactKind.TEXT) {
      return ExecutionPlan.error(
          "Anthropic models support only TEXT output, but received "
              + mapping.intent().kind() + " intent: " + mapping.intent().description()
              + ". Choose a different model for " + mapping.intent().kind() + " generation.");
    }
  }
  return new ExecutionPlan(objectMapper.createObjectNode().put("prompt", task), null);
}
```

OpenAI and OpenRouter executors should apply the same text-only validation. Media executors (Fal, Replicate, Runway, ElevenLabs, AssemblyAI) include intent context in their planning prompts.

---

## Revised Changes

### §1. Remove `ToolExecutionContext` — Move Model Selection Into `doExecute()`

**Root cause:** `ToolExecutionContext` exists only because model selection happened in `generate()` and needed to pass the model through `orchestrate()` → `doExecute()`. If model selection moves inside `doExecute()`, the wrapper is unnecessary.

**New flow for `GenerateCreativeTool.generate()`:**

```java
public GenerateCreativeResult generate(String task) {
    try {
        List<IntentMapping> mappings = deriveMappings(task);  // intent analysis first
        return orchestrate(task, mappings);
    } catch (Exception e) {
        return GenerateCreativeResult.error(e.getMessage());
    }
}
```

**New flow for `GenerateCreativeTool.doExecute()`:**

```java
@Override
protected List<Artifact> doExecute(String task, List<Artifact> gestating,
    List<IntentMapping> mappings) {

    // Step 1: Use intents to inform model requirement generation
    ModelRequirement req = wordingService.generateModelRequirement(task);
    var models = modelRag.search(req);
    if (models.isEmpty()) {
        throw new IllegalStateException("No suitable model found for: " + req);
    }

    // Step 2: Try models in ranked order — fallback on failure
    var errors = new ArrayList<String>();
    var failedPlatforms = new LinkedHashSet<String>();
    for (var model : models.stream().limit(MAX_MODEL_ATTEMPTS).toList()) {
        try {
            var executor = executionRouter.resolve(model.platform());
            List<Artifact> available = TaskExecutionContextHolder.getContext()
                .getArtifacts().getAllWithChanges();
            var result = executor.run(gestating, task, model, mappings, available);

            // Validate count and kind alignment
            validateExecutorResult(result, gestating);

            // Enrich metadata and return
            return result.artifacts().stream().map(finalized ->
                finalized.withMetadata(
                    mergeMetadata(finalized.metadata(), model, task, result.providerInput()))
            ).toList();
        } catch (Exception e) {
            errors.add(model.name() + ": " + e.getMessage());
            failedPlatforms.add(model.platform());
        }
    }

    // All models failed — throw with classifiable message
    boolean allCapacityErrors = errors.stream()
        .allMatch(e -> e.contains("per call") || e.contains("individually"));

    if (allCapacityErrors) {
        throw new IllegalStateException(
            "All attempted models produce fewer outputs than requested. "
                + "Details: " + String.join("; ", errors) + ". "
                + "Try requesting fewer artifacts per call.");
    } else {
        throw new IllegalStateException(
            "All models failed. Providers attempted: "
                + String.join(", ", failedPlatforms)
                + ". Details: " + String.join("; ", errors) + ". "
                + "DO NOT retry generation with similar parameters — "
                + "the underlying service appears unavailable.");
    }
}
```

**Key benefits:**
- Intent analysis is complete before model selection → intents can inform `generateModelRequirement()` later.
- Model fallback happens inside `doExecute()` with the same gestating artifacts — no re-derivation of intents, no re-creation of gestating artifacts per attempt.
- `ToolExecutionContext` is deleted entirely.

**`finalizeResult()` reads model name from artifact metadata:**

```java
@Override
protected GenerateCreativeResult finalizeResult(List<Artifact> results,
    List<IntentMapping> mappings) {
    List<String> names = results.stream().map(Artifact::name).toList();
    String modelName = results.isEmpty() ? "unknown"
        : results.getFirst().metadata().path("generation_details").path("model_name").asText("unknown");
    return GenerateCreativeResult.success(names,
        "Generated artifacts: " + String.join(", ", names) + " using " + modelName);
}
```

### Files Changed

| File | Change |
|---|---|
| `ToolExecutionContext.java` | **Delete.** |
| `AbstractArtifactTool.java` | Remove all `ToolExecutionContext` references. See §2 for new signature. |
| `GenerateCreativeTool.java` | Move model selection + fallback from `generate()` into `doExecute()`. Remove `ToolExecutionContext` usage. Update `finalizeResult()` to read model from metadata. |
| `DeleteArtifactTool.java` | Remove `ToolExecutionContext` from `doExecute()` and `finalizeResult()` signatures. |

---

### §2. Consolidate `orchestrate()` to a Single Signature

**Current state:** Three protected overloads + one private implementation.

**New state:** One protected final method.

```java
/**
 * The orchestration template. Caller provides pre-built mappings — either
 * LLM-derived via deriveMappings(), or programmatic (e.g., DeleteArtifactTool).
 */
protected final R orchestrate(String task, List<IntentMapping> mappings) {
    log.info("[PARENT] Starting orchestration for task: {}", task);
    log.info("[PARENT] {} mappings", mappings.size());

    List<Artifact> gestating = prepareArtifacts(mappings, task);
    log.info("[PARENT] Prepared {} gestating artifacts", gestating.size());

    try {
        List<Artifact> results = doExecute(task, gestating, mappings);
        log.info("[CHILD] doExecute returned {} artifacts", results.size());

        for (Artifact result : results) {
            validateFinalized(result);
            TaskExecutionContextHolder.updateArtifact(result);
        }

        return finalizeResult(results, mappings);
    } catch (Exception e) {
        log.warn("[PARENT] Execution failed, rolling back: {}", e.getMessage());
        rollbackArtifacts(gestating);
        throw e;
    }
}
```

**Abstract method signatures simplified:**

```java
protected abstract List<Artifact> doExecute(
    String task, List<Artifact> gestating, List<IntentMapping> mappings);

protected abstract R finalizeResult(
    List<Artifact> results, List<IntentMapping> mappings);
```

**Callers are explicit about how they provide mappings:**

```java
// GenerateCreativeTool.generate():
List<IntentMapping> mappings = deriveMappings(task);  // LLM-based
return orchestrate(task, mappings);

// DeleteArtifactTool.delete():
var mapping = new IntentMapping(ArtifactIntent.builder()
    .action(ArtifactAction.DELETE)
    .targetName(name)
    .description("Delete artifact " + name)
    .build());
return orchestrate("Delete " + name, List.of(mapping));  // programmatic
```

The `deriveMappings()` helper stays as a protected method on `AbstractArtifactTool` — callers invoke it explicitly when they need LLM-based derivation.

### Files Changed

| File | Change |
|---|---|
| `AbstractArtifactTool.java` | Replace three overloads + private impl with single `orchestrate(String, List<IntentMapping>)`. Remove `ToolExecutionContext` from `doExecute`/`finalizeResult` signatures. |
| `GenerateCreativeTool.java` | Call `deriveMappings(task)` explicitly in `generate()`, pass result to `orchestrate()`. |
| `DeleteArtifactTool.java` | Already constructs explicit mappings — just call `orchestrate(task, List.of(mapping))`. |

---

### §3. Wire Intent Context Into Executor Planning Prompts

**Currently:** All 8 executors accept `List<IntentMapping> intents` in `generatePlan()` but ignore the parameter.

**Change:** `AbstractModelExecutor` provides a `formatIntentContext()` helper (see Q3 answer above). Each executor integrates it:

| Executor | Change |
|---|---|
| `AbstractModelExecutor` | Add `formatIntentContext(List<IntentMapping>)` protected helper. Add Javadoc error message contract (see Q2). |
| `FalModelExecutor` | Include `formatIntentContext(intents)` in user prompt for `generatePlan()`. |
| `ReplicateModelExecutor` | Same — include in user prompt. |
| `ElevenLabsModelExecutor` | Same — include in user prompt. |
| `RunwayModelExecutor` | Same — include in user prompt. |
| `AssemblyAiModelExecutor` | Same — include in user prompt. |
| `AnthropicModelExecutor` | Validate all intents are `TEXT` kind. Return `ExecutionPlan.error()` if not. |
| `OpenAiModelExecutor` | Same text-only validation as Anthropic. |
| `OpenRouterModelExecutor` | Same text-only validation as Anthropic. |

**Note on the validation pattern for text-only executors:** The error message must be classifiable as "capacity/scope" so the decomposer restructures (picks a different model) rather than giving up:

```java
return ExecutionPlan.error(
    "Anthropic models support only TEXT output, but received "
        + mapping.intent().kind() + " intent: " + mapping.intent().description()
        + ". Choose a different model for " + mapping.intent().kind() + " generation.");
```

---

### §4. Clarify Multi-Variation Intent in the `artifactIntents()` Prompt

**Currently:** The prompt says nothing about variations vs. in-place edits. The intent LLM might generate 4 EDIT intents with the same `targetName`, which would break the Changelist.

**Change:** Add a clarification to `PromptFragments.artifactIntents()`:

```
Rules:
- EDIT replaces an existing artifact in-place. There can be at most one EDIT per targetName.
- To create multiple variations of an existing artifact, use ADD for each variation.
  The downstream tool will receive the original via @name reference in the task.
- DELETE has no output artifact. Only use it when the user explicitly wants removal.
- For ADD, always provide a 'kind'. For EDIT, 'kind' is inherited from the target.
  For DELETE, 'kind' can be omitted.
```

### Files Changed

| File | Change |
|---|---|
| `PromptFragments.java` | Extend `artifactIntents()` body with the variation rules above. |

---

### §5. Update Tests

The existing tests need to be aligned with the new architecture:

**`GenerateCreativeToolTest`:**
- Mock `ArtifactIntentService` (currently not mocked — field-injected on the parent, left null).
- Remove all `ToolExecutionContext` references.
- `generate_success()`: Mock `intentService.deriveMappings(task)` to return an ADD mapping. Assert that model selection + fallback behavior works inside `doExecute()`.
- `generate_fallbackToSecondModel_onFirstModelFailure()`: Verify that intent derivation and artifact preparation happen ONCE even when the first model fails. The gestating artifact should NOT be re-created on fallback.
- `generate_noModelFound_returnsError()`: Now returns error via exception caught in `generate()`, not via early return.

**`DeleteArtifactToolTest`:**
- Remove `ToolExecutionContext` from method signatures. Otherwise, minimal changes — the delete flow stays the same.

**New tests to add:**
- `AnthropicModelExecutorTest`: Verify that non-TEXT intents produce `ExecutionPlan.error()`.
- `AbstractModelExecutor.formatIntentContext()` test: Verify output formatting.

---

## Summary of All File Changes

### Delete

| File | Reason |
|---|---|
| `tool/ToolExecutionContext.java` | Replaced by typed method arguments. |

### Modify — Tool Layer

| File | Changes |
|---|---|
| `tool/AbstractArtifactTool.java` | Single `orchestrate(String, List<IntentMapping>)`. Remove ToolExecutionContext from all signatures. Simplified `doExecute` and `finalizeResult` abstract signatures. |
| `tool/creative/GenerateCreativeTool.java` | Model selection + fallback moves into `doExecute()`. `generate()` becomes: derive intents → orchestrate. `finalizeResult()` reads model from metadata. Remove all ToolExecutionContext references. |
| `tool/creative/DeleteArtifactTool.java` | Remove ToolExecutionContext from `doExecute()` and `finalizeResult()` signatures. |

### Modify — Executor Layer

| File | Changes |
|---|---|
| `models/AbstractModelExecutor.java` | Add `formatIntentContext()` helper. Add Javadoc for error message contract. |
| `models/anthropic/AnthropicModelExecutor.java` | Add text-only intent validation in `generatePlan()`. |
| `models/openai/OpenAiModelExecutor.java` | Add text-only intent validation. |
| `models/openrouter/OpenRouterModelExecutor.java` | Add text-only intent validation. |
| `models/fal/FalModelExecutor.java` | Include `formatIntentContext(intents)` in planning prompt. |
| `models/replicate/ReplicateModelExecutor.java` | Include `formatIntentContext(intents)` in planning prompt. |
| `models/elevenlabs/ElevenLabsModelExecutor.java` | Include `formatIntentContext(intents)` in planning prompt. |
| `models/runway/RunwayModelExecutor.java` | Include `formatIntentContext(intents)` in planning prompt. |
| `models/assembly/AssemblyAiModelExecutor.java` | Include `formatIntentContext(intents)` in planning prompt. |

### Modify — Prompt Layer

| File | Changes |
|---|---|
| `prompt/fragments/PromptFragments.java` | Add variation rules to `artifactIntents()`. |

### Modify — Tests

| File | Changes |
|---|---|
| `tool/creative/GenerateCreativeToolTest.java` | Mock `ArtifactIntentService`. Remove ToolExecutionContext. Update assertions for new flow (single intent derivation, model selection inside doExecute). |
| `tool/creative/DeleteArtifactToolTest.java` | Remove ToolExecutionContext from signatures. |
| New: `models/anthropic/AnthropicModelExecutorTest.java` | Test text-only validation. |

---

## Order of Implementation

1. **§2 — Consolidate `orchestrate()` signature** and **§1 — Remove `ToolExecutionContext`** (these are coupled — do together):
   - Simplify `AbstractArtifactTool` to single orchestrate signature.
   - Remove `ToolExecutionContext` from `doExecute`/`finalizeResult`.
   - Move model selection + fallback into `GenerateCreativeTool.doExecute()`.
   - Update `DeleteArtifactTool` signatures.
   - Delete `ToolExecutionContext.java`.
   - Update tests.

2. **§3 — Wire intents into executor planning:**
   - Add `formatIntentContext()` to `AbstractModelExecutor`.
   - Add text-only validation to Anthropic/OpenAI/OpenRouter.
   - Include intent context in Fal/Replicate/ElevenLabs/Runway/AssemblyAI.

3. **§4 — Update `artifactIntents()` prompt:**
   - Add variation rules.

4. **Verify:** Run full test suite. Confirm compilation. Smoke-test a generation + delete flow if integration tests exist.

---

## What This Plan Does NOT Change

- `ModelExecutor.run()` interface — already correct (accepts `intents` and `availableArtifacts`).
- `AbstractModelExecutor.run()` pipeline — already correct (plan → resolve → execute → extract → finalize).
- `ArtifactResolver` — already in `util`, already injected into executors.
- `ArtifactIntentService` — already correct signature (`deriveMappings(String task)`).
- `selfHealing()` prompt — already correct (category-based reasoning).
- Error message content in `AbstractModelExecutor` and `GenerateCreativeTool` — already correct (capacity vs. infrastructure distinction).
- `RestoreArtifactTool` — does not extend `AbstractArtifactTool`, unaffected.
- `Changelist` / `TaskExecutionDelta` — no structural changes.
